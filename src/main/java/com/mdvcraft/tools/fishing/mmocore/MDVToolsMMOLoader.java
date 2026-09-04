package com.mdvcraft.tools.fishing.mmocore;

import io.lumine.mythic.lib.api.MMOLineConfig;
import net.Indyuce.mmocore.api.load.MMOLoader;
import net.Indyuce.mmocore.loot.droptable.dropitem.DropItem;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Extra MMOCore drop types used by MDVTools fishing.
 *
 * <p>Registered during MDVTools#onLoad so MMOCore can parse these entries
 * while loading profession on-fish tables.</p>
 */
public final class MDVToolsMMOLoader extends MMOLoader {
    private final JavaPlugin plugin;

    public MDVToolsMMOLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public DropItem loadDropItem(MMOLineConfig config) {
        String key = config.getKey();

        if (key.equalsIgnoreCase("enchantedbook") || key.equalsIgnoreCase("enchanted_book")) {
            return new EnchantedBookDropItem(config);
        }

        if (key.equalsIgnoreCase("fishmob") || key.equalsIgnoreCase("fish_mob")) {
            return new FishMobDropItem(plugin, config);
        }

        return null;
    }
}
