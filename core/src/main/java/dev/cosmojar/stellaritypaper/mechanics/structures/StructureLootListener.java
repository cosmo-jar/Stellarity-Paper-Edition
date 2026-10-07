package dev.cosmojar.stellaritypaper.mechanics.structures;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BrushableBlock;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StructureLootListener implements Listener {

    private final Plugin plugin;
    private final CustomLootService customLootService;
    private final ItemStateRepository itemStateRepository;
    private final NamespacedKey containerLootedKey;

    public StructureLootListener(
            final Plugin plugin,
            final CustomLootService customLootService,
            final ItemStateRepository itemStateRepository
    ) {
        this.plugin = plugin;
        this.customLootService = customLootService;
        this.itemStateRepository = itemStateRepository;
        this.containerLootedKey = new NamespacedKey(plugin, "container_looted");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLootGenerate(final LootGenerateEvent event) {
        final Location loc = event.getLootContext() != null ? event.getLootContext().getLocation() : null;
        final Block block = loc != null ? loc.getBlock() : null;

        if (block != null && (block.getType() == Material.VAULT || block.getType() == Material.TRIAL_SPAWNER)) {
            return;
        }

        if (block != null && block.getState() instanceof org.bukkit.block.TileState tile) {
            if (tile.getPersistentDataContainer().has(containerLootedKey, PersistentDataType.BYTE)) {
                return;
            }
            tile.getPersistentDataContainer().set(containerLootedKey, PersistentDataType.BYTE, (byte) 1);
            tile.update(true, false);
        }

        final String tableKey = event.getLootTable() != null ? event.getLootTable().getKey().toString().toLowerCase() : "";

        if (isBlacklistedVanillaTable(tableKey)) {
            return;
        }

        final List<ItemStack> loot = event.getLoot();

        String structureName = null;
        if (tableKey.contains("stronghold")) {
            structureName = "Stronghold";
        } else if (tableKey.contains("end_city")) {
            structureName = "EndCity";
        } else if (tableKey.contains("village")) {
            structureName = "EndVillage";
        } else if (tableKey.contains("shipwreck") && tableKey.contains("stellarity")) {
            structureName = "Shipwreck";
        } else if (tableKey.contains("floating_treasure")) {
            structureName = "FloatingTreasure";
        } else if (tableKey.contains("campsite")) {
            structureName = "Campsite";
        } else if (tableKey.contains("chapel")) {
            structureName = "Chapel";
        } else if (tableKey.contains("dungeon")) {
            structureName = "EndDungeon";
        } else if (tableKey.contains("fisherman_hut")) {
            structureName = "FishermanHut";
        } else if (tableKey.contains("desert_ruin")) {
            structureName = "DesertRuin";
        } else if (loc != null && loc.getWorld() != null) {
            structureName = determineStructureNameAt(loc);
        }

        if (structureName == null) {
            return;
        }

        final String category = determineCategory(block, tableKey);
        if (category == null) {
            return;
        }

        final CustomLootService.StructureInstanceKey instanceKey = getStructureInstanceKey(loc, structureName);
        final List<ItemStack> extraLoot = customLootService.rollStructureLoot(structureName, category, instanceKey, loc);

        if (!extraLoot.isEmpty()) {
            final int capacity = getContainerCapacity(block, category);
            safeAddLoot(loot, extraLoot, capacity);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBrush(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.SUSPICIOUS_SAND && block.getType() != Material.SUSPICIOUS_GRAVEL) {
            return;
        }
        final ItemStack handItem = event.getItem();
        if (handItem == null || handItem.getType() != Material.BRUSH) {
            return;
        }

        if (!(block.getState() instanceof BrushableBlock brushable)) {
            return;
        }

        if (brushable.getPersistentDataContainer().has(containerLootedKey, PersistentDataType.BYTE)) {
            return;
        }

        final org.bukkit.loot.LootTable lootTable = brushable.getLootTable();
        final String tableKey = lootTable != null ? lootTable.getKey().toString().toLowerCase(Locale.ROOT) : "";

        String structureName = null;
        if (tableKey.contains("desert_ruin")) {
            structureName = "DesertRuin";
        } else if (tableKey.contains("shipwreck")) {
            structureName = "Shipwreck";
        } else {
            structureName = determineStructureNameAt(block.getLocation());
        }

        if (structureName == null) {
            return;
        }

        String category = "SUSPICIOUS_SAND";
        if (tableKey.contains("rare")) {
            category = "SUSPICIOUS_SAND_RARE";
        } else if (tableKey.contains("common")) {
            category = "SUSPICIOUS_SAND_COMMON";
        }

        brushable.getPersistentDataContainer().set(containerLootedKey, PersistentDataType.BYTE, (byte) 1);

        final CustomLootService.StructureInstanceKey instanceKey = getStructureInstanceKey(block.getLocation(), structureName);
        final List<ItemStack> extraLoot = customLootService.rollStructureLoot(structureName, category, instanceKey, block.getLocation());

        if (!extraLoot.isEmpty()) {
            brushable.setItem(extraLoot.get(0));
            brushable.setLootTable(null);
        }
        brushable.update(true, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(final BlockDropItemEvent event) {
        if (!(event.getBlockState() instanceof BrushableBlock brushable)) {
            return;
        }

        final List<Item> items = event.getItems();
        if (items.isEmpty()) {
            return;
        }

        final Item itemEntity = items.get(0);
        final ItemStack droppedItem = itemEntity.getItemStack();

        if (itemStateRepository.getItemId(droppedItem).isPresent()) {
            return;
        }

        final Location loc = event.getBlock().getLocation();
        String structureName = null;

        final org.bukkit.loot.LootTable lt = brushable.getLootTable();
        final String tableKey = lt != null ? lt.getKey().toString().toLowerCase(Locale.ROOT) : "";

        if (tableKey.contains("desert_ruin")) {
            structureName = "DesertRuin";
        } else if (tableKey.contains("shipwreck")) {
            structureName = "Shipwreck";
        } else {
            structureName = determineStructureNameAt(loc);
        }

        if (structureName == null) {
            return;
        }

        String category = "SUSPICIOUS_SAND";
        if (tableKey.contains("rare")) {
            category = "SUSPICIOUS_SAND_RARE";
        } else if (tableKey.contains("common")) {
            category = "SUSPICIOUS_SAND_COMMON";
        } else if ("DesertRuin".equals(structureName)) {
            if (isRareDesertRuinItem(droppedItem.getType())) {
                category = "SUSPICIOUS_SAND_RARE";
            } else {
                category = "SUSPICIOUS_SAND_COMMON";
            }
        }

        final CustomLootService.StructureInstanceKey instanceKey = getStructureInstanceKey(loc, structureName);
        final List<ItemStack> extraLoot = customLootService.rollStructureLoot(structureName, category, instanceKey, loc);

        if (!extraLoot.isEmpty()) {
            itemEntity.setItemStack(extraLoot.get(0));
        }
    }

    private boolean isRareDesertRuinItem(final Material mat) {
        return mat == Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE
                || mat == Material.ARMS_UP_POTTERY_SHERD
                || mat == Material.BREWER_POTTERY_SHERD
                || mat == Material.BURN_POTTERY_SHERD;
    }

    private String determineStructureNameAt(final Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }

        final World world = loc.getWorld();

        if (world.getEnvironment() == World.Environment.NORMAL && isStructure(loc, Structure.STRONGHOLD)) {
            return "Stronghold";
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            if (isStructure(loc, Structure.END_CITY)) {
                return "EndCity";
            }
            final String biomeKey = loc.getBlock().getBiome().getKey().toString().toLowerCase(Locale.ROOT);
            if (biomeKey.contains("dune")) {
                return "DesertRuin";
            }
            try {
                final Structure desertRuin = io.papermc.paper.registry.RegistryAccess.registryAccess()
                        .getRegistry(io.papermc.paper.registry.RegistryKey.STRUCTURE)
                        .get(NamespacedKey.fromString("stellarity:desert_ruin"));
                if (desertRuin != null && isStructure(loc, desertRuin)) {
                    return "DesertRuin";
                }
            } catch (final Throwable ignored) {
            }
        }

        return null;
    }

    private boolean isBlacklistedVanillaTable(final String tableKey) {
        if (tableKey == null || tableKey.isEmpty()) {
            return false;
        }
        return tableKey.contains("mineshaft")
                || tableKey.contains("trial")
                || tableKey.contains("vault")
                || tableKey.contains("simple_dungeon")
                || tableKey.contains("ancient_city")
                || tableKey.contains("desert_pyramid")
                || tableKey.contains("jungle_temple")
                || tableKey.contains("igloo")
                || tableKey.contains("nether_bridge")
                || tableKey.contains("bastion")
                || tableKey.contains("ruined_portal")
                || tableKey.contains("underwater_ruin")
                || tableKey.contains("woodland_mansion")
                || tableKey.contains("ocean_monument")
                || tableKey.contains("buried_treasure")
                || tableKey.contains("spawn_bonus_chest");
    }

    private String determineCategory(final Block block, final String tableKey) {
        if (block != null) {
            final Material mat = block.getType();
            if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST || mat == Material.CHEST_MINECART) {
                return "CHEST";
            }
            if (mat == Material.BARREL) {
                return "BARREL";
            }
            if (mat == Material.DECORATED_POT) {
                return "DECORATED_POT";
            }
            if (mat == Material.DROPPER) {
                return "DROPPER";
            }
            if (mat == Material.DISPENSER) {
                return "DISPENSER";
            }
            if (mat.name().endsWith("SHULKER_BOX")) {
                return "SHULKER_BOX";
            }
            if (mat == Material.SPAWNER) {
                return "SPAWNER";
            }
            if (mat == Material.SUSPICIOUS_SAND || mat == Material.SUSPICIOUS_GRAVEL) {
                if (tableKey.contains("rare")) {
                    return "SUSPICIOUS_SAND_RARE";
                }
                if (tableKey.contains("common")) {
                    return "SUSPICIOUS_SAND_COMMON";
                }
                return "SUSPICIOUS_SAND";
            }
        }

        if (tableKey.contains("pot") || tableKey.contains("decorated_pot")) {
            return "DECORATED_POT";
        } else if (tableKey.contains("barrel")) {
            return "BARREL";
        } else if (tableKey.contains("chest")) {
            return "CHEST";
        } else if (tableKey.contains("shulker")) {
            return "SHULKER_BOX";
        } else if (tableKey.contains("sand") || tableKey.contains("archaeology")) {
            if (tableKey.contains("rare")) {
                return "SUSPICIOUS_SAND_RARE";
            }
            if (tableKey.contains("common")) {
                return "SUSPICIOUS_SAND_COMMON";
            }
            return "SUSPICIOUS_SAND";
        } else if (tableKey.contains("spawner")) {
            return "SPAWNER";
        }
        return null;
    }

    private int getContainerCapacity(final Block block, final String category) {
        if (block != null) {
            if (block.getState() instanceof org.bukkit.block.Chest chest) {
                return chest.getInventory().getSize();
            }
        }
        return switch (category) {
            case "CHEST", "BARREL", "SHULKER_BOX" -> 27;
            case "DROPPER", "DISPENSER" -> 9;
            case "DECORATED_POT", "SUSPICIOUS_SAND", "SUSPICIOUS_SAND_COMMON", "SUSPICIOUS_SAND_RARE" -> 1;
            default -> 27;
        };
    }

    private void safeAddLoot(final List<ItemStack> loot, final List<ItemStack> extraLoot, final int capacity) {
        if (extraLoot.isEmpty()) {
            return;
        }

        final List<ItemStack> filteredExtra = new ArrayList<>();
        for (final ItemStack extraItem : extraLoot) {
            if (extraItem == null) continue;
            boolean isDuplicate = false;
            for (final ItemStack existing : loot) {
                if (existing != null && isSameItem(existing, extraItem)) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                filteredExtra.add(extraItem);
            }
        }

        if (filteredExtra.isEmpty()) {
            return;
        }

        int freeSlots = capacity - loot.size();
        if (filteredExtra.size() > freeSlots) {
            int neededSlots = filteredExtra.size() - freeSlots;
            for (int i = loot.size() - 1; i >= 0 && neededSlots > 0; i--) {
                final ItemStack item = loot.get(i);
                if (item != null && isLowValueFiller(item.getType())) {
                    loot.remove(i);
                    neededSlots--;
                }
            }
        }

        freeSlots = capacity - loot.size();
        if (filteredExtra.size() > freeSlots) {
            int neededSlots = filteredExtra.size() - freeSlots;
            for (int i = loot.size() - 1; i >= 0 && neededSlots > 0; i--) {
                loot.remove(i);
                neededSlots--;
            }
        }

        final int slotsAvailableNow = Math.max(0, capacity - loot.size());
        for (int i = 0; i < Math.min(filteredExtra.size(), slotsAvailableNow); i++) {
            loot.add(filteredExtra.get(i));
        }
    }

    private boolean isSameItem(final ItemStack a, final ItemStack b) {
        if (a.getType() != b.getType()) {
            return false;
        }
        if (a.hasItemMeta() != b.hasItemMeta()) {
            return false;
        }
        if (a.hasItemMeta() && b.hasItemMeta()) {
            return a.getItemMeta().getDisplayName().equals(b.getItemMeta().getDisplayName());
        }
        return true;
    }

    private boolean isLowValueFiller(final Material mat) {
        return mat == Material.WHEAT || mat == Material.BREAD || mat == Material.STRING
                || mat == Material.BONE || mat == Material.ROTTEN_FLESH || mat == Material.COAL
                || mat == Material.PAPER || mat == Material.STICK || mat == Material.BEETROOT_SEEDS
                || mat == Material.SANDSTONE || mat == Material.TERRACOTTA || mat == Material.GOLD_NUGGET;
    }

    private CustomLootService.StructureInstanceKey getStructureInstanceKey(final Location loc, final String structureName) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        final int startChunkX = Math.floorDiv(loc.getBlockX(), 512) * 32;
        final int startChunkZ = Math.floorDiv(loc.getBlockZ(), 512) * 32;
        return new CustomLootService.StructureInstanceKey(loc.getWorld().getUID(), structureName.toLowerCase(Locale.ROOT), startChunkX, startChunkZ);
    }

    private boolean isStructure(final Location loc, final Structure structure) {
        return loc.getWorld() != null && loc.getWorld().hasStructureAt(loc, structure);
    }
}
