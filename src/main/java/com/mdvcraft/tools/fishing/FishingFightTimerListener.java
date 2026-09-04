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
 * MMOCore remains responsible for tugs, its 1-second inter-click timeout,
 * rewards, EXP and fishing stats. MDVTools only starts one independent clock
 * on BITE and removes the hook when the configured total time is exhausted.
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
        announceSettings();
    }

    public void reload() {
        this.settings = FishingFightTimerSettings.from(plugin.getConfig());
        this.mmocoreBridge.reload(settings.mmocorePluginName());
        clearAll();
        announceSettings();
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
            FishHook hook = event.getHook();

            if (settings.onlyMmocoreCustomFishing() && !mmocoreBridge.hasCustomFishingTable(player, hook)) {
                debug("BITE ignorado para " + player.getName() + ": MMOCore no devolvió tabla custom.");
                return;
            }

            startFight(player, hook);
            return;
        }

        // MMOCore uses CAUGHT_FISH / FAILED_ATTEMPT / REEL_IN as tug pulls.
        // These events MUST NOT restart the total timer.
        ActiveFight fight = activeFights.get(playerId);
        if (fight != null && (!fight.hook().isValid() || fight.hook().isDead())) {
            debug("Timer limpiado para " + player.getName() + ": MMOCore cerró la captura antes del límite.");
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

        debug("Timer iniciado para " + player.getName() + ": "
                + settings.maxFightTimeSeconds() + "s (" + delay + " ticks), hook=" + hookId + ".");

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ActiveFight fight = activeFights.get(playerId);
            if (fight == null || !fight.hookId().equals(hookId)) return;

            if (!hook.isValid() || hook.isDead()) {
                activeFights.remove(playerId);
                debug("Timer venció pero el hook de " + player.getName() + " ya estaba cerrado.");
                return;
            }

            activeFights.remove(playerId);
            hook.remove();
            debug("Tiempo total agotado para " + player.getName() + "; captura cancelada.");
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
                debug("Sonido inválido: " + settings.escapeSound());
            }
        }
    }

    private void cancelFight(UUID playerId) {
        ActiveFight previous = activeFights.remove(playerId);
        if (previous != null && previous.task() != null) previous.task().cancel();
    }

    private void clearAll() {
        for (ActiveFight fight : activeFights.values()) {
            if (fight.task() != null) fight.task().cancel();
        }
        activeFights.clear();
    }

    private void announceSettings() {
        if (!settings.enabled()) {
            plugin.getLogger().info("[FishingFightTimer] Desactivado por config.");
            return;
        }

        plugin.getLogger().info("[FishingFightTimer] Activado: límite total="
                + settings.maxFightTimeSeconds() + "s, only-mmocore=" + settings.onlyMmocoreCustomFishing() + ".");

        if (settings.onlyMmocoreCustomFishing()) {
            // Resolve immediately so an incompatible MMOCore build is visible at startup,
            // rather than discovering it only when a player fishes.
            mmocoreBridge.isAvailable();
        }
    }

    private void debug(String message) {
        if (settings.debug()) plugin.getLogger().info("[FishingFightTimer][DEBUG] " + message);
    }

    @SuppressWarnings("deprecation")
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private record ActiveFight(UUID hookId, FishHook hook, BukkitTask task) {
    }
}
