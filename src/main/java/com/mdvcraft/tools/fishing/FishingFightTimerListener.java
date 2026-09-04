package com.mdvcraft.tools.fishing;

import org.bukkit.ChatColor;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adds a TOTAL time limit to MMOCore's tug minigame.
 *
 * MMOCore keeps full ownership of its own mechanics: tugs, the ~1 second
 * timeout between pulls, rewards, experience, critical fishing and rod logic.
 * This listener only starts a second clock when the fish BITES. If the hook is
 * still active when that clock expires, the hook is removed and the catch is
 * lost.
 */
public final class FishingFightTimerListener implements Listener {
    private final JavaPlugin plugin;
    private final MMOCoreFishingBridge mmocoreBridge;
    private final Map<UUID, ActiveFight> activeFights = new HashMap<>();

    private FishingFightTimerSettings settings;

    public FishingFightTimerListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mmocoreBridge = new MMOCoreFishingBridge(plugin);
        this.settings = FishingFightTimerSettings.from(plugin.getConfig());
        this.mmocoreBridge.reload(settings.mmocorePluginName());
    }

    public void reload() {
        this.settings = FishingFightTimerSettings.from(plugin.getConfig());
        this.mmocoreBridge.reload(settings.mmocorePluginName());

        // A reload must never leave old timers running with stale values.
        clearAll();
    }

    public void shutdown() {
        clearAll();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!settings.enabled()) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.getState() == PlayerFishEvent.State.BITE) {
            if (settings.onlyMmocoreCustomFishing() && !mmocoreBridge.hasCustomFishingTable(player)) {
                return;
            }

            startFight(player, event.getHook());
            return;
        }

        // MMOCore reuses CAUGHT_FISH / FAILED_ATTEMPT / REEL_IN as tug clicks.
        // Never reset the total timer on those clicks. We only clean up when
        // MMOCore has already removed the hook after success/failure.
        ActiveFight fight = activeFights.get(playerId);
        if (fight != null && (!fight.hook().isValid() || fight.hook().isDead())) {
            cancelFight(playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        cancelFight(event.getPlayer().getUniqueId());
    }

    private void startFight(Player player, FishHook hook) {
        cancelFight(player.getUniqueId());

        UUID playerId = player.getUniqueId();
        UUID hookId = hook.getUniqueId();
        long delay = settings.maxFightTicks();

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ActiveFight fight = activeFights.get(playerId);
            if (fight == null || !fight.hookId().equals(hookId)) return;

            // If MMOCore already completed/closed the catch, this timer is stale.
            if (!hook.isValid() || hook.isDead()) {
                activeFights.remove(playerId);
                return;
            }

            activeFights.remove(playerId);
            hook.remove();
            playEscapeFeedback(player);
        }, delay);

        activeFights.put(playerId, new ActiveFight(hookId, hook, task));
    }

    private void playEscapeFeedback(Player player) {
        if (!player.isOnline()) return;

        if (settings.sendEscapeMessage() && settings.escapeMessage() != null && !settings.escapeMessage().isBlank()) {
            player.sendMessage(color(settings.escapeMessage()));
        }

        if (settings.escapeSoundEnabled() && settings.escapeSound() != null && !settings.escapeSound().isBlank()) {
            try {
                player.playSound(
                        player.getLocation(),
                        settings.escapeSound(),
                        settings.escapeSoundVolume(),
                        settings.escapeSoundPitch()
                );
            } catch (IllegalArgumentException ignored) {
                // Invalid sound names must never break the fishing flow.
            }
        }
    }

    private void cancelFight(UUID playerId) {
        ActiveFight previous = activeFights.remove(playerId);
        if (previous != null && previous.task() != null) {
            previous.task().cancel();
        }
    }

    private void clearAll() {
        for (ActiveFight fight : activeFights.values()) {
            if (fight.task() != null) fight.task().cancel();
        }
        activeFights.clear();
    }

    @SuppressWarnings("deprecation")
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private record ActiveFight(UUID hookId, FishHook hook, BukkitTask task) {
    }
}
