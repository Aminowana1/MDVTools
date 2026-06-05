package com.mdvcraft.tools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MDVToolsPlugin extends JavaPlugin implements Listener {

    private boolean debug;

    private Set<Material> miningAllowed = EnumSet.noneOf(Material.class);
    private Set<Material> logsAllowed = EnumSet.noneOf(Material.class);
    private Set<Material> cropsAllowed = EnumSet.noneOf(Material.class);

    private boolean miningEnabled;
    private boolean woodcuttingEnabled;
    private boolean farmingEnabled;

    private int maxMiningExtra;
    private int maxWoodExtra;
    private int maxCropExtra;

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

        debug("Config cargada. Mining=" + miningAllowed.size() + ", Logs=" + logsAllowed.size() + ", Crops=" + cropsAllowed.size());
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

            harvestCrop(crop, tool, lore.autoReplantar);
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
        return block.breakNaturally(tool);
    }

    private void harvestCrop(Block block, ItemStack tool, boolean autoReplant) {
        Material cropType = block.getType();
        World world = block.getWorld();
        Collection<ItemStack> drops = new ArrayList<>(block.getDrops(tool));

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
