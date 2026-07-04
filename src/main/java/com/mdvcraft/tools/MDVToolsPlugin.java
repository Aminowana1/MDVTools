package com.mdvcraft.tools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class MDVToolsPlugin extends JavaPlugin implements Listener {

    private boolean debug;

    private Set<Material> miningAllowed = EnumSet.noneOf(Material.class);
    private Set<Material> logsAllowed = EnumSet.noneOf(Material.class);
    private Set<Material> cropsAllowed = EnumSet.noneOf(Material.class);
    private Set<Material> antiGhostAllowed = EnumSet.noneOf(Material.class);

    private boolean antiGhostEnabled;
    private boolean antiGhostUpdateNeighbors;
    private boolean antiGhostUpdateNearbyPlayers;
    private boolean antiGhostOnlyMdvLore;
    private int antiGhostPlayerRadius;
    private List<Integer> antiGhostDelays = new ArrayList<>();

    private boolean miningEnabled;
    private boolean woodcuttingEnabled;
    private boolean farmingEnabled;

    private int maxMiningExtra;
    private int maxWoodExtra;
    private int maxCropExtra;

    private File customDropsFile;
    private FileConfiguration customDropsConfig;
    private boolean customDropsEnabled;
    private String customDropsFallbackCommand;
    private final List<CustomDropDefinition> customDrops = new ArrayList<>();

    private boolean equipmentBonusesEnabled;
    private boolean agricultureRareBonusEnabled;
    private boolean headOreExtraBonusEnabled;
    private boolean treeNodeExtraBonusEnabled;
    private double maxEquipmentBonusPercent;
    private int headOreExtraAmount;
    private int treeNodeExtraAmount;
    private Pattern agricultureRareBonusPattern;
    private Pattern rareMineralsBonusPattern;
    private Pattern treeNodeExtraBonusPattern;
    private NamespacedKey headOreOreKey;
    private NamespacedKey headOreNodeKey;
    private NamespacedKey headOreDropTypeKey;
    private NamespacedKey headOreDropIdKey;

    private boolean crossbowAutoReloadEnabled;
    private boolean crossbowOnlyOnEntityHit;
    private boolean crossbowConsumeArrow;
    private boolean crossbowRequirePlayerOnline;
    private long crossbowCooldownMs;
    private long crossbowProjectileTtlTicks;
    private Material crossbowAmmoMaterial;
    private List<String> crossbowLoreKeys = new ArrayList<>();
    private String crossbowSoundName;
    private float crossbowSoundVolume;
    private float crossbowSoundPitch;
    private boolean crossbowSoundEnabled;
    private boolean crossbowParticlesEnabled;
    private String crossbowParticleName;
    private int crossbowParticleAmount;
    private final Map<UUID, AutoReloadShot> autoReloadProjectiles = new HashMap<>();
    private final Map<UUID, Long> autoReloadCooldowns = new HashMap<>();

    private boolean weaponSwapLockEnabled;
    private long weaponSwapLockDurationMs;
    private boolean weaponSwapLockOnlyWhenNewItemIsWeapon;
    private boolean weaponSwapLockIgnoreReturnToSameWeaponAfterNonWeapon;
    private Set<String> weaponSwapLockMmoTypes = new HashSet<>();
    private boolean weaponSwapLockFallbackEnabled;
    private Set<Material> weaponSwapLockFallbackMaterials = EnumSet.noneOf(Material.class);
    private boolean weaponSwapLockBlockInteract;
    private boolean weaponSwapLockBlockMeleeHit;
    private boolean weaponSwapLockBlockBowShoot;
    private boolean weaponSwapLockBlockMmoItemAbilities;
    private boolean weaponSwapLockBlockMythicLibSkills;
    private String weaponSwapLockBlockedMessage;
    private long weaponSwapLockBlockedMessageCooldownMs;
    private boolean weaponSwapLockReadyFeedbackEnabled;
    private String weaponSwapLockReadySoundName;
    private float weaponSwapLockReadySoundVolume;
    private float weaponSwapLockReadySoundPitch;
    private boolean weaponSwapLockReadyParticlesEnabled;
    private String weaponSwapLockReadyParticleName;
    private int weaponSwapLockReadyParticleAmount;
    private final Map<UUID, Long> weaponSwapLockUntil = new HashMap<>();
    private final Map<UUID, Long> weaponSwapLockLastBlockedMessage = new HashMap<>();
    private final Map<UUID, WeaponSwapItemIdentity> weaponSwapLastWeaponBeforeNonWeapon = new HashMap<>();
    private final Set<String> weaponSwapLockHookedEvents = new HashSet<>();

    private boolean mmoNbtReflectionTried;
    private Class<?> mmoNbtItemClass;
    private Method mmoNbtGetMethod;
    private Method mmoNbtHasTypeMethod;
    private Method mmoNbtGetTypeMethod;
    private Method mmoNbtGetStringMethod;

    private boolean durabilityEnabled;
    private int durabilityCostBlock;
    private int durabilityCostCrop;
    private boolean respectUnbreaking;

    private boolean handleOriginalCrop;
    private boolean replantNeedsSeed;

    private boolean internalBreakEvent = false;

    private Pattern talaPattern;
    private Pattern roturaPattern;
    private Pattern cosechaPattern;
    private String autoReplantKey;

    private String prefix;
    private String msgReloaded;
    private String msgNoPerm;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureCustomDropsFile();
        loadSettings();
        Bukkit.getPluginManager().registerEvents(this, this);
        registerWeaponSwapLockExternalEvents();
        getLogger().info("MDVTools activado.");
    }

    @Override
    public void onDisable() {
        weaponSwapLockUntil.clear();
        weaponSwapLockLastBlockedMessage.clear();
        weaponSwapLastWeaponBeforeNonWeapon.clear();
        getLogger().info("MDVTools desactivado.");
    }

    private void loadSettings() {
        reloadConfig();

        debug = getConfig().getBoolean("debug", false);

        prefix = color(getConfig().getString("messages.prefix", "&6&l[&5&lMDVCRAFT&6&l]  &4»  &r"));
        msgReloaded = color(getConfig().getString("messages.reloaded", "&aMDVTools recargado."));
        msgNoPerm = color(getConfig().getString("messages.no-permission", "&cNo tienes permiso para hacer eso."));

        String tala = getConfig().getString("lore.tala-multiple", "Tala Multiple");
        String rotura = getConfig().getString("lore.rotura-multiple", "Rotura Multiple");
        String cosecha = getConfig().getString("lore.multi-cosecha", "Multi Cosecha");
        autoReplantKey = normalize(getConfig().getString("lore.auto-replantar", "Auto Replantar"));

        talaPattern = Pattern.compile(Pattern.quote(normalize(tala)) + "\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        roturaPattern = Pattern.compile(Pattern.quote(normalize(rotura)) + "\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        cosechaPattern = Pattern.compile(Pattern.quote(normalize(cosecha)) + "\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

        miningEnabled = getConfig().getBoolean("mining.enabled", true);
        woodcuttingEnabled = getConfig().getBoolean("woodcutting.enabled", true);
        farmingEnabled = getConfig().getBoolean("farming.enabled", true);

        maxMiningExtra = Math.max(0, getConfig().getInt("mining.max-extra-blocks", 12));
        maxWoodExtra = Math.max(0, getConfig().getInt("woodcutting.max-extra-blocks", 24));
        maxCropExtra = Math.max(0, getConfig().getInt("farming.max-extra-crops", 24));

        durabilityEnabled = getConfig().getBoolean("durability.enabled", true);
        durabilityCostBlock = Math.max(0, getConfig().getInt("durability.cost-per-extra-block", 1));
        durabilityCostCrop = Math.max(0, getConfig().getInt("durability.cost-per-extra-crop", 1));
        respectUnbreaking = getConfig().getBoolean("durability.respect-unbreaking", true);

        handleOriginalCrop = getConfig().getBoolean("farming.handle-original-crop", true);
        replantNeedsSeed = getConfig().getBoolean("farming.auto-replant-needs-seed-from-drops", true);

        miningAllowed = loadMaterials("mining.allowed-blocks");
        logsAllowed = loadMaterials("woodcutting.allowed-blocks");
        cropsAllowed = loadMaterials("farming.allowed-crops");

        antiGhostEnabled = getConfig().getBoolean("anti-ghost.enabled", true);
        antiGhostUpdateNeighbors = getConfig().getBoolean("anti-ghost.update-neighbors", true);
        antiGhostUpdateNearbyPlayers = getConfig().getBoolean("anti-ghost.update-nearby-players", true);
        antiGhostOnlyMdvLore = getConfig().getBoolean("anti-ghost.only-tools-with-mdv-lore", false);
        antiGhostPlayerRadius = Math.max(1, getConfig().getInt("anti-ghost.player-radius", 8));
        antiGhostAllowed = loadMaterials("anti-ghost.allowed-blocks");
        antiGhostDelays = new ArrayList<>();
        for (Integer delay : getConfig().getIntegerList("anti-ghost.delays")) {
            if (delay != null && delay >= 0 && delay <= 40) {
                antiGhostDelays.add(delay);
            }
        }
        if (antiGhostDelays.isEmpty()) {
            antiGhostDelays.add(1);
            antiGhostDelays.add(3);
        }

        loadEquipmentBonusSettings();
        loadCrossbowAutoReloadSettings();
        loadWeaponSwapLockSettings();
        loadCustomDrops();

        debug("Config cargada. Mining=" + miningAllowed.size() + ", Logs=" + logsAllowed.size() + ", Crops=" + cropsAllowed.size() + ", AntiGhost=" + antiGhostAllowed.size() + ", CustomDrops=" + customDrops.size());
    }

    private void loadEquipmentBonusSettings() {
        equipmentBonusesEnabled = getConfig().getBoolean("equipment-bonuses.enabled", true);
        agricultureRareBonusEnabled = getConfig().getBoolean("equipment-bonuses.agriculture-rare-drops.enabled", true);
        headOreExtraBonusEnabled = getConfig().getBoolean("equipment-bonuses.rare-minerals.enabled", true);
        treeNodeExtraBonusEnabled = getConfig().getBoolean("equipment-bonuses.tree-node-extra.enabled", true);
        maxEquipmentBonusPercent = Math.max(0.0, getConfig().getDouble("equipment-bonuses.max-total-bonus-percent", 100.0));
        headOreExtraAmount = Math.max(1, getConfig().getInt("equipment-bonuses.rare-minerals.extra-amount", 1));
        treeNodeExtraAmount = Math.max(1, getConfig().getInt("equipment-bonuses.tree-node-extra.extra-amount", 1));

        agricultureRareBonusPattern = buildPercentLorePattern(getConfig().getString("equipment-bonuses.lore.agriculture-rare-drops", "Agricultura Drops raros"));
        rareMineralsBonusPattern = buildPercentLorePattern(getConfig().getString("equipment-bonuses.lore.rare-minerals", "Minerales Raros"));
        treeNodeExtraBonusPattern = buildPercentLorePattern(getConfig().getString("equipment-bonuses.lore.tree-node-extra", "Nodos de arbol extra"));

        headOreOreKey = null;
        headOreNodeKey = null;
        headOreDropTypeKey = null;
        headOreDropIdKey = null;

        String pluginName = getConfig().getString("equipment-bonuses.mdvheadores.plugin-name", "MDVHeadOres");
        Plugin headOres = pluginName == null ? null : Bukkit.getPluginManager().getPlugin(pluginName);
        if (headOres != null) {
            headOreOreKey = new NamespacedKey(headOres, "ore_key");
            headOreNodeKey = new NamespacedKey(headOres, "tree_node_key");
            headOreDropTypeKey = new NamespacedKey(headOres, "drop_type");
            headOreDropIdKey = new NamespacedKey(headOres, "drop_id");
        } else if (debug && (headOreExtraBonusEnabled || treeNodeExtraBonusEnabled)) {
            getLogger().info("MDVHeadOres no está cargado todavía. Los bonus de Minerales Raros/Nodos se intentarán al romper bloques si el plugin está disponible.");
        }
    }


    private void loadCrossbowAutoReloadSettings() {
        crossbowAutoReloadEnabled = getConfig().getBoolean("crossbow-auto-reload.enabled", true);
        crossbowOnlyOnEntityHit = getConfig().getBoolean("crossbow-auto-reload.only-on-entity-hit", true);
        crossbowConsumeArrow = getConfig().getBoolean("crossbow-auto-reload.consume-arrow", true);
        crossbowRequirePlayerOnline = getConfig().getBoolean("crossbow-auto-reload.require-player-online", true);
        crossbowCooldownMs = Math.max(0L, getConfig().getLong("crossbow-auto-reload.cooldown-ms", 150L));
        crossbowProjectileTtlTicks = Math.max(20L, getConfig().getLong("crossbow-auto-reload.projectile-ttl-ticks", 400L));

        Material ammo = Material.matchMaterial(getConfig().getString("crossbow-auto-reload.ammo-material", "ARROW"));
        crossbowAmmoMaterial = ammo == null ? Material.ARROW : ammo;

        crossbowLoreKeys = new ArrayList<>();
        for (String raw : getConfig().getStringList("crossbow-auto-reload.lore-lines")) {
            String normalized = normalize(raw);
            if (!normalized.isBlank()) crossbowLoreKeys.add(normalized.toLowerCase(Locale.ROOT));
        }
        if (crossbowLoreKeys.isEmpty()) {
            crossbowLoreKeys.add(normalize("Recarga Automatica al Impacto").toLowerCase(Locale.ROOT));
            crossbowLoreKeys.add(normalize("Recarga Automática al Impacto").toLowerCase(Locale.ROOT));
        }

        crossbowSoundEnabled = getConfig().getBoolean("crossbow-auto-reload.sound.enabled", true);
        crossbowSoundName = getConfig().getString("crossbow-auto-reload.sound.value", "item.crossbow.loading_end");
        crossbowSoundVolume = (float) getConfig().getDouble("crossbow-auto-reload.sound.volume", 0.8);
        crossbowSoundPitch = (float) getConfig().getDouble("crossbow-auto-reload.sound.pitch", 1.3);

        crossbowParticlesEnabled = getConfig().getBoolean("crossbow-auto-reload.particles.enabled", true);
        crossbowParticleName = getConfig().getString("crossbow-auto-reload.particles.particle", "CRIT");
        crossbowParticleAmount = Math.max(0, getConfig().getInt("crossbow-auto-reload.particles.amount", 8));

        if (!crossbowAutoReloadEnabled) {
            autoReloadProjectiles.clear();
            autoReloadCooldowns.clear();
        }
    }


    private void loadWeaponSwapLockSettings() {
        weaponSwapLockEnabled = getConfig().getBoolean("weapon-swap-lock.enabled", true);
        int durationTicks = Math.max(0, getConfig().getInt("weapon-swap-lock.duration-ticks", 40));
        weaponSwapLockDurationMs = durationTicks * 50L;
        weaponSwapLockOnlyWhenNewItemIsWeapon = getConfig().getBoolean("weapon-swap-lock.only-when-new-item-is-weapon", true);
        weaponSwapLockIgnoreReturnToSameWeaponAfterNonWeapon = getConfig().getBoolean("weapon-swap-lock.ignore-return-to-same-weapon-after-non-weapon", true);

        weaponSwapLockMmoTypes = new HashSet<>();
        for (String raw : getConfig().getStringList("weapon-swap-lock.mmoitems-types")) {
            if (raw == null) continue;
            String type = raw.trim().toUpperCase(Locale.ROOT);
            if (!type.isBlank()) weaponSwapLockMmoTypes.add(type);
        }
        if (weaponSwapLockMmoTypes.isEmpty()) {
            weaponSwapLockMmoTypes.addAll(Arrays.asList(
                    "SWORD", "DAGGER", "AXE", "HAMMER", "MACE",
                    "BOW", "CROSSBOW", "STAFF", "WAND", "CATALYST",
                    "SPEAR", "GREATSWORD", "GREATSTAFF", "LUTE"
            ));
        }

        weaponSwapLockFallbackEnabled = getConfig().getBoolean("weapon-swap-lock.fallback-vanilla-materials.enabled", false);
        weaponSwapLockFallbackMaterials = EnumSet.noneOf(Material.class);
        for (String raw : getConfig().getStringList("weapon-swap-lock.fallback-vanilla-materials.materials")) {
            Material material = Material.matchMaterial(raw);
            if (material != null) {
                weaponSwapLockFallbackMaterials.add(material);
            } else if (raw != null && !raw.isBlank()) {
                getLogger().warning("Material inválido en weapon-swap-lock.fallback-vanilla-materials.materials: " + raw);
            }
        }

        weaponSwapLockBlockInteract = getConfig().getBoolean("weapon-swap-lock.block.interact-clicks", true);
        weaponSwapLockBlockMeleeHit = getConfig().getBoolean("weapon-swap-lock.block.melee-hit", true);
        weaponSwapLockBlockBowShoot = getConfig().getBoolean("weapon-swap-lock.block.bow-shoot", true);
        weaponSwapLockBlockMmoItemAbilities = getConfig().getBoolean("weapon-swap-lock.block.mmoitems-abilities", true);
        weaponSwapLockBlockMythicLibSkills = getConfig().getBoolean("weapon-swap-lock.block.mythiclib-skills", true);
        weaponSwapLockBlockedMessage = color(getConfig().getString("weapon-swap-lock.messages.blocked", "&cAún no afirmas bien el arma en tus manos."));
        weaponSwapLockBlockedMessageCooldownMs = Math.max(0L, getConfig().getLong("weapon-swap-lock.messages.cooldown-ms", 800L));

        weaponSwapLockReadyFeedbackEnabled = getConfig().getBoolean("weapon-swap-lock.ready-feedback.enabled", true);
        weaponSwapLockReadySoundName = getConfig().getString("weapon-swap-lock.ready-feedback.sound.value", "block.note_block.hat");
        weaponSwapLockReadySoundVolume = (float) getConfig().getDouble("weapon-swap-lock.ready-feedback.sound.volume", 0.3);
        weaponSwapLockReadySoundPitch = (float) getConfig().getDouble("weapon-swap-lock.ready-feedback.sound.pitch", 1.6);
        weaponSwapLockReadyParticlesEnabled = getConfig().getBoolean("weapon-swap-lock.ready-feedback.particles.enabled", true);
        weaponSwapLockReadyParticleName = getConfig().getString("weapon-swap-lock.ready-feedback.particles.particle", "CRIT");
        weaponSwapLockReadyParticleAmount = Math.max(0, getConfig().getInt("weapon-swap-lock.ready-feedback.particles.amount", 5));

        if (!weaponSwapLockEnabled || weaponSwapLockDurationMs <= 0L) {
            weaponSwapLockUntil.clear();
            weaponSwapLockLastBlockedMessage.clear();
            weaponSwapLastWeaponBeforeNonWeapon.clear();
        }
    }

    private Pattern buildPercentLorePattern(String label) {
        String normalizedLabel = normalize(label);
        if (normalizedLabel.isBlank()) normalizedLabel = "Drops raros";
        return Pattern.compile(Pattern.quote(normalizedLabel) + "\\s*:\\s*\\+?([0-9]+(?:[\\.,][0-9]+)?)\\s*%?", Pattern.CASE_INSENSITIVE);
    }

    private Set<Material> loadMaterials(String path) {
        Set<Material> result = EnumSet.noneOf(Material.class);
        for (String raw : getConfig().getStringList(path)) {
            Material mat = Material.matchMaterial(raw);
            if (mat != null) {
                result.add(mat);
            } else {
                getLogger().warning("Material inválido en " + path + ": " + raw);
            }
        }
        return result;
    }

    private void ensureCustomDropsFile() {
        customDropsFile = new File(getDataFolder(), "custom-drops.yml");
        if (!customDropsFile.exists()) {
            try {
                saveResource("custom-drops.yml", false);
            } catch (IllegalArgumentException ignored) {
                // Si el recurso no existe por alguna razón, se crea desde loadCustomDrops().
            }
        }
    }

    private void loadCustomDrops() {
        ensureCustomDropsFile();
        customDrops.clear();

        if (!customDropsFile.exists()) {
            try {
                getDataFolder().mkdirs();
                customDropsFile.createNewFile();
            } catch (Exception exception) {
                getLogger().warning("No pude crear custom-drops.yml: " + exception.getMessage());
            }
        }

        customDropsConfig = YamlConfiguration.loadConfiguration(customDropsFile);
        customDropsEnabled = customDropsConfig.getBoolean("enabled", true);
        customDropsFallbackCommand = customDropsConfig.getString("default-fallback-command", "mi give %type% %id% %player% %amount%");

        ConfigurationSection section = customDropsConfig.getConfigurationSection("drops");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection cfg = section.getConfigurationSection(key);
            if (cfg == null) continue;

            CustomDropDefinition def = new CustomDropDefinition();
            def.key = key;
            def.enabled = cfg.getBoolean("enabled", true);
            def.matureOnly = cfg.getBoolean("mature-only", cfg.getBoolean("only-mature", true));
            def.chance = Math.max(0.0, cfg.getDouble("chance", 0.0));
            def.amountMin = Math.max(1, cfg.getInt("amount-min", 1));
            def.amountMax = Math.max(def.amountMin, cfg.getInt("amount-max", def.amountMin));
            parseAmount(cfg.get("amount", null), def);
            def.mmoitemsType = cfg.getString("mmoitems-type", cfg.getString("type", "MATERIAL")).toUpperCase(Locale.ROOT);
            def.mmoitemsId = cfg.getString("mmoitems-id", cfg.getString("id", "")).toUpperCase(Locale.ROOT);
            def.dropNaturally = cfg.getBoolean("drop-naturally", true);
            def.fallbackCommand = cfg.getString("fallback-command", customDropsFallbackCommand);
            def.requireToolLore = cfg.getBoolean("require-tool-lore", false);
            def.requiredToolLoreContains = new ArrayList<>();
            for (String raw : cfg.getStringList("required-tool-lore-contains")) {
                String normalized = normalize(raw);
                if (!normalized.isBlank()) def.requiredToolLoreContains.add(normalized);
            }
            def.worlds = new HashSet<>(cfg.getStringList("worlds"));

            for (String raw : cfg.getStringList("blocks")) {
                Material mat = Material.matchMaterial(raw);
                if (mat != null) def.blocks.add(mat);
                else getLogger().warning("Material inválido en custom-drops.yml -> " + key + ".blocks: " + raw);
            }
            String singleBlock = cfg.getString("block", null);
            if (singleBlock != null && !singleBlock.isBlank()) {
                Material mat = Material.matchMaterial(singleBlock);
                if (mat != null) def.blocks.add(mat);
                else getLogger().warning("Material inválido en custom-drops.yml -> " + key + ".block: " + singleBlock);
            }

            if (def.blocks.isEmpty()) {
                getLogger().warning("Drop custom '" + key + "' no tiene block/blocks válidos.");
                continue;
            }
            if (def.mmoitemsId.isBlank()) {
                getLogger().warning("Drop custom '" + key + "' no tiene mmoitems-id.");
                continue;
            }

            customDrops.add(def);
        }
    }

    private void parseAmount(Object raw, CustomDropDefinition def) {
        if (raw == null) return;
        String text = raw.toString().trim();
        if (text.isEmpty()) return;

        try {
            if (text.contains("-")) {
                String[] split = text.split("-", 2);
                int min = Math.max(1, Integer.parseInt(split[0].trim()));
                int max = Math.max(min, Integer.parseInt(split[1].trim()));
                def.amountMin = min;
                def.amountMax = max;
            } else {
                int amount = Math.max(1, Integer.parseInt(text));
                def.amountMin = amount;
                def.amountMax = amount;
            }
        } catch (NumberFormatException ignored) {
            getLogger().warning("Cantidad inválida en custom-drops.yml -> " + def.key + ".amount: " + text);
        }
    }




    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponSwapHotbar(PlayerItemHeldEvent event) {
        if (!weaponSwapLockEnabled || weaponSwapLockDurationMs <= 0L) return;
        Player player = event.getPlayer();
        if (player == null) return;

        PlayerInventory inventory = player.getInventory();
        ItemStack oldItem = inventory.getItem(event.getPreviousSlot());
        ItemStack newItem = inventory.getItem(event.getNewSlot());
        handlePossibleWeaponSwap(player, oldItem, newItem);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponSwapHands(PlayerSwapHandItemsEvent event) {
        if (!weaponSwapLockEnabled || weaponSwapLockDurationMs <= 0L) return;
        // Después del swap, el item de offhand pasa a ser el arma de la mano principal.
        handlePossibleWeaponSwap(event.getPlayer(), event.getMainHandItem(), event.getOffHandItem());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onWeaponSwapLockInteract(PlayerInteractEvent event) {
        if (!weaponSwapLockEnabled || !weaponSwapLockBlockInteract) return;
        Player player = event.getPlayer();
        if (!isWeaponSwapLocked(player)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            item = player.getInventory().getItemInMainHand();
        }
        if (!isWeaponSwapLockWeapon(item)) return;

        event.setCancelled(true);
        sendWeaponSwapLockBlockedFeedback(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onWeaponSwapLockInteractEntity(PlayerInteractEntityEvent event) {
        if (!weaponSwapLockEnabled || !weaponSwapLockBlockInteract) return;
        Player player = event.getPlayer();
        if (!isWeaponSwapLocked(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isWeaponSwapLockWeapon(item)) return;

        event.setCancelled(true);
        sendWeaponSwapLockBlockedFeedback(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onWeaponSwapLockMeleeHit(EntityDamageByEntityEvent event) {
        if (!weaponSwapLockEnabled || !weaponSwapLockBlockMeleeHit) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isWeaponSwapLocked(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isWeaponSwapLockWeapon(item)) return;

        event.setCancelled(true);
        sendWeaponSwapLockBlockedFeedback(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onWeaponSwapLockBowShoot(EntityShootBowEvent event) {
        if (!weaponSwapLockEnabled || !weaponSwapLockBlockBowShoot) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isWeaponSwapLocked(player)) return;

        ItemStack item = event.getBow();
        if (!isWeaponSwapLockWeapon(item)) return;

        event.setCancelled(true);
        sendWeaponSwapLockBlockedFeedback(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeaponSwapLockQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        weaponSwapLockUntil.remove(id);
        weaponSwapLockLastBlockedMessage.remove(id);
        weaponSwapLastWeaponBeforeNonWeapon.remove(id);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAutoReloadShoot(EntityShootBowEvent event) {
        if (!crossbowAutoReloadEnabled) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Projectile projectile)) return;

        ItemStack bow = event.getBow();
        if (!isAutoReloadCrossbow(bow)) return;

        UUID projectileId = projectile.getUniqueId();
        autoReloadProjectiles.put(projectileId, new AutoReloadShot(player.getUniqueId(), System.currentTimeMillis()));
        Bukkit.getScheduler().runTaskLater(this, () -> autoReloadProjectiles.remove(projectileId), crossbowProjectileTtlTicks);
        debug("Proyectil marcado para recarga automática: " + projectileId + " jugador=" + player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAutoReloadProjectileHit(ProjectileHitEvent event) {
        if (!crossbowAutoReloadEnabled) return;
        if (crossbowOnlyOnEntityHit && event.getHitEntity() == null) return;
        if (!(event.getEntity() instanceof Projectile projectile)) return;

        AutoReloadShot shot = autoReloadProjectiles.remove(projectile.getUniqueId());
        if (shot == null) return;

        Player player = Bukkit.getPlayer(shot.playerId);
        if (player == null || !player.isOnline()) {
            if (crossbowRequirePlayerOnline) return;
        }
        if (player == null) return;

        if (crossbowCooldownMs > 0) {
            long now = System.currentTimeMillis();
            long last = autoReloadCooldowns.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < crossbowCooldownMs) return;
            autoReloadCooldowns.put(player.getUniqueId(), now);
        }

        ItemStack crossbow = findHeldAutoReloadCrossbow(player);
        if (crossbow == null) {
            debug("No se encontró ballesta válida en mano para recargar: " + player.getName());
            return;
        }

        if (crossbowConsumeArrow && !consumeOne(player.getInventory(), crossbowAmmoMaterial)) {
            debug("Sin munición para recarga automática: " + player.getName());
            return;
        }

        if (!chargeCrossbow(crossbow, crossbowAmmoMaterial)) {
            debug("No pude cargar la ballesta de " + player.getName());
            return;
        }

        playAutoReloadFeedback(player);
        player.updateInventory();
        debug("Ballesta recargada automáticamente para " + player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonitorCustomDrops(BlockBreakEvent event) {
        if (internalBreakEvent || !customDropsEnabled || customDrops.isEmpty()) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();
        rollCustomDrops(player, block, tool);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonitorHeadOreExtraDrops(BlockBreakEvent event) {
        if (internalBreakEvent || !equipmentBonusesEnabled) return;
        if (!headOreExtraBonusEnabled && !treeNodeExtraBonusEnabled) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Block block = event.getBlock();
        Material type = block.getType();
        if (type != Material.PLAYER_HEAD && type != Material.PLAYER_WALL_HEAD) return;

        ensureHeadOreKeys();
        if (headOreOreKey == null || headOreNodeKey == null || headOreDropTypeKey == null || headOreDropIdKey == null) return;

        BlockState state = block.getState();
        if (!(state instanceof TileState tileState)) return;

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        String oreName = pdc.get(headOreOreKey, PersistentDataType.STRING);
        String nodeName = pdc.get(headOreNodeKey, PersistentDataType.STRING);
        if (oreName == null && nodeName == null) return;

        String dropType = pdc.get(headOreDropTypeKey, PersistentDataType.STRING);
        String dropId = pdc.get(headOreDropIdKey, PersistentDataType.STRING);
        if (dropType == null || dropType.isBlank() || dropId == null || dropId.isBlank()) return;

        EquipmentBonuses bonuses = readEquipmentBonuses(player);
        boolean isNode = nodeName != null;
        double chance = isNode ? bonuses.treeNodeExtra : bonuses.rareMinerals;
        if (isNode && !treeNodeExtraBonusEnabled) return;
        if (!isNode && !headOreExtraBonusEnabled) return;
        if (chance <= 0.0) return;

        if (ThreadLocalRandom.current().nextDouble(100.0) >= chance) return;

        int amount = isNode ? treeNodeExtraAmount : headOreExtraAmount;
        dropExtraMmoItem(dropType, dropId, player, block, amount, isNode ? "nodo" : "mineral");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonitorBreak(BlockBreakEvent event) {
        if (internalBreakEvent || !antiGhostEnabled) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;
        if (!isPickaxe(tool.getType()) && !isAxe(tool.getType()) && !tool.getType().name().endsWith("_HOE")) return;

        if (antiGhostOnlyMdvLore && !readLore(tool).hasAny()) return;

        Block block = event.getBlock();
        if (!antiGhostAllowed.isEmpty() && !antiGhostAllowed.contains(block.getType())) return;

        scheduleAntiGhostRefresh(player, block);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (internalBreakEvent) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

        ToolLore lore = readLore(tool);
        if (!lore.hasAny()) return;

        Block block = event.getBlock();
        Material type = block.getType();

        if (farmingEnabled && lore.multiCosecha > 0 && cropsAllowed.contains(type) && isMatureCrop(block)) {
            handleCropBreak(event, player, tool, lore);
            return;
        }

        if (miningEnabled && lore.roturaMultiple > 0 && miningAllowed.contains(type) && isPickaxe(tool.getType())) {
            int extra = Math.min(lore.roturaMultiple, maxMiningExtra);
            int broken = breakLine(player, block, tool, extra, miningAllowed);
            damageTool(player, tool, broken * durabilityCostBlock);
            debug("Rotura múltiple: " + broken + " extra.");
            return;
        }

        if (woodcuttingEnabled && lore.talaMultiple > 0 && logsAllowed.contains(type) && isAxe(tool.getType())) {
            int extra = Math.min(lore.talaMultiple, maxWoodExtra);
            int broken = breakConnected(player, block, tool, extra, logsAllowed);
            damageTool(player, tool, broken * durabilityCostBlock);
            debug("Tala múltiple: " + broken + " extra.");
        }
    }

    private void handleCropBreak(BlockBreakEvent event, Player player, ItemStack tool, ToolLore lore) {
        Block original = event.getBlock();
        int extra = Math.min(lore.multiCosecha, maxCropExtra);

        List<Block> crops = new ArrayList<>();
        crops.add(original);
        crops.addAll(findAdjacentMatureCrops(original, extra));

        if (handleOriginalCrop) {
            event.setCancelled(true);
        } else {
            crops.remove(original);
        }

        int harvested = 0;
        for (Block crop : crops) {
            if (!crop.equals(original) && !canBreakExtraBlock(player, crop)) continue;
            if (!cropsAllowed.contains(crop.getType()) || !isMatureCrop(crop)) continue;

            harvestCrop(crop, player, tool, lore.autoReplantar);
            harvested++;
        }

        int extraHarvested = handleOriginalCrop ? Math.max(0, harvested - 1) : harvested;
        damageTool(player, tool, extraHarvested * durabilityCostCrop);
        debug("Multi cosecha: " + harvested + " cultivos.");
    }

    private int breakLine(Player player, Block original, ItemStack tool, int extra, Set<Material> allowed) {
        if (extra <= 0) return 0;

        BlockFace face = dominantFace(player.getEyeLocation().getDirection());
        int broken = 0;
        Block current = original;

        for (int i = 0; i < extra; i++) {
            current = current.getRelative(face);
            if (!allowed.contains(current.getType())) break;

            if (breakExtraNaturally(player, current, tool)) {
                broken++;
            } else {
                break;
            }
        }

        return broken;
    }

    private int breakConnected(Player player, Block original, ItemStack tool, int extra, Set<Material> allowed) {
        if (extra <= 0) return 0;

        int broken = 0;
        Set<String> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();

        visited.add(key(original));
        addNeighbors(original, queue, visited);

        while (!queue.isEmpty() && broken < extra) {
            Block block = queue.poll();
            if (!allowed.contains(block.getType())) continue;

            if (breakExtraNaturally(player, block, tool)) {
                broken++;
                addNeighbors(block, queue, visited);
            }
        }

        return broken;
    }

    private List<Block> findAdjacentMatureCrops(Block original, int extra) {
        List<Block> result = new ArrayList<>();
        if (extra <= 0) return result;

        Material cropType = original.getType();
        Set<String> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();

        visited.add(key(original));
        for (BlockFace face : HORIZONTAL_FACES) {
            Block next = original.getRelative(face);
            visited.add(key(next));
            queue.add(next);
        }

        while (!queue.isEmpty() && result.size() < extra) {
            Block block = queue.poll();
            if (block.getType() == cropType && isMatureCrop(block)) {
                result.add(block);
                for (BlockFace face : HORIZONTAL_FACES) {
                    Block next = block.getRelative(face);
                    String k = key(next);
                    if (!visited.contains(k)) {
                        visited.add(k);
                        queue.add(next);
                    }
                }
            }
        }

        return result;
    }

    private void addNeighbors(Block block, Queue<Block> queue, Set<String> visited) {
        for (BlockFace face : CONNECTED_FACES) {
            Block next = block.getRelative(face);
            String k = key(next);
            if (!visited.contains(k)) {
                visited.add(k);
                queue.add(next);
            }
        }
    }


    private void scheduleAntiGhostRefresh(Player player, Block original) {
        if (!antiGhostEnabled || original == null || original.getWorld() == null) return;

        World world = original.getWorld();
        int x = original.getX();
        int y = original.getY();
        int z = original.getZ();

        for (int delay : antiGhostDelays) {
            Bukkit.getScheduler().runTaskLater(this, () -> refreshRealBlocks(player, world, x, y, z), delay);
        }
    }

    private void refreshRealBlocks(Player breaker, World world, int x, int y, int z) {
        if (world == null || breaker == null || !breaker.isOnline()) return;

        List<Block> blocks = new ArrayList<>();
        Block center = world.getBlockAt(x, y, z);
        blocks.add(center);

        if (antiGhostUpdateNeighbors) {
            for (BlockFace face : CONNECTED_FACES) {
                blocks.add(center.getRelative(face));
            }
        }

        if (antiGhostUpdateNearbyPlayers) {
            double radiusSquared = antiGhostPlayerRadius * antiGhostPlayerRadius;
            for (Player viewer : world.getPlayers()) {
                if (viewer.getLocation().distanceSquared(center.getLocation()) <= radiusSquared) {
                    sendRealBlocks(viewer, blocks);
                }
            }
        } else {
            sendRealBlocks(breaker, blocks);
        }
    }

    private void sendRealBlocks(Player viewer, List<Block> blocks) {
        if (viewer == null || !viewer.isOnline()) return;
        for (Block block : blocks) {
            try {
                viewer.sendBlockChange(block.getLocation(), block.getBlockData());
            } catch (Throwable ignored) {
                // Si una versión futura cambia la API, evitamos romper el evento.
            }
        }
    }

    private boolean canBreakExtraBlock(Player player, Block block) {
        BlockBreakEvent extraEvent = new BlockBreakEvent(block, player);
        try {
            internalBreakEvent = true;
            Bukkit.getPluginManager().callEvent(extraEvent);
        } finally {
            internalBreakEvent = false;
        }
        return !extraEvent.isCancelled();
    }

    private boolean breakExtraNaturally(Player player, Block block, ItemStack tool) {
        if (!canBreakExtraBlock(player, block)) return false;
        boolean broken = block.breakNaturally(tool);
        if (broken) {
            scheduleAntiGhostRefresh(player, block);
        }
        return broken;
    }

    private void harvestCrop(Block block, Player player, ItemStack tool, boolean autoReplant) {
        Material cropType = block.getType();
        World world = block.getWorld();
        Collection<ItemStack> drops = new ArrayList<>(block.getDrops(tool));
        rollCustomDrops(player, block, tool);

        if (autoReplant) {
            Material seed = seedForCrop(cropType);
            boolean consumedSeed = false;
            if (seed != null) consumedSeed = consumeOne(drops, seed);

            if (!replantNeedsSeed || consumedSeed) {
                dropItems(world, block, drops);
                block.setType(cropType, false);
                if (block.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(0);
                    block.setBlockData(ageable, false);
                }
                return;
            }
        }

        dropItems(world, block, drops);
        block.setType(Material.AIR, true);
    }

    private void rollCustomDrops(Player player, Block block, ItemStack tool) {
        if (!customDropsEnabled || customDrops.isEmpty() || player == null || block == null) return;

        EquipmentBonuses bonuses = equipmentBonusesEnabled && agricultureRareBonusEnabled ? readEquipmentBonuses(player) : EquipmentBonuses.EMPTY;

        for (CustomDropDefinition def : customDrops) {
            if (!matchesCustomDrop(def, player, block, tool)) continue;

            double effectiveChance = def.chance;
            if (agricultureRareBonusEnabled && isAgricultureCustomDrop(block)) {
                effectiveChance = applyRelativeBonus(effectiveChance, bonuses.agricultureRareDrops);
            }

            if (ThreadLocalRandom.current().nextDouble(100.0) >= effectiveChance) continue;

            int amount = def.amountMin;
            if (def.amountMax > def.amountMin) {
                amount = ThreadLocalRandom.current().nextInt(def.amountMin, def.amountMax + 1);
            }
            dropCustomMmoItem(def, player, block, amount);
        }
    }

    private boolean matchesCustomDrop(CustomDropDefinition def, Player player, Block block, ItemStack tool) {
        if (def == null || !def.enabled) return false;
        if (!def.blocks.contains(block.getType())) return false;
        if (!def.worlds.isEmpty() && !def.worlds.contains(block.getWorld().getName())) return false;
        if (def.matureOnly && !isMatureCrop(block)) return false;

        if (def.requireToolLore) {
            if (tool == null || tool.getType() == Material.AIR) return false;

            if (def.requiredToolLoreContains.isEmpty()) {
                if (!readLore(tool).hasAny()) return false;
            } else {
                ItemMeta meta = tool.getItemMeta();
                if (meta == null || !meta.hasLore() || meta.getLore() == null) return false;
                String joinedLore = normalize(String.join(" ", meta.getLore()));
                for (String required : def.requiredToolLoreContains) {
                    if (!joinedLore.contains(required)) return false;
                }
            }
        }

        return true;
    }

    private boolean isAgricultureCustomDrop(Block block) {
        if (block == null) return false;
        if (cropsAllowed.contains(block.getType())) return true;
        return block.getBlockData() instanceof Ageable;
    }

    private double applyRelativeBonus(double baseChance, double bonusPercent) {
        if (baseChance <= 0.0 || bonusPercent <= 0.0) return baseChance;
        double result = baseChance * (1.0 + (bonusPercent / 100.0));
        return Math.max(0.0, Math.min(100.0, result));
    }

    private void ensureHeadOreKeys() {
        if (headOreOreKey != null && headOreNodeKey != null && headOreDropTypeKey != null && headOreDropIdKey != null) return;
        String pluginName = getConfig().getString("equipment-bonuses.mdvheadores.plugin-name", "MDVHeadOres");
        Plugin headOres = pluginName == null ? null : Bukkit.getPluginManager().getPlugin(pluginName);
        if (headOres == null) return;
        headOreOreKey = new NamespacedKey(headOres, "ore_key");
        headOreNodeKey = new NamespacedKey(headOres, "tree_node_key");
        headOreDropTypeKey = new NamespacedKey(headOres, "drop_type");
        headOreDropIdKey = new NamespacedKey(headOres, "drop_id");
    }

    private void dropExtraMmoItem(String typeId, String itemId, Player player, Block block, int amount, String reason) {
        amount = Math.max(1, amount);
        ItemStack stack = buildMmoItemStack(typeId, itemId, amount);
        if (stack != null && stack.getType() != Material.AIR) {
            Location location = block.getLocation().add(0.5, 0.55, 0.5);
            Item item = block.getWorld().dropItemNaturally(location, stack);
            item.setPickupDelay(10);
            debug("Drop extra por lore (" + reason + "): " + typeId + ":" + itemId + " x" + amount);
            return;
        }

        String command = customDropsFallbackCommand;
        if (command == null || command.isBlank()) command = "mi give %type% %id% %player% %amount%";
        command = command
                .replace("%player%", player.getName())
                .replace("%world%", block.getWorld().getName())
                .replace("%x%", Integer.toString(block.getX()))
                .replace("%y%", Integer.toString(block.getY()))
                .replace("%z%", Integer.toString(block.getZ()))
                .replace("%drop%", reason)
                .replace("%type%", typeId)
                .replace("%id%", itemId)
                .replace("%amount%", Integer.toString(amount));
        if (command.startsWith("/")) command = command.substring(1);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        debug("Fallback drop extra por lore usado para " + typeId + ":" + itemId);
    }

    private void dropCustomMmoItem(CustomDropDefinition def, Player player, Block block, int amount) {
        amount = Math.max(1, amount);

        if (def.dropNaturally) {
            ItemStack stack = buildMmoItemStack(def.mmoitemsType, def.mmoitemsId, amount);
            if (stack != null && stack.getType() != Material.AIR) {
                Location location = block.getLocation().add(0.5, 0.55, 0.5);
                Item item = block.getWorld().dropItemNaturally(location, stack);
                item.setPickupDelay(10);
                debug("Drop custom: " + def.mmoitemsType + ":" + def.mmoitemsId + " x" + amount + " en " + block.getType());
                return;
            }
        }

        runCustomDropFallback(def, player, block, amount);
    }

    private void runCustomDropFallback(CustomDropDefinition def, Player player, Block block, int amount) {
        String command = def.fallbackCommand;
        if (command == null || command.isBlank()) command = customDropsFallbackCommand;
        if (command == null || command.isBlank()) return;

        command = command
                .replace("%player%", player.getName())
                .replace("%world%", block.getWorld().getName())
                .replace("%x%", Integer.toString(block.getX()))
                .replace("%y%", Integer.toString(block.getY()))
                .replace("%z%", Integer.toString(block.getZ()))
                .replace("%drop%", def.key)
                .replace("%type%", def.mmoitemsType)
                .replace("%id%", def.mmoitemsId)
                .replace("%amount%", Integer.toString(amount));

        if (command.startsWith("/")) command = command.substring(1);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        debug("Fallback custom-drop usado para " + def.mmoitemsType + ":" + def.mmoitemsId);
    }

    private ItemStack buildMmoItemStack(String typeId, String itemId, int amount) {
        try {
            Class<?> mmoItemsClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
            Object plugin = getStaticField(mmoItemsClass, "plugin");
            if (plugin == null) return null;

            Class<?> typeClass = Class.forName("net.Indyuce.mmoitems.api.Type");
            Object type = getMmoItemsType(typeClass, typeId);
            if (type == null) {
                if (debug) getLogger().warning("Tipo MMOItems no encontrado: " + typeId);
                return null;
            }

            ItemStack direct = tryInvokeItemStack(plugin, "getItem", type, itemId);
            if (direct != null) {
                direct.setAmount(Math.max(1, amount));
                return direct;
            }

            Object mmoItem = tryInvokeObject(plugin, "getMMOItem", type, itemId);
            if (mmoItem != null) {
                ItemStack built = buildFromMmoItemObject(mmoItem);
                if (built != null) {
                    built.setAmount(Math.max(1, amount));
                    return built;
                }
            }
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("Error creando item MMOItems " + typeId + ":" + itemId + " -> " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
        return null;
    }

    private Object getMmoItemsType(Class<?> typeClass, String typeId) {
        try {
            Method get = typeClass.getMethod("get", String.class);
            return get.invoke(null, typeId);
        } catch (Throwable ignored) {
        }

        try {
            Method valueOf = typeClass.getMethod("valueOf", String.class);
            return valueOf.invoke(null, typeId.toUpperCase(Locale.ROOT));
        } catch (Throwable ignored) {
        }
        return null;
    }

    private ItemStack tryInvokeItemStack(Object target, String methodName, Object type, String itemId) {
        Object result = tryInvokeObject(target, methodName, type, itemId);
        if (result instanceof ItemStack stack) return stack.clone();
        return null;
    }

    private Object tryInvokeObject(Object target, String methodName, Object type, String itemId) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (method.getParameterCount() != 2) continue;
            try {
                return method.invoke(target, type, itemId);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private ItemStack buildFromMmoItemObject(Object mmoItem) {
        try {
            Method newBuilder = mmoItem.getClass().getMethod("newBuilder");
            Object builder = newBuilder.invoke(mmoItem);
            if (builder == null) return null;
            Method build = builder.getClass().getMethod("build");
            Object result = build.invoke(builder);
            if (result instanceof ItemStack stack) return stack.clone();
        } catch (Throwable ignored) {
        }

        try {
            Method build = mmoItem.getClass().getMethod("build");
            Object result = build.invoke(mmoItem);
            if (result instanceof ItemStack stack) return stack.clone();
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object getStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }




    @SuppressWarnings("unchecked")
    private void registerWeaponSwapLockExternalEvents() {
        if (!weaponSwapLockEnabled) return;

        // MMOItems dispara este evento para habilidades de item. Cancelarlo evita que la habilidad se castee
        // aunque PlayerInteractEvent ya haya sido cancelado.
        if (weaponSwapLockBlockMmoItemAbilities) {
            registerWeaponSwapLockExternalEvent("net.Indyuce.mmoitems.api.event.AbilityUseEvent");
        }

        // MythicLib centraliza skills de MMOItems/MMOCore desde versiones modernas.
        if (weaponSwapLockBlockMythicLibSkills) {
            registerWeaponSwapLockExternalEvent("io.lumine.mythic.lib.api.event.skill.PlayerCastSkillEvent");
        }
    }

    @SuppressWarnings("unchecked")
    private void registerWeaponSwapLockExternalEvent(String className) {
        if (weaponSwapLockHookedEvents.contains(className)) return;

        try {
            Class<?> rawClass = Class.forName(className);
            if (!Event.class.isAssignableFrom(rawClass)) {
                if (debug) getLogger().warning("No registré " + className + " porque no extiende Bukkit Event.");
                return;
            }

            Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
            EventExecutor executor = (listener, event) -> {
                try {
                    handleWeaponSwapLockExternalAbilityEvent(event);
                } catch (Throwable throwable) {
                    if (debug) {
                        getLogger().warning("Error revisando weapon-swap-lock en " + event.getEventName() + ": " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
                    }
                }
            };

            Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.LOWEST, executor, this, false);
            weaponSwapLockHookedEvents.add(className);
            debug("weapon-swap-lock conectado a " + className);
        } catch (ClassNotFoundException ignored) {
            debug("weapon-swap-lock: evento externo no encontrado: " + className);
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("No pude registrar weapon-swap-lock para " + className + ": " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private void handleWeaponSwapLockExternalAbilityEvent(Event event) {
        if (!weaponSwapLockEnabled) return;
        if (!(event instanceof Cancellable cancellable)) return;
        if (cancellable.isCancelled()) return;

        Player player = extractPlayerFromExternalEvent(event);
        if (player == null) return;
        if (!isWeaponSwapLocked(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isWeaponSwapLockWeapon(item)) return;

        cancellable.setCancelled(true);
        sendWeaponSwapLockBlockedFeedback(player);
    }

    private Player extractPlayerFromExternalEvent(Event event) {
        if (event == null) return null;

        // Muchos eventos de Bukkit/ML heredan o exponen getPlayer().
        try {
            Method method = event.getClass().getMethod("getPlayer");
            Object value = method.invoke(event);
            if (value instanceof Player player) return player;
        } catch (Throwable ignored) {
        }

        // Fallback para APIs que usan getCaster().
        try {
            Method method = event.getClass().getMethod("getCaster");
            Object value = method.invoke(event);
            Player player = extractPlayerFromUnknownObject(value);
            if (player != null) return player;
        } catch (Throwable ignored) {
        }

        // Fallback para APIs que usan getPlayerData().getPlayer().
        try {
            Method method = event.getClass().getMethod("getPlayerData");
            Object value = method.invoke(event);
            Player player = extractPlayerFromUnknownObject(value);
            if (player != null) return player;
        } catch (Throwable ignored) {
        }

        return null;
    }

    private Player extractPlayerFromUnknownObject(Object object) {
        if (object == null) return null;
        if (object instanceof Player player) return player;

        for (String methodName : new String[]{"getPlayer", "getBukkitPlayer", "getEntity"}) {
            try {
                Method method = object.getClass().getMethod(methodName);
                Object value = method.invoke(object);
                if (value instanceof Player player) return player;
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private void handlePossibleWeaponSwap(Player player, ItemStack oldItem, ItemStack newItem) {
        if (player == null) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        UUID id = player.getUniqueId();
        boolean oldIsWeapon = isWeaponSwapLockWeapon(oldItem);
        boolean newIsWeapon = isWeaponSwapLockWeapon(newItem);

        // Caso importante para MDVCRAFT: arma -> consumible/poción -> misma arma.
        // Se guarda el arma al salir a un item que no es arma, y si vuelve a la misma no se aplica lock nuevo.
        if (oldIsWeapon && !newIsWeapon) {
            if (weaponSwapLockIgnoreReturnToSameWeaponAfterNonWeapon) {
                WeaponSwapItemIdentity oldIdentity = getWeaponSwapItemIdentity(oldItem);
                if (oldIdentity != null) {
                    weaponSwapLastWeaponBeforeNonWeapon.put(id, oldIdentity);
                }
            }
            return;
        }

        if (!oldIsWeapon && !newIsWeapon) return;

        if (newIsWeapon) {
            if (weaponSwapLockIgnoreReturnToSameWeaponAfterNonWeapon && !oldIsWeapon) {
                WeaponSwapItemIdentity previousWeapon = weaponSwapLastWeaponBeforeNonWeapon.get(id);
                WeaponSwapItemIdentity newIdentity = getWeaponSwapItemIdentity(newItem);
                if (previousWeapon != null && previousWeapon.equals(newIdentity)) {
                    weaponSwapLastWeaponBeforeNonWeapon.remove(id);
                    debug("weapon-swap-lock omitido: retorno a la misma arma tras item no arma: " + player.getName());
                    return;
                }
            }
            weaponSwapLastWeaponBeforeNonWeapon.remove(id);
        }

        if (weaponSwapLockOnlyWhenNewItemIsWeapon && !newIsWeapon) return;
        if (!weaponSwapLockOnlyWhenNewItemIsWeapon && !newIsWeapon && !oldIsWeapon) return;

        applyWeaponSwapLock(player);
    }

    private WeaponSwapItemIdentity getWeaponSwapItemIdentity(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return null;

        String mmoType = readMmoItemTypeId(item);
        if (mmoType != null && weaponSwapLockMmoTypes.contains(mmoType.toUpperCase(Locale.ROOT))) {
            String mmoId = readMmoItemString(item, "MMOITEMS_ITEM_ID");
            if (mmoId == null || mmoId.isBlank()) {
                mmoId = fallbackItemSignature(item);
            }
            return new WeaponSwapItemIdentity("MMOITEMS", mmoType.toUpperCase(Locale.ROOT), mmoId);
        }

        if (weaponSwapLockFallbackEnabled && weaponSwapLockFallbackMaterials.contains(item.getType())) {
            return new WeaponSwapItemIdentity("VANILLA", item.getType().name(), fallbackItemSignature(item));
        }

        return null;
    }

    private String fallbackItemSignature(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "AIR";
        ItemMeta meta = item.getItemMeta();
        String display = "";
        int customModelData = 0;
        if (meta != null) {
            if (meta.hasDisplayName()) display = ChatColor.stripColor(meta.getDisplayName());
            if (meta.hasCustomModelData()) customModelData = meta.getCustomModelData();
        }
        return item.getType().name() + "|" + normalize(display) + "|" + customModelData;
    }

    private void applyWeaponSwapLock(Player player) {
        long until = System.currentTimeMillis() + weaponSwapLockDurationMs;
        UUID id = player.getUniqueId();
        weaponSwapLockUntil.put(id, until);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null || !online.isOnline()) return;
            Long currentUntil = weaponSwapLockUntil.get(id);
            if (currentUntil == null) return;

            // Si el jugador volvió a cambiar de arma, este aviso viejo ya no corresponde.
            if (!Objects.equals(currentUntil, until)) return;

            endWeaponSwapLock(online, true);
        }, Math.max(1L, weaponSwapLockDurationMs / 50L));
    }

    private boolean isWeaponSwapLocked(Player player) {
        if (player == null) return false;
        Long until = weaponSwapLockUntil.get(player.getUniqueId());
        if (until == null) return false;

        if (System.currentTimeMillis() >= until) {
            endWeaponSwapLock(player, true);
            return false;
        }
        return true;
    }

    private void endWeaponSwapLock(Player player, boolean feedback) {
        if (player == null) return;
        weaponSwapLockUntil.remove(player.getUniqueId());
        if (feedback) {
            playWeaponSwapReadyFeedback(player);
        }
    }

    private void sendWeaponSwapLockBlockedFeedback(Player player) {
        if (player == null || weaponSwapLockBlockedMessage == null || weaponSwapLockBlockedMessage.isBlank()) return;
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        long last = weaponSwapLockLastBlockedMessage.getOrDefault(id, 0L);
        if (weaponSwapLockBlockedMessageCooldownMs > 0L && now - last < weaponSwapLockBlockedMessageCooldownMs) return;
        weaponSwapLockLastBlockedMessage.put(id, now);
        player.sendMessage(prefix + weaponSwapLockBlockedMessage);
    }

    private void playWeaponSwapReadyFeedback(Player player) {
        if (!weaponSwapLockReadyFeedbackEnabled || player == null || !player.isOnline()) return;

        if (weaponSwapLockReadySoundName != null && !weaponSwapLockReadySoundName.isBlank()) {
            try {
                player.playSound(player.getLocation(), weaponSwapLockReadySoundName, weaponSwapLockReadySoundVolume, weaponSwapLockReadySoundPitch);
            } catch (Throwable ignored) {
                try {
                    Sound sound = Sound.valueOf(weaponSwapLockReadySoundName.toUpperCase(Locale.ROOT).replace('.', '_'));
                    player.playSound(player.getLocation(), sound, weaponSwapLockReadySoundVolume, weaponSwapLockReadySoundPitch);
                } catch (Throwable ignoredAgain) {
                    if (debug) getLogger().warning("Sonido inválido para weapon-swap-lock: " + weaponSwapLockReadySoundName);
                }
            }
        }

        if (weaponSwapLockReadyParticlesEnabled && weaponSwapLockReadyParticleAmount > 0 && weaponSwapLockReadyParticleName != null && !weaponSwapLockReadyParticleName.isBlank()) {
            try {
                Particle particle = Particle.valueOf(weaponSwapLockReadyParticleName.toUpperCase(Locale.ROOT));
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.0, 0), weaponSwapLockReadyParticleAmount, 0.18, 0.22, 0.18, 0.01);
            } catch (Throwable ignored) {
                if (debug) getLogger().warning("Partícula inválida para weapon-swap-lock: " + weaponSwapLockReadyParticleName);
            }
        }
    }

    private boolean isWeaponSwapLockWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return false;

        String mmoType = readMmoItemTypeId(item);
        if (mmoType != null && weaponSwapLockMmoTypes.contains(mmoType.toUpperCase(Locale.ROOT))) {
            return true;
        }

        return weaponSwapLockFallbackEnabled && weaponSwapLockFallbackMaterials.contains(item.getType());
    }

    private String readMmoItemTypeId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (!ensureMmoNbtReflection()) return null;

        try {
            Object nbt = mmoNbtGetMethod.invoke(null, item);
            if (nbt == null) return null;
            Object hasType = mmoNbtHasTypeMethod.invoke(nbt);
            if (!(hasType instanceof Boolean) || !((Boolean) hasType)) return null;

            Object typeObject = mmoNbtGetTypeMethod.invoke(nbt);
            return extractTypeId(typeObject);
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("No pude leer tipo MMOItems del item: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return null;
        }
    }

    private String readMmoItemString(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR || key == null || key.isBlank()) return null;
        if (!ensureMmoNbtReflection() || mmoNbtGetStringMethod == null) return null;

        try {
            Object nbt = mmoNbtGetMethod.invoke(null, item);
            if (nbt == null) return null;
            Object value = mmoNbtGetStringMethod.invoke(nbt, key);
            if (value == null) return null;
            String string = String.valueOf(value).trim();
            return string.isBlank() ? null : string.toUpperCase(Locale.ROOT);
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("No pude leer NBT MMOItems " + key + ": " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return null;
        }
    }

    private boolean ensureMmoNbtReflection() {
        if (mmoNbtReflectionTried) return mmoNbtItemClass != null;
        mmoNbtReflectionTried = true;

        String[] classNames = new String[]{
                "net.Indyuce.mmoitems.api.item.NBTItem",
                "io.lumine.mythic.lib.api.item.NBTItem"
        };

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                Method get = clazz.getMethod("get", ItemStack.class);
                Method hasType = clazz.getMethod("hasType");
                Method getType = clazz.getMethod("getType");
                Method getString = null;
                try {
                    getString = clazz.getMethod("getString", String.class);
                } catch (Throwable ignored) {
                    // Algunas versiones exponen el tipo sin permitir leer strings arbitrarios.
                }

                mmoNbtItemClass = clazz;
                mmoNbtGetMethod = get;
                mmoNbtHasTypeMethod = hasType;
                mmoNbtGetTypeMethod = getType;
                mmoNbtGetStringMethod = getString;
                debug("NBTItem de MMOItems detectado: " + className);
                return true;
            } catch (Throwable ignored) {
            }
        }

        if (debug) getLogger().warning("No encontré NBTItem de MMOItems/MythicLib. weapon-swap-lock usará solo fallback-vanilla-materials si está activado.");
        return false;
    }

    private String extractTypeId(Object typeObject) {
        if (typeObject == null) return null;
        if (typeObject instanceof String string) return string.trim().toUpperCase(Locale.ROOT);

        for (String methodName : new String[]{"getId", "getName", "name"}) {
            try {
                Method method = typeObject.getClass().getMethod(methodName);
                Object value = method.invoke(typeObject);
                if (value != null) {
                    String text = value.toString().trim();
                    if (!text.isBlank()) return text.toUpperCase(Locale.ROOT);
                }
            } catch (Throwable ignored) {
            }
        }

        String text = typeObject.toString().trim();
        return text.isBlank() ? null : text.toUpperCase(Locale.ROOT);
    }

    private boolean isAutoReloadCrossbow(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return false;

        for (String line : meta.getLore()) {
            String clean = normalize(line).toLowerCase(Locale.ROOT);
            for (String required : crossbowLoreKeys) {
                if (clean.contains(required)) return true;
            }
        }
        return false;
    }

    private ItemStack findHeldAutoReloadCrossbow(Player player) {
        if (player == null) return null;
        PlayerInventory inventory = player.getInventory();
        ItemStack main = inventory.getItemInMainHand();
        if (isAutoReloadCrossbow(main)) return main;
        ItemStack off = inventory.getItemInOffHand();
        if (isAutoReloadCrossbow(off)) return off;
        return null;
    }

    private boolean chargeCrossbow(ItemStack crossbow, Material ammoMaterial) {
        if (crossbow == null || crossbow.getType() != Material.CROSSBOW) return false;
        ItemMeta meta = crossbow.getItemMeta();
        if (!(meta instanceof CrossbowMeta crossbowMeta)) return false;

        try {
            crossbowMeta.setChargedProjectiles(new ArrayList<>());
            crossbowMeta.addChargedProjectile(new ItemStack(ammoMaterial, 1));
            crossbow.setItemMeta(crossbowMeta);
            return true;
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("Error recargando ballesta: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return false;
        }
    }

    private boolean consumeOne(PlayerInventory inventory, Material material) {
        if (inventory == null || material == null || material == Material.AIR) return false;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() != material || item.getAmount() <= 0) continue;
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) inventory.setItem(slot, null);
            return true;
        }
        return false;
    }

    private void playAutoReloadFeedback(Player player) {
        if (player == null || !player.isOnline()) return;
        if (crossbowSoundEnabled && crossbowSoundName != null && !crossbowSoundName.isBlank()) {
            try {
                player.playSound(player.getLocation(), crossbowSoundName, crossbowSoundVolume, crossbowSoundPitch);
            } catch (Throwable ignored) {
                try {
                    Sound sound = Sound.valueOf(crossbowSoundName.toUpperCase(Locale.ROOT).replace('.', '_'));
                    player.playSound(player.getLocation(), sound, crossbowSoundVolume, crossbowSoundPitch);
                } catch (Throwable ignoredAgain) {
                    if (debug) getLogger().warning("Sonido inválido para crossbow-auto-reload: " + crossbowSoundName);
                }
            }
        }

        if (crossbowParticlesEnabled && crossbowParticleAmount > 0 && crossbowParticleName != null && !crossbowParticleName.isBlank()) {
            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(crossbowParticleName.toUpperCase(Locale.ROOT));
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.1, 0), crossbowParticleAmount, 0.25, 0.25, 0.25, 0.02);
            } catch (Throwable ignored) {
                if (debug) getLogger().warning("Partícula inválida para crossbow-auto-reload: " + crossbowParticleName);
            }
        }
    }

    private boolean consumeOne(Collection<ItemStack> drops, Material mat) {
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() == mat && drop.getAmount() > 0) {
                drop.setAmount(drop.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    private void dropItems(World world, Block block, Collection<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) continue;
            world.dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
    }

    private Material seedForCrop(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> null;
        };
    }

    private boolean isMatureCrop(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return false;
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private void damageTool(Player player, ItemStack tool, int amount) {
        if (!durabilityEnabled || amount <= 0) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return;

        int realDamage = 0;
        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);

        for (int i = 0; i < amount; i++) {
            if (respectUnbreaking && unbreaking > 0) {
                if (Math.random() >= (1.0 / (unbreaking + 1.0))) continue;
            }
            realDamage++;
        }

        if (realDamage <= 0) return;

        int max = tool.getType().getMaxDurability();
        if (max <= 0) return;

        int newDamage = damageable.getDamage() + realDamage;
        if (newDamage >= max) {
            tool.setAmount(0);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return;
        }

        damageable.setDamage(newDamage);
        tool.setItemMeta(meta);
    }

    private ToolLore readLore(ItemStack item) {
        ToolLore result = new ToolLore();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return result;

        for (String line : meta.getLore()) {
            String clean = normalize(ChatColor.stripColor(line));

            Matcher tala = talaPattern.matcher(clean);
            if (tala.find()) result.talaMultiple = Math.max(result.talaMultiple, parseIntSafe(tala.group(1)));

            Matcher rotura = roturaPattern.matcher(clean);
            if (rotura.find()) result.roturaMultiple = Math.max(result.roturaMultiple, parseIntSafe(rotura.group(1)));

            Matcher cosecha = cosechaPattern.matcher(clean);
            if (cosecha.find()) result.multiCosecha = Math.max(result.multiCosecha, parseIntSafe(cosecha.group(1)));

            if (clean.contains(autoReplantKey)) result.autoReplantar = true;
        }

        return result;
    }

    private EquipmentBonuses readEquipmentBonuses(Player player) {
        if (player == null || !equipmentBonusesEnabled) return EquipmentBonuses.EMPTY;

        EquipmentBonuses result = new EquipmentBonuses();
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null) return result;

        for (ItemStack piece : armor) {
            readEquipmentBonusLore(piece, result);
        }

        result.cap(maxEquipmentBonusPercent);
        return result;
    }

    private void readEquipmentBonusLore(ItemStack item, EquipmentBonuses result) {
        if (item == null || item.getType() == Material.AIR || result == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return;

        for (String line : meta.getLore()) {
            String clean = normalize(ChatColor.stripColor(line));
            result.agricultureRareDrops += matchPercent(agricultureRareBonusPattern, clean);
            result.rareMinerals += matchPercent(rareMineralsBonusPattern, clean);
            result.treeNodeExtra += matchPercent(treeNodeExtraBonusPattern, clean);
        }
    }

    private double matchPercent(Pattern pattern, String cleanLine) {
        if (pattern == null || cleanLine == null || cleanLine.isBlank()) return 0.0;
        Matcher matcher = pattern.matcher(cleanLine);
        if (!matcher.find()) return 0.0;
        return Math.max(0.0, parseDoubleSafe(matcher.group(1)));
    }

    private double parseDoubleSafe(String raw) {
        if (raw == null) return 0.0;
        try {
            return Double.parseDouble(raw.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private int parseIntSafe(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean isPickaxe(Material mat) {
        return mat.name().endsWith("_PICKAXE");
    }

    private boolean isAxe(Material mat) {
        String name = mat.name();
        return name.endsWith("_AXE") && !name.endsWith("_PICKAXE");
    }

    private BlockFace dominantFace(Vector direction) {
        double ax = Math.abs(direction.getX());
        double ay = Math.abs(direction.getY());
        double az = Math.abs(direction.getZ());

        if (ay >= ax && ay >= az) return direction.getY() >= 0 ? BlockFace.UP : BlockFace.DOWN;
        if (ax >= az) return direction.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST;
        return direction.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private String key(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private String normalize(String raw) {
        if (raw == null) return "";
        String stripped = ChatColor.stripColor(raw);
        String normalized = Normalizer.normalize(stripped, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        return normalized.trim().replaceAll("\\s+", " ");
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    private void debug(String msg) {
        if (debug) getLogger().info("[DEBUG] " + msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mdvtools")) return false;

        if (!sender.hasPermission("mdvtools.admin")) {
            sender.sendMessage(prefix + msgNoPerm);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            registerWeaponSwapLockExternalEvents();
            sender.sendMessage(prefix + msgReloaded);
            return true;
        }

        sender.sendMessage(color("&6&lMDVTools &7comandos:"));
        sender.sendMessage(color("&e/mdvtools reload &7- Recarga la config."));
        return true;
    }

    private static final BlockFace[] CONNECTED_FACES = new BlockFace[]{
            BlockFace.UP, BlockFace.DOWN,
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST
    };

    private static final BlockFace[] HORIZONTAL_FACES = new BlockFace[]{
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST
    };


    private static final class WeaponSwapItemIdentity {
        final String source;
        final String type;
        final String id;

        WeaponSwapItemIdentity(String source, String type, String id) {
            this.source = source == null ? "" : source.toUpperCase(Locale.ROOT);
            this.type = type == null ? "" : type.toUpperCase(Locale.ROOT);
            this.id = id == null ? "" : id.toUpperCase(Locale.ROOT);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof WeaponSwapItemIdentity that)) return false;
            return source.equals(that.source) && type.equals(that.type) && id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, type, id);
        }

        @Override
        public String toString() {
            return source + ":" + type + ":" + id;
        }
    }

    private static final class AutoReloadShot {
        final UUID playerId;
        final long createdAtMs;

        AutoReloadShot(UUID playerId, long createdAtMs) {
            this.playerId = playerId;
            this.createdAtMs = createdAtMs;
        }
    }

    private static final class CustomDropDefinition {
        String key;
        boolean enabled;
        boolean matureOnly;
        double chance;
        int amountMin;
        int amountMax;
        String mmoitemsType;
        String mmoitemsId;
        boolean dropNaturally;
        String fallbackCommand;
        boolean requireToolLore;
        List<String> requiredToolLoreContains = new ArrayList<>();
        Set<String> worlds = new HashSet<>();
        Set<Material> blocks = EnumSet.noneOf(Material.class);
    }

    private static final class EquipmentBonuses {
        static final EquipmentBonuses EMPTY = new EquipmentBonuses();

        double agricultureRareDrops = 0.0;
        double rareMinerals = 0.0;
        double treeNodeExtra = 0.0;

        void cap(double max) {
            if (max <= 0.0) {
                agricultureRareDrops = 0.0;
                rareMinerals = 0.0;
                treeNodeExtra = 0.0;
                return;
            }
            agricultureRareDrops = Math.min(agricultureRareDrops, max);
            rareMinerals = Math.min(rareMinerals, max);
            treeNodeExtra = Math.min(treeNodeExtra, max);
        }
    }

    private static final class ToolLore {
        int talaMultiple = 0;
        int roturaMultiple = 0;
        int multiCosecha = 0;
        boolean autoReplantar = false;

        boolean hasAny() {
            return talaMultiple > 0 || roturaMultiple > 0 || multiCosecha > 0 || autoReplantar;
        }
    }
}
