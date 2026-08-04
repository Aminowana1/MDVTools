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
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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

    private MiningShape defaultMiningShape = MiningShape.LINE;
    private int miningShapeMaxWidth;
    private int miningShapeMaxHeight;
    private int miningShapeMaxDepth;

    private File customDropsFile;
    private FileConfiguration customDropsConfig;
    private boolean customDropsEnabled;
    private String customDropsFallbackCommand;
    private final List<CustomDropDefinition> customDrops = new ArrayList<>();
    private boolean customDropsRollMiningExtras;
    private boolean customDropsRollWoodcuttingExtras;
    private boolean customDropsRelativeBonusesEnabled;
    private boolean customDropsRelativeFarmingBonus;
    private boolean customDropsRelativeMiningBonus;
    private boolean customDropsRelativeWoodcuttingBonus;

    private boolean equipmentBonusesEnabled;
    private boolean agricultureRareBonusEnabled;
    private boolean headOreExtraBonusEnabled;
    private boolean treeNodeExtraBonusEnabled;
    private boolean mainHandToolBonusesEnabled;
    private boolean mainHandToolRequireMatchingType;
    private double maxEquipmentBonusPercent;
    private double maxCombinedBonusPercent;
    private int headOreExtraAmount;
    private int treeNodeExtraAmount;
    private Pattern agricultureRareBonusPattern;
    private Pattern rareMineralsBonusPattern;
    private Pattern treeNodeExtraBonusPattern;
    private NamespacedKey headOreOreKey;
    private NamespacedKey headOreNodeKey;
    private NamespacedKey headOreDropTypeKey;
    private NamespacedKey headOreDropIdKey;

    private boolean professionBonusesEnabled;
    private String mmocorePluginName;
    private long professionBonusCacheMs;
    private double professionBonusMaxPercent;
    private boolean professionBonusCountStartingLevel;
    private String miningProfessionId;
    private String farmingProfessionId;
    private String woodcuttingProfessionId;
    private double miningBonusPerLevel;
    private double farmingBonusPerLevel;
    private double woodcuttingBonusPerLevel;
    private final Map<UUID, ProfessionSnapshot> professionBonusCache = new ConcurrentHashMap<>();

    private boolean mmocoreReflectionTried;
    private Method mmocorePlayerDataHasMethod;
    private Method mmocorePlayerDataGetMethod;
    private Class<?> mmocorePlayerDataHasLookupType;
    private Class<?> mmocorePlayerDataGetLookupType;
    private Method mmocoreGetCollectionSkillsMethod;
    private Method mmocoreGetProfessionLevelMethod;

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

    private boolean identificationEnabled;
    private boolean identificationAllowItemsWithoutTier;
    private final List<String> identificationTierOrder = new ArrayList<>();
    private final Map<String, IdentificationScrollDefinition> identificationScrolls = new HashMap<>();
    private final Map<InventoryClickEvent, ItemStack> identificationSuppressedTargets = new IdentityHashMap<>();
    private String identificationMessageSuccess;
    private String identificationMessageFailure;
    private String identificationMessageTierTooHigh;
    private String identificationMessageInvalidTarget;
    private String identificationMessageStackedTarget;
    private String identificationMessageNoTier;
    private String identificationMessageInternalError;
    private IdentificationSound identificationSuccessSound = new IdentificationSound();
    private IdentificationSound identificationFailureSound = new IdentificationSound();
    private IdentificationSound identificationDeniedSound = new IdentificationSound();

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

    private boolean twoHandedAbilityLockEnabled;
    private boolean twoHandedAbilityLockOnlyWeaponTypes;
    private boolean twoHandedAbilityLockUseMmoItemsStat;
    private boolean twoHandedAbilityLockUseLore;
    private Set<String> twoHandedAbilityLockMmoTypes = new HashSet<>();
    private List<String> twoHandedAbilityLockNbtKeys = new ArrayList<>();
    private List<String> twoHandedAbilityLockLoreKeys = new ArrayList<>();
    private String twoHandedAbilityLockBlockedMessage;
    private long twoHandedAbilityLockBlockedMessageCooldownMs;
    private final Map<UUID, Long> twoHandedAbilityLockLastBlockedMessage = new HashMap<>();

    private boolean abilityDurabilityCostEnabled;
    private int abilityDurabilityCostAmount;
    private boolean abilityDurabilityOnlyCustomDurability;
    private long abilityDurabilityDedupeWindowMs;
    private final Map<UUID, AbilityDurabilityCharge> abilityDurabilityLastCharge = new HashMap<>();

    // Protección event-driven para la durabilidad personalizada de MMOItems.
    // Cualquier item con unbreakable: true puede conservar su durabilidad custom intacta.
    private boolean customDurabilityProtectionEnabled;
    private boolean customDurabilityProtectionUseItemMeta;
    private boolean customDurabilityProtectionUseMmoItemsNbt;
    private final Set<String> customDurabilityProtectionHookedEvents = new HashSet<>();

    private boolean mmoNbtReflectionTried;
    private Class<?> mmoNbtItemClass;
    private Method mmoNbtGetMethod;
    private Method mmoNbtHasTypeMethod;
    private Method mmoNbtGetTypeMethod;
    private Method mmoNbtGetStringMethod;
    private Method mmoNbtGetBooleanMethod;
    private Method mmoNbtGetIntegerMethod;
    private Method mmoNbtHasTagMethod;

    private boolean mmoIdentificationReflectionTried;
    private Constructor<?> mmoIdentifiedItemConstructor;
    private Method mmoIdentifiedItemIdentifyMethod;

    private boolean mmoDurabilityReflectionTried;
    private Constructor<?> mmoDurabilityConstructorItemStack;
    private Constructor<?> mmoDurabilityConstructorNbt;
    private Method mmoDurabilityIsValidMethod;
    private Method mmoDurabilityGetDurabilityMethod;
    private Method mmoDurabilityDecreaseMethod;
    private Method mmoDurabilityToItemMethod;

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
    private Pattern miningShapePattern;
    private String autoReplantKey;

    private String prefix;
    private String msgReloaded;
    private String msgNoPerm;

    // TPA simple
    private boolean tpaEnabled;
    private int tpaRequestTimeoutSeconds;
    private int tpaCooldownSeconds;
    private int tpaInvulnerabilitySeconds;
    private boolean tpaInvulnerabilityCancelOnAttack;
    private boolean tpaAllowCrossWorld;
    private int tpaTeleportDelaySeconds;
    private int tpaMovementGraceSeconds;
    private boolean tpaCancelWarmupOnMove;
    private boolean tpaCancelWarmupOnDamage;
    private boolean tpaCancelWarmupOnAttack;
    private final Map<UUID, LinkedHashMap<UUID, TpaRequest>> tpaIncoming = new HashMap<>();
    private final Map<UUID, TpaRequest> tpaOutgoing = new HashMap<>();
    private final Map<UUID, Long> tpaCooldownUntil = new HashMap<>();
    private final Map<UUID, Long> tpaInvulnerableUntil = new HashMap<>();
    private final Map<UUID, TpaWarmup> tpaWarmups = new HashMap<>();


    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureCustomDropsFile();
        loadSettings();
        Bukkit.getPluginManager().registerEvents(this, this);
        registerWeaponSwapLockExternalEvents();
        registerCustomDurabilityProtectionEvent();
        registerPlaceholderExpansion();
        getLogger().info("MDVTools activado.");
    }

    @Override
    public void onDisable() {
        weaponSwapLockUntil.clear();
        weaponSwapLockLastBlockedMessage.clear();
        weaponSwapLastWeaponBeforeNonWeapon.clear();
        twoHandedAbilityLockLastBlockedMessage.clear();
        abilityDurabilityLastCharge.clear();
        identificationSuppressedTargets.clear();
        professionBonusCache.clear();
        tpaIncoming.clear();
        tpaOutgoing.clear();
        tpaCooldownUntil.clear();
        for (TpaWarmup warmup : new ArrayList<>(tpaWarmups.values())) {
            if (warmup.task != null) warmup.task.cancel();
        }
        tpaWarmups.clear();
        tpaInvulnerableUntil.clear();
        getLogger().info("MDVTools desactivado.");
    }

    private void registerPlaceholderExpansion() {
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderApi == null || !placeholderApi.isEnabled()) {
            debug("PlaceholderAPI no está cargado; placeholders de MDVTools no registrados.");
            return;
        }

        try {
            Class<?> expansionClass = Class.forName("com.mdvcraft.tools.MDVToolsPlaceholderExpansion");
            Object expansion = expansionClass.getConstructor(MDVToolsPlugin.class).newInstance(this);
            Object registered = expansionClass.getMethod("register").invoke(expansion);
            if (registered instanceof Boolean bool && !bool) {
                getLogger().warning("PlaceholderAPI rechazó el registro de la expansión mdvtools.");
            } else {
                getLogger().info("Placeholders %mdvtools_*% registrados.");
            }
        } catch (Throwable throwable) {
            getLogger().warning("No se pudieron registrar los placeholders de MDVTools: "
                    + throwable.getClass().getSimpleName()
                    + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
        }
    }

    private void loadSettings() {
        reloadConfig();

        debug = getConfig().getBoolean("debug", false);

        prefix = color(getConfig().getString("messages.prefix", "&6&l[&5&lMDVCRAFT&6&l]  &4»  &r"));
        msgReloaded = color(getConfig().getString("messages.reloaded", "&aMDVTools recargado."));
        msgNoPerm = color(getConfig().getString("messages.no-permission", "&cNo tienes permiso para hacer eso."));

        tpaEnabled = getConfig().getBoolean("tpa.enabled", true);
        tpaRequestTimeoutSeconds = Math.max(5, getConfig().getInt("tpa.request-timeout-seconds", 60));
        tpaCooldownSeconds = Math.max(0, getConfig().getInt("tpa.cooldown-seconds", 15));
        tpaInvulnerabilitySeconds = Math.max(0, getConfig().getInt("tpa.invulnerability-after-teleport-seconds", 5));
        tpaInvulnerabilityCancelOnAttack = getConfig().getBoolean("tpa.invulnerability-cancel-on-attack", true);
        tpaAllowCrossWorld = getConfig().getBoolean("tpa.allow-cross-world", true);
        tpaTeleportDelaySeconds = Math.max(0, getConfig().getInt("tpa.teleport-delay-seconds", 4));
        tpaMovementGraceSeconds = Math.min(
                tpaTeleportDelaySeconds,
                Math.max(0, getConfig().getInt("tpa.movement-grace-seconds", 1))
        );
        tpaCancelWarmupOnMove = getConfig().getBoolean("tpa.cancel-delay-on-move", true);
        tpaCancelWarmupOnDamage = getConfig().getBoolean("tpa.cancel-delay-on-damage", true);
        tpaCancelWarmupOnAttack = getConfig().getBoolean("tpa.cancel-delay-on-attack", true);

        String tala = getConfig().getString("lore.tala-multiple", "Tala Multiple");
        String rotura = getConfig().getString("lore.rotura-multiple", "Rotura Multiple");
        String cosecha = getConfig().getString("lore.multi-cosecha", "Multi Cosecha");
        String miningShape = getConfig().getString("lore.forma-pico", "Forma de Pico");
        autoReplantKey = normalize(getConfig().getString("lore.auto-replantar", "Auto Replantar"));

        talaPattern = Pattern.compile(Pattern.quote(normalize(tala)) + "\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        roturaPattern = Pattern.compile(Pattern.quote(normalize(rotura)) + "\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        cosechaPattern = Pattern.compile(Pattern.quote(normalize(cosecha)) + "\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        miningShapePattern = Pattern.compile(Pattern.quote(normalize(miningShape)) + "\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

        miningEnabled = getConfig().getBoolean("mining.enabled", true);
        woodcuttingEnabled = getConfig().getBoolean("woodcutting.enabled", true);
        farmingEnabled = getConfig().getBoolean("farming.enabled", true);

        maxMiningExtra = Math.max(0, getConfig().getInt("mining.max-extra-blocks", 32));
        maxWoodExtra = Math.max(0, getConfig().getInt("woodcutting.max-extra-blocks", 24));
        maxCropExtra = Math.max(0, getConfig().getInt("farming.max-extra-crops", 24));

        defaultMiningShape = parseMiningShapeName(getConfig().getString("mining.mode", "LINE"), MiningShape.LINE);
        miningShapeMaxWidth = Math.max(1, getConfig().getInt("mining.shapes.max-width", 7));
        miningShapeMaxHeight = Math.max(1, getConfig().getInt("mining.shapes.max-height", 7));
        miningShapeMaxDepth = Math.max(1, getConfig().getInt("mining.shapes.max-depth", 5));

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
        loadProfessionBonusSettings();
        loadIdentificationSettings();
        loadCrossbowAutoReloadSettings();
        loadWeaponSwapLockSettings();
        loadTwoHandedAbilityLockSettings();
        loadAbilityDurabilityCostSettings();
        loadCustomDurabilityProtectionSettings();
        loadCustomDropBehaviorSettings();
        loadCustomDrops();

        debug("Config cargada. Mining=" + miningAllowed.size() + ", Logs=" + logsAllowed.size() + ", Crops=" + cropsAllowed.size() + ", AntiGhost=" + antiGhostAllowed.size() + ", CustomDrops=" + customDrops.size());
    }

    private void loadEquipmentBonusSettings() {
        equipmentBonusesEnabled = getConfig().getBoolean("equipment-bonuses.enabled", true);
        agricultureRareBonusEnabled = getConfig().getBoolean("equipment-bonuses.agriculture-rare-drops.enabled", true);
        headOreExtraBonusEnabled = getConfig().getBoolean("equipment-bonuses.rare-minerals.enabled", true);
        treeNodeExtraBonusEnabled = getConfig().getBoolean("equipment-bonuses.tree-node-extra.enabled", true);
        mainHandToolBonusesEnabled = getConfig().getBoolean("equipment-bonuses.main-hand-tools.enabled", true);
        mainHandToolRequireMatchingType = getConfig().getBoolean("equipment-bonuses.main-hand-tools.require-matching-tool-type", true);
        maxEquipmentBonusPercent = Math.max(0.0, getConfig().getDouble("equipment-bonuses.max-total-bonus-percent", 100.0));
        maxCombinedBonusPercent = Math.max(0.0, getConfig().getDouble("equipment-bonuses.max-combined-bonus-percent", 100.0));
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


    private void loadProfessionBonusSettings() {
        professionBonusesEnabled = getConfig().getBoolean("profession-bonuses.enabled", true);
        mmocorePluginName = getConfig().getString("profession-bonuses.mmocore-plugin-name", "MMOCore");
        professionBonusCacheMs = Math.max(0L, getConfig().getLong("profession-bonuses.cache-ms", 1000L));
        professionBonusMaxPercent = Math.max(0.0, getConfig().getDouble("profession-bonuses.max-profession-bonus-percent", 100.0));
        professionBonusCountStartingLevel = getConfig().getBoolean("profession-bonuses.count-starting-level", true);

        miningProfessionId = normalizeProfessionId(getConfig().getString("profession-bonuses.mining.profession-id", "mining"));
        farmingProfessionId = normalizeProfessionId(getConfig().getString("profession-bonuses.farming.profession-id", "farming"));
        woodcuttingProfessionId = normalizeProfessionId(getConfig().getString("profession-bonuses.woodcutting.profession-id", "woodcutting"));

        miningBonusPerLevel = Math.max(0.0, getConfig().getDouble("profession-bonuses.mining.bonus-per-level", 0.60));
        farmingBonusPerLevel = Math.max(0.0, getConfig().getDouble("profession-bonuses.farming.bonus-per-level", 0.75));
        woodcuttingBonusPerLevel = Math.max(0.0, getConfig().getDouble("profession-bonuses.woodcutting.bonus-per-level", 0.50));

        professionBonusCache.clear();
        mmocoreReflectionTried = false;
        mmocorePlayerDataHasMethod = null;
        mmocorePlayerDataGetMethod = null;
        mmocorePlayerDataHasLookupType = null;
        mmocorePlayerDataGetLookupType = null;
        mmocoreGetCollectionSkillsMethod = null;
        mmocoreGetProfessionLevelMethod = null;
    }

    private void loadIdentificationSettings() {
        identificationEnabled = getConfig().getBoolean("identification.enabled", true);
        identificationAllowItemsWithoutTier = getConfig().getBoolean("identification.allow-items-without-tier", false);

        identificationTierOrder.clear();
        for (String raw : getConfig().getStringList("identification.tier-order")) {
            if (raw == null) continue;
            String tier = raw.trim().toUpperCase(Locale.ROOT);
            if (!tier.isBlank() && !identificationTierOrder.contains(tier)) identificationTierOrder.add(tier);
        }
        if (identificationTierOrder.isEmpty()) {
            identificationTierOrder.addAll(Arrays.asList("COMUN", "ESPECIAL", "RARO", "EPICO", "LEGENDARIO"));
        }

        identificationScrolls.clear();
        ConfigurationSection scrolls = getConfig().getConfigurationSection("identification.scrolls");
        if (scrolls != null) {
            for (String itemIdRaw : scrolls.getKeys(false)) {
                ConfigurationSection cfg = scrolls.getConfigurationSection(itemIdRaw);
                if (cfg == null) continue;

                IdentificationScrollDefinition definition = new IdentificationScrollDefinition();
                definition.itemId = itemIdRaw.trim().toUpperCase(Locale.ROOT);
                definition.typeId = cfg.getString("type", "CONSUMABLE").trim().toUpperCase(Locale.ROOT);
                definition.chance = Math.max(0.0, Math.min(100.0, cfg.getDouble("chance", 100.0)));
                definition.maximumTier = cfg.getString("maximum-tier", "ANY").trim().toUpperCase(Locale.ROOT);
                definition.consumeOnSuccess = cfg.getBoolean("consume-on-success", true);
                definition.consumeOnFailure = cfg.getBoolean("consume-on-failure", true);
                definition.consumeOnDenied = cfg.getBoolean("consume-on-denied", false);

                if (definition.itemId.isBlank() || definition.typeId.isBlank()) {
                    getLogger().warning("Pergamino inválido en identification.scrolls: " + itemIdRaw);
                    continue;
                }
                if (!definition.maximumTier.equals("ANY") && !definition.maximumTier.equals("*")
                        && !identificationTierOrder.contains(definition.maximumTier)) {
                    getLogger().warning("maximum-tier desconocido para " + definition.itemId + ": " + definition.maximumTier);
                }

                identificationScrolls.put(identificationScrollKey(definition.typeId, definition.itemId), definition);
            }
        }

        identificationMessageSuccess = color(getConfig().getString("identification.messages.success", "&aHas identificado correctamente el objeto."));
        identificationMessageFailure = color(getConfig().getString("identification.messages.failure", "&cEl pergamino se deshizo sin revelar el objeto."));
        identificationMessageTierTooHigh = color(getConfig().getString("identification.messages.tier-too-high", "&cEste pergamino no tiene poder suficiente para identificar un objeto de tier &f{tier}&c."));
        identificationMessageInvalidTarget = color(getConfig().getString("identification.messages.invalid-target", "&cSolo puedes usar este pergamino sobre un objeto no identificado."));
        identificationMessageStackedTarget = color(getConfig().getString("identification.messages.stacked-target", "&cSepara los objetos apilados antes de identificarlos."));
        identificationMessageNoTier = color(getConfig().getString("identification.messages.no-tier", "&cEste objeto no tiene un tier reconocible."));
        identificationMessageInternalError = color(getConfig().getString("identification.messages.internal-error", "&cNo se pudo revelar el objeto. Revisa la consola."));

        identificationSuccessSound = loadIdentificationSound("identification.sounds.success", true, "entity.player.levelup", 0.8f, 1.6f);
        identificationFailureSound = loadIdentificationSound("identification.sounds.failure", true, "block.fire.extinguish", 0.7f, 0.8f);
        identificationDeniedSound = loadIdentificationSound("identification.sounds.denied", true, "block.note_block.bass", 0.6f, 0.7f);

        debug("Identificación cargada. Pergaminos=" + identificationScrolls.size() + ", tiers=" + identificationTierOrder);
    }

    private IdentificationSound loadIdentificationSound(String path, boolean defaultEnabled, String defaultValue, float defaultVolume, float defaultPitch) {
        IdentificationSound sound = new IdentificationSound();
        sound.enabled = getConfig().getBoolean(path + ".enabled", defaultEnabled);
        sound.value = getConfig().getString(path + ".value", defaultValue);
        sound.volume = (float) getConfig().getDouble(path + ".volume", defaultVolume);
        sound.pitch = (float) getConfig().getDouble(path + ".pitch", defaultPitch);
        return sound;
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


    private void loadTwoHandedAbilityLockSettings() {
        twoHandedAbilityLockEnabled = getConfig().getBoolean("two-handed-ability-lock.enabled", true);
        twoHandedAbilityLockOnlyWeaponTypes = getConfig().getBoolean("two-handed-ability-lock.only-weapon-types", true);
        twoHandedAbilityLockUseMmoItemsStat = getConfig().getBoolean("two-handed-ability-lock.detect.mmoitems-stat", true);
        twoHandedAbilityLockUseLore = getConfig().getBoolean("two-handed-ability-lock.detect.lore", true);

        twoHandedAbilityLockMmoTypes = new HashSet<>();
        for (String raw : getConfig().getStringList("two-handed-ability-lock.mmoitems-types")) {
            if (raw == null) continue;
            String type = raw.trim().toUpperCase(Locale.ROOT);
            if (!type.isBlank()) twoHandedAbilityLockMmoTypes.add(type);
        }
        if (twoHandedAbilityLockMmoTypes.isEmpty()) {
            twoHandedAbilityLockMmoTypes.addAll(weaponSwapLockMmoTypes);
        }
        if (twoHandedAbilityLockMmoTypes.isEmpty()) {
            twoHandedAbilityLockMmoTypes.addAll(Arrays.asList(
                    "SWORD", "DAGGER", "AXE", "HAMMER", "MACE",
                    "BOW", "CROSSBOW", "STAFF", "WAND", "CATALYST",
                    "SPEAR", "GREATSWORD", "GREATSTAFF", "LUTE", "TOME", "TOMO"
            ));
        }

        twoHandedAbilityLockNbtKeys = new ArrayList<>();
        for (String raw : getConfig().getStringList("two-handed-ability-lock.detect.nbt-keys")) {
            if (raw == null) continue;
            String key = raw.trim();
            if (!key.isBlank()) twoHandedAbilityLockNbtKeys.add(key);
        }
        if (twoHandedAbilityLockNbtKeys.isEmpty()) {
            twoHandedAbilityLockNbtKeys.addAll(Arrays.asList(
                    "MMOITEMS_TWO_HANDED", "MMOITEMS_TWO-HANDED",
                    "TWO_HANDED", "TWO-HANDED", "two-handed", "two_handed"
            ));
        }

        twoHandedAbilityLockLoreKeys = new ArrayList<>();
        for (String raw : getConfig().getStringList("two-handed-ability-lock.detect.lore-lines")) {
            String normalized = normalizeLoose(raw).toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) twoHandedAbilityLockLoreKeys.add(normalized);
        }
        if (twoHandedAbilityLockLoreKeys.isEmpty()) {
            for (String value : Arrays.asList("Dos Manos", "Dos manos", "Two Handed", "Two-Handed", "two-handed", "Requiere ambas manos", "Ambas manos")) {
                twoHandedAbilityLockLoreKeys.add(normalizeLoose(value).toLowerCase(Locale.ROOT));
            }
        }

        twoHandedAbilityLockBlockedMessage = color(getConfig().getString("two-handed-ability-lock.messages.blocked", "&cNecesitas ambas manos libres para usar esta arma."));
        twoHandedAbilityLockBlockedMessageCooldownMs = Math.max(0L, getConfig().getLong("two-handed-ability-lock.messages.cooldown-ms", 800L));

        if (!twoHandedAbilityLockEnabled) {
            twoHandedAbilityLockLastBlockedMessage.clear();
        }
    }



    private void loadCustomDurabilityProtectionSettings() {
        customDurabilityProtectionEnabled = getConfig().getBoolean("custom-durability-protection.enabled", true);
        customDurabilityProtectionUseItemMeta = getConfig().getBoolean("custom-durability-protection.detect.item-meta-unbreakable", true);
        customDurabilityProtectionUseMmoItemsNbt = getConfig().getBoolean("custom-durability-protection.detect.mmoitems-unbreakable-stat", true);
    }

    private void loadAbilityDurabilityCostSettings() {
        abilityDurabilityCostEnabled = getConfig().getBoolean("ability-durability-cost.enabled", true);
        abilityDurabilityCostAmount = Math.max(0, getConfig().getInt("ability-durability-cost.cost", 1));
        abilityDurabilityOnlyCustomDurability = getConfig().getBoolean("ability-durability-cost.only-custom-durability", true);
        abilityDurabilityDedupeWindowMs = Math.max(0L, getConfig().getLong("ability-durability-cost.dedupe-window-ms", 75L));

        if (!abilityDurabilityCostEnabled || abilityDurabilityCostAmount <= 0) {
            abilityDurabilityLastCharge.clear();
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

    private void loadCustomDropBehaviorSettings() {
        customDropsRollMiningExtras = getConfig().getBoolean("custom-drops.roll-on-extra-blocks.mining", true);
        customDropsRollWoodcuttingExtras = getConfig().getBoolean("custom-drops.roll-on-extra-blocks.woodcutting", true);

        customDropsRelativeBonusesEnabled = getConfig().getBoolean("custom-drops.relative-bonuses.enabled", true);
        customDropsRelativeFarmingBonus = getConfig().getBoolean("custom-drops.relative-bonuses.farming", true);
        customDropsRelativeMiningBonus = getConfig().getBoolean("custom-drops.relative-bonuses.mining", true);
        customDropsRelativeWoodcuttingBonus = getConfig().getBoolean("custom-drops.relative-bonuses.woodcutting", true);
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
            def.category = parseCustomDropCategory(cfg.getString("category", cfg.getString("categoria", "AUTO")), key);
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




    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onIdentificationScrollUse(InventoryClickEvent event) {
        if (!identificationEnabled || identificationScrolls.isEmpty()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;
        if (event.getClickedInventory() != player.getInventory()) return;

        ItemStack cursor = event.getCursor();
        IdentificationScrollDefinition scroll = getIdentificationScroll(cursor);
        if (scroll == null) return;

        ItemStack target = event.getCurrentItem();
        if (target == null || target.getType() == Material.AIR) return;

        // MDVTools controla completamente este gesto. El pergamino NO debe tener can-identify: true,
        // porque la identificación nativa de MMOItems siempre tiene éxito.
        event.setCancelled(true);

        if (!hasMmoItemTag(target, "MMOITEMS_UNIDENTIFIED_ITEM")) {
            sendIdentificationMessage(player, identificationMessageInvalidTarget, scroll, null);
            playIdentificationSound(player, identificationDeniedSound);
            suppressNativeIdentificationIfNeeded(event, target);
            scheduleInventoryRefresh(player);
            return;
        }

        if (target.getAmount() > 1) {
            sendIdentificationMessage(player, identificationMessageStackedTarget, scroll, null);
            playIdentificationSound(player, identificationDeniedSound);
            suppressNativeIdentificationIfNeeded(event, target);
            scheduleInventoryRefresh(player);
            return;
        }

        ItemStack identified = identifyMmoItem(target);
        if (identified == null || identified.getType() == Material.AIR) {
            sendIdentificationMessage(player, identificationMessageInternalError, scroll, null);
            playIdentificationSound(player, identificationDeniedSound);
            getLogger().warning("No pude deserializar un objeto no identificado para " + player.getName() + ".");
            suppressNativeIdentificationIfNeeded(event, target);
            scheduleInventoryRefresh(player);
            return;
        }

        String tier = readMmoItemString(identified, "MMOITEMS_TIER");
        if ((tier == null || tier.isBlank()) && !identificationAllowItemsWithoutTier) {
            if (scroll.consumeOnDenied) consumeIdentificationScroll(event);
            sendIdentificationMessage(player, identificationMessageNoTier, scroll, null);
            playIdentificationSound(player, identificationDeniedSound);
            suppressNativeIdentificationIfNeeded(event, target);
            scheduleInventoryRefresh(player);
            return;
        }

        if (tier != null && !isIdentificationTierAllowed(tier, scroll.maximumTier)) {
            if (scroll.consumeOnDenied) consumeIdentificationScroll(event);
            sendIdentificationMessage(player, identificationMessageTierTooHigh, scroll, tier);
            playIdentificationSound(player, identificationDeniedSound);
            suppressNativeIdentificationIfNeeded(event, target);
            scheduleInventoryRefresh(player);
            return;
        }

        boolean success = scroll.chance >= 100.0 || (scroll.chance > 0.0 && ThreadLocalRandom.current().nextDouble(100.0) < scroll.chance);
        if (!success) {
            if (scroll.consumeOnFailure) consumeIdentificationScroll(event);
            sendIdentificationMessage(player, identificationMessageFailure, scroll, tier);
            playIdentificationSound(player, identificationFailureSound);
            suppressNativeIdentificationIfNeeded(event, target);
            scheduleInventoryRefresh(player);
            return;
        }

        if (scroll.consumeOnSuccess) consumeIdentificationScroll(event);
        identified.setAmount(1);
        event.setCurrentItem(identified);
        sendIdentificationMessage(player, identificationMessageSuccess, scroll, tier);
        playIdentificationSound(player, identificationSuccessSound);
        scheduleInventoryRefresh(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void restoreIdentificationTargetAfterNativeListeners(InventoryClickEvent event) {
        ItemStack restore = identificationSuppressedTargets.remove(event);
        if (restore != null) event.setCurrentItem(restore);
    }

    private void suppressNativeIdentificationIfNeeded(InventoryClickEvent event, ItemStack target) {
        if (event == null || target == null) return;
        if (!Boolean.TRUE.equals(readMmoItemBoolean(event.getCursor(), "MMOITEMS_CAN_IDENTIFY"))) return;

        // Algunas configuraciones antiguas pueden conservar can-identify: true.
        // Ocultamos el objetivo solo durante el resto de este evento para impedir que
        // MMOItems aplique después su identificación nativa garantizada.
        identificationSuppressedTargets.put(event, target.clone());
        event.setCurrentItem(null);
    }

    private IdentificationScrollDefinition getIdentificationScroll(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return null;
        String typeId = readMmoItemTypeId(item);
        String itemId = readMmoItemString(item, "MMOITEMS_ITEM_ID");
        if (typeId == null || itemId == null) return null;
        return identificationScrolls.get(identificationScrollKey(typeId, itemId));
    }

    private String identificationScrollKey(String typeId, String itemId) {
        return (typeId == null ? "" : typeId.trim().toUpperCase(Locale.ROOT)) + ":"
                + (itemId == null ? "" : itemId.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isIdentificationTierAllowed(String itemTier, String maximumTier) {
        if (maximumTier == null || maximumTier.isBlank() || maximumTier.equalsIgnoreCase("ANY") || maximumTier.equals("*")) return true;
        int itemIndex = identificationTierOrder.indexOf(itemTier.toUpperCase(Locale.ROOT));
        int maximumIndex = identificationTierOrder.indexOf(maximumTier.toUpperCase(Locale.ROOT));
        return itemIndex >= 0 && maximumIndex >= 0 && itemIndex <= maximumIndex;
    }

    private void consumeIdentificationScroll(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR || cursor.getAmount() <= 0) return;
        if (cursor.getAmount() <= 1) {
            event.getView().setCursor(null);
        } else {
            ItemStack remaining = cursor.clone();
            remaining.setAmount(cursor.getAmount() - 1);
            event.getView().setCursor(remaining);
        }
    }

    private void sendIdentificationMessage(Player player, String message, IdentificationScrollDefinition scroll, String tier) {
        if (player == null || message == null || message.isBlank()) return;
        String rendered = message
                .replace("{chance}", formatChance(scroll == null ? 0.0 : scroll.chance))
                .replace("{maximum-tier}", scroll == null ? "" : scroll.maximumTier)
                .replace("{tier}", tier == null ? "DESCONOCIDO" : tier);
        player.sendMessage(prefix + rendered);
    }

    private String formatChance(double chance) {
        if (Math.rint(chance) == chance) return String.valueOf((int) chance);
        return String.format(Locale.US, "%.2f", chance).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void playIdentificationSound(Player player, IdentificationSound configured) {
        if (player == null || configured == null || !configured.enabled || configured.value == null || configured.value.isBlank()) return;
        try {
            player.playSound(player.getLocation(), configured.value, configured.volume, configured.pitch);
        } catch (Throwable first) {
            try {
                Sound sound = Sound.valueOf(configured.value.toUpperCase(Locale.ROOT).replace('.', '_'));
                player.playSound(player.getLocation(), sound, configured.volume, configured.pitch);
            } catch (Throwable ignored) {
                if (debug) getLogger().warning("Sonido inválido para identificación: " + configured.value);
            }
        }
    }

    private void scheduleInventoryRefresh(Player player) {
        Bukkit.getScheduler().runTask(this, player::updateInventory);
    }

    private boolean hasMmoItemTag(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR || key == null || key.isBlank()) return false;
        if (!ensureMmoNbtReflection()) return false;
        try {
            Object nbt = mmoNbtGetMethod.invoke(null, item);
            if (nbt == null) return false;
            if (mmoNbtHasTagMethod != null) {
                Object result = mmoNbtHasTagMethod.invoke(nbt, key);
                return result instanceof Boolean bool && bool;
            }
            String raw = readMmoItemStringRaw(item, key);
            return raw != null && !raw.isBlank();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ItemStack identifyMmoItem(ItemStack unidentified) {
        if (!ensureMmoIdentificationReflection()) return null;
        try {
            Object nbt = mmoNbtGetMethod.invoke(null, unidentified);
            if (nbt == null) return null;
            Object wrapper = mmoIdentifiedItemConstructor.newInstance(nbt);
            Object result = mmoIdentifiedItemIdentifyMethod.invoke(wrapper);
            return result instanceof ItemStack stack ? stack.clone() : null;
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("Error identificando objeto MMOItems: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return null;
        }
    }

    private boolean ensureMmoIdentificationReflection() {
        if (mmoIdentificationReflectionTried) return mmoIdentifiedItemConstructor != null && mmoIdentifiedItemIdentifyMethod != null;
        mmoIdentificationReflectionTried = true;
        if (!ensureMmoNbtReflection()) return false;

        try {
            Class<?> identifiedClass = Class.forName("net.Indyuce.mmoitems.api.item.util.identify.IdentifiedItem");
            for (Constructor<?> constructor : identifiedClass.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 1 && parameters[0].isAssignableFrom(mmoNbtItemClass)) {
                    mmoIdentifiedItemConstructor = constructor;
                    break;
                }
            }
            if (mmoIdentifiedItemConstructor == null) return false;
            mmoIdentifiedItemIdentifyMethod = identifiedClass.getMethod("identify");
            debug("Sistema IdentifiedItem de MMOItems detectado.");
            return true;
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("No pude conectar con IdentifiedItem de MMOItems: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return false;
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
        twoHandedAbilityLockLastBlockedMessage.remove(id);
        weaponSwapLastWeaponBeforeNonWeapon.remove(id);
        professionBonusCache.remove(id);
        removeTpaRequestsFor(id, true);
        cancelTpaWarmupsFor(id, true);
        tpaCooldownUntil.remove(id);
        tpaInvulnerableUntil.remove(id);
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
        if (internalBreakEvent || (!equipmentBonusesEnabled && !professionBonusesEnabled)) return;
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

        EquipmentBonuses bonuses = readCombinedBonuses(player);
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
            int broken = breakMiningShape(player, block, tool, extra, miningAllowed, lore.miningShape);
            damageTool(player, tool, broken * durabilityCostBlock);
            MiningShapeSpec usedShape = lore.miningShape == null ? defaultMiningShapeSpec() : lore.miningShape;
            debug("Rotura múltiple " + usedShape.shape + ": " + broken + " extra.");
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

    private int breakMiningShape(Player player, Block original, ItemStack tool, int extra, Set<Material> allowed, MiningShapeSpec requestedShape) {
        if (extra <= 0) return 0;

        MiningShapeSpec shape = requestedShape == null ? defaultMiningShapeSpec() : requestedShape;
        return switch (shape.shape) {
            case LINE -> breakLine(player, original, tool, extra, allowed);
            case PLANE -> breakAreaCandidates(player, original, tool, extra, allowed,
                    collectPlaneBlocks(original, dominantFace(player.getEyeLocation().getDirection()), shape.width, shape.height));
            case CUBE -> breakAreaCandidates(player, original, tool, extra, allowed,
                    collectCubeBlocks(original, shape.width, shape.height, shape.depth));
        };
    }

    private int breakAreaCandidates(Player player, Block original, ItemStack tool, int extra, Set<Material> allowed, List<Block> candidates) {
        if (extra <= 0 || candidates.isEmpty()) return 0;

        candidates.sort(Comparator
                .comparingInt((Block block) -> blockDistanceSquared(original, block))
                .thenComparingInt(Block::getY)
                .thenComparingInt(Block::getX)
                .thenComparingInt(Block::getZ));

        int broken = 0;
        for (Block candidate : candidates) {
            if (broken >= extra) break;
            if (!allowed.contains(candidate.getType())) continue;
            if (breakExtraNaturally(player, candidate, tool, CustomDropCategory.MINING)) broken++;
        }
        return broken;
    }

    private List<Block> collectPlaneBlocks(Block original, BlockFace face, int width, int height) {
        List<Block> blocks = new ArrayList<>(Math.max(0, width * height - 1));
        int startWidth = centeredStart(width);
        int startHeight = centeredStart(height);

        for (int widthIndex = 0; widthIndex < width; widthIndex++) {
            int horizontalOffset = startWidth + widthIndex;
            for (int heightIndex = 0; heightIndex < height; heightIndex++) {
                int verticalOffset = startHeight + heightIndex;
                if (horizontalOffset == 0 && verticalOffset == 0) continue;

                Block candidate;
                if (face == BlockFace.UP || face == BlockFace.DOWN) {
                    candidate = original.getRelative(horizontalOffset, 0, verticalOffset);
                } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
                    candidate = original.getRelative(0, verticalOffset, horizontalOffset);
                } else {
                    candidate = original.getRelative(horizontalOffset, verticalOffset, 0);
                }
                blocks.add(candidate);
            }
        }
        return blocks;
    }

    private List<Block> collectCubeBlocks(Block original, int width, int height, int depth) {
        List<Block> blocks = new ArrayList<>(Math.max(0, width * height * depth - 1));
        int startX = centeredStart(width);
        int startY = centeredStart(height);
        int startZ = centeredStart(depth);

        for (int xIndex = 0; xIndex < width; xIndex++) {
            int x = startX + xIndex;
            for (int yIndex = 0; yIndex < height; yIndex++) {
                int y = startY + yIndex;
                for (int zIndex = 0; zIndex < depth; zIndex++) {
                    int z = startZ + zIndex;
                    if (x == 0 && y == 0 && z == 0) continue;
                    blocks.add(original.getRelative(x, y, z));
                }
            }
        }
        return blocks;
    }

    private int centeredStart(int size) {
        return -((Math.max(1, size) - 1) / 2);
    }

    private int blockDistanceSquared(Block origin, Block other) {
        int dx = other.getX() - origin.getX();
        int dy = other.getY() - origin.getY();
        int dz = other.getZ() - origin.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private int breakLine(Player player, Block original, ItemStack tool, int extra, Set<Material> allowed) {
        if (extra <= 0) return 0;

        BlockFace face = dominantFace(player.getEyeLocation().getDirection());
        int broken = 0;
        Block current = original;

        for (int i = 0; i < extra; i++) {
            current = current.getRelative(face);
            if (!allowed.contains(current.getType())) break;

            if (breakExtraNaturally(player, current, tool, CustomDropCategory.MINING)) {
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

            if (breakExtraNaturally(player, block, tool, CustomDropCategory.WOODCUTTING)) {
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

    private boolean breakExtraNaturally(Player player, Block block, ItemStack tool, CustomDropCategory sourceCategory) {
        if (!canBreakExtraBlock(player, block)) return false;

        CustomDropBlockSnapshot dropSnapshot = CustomDropBlockSnapshot.capture(block);
        boolean broken = block.breakNaturally(tool);
        if (broken) {
            if (shouldRollCustomDropsOnExtra(sourceCategory)) {
                rollCustomDrops(player, dropSnapshot, tool);
            }
            scheduleAntiGhostRefresh(player, block);
        }
        return broken;
    }

    private boolean shouldRollCustomDropsOnExtra(CustomDropCategory sourceCategory) {
        if (!customDropsEnabled || customDrops.isEmpty() || sourceCategory == null) return false;
        return switch (sourceCategory) {
            case MINING -> customDropsRollMiningExtras;
            case WOODCUTTING -> customDropsRollWoodcuttingExtras;
            default -> false;
        };
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
        if (block == null) return;
        rollCustomDrops(player, CustomDropBlockSnapshot.capture(block), tool);
    }

    private void rollCustomDrops(Player player, CustomDropBlockSnapshot snapshot, ItemStack tool) {
        if (!customDropsEnabled || customDrops.isEmpty() || player == null || snapshot == null) return;

        EquipmentBonuses bonuses = customDropsRelativeBonusesEnabled
                ? readCombinedBonuses(player)
                : EquipmentBonuses.EMPTY;

        for (CustomDropDefinition def : customDrops) {
            if (!matchesCustomDrop(def, snapshot, tool)) continue;

            CustomDropCategory category = resolveCustomDropCategory(def, snapshot);
            double effectiveChance = def.chance;
            if (isRelativeCustomDropBonusEnabled(category)) {
                effectiveChance = applyRelativeBonus(effectiveChance, relativeBonusForCategory(bonuses, category));
            }

            if (ThreadLocalRandom.current().nextDouble(100.0) >= effectiveChance) continue;

            int amount = def.amountMin;
            if (def.amountMax > def.amountMin) {
                amount = ThreadLocalRandom.current().nextInt(def.amountMin, def.amountMax + 1);
            }
            dropCustomMmoItem(def, player, snapshot, amount);
        }
    }

    private boolean matchesCustomDrop(CustomDropDefinition def, CustomDropBlockSnapshot snapshot, ItemStack tool) {
        if (def == null || !def.enabled || snapshot == null) return false;
        if (!def.blocks.contains(snapshot.type)) return false;
        if (!def.worlds.isEmpty() && !def.worlds.contains(snapshot.world.getName())) return false;
        if (def.matureOnly && !isMatureCrop(snapshot.blockData)) return false;

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

    private CustomDropCategory resolveCustomDropCategory(CustomDropDefinition def, CustomDropBlockSnapshot snapshot) {
        if (def != null && def.category != null && def.category != CustomDropCategory.AUTO) return def.category;
        if (snapshot == null) return CustomDropCategory.NONE;

        if (cropsAllowed.contains(snapshot.type) || snapshot.blockData instanceof Ageable) {
            return CustomDropCategory.FARMING;
        }
        if (logsAllowed.contains(snapshot.type) || isWoodLikeMaterial(snapshot.type)) {
            return CustomDropCategory.WOODCUTTING;
        }
        if (miningAllowed.contains(snapshot.type) || isOreLikeMaterial(snapshot.type)) {
            return CustomDropCategory.MINING;
        }
        return CustomDropCategory.NONE;
    }

    private CustomDropCategory parseCustomDropCategory(String raw, String key) {
        String normalized = normalizeLoose(raw).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "auto", "automatico", "automatica" -> CustomDropCategory.AUTO;
            case "none", "ninguna", "ninguno", "sin bonus", "sin categoria" -> CustomDropCategory.NONE;
            case "farming", "farm", "agriculture", "agricultura", "cultivo", "cosecha" -> CustomDropCategory.FARMING;
            case "mining", "mine", "mineria", "minero", "mineral", "minerales" -> CustomDropCategory.MINING;
            case "woodcutting", "wood", "tala", "lenador", "arbol", "arboles" -> CustomDropCategory.WOODCUTTING;
            default -> {
                getLogger().warning("Categoría inválida en custom-drops.yml -> " + key + ".category: " + raw + ". Usaré AUTO.");
                yield CustomDropCategory.AUTO;
            }
        };
    }

    private boolean isRelativeCustomDropBonusEnabled(CustomDropCategory category) {
        if (!customDropsRelativeBonusesEnabled || category == null) return false;
        return switch (category) {
            case FARMING -> customDropsRelativeFarmingBonus && agricultureRareBonusEnabled;
            case MINING -> customDropsRelativeMiningBonus && headOreExtraBonusEnabled;
            case WOODCUTTING -> customDropsRelativeWoodcuttingBonus && treeNodeExtraBonusEnabled;
            default -> false;
        };
    }

    private double relativeBonusForCategory(EquipmentBonuses bonuses, CustomDropCategory category) {
        if (bonuses == null || category == null) return 0.0;
        return switch (category) {
            case FARMING -> bonuses.agricultureRareDrops;
            case MINING -> bonuses.rareMinerals;
            case WOODCUTTING -> bonuses.treeNodeExtra;
            default -> 0.0;
        };
    }

    private boolean isWoodLikeMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD")
                || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    private boolean isOreLikeMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
    }

    private boolean isMatureCrop(BlockData blockData) {
        if (!(blockData instanceof Ageable ageable)) return false;
        return ageable.getAge() >= ageable.getMaximumAge();
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

    private void dropCustomMmoItem(CustomDropDefinition def, Player player, CustomDropBlockSnapshot snapshot, int amount) {
        amount = Math.max(1, amount);

        if (def.dropNaturally) {
            ItemStack stack = buildMmoItemStack(def.mmoitemsType, def.mmoitemsId, amount);
            if (stack != null && stack.getType() != Material.AIR) {
                Location location = snapshot.location.clone().add(0.5, 0.55, 0.5);
                Item item = snapshot.world.dropItemNaturally(location, stack);
                item.setPickupDelay(10);
                debug("Drop custom [" + resolveCustomDropCategory(def, snapshot) + "]: "
                        + def.mmoitemsType + ":" + def.mmoitemsId + " x" + amount + " en " + snapshot.type);
                return;
            }
        }

        runCustomDropFallback(def, player, snapshot, amount);
    }

    private void runCustomDropFallback(CustomDropDefinition def, Player player, CustomDropBlockSnapshot snapshot, int amount) {
        String command = def.fallbackCommand;
        if (command == null || command.isBlank()) command = customDropsFallbackCommand;
        if (command == null || command.isBlank()) return;

        command = command
                .replace("%player%", player.getName())
                .replace("%world%", snapshot.world.getName())
                .replace("%x%", Integer.toString(snapshot.location.getBlockX()))
                .replace("%y%", Integer.toString(snapshot.location.getBlockY()))
                .replace("%z%", Integer.toString(snapshot.location.getBlockZ()))
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




    /**
     * Conecta MDVTools al evento cancelable de daño de durabilidad custom de MMOItems.
     *
     * Se registra por reflexión para mantener MMOItems como softdepend y no exigir
     * su API como dependencia de compilación. No hay tareas periódicas ni escaneo
     * de inventarios: solo se ejecuta cuando MMOItems intenta gastar durabilidad.
     */
    @SuppressWarnings("unchecked")
    private void registerCustomDurabilityProtectionEvent() {
        final String className = "net.Indyuce.mmoitems.api.event.item.CustomDurabilityDamage";
        if (customDurabilityProtectionHookedEvents.contains(className)) return;

        try {
            Class<?> rawClass = Class.forName(className);
            if (!Event.class.isAssignableFrom(rawClass)) {
                if (debug) getLogger().warning("No registré " + className + " porque no extiende Bukkit Event.");
                return;
            }

            Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
            EventExecutor executor = (listener, event) -> {
                try {
                    handleCustomDurabilityProtectionEvent(event);
                } catch (Throwable throwable) {
                    if (debug) {
                        getLogger().warning("Error protegiendo durabilidad custom en " + event.getEventName()
                                + ": " + throwable.getClass().getSimpleName()
                                + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
                    }
                }
            };

            Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.HIGHEST, executor, this, false);
            customDurabilityProtectionHookedEvents.add(className);
            getLogger().info("Protección de durabilidad custom conectada a MMOItems.");
        } catch (ClassNotFoundException ignored) {
            debug("Protección de durabilidad custom: evento de MMOItems no encontrado.");
        } catch (Throwable throwable) {
            getLogger().warning("No pude registrar la protección de durabilidad custom: "
                    + throwable.getClass().getSimpleName()
                    + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
        }
    }

    private void handleCustomDurabilityProtectionEvent(Event event) {
        if (!customDurabilityProtectionEnabled) return;
        if (!(event instanceof Cancellable cancellable) || cancellable.isCancelled()) return;

        ItemStack item = extractItemFromCustomDurabilityEvent(event);
        if (!isCustomDurabilityProtected(item)) return;

        cancellable.setCancelled(true);
    }

    private ItemStack extractItemFromCustomDurabilityEvent(Event event) {
        if (event == null) return null;

        try {
            // CustomDurabilityDamage#getItem() -> CustomDurabilityItem
            Method getSourceItem = event.getClass().getMethod("getItem");
            Object sourceItem = getSourceItem.invoke(event);
            if (sourceItem == null) return null;

            // DurabilityItem#getNBTItem() -> NBTItem
            Method getNbtItem = sourceItem.getClass().getMethod("getNBTItem");
            Object nbtItem = getNbtItem.invoke(sourceItem);
            if (nbtItem == null) return null;

            // NBTItem#getItem() -> ItemStack
            Method getItem = nbtItem.getClass().getMethod("getItem");
            Object value = getItem.invoke(nbtItem);
            return value instanceof ItemStack stack ? stack : null;
        } catch (Throwable throwable) {
            if (debug) {
                getLogger().warning("No pude extraer el item del evento de durabilidad custom: "
                        + throwable.getClass().getSimpleName()
                        + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
            }
            return null;
        }
    }

    /**
     * Marcador universal de objetos eternos/reliquias.
     *
     * No depende del ID, nombre, tipo, tier ni familia del modificador. Por eso
     * funciona igual para melee, rango, magia, soporte, armaduras o reliquias:
     * basta con que el resultado final tenga unbreakable: true.
     */
    private boolean isCustomDurabilityProtected(ItemStack item) {
        if (!customDurabilityProtectionEnabled || !isRealItem(item)) return false;

        if (customDurabilityProtectionUseItemMeta) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.isUnbreakable()) return true;
        }

        // MMOItems 6.10.1 guarda el stat UNBREAKABLE en el path NBT "Unbreakable".
        // Se usa como respaldo por si otra transformación reconstruye el ItemMeta.
        if (customDurabilityProtectionUseMmoItemsNbt) {
            Boolean nbtUnbreakable = readMmoItemBoolean(item, "Unbreakable");
            if (Boolean.TRUE.equals(nbtUnbreakable)) return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void registerWeaponSwapLockExternalEvents() {
        boolean hookMmoItemsAbilities = (weaponSwapLockEnabled && weaponSwapLockBlockMmoItemAbilities) || twoHandedAbilityLockEnabled || abilityDurabilityCostEnabled;
        boolean hookMythicLibSkills = (weaponSwapLockEnabled && weaponSwapLockBlockMythicLibSkills) || twoHandedAbilityLockEnabled || abilityDurabilityCostEnabled;
        if (!hookMmoItemsAbilities && !hookMythicLibSkills) return;

        // MMOItems dispara este evento para habilidades de item. Cancelarlo evita que la habilidad se castee
        // aunque PlayerInteractEvent ya haya sido cancelado.
        if (hookMmoItemsAbilities) {
            registerWeaponSwapLockExternalEvent("net.Indyuce.mmoitems.api.event.AbilityUseEvent");
        }

        // MythicLib centraliza skills de MMOItems/MMOCore desde versiones modernas.
        if (hookMythicLibSkills) {
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
        if (!(event instanceof Cancellable cancellable)) return;
        if (cancellable.isCancelled()) return;

        Player player = extractPlayerFromExternalEvent(event);
        if (player == null) return;

        if (isTwoHandedAbilityBlocked(player)) {
            cancellable.setCancelled(true);
            sendTwoHandedAbilityLockBlockedFeedback(player);
            return;
        }

        if (weaponSwapLockEnabled && isWeaponSwapLocked(player)) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (isWeaponSwapLockWeapon(item)) {
                cancellable.setCancelled(true);
                sendWeaponSwapLockBlockedFeedback(player);
                return;
            }
        }

        if (!chargeAbilityCustomDurability(player)) {
            cancellable.setCancelled(true);
        }
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


    private boolean isTwoHandedAbilityBlocked(Player player) {
        if (!twoHandedAbilityLockEnabled || player == null) return false;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;

        PlayerInventory inventory = player.getInventory();
        ItemStack main = inventory.getItemInMainHand();
        ItemStack offhand = inventory.getItemInOffHand();

        boolean hasMain = isRealItem(main);
        boolean hasOffhand = isRealItem(offhand);
        if (!hasMain || !hasOffhand) return false;

        // Caso original: arma de 2 manos en mainhand + cualquier item en offhand.
        if (isTwoHandedAbilityLockCandidate(main)) return true;

        // Caso inverso: arma de 2 manos en offhand + cualquier item en mainhand.
        // Esto evita que el jugador esquive el bloqueo moviendo el arma de 2 manos a la offhand.
        return isTwoHandedAbilityLockCandidate(offhand);
    }

    private boolean isTwoHandedAbilityLockCandidate(ItemStack item) {
        if (!isRealItem(item)) return false;
        if (twoHandedAbilityLockOnlyWeaponTypes && !isTwoHandedAbilityLockWeapon(item)) return false;
        return isTwoHandedItem(item);
    }

    private boolean isRealItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() > 0;
    }

    private boolean isTwoHandedAbilityLockWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return false;

        String mmoType = readMmoItemTypeId(item);
        if (mmoType != null && twoHandedAbilityLockMmoTypes.contains(mmoType.toUpperCase(Locale.ROOT))) {
            return true;
        }

        return weaponSwapLockFallbackEnabled && weaponSwapLockFallbackMaterials.contains(item.getType());
    }

    private boolean isTwoHandedItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return false;

        if (twoHandedAbilityLockUseMmoItemsStat) {
            for (String key : twoHandedAbilityLockNbtKeys) {
                Boolean booleanValue = readMmoItemBoolean(item, key);
                if (Boolean.TRUE.equals(booleanValue)) return true;

                String stringValue = readMmoItemStringRaw(item, key);
                if (isTruthy(stringValue)) return true;
            }
        }

        return twoHandedAbilityLockUseLore && hasTwoHandedLore(item);
    }

    private boolean hasTwoHandedLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return false;

        for (String line : meta.getLore()) {
            String clean = normalizeLoose(line).toLowerCase(Locale.ROOT);
            if (clean.isBlank()) continue;
            for (String key : twoHandedAbilityLockLoreKeys) {
                if (!key.isBlank() && clean.contains(key)) return true;
            }
        }
        return false;
    }

    private boolean isTruthy(String raw) {
        if (raw == null) return false;
        String value = normalizeLoose(raw).toLowerCase(Locale.ROOT);
        return value.equals("true") || value.equals("1") || value.equals("yes") || value.equals("si") || value.equals("sí") || value.equals("on");
    }

    private void sendTwoHandedAbilityLockBlockedFeedback(Player player) {
        if (player == null || twoHandedAbilityLockBlockedMessage == null || twoHandedAbilityLockBlockedMessage.isBlank()) return;
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        long last = twoHandedAbilityLockLastBlockedMessage.getOrDefault(id, 0L);
        if (twoHandedAbilityLockBlockedMessageCooldownMs > 0L && now - last < twoHandedAbilityLockBlockedMessageCooldownMs) return;
        twoHandedAbilityLockLastBlockedMessage.put(id, now);
        player.sendMessage(prefix + twoHandedAbilityLockBlockedMessage);
    }

    private boolean chargeAbilityCustomDurability(Player player) {
        if (!abilityDurabilityCostEnabled || abilityDurabilityCostAmount <= 0) return true;
        if (player == null) return true;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return true;

        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItemInMainHand();
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return true;

        // Evita incluso entrar al cobro manual de habilidades. El listener del evento
        // de MMOItems también lo cubre, pero esta comprobación protege items viejos
        // que hayan quedado accidentalmente en 0 de durabilidad.
        if (isCustomDurabilityProtected(item)) return true;

        if (abilityDurabilityOnlyCustomDurability && !hasMmoCustomDurability(item)) return true;

        UUID playerId = player.getUniqueId();
        String identity = getAbilityDurabilityItemIdentity(item);
        long now = System.currentTimeMillis();
        AbilityDurabilityCharge lastCharge = abilityDurabilityLastCharge.get(playerId);
        if (lastCharge != null
                && lastCharge.itemIdentity.equals(identity)
                && abilityDurabilityDedupeWindowMs > 0L
                && now - lastCharge.createdAtMs <= abilityDurabilityDedupeWindowMs) {
            return true;
        }

        Boolean charged = chargeAbilityCustomDurabilityWithMmoItems(player, item);
        if (charged != null) {
            if (charged) {
                abilityDurabilityLastCharge.put(playerId, new AbilityDurabilityCharge(identity, now));
            }
            return charged;
        }

        // Fallback mínimo: si por alguna razón no se pudo enganchar la API de durabilidad,
        // no cancela ni rompe habilidades. Así no deja armas inutilizables por incompatibilidad.
        if (debug) getLogger().warning("No pude conectar con la API de durabilidad custom de MMOItems para cobrar habilidad.");
        return true;
    }

    private Boolean chargeAbilityCustomDurabilityWithMmoItems(Player player, ItemStack item) {
        if (!ensureMmoDurabilityReflection()) return null;

        try {
            Object durabilityItem;
            if (mmoDurabilityConstructorItemStack != null) {
                durabilityItem = mmoDurabilityConstructorItemStack.newInstance(player, item);
            } else if (mmoDurabilityConstructorNbt != null) {
                Object nbt = mmoNbtGetMethod.invoke(null, item);
                durabilityItem = mmoDurabilityConstructorNbt.newInstance(player, nbt, EquipmentSlot.HAND);
            } else {
                return null;
            }

            if (mmoDurabilityIsValidMethod != null) {
                Object valid = mmoDurabilityIsValidMethod.invoke(durabilityItem);
                if (valid instanceof Boolean bool && !bool) return true;
            }

            int current = -1;
            if (mmoDurabilityGetDurabilityMethod != null) {
                Object value = mmoDurabilityGetDurabilityMethod.invoke(durabilityItem);
                if (value instanceof Number number) current = number.intValue();
            }

            if (current == 0) return false;

            if (mmoDurabilityDecreaseMethod == null || mmoDurabilityToItemMethod == null) return null;

            mmoDurabilityDecreaseMethod.invoke(durabilityItem, abilityDurabilityCostAmount);
            Object result = mmoDurabilityToItemMethod.invoke(durabilityItem);

            if (result == null) {
                player.getInventory().setItemInMainHand(null);
            } else if (result instanceof ItemStack newItem) {
                player.getInventory().setItemInMainHand(newItem);
            } else {
                return null;
            }
            return true;
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("Error cobrando durabilidad custom por habilidad: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return null;
        }
    }

    private boolean hasMmoCustomDurability(ItemStack item) {
        Integer max = readMmoItemInteger(item, "MMOITEMS_MAX_DURABILITY");
        return max != null && max > 0;
    }

    private String getAbilityDurabilityItemIdentity(ItemStack item) {
        String type = readMmoItemTypeId(item);
        String id = readMmoItemString(item, "MMOITEMS_ITEM_ID");
        if (type != null && id != null) return type + ":" + id;
        return fallbackItemSignature(item);
    }

    private boolean ensureMmoDurabilityReflection() {
        if (mmoDurabilityReflectionTried) return mmoDurabilityDecreaseMethod != null && mmoDurabilityToItemMethod != null;
        mmoDurabilityReflectionTried = true;

        if (!ensureMmoNbtReflection()) return false;

        // MMOItems moderno: DurabilityItem(Player, ItemStack) con decreaseDurability() y toItem().
        try {
            Class<?> durabilityClass = Class.forName("net.Indyuce.mmoitems.api.interaction.util.DurabilityItem");
            Constructor<?> constructor = durabilityClass.getConstructor(Player.class, ItemStack.class);
            Method decrease = findMethodInHierarchy(durabilityClass, "decreaseDurability", int.class);
            Method toItem = findMethodInHierarchy(durabilityClass, "toItem");
            Method isValid = findMethodInHierarchy(durabilityClass, "isValid");
            Method getDurability = findMethodInHierarchy(durabilityClass, "getDurability");

            if (decrease != null && toItem != null) {
                mmoDurabilityConstructorItemStack = constructor;
                mmoDurabilityDecreaseMethod = decrease;
                mmoDurabilityToItemMethod = toItem;
                mmoDurabilityIsValidMethod = isValid;
                mmoDurabilityGetDurabilityMethod = getDurability;
                debug("DurabilityItem de MMOItems detectado.");
                return true;
            }
        } catch (Throwable ignored) {
        }

        // MMOItems alternativo: CustomDurabilityItem(Player, NBTItem, EquipmentSlot).
        try {
            Class<?> customClass = Class.forName("net.Indyuce.mmoitems.api.interaction.util.CustomDurabilityItem");
            Constructor<?> constructor = customClass.getConstructor(Player.class, mmoNbtItemClass, EquipmentSlot.class);
            Method decrease = findMethodInHierarchy(customClass, "decreaseDurability", int.class);
            if (decrease == null) decrease = findMethodInHierarchy(customClass, "onDurabilityDecrease", int.class);
            Method toItem = findMethodInHierarchy(customClass, "toItem");
            if (toItem == null) toItem = findMethodInHierarchy(customClass, "applyChanges");
            Method isValid = findMethodInHierarchy(customClass, "isValid");
            Method getDurability = findMethodInHierarchy(customClass, "getDurability");

            if (decrease != null && toItem != null) {
                mmoDurabilityConstructorNbt = constructor;
                mmoDurabilityDecreaseMethod = decrease;
                mmoDurabilityToItemMethod = toItem;
                mmoDurabilityIsValidMethod = isValid;
                mmoDurabilityGetDurabilityMethod = getDurability;
                debug("CustomDurabilityItem de MMOItems detectado.");
                return true;
            }
        } catch (Throwable ignored) {
        }

        if (debug) getLogger().warning("No encontré clases compatibles de durabilidad custom de MMOItems.");
        return false;
    }

    private Method findMethodInHierarchy(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
            }
            try {
                Method method = current.getMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
            }
            current = current.getSuperclass();
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


    private Boolean readMmoItemBoolean(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR || key == null || key.isBlank()) return null;
        if (!ensureMmoNbtReflection() || mmoNbtGetBooleanMethod == null) return null;

        try {
            Object nbt = mmoNbtGetMethod.invoke(null, item);
            if (nbt == null) return null;
            Object value = mmoNbtGetBooleanMethod.invoke(nbt, key);
            if (value instanceof Boolean bool) return bool;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String readMmoItemStringRaw(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR || key == null || key.isBlank()) return null;
        if (!ensureMmoNbtReflection() || mmoNbtGetStringMethod == null) return null;

        try {
            Object nbt = mmoNbtGetMethod.invoke(null, item);
            if (nbt == null) return null;
            Object value = mmoNbtGetStringMethod.invoke(nbt, key);
            if (value == null) return null;
            String string = String.valueOf(value).trim();
            return string.isBlank() ? null : string;
        } catch (Throwable ignored) {
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

    private Integer readMmoItemInteger(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR || key == null || key.isBlank()) return null;
        if (!ensureMmoNbtReflection() || mmoNbtGetIntegerMethod == null) return null;

        try {
            Object nbt = mmoNbtGetMethod.invoke(null, item);
            if (nbt == null) return null;
            if (mmoNbtHasTagMethod != null) {
                Object hasTag = mmoNbtHasTagMethod.invoke(nbt, key);
                if (hasTag instanceof Boolean bool && !bool) return null;
            }
            Object value = mmoNbtGetIntegerMethod.invoke(nbt, key);
            if (value instanceof Number number) return number.intValue();
            return null;
        } catch (Throwable ignored) {
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
                Method getBoolean = null;
                try {
                    getBoolean = clazz.getMethod("getBoolean", String.class);
                } catch (Throwable ignored) {
                    // Algunas versiones no exponen lectura booleana directa.
                }
                Method getInteger = null;
                try {
                    getInteger = clazz.getMethod("getInteger", String.class);
                } catch (Throwable ignored) {
                    // Algunas versiones no exponen lectura entera directa.
                }
                Method hasTag = null;
                try {
                    hasTag = clazz.getMethod("hasTag", String.class);
                } catch (Throwable ignored) {
                    // Algunas versiones no exponen hasTag.
                }

                mmoNbtItemClass = clazz;
                mmoNbtGetMethod = get;
                mmoNbtHasTypeMethod = hasType;
                mmoNbtGetTypeMethod = getType;
                mmoNbtGetStringMethod = getString;
                mmoNbtGetBooleanMethod = getBoolean;
                mmoNbtGetIntegerMethod = getInteger;
                mmoNbtHasTagMethod = hasTag;
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

            Matcher shape = miningShapePattern.matcher(clean);
            if (shape.find()) result.miningShape = parseMiningShapeSpec(shape.group(1));

            Matcher cosecha = cosechaPattern.matcher(clean);
            if (cosecha.find()) result.multiCosecha = Math.max(result.multiCosecha, parseIntSafe(cosecha.group(1)));

            if (clean.contains(autoReplantKey)) result.autoReplantar = true;
        }

        return result;
    }

    private EquipmentBonuses readEquipmentBonuses(Player player) {
        EquipmentBonuses result = new EquipmentBonuses();
        if (player == null || !equipmentBonusesEnabled) return result;

        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor != null) {
            for (ItemStack piece : armor) {
                readEquipmentBonusLore(piece, result, true, true, true);
            }
        }

        if (mainHandToolBonusesEnabled) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType() != Material.AIR) {
                Material material = mainHand.getType();
                boolean agriculture = !mainHandToolRequireMatchingType || isHoe(material);
                boolean minerals = !mainHandToolRequireMatchingType || isPickaxe(material);
                boolean treeNodes = !mainHandToolRequireMatchingType || isAxe(material);
                readEquipmentBonusLore(mainHand, result, agriculture, minerals, treeNodes);
            }
        }

        result.cap(maxEquipmentBonusPercent);
        return result;
    }

    private EquipmentBonuses readArmorBonuses(Player player) {
        EquipmentBonuses result = new EquipmentBonuses();
        if (player == null || !equipmentBonusesEnabled) return result;
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor != null) {
            for (ItemStack piece : armor) readEquipmentBonusLore(piece, result, true, true, true);
        }
        result.cap(maxEquipmentBonusPercent);
        return result;
    }

    private EquipmentBonuses readMainHandToolBonuses(Player player) {
        EquipmentBonuses result = new EquipmentBonuses();
        if (player == null || !equipmentBonusesEnabled || !mainHandToolBonusesEnabled) return result;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType() == Material.AIR) return result;
        Material material = mainHand.getType();
        boolean agriculture = !mainHandToolRequireMatchingType || isHoe(material);
        boolean minerals = !mainHandToolRequireMatchingType || isPickaxe(material);
        boolean treeNodes = !mainHandToolRequireMatchingType || isAxe(material);
        readEquipmentBonusLore(mainHand, result, agriculture, minerals, treeNodes);
        result.cap(maxEquipmentBonusPercent);
        return result;
    }

    private EquipmentBonuses readCombinedBonuses(Player player) {
        EquipmentBonuses result = new EquipmentBonuses();
        result.add(readEquipmentBonuses(player));
        result.add(readProfessionBonuses(player));
        result.cap(maxCombinedBonusPercent);
        return result;
    }

    private void readEquipmentBonusLore(ItemStack item, EquipmentBonuses result,
                                        boolean agriculture, boolean minerals, boolean treeNodes) {
        if (item == null || item.getType() == Material.AIR || result == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return;

        for (String line : meta.getLore()) {
            String clean = normalize(ChatColor.stripColor(line));
            if (agriculture) result.agricultureRareDrops += matchPercent(agricultureRareBonusPattern, clean);
            if (minerals) result.rareMinerals += matchPercent(rareMineralsBonusPattern, clean);
            if (treeNodes) result.treeNodeExtra += matchPercent(treeNodeExtraBonusPattern, clean);
        }
    }

    private EquipmentBonuses readProfessionBonuses(Player player) {
        EquipmentBonuses result = new EquipmentBonuses();
        ProfessionSnapshot snapshot = getProfessionSnapshot(player);
        result.rareMinerals = snapshot.miningBonus;
        result.agricultureRareDrops = snapshot.farmingBonus;
        result.treeNodeExtra = snapshot.woodcuttingBonus;
        return result;
    }

    private ProfessionSnapshot getProfessionSnapshot(Player player) {
        if (player == null || !professionBonusesEnabled) return ProfessionSnapshot.EMPTY;

        long now = System.currentTimeMillis();
        ProfessionSnapshot cached = professionBonusCache.get(player.getUniqueId());
        if (cached != null && professionBonusCacheMs > 0L
                && now - cached.createdAtMs <= professionBonusCacheMs) {
            return cached;
        }

        // Algunos plugins resuelven placeholders fuera del hilo principal. En ese caso
        // devolvemos el último valor seguro en caché y no tocamos datos vivos de MMOCore.
        if (!Bukkit.isPrimaryThread()) return cached == null ? ProfessionSnapshot.EMPTY : cached;

        int miningLevel = getMMOCoreProfessionLevel(player, miningProfessionId);
        int farmingLevel = getMMOCoreProfessionLevel(player, farmingProfessionId);
        int woodcuttingLevel = getMMOCoreProfessionLevel(player, woodcuttingProfessionId);

        ProfessionSnapshot snapshot = new ProfessionSnapshot(
                now,
                miningLevel,
                farmingLevel,
                woodcuttingLevel,
                professionBonusForLevel(miningLevel, miningBonusPerLevel),
                professionBonusForLevel(farmingLevel, farmingBonusPerLevel),
                professionBonusForLevel(woodcuttingLevel, woodcuttingBonusPerLevel)
        );
        professionBonusCache.put(player.getUniqueId(), snapshot);
        return snapshot;
    }

    private double professionBonusForLevel(int level, double bonusPerLevel) {
        int countedLevels = professionBonusCountStartingLevel ? Math.max(0, level) : Math.max(0, level - 1);
        return Math.min(professionBonusMaxPercent, countedLevels * Math.max(0.0, bonusPerLevel));
    }

    private int getMMOCoreProfessionLevel(Player player, String professionId) {
        if (player == null || professionId == null || professionId.isBlank()) return 0;
        if (!initializeMMOCoreReflection()) return 0;

        try {
            if (mmocorePlayerDataHasMethod != null) {
                Object hasArgument = mmocoreLookupArgument(player, mmocorePlayerDataHasLookupType);
                Object loaded = mmocorePlayerDataHasMethod.invoke(null, hasArgument);
                if (loaded instanceof Boolean bool && !bool) return 0;
            }

            Object getArgument = mmocoreLookupArgument(player, mmocorePlayerDataGetLookupType);
            Object playerData = mmocorePlayerDataGetMethod.invoke(null, getArgument);
            if (playerData == null) return 0;

            Object professions = mmocoreGetCollectionSkillsMethod.invoke(playerData);
            if (professions == null) return 0;

            Object value = mmocoreGetProfessionLevelMethod.invoke(professions, professionId);
            return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
        } catch (Throwable throwable) {
            debug("No se pudo leer la profesión '" + professionId + "' de " + player.getName() + ": " + throwable.getClass().getSimpleName());
            return 0;
        }
    }

    private boolean initializeMMOCoreReflection() {
        if (mmocoreReflectionTried) return mmocorePlayerDataGetMethod != null
                && mmocoreGetCollectionSkillsMethod != null && mmocoreGetProfessionLevelMethod != null;
        mmocoreReflectionTried = true;

        Plugin mmocore = mmocorePluginName == null ? null : Bukkit.getPluginManager().getPlugin(mmocorePluginName);
        if (mmocore == null || !mmocore.isEnabled()) {
            debug("MMOCore no está cargado; bonus de profesiones desactivados temporalmente.");
            return false;
        }

        try {
            ClassLoader loader = mmocore.getClass().getClassLoader();
            Class<?> playerDataClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData", false, loader);

            for (Class<?> lookupType : List.of(Player.class, UUID.class)) {
                if (mmocorePlayerDataGetMethod == null) {
                    try {
                        mmocorePlayerDataGetMethod = playerDataClass.getMethod("get", lookupType);
                        mmocorePlayerDataGetLookupType = lookupType;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                if (mmocorePlayerDataHasMethod == null) {
                    try {
                        mmocorePlayerDataHasMethod = playerDataClass.getMethod("has", lookupType);
                        mmocorePlayerDataHasLookupType = lookupType;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }

            if (mmocorePlayerDataGetMethod == null) {
                getLogger().warning("No encontré PlayerData.get(Player/UUID) en MMOCore. Los bonus de profesión no podrán leerse.");
                return false;
            }

            mmocoreGetCollectionSkillsMethod = playerDataClass.getMethod("getCollectionSkills");
            Class<?> professionsClass = mmocoreGetCollectionSkillsMethod.getReturnType();
            mmocoreGetProfessionLevelMethod = professionsClass.getMethod("getLevel", String.class);
            return true;
        } catch (Throwable throwable) {
            getLogger().warning("No se pudo enlazar la API de MMOCore para profesiones: " + throwable.getClass().getSimpleName()
                    + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
            mmocorePlayerDataGetMethod = null;
            mmocoreGetCollectionSkillsMethod = null;
            mmocoreGetProfessionLevelMethod = null;
            return false;
        }
    }

    private Object mmocoreLookupArgument(Player player, Class<?> lookupType) {
        return lookupType == UUID.class ? player.getUniqueId() : player;
    }

    private String normalizeProfessionId(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT);
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

    private MiningShapeSpec defaultMiningShapeSpec() {
        return switch (defaultMiningShape) {
            case LINE -> new MiningShapeSpec(MiningShape.LINE, 1, 1, 1);
            case PLANE -> new MiningShapeSpec(MiningShape.PLANE,
                    Math.min(3, miningShapeMaxWidth), Math.min(3, miningShapeMaxHeight), 1);
            case CUBE -> new MiningShapeSpec(MiningShape.CUBE,
                    Math.min(3, miningShapeMaxWidth), Math.min(3, miningShapeMaxHeight), Math.min(3, miningShapeMaxDepth));
        };
    }

    private MiningShape parseMiningShapeName(String raw, MiningShape fallback) {
        String clean = normalizeLoose(raw).toLowerCase(Locale.ROOT);
        if (clean.contains("cubo") || clean.contains("cube")) return MiningShape.CUBE;
        if (clean.contains("plano") || clean.contains("plane") || clean.contains("cuadrado") || clean.contains("square")
                || clean.contains("rectangulo") || clean.contains("rectangle") || clean.contains("area")) return MiningShape.PLANE;
        if (clean.contains("linea") || clean.contains("line")) return MiningShape.LINE;
        return fallback == null ? MiningShape.LINE : fallback;
    }

    private MiningShapeSpec parseMiningShapeSpec(String raw) {
        MiningShape shape = parseMiningShapeName(raw, defaultMiningShape);
        String clean = normalizeLoose(raw).toLowerCase(Locale.ROOT);

        int width = shape == MiningShape.LINE ? 1 : 3;
        int height = shape == MiningShape.LINE ? 1 : 3;
        int depth = shape == MiningShape.CUBE ? 3 : 1;
        if (shape == MiningShape.PLANE && (clean.contains("rectangulo") || clean.contains("rectangle"))) {
            width = 4;
            height = 4;
        }

        Matcher dimensions = Pattern.compile("(\\d+)\\s*x\\s*(\\d+)(?:\\s*x\\s*(\\d+))?", Pattern.CASE_INSENSITIVE).matcher(clean);
        if (dimensions.find()) {
            width = parseIntSafe(dimensions.group(1));
            height = parseIntSafe(dimensions.group(2));
            if (shape == MiningShape.CUBE) {
                depth = dimensions.group(3) == null ? height : parseIntSafe(dimensions.group(3));
            }
        } else {
            Matcher singleDimension = Pattern.compile("\\b(\\d+)\\b").matcher(clean);
            if (singleDimension.find()) {
                int size = parseIntSafe(singleDimension.group(1));
                if (shape != MiningShape.LINE) {
                    width = size;
                    height = size;
                    if (shape == MiningShape.CUBE) depth = size;
                }
            }
        }

        width = Math.max(1, Math.min(width, miningShapeMaxWidth));
        height = Math.max(1, Math.min(height, miningShapeMaxHeight));
        depth = Math.max(1, Math.min(depth, miningShapeMaxDepth));
        return new MiningShapeSpec(shape, width, height, depth);
    }

    private boolean isHoe(Material mat) {
        return mat.name().endsWith("_HOE");
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

    private String normalizeLoose(String raw) {
        String normalized = normalize(raw);
        normalized = normalized.replace('-', ' ').replace('_', ' ');
        return normalized.trim().replaceAll("\\s+", " ");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTpaWarmupMove(PlayerMoveEvent event) {
        if (!tpaEnabled || !tpaCancelWarmupOnMove || event.getTo() == null) return;
        TpaWarmup warmup = tpaWarmups.get(event.getPlayer().getUniqueId());
        if (warmup == null) return;

        // Durante el breve margen inicial el jugador puede terminar de detenerse
        // y leer que la solicitud fue aceptada sin cancelar accidentalmente el TPA.
        if (System.currentTimeMillis() < warmup.movementLockedFromMs) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == to.getWorld()
                && from.getX() == to.getX()
                && from.getY() == to.getY()
                && from.getZ() == to.getZ()) return;

        cancelTpaWarmup(warmup, "warmup-cancelled-move", "&cEl teletransporte fue cancelado porque te moviste.", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTpaWarmupDamage(EntityDamageEvent event) {
        if (!tpaEnabled || !tpaCancelWarmupOnDamage || !(event.getEntity() instanceof Player player)) return;
        if (event.getFinalDamage() <= 0.0) return;
        TpaWarmup warmup = tpaWarmups.get(player.getUniqueId());
        if (warmup == null) return;
        cancelTpaWarmup(warmup, "warmup-cancelled-damage", "&cEl teletransporte fue cancelado porque recibiste daño.", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTpaWarmupAttack(EntityDamageByEntityEvent event) {
        if (!tpaEnabled || !tpaCancelWarmupOnAttack) return;
        Player attacker = getAttackingPlayer(event.getDamager());
        if (attacker == null) return;
        TpaWarmup warmup = tpaWarmups.get(attacker.getUniqueId());
        if (warmup == null) return;
        cancelTpaWarmup(warmup, "warmup-cancelled-attack", "&cEl teletransporte fue cancelado porque atacaste.", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTpaTemporaryInvulnerability(EntityDamageEvent event) {
        if (!tpaEnabled || !(event.getEntity() instanceof Player player)) return;
        Long until = tpaInvulnerableUntil.get(player.getUniqueId());
        if (until == null) return;
        if (System.currentTimeMillis() >= until) {
            tpaInvulnerableUntil.remove(player.getUniqueId());
            return;
        }
        String cause = event.getCause().name();
        if (Set.of("VOID", "SUICIDE", "KILL", "WORLD_BORDER").contains(cause)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTpaInvulnerabilityAttack(EntityDamageByEntityEvent event) {
        if (!tpaEnabled || !tpaInvulnerabilityCancelOnAttack) return;
        Player attacker = getAttackingPlayer(event.getDamager());
        if (attacker == null) return;
        Long until = tpaInvulnerableUntil.get(attacker.getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            tpaInvulnerableUntil.remove(attacker.getUniqueId());
            sendTpa(attacker, "invulnerability-ended-on-attack", "&cTu protección temporal terminó al atacar.", Map.of());
        }
    }

    private Player getAttackingPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private boolean handleTpaCommand(CommandSender sender, Command command, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + color("&cEste comando solo puede usarlo un jugador."));
            return true;
        }
        if (!tpaEnabled) {
            sendTpa(player, "disabled", "&cEl sistema de TPA está desactivado.", Map.of());
            return true;
        }

        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("tpaccept") || name.equals("aceptartp")) return acceptLatestTpa(player);
        if (name.equals("tpdeny") || name.equals("rechazartp")) return denyLatestTpa(player);
        if (name.equals("tpacancel")) return cancelOutgoingTpa(player);

        if (args.length == 0) {
            sendTpaHelp(player);
            return true;
        }

        String sub = normalizeLoose(args[0]).toLowerCase(Locale.ROOT);
        if ((sub.equals("aceptarsolicitud") || sub.equals("acceptrequest")) && args.length >= 2) {
            return acceptSpecificTpa(player, args[1]);
        }
        if ((sub.equals("rechazarsolicitud") || sub.equals("denyrequest")) && args.length >= 2) {
            return denySpecificTpa(player, args[1]);
        }
        if (sub.equals("aceptar") || sub.equals("accept")) return acceptLatestTpa(player);
        if (sub.equals("rechazar") || sub.equals("deny") || sub.equals("denegar")) return denyLatestTpa(player);
        if (sub.equals("cancelar") || sub.equals("cancel")) return cancelOutgoingTpa(player);
        if (sub.equals("ayuda") || sub.equals("help")) {
            sendTpaHelp(player);
            return true;
        }
        return sendTpaRequest(player, args[0]);
    }

    private boolean sendTpaRequest(Player sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            sendTpa(sender, "player-not-found", "&cJugador no encontrado o desconectado.", Map.of("target", targetName));
            return true;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sendTpa(sender, "cannot-self", "&cNo puedes enviarte una solicitud a ti mismo.", Map.of());
            return true;
        }
        if (!tpaAllowCrossWorld && !target.getWorld().equals(sender.getWorld())) {
            sendTpa(sender, "cross-world-disabled", "&cNo puedes usar TPA entre mundos distintos.", Map.of());
            return true;
        }
        long now = System.currentTimeMillis();
        long cooldown = tpaCooldownUntil.getOrDefault(sender.getUniqueId(), 0L);
        if (cooldown > now) {
            long seconds = Math.max(1L, (cooldown - now + 999L) / 1000L);
            sendTpa(sender, "cooldown", "&cDebes esperar &f%seconds%s &cantes de enviar otra solicitud.", Map.of("seconds", String.valueOf(seconds)));
            return true;
        }

        TpaRequest old = tpaOutgoing.get(sender.getUniqueId());
        if (old != null) removeTpaRequest(old);

        TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), now, now + tpaRequestTimeoutSeconds * 1000L);
        tpaOutgoing.put(sender.getUniqueId(), request);
        tpaIncoming.computeIfAbsent(target.getUniqueId(), id -> new LinkedHashMap<>()).put(sender.getUniqueId(), request);
        tpaCooldownUntil.put(sender.getUniqueId(), now + tpaCooldownSeconds * 1000L);

        sendTpa(sender, "request-sent", "&eSolicitud de teletransporte enviada a &6%target%&e. &7Expira en &f%seconds% segundos&7.", Map.of(
                "target", target.getName(),
                "seconds", String.valueOf(tpaRequestTimeoutSeconds)
        ));
        sendClickableTpaRequest(target, sender, request);

        Bukkit.getScheduler().runTaskLater(this, () -> expireTpaRequest(request), tpaRequestTimeoutSeconds * 20L);
        return true;
    }

    private void sendClickableTpaRequest(Player target, Player requester, TpaRequest request) {
        String raw = getConfig().getString("tpa.messages.request-received", "&6%player% &equiere teletransportarse hacia ti. &7Expira en &f%seconds% segundos&7.");
        raw = raw.replace("%player%", requester.getName())
                .replace("%seconds%", String.valueOf(tpaRequestTimeoutSeconds));
        target.sendMessage(prefix + color(raw));

        String requesterId = request.senderId.toString();
        Component accept = Component.text("[ACEPTAR]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpa aceptarsolicitud " + requesterId))
                .hoverEvent(HoverEvent.showText(Component.text("Aceptar esta solicitud de " + requester.getName(), NamedTextColor.GREEN)));
        Component spacer = Component.text("       ");
        Component deny = Component.text("[RECHAZAR]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpa rechazarsolicitud " + requesterId))
                .hoverEvent(HoverEvent.showText(Component.text("Rechazar esta solicitud de " + requester.getName(), NamedTextColor.RED)));
        target.sendMessage(Component.text("        ").append(accept).append(spacer).append(deny));
    }

    private boolean acceptSpecificTpa(Player target, String senderUuidText) {
        TpaRequest request = findSpecificTpaRequest(target, senderUuidText);
        if (request == null) return true;
        return acceptTpaRequest(target, request);
    }

    private boolean denySpecificTpa(Player target, String senderUuidText) {
        TpaRequest request = findSpecificTpaRequest(target, senderUuidText);
        if (request == null) return true;
        return denyTpaRequest(target, request);
    }

    private TpaRequest findSpecificTpaRequest(Player target, String senderUuidText) {
        UUID senderId;
        try {
            senderId = UUID.fromString(senderUuidText);
        } catch (IllegalArgumentException exception) {
            sendTpa(target, "request-not-valid", "&cEsa solicitud ya no es válida o ha expirado.", Map.of());
            return null;
        }

        LinkedHashMap<UUID, TpaRequest> requests = tpaIncoming.get(target.getUniqueId());
        TpaRequest request = requests == null ? null : requests.get(senderId);
        if (request == null || request.targetId.equals(target.getUniqueId()) == false) {
            sendTpa(target, "request-not-valid", "&cEsa solicitud ya no es válida o ha expirado.", Map.of());
            return null;
        }
        if (request.expiresAtMs <= System.currentTimeMillis()) {
            removeTpaRequest(request);
            sendTpa(target, "request-not-valid", "&cEsa solicitud ya no es válida o ha expirado.", Map.of());
            return null;
        }
        return request;
    }

    private boolean acceptLatestTpa(Player target) {
        TpaRequest request = latestValidRequest(target.getUniqueId());
        if (request == null) {
            sendTpa(target, "no-pending-request", "&cNo tienes solicitudes de teletransporte pendientes.", Map.of());
            return true;
        }
        return acceptTpaRequest(target, request);
    }

    private boolean acceptTpaRequest(Player target, TpaRequest request) {
        Player requester = Bukkit.getPlayer(request.senderId);
        if (requester == null || !requester.isOnline()) {
            removeTpaRequest(request);
            sendTpa(target, "requester-offline", "&cEl jugador que envió la solicitud ya no está conectado.", Map.of());
            return true;
        }
        if (!tpaAllowCrossWorld && !target.getWorld().equals(requester.getWorld())) {
            removeTpaRequest(request);
            sendTpa(target, "cross-world-disabled", "&cNo puedes usar TPA entre mundos distintos.", Map.of());
            return true;
        }

        removeTpaRequest(request);
        sendTpa(target, "request-accepted-target", "&aAceptaste la solicitud de &f%player%&a.", Map.of("player", requester.getName()));

        if (tpaTeleportDelaySeconds <= 0) {
            completeTpaTeleport(requester, target);
            return true;
        }

        TpaWarmup previous = tpaWarmups.get(requester.getUniqueId());
        if (previous != null) cancelTpaWarmup(previous, null, null, false);

        long movementLockedFromMs = System.currentTimeMillis() + tpaMovementGraceSeconds * 1000L;
        TpaWarmup warmup = new TpaWarmup(
                requester.getUniqueId(),
                target.getUniqueId(),
                requester.getLocation().clone(),
                movementLockedFromMs
        );
        tpaWarmups.put(requester.getUniqueId(), warmup);

        int movementLockSeconds = Math.max(0, tpaTeleportDelaySeconds - tpaMovementGraceSeconds);
        sendTpa(requester, "warmup-started",
                "&eTeletransporte aceptado. Tienes &f%grace_seconds%s &epara detenerte; después no te muevas durante &f%movement_lock_seconds%s&e. Entrar en combate cancela la espera.",
                Map.of(
                        "seconds", String.valueOf(tpaTeleportDelaySeconds),
                        "grace_seconds", String.valueOf(tpaMovementGraceSeconds),
                        "movement_lock_seconds", String.valueOf(movementLockSeconds),
                        "target", target.getName()
                ));
        sendTpa(target, "warmup-started-target", "&7%player% se teletransportará hacia ti en &f%seconds% segundos&7.", Map.of(
                "seconds", String.valueOf(tpaTeleportDelaySeconds),
                "player", requester.getName()
        ));

        warmup.task = Bukkit.getScheduler().runTaskLater(this, () -> finishTpaWarmup(warmup), tpaTeleportDelaySeconds * 20L);
        return true;
    }

    private void finishTpaWarmup(TpaWarmup warmup) {
        if (tpaWarmups.get(warmup.requesterId) != warmup) return;
        tpaWarmups.remove(warmup.requesterId);

        Player requester = Bukkit.getPlayer(warmup.requesterId);
        Player target = Bukkit.getPlayer(warmup.targetId);
        if (requester == null || !requester.isOnline()) return;
        if (target == null || !target.isOnline()) {
            sendTpa(requester, "target-disconnected", "&cEl jugador objetivo se desconectó.", Map.of());
            return;
        }
        if (!tpaAllowCrossWorld && !target.getWorld().equals(requester.getWorld())) {
            sendTpa(requester, "cross-world-disabled", "&cNo puedes usar TPA entre mundos distintos.", Map.of());
            return;
        }
        completeTpaTeleport(requester, target);
    }

    private void completeTpaTeleport(Player requester, Player target) {
        boolean teleported = requester.teleport(target.getLocation());
        if (!teleported) {
            sendTpa(requester, "teleport-failed", "&cNo se pudo completar el teletransporte.", Map.of());
            sendTpa(target, "teleport-failed", "&cNo se pudo completar el teletransporte.", Map.of());
            return;
        }

        sendTpa(requester, "request-accepted", "&aTeletransporte completado.", Map.of("target", target.getName()));
        if (tpaInvulnerabilitySeconds > 0) {
            tpaInvulnerableUntil.put(requester.getUniqueId(), System.currentTimeMillis() + tpaInvulnerabilitySeconds * 1000L);
            sendTpa(requester, "invulnerable", "&aTienes protección temporal durante &f%seconds%s&a.", Map.of("seconds", String.valueOf(tpaInvulnerabilitySeconds)));
        }
    }

    private void cancelTpaWarmup(TpaWarmup warmup, String messageKey, String fallback, boolean notifyTarget) {
        if (tpaWarmups.get(warmup.requesterId) != warmup) return;
        tpaWarmups.remove(warmup.requesterId);
        if (warmup.task != null) warmup.task.cancel();

        Player requester = Bukkit.getPlayer(warmup.requesterId);
        Player target = Bukkit.getPlayer(warmup.targetId);
        if (requester != null && messageKey != null && fallback != null) {
            sendTpa(requester, messageKey, fallback, Map.of());
        }
        if (notifyTarget && target != null) {
            sendTpa(target, "warmup-cancelled-target", "&7El teletransporte de &f%player% &7fue cancelado.", Map.of(
                    "player", requester == null ? "un jugador" : requester.getName()
            ));
        }
    }

    private void cancelTpaWarmupsFor(UUID playerId, boolean notifyOther) {
        TpaWarmup own = tpaWarmups.get(playerId);
        if (own != null) cancelTpaWarmup(own, null, null, notifyOther);

        for (TpaWarmup warmup : new ArrayList<>(tpaWarmups.values())) {
            if (!warmup.targetId.equals(playerId)) continue;
            tpaWarmups.remove(warmup.requesterId, warmup);
            if (warmup.task != null) warmup.task.cancel();
            Player requester = Bukkit.getPlayer(warmup.requesterId);
            if (notifyOther && requester != null) {
                sendTpa(requester, "target-disconnected", "&cEl jugador objetivo se desconectó.", Map.of());
            }
        }
    }

    private boolean denyLatestTpa(Player target) {
        TpaRequest request = latestValidRequest(target.getUniqueId());
        if (request == null) {
            sendTpa(target, "no-pending-request", "&cNo tienes solicitudes de teletransporte pendientes.", Map.of());
            return true;
        }
        return denyTpaRequest(target, request);
    }

    private boolean denyTpaRequest(Player target, TpaRequest request) {
        Player requester = Bukkit.getPlayer(request.senderId);
        removeTpaRequest(request);
        sendTpa(target, "request-denied-target", "&eRechazaste la solicitud de &f%player%&e.", Map.of("player", requester == null ? "jugador" : requester.getName()));
        if (requester != null) sendTpa(requester, "request-denied", "&cSolicitud de teletransporte rechazada.", Map.of("target", target.getName()));
        return true;
    }

    private boolean cancelOutgoingTpa(Player sender) {
        TpaRequest request = tpaOutgoing.get(sender.getUniqueId());
        if (request == null) {
            sendTpa(sender, "no-outgoing-request", "&cNo tienes una solicitud enviada pendiente.", Map.of());
            return true;
        }
        Player target = Bukkit.getPlayer(request.targetId);
        removeTpaRequest(request);
        sendTpa(sender, "request-cancelled", "&eCancelaste tu solicitud de teletransporte.", Map.of());
        if (target != null) sendTpa(target, "request-cancelled-target", "&7%player% canceló su solicitud de teletransporte.", Map.of("player", sender.getName()));
        return true;
    }

    private TpaRequest latestValidRequest(UUID targetId) {
        LinkedHashMap<UUID, TpaRequest> requests = tpaIncoming.get(targetId);
        if (requests == null || requests.isEmpty()) return null;
        long now = System.currentTimeMillis();
        TpaRequest latest = null;
        for (TpaRequest request : new ArrayList<>(requests.values())) {
            if (request.expiresAtMs <= now) {
                removeTpaRequest(request);
                continue;
            }
            latest = request;
        }
        return latest;
    }

    private void expireTpaRequest(TpaRequest request) {
        TpaRequest current = tpaOutgoing.get(request.senderId);
        if (current != request || System.currentTimeMillis() < request.expiresAtMs) return;
        Player sender = Bukkit.getPlayer(request.senderId);
        Player target = Bukkit.getPlayer(request.targetId);
        removeTpaRequest(request);
        if (sender != null) sendTpa(sender, "request-expired", "&cLa solicitud de teletransporte ha expirado.", Map.of());
        if (target != null) sendTpa(target, "request-expired-target", "&7La solicitud de &f%player% &7ha expirado.", Map.of("player", sender == null ? "un jugador" : sender.getName()));
    }

    private void removeTpaRequest(TpaRequest request) {
        if (request == null) return;
        tpaOutgoing.remove(request.senderId, request);
        LinkedHashMap<UUID, TpaRequest> requests = tpaIncoming.get(request.targetId);
        if (requests != null) {
            requests.remove(request.senderId, request);
            if (requests.isEmpty()) tpaIncoming.remove(request.targetId);
        }
    }

    private void removeTpaRequestsFor(UUID playerId, boolean notifyOthers) {
        TpaRequest outgoing = tpaOutgoing.get(playerId);
        if (outgoing != null) {
            Player target = Bukkit.getPlayer(outgoing.targetId);
            removeTpaRequest(outgoing);
            if (notifyOthers && target != null) sendTpa(target, "request-cancelled-disconnect", "&7La solicitud fue cancelada porque el jugador se desconectó.", Map.of());
        }
        LinkedHashMap<UUID, TpaRequest> incoming = tpaIncoming.get(playerId);
        if (incoming != null) {
            for (TpaRequest request : new ArrayList<>(incoming.values())) {
                Player sender = Bukkit.getPlayer(request.senderId);
                removeTpaRequest(request);
                if (notifyOthers && sender != null) sendTpa(sender, "target-disconnected", "&cEl jugador objetivo se desconectó.", Map.of());
            }
        }
    }

    private void sendTpaHelp(Player player) {
        player.sendMessage(color("&6&lTPA &7comandos:"));
        player.sendMessage(color("&e/tpa <jugador> &7- Solicita teletransportarte."));
        player.sendMessage(color("&e/tpa aceptar &7- Acepta la solicitud más reciente."));
        player.sendMessage(color("&e/tpa rechazar &7- Rechaza la solicitud más reciente."));
        player.sendMessage(color("&e/tpa cancelar &7- Cancela tu solicitud enviada."));
    }

    private void sendTpa(Player player, String key, String fallback, Map<String, String> replacements) {
        String raw = getConfig().getString("tpa.messages." + key, fallback);
        if (raw == null || raw.isBlank()) return;
        for (Map.Entry<String, String> entry : replacements.entrySet()) raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        player.sendMessage(prefix + color(raw));
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    private void debug(String msg) {
        if (debug) getLogger().info("[DEBUG] " + msg);
    }

    String resolvePlaceholder(Player player, String identifier) {
        if (player == null || identifier == null || identifier.isBlank()) return "";

        String key = normalize(identifier).toLowerCase(Locale.ROOT).replace(' ', '_');
        boolean formatted = false;
        for (String suffix : List.of("_formatted", "_formateado", "_con_signo")) {
            if (key.endsWith(suffix)) {
                formatted = true;
                key = key.substring(0, key.length() - suffix.length());
                break;
            }
        }

        ProfessionKind kind = null;
        String metric = null;
        for (ProfessionKind candidate : ProfessionKind.values()) {
            for (String alias : candidate.aliases) {
                String prefix = alias + "_";
                if (key.startsWith(prefix)) {
                    kind = candidate;
                    metric = key.substring(prefix.length());
                    break;
                }
            }
            if (kind != null) break;
        }
        if (kind == null || metric == null) return null;

        ProfessionSnapshot professions = getProfessionSnapshot(player);
        if (metric.equals("level") || metric.equals("nivel")) {
            return Integer.toString(kind.level(professions));
        }

        EquipmentBonuses armor = readArmorBonuses(player);
        EquipmentBonuses tool = readMainHandToolBonuses(player);
        EquipmentBonuses equipment = new EquipmentBonuses();
        equipment.add(armor);
        equipment.add(tool);
        equipment.cap(maxEquipmentBonusPercent);

        double value;
        switch (metric) {
            case "bonus", "profession_bonus", "bonus_profesion", "bonus_profession" -> value = kind.professionBonus(professions);
            case "armor_bonus", "bonus_armadura" -> value = kind.equipmentBonus(armor);
            case "tool_bonus", "bonus_herramienta" -> value = kind.equipmentBonus(tool);
            case "equipment_bonus", "bonus_equipo" -> value = kind.equipmentBonus(equipment);
            case "total_bonus", "bonus_total" -> value = Math.min(maxCombinedBonusPercent,
                    kind.professionBonus(professions) + kind.equipmentBonus(equipment));
            default -> {
                return null;
            }
        }

        String number = formatPercentNumber(value);
        return formatted ? "+" + number + "%" : number;
    }

    private String formatPercentNumber(double value) {
        String formatted = String.format(Locale.US, "%.2f", Math.max(0.0, value));
        while (formatted.contains(".") && formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        if (formatted.endsWith(".")) formatted = formatted.substring(0, formatted.length() - 1);
        return formatted;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (Set.of("tpa", "tpaccept", "tpdeny", "aceptartp", "rechazartp", "tpacancel").contains(command.getName().toLowerCase(Locale.ROOT))) {
            return handleTpaCommand(sender, command, args);
        }
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


    private static final class IdentificationScrollDefinition {
        String typeId;
        String itemId;
        double chance;
        String maximumTier;
        boolean consumeOnSuccess;
        boolean consumeOnFailure;
        boolean consumeOnDenied;
    }

    private static final class IdentificationSound {
        boolean enabled;
        String value;
        float volume;
        float pitch;
    }

    private static final class TpaWarmup {
        final UUID requesterId;
        final UUID targetId;
        final Location startLocation;
        final long movementLockedFromMs;
        BukkitTask task;

        TpaWarmup(UUID requesterId, UUID targetId, Location startLocation, long movementLockedFromMs) {
            this.requesterId = requesterId;
            this.targetId = targetId;
            this.startLocation = startLocation;
            this.movementLockedFromMs = movementLockedFromMs;
        }
    }

    private static final class TpaRequest {
        final UUID senderId;
        final UUID targetId;
        final long createdAtMs;
        final long expiresAtMs;

        TpaRequest(UUID senderId, UUID targetId, long createdAtMs, long expiresAtMs) {
            this.senderId = senderId;
            this.targetId = targetId;
            this.createdAtMs = createdAtMs;
            this.expiresAtMs = expiresAtMs;
        }
    }

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

    private static final class AbilityDurabilityCharge {
        final String itemIdentity;
        final long createdAtMs;

        AbilityDurabilityCharge(String itemIdentity, long createdAtMs) {
            this.itemIdentity = itemIdentity == null ? "" : itemIdentity;
            this.createdAtMs = createdAtMs;
        }
    }

    private enum CustomDropCategory {
        AUTO,
        NONE,
        FARMING,
        MINING,
        WOODCUTTING
    }

    private static final class CustomDropBlockSnapshot {
        final World world;
        final Location location;
        final Material type;
        final BlockData blockData;

        CustomDropBlockSnapshot(World world, Location location, Material type, BlockData blockData) {
            this.world = world;
            this.location = location;
            this.type = type;
            this.blockData = blockData;
        }

        static CustomDropBlockSnapshot capture(Block block) {
            return new CustomDropBlockSnapshot(
                    block.getWorld(),
                    block.getLocation().clone(),
                    block.getType(),
                    block.getBlockData().clone()
            );
        }
    }

    private static final class CustomDropDefinition {
        String key;
        boolean enabled;
        CustomDropCategory category = CustomDropCategory.AUTO;
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

    private enum ProfessionKind {
        MINING(List.of("mining", "minero")),
        FARMING(List.of("farming", "agricultor")),
        WOODCUTTING(List.of("woodcutting", "lenador"));

        final List<String> aliases;

        ProfessionKind(List<String> aliases) {
            this.aliases = aliases;
        }

        int level(ProfessionSnapshot snapshot) {
            return switch (this) {
                case MINING -> snapshot.miningLevel;
                case FARMING -> snapshot.farmingLevel;
                case WOODCUTTING -> snapshot.woodcuttingLevel;
            };
        }

        double professionBonus(ProfessionSnapshot snapshot) {
            return switch (this) {
                case MINING -> snapshot.miningBonus;
                case FARMING -> snapshot.farmingBonus;
                case WOODCUTTING -> snapshot.woodcuttingBonus;
            };
        }

        double equipmentBonus(EquipmentBonuses bonuses) {
            return switch (this) {
                case MINING -> bonuses.rareMinerals;
                case FARMING -> bonuses.agricultureRareDrops;
                case WOODCUTTING -> bonuses.treeNodeExtra;
            };
        }
    }

    private static final class ProfessionSnapshot {
        static final ProfessionSnapshot EMPTY = new ProfessionSnapshot(0L, 0, 0, 0, 0.0, 0.0, 0.0);

        final long createdAtMs;
        final int miningLevel;
        final int farmingLevel;
        final int woodcuttingLevel;
        final double miningBonus;
        final double farmingBonus;
        final double woodcuttingBonus;

        ProfessionSnapshot(long createdAtMs, int miningLevel, int farmingLevel, int woodcuttingLevel,
                           double miningBonus, double farmingBonus, double woodcuttingBonus) {
            this.createdAtMs = createdAtMs;
            this.miningLevel = miningLevel;
            this.farmingLevel = farmingLevel;
            this.woodcuttingLevel = woodcuttingLevel;
            this.miningBonus = Math.max(0.0, miningBonus);
            this.farmingBonus = Math.max(0.0, farmingBonus);
            this.woodcuttingBonus = Math.max(0.0, woodcuttingBonus);
        }
    }

    private static final class EquipmentBonuses {
        static final EquipmentBonuses EMPTY = new EquipmentBonuses();

        double agricultureRareDrops = 0.0;
        double rareMinerals = 0.0;
        double treeNodeExtra = 0.0;

        void add(EquipmentBonuses other) {
            if (other == null) return;
            agricultureRareDrops += other.agricultureRareDrops;
            rareMinerals += other.rareMinerals;
            treeNodeExtra += other.treeNodeExtra;
        }

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

    private enum MiningShape {
        LINE,
        PLANE,
        CUBE
    }

    private static final class MiningShapeSpec {
        final MiningShape shape;
        final int width;
        final int height;
        final int depth;

        MiningShapeSpec(MiningShape shape, int width, int height, int depth) {
            this.shape = shape == null ? MiningShape.LINE : shape;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            this.depth = Math.max(1, depth);
        }
    }

    private static final class ToolLore {
        int talaMultiple = 0;
        int roturaMultiple = 0;
        int multiCosecha = 0;
        MiningShapeSpec miningShape;
        boolean autoReplantar = false;

        boolean hasAny() {
            return talaMultiple > 0 || roturaMultiple > 0 || multiCosecha > 0 || autoReplantar;
        }
    }
}
