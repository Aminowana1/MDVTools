package com.mdvcraft.tools.fishing.mmocore;

import io.lumine.mythic.lib.api.MMOLineConfig;
import net.Indyuce.mmocore.loot.LootBuilder;
import net.Indyuce.mmocore.loot.droptable.dropitem.DropItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Creates a genuine vanilla ENCHANTED_BOOK with one stored enchantment.
 * No MMOItems NBT/PDC is added to the book.
 */
public final class EnchantedBookDropItem extends DropItem {
    private static final Map<String, String> ALIASES = createAliases();

    private final Enchantment enchantment;
    private final int level;

    public EnchantedBookDropItem(MMOLineConfig config) {
        super(config);

        String rawEnchant = null;
        if (config.contains("enchant")) rawEnchant = config.getString("enchant");
        else if (config.contains("enchantment")) rawEnchant = config.getString("enchantment");

        if (rawEnchant == null || rawEnchant.isBlank()) {
            throw new IllegalArgumentException("enchantedbook requires 'enchant=<vanilla enchantment>'");
        }

        String enchantKey = normalizeEnchantKey(rawEnchant);
        NamespacedKey namespacedKey = NamespacedKey.minecraft(enchantKey);

        @SuppressWarnings("deprecation")
        Enchantment resolved = Enchantment.getByKey(namespacedKey);
        if (resolved == null) {
            throw new IllegalArgumentException("Unknown vanilla enchantment '" + rawEnchant + "' (resolved as '" + enchantKey + "')");
        }
        this.enchantment = resolved;

        this.level = config.contains("level") ? config.getInt("level") : 1;
        if (level < enchantment.getStartLevel() || level > enchantment.getMaxLevel()) {
            throw new IllegalArgumentException("Invalid level " + level + " for enchantment '" + enchantKey
                    + "' (allowed " + enchantment.getStartLevel() + "-" + enchantment.getMaxLevel() + ")");
        }
    }

    @Override
    public void collect(LootBuilder builder) {
        int amount = Math.max(1, rollAmount());
        for (int i = 0; i < amount; i++) {
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
            if (!(book.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
                throw new IllegalStateException("ENCHANTED_BOOK did not provide EnchantmentStorageMeta");
            }

            meta.addStoredEnchant(enchantment, level, false);
            book.setItemMeta(meta);
            builder.addLoot(book);
        }
    }

    private static String normalizeEnchantKey(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');

        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }

        return ALIASES.getOrDefault(normalized, normalized);
    }

    private static Map<String, String> createAliases() {
        Map<String, String> aliases = new HashMap<>();

        aliases.put("protection", "protection");
        aliases.put("protection_environmental", "protection");
        aliases.put("proteccion", "protection");

        aliases.put("fortune", "fortune");
        aliases.put("fortuna", "fortune");

        aliases.put("sharpness", "sharpness");
        aliases.put("damage_all", "sharpness");
        aliases.put("filo", "sharpness");

        aliases.put("efficiency", "efficiency");
        aliases.put("dig_speed", "efficiency");
        aliases.put("eficiencia", "efficiency");

        aliases.put("unbreaking", "unbreaking");
        aliases.put("durability", "unbreaking");
        aliases.put("irrompibilidad", "unbreaking");

        aliases.put("mending", "mending");
        aliases.put("reparacion", "mending");

        aliases.put("feather_falling", "feather_falling");
        aliases.put("protection_fall", "feather_falling");
        aliases.put("caida_de_pluma", "feather_falling");
        aliases.put("caida_pluma", "feather_falling");

        aliases.put("power", "power");
        aliases.put("arrow_damage", "power");
        aliases.put("poder", "power");

        return aliases;
    }
}
