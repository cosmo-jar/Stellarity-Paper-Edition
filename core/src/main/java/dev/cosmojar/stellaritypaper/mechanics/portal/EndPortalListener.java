package dev.cosmojar.stellaritypaper.mechanics.portal;

import dev.cosmojar.stellaritypaper.enchants.StellaritySoundService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Слушатель событий для анимации рамок и перехвата открытия Портала в Энд.
 */
public final class EndPortalListener implements Listener {

    private static final String SOUND_FRAME_FILL = "stellarity:block.end_portal_frame.fill";
    private static final String MODEL_FRAME_FILL = "stellarity:_particle/end_portal_fill";

    private final Plugin plugin;
    private final StellaritySoundService soundService;
    private final EndPortalCinematicService cinematicService;
    private final EndPortalAmbientService ambientService;

    public EndPortalListener(
            final Plugin plugin,
            final StellaritySoundService soundService,
            final EndPortalCinematicService cinematicService,
            final EndPortalAmbientService ambientService
    ) {
        this.plugin = plugin;
        this.soundService = soundService;
        this.cinematicService = cinematicService;
        this.ambientService = ambientService;
    }

    /**
     * Анимация и звук при установке ока в рамку портала, а также проверка активации 12 рамок.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.END_PORTAL_FRAME) {
            return;
        }

        final ItemStack handItem = event.getItem();
        if (handItem == null || handItem.getType() != Material.ENDER_EYE) {
            return;
        }

        if (!(clicked.getBlockData() instanceof EndPortalFrame frame) || frame.hasEye()) {
            return;
        }

        // Кастомный звук установки ока
        final Location soundLoc = clicked.getLocation().add(0.5D, 0.5D, 0.5D);
        soundService.play(soundLoc, SOUND_FRAME_FILL, 1.0F, 1.0F, Sound.BLOCK_END_PORTAL_FRAME_FILL);

        // Спавн ItemDisplay вспышки (30 тиков) над установленным оком с моделью из ресурс-пака
        final Location displayLoc = clicked.getLocation().add(0.5D, 0.95D, 0.5D);
        final ItemDisplay display = clicked.getWorld().spawn(displayLoc, ItemDisplay.class, d -> {
            d.setInvulnerable(true);
            d.setGravity(false);
            d.setPersistent(false);
            d.addScoreboardTag("smithed.entity");
            d.addScoreboardTag("smithed.strict");

            final ItemStack stick = new ItemStack(Material.STICK);
            final ItemMeta meta = stick.getItemMeta();
            if (meta != null) {
                final NamespacedKey modelKey = NamespacedKey.fromString(MODEL_FRAME_FILL);
                if (modelKey != null) {
                    meta.setItemModel(modelKey);
                }
                stick.setItemMeta(meta);
            }
            d.setItemStack(stick);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(Display.Billboard.FIXED);

            final Transformation trans = d.getTransformation();
            trans.getScale().set(1.0f, 1.35f, 1.0f);
            d.setTransformation(trans);

            d.setInterpolationDuration(25);
            d.setInterpolationDelay(0);
        });

        // Частицы при вставке
        clicked.getWorld().spawnParticle(Particle.END_ROD, displayLoc, 8, 0.15D, 0.15D, 0.15D, 0.05D);
        clicked.getWorld().spawnParticle(Particle.ENCHANT, displayLoc, 15, 0.25D, 0.25D, 0.25D, 0.5D);

        // Плавное затухание луча вспышки
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (display.isValid()) {
                final Transformation trans = display.getTransformation();
                trans.getScale().set(1.0f, 0.0f, 1.0f);
                display.setTransformation(trans);
            }
        }, 5L);

        Bukkit.getScheduler().runTaskLater(plugin, display::remove, 30L);

        // Проверка, привело ли это действие к заполнению 12 рамок и появлению портала.
        // Сначала проверка на текущем тике, а если ванилла ещё не успела выставить блоки — повтор через 1 тик.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!checkForPortalActivation(clicked)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> checkForPortalActivation(clicked), 1L);
            }
        });
    }

    private boolean checkForPortalActivation(final Block frameBlock) {
        final World world = frameBlock.getWorld();
        final int fx = frameBlock.getX();
        final int fy = frameBlock.getY();
        final int fz = frameBlock.getZ();

        // проверка соседних 4 блоков по горизонтали
        Block portalSeed = null;
        final int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (final int[] off : offsets) {
            final Block b = world.getBlockAt(fx + off[0], fy, fz + off[1]);
            if (b.getType() == Material.END_PORTAL) {
                portalSeed = b;
                break;
            }
        }

        if (portalSeed == null) {
            return false;
        }

        // поиск всех блоков портала в области 5x5 вокруг точки
        final List<Block> portalBlocks = new ArrayList<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                final Block b = world.getBlockAt(portalSeed.getX() + dx, fy, portalSeed.getZ() + dz);
                if (b.getType() == Material.END_PORTAL) {
                    portalBlocks.add(b);
                    if (b.getX() < minX) minX = b.getX();
                    if (b.getX() > maxX) maxX = b.getX();
                    if (b.getZ() < minZ) minZ = b.getZ();
                    if (b.getZ() > maxZ) maxZ = b.getZ();
                }
            }
        }

        // Если нашлись блоки портала
        if (portalBlocks.isEmpty()) {
            return false;
        }

        // Мгновенно убираются блоки портала на время анимации
        for (final Block b : portalBlocks) {
            b.setType(Material.AIR, false);
        }

        final int centerX = (minX + maxX) / 2;
        final int centerZ = (minZ + maxZ) / 2;
        final Location center = new Location(world, centerX, fy, centerZ);

        // поиск 12 рамок вокруг центра 3x3
        final List<Location> frameLocations = new ArrayList<>(12);
        for (int dx = -1; dx <= 1; dx++) {
            frameLocations.add(new Location(world, centerX + dx, fy, centerZ - 2));
            frameLocations.add(new Location(world, centerX + dx, fy, centerZ + 2));
        }
        for (int dz = -1; dz <= 1; dz++) {
            frameLocations.add(new Location(world, centerX + 2, fy, centerZ + dz));
            frameLocations.add(new Location(world, centerX - 2, fy, centerZ + dz));
        }

        // Запуск анимации открытия
        cinematicService.startCinematic(world, center, frameLocations);
        return true;
    }

    /**
     * Очистка активного портала при разрушении его блока.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Material type = event.getBlock().getType();
        if (type == Material.END_PORTAL || type == Material.END_PORTAL_FRAME) {
            ambientService.removePortal(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(final ChunkLoadEvent event) {
        ambientService.onChunkLoad(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(final ChunkUnloadEvent event) {
        ambientService.onChunkUnload(event.getChunk());
    }
}
