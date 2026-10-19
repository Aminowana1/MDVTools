package com.mdvcraft.tools.amulets;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/** Reuses the normal F event so MMOItems costs, cooldowns and MDVTools locks still apply. */
public final class BedrockSwapCastListener implements Listener {
    private final Predicate<ItemStack> configuredAmulet;
    private final Predicate<UUID> bedrockPlayer;
    private final Consumer<PlayerSwapHandItemsEvent> dispatch;
    private final LongSupplier nanoTime;
    private final BedrockPlayerDetector detector;
    private final DoubleSneakTracker<ItemStack> presses = new DoubleSneakTracker<>();
    private final Set<PlayerSwapHandItemsEvent> syntheticSwaps =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean enabled;
    private long windowNanos;

    public BedrockSwapCastListener(Plugin plugin, Predicate<ItemStack> configuredAmulet) {
        this.configuredAmulet = configuredAmulet;
        this.detector = new BedrockPlayerDetector(plugin);
        this.bedrockPlayer = detector;
        this.dispatch = event -> plugin.getServer().getPluginManager().callEvent(event);
        this.nanoTime = System::nanoTime;
    }

    BedrockSwapCastListener(Predicate<ItemStack> configuredAmulet, Predicate<UUID> bedrockPlayer,
                            Consumer<PlayerSwapHandItemsEvent> dispatch, LongSupplier nanoTime) {
        this.configuredAmulet = configuredAmulet;
        this.bedrockPlayer = bedrockPlayer;
        this.dispatch = dispatch;
        this.nanoTime = nanoTime;
        this.detector = null;
    }

    public void reload(ConfigurationSection config) {
        shutdown();
        enabled = config.getBoolean("offhand-swap-cast.enabled", true)
                && config.getBoolean("offhand-swap-cast.bedrock-double-sneak.enabled", true);
        long windowMs = Math.max(1L, Math.min(2000L,
                config.getLong("offhand-swap-cast.bedrock-double-sneak.window-ms", 450L)));
        windowNanos = TimeUnit.MILLISECONDS.toNanos(windowMs);
        if (enabled && detector != null) detector.reload();
    }

    public void shutdown() {
        enabled = false;
        presses.clear();
        syntheticSwaps.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!enabled || !event.isSneaking()) return;
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (!bedrockPlayer.test(playerId)) {
            presses.forget(playerId);
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack offhand = inventory.getItemInOffHand();
        if (!configuredAmulet.test(offhand) || player.isDead() || !player.isOnline()) {
            presses.forget(playerId);
            return;
        }
        if (!presses.press(playerId, offhand.clone(), nanoTime.getAsLong(), windowNanos)) return;

        // Bukkit/Paper fires this event before applying the new sneak state. On a
        // second press the player is still standing, so MythicLib selects SWAP_ITEMS
        // (regular F) rather than SHIFT_SWAP_ITEMS. Do not force the player's state.
        if (player.isSneaking()) return;

        // Event items describe the proposed swapped hands, just like vanilla F.
        // Only dispatch the event; no inventory swap or snapshot restoration occurs.
        PlayerSwapHandItemsEvent swap = new PlayerSwapHandItemsEvent(player,
                offhand.clone(), inventory.getItemInMainHand().clone());
        syntheticSwaps.add(swap);
        try {
            // MythicLib listens before HIGHEST with ignoreCancelled=true, so this
            // must begin uncancelled. All normal ability checks still see the cast.
            dispatch.accept(swap);
        } finally {
            swap.setCancelled(true);
            syntheticSwaps.remove(swap);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void preventSyntheticSwap(PlayerSwapHandItemsEvent event) {
        // Cancel even if the cast broke the last use of the configured amulet.
        // MONITOR listeners must not treat this gesture as a real hand swap.
        if (syntheticSwaps.contains(event)) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        presses.forget(event.getPlayer().getUniqueId());
    }
}
