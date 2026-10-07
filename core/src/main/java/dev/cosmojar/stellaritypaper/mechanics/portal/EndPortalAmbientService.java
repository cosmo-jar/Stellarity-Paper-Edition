package dev.cosmojar.stellaritypaper.mechanics.portal;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Высокопроизводительный сервис фонового эмбиента активных Порталов в Энд.
 * Включает режим сна (Sleep Mode), когда рядом нет игроков, и расчёт частиц без создания энтити.
 */
public final class EndPortalAmbientService {

    private static final double VIEW_DISTANCE = 48.0D;
    private static final Particle.DustOptions MAGENTA_DUST = new Particle.DustOptions(Color.fromRGB(237, 19, 227), 1.4F);
    private static final float[] ANCHOR_PITCHES = {0.5F, 0.55F, 0.6F, 0.65F, 0.7F};

    private final Plugin plugin;
    private final Map<Location, ActivePortal> activePortals = new ConcurrentHashMap<>();
    private BukkitTask loopTask;

    public EndPortalAmbientService(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (loopTask != null) {
            return;
        }

        // Сканирование существующих порталов в загруженных мирах
        scanExistingPortals();

        // Главный цикл эмбиента
        this.loopTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void registerPortal(final Location center, final UUID displayId, final List<Location> frameLocations) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        final Location key = center.getBlock().getLocation();
        final ActivePortal portal = new ActivePortal(center, displayId, frameLocations != null ? frameLocations : new ArrayList<>());
        activePortals.put(key, portal);
    }

    public void removePortal(final Location loc) {
        if (loc == null) {
            return;
        }
        final Location key = loc.getBlock().getLocation();
        final ActivePortal portal = activePortals.remove(key);
        if (portal != null) {
            removeDisplay(portal);
            return;
        }
        // Если была сломана рамка вокруг портала или соседний блок - попытка найти связанный портал
        for (final Iterator<Map.Entry<Location, ActivePortal>> it = activePortals.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<Location, ActivePortal> entry = it.next();
            if (entry.getValue().frames.contains(key) || (entry.getKey().getWorld().equals(loc.getWorld()) && entry.getKey().distanceSquared(loc) <= 16.0)) {
                removeDisplay(entry.getValue());
                it.remove();
            }
        }
    }

    public void onChunkLoad(final Chunk chunk) {
        if (chunk == null) {
            return;
        }
        for (final Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemDisplay display && display.getScoreboardTags().contains("stellarity.end_portal")) {
                final Location center = display.getLocation().getBlock().getLocation();
                if (center.getBlock().getType() == Material.END_PORTAL) {
                    if (!activePortals.containsKey(center)) {
                        registerPortal(center, display.getUniqueId(), findSurroundingFrames(center));
                    }
                } else {
                    // если портал был разрушен пока чанк был выгружен
                    display.remove();
                }
            }
        }
    }

    public void onChunkUnload(final Chunk chunk) {
        if (chunk == null) {
            return;
        }
        final int cx = chunk.getX();
        final int cz = chunk.getZ();
        activePortals.entrySet().removeIf(entry -> {
            final Location loc = entry.getKey();
            if (loc.getWorld().equals(chunk.getWorld()) && (loc.getBlockX() >> 4) == cx && (loc.getBlockZ() >> 4) == cz) {
                entry.getValue().activeWisps.clear();
                return true;
            }
            return false;
        });
    }

    private void tick() {
        if (activePortals.isEmpty()) {
            return;
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        for (final Iterator<Map.Entry<Location, ActivePortal>> it = activePortals.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<Location, ActivePortal> entry = it.next();
            final ActivePortal portal = entry.getValue();
            final World world = portal.center.getWorld();

            if (world == null || !portal.center.isChunkLoaded()) {
                continue;
            }

            // (Sleep Mode) - если игроков нет в радиусе 48 блоков
            final Collection<Player> nearby = world.getNearbyPlayers(portal.center, VIEW_DISTANCE);
            if (nearby.isEmpty()) {
                portal.activeWisps.clear();
                continue;
            }
            // Проверка целостности портала раз в 40 тиков (только когда рядом есть игроки)
            portal.integrityCheckTimer++;
            if (portal.integrityCheckTimer >= 40) {
                portal.integrityCheckTimer = 0;
                if (portal.center.getBlock().getType() != Material.END_PORTAL) {
                    removeDisplay(portal);
                    it.remove();
                    continue;
                }
            }

            world.spawnParticle(
                    Particle.MYCELIUM,
                    portal.center.getX() + 0.5D,
                    portal.center.getY() + 0.85D,
                    portal.center.getZ() + 0.5D,
                    2,
                    0.8D, 0.0D, 0.8D,
                    0.0D
            );

            // Шанс 3% за тик запустить восходящий пучок энергии
            if (random.nextDouble() < 0.03D) {
                final double ox = (random.nextDouble() - 0.5D) * 2.4D;
                final double oz = (random.nextDouble() - 0.5D) * 2.4D;
                final Location startLoc = portal.center.clone().add(0.5D + ox, 0.8D, 0.5D + oz);
                final Vector vel = new Vector(
                        (random.nextDouble() - 0.5D) * 0.02D,
                        0.15D + random.nextDouble() * 0.05D,
                        (random.nextDouble() - 0.5D) * 0.02D
                );
                portal.activeWisps.add(new PortalWisp(startLoc, vel));
            }

            // Обновление восходящих пучков частиц
            for (final Iterator<PortalWisp> wispIt = portal.activeWisps.iterator(); wispIt.hasNext(); ) {
                final PortalWisp wisp = wispIt.next();
                wisp.age++;
                wisp.pos.add(wisp.velocity);

                if (wisp.age >= wisp.maxAge || wisp.pos.getBlock().getType().isSolid()) {
                    wispIt.remove();
                    continue;
                }

                // поворот пучка как в ориге
                if (wisp.age > 5 && random.nextDouble() < 0.2D) {
                    wisp.velocity.rotateAroundY((random.nextDouble() - 0.5D) * 0.3D);
                }

                world.spawnParticle(Particle.DUST, wisp.pos, 1, 0.0D, 0.0D, 0.0D, 0.0D, MAGENTA_DUST);
                spawnParticleSafe(world, Particle.DRAGON_BREATH, wisp.pos, 1, 0.0D, 0.0D, 0.0D, 0.01D, 1.0F);
            }

            // звуки (каждые 10 сек.)
            portal.ambientSoundTimer++;
            if (portal.ambientSoundTimer >= 200) {
                portal.ambientSoundTimer = 0;
                if (random.nextDouble() < 0.33D) {
                    world.playSound(portal.center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.15F, 1.0F);
                }
                final float pitch = ANCHOR_PITCHES[random.nextInt(ANCHOR_PITCHES.length)];
                world.playSound(portal.center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 1.0F, pitch);
            }
        }
    }

    private void scanExistingPortals() {
        for (final World world : Bukkit.getWorlds()) {
            for (final Chunk chunk : world.getLoadedChunks()) {
                onChunkLoad(chunk);
            }
        }
    }

    private void removeDisplay(final ActivePortal portal) {
        if (portal.displayId == null) {
            return;
        }
        final Entity entity = Bukkit.getEntity(portal.displayId);
        if (entity != null) {
            entity.remove();
        }
    }

    private List<Location> findSurroundingFrames(final Location center) {
        final List<Location> frames = new ArrayList<>(12);
        final World world = center.getWorld();
        final int cx = center.getBlockX();
        final int cy = center.getBlockY();
        final int cz = center.getBlockZ();

        // North & South
        for (int dx = -1; dx <= 1; dx++) {
            frames.add(new Location(world, cx + dx, cy, cz - 2));
            frames.add(new Location(world, cx + dx, cy, cz + 2));
        }
        // East & West
        for (int dz = -1; dz <= 1; dz++) {
            frames.add(new Location(world, cx + 2, cy, cz + dz));
            frames.add(new Location(world, cx - 2, cy, cz + dz));
        }

        return frames;
    }

    public void cleanupAll() {
        if (loopTask != null) {
            loopTask.cancel();
            loopTask = null;
        }
        for (final ActivePortal portal : activePortals.values()) {
            portal.activeWisps.clear();
        }
        activePortals.clear();
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

    private static final class ActivePortal {
        private final Location center;
        private final UUID displayId;
        private final List<Location> frames;
        private final List<PortalWisp> activeWisps = new ArrayList<>();
        private int ambientSoundTimer = 0;
        private int integrityCheckTimer = 0;

        private ActivePortal(final Location center, final UUID displayId, final List<Location> frames) {
            this.center = center;
            this.displayId = displayId;
            this.frames = frames;
        }
    }

    private static final class PortalWisp {
        private final Location pos;
        private final Vector velocity;
        private int age = 0;
        private final int maxAge = 100;

        private PortalWisp(final Location pos, final Vector velocity) {
            this.pos = pos;
            this.velocity = velocity;
        }
    }
}
