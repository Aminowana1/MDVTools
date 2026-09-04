package com.mdvcraft.tools.fishing;

import org.bukkit.configuration.file.FileConfiguration;

/** Immutable configuration snapshot for the MMOCore fishing fight timer. */
public record FishingFightTimerSettings(
        boolean enabled,
        double maxFightTimeSeconds,
        boolean onlyMmocoreCustomFishing,
        String mmocorePluginName,
        boolean debug,
        boolean sendEscapeMessage,
        String escapeMessage,
        boolean escapeSoundEnabled,
        String escapeSound,
        float escapeSoundVolume,
        float escapeSoundPitch
) {
    public static FishingFightTimerSettings from(FileConfiguration config) {
        return new FishingFightTimerSettings(
                config.getBoolean("fishing-fight-timer.enabled", true),
                Math.max(0.1D, config.getDouble("fishing-fight-timer.max-fight-time-seconds", 3.0D)),
                config.getBoolean("fishing-fight-timer.only-mmocore-custom-fishing", true),
                config.getString("fishing-fight-timer.mmocore-plugin-name", "MMOCore"),
                config.getBoolean("fishing-fight-timer.debug", false),
                config.getBoolean("fishing-fight-timer.messages.enabled", true),
                config.getString("fishing-fight-timer.messages.escaped", "&c¡El pez escapó! &7No lograste recogerlo a tiempo."),
                config.getBoolean("fishing-fight-timer.sound.enabled", true),
                config.getString("fishing-fight-timer.sound.value", "entity.fishing_bobber.retrieve"),
                (float) config.getDouble("fishing-fight-timer.sound.volume", 0.8D),
                (float) config.getDouble("fishing-fight-timer.sound.pitch", 0.7D)
        );
    }

    public long maxFightTicks() {
        return Math.max(1L, Math.round(maxFightTimeSeconds * 20.0D));
    }
}
