package com.mdvcraft.tools.fishing;

import org.bukkit.configuration.file.FileConfiguration;

/** Immutable configuration snapshot for the MMOCore fishing fight timer. */
public record FishingFightTimerSettings(
        boolean enabled,
        double maxFightTimeSeconds,
        boolean onlyMmocoreCustomFishing,
        String mmocorePluginName,
        boolean debug,
        boolean hologramEnabled,
        String hologramText,
        double hologramDurationSeconds,
        double hologramYOffset,
        boolean hologramShadowed,
        boolean hologramSeeThrough,
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
                config.getBoolean("fishing-fight-timer.hologram.enabled", true),
                config.getString("fishing-fight-timer.hologram.text", "&c¡El pez escapó! &7No lograste recogerlo a tiempo."),
                Math.max(0.1D, config.getDouble("fishing-fight-timer.hologram.duration-seconds", 2.0D)),
                config.getDouble("fishing-fight-timer.hologram.y-offset", 0.85D),
                config.getBoolean("fishing-fight-timer.hologram.shadowed", true),
                config.getBoolean("fishing-fight-timer.hologram.see-through", true),
                config.getBoolean("fishing-fight-timer.sound.enabled", true),
                config.getString("fishing-fight-timer.sound.value", "entity.fishing_bobber.retrieve"),
                (float) config.getDouble("fishing-fight-timer.sound.volume", 0.8D),
                (float) config.getDouble("fishing-fight-timer.sound.pitch", 0.7D)
        );
    }

    public long maxFightTicks() {
        return Math.max(1L, Math.round(maxFightTimeSeconds * 20.0D));
    }

    public long hologramDurationTicks() {
        return Math.max(1L, Math.round(hologramDurationSeconds * 20.0D));
    }
}
