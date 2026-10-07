package dev.cosmojar.stellaritypaper.mechanics.portal;

import dev.cosmojar.stellaritypaper.enchants.StellaritySoundService;
import dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Управляет кинематографичной анимацией активации Портала в Энд (0..170 тиков).
 */
public final class EndPortalCinematicService {

    private static final String SOUND_PORTAL_CREATE = "stellarity:block.end_portal.create";
    private static final String SOUND_PORTAL_OPEN = "stellarity:block.end_portal.open";
    private static final String SOUND_PORTAL_MUSIC = "stellarity:music.create_portal";
    private static final String MODEL_PORTAL_FILL = "stellarity:_particle/end_portal_fill";
    private static final String ADVANCEMENT_JOURNEYS_END = "stellarity:minecraft/story/journeys_end";

    private final Plugin plugin;
    private final StellaritySoundService soundService;
    private final EndPortalAmbientService ambientService;
    private final AdvancementService advancementService;
    private final Map<Location, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public EndPortalCinematicService(
            final Plugin plugin,
            final StellaritySoundService soundService,
            final EndPortalAmbientService ambientService,
            final AdvancementService advancementService
    ) {
        this.plugin = plugin;
        this.soundService = soundService;
        this.ambientService = ambientService;
        this.advancementService = advancementService;
    }

    /**
     * Запускает анимацию активации портала.
     *
     * @param world Мир
     * @param center Точный центр 3x3 портала
     * @param frameLocations Список локаций 12 рамок вокруг портала
     */
    public void startCinematic(final World world, final Location center, final List<Location> frameLocations) {
        if (world == null || center == null) {
            return;
        }

        final Location blockCenter = center.getBlock().getLocation();
        if (activeTasks.containsKey(blockCenter)) {
            return;
        }

        // проверка на то, что ванильные блоки портала пока отсутствуют
        clearPortalArea(world, center);

        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int tick = 0;

            @Override
            public void run() {
                try {
                    // Тик 2: запуск звука создания
                    if (tick == 2) {
                        for (final Player player : world.getNearbyPlayers(center, 48.0D)) {
                            player.stopSound(SoundCategory.MUSIC);
                        }
                        soundService.play(center, SOUND_PORTAL_CREATE, 1.0F, 1.0F, Sound.BLOCK_BEACON_POWER_SELECT);
                    }

                    // Тики 2..130: Дым над рамками и затягивание частиц к центру
                    if (tick >= 2 && tick <= 130) {
                        spawnSmokeAndRunes(world, center, frameLocations);
                    }

                    // Ритмичные вспышки с нарастающей частотой (тики 66, 96, 116, 126)
                    if (tick == 66 || tick == 96 || tick == 116 || tick == 126) {
                        triggerFlash(world, center);
                    }

                    // Тик 130: конец — взрывное открытие портала
                    if (tick == 130) {
                        openPortal(world, center, frameLocations);
                    }

                    // Тик 170: звук открытия портала и завершение сессии анимации
                    if (tick >= 170) {
                        for (final Player player : world.getNearbyPlayers(center, 32.0D)) {
                            player.playSound(player.getLocation(), SOUND_PORTAL_MUSIC, SoundCategory.MUSIC, 0.5F, 1.0F);
                        }
                        stopTask();
                    }
                } catch (final Throwable t) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unexpected error in End Portal cinematic task at tick " + tick, t);
                    if (tick >= 130) {
                        final int cx = center.getBlockX();
                        final int cy = center.getBlockY();
                        final int cz = center.getBlockZ();
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                world.getBlockAt(cx + dx, cy, cz + dz).setType(Material.END_PORTAL, false);
                            }
                        }
                    }
                    stopTask();
                } finally {
                    tick++;
                }
            }

            private void stopTask() {
                final BukkitTask t = activeTasks.remove(blockCenter);
                if (t != null) {
                    t.cancel();
                }
            }
        }, 0L, 1L);

        activeTasks.put(blockCenter, task);
    }

    private void clearPortalArea(final World world, final Location center) {
        final int cx = center.getBlockX();
        final int cy = center.getBlockY();
        final int cz = center.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(cx + dx, cy, cz + dz).setType(Material.AIR, false);
            }
        }
    }

    private void spawnSmokeAndRunes(final World world, final Location center, final List<Location> frameLocations) {
        if (world.getNearbyPlayers(center, 48.0D).isEmpty()) {
            return;
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        // 50% шанс дыма над каждой из 12 рамок
        for (final Location frame : frameLocations) {
            if (random.nextBoolean()) {
                world.spawnParticle(
                        Particle.SMOKE,
                        frame.getX() + 0.5D,
                        frame.getY() + 0.8D,
                        frame.getZ() + 0.5D,
                        1,
                        0.15D, 0.15D, 0.15D,
                        0.006D
                );
            }
        }

        // Затягивающиеся к центру руны
        world.spawnParticle(
                Particle.ENCHANT,
                center.getX() + 0.5D,
                center.getY() + 0.5D,
                center.getZ() + 0.5D,
                6,
                1.2D, 0.15D, 1.2D,
                0.1D
        );
    }

    private void triggerFlash(final World world, final Location center) {
        final Location loc = center.clone().add(0.5D, 0.5D, 0.5D);
        spawnParticleSafe(world, Particle.FLASH, loc, 1, 0.0D, 0.0D, 0.0D, 0.0D, Color.WHITE);
        spawnParticleSafe(world, Particle.END_ROD, loc, 20, 0.0D, 0.0D, 0.0D, 0.1D, null);

        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0F, 1.2F);
        world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 1.0F, 0.05F);
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0F, 0.5F);
    }

    private void openPortal(final World world, final Location center, final List<Location> frameLocations) {
        final Location loc = center.clone().add(0.5D, 0.5D, 0.5D);

        // Взрывные частицы открытия
        spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, loc, 1, 0.0D, 0.0D, 0.0D, 0.0D, 1.0F);
        spawnParticleSafe(world, Particle.END_ROD, loc, 50, 0.0D, 0.0D, 0.0D, 0.33D, null);
        spawnParticleSafe(world, Particle.DRAGON_BREATH, loc, 200, 0.0D, 0.0D, 0.0D, 0.5D, 1.0F);

        // Звуки открытия
        world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 3.0F, 0.75F);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 3.0F, 0.75F);
        soundService.play(loc, SOUND_PORTAL_OPEN, 3.0F, 1.0F, Sound.BLOCK_END_PORTAL_SPAWN);

        // Заполнение 3x3 блоков портала
        final int cx = center.getBlockX();
        final int cy = center.getBlockY();
        final int cz = center.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(cx + dx, cy, cz + dz).setType(Material.END_PORTAL, false);
            }
        }

        // Спавн центрального светящегося ItemDisplay
        final Location displayLoc = center.clone().add(0.5D, 0.2D, 0.5D);
        final ItemDisplay display = world.spawn(displayLoc, ItemDisplay.class, d -> {
            d.setInvulnerable(true);
            d.setGravity(false);
            d.setPersistent(true);
            d.setViewRange(0.6f);
            d.addScoreboardTag("stellarity.end_portal");
            d.addScoreboardTag("smithed.entity");
            d.addScoreboardTag("smithed.strict");
            d.getPersistentDataContainer().set(new NamespacedKey(plugin, "end_portal_fill"), PersistentDataType.BYTE, (byte) 1);

            final ItemStack stick = new ItemStack(Material.STICK);
            final ItemMeta meta = stick.getItemMeta();
            if (meta != null) {
                final NamespacedKey modelKey = NamespacedKey.fromString(MODEL_PORTAL_FILL);
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
            trans.getScale().set(5.92f, 1.0f, 5.92f);
            d.setTransformation(trans);
        });

        // Выдача ачивки
        grantAdvancement(world, center);

        // Регистрация в сервисе фонового эмбиента
        ambientService.registerPortal(center, display.getUniqueId(), frameLocations);
    }

    private void spawnParticleSafe(
            final World world,
            final Particle particle,
            final Location loc,
            final int count,
            final double ox,
            final double oy,
            final double oz,
            final double speed,
            final Object preferredData
    ) {
        if (world == null || loc == null || particle == null) {
            return;
        }
        final Class<?> dataType = particle.getDataType();
        if (dataType == Void.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed);
        } else if (dataType.isInstance(preferredData)) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, preferredData);
        } else if (dataType == Float.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, 1.0F);
        } else if (dataType == Color.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, Color.WHITE);
        } else {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed);
        }
    }

    private void grantAdvancement(final World world, final Location center) {
        if (advancementService == null) {
            return;
        }
        for (final Player player : world.getNearbyPlayers(center, 48.0D)) {
            advancementService.grant(player, ADVANCEMENT_JOURNEYS_END);
        }
    }

    public void cleanupAll() {
        for (final BukkitTask task : activeTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        activeTasks.clear();
    }
}
