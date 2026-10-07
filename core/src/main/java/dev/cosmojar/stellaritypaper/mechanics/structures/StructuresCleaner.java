package dev.cosmojar.stellaritypaper.mechanics.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;

/**
 * Service dedicated to cleaning biome intruder blocks
 * from End structures Chapel of Light while preserving structure blocks.
 */
public class StructuresCleaner {

    private final Plugin plugin;

    private final Set<Material> protectedChapelMaterials = EnumSet.of(
            Material.QUARTZ_BLOCK,
            Material.SMOOTH_QUARTZ,
            Material.CHISELED_QUARTZ_BLOCK,
            Material.QUARTZ_PILLAR,
            Material.QUARTZ_STAIRS,
            Material.QUARTZ_SLAB,
            Material.SEA_LANTERN,
            Material.GLASS,
            Material.TINTED_GLASS,
            Material.WHITE_STAINED_GLASS,
            Material.PINK_STAINED_GLASS,
            Material.PURPLE_STAINED_GLASS,
            Material.MAGENTA_STAINED_GLASS,
            Material.WHITE_CARPET,
            Material.PINK_CARPET,
            Material.PURPLE_CARPET,
            Material.MAGENTA_CARPET,
            Material.CANDLE,
            Material.WHITE_CANDLE,
            Material.PINK_CANDLE,
            Material.PURPLE_CANDLE,
            Material.LANTERN,
            Material.SOUL_LANTERN,
            Material.TORCH,
            Material.SOUL_TORCH,
            Material.OAK_FENCE,
            Material.BIRCH_FENCE,
            Material.END_STONE_BRICKS,
            Material.END_STONE_BRICK_STAIRS,
            Material.END_STONE_BRICK_SLAB,
            Material.END_STONE_BRICK_WALL,
            Material.END_STONE,
            Material.AIR,
            Material.CAVE_AIR,
            Material.LIGHT
    );

    private final Set<Material> intruderMaterials = EnumSet.of(
            Material.NETHER_WART_BLOCK,
            Material.WARPED_WART_BLOCK,
            Material.CRIMSON_FUNGUS,
            Material.WARPED_FUNGUS,
            Material.SHROOMLIGHT,
            Material.OAK_LEAVES,
            Material.SPRUCE_LEAVES,
            Material.BIRCH_LEAVES,
            Material.JUNGLE_LEAVES,
            Material.ACACIA_LEAVES,
            Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES,
            Material.AZALEA_LEAVES,
            Material.FLOWERING_AZALEA_LEAVES,
            Material.CHERRY_LEAVES,
            Material.VINE,
            Material.TWISTING_VINES,
            Material.WEEPING_VINES,
            Material.SCULK_VEIN,
            Material.DEAD_BUSH
    );

    public StructuresCleaner(final Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cleans biome intruder foliage/vegetation inside the Chapel of Light hall around the Altar location.
     *
     * @param altarLoc Center location of the Altar in the Chapel
     */
    public void cleanChapelOfLight(final Location altarLoc) {
        if (altarLoc == null || altarLoc.getWorld() == null) {
            return;
        }

        final World world = altarLoc.getWorld();
        final int baseX = altarLoc.getBlockX();
        final int baseY = altarLoc.getBlockY();
        final int baseZ = altarLoc.getBlockZ();

        int removedCount = 0;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 1; dy <= 4; dy++) {
                    final Block block = world.getBlockAt(baseX + dx, baseY + dy, baseZ + dz);
                    final Material mat = block.getType();

                    if (intruderMaterials.contains(mat) && !protectedChapelMaterials.contains(mat)) {
                        block.setType(Material.AIR, false);
                        removedCount++;
                    }
                }
            }
        }

        if (removedCount > 0) {
            dev.cosmojar.stellaritypaper.util.DebugLog.log(plugin, "[DEBUG] Очищено " + removedCount + 
                    " вторгшихся блоков в зале Часовни Света на " + baseX + ", " + baseY + ", " + baseZ);
        }
    }
}
