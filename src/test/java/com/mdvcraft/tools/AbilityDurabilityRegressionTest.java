package com.mdvcraft.tools;

import io.lumine.mythic.lib.api.event.PlayerClickEvent;
import net.Indyuce.mmoitems.api.interaction.util.DurabilityItem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AbilityDurabilityRegressionTest {
    private MDVToolsPlugin plugin;
    private Player player;
    private PlayerInventory inventory;
    private ItemStack main;
    private ItemStack offhand;

    @BeforeEach
    void prepare() throws Exception {
        plugin = mock(MDVToolsPlugin.class, CALLS_REAL_METHODS);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        set("abilityDurabilityCostEnabled", true);
        set("abilityDurabilityCostAmount", 1);
        set("abilityDurabilityOnlyCustomDurability", true);
        set("abilityDurabilityDedupeWindowMs", 60_000L);
        set("abilityDurabilitySourceHints", new HashMap<>());
        set("abilityDurabilityLastCharge", new HashMap<>());
        set("weaponSwapLockHookedEvents", new HashSet<>());
        set("mmoNbtReflectionTried", true);
        set("mmoNbtItemClass", FakeNbt.class);
        set("mmoNbtGetMethod", FakeNbt.class.getMethod("get", ItemStack.class));
        set("mmoNbtHasTypeMethod", FakeNbt.class.getMethod("hasType"));
        set("mmoNbtGetTypeMethod", FakeNbt.class.getMethod("getType"));
        set("mmoNbtGetStringMethod", FakeNbt.class.getMethod("getString", String.class));
        set("mmoNbtGetIntegerMethod", FakeNbt.class.getMethod("getInteger", String.class));
        set("mmoNbtHasTagMethod", FakeNbt.class.getMethod("hasTag", String.class));
        DurabilityItem.reset();
        FakeNbt.ITEMS.clear();
        main = item(Material.DIAMOND_SWORD, "SWORD", "MAIN", 100);
        offhand = item(Material.PLAYER_HEAD, "AMULETO", "OFFHAND", 80);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(main);
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenReturn(offhand);
        when(inventory.getItemInMainHand()).thenReturn(main);
        when(inventory.getItemInOffHand()).thenReturn(offhand);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void clickHookRunsAtLowestBeforeNestedSkillAndChargesFirstMainhandClick() throws Exception {
        PluginManager manager = mock(PluginManager.class);
        ArgumentCaptor<EventExecutor> executor = ArgumentCaptor.forClass(EventExecutor.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            call("registerAbilityDurabilityClickSourceEvent");
            verify(manager).registerEvent(eq((Class) PlayerClickEvent.class), eq(plugin),
                    eq(EventPriority.LOWEST), executor.capture(), eq(plugin), eq(false));
            // MythicLib dispatches this click inside its own Bukkit LOWEST listener.
            executor.getValue().execute(plugin, new PlayerClickEvent(player,
                    io.lumine.mythic.lib.api.player.EquipmentSlot.MAIN_HAND,
                    false, null, main, mock(Cancellable.class)));
            call("handleAbilityDurabilityExternalEvent", new SkillEvent(player));
        }
        assertEquals(99, DurabilityItem.remaining(main));
        assertEquals(80, DurabilityItem.remaining(offhand));
        assertEquals(EquipmentSlot.HAND, DurabilityItem.lastSlot);
    }

    @Test
    void offhandInputChargesAmuletAndRetainsSourceForNestedCallback() throws Exception {
        remember(EquipmentSlot.OFF_HAND, offhand);
        SkillEvent event = new SkillEvent(player);
        call("handleAbilityDurabilityExternalEvent", event);
        call("handleAbilityDurabilityExternalEvent", event);
        assertEquals(79, DurabilityItem.remaining(offhand));
        assertEquals(100, DurabilityItem.remaining(main));
        assertEquals(1, DurabilityItem.fixedLossCalls);
        assertEquals(EquipmentSlot.OFF_HAND, sourceSlot(call("resolveAbilityDurabilitySource", event, player)));
        verify(inventory, never()).setItemInMainHand(any());
    }

    @Test
    void explicitEventItemWinsAnUnrelatedMainhandHintEvenWithoutSlot() throws Exception {
        remember(EquipmentSlot.HAND, main);
        Object source = call("resolveAbilityDurabilitySource", new ItemSkillEvent(player, offhand), player);
        assertEquals(EquipmentSlot.OFF_HAND, sourceSlot(source));
        assertSame(offhand, sourceItem(source));
    }

    @Test
    void replacingHeldItemAfterInputNeverChargesItsReplacement() throws Exception {
        remember(EquipmentSlot.HAND, main);
        ItemStack replacement = item(Material.DIAMOND_SWORD, "SWORD", "DIFFERENT", 50);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(replacement);
        assertNull(call("resolveAbilityDurabilitySource", new MetadataSkillEvent(player), player));
        // LOWEST precheck and repeated MONITOR callbacks must retain that rejection.
        call("handleAbilityDurabilityExternalEvent", new MetadataSkillEvent(player));
        call("handleAbilityDurabilityExternalEvent", new MetadataSkillEvent(player));
        assertEquals(50, DurabilityItem.remaining(replacement));
        assertEquals(0, DurabilityItem.fixedLossCalls);
    }

    @Test
    void cancelledCastDoesNotSpendOrOccupyTheDedupeWindow() throws Exception {
        remember(EquipmentSlot.HAND, main);
        SkillEvent cancelled = new SkillEvent(player);
        cancelled.setCancelled(true);
        call("handleAbilityDurabilityExternalEvent", cancelled);
        assertEquals(100, DurabilityItem.remaining(main));
        call("handleAbilityDurabilityExternalEvent", new SkillEvent(player));
        assertEquals(99, DurabilityItem.remaining(main));
        assertEquals(1, DurabilityItem.fixedLossCalls);
    }

    @Test
    void zeroCustomDurabilityCancelsBeforeCastingAndDoesNotSpendAgain() throws Exception {
        DurabilityItem.register(main, 0);
        remember(EquipmentSlot.HAND, main);
        SkillEvent exhausted = new SkillEvent(player);
        call("handleWeaponSwapLockExternalAbilityEvent", exhausted);
        assertTrue(exhausted.isCancelled());
        call("handleAbilityDurabilityExternalEvent", exhausted);
        assertEquals(0, DurabilityItem.fixedLossCalls);
    }

    @Test
    void newItemWithoutCurrentDurabilityTagIsFullAndMayCast() throws Exception {
        FakeNbt.ITEMS.get(main).hasCurrent = false;
        remember(EquipmentSlot.HAND, main);
        SkillEvent fresh = new SkillEvent(player);
        call("handleWeaponSwapLockExternalAbilityEvent", fresh);
        assertFalse(fresh.isCancelled());
        call("handleAbilityDurabilityExternalEvent", fresh);
        assertEquals(99, DurabilityItem.remaining(main));
    }

    @Test
    void modernAbstractFactoryAppliesConfiguredFixedCostWithoutUnbreakingRoll() throws Exception {
        set("abilityDurabilityCostAmount", 3);
        remember(EquipmentSlot.HAND, main);
        call("handleAbilityDurabilityExternalEvent", new SkillEvent(player));
        assertEquals(97, DurabilityItem.remaining(main));
        assertEquals(1, DurabilityItem.factoryCalls);
        assertEquals(1, DurabilityItem.fixedLossCalls);
        assertEquals(0, DurabilityItem.randomLossCalls);
        assertEquals("onDurabilityDecrease", ((Method) field("mmoDurabilityDecreaseMethod")).getName());
        assertNotNull(field("mmoDurabilityCustomFactory"));
    }

    @Test
    void dedupeIsSpecificToTheItemAndHand() throws Exception {
        remember(EquipmentSlot.HAND, main);
        call("handleAbilityDurabilityExternalEvent", new SkillEvent(player));
        call("handleAbilityDurabilityExternalEvent", new SkillEvent(player));
        remember(EquipmentSlot.OFF_HAND, offhand);
        call("handleAbilityDurabilityExternalEvent", new SkillEvent(player));
        assertEquals(99, DurabilityItem.remaining(main));
        assertEquals(79, DurabilityItem.remaining(offhand));
        assertEquals(2, DurabilityItem.fixedLossCalls);
    }

    @Test
    void protectedUnbreakableItemKeepsItsCustomDurability() throws Exception {
        set("customDurabilityProtectionEnabled", true);
        set("customDurabilityProtectionUseItemMeta", true);
        ItemMeta meta = mock(ItemMeta.class);
        when(meta.isUnbreakable()).thenReturn(true);
        when(main.getItemMeta()).thenReturn(meta);
        remember(EquipmentSlot.HAND, main);
        SkillEvent event = new SkillEvent(player);
        call("handleWeaponSwapLockExternalAbilityEvent", event);
        call("handleAbilityDurabilityExternalEvent", event);
        assertFalse(event.isCancelled());
        assertEquals(100, DurabilityItem.remaining(main));
        assertEquals(0, DurabilityItem.factoryCalls);
    }

    @Test
    void cachedMetadataActionHandResolvesFirstCastWithoutAnyInputHint() throws Exception {
        SkillEvent event = new MetadataSkillEvent(player);
        Object source = call("resolveAbilityDurabilitySource", event, player);
        assertEquals(EquipmentSlot.HAND, sourceSlot(source));
        assertSame(main, sourceItem(source));
        call("handleAbilityDurabilityExternalEvent", event);
        assertEquals(99, DurabilityItem.remaining(main));
        assertEquals(80, DurabilityItem.remaining(offhand));
    }

    private ItemStack item(Material material, String type, String id, int remaining) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);
        FakeNbt.ITEMS.put(item, new FakeNbt(item, type, id));
        DurabilityItem.register(item, remaining);
        return item;
    }

    private void remember(EquipmentSlot slot, ItemStack item) throws Exception {
        call("rememberAbilityDurabilitySource", player, slot, item);
    }

    private Object call(String name, Object... args) throws Exception {
        for (Method method : MDVToolsPlugin.class.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(plugin, args);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private void set(String name, Object value) throws Exception {
        Field field = MDVToolsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }

    private Object field(String name) throws Exception {
        Field field = MDVToolsPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(plugin);
    }

    private Object sourceField(Object source, String name) throws Exception {
        assertNotNull(source);
        Field field = source.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(source);
    }

    private EquipmentSlot sourceSlot(Object source) throws Exception {
        return (EquipmentSlot) sourceField(source, "slot");
    }

    private ItemStack sourceItem(Object source) throws Exception {
        return (ItemStack) sourceField(source, "item");
    }

    public static class SkillEvent extends PlayerEvent implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled;
        public SkillEvent(Player player) { super(player); }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static class ItemSkillEvent extends SkillEvent {
        private final ItemStack item;
        public ItemSkillEvent(Player player, ItemStack item) { super(player); this.item = item; }
        public ItemStack getItem() { return item; }
    }

    public static class MetadataSkillEvent extends SkillEvent {
        public MetadataSkillEvent(Player player) { super(player); }
        public Metadata getMetadata() { return new Metadata(); }
    }

    public static class Metadata {
        public Event getSourceEvent() { return null; }
        public Caster getCaster() { return new Caster(); }
    }

    public static class Caster {
        public String getActionHand() { return "MAIN_HAND"; }
    }

    public static class FakeNbt {
        private static final Map<ItemStack, FakeNbt> ITEMS = new IdentityHashMap<>();
        private final ItemStack item;
        private final String type;
        private final String id;
        private boolean hasCurrent = true;
        FakeNbt(ItemStack item, String type, String id) { this.item = item; this.type = type; this.id = id; }
        public static FakeNbt get(ItemStack item) { return ITEMS.get(item); }
        public boolean hasType() { return true; }
        public String getType() { return type; }
        public String getString(String key) { return key.equals("MMOITEMS_ITEM_ID") ? id : ""; }
        public int getInteger(String key) {
            return key.equals("MMOITEMS_MAX_DURABILITY") ? 100 : DurabilityItem.remaining(item);
        }
        public boolean hasTag(String key) { return !key.equals("MMOITEMS_DURABILITY") || hasCurrent; }
    }
}
