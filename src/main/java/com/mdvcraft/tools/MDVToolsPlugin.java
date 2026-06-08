package com.mdvcraft.tools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Sound;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
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
        getLogger().info("MDVTools activado.");
    }

    @Override
    public void onDisable() {
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
