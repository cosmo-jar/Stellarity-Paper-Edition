package dev.cosmojar.stellaritypaper.mechanics.structures;

import dev.cosmojar.stellaritypaper.text.MessageService;
import dev.cosmojar.stellaritypaper.util.DebugLog;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.StructureSearchResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EndCityCrystalService {

    private final Plugin plugin;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;
    private final MessageService messageService;

    private final NamespacedKey crystalTagKey;
    private final NamespacedKey crystalStateKey;
    private final NamespacedKey crystalStructureKey;
    private final NamespacedKey crystalXKey;
    private final NamespacedKey crystalYKey;
    private final NamespacedKey crystalZKey;

    private final ConcurrentMap<String, Boolean> conqueredCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> cityHasShipCache = new ConcurrentHashMap<>();

    public EndCityCrystalService(
            final Plugin plugin,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService,
            final MessageService messageService
    ) {
        this.plugin = plugin;
        this.advancementService = advancementService;
        this.messageService = messageService;

        this.crystalTagKey = new NamespacedKey(plugin, "end_city_crystal");
        this.crystalStateKey = new NamespacedKey(plugin, "crystal_state");
        this.crystalStructureKey = new NamespacedKey(plugin, "structure_key");
        this.crystalXKey = new NamespacedKey(plugin, "crystal_x");
        this.crystalYKey = new NamespacedKey(plugin, "crystal_y");
        this.crystalZKey = new NamespacedKey(plugin, "crystal_z");
    }

    public void prepareCrystal(final EnderCrystal crystal, final String crystalType) {
        if (crystal == null) {
            return;
        }
        final Location loc = crystal.getLocation();
        prepareAndRegisterCrystal(crystal, crystalType, loc, getStructureKey(loc));
    }

    public void prepareAndRegisterCrystal(
            final EnderCrystal crystal,
            final String crystalType,
            final Location loc,
            final String structureKey
    ) {
        if (crystal == null || loc == null) {
            return;
        }
        crystal.setGlowing(true);
        crystal.setShowingBottom(true);

        final PersistentDataContainer pdc = crystal.getPersistentDataContainer();
        pdc.set(crystalTagKey, PersistentDataType.STRING, crystalType != null ? crystalType : "MAIN");
        pdc.set(crystalStateKey, PersistentDataType.STRING, "INTACT");

        final int bx = loc.getBlockX();
        final int by = loc.getBlockY();
        final int bz = loc.getBlockZ();

        pdc.set(crystalXKey, PersistentDataType.INTEGER, bx);
        pdc.set(crystalYKey, PersistentDataType.INTEGER, by);
        pdc.set(crystalZKey, PersistentDataType.INTEGER, bz);

        if (structureKey != null && !structureKey.isBlank()) {
            pdc.set(crystalStructureKey, PersistentDataType.STRING, structureKey);
            registerCrystal(loc.getWorld(), structureKey, bx, by, bz);
        }
    }

    public String readStructureKey(final TileState tileState) {
        if (tileState == null) {
            return null;
        }
        return tileState.getPersistentDataContainer().get(crystalStructureKey, PersistentDataType.STRING);
    }

    public String readStructureKey(final EnderCrystal crystal) {
        if (crystal == null) {
            return null;
        }
        return crystal.getPersistentDataContainer().get(crystalStructureKey, PersistentDataType.STRING);
    }

    public GeneratedStructure getGeneratedStructure(final Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        final World world = loc.getWorld();
        final int chunkX = loc.getBlockX() >> 4;
        final int chunkZ = loc.getBlockZ() >> 4;

        try {
            final Collection<GeneratedStructure> structures = world.getStructures(chunkX, chunkZ, Structure.END_CITY);
            if (!structures.isEmpty()) {
                return structures.iterator().next();
            }
        } catch (Throwable ignored) {
        }

        try {
            final StructureSearchResult search = world.locateNearestStructure(loc, Structure.END_CITY, 4, false);
            if (search != null && search.getLocation() != null) {
                final Location sLoc = search.getLocation();
                final Collection<GeneratedStructure> searchStructures = world.getStructures(sLoc.getBlockX() >> 4, sLoc.getBlockZ() >> 4, Structure.END_CITY);
                if (!searchStructures.isEmpty()) {
                    return searchStructures.iterator().next();
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public String getStructureKey(final Location loc) {
        final GeneratedStructure gs = getGeneratedStructure(loc);
        if (gs != null) {
            final BoundingBox box = gs.getBoundingBox();
            return "end_city_" + ((int) Math.floor(box.getMinX())) + "_" + ((int) Math.floor(box.getMinZ()))
                    + "_" + ((int) Math.floor(box.getMaxX())) + "_" + ((int) Math.floor(box.getMaxZ()));
        }
        if (loc != null) {
            return "end_city_" + (loc.getBlockX() >> 4) + "_" + (loc.getBlockZ() >> 4);
        }
        return null;
    }

    private NamespacedKey getShipKey(final String structureKey) {
        final String sanitized = structureKey.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        final String hash = Integer.toHexString(structureKey.hashCode());
        final String truncated = sanitized.length() > 25 ? sanitized.substring(0, 25) : sanitized;
        final String keyName = "ec_ship_" + hash + "_" + truncated;
        return new NamespacedKey(plugin, keyName);
    }

    public synchronized void registerCityHasShip(final World world, final String structureKey) {
        if (world == null || structureKey == null) {
            return;
        }
        final String cacheKey = world.getUID() + ":" + structureKey;
        cityHasShipCache.put(cacheKey, Boolean.TRUE);
        final NamespacedKey key = getShipKey(structureKey);
        world.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        DebugLog.log(plugin, "[DEBUG] Зарегистрировано наличие корабля для структуры: " + structureKey);
    }

    public boolean hasShipInCity(final World world, final String structureKey, final Location searchLoc) {
        if (world == null || structureKey == null) {
            return false;
        }

        final String cacheKey = world.getUID() + ":" + structureKey;
        final Boolean cached = cityHasShipCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        final NamespacedKey shipKey = getShipKey(structureKey);
        if (world.getPersistentDataContainer().has(shipKey, PersistentDataType.BYTE)) {
            cityHasShipCache.put(cacheKey, Boolean.TRUE);
            return true;
        }

        final Location loc = searchLoc != null ? searchLoc : world.getSpawnLocation();
        final GeneratedStructure gs = getGeneratedStructure(loc);
        if (gs != null) {
            if (hasShipPiece(gs)) {
                registerCityHasShip(world, structureKey);
                return true;
            }
        }

        if (hasShipFeatureNearby(loc, gs)) {
            registerCityHasShip(world, structureKey);
            return true;
        }

        cityHasShipCache.put(cacheKey, Boolean.FALSE);
        return false;
    }

    public boolean hasShipPiece(final GeneratedStructure gs) {
        if (gs == null) {
            return false;
        }
        try {
            final Collection<StructurePiece> pieces = gs.getPieces();
            if (pieces == null || pieces.isEmpty()) {
                return false;
            }
            for (final StructurePiece piece : pieces) {
                if (piece == null) continue;
                final String pStr = piece.toString().toLowerCase(Locale.ROOT);
                if (pStr.contains("ship")) {
                    return true;
                }
                try {
                    final Method getHandle = piece.getClass().getMethod("getHandle");
                    final Object handle = getHandle.invoke(piece);
                    if (handle != null) {
                        final String hStr = handle.toString().toLowerCase(Locale.ROOT);
                        if (hStr.contains("ship")) {
                            return true;
                        }
                        Class<?> clz = handle.getClass();
                        while (clz != null && clz != Object.class) {
                            for (final Field f : clz.getDeclaredFields()) {
                                try {
                                    f.setAccessible(true);
                                    final Object val = f.get(handle);
                                    if (val != null && val.toString().toLowerCase(Locale.ROOT).contains("ship")) {
                                        return true;
                                    }
                                } catch (Throwable ignored) {}
                            }
                            clz = clz.getSuperclass();
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean hasShipFeatureNearby(final Location loc, final GeneratedStructure gs) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        final World world = loc.getWorld();
        final BoundingBox box = gs != null ? gs.getBoundingBox().clone().expand(16) : null;
        final int minX = box != null ? (int) Math.floor(box.getMinX()) : loc.getBlockX() - 48;
        final int maxX = box != null ? (int) Math.ceil(box.getMaxX()) : loc.getBlockX() + 48;
        final int minZ = box != null ? (int) Math.floor(box.getMinZ()) : loc.getBlockZ() - 48;
        final int maxZ = box != null ? (int) Math.ceil(box.getMaxZ()) : loc.getBlockZ() + 48;

        final int startChunkX = minX >> 4;
        final int endChunkX = maxX >> 4;
        final int startChunkZ = minZ >> 4;
        final int endChunkZ = maxZ >> 4;

        for (int cx = startChunkX; cx <= endChunkX; cx++) {
            for (int cz = startChunkZ; cz <= endChunkZ; cz++) {
                if (world.isChunkLoaded(cx, cz)) {
                    final Chunk chunk = world.getChunkAt(cx, cz);
                    for (final BlockState state : chunk.getTileEntities(false)) {
                        if (state.getType() == Material.DRAGON_HEAD || state.getType() == Material.DRAGON_WALL_HEAD) {
                            return true;
                        }
                        if (state instanceof TileState tileState && state.getType() == Material.VAULT) {
                            final String vType = tileState.getPersistentDataContainer().get(
                                    new NamespacedKey(plugin, "vault_type"), PersistentDataType.STRING);
                            if ("VAULT_ELYTRA".equalsIgnoreCase(vType)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private NamespacedKey getCrystalsKey(final String structureKey) {
        final String sanitized = structureKey.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        final String hash = Integer.toHexString(structureKey.hashCode());
        final String truncated = sanitized.length() > 25 ? sanitized.substring(0, 25) : sanitized;
        final String keyName = "ec_crys_" + hash + "_" + truncated;
        return new NamespacedKey(plugin, keyName);
    }

    private NamespacedKey getConqueredKey(final String structureKey) {
        final String sanitized = structureKey.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        final String hash = Integer.toHexString(structureKey.hashCode());
        final String truncated = sanitized.length() > 25 ? sanitized.substring(0, 25) : sanitized;
        final String keyName = "ec_conq_" + hash + "_" + truncated;
        return new NamespacedKey(plugin, keyName);
    }

    private Set<String> loadCrystalPositions(final World world, final String structureKey) {
        if (world == null || structureKey == null) {
            return new LinkedHashSet<>();
        }
        final NamespacedKey key = getCrystalsKey(structureKey);
        final String raw = world.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        final Set<String> positions = new LinkedHashSet<>();
        if (raw != null && !raw.isBlank()) {
            for (final String part : raw.split(";")) {
                final String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    positions.add(trimmed);
                }
            }
        }
        return positions;
    }

    private void saveCrystalPositions(final World world, final String structureKey, final Set<String> positions) {
        if (world == null || structureKey == null) {
            return;
        }
        final NamespacedKey key = getCrystalsKey(structureKey);
        if (positions == null || positions.isEmpty()) {
            world.getPersistentDataContainer().remove(key);
        } else {
            final String raw = String.join(";", positions);
            world.getPersistentDataContainer().set(key, PersistentDataType.STRING, raw);
        }
    }

    public synchronized void registerCrystal(final World world, final String structureKey, final int x, final int y, final int z) {
        if (world == null || structureKey == null) {
            return;
        }
        final Set<String> positions = loadCrystalPositions(world, structureKey);
        final String posStr = x + "," + y + "," + z;
        if (positions.add(posStr)) {
            saveCrystalPositions(world, structureKey, positions);
            DebugLog.log(plugin, "[DEBUG] Зарегистрирован Защитный Кристалл для " + structureKey
                    + ": " + posStr + " (всего активных: " + positions.size() + ")");
        }
    }

    public synchronized void unregisterCrystal(final World world, final String structureKey, final int x, final int y, final int z) {
        if (world == null || structureKey == null) {
            return;
        }
        final Set<String> positions = loadCrystalPositions(world, structureKey);
        final String posStr = x + "," + y + "," + z;
        if (positions.remove(posStr)) {
            saveCrystalPositions(world, structureKey, positions);
            DebugLog.log(plugin, "[DEBUG] Кристалл " + posStr + " снят с защиты структуры " + structureKey
                    + " (осталось кристаллов: " + positions.size() + ")");
        }
    }

    public boolean isCityConquered(final World world, final String structureKey) {
        if (world == null || structureKey == null) {
            return false;
        }
        final String cacheKey = world.getUID() + ":" + structureKey;
        if (conqueredCache.getOrDefault(cacheKey, Boolean.FALSE)) {
            return true;
        }
        final NamespacedKey conqKey = getConqueredKey(structureKey);
        final boolean conquered = world.getPersistentDataContainer().has(conqKey, PersistentDataType.BYTE);
        if (conquered) {
            conqueredCache.put(cacheKey, Boolean.TRUE);
        }
        return conquered;
    }

    public synchronized boolean markCityConquered(final World world, final String structureKey) {
        if (world == null || structureKey == null) {
            return false;
        }
        if (isCityConquered(world, structureKey)) {
            return false;
        }
        final NamespacedKey conqKey = getConqueredKey(structureKey);
        world.getPersistentDataContainer().set(conqKey, PersistentDataType.BYTE, (byte) 1);
        final String cacheKey = world.getUID() + ":" + structureKey;
        conqueredCache.put(cacheKey, Boolean.TRUE);
        DebugLog.log(plugin, "[DEBUG] Город Края " + structureKey + " отмечен как ПОКОРЁННЫЙ!");
        return true;
    }

    public boolean hasRemainingCrystals(final World world, final String structureKey) {
        if (world == null || structureKey == null) {
            return false;
        }
        if (isCityConquered(world, structureKey)) {
            return false;
        }
        final Set<String> positions = loadCrystalPositions(world, structureKey);
        return !positions.isEmpty();
    }

    public boolean isEndCityCrystal(final EnderCrystal crystal) {
        if (crystal == null) {
            return false;
        }
        return crystal.getPersistentDataContainer().has(crystalTagKey, PersistentDataType.STRING);
    }

    public boolean isDestroying(final EnderCrystal crystal) {
        if (crystal == null) {
            return false;
        }
        final String state = crystal.getPersistentDataContainer().get(crystalStateKey, PersistentDataType.STRING);
        return "DESTROYING".equalsIgnoreCase(state);
    }

    public void startDestructionSequence(
            final EnderCrystal crystal,
            final Player attacker,
            final EndStructuresService endStructuresService
    ) {
        if (crystal == null || !crystal.isValid() || isDestroying(crystal)) {
            return;
        }

        final PersistentDataContainer pdc = crystal.getPersistentDataContainer();
        final String crystalType = pdc.getOrDefault(crystalTagKey, PersistentDataType.STRING, "MAIN");
        pdc.set(crystalStateKey, PersistentDataType.STRING, "DESTROYING");

        final Location loc = crystal.getLocation();
        final World world = loc.getWorld();
        if (world == null) {
            crystal.remove();
            return;
        }

        world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);

        DebugLog.log(plugin, "[DEBUG] Уничтожения Защитного Кристалла на "
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (!crystal.isValid() || crystal.isDead()) {
                    cancel();
                    return;
                }

                final Location particleLoc = crystal.getLocation().add(0, 1.0, 0);
                if (ticks <= 15) {
                    world.spawnParticle(Particle.ENCHANT, particleLoc, 8, 0.4, 0.4, 0.4, 0.5);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 8, 0.4, 0.4, 0.4, 0.5);
                }

                if (ticks >= 20) {
                    cancel();
                    final Location finalLoc = crystal.getLocation();

                    world.spawnParticle(Particle.FLASH, finalLoc, 1, 0, 0, 0, 0, Color.WHITE);
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, finalLoc, 1, 0, 0, 0, 0);
                    world.playSound(finalLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.75f);
                    world.playSound(finalLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.0f);

                    final int cx = pdc.getOrDefault(crystalXKey, PersistentDataType.INTEGER, finalLoc.getBlockX());
                    final int cy = pdc.getOrDefault(crystalYKey, PersistentDataType.INTEGER, finalLoc.getBlockY());
                    final int cz = pdc.getOrDefault(crystalZKey, PersistentDataType.INTEGER, finalLoc.getBlockZ());
                    String structKey = pdc.get(crystalStructureKey, PersistentDataType.STRING);
                    if (structKey == null || structKey.isBlank()) {
                        structKey = getStructureKey(finalLoc);
                    }

                    crystal.remove();

                    if (structKey != null) {
                        unregisterCrystal(world, structKey, cx, cy, cz);
                    }

                    // попытка выдать crystal_crusher атакующему и игрокам поблизости
                    if (advancementService != null) {
                        final Set<Player> targets = new java.util.HashSet<>(world.getNearbyPlayers(finalLoc, 48.0));
                        if (attacker != null) {
                            targets.add(attacker);
                        }
                        for (final Player p : targets) {
                            advancementService.grant(p, "stellarity:end_city/crystal_crusher");
                        }
                    }

                    // проверка, был ли этот кристалл последним в этой структуре
                    final boolean hasRemaining = structKey != null && hasRemainingCrystals(world, structKey);

                    if (!hasRemaining && structKey != null) {
                        if (markCityConquered(world, structKey)) {
                            // выдача ачивки conqueror атакующему и игрокам поблизости
                            if (advancementService != null) {
                                final Set<Player> conqTargets = new java.util.HashSet<>(world.getNearbyPlayers(finalLoc, 64.0));
                                if (attacker != null) {
                                    conqTargets.add(attacker);
                                }
                                for (final Player p : conqTargets) {
                                    advancementService.grant(p, "stellarity:end_city/conqueror");
                                }
                            }

                            final boolean hasShip = hasShipInCity(world, structKey, finalLoc);
                            DebugLog.log(plugin, "[DEBUG] Город Края " + structKey + " покорён. Наличие корабля/элитр: " + hasShip);

                            if (hasShip) {
                                // сообщение и звук разблокировки хранилища если корабль с элитрами
                                final Component unlockMsg = messageService != null
                                        ? messageService.message("mechanics.vault.unlocked_somewhere")
                                        : null;
                                for (final Player p : world.getNearbyPlayers(finalLoc, 128.0)) {
                                    if (unlockMsg != null) {
                                        p.sendActionBar(unlockMsg);
                                    }
                                    p.playSound(p.getLocation(), Sound.BLOCK_VAULT_DEACTIVATE, 1.0f, 0.8f);
                                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
                                }
                            } else {
                                // если корабля нет
                                for (final Player p : world.getNearbyPlayers(finalLoc, 128.0)) {
                                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
                                }
                            }
                        }
                    }

                    // замена кристалла на хранилище
                    if (endStructuresService != null) {
                        endStructuresService.replaceCrystalWithVault(finalLoc, crystalType);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
