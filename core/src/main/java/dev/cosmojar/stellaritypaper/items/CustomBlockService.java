package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CustomBlockService {

    private static final class BlockDataInfo {
        final Material baseMaterial;
        final String modelKey;

        BlockDataInfo(final Material baseMaterial, final String modelKey) {
            this.baseMaterial = baseMaterial;
            this.modelKey = modelKey;
        }
    }

    private final Plugin plugin;
    private final ItemStateRepository itemStateRepository;
    private final CustomItemFactory customItemFactory;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final NamespacedKey blockIdKey;

    private final Map<String, BlockDataInfo> blockInfo = new HashMap<>();

    public CustomBlockService(
            final Plugin plugin,
            final ItemStateRepository itemStateRepository,
            final CustomItemFactory customItemFactory,
            final ItemDefinitionRegistry itemDefinitionRegistry
    ) {
        this.plugin = plugin;
        this.itemStateRepository = itemStateRepository;
        this.customItemFactory = customItemFactory;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.blockIdKey = new NamespacedKey(plugin, "custom_block_id");

        blockInfo.put("ashen_froglight", new BlockDataInfo(Material.PEARLESCENT_FROGLIGHT, "stellarity:ashen_froglight"));
        blockInfo.put("ender_dirt", new BlockDataInfo(Material.COARSE_DIRT, "stellarity:ender_dirt"));
        blockInfo.put("ender_dirt_path", new BlockDataInfo(Material.DIRT_PATH, "stellarity:ender_dirt_path"));
        blockInfo.put("ender_grass_block", new BlockDataInfo(Material.GRASS_BLOCK, "stellarity:ender_grass_block"));
        blockInfo.put("enderite_block", new BlockDataInfo(Material.NETHERITE_BLOCK, "stellarity:enderite_block"));
        blockInfo.put("rooted_ender_dirt", new BlockDataInfo(Material.ROOTED_DIRT, "stellarity:rooted_ender_dirt"));
        blockInfo.put("altar_of_the_sacred", new BlockDataInfo(Material.CRYING_OBSIDIAN, "stellarity:altar_of_the_sacred"));
        blockInfo.put("altar_of_the_accursed", new BlockDataInfo(Material.END_PORTAL_FRAME, "stellarity:altar_of_the_accursed"));
        blockInfo.put("pixie_in_a_jar_yellow", new BlockDataInfo(Material.STRUCTURE_VOID, "stellarity:pixie_in_a_jar"));
        blockInfo.put("pixie_in_a_jar_magenta", new BlockDataInfo(Material.STRUCTURE_VOID, "stellarity:pixie_in_a_jar"));
        blockInfo.put("pixie_in_a_jar_lime", new BlockDataInfo(Material.STRUCTURE_VOID, "stellarity:pixie_in_a_jar"));
        blockInfo.put("pixie_in_a_jar_light_blue", new BlockDataInfo(Material.STRUCTURE_VOID, "stellarity:pixie_in_a_jar"));
        blockInfo.put("pixie_in_a_jar_radiant", new BlockDataInfo(Material.STRUCTURE_VOID, "stellarity:pixie_in_a_jar"));
    }

    public boolean isCustomBlock(final String pdcId) {
        return blockInfo.containsKey(pdcId);
    }

    public Material getBaseMaterial(final String blockId) {
        final BlockDataInfo info = blockInfo.get(blockId);
        return info != null ? info.baseMaterial : null;
    }

    public boolean placeBlock(final Location loc, final String blockId, final BlockFace facing) {
        final BlockDataInfo info = blockInfo.get(blockId);
        if (info == null) {
            return false;
        }

        loc.getBlock().setType(info.baseMaterial, true);

        final Location displayLoc = loc.getBlock().getLocation().add(0.5D, 1.02D, 0.5D);
        final ItemDisplay display = loc.getWorld().spawn(displayLoc, ItemDisplay.class);

        display.addScoreboardTag("smithed.entity");
        display.addScoreboardTag("smithed.strict");
        display.addScoreboardTag("stellarity.block");
        display.addScoreboardTag("stellarity.block." + blockId);

        display.getPersistentDataContainer().set(blockIdKey, PersistentDataType.STRING, blockId);

        final ItemStack displayItem = new ItemStack(Material.STONE);
        final ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            meta.setItemModel(NamespacedKey.fromString(info.modelKey));
            displayItem.setItemMeta(meta);
        }
        display.setItemStack(displayItem);

        if (blockId.startsWith("pixie_in_a_jar_")) {
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        }

        final Transformation trans = display.getTransformation();
        trans.getScale().set(1.01f, 1.01f, 1.01f);
        trans.getTranslation().set(0.0f, -0.52f, 0.0f);

        if (!blockId.startsWith("pixie_in_a_jar_")) {
            if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                trans.getLeftRotation().set(0.707f, 0.0f, 0.0f, 0.707f);
            } else if (facing == BlockFace.WEST || facing == BlockFace.EAST) {
                trans.getLeftRotation().set(0.0f, 0.0f, 0.707f, 0.707f);
            }
        }
        display.setTransformation(trans);

        return true;
    }

    public Optional<ItemDisplay> findCustomBlockDisplay(final Location loc) {
        final Location center = loc.getBlock().getLocation().add(0.5D, 1.02D, 0.5D);
        for (final org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(center, 0.1D, 0.1D, 0.1D)) {
            if (entity instanceof ItemDisplay display && display.getPersistentDataContainer().has(blockIdKey, PersistentDataType.STRING)) {
                return Optional.of(display);
            }
        }
        return Optional.empty();
    }

    public boolean removeBlock(final Location loc, final boolean dropItem) {
        final Optional<ItemDisplay> displayOpt = findCustomBlockDisplay(loc);
        if (displayOpt.isPresent()) {
            final ItemDisplay display = displayOpt.get();
            final String blockId = display.getPersistentDataContainer().get(blockIdKey, PersistentDataType.STRING);
            display.remove();
            loc.getBlock().setType(Material.AIR, true);

            if (dropItem && blockId != null) {
                itemDefinitionRegistry.findByPdcItemId(blockId).ifPresent(def -> {
                    final ItemStack item = customItemFactory.create(def);
                    loc.getWorld().dropItemNaturally(loc.getBlock().getLocation().add(0.5D, 0.5D, 0.5D), item);
                });
            }
            return true;
        }
        return false;
    }

    public String getBlockId(final ItemDisplay display) {
        return display.getPersistentDataContainer().get(blockIdKey, PersistentDataType.STRING);
    }
}
