package com.mdvcraft.tools.fishing.mmocore;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.event.CustomPlayerFishEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * Registers MDVTools' MMOCore drop types during plugin load and handles the
 * runtime fishmob marker after a successful MMOCore fishing capture.
 */
public final class MMOCoreFishingDropExtension implements Listener {
    private final JavaPlugin plugin;
    private final MythicMobReflectionSpawner mythicSpawner;
    private boolean loaderRegistered;
    private boolean runtimeEnabled;

    public MMOCoreFishingDropExtension(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mythicSpawner = new MythicMobReflectionSpawner(plugin);
    }

    /** Must be called from MDVTools#onLoad, before MMOCore#onEnable parses professions. */
    public void registerLoader() {
        if (loaderRegistered) return;
        MMOCore.plugin.loadManager.registerLoader(new MDVToolsMMOLoader(plugin));
        loaderRegistered = true;
        plugin.getLogger().info("[FishingDrops] MMOCore loader registrado: enchantedbook + fishmob.");
    }

    /** Called from MDVTools#onEnable. */
    public void enableRuntime() {
        if (runtimeEnabled) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        runtimeEnabled = true;
    }

    public void disableRuntime() {
        if (!runtimeEnabled) return;
        HandlerList.unregisterAll(this);
        runtimeEnabled = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCustomFish(CustomPlayerFishEvent event) {
        Item dropped = event.getDroppedItem();
        ItemStack stack = dropped.getItemStack();
        if (!isFishMobMarker(stack)) return;

        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String kind = pdc.get(key("fish-mob-kind"), PersistentDataType.STRING);
        String mobId = pdc.get(key("fish-mob-id"), PersistentDataType.STRING);
        Integer amount = pdc.get(key("fish-mob-amount"), PersistentDataType.INTEGER);
        Integer level = pdc.get(key("fish-mob-level"), PersistentDataType.INTEGER);

        // Keep MMOCore's successful fishing flow intact so profession EXP,
        // vanilla EXP, statistics and its visual feedback are still awarded.
        // The marker is hidden immediately and removed next tick instead of
        // cancelling CustomPlayerFishEvent.
        hideMarker(dropped);

        if (kind == null || mobId == null) {
            plugin.getLogger().warning("[FishingDrops] Marcador fishmob incompleto; no se invocó ningún mob.");
            return;
        }

        int count = Math.max(1, amount == null ? 1 : amount);
        int mobLevel = Math.max(1, level == null ? 1 : level);
        Player player = event.getPlayer();
        Location spawn = player.getLocation().clone();

        if (kind.equalsIgnoreCase(FishMobDropItem.KIND_MYTHIC)) {
            for (int i = 0; i < count; i++) mythicSpawner.spawn(mobId, mobLevel, spawn.clone());
            return;
        }

        if (kind.equalsIgnoreCase(FishMobDropItem.KIND_VANILLA)) {
            EntityType type;
            try {
                type = EntityType.valueOf(mobId.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("[FishingDrops] EntityType inválido almacenado: '" + mobId + "'.");
                return;
            }

            for (int i = 0; i < count; i++) {
                player.getWorld().spawnEntity(spawn.clone(), type);
            }
        }
    }

    private boolean isFishMobMarker(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        Byte marker = stack.getItemMeta().getPersistentDataContainer()
                .get(key("fish-action"), PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private void hideMarker(Item item) {
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setGravity(false);
        item.setInvulnerable(true);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getWorld().equals(item.getWorld())) viewer.hideEntity(plugin, item);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (item.isValid() && !item.isDead()) item.remove();
        });
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value);
    }
}
