package com.mdvcraft.tools.fishing.mmocore;

import io.lumine.mythic.lib.api.MMOLineConfig;
import net.Indyuce.mmocore.loot.LootBuilder;
import net.Indyuce.mmocore.loot.droptable.dropitem.DropItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * Fishing-only action drop. It returns a hidden marker item to MMOCore; the
 * runtime listener consumes that marker during CustomPlayerFishEvent and
 * spawns the configured mob at the fisher's current location.
 */
public final class FishMobDropItem extends DropItem {
    static final String KIND_VANILLA = "vanilla";
    static final String KIND_MYTHIC = "mythic";

    private final JavaPlugin plugin;
    private final String kind;
    private final String mobId;
    private final int mythicLevel;

    public FishMobDropItem(JavaPlugin plugin, MMOLineConfig config) {
        super(config);
        this.plugin = plugin;

        boolean hasVanilla = config.contains("type");
        boolean hasMythic = config.contains("mythic");
        if (hasVanilla == hasMythic) {
            throw new IllegalArgumentException("fishmob requires exactly one of 'type=<VANILLA_ENTITY>' or 'mythic=<MYTHICMOB_ID>'");
        }

        if (hasVanilla) {
            String normalized = config.getString("type").trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            EntityType type;
            try {
                type = EntityType.valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown vanilla EntityType '" + config.getString("type") + "'");
            }

            if (!type.isSpawnable()) {
                throw new IllegalArgumentException("EntityType '" + type.name() + "' cannot be spawned");
            }

            this.kind = KIND_VANILLA;
            this.mobId = type.name();
            this.mythicLevel = 1;
        } else {
            String id = config.getString("mythic").trim();
            if (id.isBlank()) throw new IllegalArgumentException("fishmob mythic ID cannot be empty");

            this.kind = KIND_MYTHIC;
            this.mobId = id;
            this.mythicLevel = config.contains("level") ? Math.max(1, config.getInt("level")) : 1;
        }
    }

    @Override
    public void collect(LootBuilder builder) {
        int mobAmount = Math.max(1, rollAmount());

        // MMOCore's fishing implementation expects every DropItem to produce an
        // ItemStack. This marker is intercepted synchronously by MDVTools and
        // hidden/removed; the player never receives it.
        ItemStack marker = new ItemStack(Material.STRUCTURE_VOID, 1);
        ItemMeta meta = marker.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key("fish-action"), PersistentDataType.BYTE, (byte) 1);
        pdc.set(key("fish-mob-kind"), PersistentDataType.STRING, kind);
        pdc.set(key("fish-mob-id"), PersistentDataType.STRING, mobId);
        pdc.set(key("fish-mob-amount"), PersistentDataType.INTEGER, mobAmount);
        pdc.set(key("fish-mob-level"), PersistentDataType.INTEGER, mythicLevel);
        marker.setItemMeta(meta);

        builder.addLoot(marker);
    }

    private NamespacedKey key(String value) {
        return new NamespacedKey(plugin, value);
    }
}
