package com.mdvcraft.tools;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MDVToolsPlaceholderExpansion extends PlaceholderExpansion {

    private final MDVToolsPlugin plugin;

    public MDVToolsPlaceholderExpansion(MDVToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mdvtools";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MDVCRAFT";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String getRequiredPlugin() {
        return "MDVTools";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return plugin.resolvePlaceholder(player, params);
    }
}
