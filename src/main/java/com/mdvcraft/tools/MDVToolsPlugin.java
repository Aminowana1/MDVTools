package com.mdvcraft.tools;

import com.mdvcraft.tools.fishing.FishingFightTimerListener;
import io.papermc.paper.datacomponent.DataComponentTypes;
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
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntitySnapshot;
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
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
    private FishingFightTimerListener fishingFightTimerListener;
    private Object mmocoreFishingDropExtension;

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

    // Configuración separada para todos los amuletos y efectos de offhand.
    private File amuletsFile;
    private FileConfiguration amuletsConfig;
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

    // Rendimiento de profesiones: cada 100% garantiza +1 unidad y el resto
    // se resuelve con un único roll porcentual. Los custom-drops raros siguen
    // usando su multiplicador relativo independiente.
    private boolean vanillaYieldEnabled;
    private boolean vanillaMiningYieldEnabled;
    private boolean vanillaWoodcuttingYieldEnabled;
    private boolean vanillaFarmingYieldEnabled;
    private boolean vanillaMiningRequireMatchingTool;
    private boolean vanillaWoodcuttingRequireMatchingTool;
    private boolean vanillaFarmingRequireMatchingTool;
    private boolean miningYieldDisabledBySilkTouch;
    private int maxYieldExtraUnitsPerBlock;
    private final Map<Material, Material> vanillaMiningYieldDrops = new EnumMap<>(Material.class);
    private final Map<Material, Material> vanillaFarmingYieldDrops = new EnumMap<>(Material.class);
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

    // Permite usar SWAP_ITEMS de MMOItems desde offhand y puede bloquear
    // globalmente el intercambio vanilla de manos.
    private boolean offhandSwapCastEnabled;
    private boolean offhandSwapCastBlockVanillaAlways;
    private Set<String> offhandSwapCastMmoItems = new HashSet<>();

    // Patrones event-driven de flechas adicionales para arcos y ballestas.
    // Se activan únicamente al disparar y pueden provenir del arma o del offhand.
    private boolean rangedExtraArrowsEnabled;
    private int rangedExtraArrowsMaxExtraPerShot;
    private long rangedExtraArrowsDuplicateWindowMs;
    private int rangedExtraArrowsDefaultDespawnTicks;
    private final List<RangedExtraArrowDefinition> rangedExtraArrowDefinitions = new ArrayList<>();
    private final Map<UUID, Long> rangedExtraArrowLastShot = new HashMap<>();
    private final Map<UUID, Map<String, Long>> rangedExtraArrowCooldowns = new HashMap<>();

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
    private static final long ABILITY_DURABILITY_SOURCE_HINT_TTL_MS = 250L;
    private final Map<UUID, AbilityDurabilityCharge> abilityDurabilityLastCharge = new HashMap<>();
    private final Map<UUID, AbilityDurabilitySourceHint> abilityDurabilitySourceHints = new HashMap<>();

    // Protección event-driven para la durabilidad personalizada de MMOItems.
    // Cualquier item con unbreakable: true puede conservar su durabilidad custom intacta.
    private boolean customDurabilityProtectionEnabled;
    private boolean customDurabilityProtectionUseItemMeta;
    private boolean customDurabilityProtectionUseMmoItemsNbt;
    private final Set<String> customDurabilityProtectionHookedEvents = new HashSet<>();

    // Puente visual ultraligero para PLAYER_HEAD con Custom Durability de MMOItems.
    // No toca ningún otro material y no usa tareas periódicas.
    private boolean playerHeadDurabilityBarEnabled;
    private final Set<String> playerHeadDurabilityBarHookedEvents = new HashSet<>();
    private final Set<UUID> playerHeadDurabilityBarSyncQueued = new HashSet<>();

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
    public void onLoad() {
        registerMMOCoreFishingDropExtensionOnLoad();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateLegacyYieldBonusCaps();
        mergeMissingConfigDefaults();
        ensureCustomDropsFile();
        loadSettings();
        enableMMOCoreFishingDropExtensionRuntime();
        fishingFightTimerListener = new FishingFightTimerListener(this);
        Bukkit.getPluginManager().registerEvents(fishingFightTimerListener, this);
        Bukkit.getPluginManager().registerEvents(this, this);
        registerWeaponSwapLockExternalEvents();
        registerCustomDurabilityProtectionEvent();
        registerPlayerHeadDurabilityBarExternalEvents();
        registerPlaceholderExpansion();
        if (playerHeadDurabilityBarEnabled) {
            for (Player player : Bukkit.getOnlinePlayers()) schedulePlayerHeadDurabilityBarSync(player);
        }
        getLogger().info("MDVTools activado.");
    }

    @Override
    public void onDisable() {
        disableMMOCoreFishingDropExtensionRuntime();
        if (fishingFightTimerListener != null) fishingFightTimerListener.shutdown();
        weaponSwapLockUntil.clear();
        weaponSwapLockLastBlockedMessage.clear();
        weaponSwapLastWeaponBeforeNonWeapon.clear();
        twoHandedAbilityLockLastBlockedMessage.clear();
        abilityDurabilityLastCharge.clear();
        abilityDurabilitySourceHints.clear();
        playerHeadDurabilityBarSyncQueued.clear();
        rangedExtraArrowLastShot.clear();
        rangedExtraArrowCooldowns.clear();
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


    /**
     * Registers optional MMOCore API extensions during Bukkit's load phase.
     * Reflection keeps MDVTools able to load even on installations without MMOCore.
     */
    private void registerMMOCoreFishingDropExtensionOnLoad() {
        Plugin mmocore = Bukkit.getPluginManager().getPlugin("MMOCore");
        if (mmocore == null) return;

        try {
            Class<?> type = Class.forName("com.mdvcraft.tools.fishing.mmocore.MMOCoreFishingDropExtension");
            Object extension = type.getConstructor(JavaPlugin.class).newInstance(this);
            type.getMethod("registerLoader").invoke(extension);
            mmocoreFishingDropExtension = extension;
        } catch (ReflectiveOperationException | LinkageError exception) {
            getLogger().warning("[FishingDrops] No se pudo registrar la extensión MMOCore: "
                    + exception.getClass().getSimpleName()
                    + (exception.getMessage() == null ? "" : " - " + exception.getMessage()));
        }
    }

    private void enableMMOCoreFishingDropExtensionRuntime() {
        if (mmocoreFishingDropExtension == null) return;
        try {
            mmocoreFishingDropExtension.getClass().getMethod("enableRuntime").invoke(mmocoreFishingDropExtension);
        } catch (ReflectiveOperationException | LinkageError exception) {
            getLogger().warning("[FishingDrops] No se pudo activar el listener runtime de MMOCore: "
                    + exception.getClass().getSimpleName()
                    + (exception.getMessage() == null ? "" : " - " + exception.getMessage()));
        }
    }

    private void disableMMOCoreFishingDropExtensionRuntime() {
        if (mmocoreFishingDropExtension == null) return;
        try {
            mmocoreFishingDropExtension.getClass().getMethod("disableRuntime").invoke(mmocoreFishingDropExtension);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Bukkit also unregisters this plugin's listeners on disable.
        }
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

    /**
     * Adds only missing keys from the config.yml bundled in the jar.
     * Existing server values are preserved. This lets new MDVTools versions
     * self-update config.yml without forcing the administrator to replace it.
     */
    private void mergeMissingConfigDefaults() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig();
    }

    /**
     * Migra únicamente los topes por defecto antiguos de 100% para que una instalación
     * existente pueda aprovechar el nuevo sistema acumulativo sin editar config.yml a mano.
     * Valores personalizados distintos de 100% se respetan.
     */
    private void migrateLegacyYieldBonusCaps() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.isFile()) return;

        try {
            YamlConfiguration disk = YamlConfiguration.loadConfiguration(configFile);
            if (disk.contains("yield-bonuses.system-version")) return;

            boolean changed = false;
            changed |= migrateLegacyDefaultCap(disk, "equipment-bonuses.max-total-bonus-percent");
            changed |= migrateLegacyDefaultCap(disk, "equipment-bonuses.max-combined-bonus-percent");
            changed |= migrateLegacyDefaultCap(disk, "profession-bonuses.max-profession-bonus-percent");

            if (changed) {
                disk.save(configFile);
                getLogger().info("Topes antiguos de bonus (100%) migrados a 10000% para el sistema de rendimiento acumulativo.");
            }
        } catch (Exception exception) {
            getLogger().warning("No se pudieron migrar los topes antiguos de bonus: " + exception.getMessage());
        }
    }

    private boolean migrateLegacyDefaultCap(YamlConfiguration config, String path) {
        if (config == null || path == null || !config.contains(path)) return false;
        double value = config.getDouble(path, 100.0);
        if (Math.abs(value - 100.0) > 0.000001) return false;
        config.set(path, 10000.0);
        return true;
    }

    private void loadSettings() {
        reloadConfig();
        loadAmuletsConfiguration();

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
        loadVanillaYieldSettings();
        loadIdentificationSettings();
        loadCrossbowAutoReloadSettings();
        loadWeaponSwapLockSettings();
        loadOffhandSwapCastSettings();
        loadRangedExtraArrowSettings();
        loadTwoHandedAbilityLockSettings();
        loadAbilityDurabilityCostSettings();
        loadCustomDurabilityProtectionSettings();
        loadPlayerHeadDurabilityBarSettings();
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
        maxEquipmentBonusPercent = Math.max(0.0, getConfig().getDouble("equipment-bonuses.max-total-bonus-percent", 10000.0));
        maxCombinedBonusPercent = Math.max(0.0, getConfig().getDouble("equipment-bonuses.max-combined-bonus-percent", 10000.0));
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
        professionBonusMaxPercent = Math.max(0.0, getConfig().getDouble("profession-bonuses.max-profession-bonus-percent", 10000.0));
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

    private void loadVanillaYieldSettings() {
        vanillaYieldEnabled = getConfig().getBoolean("yield-bonuses.enabled", true);
        miningYieldDisabledBySilkTouch = getConfig().getBoolean("yield-bonuses.mining-disable-with-silk-touch", true);
        maxYieldExtraUnitsPerBlock = Math.max(1, getConfig().getInt("yield-bonuses.max-extra-units-per-block", 128));

        vanillaMiningYieldEnabled = getConfig().getBoolean("yield-bonuses.vanilla.mining.enabled", true);
        vanillaWoodcuttingYieldEnabled = getConfig().getBoolean("yield-bonuses.vanilla.woodcutting.enabled", true);
        vanillaFarmingYieldEnabled = getConfig().getBoolean("yield-bonuses.vanilla.farming.enabled", true);

        vanillaMiningRequireMatchingTool = getConfig().getBoolean("yield-bonuses.vanilla.mining.require-matching-tool", true);
        vanillaWoodcuttingRequireMatchingTool = getConfig().getBoolean("yield-bonuses.vanilla.woodcutting.require-matching-tool", true);
        vanillaFarmingRequireMatchingTool = getConfig().getBoolean("yield-bonuses.vanilla.farming.require-matching-tool", false);

        vanillaMiningYieldDrops.clear();
        loadYieldMaterialMap("yield-bonuses.vanilla.mining.resources", vanillaMiningYieldDrops);

        vanillaFarmingYieldDrops.clear();
        loadYieldMaterialMap("yield-bonuses.vanilla.farming.resources", vanillaFarmingYieldDrops);
    }

    private void loadYieldMaterialMap(String path, Map<Material, Material> target) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null || target == null) return;

        for (String blockName : section.getKeys(false)) {
            Material block = Material.matchMaterial(blockName);
            Material drop = Material.matchMaterial(section.getString(blockName, ""));
            if (block == null || drop == null || drop == Material.AIR) {
                getLogger().warning("Entrada inválida en " + path + ": " + blockName + " -> " + section.getString(blockName));
                continue;
            }
            target.put(block, drop);
        }
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


    private void loadOffhandSwapCastSettings() {
        FileConfiguration cfg = amuletsConfig == null ? getConfig() : amuletsConfig;
        offhandSwapCastEnabled = cfg.getBoolean("offhand-swap-cast.enabled", true);
        offhandSwapCastBlockVanillaAlways = cfg.getBoolean(
                "offhand-swap-cast.block-vanilla-swap-always", true);
        offhandSwapCastMmoItems = new HashSet<>();

        for (String raw : cfg.getStringList("offhand-swap-cast.mmoitems")) {
            if (raw == null) continue;
            String entry = raw.trim().toUpperCase(Locale.ROOT).replace(" ", "");
            if (entry.isBlank()) continue;

            int separator = entry.indexOf(':');
            if (separator <= 0 || separator >= entry.length() - 1) {
                getLogger().warning("Entrada inválida en amulets.yml -> offhand-swap-cast.mmoitems: " + raw
                        + " (usa TIPO:ID, TIPO:* o *:ID)");
                continue;
            }

            offhandSwapCastMmoItems.add(entry);
        }

        debug("amulets.yml/offhand-swap-cast cargado: enabled=" + offhandSwapCastEnabled
                + ", always-block=" + offhandSwapCastBlockVanillaAlways
                + ", items=" + offhandSwapCastMmoItems.size());
    }

    private void loadRangedExtraArrowSettings() {
        FileConfiguration cfg = amuletsConfig == null ? getConfig() : amuletsConfig;
        rangedExtraArrowsEnabled = cfg.getBoolean("ranged-extra-arrows.enabled", false);
        rangedExtraArrowsMaxExtraPerShot = Math.max(0, Math.min(32,
                cfg.getInt("ranged-extra-arrows.max-extra-arrows-per-shot", 8)));
        rangedExtraArrowsDuplicateWindowMs = Math.max(0L,
                cfg.getLong("ranged-extra-arrows.duplicate-shot-window-ms", 35L));
        rangedExtraArrowsDefaultDespawnTicks = Math.max(0,
                cfg.getInt("ranged-extra-arrows.default-despawn-after-ticks", 100));

        rangedExtraArrowDefinitions.clear();
        ConfigurationSection patterns = cfg.getConfigurationSection("ranged-extra-arrows.patterns");
        if (patterns != null) {
            for (String key : patterns.getKeys(false)) {
                ConfigurationSection section = patterns.getConfigurationSection(key);
                if (section == null || !section.getBoolean("enabled", true)) continue;

                RangedExtraArrowDefinition definition = new RangedExtraArrowDefinition();
                definition.key = key;
                definition.source = parseRangedExtraArrowSource(section.getString("source", "OFFHAND"));
                definition.mode = parseRangedExtraArrowMode(section.getString("mode", "BURST"));
                definition.totalArrows = Math.max(2, Math.min(rangedExtraArrowsMaxExtraPerShot + 1,
                        section.getInt("total-arrows", 2)));
                definition.chancePercent = Math.max(0.0, Math.min(100.0,
                        section.getDouble("chance-percent", 100.0)));
                definition.cooldownMs = Math.max(0L, section.getLong("cooldown-ticks", 0L) * 50L);
                definition.minimumForce = Math.max(0.0, Math.min(1.0,
                        section.getDouble("minimum-force", 0.0)));
                definition.damageMultiplier = Math.max(0.0,
                        section.getDouble("damage-multiplier", 1.0));
                definition.velocityMultiplier = Math.max(0.01,
                        section.getDouble("velocity-multiplier", 1.0));
                definition.extraArrowsPickup = section.getBoolean("extra-arrows-pickup", false);
                definition.despawnAfterTicks = Math.max(0,
                        section.getInt("despawn-after-ticks", rangedExtraArrowsDefaultDespawnTicks));
                definition.burstDelayTicks = Math.max(0L,
                        section.getLong("burst.delay-ticks", 3L));
                definition.horizontalAngleStepDegrees = Math.max(0.0,
                        section.getDouble("horizontal.angle-step-degrees", 6.0));
                definition.volleyHorizontalSpreadDegrees = Math.max(0.0,
                        section.getDouble("volley.horizontal-spread-degrees", 10.0));
                definition.volleyVerticalSpreadDegrees = Math.max(0.0,
                        section.getDouble("volley.vertical-spread-degrees", 5.0));

                for (String raw : section.getStringList("mmoitems")) {
                    String selector = normalizeMmoItemSelector(raw);
                    if (selector != null) definition.mmoItems.add(selector);
                }
                if (definition.mmoItems.isEmpty()) {
                    getLogger().warning("Patrón ranged-extra-arrows sin MMOItems válidos: " + key);
                    continue;
                }

                for (String raw : section.getStringList("weapons")) {
                    if (raw == null || raw.isBlank()) continue;
                    Material material = Material.matchMaterial(raw);
                    if (material == Material.BOW || material == Material.CROSSBOW) {
                        definition.weapons.add(material);
                    } else {
                        getLogger().warning("Arma inválida en ranged-extra-arrows.patterns." + key
                                + ".weapons: " + raw + " (usa BOW o CROSSBOW)");
                    }
                }
                if (definition.weapons.isEmpty()) {
                    definition.weapons.add(Material.BOW);
                    definition.weapons.add(Material.CROSSBOW);
                }

                rangedExtraArrowDefinitions.add(definition);
            }
        }

        if (!rangedExtraArrowsEnabled || rangedExtraArrowDefinitions.isEmpty()) {
            rangedExtraArrowLastShot.clear();
            rangedExtraArrowCooldowns.clear();
        }

        debug("ranged-extra-arrows cargado: enabled=" + rangedExtraArrowsEnabled
                + ", patterns=" + rangedExtraArrowDefinitions.size()
                + ", max-extra=" + rangedExtraArrowsMaxExtraPerShot);
    }

    private RangedExtraArrowSource parseRangedExtraArrowSource(String raw) {
        if (raw == null) return RangedExtraArrowSource.OFFHAND;
        try {
            return RangedExtraArrowSource.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            getLogger().warning("source inválido en ranged-extra-arrows: " + raw
                    + " (usa WEAPON, OFFHAND o EITHER)");
            return RangedExtraArrowSource.OFFHAND;
        }
    }

    private RangedExtraArrowMode parseRangedExtraArrowMode(String raw) {
        if (raw == null) return RangedExtraArrowMode.BURST;
        try {
            return RangedExtraArrowMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            getLogger().warning("mode inválido en ranged-extra-arrows: " + raw
                    + " (usa BURST, HORIZONTAL o VOLLEY)");
            return RangedExtraArrowMode.BURST;
        }
    }

    private String normalizeMmoItemSelector(String raw) {
        if (raw == null) return null;
        String selector = raw.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (selector.isBlank()) return null;
        int separator = selector.indexOf(':');
        if (separator <= 0 || separator >= selector.length() - 1) {
            getLogger().warning("Selector MMOItems inválido: " + raw
                    + " (usa TIPO:ID, TIPO:* o *:ID)");
            return null;
        }
        return selector;
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

    private void loadPlayerHeadDurabilityBarSettings() {
        playerHeadDurabilityBarEnabled = getConfig().getBoolean("player-head-durability-bar.enabled", true);
        if (!playerHeadDurabilityBarEnabled) playerHeadDurabilityBarSyncQueued.clear();
    }

    private void loadAbilityDurabilityCostSettings() {
        abilityDurabilityCostEnabled = getConfig().getBoolean("ability-durability-cost.enabled", true);
        abilityDurabilityCostAmount = Math.max(0, getConfig().getInt("ability-durability-cost.cost", 1));
        abilityDurabilityOnlyCustomDurability = getConfig().getBoolean("ability-durability-cost.only-custom-durability", true);
        abilityDurabilityDedupeWindowMs = Math.max(0L, getConfig().getLong("ability-durability-cost.dedupe-window-ms", 75L));

        if (!abilityDurabilityCostEnabled || abilityDurabilityCostAmount <= 0) {
            abilityDurabilityLastCharge.clear();
            abilityDurabilitySourceHints.clear();
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

    private void ensureAmuletsFile() {
        amuletsFile = new File(getDataFolder(), "amulets.yml");
        if (!amuletsFile.exists()) {
            try {
                saveResource("amulets.yml", false);
            } catch (IllegalArgumentException ignored) {
                // Si el recurso no existe por alguna razón, se crea vacío abajo.
            }
        }
    }

    private void loadAmuletsConfiguration() {
        boolean firstCreation = !new File(getDataFolder(), "amulets.yml").exists();

        ensureAmuletsFile();
        if (!amuletsFile.exists()) {
            try {
                getDataFolder().mkdirs();
                amuletsFile.createNewFile();
            } catch (Exception exception) {
                getLogger().warning("No pude crear amulets.yml: " + exception.getMessage());
            }
        }

        amuletsConfig = YamlConfiguration.loadConfiguration(amuletsFile);

        // Migración automática de 1.0.20: si todavía estaban en config.yml,
        // los mueve al archivo nuevo solo durante su primera creación.
        if (firstCreation) {
            boolean migrated = false;
            migrated |= copyLegacySectionToAmulets("offhand-swap-cast");
            migrated |= copyLegacySectionToAmulets("ranged-extra-arrows");

            // La nueva versión bloquea el swap vanilla globalmente por defecto.
            if (!amuletsConfig.contains("offhand-swap-cast.block-vanilla-swap-always")) {
                amuletsConfig.set("offhand-swap-cast.block-vanilla-swap-always", true);
                migrated = true;
            }

            if (migrated) {
                try {
                    amuletsConfig.save(amuletsFile);
                    getLogger().info("Configuración de amuletos migrada automáticamente a amulets.yml.");
                } catch (Exception exception) {
                    getLogger().warning("No pude guardar la migración a amulets.yml: " + exception.getMessage());
                }
            }
        }
    }

    private boolean copyLegacySectionToAmulets(String root) {
        ConfigurationSection source = getConfig().getConfigurationSection(root);
        if (source == null) return false;

        ConfigurationSection target = amuletsConfig.getConfigurationSection(root);
        if (target == null) target = amuletsConfig.createSection(root);
        copyConfigurationSectionValues(source, target);
        return true;
    }

    private void copyConfigurationSectionValues(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child != null) {
                ConfigurationSection targetChild = target.getConfigurationSection(key);
                if (targetChild == null) targetChild = target.createSection(key);
                copyConfigurationSectionValues(child, targetChild);
            } else {
                target.set(key, source.get(key));
            }
        }
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

    /**
     * Deja que MythicLib/MMOItems procese primero el trigger SWAP_ITEMS y
     * cancela después el intercambio vanilla de manos. Con
     * block-vanilla-swap-always: true, la tecla F nunca mueve objetos,
     * aunque el jugador no tenga un amuleto configurado.
     *
     * Es completamente event-driven: solo se ejecuta al pulsar F.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onOffhandSwapCast(PlayerSwapHandItemsEvent event) {
        if (!offhandSwapCastEnabled) return;

        Player player = event.getPlayer();
        if (player == null) return;

        if (!offhandSwapCastBlockVanillaAlways) {
            if (offhandSwapCastMmoItems.isEmpty()) return;

            // En modo selectivo solo bloquea F cuando el offhand coincide
            // con uno de los MMOItems declarados en amulets.yml.
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (!isConfiguredOffhandSwapCastItem(offhand)) return;
        }

        // MythicLib/MMOItems ya pudo procesar SWAP_ITEMS en prioridades
        // anteriores. Aquí se cancela únicamente el movimiento vanilla.
        event.setCancelled(true);
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
        abilityDurabilityLastCharge.remove(id);
        abilityDurabilitySourceHints.remove(id);
        weaponSwapLastWeaponBeforeNonWeapon.remove(id);
        professionBonusCache.remove(id);
        rangedExtraArrowLastShot.remove(id);
        rangedExtraArrowCooldowns.remove(id);
        removeTpaRequestsFor(id, true);
        cancelTpaWarmupsFor(id, true);
        tpaCooldownUntil.remove(id);
        tpaInvulnerableUntil.remove(id);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRangedExtraArrowShoot(EntityShootBowEvent event) {
        if (!rangedExtraArrowsEnabled || rangedExtraArrowDefinitions.isEmpty()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof AbstractArrow originalArrow)) return;

        ItemStack weapon = event.getBow();
        if (weapon == null || (weapon.getType() != Material.BOW && weapon.getType() != Material.CROSSBOW)) return;

        RangedExtraArrowDefinition definition = findRangedExtraArrowDefinition(player, weapon);
        if (definition == null) return;
        if (event.getForce() + 1.0E-6 < definition.minimumForce) return;

        long now = System.currentTimeMillis();
        if (rangedExtraArrowsDuplicateWindowMs > 0L) {
            long last = rangedExtraArrowLastShot.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < rangedExtraArrowsDuplicateWindowMs) return;
            rangedExtraArrowLastShot.put(player.getUniqueId(), now);
        }

        Map<String, Long> playerCooldowns = null;
        if (definition.cooldownMs > 0L) {
            playerCooldowns = rangedExtraArrowCooldowns.get(player.getUniqueId());
            if (playerCooldowns != null) {
                long cooldownUntil = playerCooldowns.getOrDefault(definition.key, 0L);
                if (cooldownUntil > now) return;
            }
        }

        if (definition.chancePercent < 100.0
                && ThreadLocalRandom.current().nextDouble(100.0) >= definition.chancePercent) return;

        EntitySnapshot snapshot;
        try {
            snapshot = originalArrow.createSnapshot();
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("No pude crear snapshot de flecha para " + definition.key
                    + ": " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return;
        }
        if (snapshot == null) return;

        Vector baseVelocity = originalArrow.getVelocity().clone();
        if (baseVelocity.lengthSquared() <= 1.0E-8) return;

        if (definition.cooldownMs > 0L) {
            if (playerCooldowns == null) {
                playerCooldowns = rangedExtraArrowCooldowns.computeIfAbsent(
                        player.getUniqueId(), ignored -> new HashMap<>());
            }
            playerCooldowns.put(definition.key, now + definition.cooldownMs);
        }

        triggerRangedExtraArrows(player, originalArrow.getLocation().clone(), snapshot, baseVelocity, definition);
    }

    private RangedExtraArrowDefinition findRangedExtraArrowDefinition(Player player, ItemStack weapon) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        for (RangedExtraArrowDefinition definition : rangedExtraArrowDefinitions) {
            if (!definition.weapons.contains(weapon.getType())) continue;

            boolean matches = switch (definition.source) {
                case WEAPON -> matchesConfiguredMmoItem(weapon, definition.mmoItems);
                case OFFHAND -> matchesConfiguredMmoItem(offhand, definition.mmoItems);
                case EITHER -> matchesConfiguredMmoItem(weapon, definition.mmoItems)
                        || matchesConfiguredMmoItem(offhand, definition.mmoItems);
            };
            if (matches) return definition;
        }
        return null;
    }

    private void triggerRangedExtraArrows(Player player, Location originalLocation, EntitySnapshot snapshot,
                                          Vector baseVelocity, RangedExtraArrowDefinition definition) {
        int extraCount = Math.max(0, Math.min(rangedExtraArrowsMaxExtraPerShot,
                definition.totalArrows - 1));
        if (extraCount <= 0) return;

        List<Entity> spawned = new ArrayList<>(extraCount);
        switch (definition.mode) {
            case BURST -> spawnBurstExtraArrows(player, originalLocation, snapshot, baseVelocity,
                    definition, extraCount, spawned);
            case HORIZONTAL -> {
                for (int index = 1; index <= extraCount; index++) {
                    int step = (index + 1) / 2;
                    double sign = (index % 2 == 1) ? -1.0 : 1.0;
                    double angle = sign * step * definition.horizontalAngleStepDegrees;
                    Vector velocity = rotateHorizontal(baseVelocity, angle)
                            .multiply(definition.velocityMultiplier);
                    AbstractArrow arrow = spawnExtraArrow(snapshot, originalLocation, player, velocity, definition);
                    if (arrow != null) spawned.add(arrow);
                }
                scheduleExtraArrowCleanup(spawned, definition.despawnAfterTicks);
            }
            case VOLLEY -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int index = 0; index < extraCount; index++) {
                    double yaw = random.nextDouble(-definition.volleyHorizontalSpreadDegrees,
                            definition.volleyHorizontalSpreadDegrees + Math.ulp(definition.volleyHorizontalSpreadDegrees));
                    double pitch = random.nextDouble(-definition.volleyVerticalSpreadDegrees,
                            definition.volleyVerticalSpreadDegrees + Math.ulp(definition.volleyVerticalSpreadDegrees));
                    Vector velocity = rotateYawPitch(baseVelocity, yaw, pitch)
                            .multiply(definition.velocityMultiplier);
                    AbstractArrow arrow = spawnExtraArrow(snapshot, originalLocation, player, velocity, definition);
                    if (arrow != null) spawned.add(arrow);
                }
                scheduleExtraArrowCleanup(spawned, definition.despawnAfterTicks);
            }
        }

        debug("Patrón de flechas activado: " + definition.key + " jugador=" + player.getName()
                + " mode=" + definition.mode + " extra=" + extraCount);
    }

    private void spawnBurstExtraArrows(Player player, Location originalLocation, EntitySnapshot snapshot,
                                       Vector baseVelocity, RangedExtraArrowDefinition definition,
                                       int extraCount, List<Entity> spawned) {
        World originalWorld = originalLocation.getWorld();
        if (originalWorld == null) return;

        long delay = definition.burstDelayTicks;
        if (delay <= 0L) {
            for (int index = 0; index < extraCount; index++) {
                AbstractArrow arrow = spawnExtraArrow(snapshot, originalLocation, player,
                        baseVelocity.clone().multiply(definition.velocityMultiplier), definition);
                if (arrow != null) spawned.add(arrow);
            }
            scheduleExtraArrowCleanup(spawned, definition.despawnAfterTicks);
            return;
        }

        final int[] remaining = {extraCount};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (remaining[0] <= 0 || !player.isOnline() || player.isDead()
                    || player.getWorld() != originalWorld) {
                if (task[0] != null) task[0].cancel();
                return;
            }

            AbstractArrow arrow = spawnExtraArrow(snapshot, originalLocation, player,
                    baseVelocity.clone().multiply(definition.velocityMultiplier), definition);
            if (arrow != null) spawned.add(arrow);

            remaining[0]--;
            if (remaining[0] <= 0 && task[0] != null) task[0].cancel();
        }, delay, delay);

        long cleanupDelay = definition.despawnAfterTicks <= 0 ? 0L
                : definition.despawnAfterTicks + (delay * extraCount);
        scheduleExtraArrowCleanup(spawned, cleanupDelay);
    }


    private AbstractArrow spawnExtraArrow(EntitySnapshot snapshot, Location location, Player shooter,
                                          Vector velocity, RangedExtraArrowDefinition definition) {
        if (snapshot == null || location == null || location.getWorld() == null) return null;
        try {
            Entity copied = snapshot.createEntity(location);
            if (!(copied instanceof AbstractArrow arrow)) {
                copied.remove();
                return null;
            }

            arrow.setShooter(shooter, false);
            arrow.setVelocity(velocity);
            arrow.setDamage(Math.max(0.0, arrow.getDamage() * definition.damageMultiplier));
            if (!definition.extraArrowsPickup) {
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            }
            arrow.addScoreboardTag("mdvtools_extra_arrow");
            return arrow;
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("No pude duplicar flecha de " + definition.key + ": "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return null;
        }
    }

    private void scheduleExtraArrowCleanup(List<Entity> arrows, long delayTicks) {
        if (delayTicks <= 0L || arrows == null) return;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Entity arrow : arrows) {
                if (arrow != null && arrow.isValid()) arrow.remove();
            }
            arrows.clear();
        }, delayTicks);
    }

    private Vector rotateHorizontal(Vector vector, double degrees) {
        return vector.clone().rotateAroundY(Math.toRadians(degrees));
    }

    private Vector rotateYawPitch(Vector vector, double yawDegrees, double pitchDegrees) {
        double speed = vector.length();
        if (speed <= 1.0E-8) return vector.clone();

        Vector direction = vector.clone().normalize().rotateAroundY(Math.toRadians(yawDegrees));
        Vector right = direction.clone().crossProduct(new Vector(0.0, 1.0, 0.0));
        if (right.lengthSquared() <= 1.0E-8) right = new Vector(1.0, 0.0, 0.0);
        right.normalize();
        direction.rotateAroundAxis(right, Math.toRadians(pitchDegrees));
        return direction.normalize().multiply(speed);
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
        if (isNode && !treeNodeExtraBonusEnabled) return;
        if (!isNode && !headOreExtraBonusEnabled) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isNode && shouldDisableMiningBonusForSilkTouch(tool)) return;

        double bonusPercent = isNode ? bonuses.treeNodeExtra : bonuses.rareMinerals;
        int extraUnits = rollYieldExtraUnits(bonusPercent);
        if (extraUnits <= 0) return;

        int perUnit = isNode ? treeNodeExtraAmount : headOreExtraAmount;
        int amount = safeMultiplyYieldAmount(extraUnits, perUnit);
        dropExtraMmoItem(dropType, dropId, player, block, amount, isNode ? "nodo" : "mineral");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonitorVanillaYield(BlockBreakEvent event) {
        if (internalBreakEvent || !vanillaYieldEnabled) return;
        if (!event.isDropItems()) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();
        VanillaYieldDrop yield = calculateVanillaYield(player, block, tool);
        if (yield == null || yield.amount <= 0) return;

        dropMaterialExtra(block.getWorld(), block.getLocation().add(0.5, 0.55, 0.5), yield.material, yield.amount);
        debug("Rendimiento vanilla: " + block.getType() + " -> " + yield.material + " x" + yield.amount);
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
        VanillaYieldDrop vanillaYield = calculateVanillaYield(player, block, tool);
        boolean broken = block.breakNaturally(tool);
        if (broken) {
            if (vanillaYield != null && vanillaYield.amount > 0) {
                dropMaterialExtra(dropSnapshot.world, dropSnapshot.location.clone().add(0.5, 0.55, 0.5), vanillaYield.material, vanillaYield.amount);
            }
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
        VanillaYieldDrop vanillaYield = calculateVanillaYield(player, block, tool);
        rollCustomDrops(player, block, tool);

        if (autoReplant) {
            Material seed = seedForCrop(cropType);
            boolean consumedSeed = false;
            if (seed != null) consumedSeed = consumeOne(drops, seed);

            if (!replantNeedsSeed || consumedSeed) {
                addMaterialExtra(drops, vanillaYield);
                dropItems(world, block, drops);
                block.setType(cropType, false);
                if (block.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(0);
                    block.setBlockData(ageable, false);
                }
                return;
            }
        }

        addMaterialExtra(drops, vanillaYield);
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
            boolean miningBonusBlockedBySilkTouch = category == CustomDropCategory.MINING
                    && shouldDisableMiningBonusForSilkTouch(tool);
            if (isRelativeCustomDropBonusEnabled(category) && !miningBonusBlockedBySilkTouch) {
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

    /**
     * Convierte un porcentaje acumulativo en unidades extra sin hacer un loop por cada 100%.
     * 80%  -> 0 seguros + 80% de +1
     * 170% -> 1 seguro  + 70% de +1
     * 550% -> 5 seguros + 50% de +1
     */
    private int rollYieldExtraUnits(double bonusPercent) {
        if (bonusPercent <= 0.0 || maxYieldExtraUnitsPerBlock <= 0) return 0;

        double safeBonus = Math.max(0.0, bonusPercent);
        long guaranteed = (long) Math.floor(safeBonus / 100.0);
        double remainder = safeBonus - (guaranteed * 100.0);

        int extra = (int) Math.min(guaranteed, (long) maxYieldExtraUnitsPerBlock);
        if (extra < maxYieldExtraUnitsPerBlock
                && remainder > 0.0
                && ThreadLocalRandom.current().nextDouble(100.0) < remainder) {
            extra++;
        }
        return extra;
    }

    private int safeMultiplyYieldAmount(int units, int amountPerUnit) {
        if (units <= 0 || amountPerUnit <= 0) return 0;
        long result = (long) units * (long) amountPerUnit;
        return (int) Math.min(result, Integer.MAX_VALUE);
    }

    private boolean shouldDisableMiningBonusForSilkTouch(ItemStack tool) {
        return miningYieldDisabledBySilkTouch
                && tool != null
                && tool.getType() != Material.AIR
                && tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
    }

    private VanillaYieldDrop calculateVanillaYield(Player player, Block block, ItemStack tool) {
        if (!vanillaYieldEnabled || player == null || block == null) return null;

        Material type = block.getType();
        Material dropMaterial = null;
        double bonusPercent = 0.0;

        if (vanillaMiningYieldEnabled && vanillaMiningYieldDrops.containsKey(type)) {
            if (vanillaMiningRequireMatchingTool && (tool == null || !isPickaxe(tool.getType()))) return null;
            if (shouldDisableMiningBonusForSilkTouch(tool)) return null;

            // Evita generar recursos si la herramienta ni siquiera puede cosechar la mena.
            // Fortune solo interviene en el drop vanilla; el extra de MDVTools siempre es
            // una unidad base por cada 100% de bonus. isValidTool está deprecado en Paper,
            // pero en 1.21.6 sigue siendo la comprobación directa de "correcto para drops".
            if (tool == null || !block.isValidTool(tool)) return null;

            dropMaterial = vanillaMiningYieldDrops.get(type);
            bonusPercent = readCombinedBonuses(player).rareMinerals;
        } else if (vanillaWoodcuttingYieldEnabled && logsAllowed.contains(type) && isWoodLikeMaterial(type)) {
            if (vanillaWoodcuttingRequireMatchingTool && (tool == null || !isAxe(tool.getType()))) return null;
            dropMaterial = type;
            bonusPercent = readCombinedBonuses(player).treeNodeExtra;
        } else if (vanillaFarmingYieldEnabled && vanillaFarmingYieldDrops.containsKey(type)) {
            if (!isMatureCrop(block)) return null;
            if (vanillaFarmingRequireMatchingTool && (tool == null || !isHoe(tool.getType()))) return null;
            dropMaterial = vanillaFarmingYieldDrops.get(type);
            bonusPercent = readCombinedBonuses(player).agricultureRareDrops;
        }

        if (dropMaterial == null || dropMaterial == Material.AIR || bonusPercent <= 0.0) return null;

        int amount = rollYieldExtraUnits(bonusPercent);
        return amount <= 0 ? null : new VanillaYieldDrop(dropMaterial, amount);
    }

    private void addMaterialExtra(Collection<ItemStack> drops, VanillaYieldDrop yield) {
        if (yield == null || drops == null || yield.amount <= 0 || yield.material == Material.AIR) return;

        ItemStack probe = new ItemStack(yield.material, 1);
        int maxStack = Math.max(1, probe.getMaxStackSize());
        int remaining = yield.amount;
        while (remaining > 0) {
            int amount = Math.min(remaining, maxStack);
            drops.add(new ItemStack(yield.material, amount));
            remaining -= amount;
        }
    }

    private void dropMaterialExtra(World world, Location location, Material material, int amount) {
        if (world == null || location == null || material == null || material == Material.AIR || amount <= 0) return;

        ItemStack probe = new ItemStack(material, 1);
        int maxStack = Math.max(1, probe.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            Item item = world.dropItemNaturally(location, new ItemStack(material, stackAmount));
            item.setPickupDelay(10);
            remaining -= stackAmount;
        }
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
        ItemStack stack = buildMmoItemStack(typeId, itemId, 1);
        if (stack != null && stack.getType() != Material.AIR) {
            Location location = block.getLocation().add(0.5, 0.55, 0.5);
            int maxStack = Math.max(1, stack.getMaxStackSize());
            int remaining = amount;
            while (remaining > 0) {
                int stackAmount = Math.min(remaining, maxStack);
                ItemStack droppedStack = stack.clone();
                droppedStack.setAmount(stackAmount);
                Item item = block.getWorld().dropItemNaturally(location, droppedStack);
                item.setPickupDelay(10);
                remaining -= stackAmount;
            }
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
        if (event == null) return;

        ItemStack item = extractItemFromCustomDurabilityEvent(event);

        // El mismo evento de MMOItems se aprovecha para sincronizar ÚNICAMENTE
        // PLAYER_HEAD con Custom Durability. Se difiere 1 tick para leer el valor
        // final que MMOItems acaba de guardar.
        if (playerHeadDurabilityBarEnabled && isMmoCustomDurabilityPlayerHead(item)) {
            Player player = extractPlayerFromCustomDurabilityEvent(event);
            if (player != null) schedulePlayerHeadDurabilityBarSync(player);
        }

        if (!customDurabilityProtectionEnabled) return;
        if (!(event instanceof Cancellable cancellable) || cancellable.isCancelled()) return;
        if (!isCustomDurabilityProtected(item)) return;

        cancellable.setCancelled(true);
    }

    private Object extractDurabilitySource(Event event) {
        if (event == null) return null;

        // API actual: getSourceItem(). Versiones anteriores/forks pueden exponer getItem().
        for (String methodName : new String[]{"getSourceItem", "getItem"}) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object source = method.invoke(event);
                if (source != null) return source;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private ItemStack extractItemFromCustomDurabilityEvent(Event event) {
        Object sourceItem = extractDurabilitySource(event);
        if (sourceItem == null) return null;

        try {
            // DurabilityItem#getNBTItem() -> NBTItem -> getItem() -> ItemStack
            Method getNbtItem = sourceItem.getClass().getMethod("getNBTItem");
            Object nbtItem = getNbtItem.invoke(sourceItem);
            if (nbtItem == null) return null;

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

    private Player extractPlayerFromCustomDurabilityEvent(Event event) {
        Object sourceItem = extractDurabilitySource(event);
        if (sourceItem == null) return null;

        try {
            Method getPlayer = sourceItem.getClass().getMethod("getPlayer");
            Object value = getPlayer.invoke(sourceItem);
            return value instanceof Player player ? player : null;
        } catch (Throwable ignored) {
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

    /**
     * Registra solo los dos eventos extra que necesita la barra visual:
     * ItemBuildEvent para que las cabezas nuevas nazcan con barra y
     * ItemCustomRepairEvent para refrescarla al reparar.
     *
     * CustomDurabilityDamage ya está registrado por el puente de protección
     * de durabilidad y se reutiliza arriba; no se duplica el listener.
     */
    @SuppressWarnings("unchecked")
    private void registerPlayerHeadDurabilityBarExternalEvents() {
        if (!playerHeadDurabilityBarEnabled) return;

        registerPlayerHeadDurabilityBarEvent(
                "net.Indyuce.mmoitems.api.event.ItemBuildEvent",
                false,
                event -> {
                    try {
                        Method getter = event.getClass().getMethod("getItemStack");
                        Object value = getter.invoke(event);
                        if (value instanceof ItemStack item) syncPlayerHeadDurabilityBar(item);
                    } catch (Throwable throwable) {
                        if (debug) getLogger().warning("No pude sincronizar ItemBuildEvent: " + throwable.getClass().getSimpleName());
                    }
                });

        registerPlayerHeadDurabilityBarEvent(
                "net.Indyuce.mmoitems.api.event.item.ItemCustomRepairEvent",
                true,
                event -> {
                    ItemStack item = extractItemFromCustomDurabilityEvent(event);
                    if (!isMmoCustomDurabilityPlayerHead(item)) return;
                    Player player = extractPlayerFromCustomDurabilityEvent(event);
                    if (player != null) schedulePlayerHeadDurabilityBarSync(player);
                });
    }

    @SuppressWarnings("unchecked")
    private void registerPlayerHeadDurabilityBarEvent(String className, boolean ignoreCancelled, java.util.function.Consumer<Event> handler) {
        if (playerHeadDurabilityBarHookedEvents.contains(className)) return;

        try {
            Class<?> rawClass = Class.forName(className);
            if (!Event.class.isAssignableFrom(rawClass)) return;

            Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
            EventExecutor executor = (listener, event) -> {
                if (!playerHeadDurabilityBarEnabled) return;
                try {
                    handler.accept(event);
                } catch (Throwable throwable) {
                    if (debug) {
                        getLogger().warning("Error en player-head-durability-bar para " + event.getEventName()
                                + ": " + throwable.getClass().getSimpleName()
                                + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
                    }
                }
            };

            Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, executor, this, ignoreCancelled);
            playerHeadDurabilityBarHookedEvents.add(className);
            debug("player-head-durability-bar conectado a " + className + ".");
        } catch (ClassNotFoundException ignored) {
            debug("player-head-durability-bar: evento no encontrado: " + className);
        } catch (Throwable throwable) {
            getLogger().warning("No pude registrar player-head-durability-bar para " + className + ": "
                    + throwable.getClass().getSimpleName()
                    + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
        }
    }

    private boolean isMmoCustomDurabilityPlayerHead(ItemStack item) {
        if (!playerHeadDurabilityBarEnabled || item == null || item.getType() != Material.PLAYER_HEAD) return false;
        Integer max = readMmoItemInteger(item, "MMOITEMS_MAX_DURABILITY");
        return max != null && max > 0;
    }

    /**
     * Convierte la durabilidad restante de MMOItems en componentes vanilla
     * EXCLUSIVAMENTE para PLAYER_HEAD. Los componentes son solo una barra visual;
     * MMOItems sigue siendo la fuente real de durabilidad.
     */
    @SuppressWarnings("UnstableApiUsage")
    private boolean syncPlayerHeadDurabilityBar(ItemStack item) {
        if (!isMmoCustomDurabilityPlayerHead(item)) return false;

        Integer maxValue = readMmoItemInteger(item, "MMOITEMS_MAX_DURABILITY");
        if (maxValue == null || maxValue <= 0) return false;
        int max = maxValue;

        Integer currentValue = readMmoItemInteger(item, "MMOITEMS_DURABILITY");
        int current = currentValue == null ? max : Math.max(0, Math.min(max, currentValue));

        // La cabeza debe ser no-stackeable para poder activar MAX_DAMAGE cuando
        // realmente haga falta mostrar la barra. Esto no crea ningún loop.
        boolean changed = false;
        int currentMaxStack = item.getDataOrDefault(DataComponentTypes.MAX_STACK_SIZE, item.getType().getMaxStackSize());
        if (currentMaxStack != 1) {
            item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
            changed = true;
        }

        // Al 100% de durabilidad NO ponemos MAX_DAMAGE/DAMAGE. Así el cliente
        // no dibuja ninguna barra. En el primer punto perdido, CustomDurabilityDamage
        // vuelve a llamar este método y la barra aparece automáticamente.
        if (current >= max || isCustomDurabilityProtected(item)) {
            if (item.hasData(DataComponentTypes.DAMAGE)) {
                item.unsetData(DataComponentTypes.DAMAGE);
                changed = true;
            }
            if (item.hasData(DataComponentTypes.MAX_DAMAGE)) {
                item.unsetData(DataComponentTypes.MAX_DAMAGE);
                changed = true;
            }
            return changed;
        }

        int visualDamage = Math.max(1, max - current);
        visualDamage = Math.min(Math.max(1, max - 1), visualDamage);

        int currentMaxDamage = item.getDataOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
        int currentDamage = item.getDataOrDefault(DataComponentTypes.DAMAGE, 0);
        if (currentMaxDamage == max && currentDamage == visualDamage) return changed;

        item.setData(DataComponentTypes.MAX_DAMAGE, max);
        item.setData(DataComponentTypes.DAMAGE, visualDamage);
        return true;
    }

    /**
     * Una sola pasada de inventario y solo cuando realmente hubo un evento de
     * durabilidad/reparación o al entrar. No existe escaneo periódico.
     */
    private void syncPlayerHeadDurabilityBars(Player player) {
        if (!playerHeadDurabilityBarEnabled || player == null || !player.isOnline()) return;

        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() != Material.PLAYER_HEAD) continue;
            if (syncPlayerHeadDurabilityBar(item)) inventory.setItem(slot, item);
        }
    }

    private void schedulePlayerHeadDurabilityBarSync(Player player) {
        if (!playerHeadDurabilityBarEnabled || player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        if (!playerHeadDurabilityBarSyncQueued.add(uuid)) return;

        Bukkit.getScheduler().runTask(this, () -> {
            playerHeadDurabilityBarSyncQueued.remove(uuid);
            syncPlayerHeadDurabilityBars(player);
        });
    }

    /**
     * Al añadir MAX_DAMAGE una PLAYER_HEAD puede disparar PlayerItemDamageEvent,
     * aunque Material.PLAYER_HEAD siga teniendo maxDurability vanilla = 0.
     *
     * MMOItems ya posee OTRO camino específico para materiales no-damageable
     * (EntityDamageEvent -> Custom Durability). Si dejamos llegar este evento a
     * su listener HIGHEST, la cabeza pierde durabilidad por ambos caminos y baja
     * el doble. Por eso se cancela en LOWEST, antes que MMOItems, SOLO para estas
     * PLAYER_HEAD. El desgaste custom real sigue ocurriendo una única vez por el
     * camino de materiales no-damageable.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerHeadVanillaDurabilityDamage(PlayerItemDamageEvent event) {
        if (!isMmoCustomDurabilityPlayerHead(event.getItem())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerHeadDurabilityJoin(PlayerJoinEvent event) {
        if (playerHeadDurabilityBarEnabled) schedulePlayerHeadDurabilityBarSync(event.getPlayer());
    }

    /**
     * Guarda una pista diminuta de qué mano originó la interacción. MMOItems y
     * MythicLib disparan después sus eventos de habilidad de forma síncrona, así
     * que una ventana de 250 ms es más que suficiente y evita adivinar siempre
     * la mainhand. No se agenda ninguna tarea.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAbilityDurabilityInteractSource(PlayerInteractEvent event) {
        if (!abilityDurabilityCostEnabled) return;
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return;
        rememberAbilityDurabilitySource(event.getPlayer(), hand, event.getItem());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAbilityDurabilityInteractEntitySource(PlayerInteractEntityEvent event) {
        if (!abilityDurabilityCostEnabled) return;
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return;
        ItemStack item = event.getPlayer().getInventory().getItem(hand);
        rememberAbilityDurabilitySource(event.getPlayer(), hand, item);
    }

    /**
     * MDVCRAFT usa SWAP_ITEMS/F para castear desde offhand. Si hay un MMOItem en
     * offhand, esa es la fuente prioritaria. Así un amuleto/escudo no gasta por
     * error la espada que el jugador tenga en mainhand.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAbilityDurabilitySwapSource(PlayerSwapHandItemsEvent event) {
        if (!abilityDurabilityCostEnabled) return;
        Player player = event.getPlayer();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isMmoItem(offhand)) {
            rememberAbilityDurabilitySource(player, EquipmentSlot.OFF_HAND, offhand);
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        if (isRealItem(main)) rememberAbilityDurabilitySource(player, EquipmentSlot.HAND, main);
    }

    /**
     * Habilidades ATTACK suelen nacer dentro de EntityDamageByEntityEvent y no
     * pasan por PlayerInteractEvent. Marcamos mainhand solo para ese caso.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAbilityDurabilityAttackSource(EntityDamageByEntityEvent event) {
        if (!abilityDurabilityCostEnabled || !(event.getDamager() instanceof Player player)) return;
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isRealItem(main)) rememberAbilityDurabilitySource(player, EquipmentSlot.HAND, main);
    }

    private void rememberAbilityDurabilitySource(Player player, EquipmentSlot slot, ItemStack item) {
        if (!abilityDurabilityCostEnabled || player == null || slot == null || !isRealItem(item)) return;
        if (slot != EquipmentSlot.HAND && slot != EquipmentSlot.OFF_HAND) return;

        // Solo guardamos mano + timestamp. No leemos NBT aquí: este handler puede
        // ejecutarse en interacciones normales y queremos que el coste sea mínimo.
        abilityDurabilitySourceHints.put(player.getUniqueId(),
                new AbilityDurabilitySourceHint(slot, System.currentTimeMillis()));
    }

    private boolean isMmoItem(ItemStack item) {
        return isRealItem(item) && readMmoItemTypeId(item) != null;
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

        AbilityDurabilitySource source = resolveAbilityDurabilitySource(event, player);
        if (source != null && !chargeAbilityCustomDurability(player, source.item, source.slot)) {
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

    private AbilityDurabilitySource resolveAbilityDurabilitySource(Event event, Player player) {
        if (player == null) return null;

        // Si el evento expone item + mano de forma explícita, esa es la fuente
        // más fiable y no necesitamos ninguna pista adicional.
        ItemStack directItem = extractAbilitySourceItem(event);
        EquipmentSlot directSlot = extractAbilitySourceSlot(event);
        if (isRealItem(directItem)
                && (directSlot == EquipmentSlot.HAND || directSlot == EquipmentSlot.OFF_HAND)) {
            abilityDurabilitySourceHints.remove(player.getUniqueId());
            return new AbilityDurabilitySource(directItem, directSlot);
        }

        // Fallback preferido: mano del input Bukkit inmediatamente anterior.
        // Esto es lo que permite distinguir con seguridad mainhand de offhand.
        AbilityDurabilitySourceHint hint = abilityDurabilitySourceHints.get(player.getUniqueId());
        if (hint != null) {
            long now = System.currentTimeMillis();
            if (now - hint.createdAtMs <= ABILITY_DURABILITY_SOURCE_HINT_TTL_MS) {
                ItemStack hintedItem = player.getInventory().getItem(hint.slot);
                if (isRealItem(hintedItem)) {
                    abilityDurabilitySourceHints.remove(player.getUniqueId());
                    return new AbilityDurabilitySource(hintedItem, hint.slot);
                }
            } else {
                abilityDurabilitySourceHints.remove(player.getUniqueId());
            }
        }

        // Último fallback: si el evento trae el item pero no la mano, buscamos
        // en las dos manos. Si no se puede resolver, NO adivinamos mainhand.
        if (isRealItem(directItem)) {
            EquipmentSlot located = locateHeldAbilityItem(player, directItem);
            if (located != null) return new AbilityDurabilitySource(directItem, located);
        }
        return null;
    }

    private ItemStack extractAbilitySourceItem(Event event) {
        if (event == null) return null;
        for (String methodName : new String[]{"getItemStack", "getItem", "getSourceItem", "getWeapon"}) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object value = method.invoke(event);
                ItemStack item = itemStackFromUnknownAbilitySource(value);
                if (item != null) return item;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private ItemStack itemStackFromUnknownAbilitySource(Object source) {
        if (source == null) return null;
        if (source instanceof ItemStack item) return item;

        for (String methodName : new String[]{"getItemStack", "getItem"}) {
            try {
                Method method = source.getClass().getMethod(methodName);
                Object value = method.invoke(source);
                if (value instanceof ItemStack item) return item;
            } catch (Throwable ignored) {
            }
        }

        try {
            Method getNbtItem = source.getClass().getMethod("getNBTItem");
            Object nbt = getNbtItem.invoke(source);
            if (nbt != null) {
                Method getItem = nbt.getClass().getMethod("getItem");
                Object value = getItem.invoke(nbt);
                if (value instanceof ItemStack item) return item;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private EquipmentSlot extractAbilitySourceSlot(Event event) {
        if (event == null) return null;
        for (String methodName : new String[]{"getHand", "getSlot", "getEquipmentSlot", "getSourceSlot"}) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object value = method.invoke(event);
                EquipmentSlot slot = normalizeAbilityEquipmentSlot(value);
                if (slot != null) return slot;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private EquipmentSlot normalizeAbilityEquipmentSlot(Object value) {
        if (value == null) return null;
        if (value instanceof EquipmentSlot slot) return slot;

        String name = value.toString().toUpperCase(Locale.ROOT);
        if (name.contains("OFF") && name.contains("HAND")) return EquipmentSlot.OFF_HAND;
        if (name.equals("HAND") || name.contains("MAIN_HAND") || name.contains("MAINHAND")) return EquipmentSlot.HAND;
        return null;
    }

    private EquipmentSlot locateHeldAbilityItem(Player player, ItemStack item) {
        if (player == null || !isRealItem(item)) return null;
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        if (sameAbilityItem(main, item)) return EquipmentSlot.HAND;
        if (sameAbilityItem(offhand, item)) return EquipmentSlot.OFF_HAND;
        return null;
    }

    private boolean sameAbilityItem(ItemStack first, ItemStack second) {
        if (!isRealItem(first) || !isRealItem(second)) return false;
        if (first == second) return true;
        if (first.getType() != second.getType()) return false;
        return getAbilityDurabilityItemIdentity(first).equals(getAbilityDurabilityItemIdentity(second));
    }

    private boolean chargeAbilityCustomDurability(Player player, ItemStack item, EquipmentSlot slot) {
        if (!abilityDurabilityCostEnabled || abilityDurabilityCostAmount <= 0) return true;
        if (player == null || slot == null) return true;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return true;
        if (!isRealItem(item)) return true;

        // Nunca se adivina mainhand: solo se cobra al item identificado como fuente.
        if (isCustomDurabilityProtected(item)) return true;
        if (abilityDurabilityOnlyCustomDurability && !hasMmoCustomDurability(item)) return true;

        UUID playerId = player.getUniqueId();
        String identity = slot.name() + ":" + getAbilityDurabilityItemIdentity(item);
        long now = System.currentTimeMillis();
        AbilityDurabilityCharge lastCharge = abilityDurabilityLastCharge.get(playerId);
        if (lastCharge != null
                && lastCharge.itemIdentity.equals(identity)
                && abilityDurabilityDedupeWindowMs > 0L
                && now - lastCharge.createdAtMs <= abilityDurabilityDedupeWindowMs) {
            return true;
        }

        Boolean charged = chargeAbilityCustomDurabilityWithMmoItems(player, item, slot);
        if (charged != null) {
            if (charged) abilityDurabilityLastCharge.put(playerId, new AbilityDurabilityCharge(identity, now));
            return charged;
        }

        if (debug) getLogger().warning("No pude conectar con la API de durabilidad custom de MMOItems para cobrar habilidad.");
        return true;
    }

    private Boolean chargeAbilityCustomDurabilityWithMmoItems(Player player, ItemStack item, EquipmentSlot slot) {
        if (!ensureMmoDurabilityReflection()) return null;

        try {
            Object durabilityItem;
            if (mmoDurabilityConstructorItemStack != null) {
                durabilityItem = mmoDurabilityConstructorItemStack.newInstance(player, item);
            } else if (mmoDurabilityConstructorNbt != null) {
                Object nbt = mmoNbtGetMethod.invoke(null, item);
                durabilityItem = mmoDurabilityConstructorNbt.newInstance(player, nbt, slot);
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
                setAbilitySourceItem(player, slot, null);
            } else if (result instanceof ItemStack newItem) {
                syncPlayerHeadDurabilityBar(newItem);
                setAbilitySourceItem(player, slot, newItem);
            } else {
                return null;
            }
            return true;
        } catch (Throwable throwable) {
            if (debug) getLogger().warning("Error cobrando durabilidad custom por habilidad: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return null;
        }
    }

    private void setAbilitySourceItem(Player player, EquipmentSlot slot, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        if (slot == EquipmentSlot.OFF_HAND) inventory.setItemInOffHand(item);
        else inventory.setItemInMainHand(item);
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

    private boolean isConfiguredOffhandSwapCastItem(ItemStack item) {
        return matchesConfiguredMmoItem(item, offhandSwapCastMmoItems);
    }

    private boolean matchesConfiguredMmoItem(ItemStack item, Set<String> selectors) {
        if (selectors == null || selectors.isEmpty()) return false;
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return false;

        String type = readMmoItemTypeId(item);
        String id = readMmoItemString(item, "MMOITEMS_ITEM_ID");
        if (type == null || type.isBlank() || id == null || id.isBlank()) return false;

        type = type.toUpperCase(Locale.ROOT);
        id = id.toUpperCase(Locale.ROOT);

        return selectors.contains(type + ":" + id)
                || selectors.contains(type + ":*")
                || selectors.contains("*:" + id)
                || selectors.contains("*:*");
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
            mergeMissingConfigDefaults();
            loadSettings();
            if (fishingFightTimerListener != null) fishingFightTimerListener.reload();
            registerWeaponSwapLockExternalEvents();
            registerCustomDurabilityProtectionEvent();
            registerPlayerHeadDurabilityBarExternalEvents();
            if (playerHeadDurabilityBarEnabled) {
                for (Player player : Bukkit.getOnlinePlayers()) schedulePlayerHeadDurabilityBarSync(player);
            }
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

    private static final class AbilityDurabilitySourceHint {
        final EquipmentSlot slot;
        final long createdAtMs;

        AbilityDurabilitySourceHint(EquipmentSlot slot, long createdAtMs) {
            this.slot = slot;
            this.createdAtMs = createdAtMs;
        }
    }

    private static final class AbilityDurabilitySource {
        final ItemStack item;
        final EquipmentSlot slot;

        AbilityDurabilitySource(ItemStack item, EquipmentSlot slot) {
            this.item = item;
            this.slot = slot;
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

    private enum RangedExtraArrowSource {
        WEAPON,
        OFFHAND,
        EITHER
    }

    private enum RangedExtraArrowMode {
        BURST,
        HORIZONTAL,
        VOLLEY
    }

    private static final class RangedExtraArrowDefinition {
        String key;
        RangedExtraArrowSource source = RangedExtraArrowSource.OFFHAND;
        RangedExtraArrowMode mode = RangedExtraArrowMode.BURST;
        final Set<String> mmoItems = new HashSet<>();
        final Set<Material> weapons = EnumSet.noneOf(Material.class);
        int totalArrows = 2;
        double chancePercent = 100.0;
        long cooldownMs;
        double minimumForce;
        double damageMultiplier = 1.0;
        double velocityMultiplier = 1.0;
        boolean extraArrowsPickup;
        int despawnAfterTicks = 100;
        long burstDelayTicks = 3L;
        double horizontalAngleStepDegrees = 6.0;
        double volleyHorizontalSpreadDegrees = 10.0;
        double volleyVerticalSpreadDegrees = 5.0;
    }

    private enum CustomDropCategory {
        AUTO,
        NONE,
        FARMING,
        MINING,
        WOODCUTTING
    }

    private static final class VanillaYieldDrop {
        final Material material;
        final int amount;

        VanillaYieldDrop(Material material, int amount) {
            this.material = material;
            this.amount = Math.max(0, amount);
        }
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
