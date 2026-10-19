package net.Indyuce.mmoitems.api.interaction.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/** Minimal modern MMOItems API contract: abstract base, public factory, fixed loss hook. */
public abstract class DurabilityItem {
    private static final Map<ItemStack, Integer> REMAINING = new IdentityHashMap<>();
    public static int factoryCalls;
    public static int fixedLossCalls;
    public static int randomLossCalls;
    public static EquipmentSlot lastSlot;

    protected DurabilityItem() { }

    public static void reset() {
        REMAINING.clear();
        factoryCalls = fixedLossCalls = randomLossCalls = 0;
        lastSlot = null;
    }

    public static void register(ItemStack item, int remaining) {
        REMAINING.put(item, remaining);
    }

    public static int remaining(ItemStack item) {
        return REMAINING.getOrDefault(item, 100);
    }

    public static DurabilityItem custom(Player player, EquipmentSlot slot, ItemStack item) {
        factoryCalls++;
        lastSlot = slot;
        return new DurabilityItem() {
            @Override public int getDurability() { return remaining(item); }
            @Override public void onDurabilityDecrease(int cost) {
                fixedLossCalls++;
                REMAINING.put(item, Math.max(0, remaining(item) - cost));
            }
            @Override public ItemStack toItem() { return item; }
        };
    }

    public abstract int getDurability();
    public abstract void onDurabilityDecrease(int cost);
    public abstract ItemStack toItem();
    public boolean isValid() { return true; }

    public DurabilityItem decreaseDurability(int cost) {
        randomLossCalls++;
        // Models an Unbreaking roll that prevents this durability loss.
        return this;
    }
}
