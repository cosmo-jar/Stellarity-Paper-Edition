package dev.cosmojar.stellaritypaper.mechanics.structures;

import dev.cosmojar.stellaritypaper.util.DebugLog;

import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureGenerateEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.EntityTransformer;
import org.bukkit.util.BlockTransformer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EndStructuresService implements Listener {

    private static final String FN_VAULT = "stellarity:structure/end_city/vault";
    private static final String FN_VAULT_ELYTRA = "stellarity:structure/end_city/vault_elytra";
    private static final String FN_VAULT_OMINOUS = "stellarity:structure/end_city/vault_ominous";
    private static final String FN_SPAWNER = "stellarity:structure/end_city/spawner";
    private static final String FN_SPAWNER_ELYTRA = "stellarity:structure/end_city/spawner_elytra";
    private static final String FN_SHIP_MIDDLE = "stellarity:structure/end_city/ship/middle";
    private static final String FN_SHIP_ELYTRA = "stellarity:structure/end_city/ship/elytra";
    private static final String FN_BASE_ROOM_GENERATE = "stellarity:structure/end_city/base_room/generate";
    private static final String FN_BASE_ROOF_ROOM = "stellarity:structure/end_city/base_roof/room";
    private static final String FN_TOP_TOWER_FLOOR_1 = "stellarity:structure/end_city/top_tower/floor_1";
    private static final String FN_TOP_TOWER_FLOOR_2 = "stellarity:structure/end_city/top_tower/floor_2";

    private static final String FN_DECOR_1X = "stellarity:structure/end_city/decorations/1x";
    private static final String FN_DECOR_2X = "stellarity:structure/end_city/decorations/2x";
    private static final String FN_DECOR_3X = "stellarity:structure/end_city/decorations/3x";
    private static final String FN_BREWING_STAND = "stellarity:structure/end_city/decorations/brewing_stand";
    private static final String FN_BASE_ROOF_CHAINS = "stellarity:structure/end_city/base_roof/chains";
    private static final String FN_TOP_TOWER_CHAINS = "stellarity:structure/end_city/top_tower/chains";
    private static final String FN_SPAWN_CRYSTAL = "stellarity:structure/end_city/spawn_crystal";
    private static final String FN_SPAWN_CRYSTAL_SMALL_TOWER = "stellarity:structure/end_city/spawn_crystal_small_tower";
    private static final String FN_CLEAR_ENTRANCE = "stellarity:structure/end_city/base_room/clear_entrance";

    private static final List<String> TPL_SHIP_MIDDLE = List.of(
            "stellarity:end_city/ship/middle/1",
            "stellarity:end_city/ship/middle/2",
            "stellarity:end_city/ship/middle/3"
    );
    private static final List<String> TPL_SHIP_ELYTRA = List.of(
            "stellarity:end_city/ship/elytra/1",
            "stellarity:end_city/ship/elytra/2",
            "stellarity:end_city/ship/elytra/3"
    );
    private static final List<String> TPL_BASE_ROOM = List.of(
            "stellarity:end_city/base_floor/1",
            "stellarity:end_city/base_floor/2",
            "stellarity:end_city/base_floor/3",
            "stellarity:end_city/base_floor/4"
    );
    private static final List<String> TPL_BASE_ROOF_ROOM = List.of(
            "stellarity:end_city/base_roof/1",
            "stellarity:end_city/base_roof/2",
            "stellarity:end_city/base_roof/3",
            "stellarity:end_city/base_roof/4",
            "stellarity:end_city/base_roof/5",
            "stellarity:end_city/base_roof/6"
    );
    private static final List<String> TPL_TOP_TOWER_FLOOR_1 = List.of(
            "stellarity:end_city/top_tower/floor_1/1",
            "stellarity:end_city/top_tower/floor_1/2",
            "stellarity:end_city/top_tower/floor_1/3"
    );
    private static final List<String> TPL_TOP_TOWER_FLOOR_2 = List.of(
            "stellarity:end_city/top_tower/floor_2/1",
            "stellarity:end_city/top_tower/floor_2/2",
            "stellarity:end_city/top_tower/floor_2/3"
    );

    private static final List<String> TPL_DECOR_2X = createDecor2xList();
    private static final List<String> TPL_DECOR_3X = createDecor3xList();

    private static List<String> createDecor2xList() {
        final List<String> list = new ArrayList<>();
        for (int i = 1; i <= 23; i++) {
            list.add("stellarity:end_city/decorations/2x/" + i);
        }
        return List.copyOf(list);
    }

    private static List<String> createDecor3xList() {
        final List<String> list = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            list.add("stellarity:end_city/decorations/3x/" + i);
        }
        return List.copyOf(list);
    }

    private final org.bukkit.plugin.Plugin plugin;
    private final FeatureFlags featureFlags;
    private final dev.cosmojar.stellaritypaper.items.CustomBlockService customBlockService;
    private final StructuresCleaner structuresCleaner;
    private final EndCityCrystalService endCityCrystalService;
    private final Random random = new Random();
    private final NamespacedKey transformerKey;

    private final ConcurrentMap<BatchKey, PendingBatch> pendingByChunk = new ConcurrentHashMap<>();

    public EndStructuresService(
            final org.bukkit.plugin.Plugin plugin,
            final FeatureFlags featureFlags,
            final dev.cosmojar.stellaritypaper.items.CustomBlockService customBlockService,
            final StructuresCleaner structuresCleaner,
            final EndCityCrystalService endCityCrystalService
    ) {
        this.plugin = plugin;
        this.featureFlags = featureFlags;
        this.customBlockService = customBlockService;
        this.structuresCleaner = structuresCleaner;
        this.endCityCrystalService = endCityCrystalService;
        this.transformerKey = new NamespacedKey(plugin, "stage2_marker_transformer");
    }

    public void enable() {
        if (!featureFlags.isStellarityEnderStructuresEnabled()) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        DebugLog.log(plugin, "[DEBUG] EndStructuresService успешно запущен.");
    }

    public void disable() {
        HandlerList.unregisterAll(this);
        pendingByChunk.clear();
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onAsyncStructureGenerate(final AsyncStructureGenerateEvent event) {
        if (!featureFlags.isStellarityEnderStructuresEnabled()) {
            return;
        }
        if (event.getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }
        final org.bukkit.NamespacedKey structKey = org.bukkit.Registry.STRUCTURE.getKey(event.getStructure());
        if (structKey == null || !structKey.toString().equals("minecraft:end_city")) {
            return;
        }

        DebugLog.log(plugin, "[DEBUG] Установка EntityTransformer и BlockTransformer для генерации End City в чанке " + event.getChunkX() + ", " + event.getChunkZ());

        final BatchKey batchKey = new BatchKey(event.getWorld().getUID(), event.getChunkX(), event.getChunkZ());
        
        event.setEntityTransformer(transformerKey, (region, x, y, z, entity, allowed) -> {
            final TriggerRecord trigger = extractTrigger(entity, region.getWorld().getUID(), x, y, z, batchKey);
            if (trigger == null) {
                return allowed;
            }

            DebugLog.log(plugin, "[DEBUG] Перехвачен маркер во время генерации структуры в чанке: " + trigger.fn() + " на " + x + ", " + y + ", " + z);
            enqueueTrigger(batchKey, trigger);
            return false;
        });

        // Перехват командных блоков и наблюдателей из датапака на лету
        event.setBlockTransformer(transformerKey, (region, x, y, z, state, transformationState) -> {
            if (state instanceof org.bukkit.block.CommandBlock commandBlock) {
                final String command = commandBlock.getCommand();
                if (command != null && !command.isBlank()) {
                    String dir = "north";
                    final org.bukkit.block.data.BlockData data = state.getBlockData();
                    if (data instanceof org.bukkit.block.data.Directional directional) {
                        dir = directional.getFacing().name().toLowerCase(Locale.ROOT);
                    }

                    final String normalizedFn = normalizeCommand(command);
                    final String normalizedFace = normalizeFace(dir);

                    final TriggerRecord trigger = new TriggerRecord(
                            event.getWorld().getUID(),
                            x,
                            y,
                            z,
                            normalizedFn,
                            normalizedFace,
                            batchKey
                    );

                    DebugLog.log(plugin, "[DEBUG] Перехвачен командный блок во время генерации структуры: " + normalizedFn + " на " + x + ", " + y + ", " + z);
                    enqueueTrigger(batchKey, trigger);

                    state.setType(Material.AIR);
                }
            } else if (state.getType() == Material.OBSERVER) {

                state.setType(Material.AIR);
            }
            return state;
        });
    }

    private void enqueueTrigger(final BatchKey key, final TriggerRecord trigger) {
        final PendingBatch batch = pendingByChunk.computeIfAbsent(key, ignored -> new PendingBatch());
        synchronized (batch.triggers) {
            batch.triggers.add(trigger);
        }

        if (batch.scheduled.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTask(plugin, () -> processPendingBatch(key));
        }
    }

    private void processPendingBatch(final BatchKey key) {
        final PendingBatch batch = pendingByChunk.remove(key);
        if (batch == null) {
            return;
        }

        final World world = Bukkit.getWorld(key.worldId());
        if (world == null) {
            return;
        }

        final List<TriggerRecord> firstWave;
        synchronized (batch.triggers) {
            firstWave = new ArrayList<>(batch.triggers);
        }
        if (firstWave.isEmpty()) {
            return;
        }

        final Queue<TriggerRecord> queue = new ArrayDeque<>(firstWave);
        int processed = 0;
        final int hardLimit = 4096;

        while (!queue.isEmpty()) {
            final TriggerRecord trigger = queue.poll();
            if (trigger == null) {
                continue;
            }
            processed++;
            if (processed > hardLimit) {
                DebugLog.log(plugin, "[DEBUG] Превышен лимит триггеров структуры в одном чанке!");
                break;
            }

            final List<TriggerRecord> nested = executeTrigger(world, trigger);
            if (nested != null && !nested.isEmpty()) {
                queue.addAll(nested);
            }
        }
    }

    private TriggerRecord extractTrigger(final Entity entity, final UUID worldId, final int x, final int y, final int z, final BatchKey batchKey) {
        if (entity == null) {
            return null;
        }
        DebugLog.log(plugin, "[DEBUG] extractTrigger: entity=" + entity + ", type=" + entity.getType() + ", tags=" + entity.getScoreboardTags() + " at " + x + ", " + y + ", " + z);
        if (entity.getType() != org.bukkit.entity.EntityType.MARKER) {
            return null;
        }
        final Set<String> tags = entity.getScoreboardTags();
        if (!tags.contains("ke_trigger")) {
            return null;
        }

        String fn = null;
        String face = "north";
        for (final String tag : tags) {
            if (tag == null) {
                continue;
            }
            if (tag.startsWith("ke_fn:")) {
                fn = tag.substring("ke_fn:".length());
            } else if (tag.startsWith("ke_face:")) {
                face = tag.substring("ke_face:".length());
            }
        }

        if (fn == null || fn.isBlank()) {
            return null;
        }

        final String normalizedFn = fn.trim().toLowerCase(Locale.ROOT);
        final String normalizedFace = normalizeFace(face);
        return new TriggerRecord(worldId, x, y, z, normalizedFn, normalizedFace, batchKey);
    }

    private String normalizeCommand(final String command) {
        if (command == null) {
            return "";
        }
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.startsWith("function ")) {
            normalized = normalized.substring("function ".length()).trim();
        }
        if (normalized.startsWith("stellarity:post_gen/")) {
            normalized = "stellarity:structure/" + normalized.substring("stellarity:post_gen/".length());
        }
        final int spaceIdx = normalized.indexOf(' ');
        if (spaceIdx > 0) {
            normalized = normalized.substring(0, spaceIdx);
        }
        return normalized;
    }

    private List<TriggerRecord> executeTrigger(final World world, final TriggerRecord trigger) {
        final Block at = world.getBlockAt(trigger.x(), trigger.y(), trigger.z());

        switch (trigger.fn()) {
            case FN_VAULT -> {
                clearAbove(at);
                placeVault(at, vaultFacing(trigger.face()), false, "VAULT");
                return List.of();
            }
            case FN_VAULT_ELYTRA -> {
                clearAbove(at);
                placeVault(at, vaultFacing(trigger.face()), true, "VAULT_ELYTRA");
                if (endCityCrystalService != null) {
                    final String sKey = endCityCrystalService.getStructureKey(at.getLocation());
                    if (sKey != null) {
                        endCityCrystalService.registerCityHasShip(world, sKey);
                    }
                }
                return List.of();
            }
            case FN_VAULT_OMINOUS -> {
                clearAbove(at);
                placeVault(at, vaultFacing(trigger.face()), true, "VAULT_OMINOUS");
                return List.of();
            }
            case FN_SPAWNER -> {
                clearAbove(at);
                placeTrialSpawner(at, false, "city");
                return List.of();
            }
            case FN_SPAWNER_ELYTRA -> {
                clearAbove(at);
                placeTrialSpawner(at, false, "ship");
                if (endCityCrystalService != null) {
                    final String sKey = endCityCrystalService.getStructureKey(at.getLocation());
                    if (sKey != null) {
                        endCityCrystalService.registerCityHasShip(world, sKey);
                    }
                }
                return List.of();
            }
            case FN_SHIP_MIDDLE -> {
                if (endCityCrystalService != null) {
                    final String sKey = endCityCrystalService.getStructureKey(at.getLocation());
                    if (sKey != null) {
                        endCityCrystalService.registerCityHasShip(world, sKey);
                    }
                }
                return placeRandomTemplate(world, trigger, TPL_SHIP_MIDDLE, "ship/middle");
            }
            case FN_SHIP_ELYTRA -> {
                if (endCityCrystalService != null) {
                    final String sKey = endCityCrystalService.getStructureKey(at.getLocation());
                    if (sKey != null) {
                        endCityCrystalService.registerCityHasShip(world, sKey);
                    }
                }
                return placeRandomTemplate(world, trigger, TPL_SHIP_ELYTRA, "ship/elytra");
            }
            case FN_BASE_ROOM_GENERATE -> {
                clearAbove(at);
                return placeRandomTemplate(world, trigger, TPL_BASE_ROOM, "base_room/generate");
            }
            case FN_BASE_ROOF_ROOM -> {
                return placeRandomTemplate(world, trigger, TPL_BASE_ROOF_ROOM, "base_roof/room");
            }
            case FN_TOP_TOWER_FLOOR_1 -> {
                return placeRandomTemplate(world, trigger, TPL_TOP_TOWER_FLOOR_1, "top_tower/floor_1");
            }
            case FN_TOP_TOWER_FLOOR_2 -> {
                return placeRandomTemplate(world, trigger, TPL_TOP_TOWER_FLOOR_2, "top_tower/floor_2");
            }
            case FN_DECOR_1X -> {
                placeDecor1x(at);
                return List.of();
            }
            case FN_DECOR_2X -> {
                return placeRandomTemplate(world, trigger, TPL_DECOR_2X, "decorations/2x");
            }
            case FN_DECOR_3X -> {
                return placeRandomTemplate(world, trigger, TPL_DECOR_3X, "decorations/3x");
            }
            case FN_BREWING_STAND -> {
                placeBrewingStand(at);
                return List.of();
            }
            case FN_BASE_ROOF_CHAINS -> {
                return placeChains(world, trigger, "stellarity:end_city/base_roof/chains");
            }
            case FN_TOP_TOWER_CHAINS -> {
                return placeChains(world, trigger, "stellarity:end_city/top_tower/chains");
            }
            case FN_SPAWN_CRYSTAL -> {
                spawnCrystal(at);
                return List.of();
            }
            case FN_SPAWN_CRYSTAL_SMALL_TOWER -> {
                spawnCrystalSmallTower(at);
                return List.of();
            }
            case FN_CLEAR_ENTRANCE -> {
                clearEntrance(at, trigger.face());
                return List.of();
            }
            case "stellarity:structure/end_city/spawn_shulker" -> {
                at.setType(Material.AIR, false);
                clearAbove(at);
                return List.of();
            }
            default -> {
                return List.of();
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private List<TriggerRecord> placeRandomTemplate(final World world,
                                                    final TriggerRecord source,
                                                    final List<String> candidates,
                                                    final String label) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        final String picked = candidates.get(random.nextInt(candidates.size()));
        final NamespacedKey key = NamespacedKey.fromString(picked);
        if (key == null) {
            return List.of();
        }

        final Structure structure = loadStructure(key);
        if (structure == null) {
            DebugLog.log(plugin, "[DEBUG] Не удалось загрузить структуру: " + picked);
            return List.of();
        }

        final Location at = new Location(world, source.x(), source.y(), source.z());
        final StructureRotation rotation = structureRotation(source.face());

        final List<TriggerRecord> nested = new ArrayList<>();
        final EntityTransformer markerTransformer = (region, x, y, z, entity, allowed) -> {
            final TriggerRecord trigger = extractTrigger(entity, region.getWorld().getUID(), x, y, z, source.batchKey());
            if (trigger == null) {
                return allowed;
            }
            nested.add(trigger);
            return false;
        };

        final BlockTransformer structureBlockTransformer = (region, x, y, z, state, transformationState) -> {
            if (state instanceof org.bukkit.block.CommandBlock commandBlock) {
                final String command = commandBlock.getCommand();
                if (command != null && !command.isBlank()) {
                    String dir = "north";
                    final org.bukkit.block.data.BlockData data = state.getBlockData();
                    if (data instanceof org.bukkit.block.data.Directional directional) {
                        dir = directional.getFacing().name().toLowerCase(Locale.ROOT);
                    }

                    final String normalizedFn = normalizeCommand(command);
                    final String normalizedFace = normalizeFace(dir);

                    final TriggerRecord trigger = new TriggerRecord(
                            world.getUID(),
                            x,
                            y,
                            z,
                            normalizedFn,
                            normalizedFace,
                            source.batchKey()
                    );

                    DebugLog.log(plugin, "[DEBUG] Перехвачен командный блок в подструктуре: " + normalizedFn + " на " + x + ", " + y + ", " + z);
                    enqueueTrigger(source.batchKey(), trigger);

                    state.setType(Material.AIR);
                }
            } else if (state.getType() == Material.OBSERVER) {
                state.setType(Material.AIR);
            }
            return state;
        };

        structure.place(
                at,
                true,
                rotation,
                Mirror.NONE,
                0,
                1.0f,
                random,
                List.of(structureBlockTransformer),
                List.of(markerTransformer)
        );

        DebugLog.log(plugin, "[DEBUG] Успешно сгенерирована подструктура " + label + " (шаблон: " + picked + ") на " + at.getBlockX() + ", " + at.getBlockY() + ", " + at.getBlockZ());
        return nested;
    }

    private Structure loadStructure(final NamespacedKey key) {
        final StructureManager manager = Bukkit.getStructureManager();
        Structure structure = manager.getStructure(key);
        if (structure != null) {
            return structure;
        }

        structure = manager.loadStructure(key, true);
        if (structure != null) {
            return structure;
        }

        return manager.loadStructure(key);
    }

    private void placeVault(final Block at, final org.bukkit.block.BlockFace facing, final boolean ominous, final String vaultType) {
        if (at == null) {
            return;
        }

        at.setType(Material.VAULT, false);

        final BlockData blockData = at.getBlockData();
        if (blockData instanceof org.bukkit.block.data.type.Vault vaultData) {
            if (facing != null) {
                vaultData.setFacing(facing);
            }
            vaultData.setOminous(ominous);
            at.setBlockData(vaultData, false);
        } else if (blockData instanceof Directional directional && facing != null) {
            directional.setFacing(facing);
            at.setBlockData(directional, false);
        }

        if (at.getState() instanceof org.bukkit.block.TileState tileState) {
            tileState.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "vault_type"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    vaultType
            );
            if ("VAULT_ELYTRA".equalsIgnoreCase(vaultType) && endCityCrystalService != null) {
                final String structureKey = endCityCrystalService.getStructureKey(at.getLocation());
                if (structureKey != null && !structureKey.isBlank()) {
                    tileState.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(plugin, "structure_key"),
                            org.bukkit.persistence.PersistentDataType.STRING,
                            structureKey
                    );
                }
            }
            tileState.update(true, false);
        }

        DebugLog.log(plugin, "[DEBUG] Размещен блок Vault (" + vaultType + ", ominous=" + ominous + ") на " + at.getX() + ", " + at.getY() + ", " + at.getZ());
    }

    private void placeTrialSpawner(final Block at, final boolean ominous, final String spawnerType) {
        if (at == null) {
            return;
        }

        at.setType(Material.TRIAL_SPAWNER, false);

        final BlockData blockData = at.getBlockData();
        if (blockData instanceof org.bukkit.block.data.type.TrialSpawner spawnerData) {
            spawnerData.setOminous(ominous);
            at.setBlockData(spawnerData, false);
        }

        if (at.getState() instanceof org.bukkit.block.TileState tileState) {
            tileState.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "spawner_type"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    spawnerType
            );
            tileState.update(true, false);
        }

        DebugLog.log(plugin, "[DEBUG] Размещен блок Trial Spawner (" + spawnerType + ") на " + at.getX() + ", " + at.getY() + ", " + at.getZ());
    }

    private static void clearAbove(final Block block) {
        final Block above = block.getRelative(0, 1, 0);
        if (!above.getType().isAir()) {
            above.setType(Material.AIR, false);
        }
    }

    private static String normalizeFace(final String face) {
        if (face == null || face.isBlank()) {
            return "north";
        }
        return face.trim().toLowerCase(Locale.ROOT);
    }

    private static org.bukkit.block.BlockFace blockFace(final String face) {
        return switch (normalizeFace(face)) {
            case "south" -> org.bukkit.block.BlockFace.SOUTH;
            case "east" -> org.bukkit.block.BlockFace.EAST;
            case "west" -> org.bukkit.block.BlockFace.WEST;
            default -> org.bukkit.block.BlockFace.NORTH;
        };
    }

    private static org.bukkit.block.BlockFace vaultFacing(final String face) {
        return blockFace(face).getOppositeFace();
    }

    private static StructureRotation structureRotation(final String face) {
        return switch (normalizeFace(face)) {
            case "east" -> StructureRotation.CLOCKWISE_90;
            case "south" -> StructureRotation.CLOCKWISE_180;
            case "west" -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> StructureRotation.NONE;
        };
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityAddToWorld(final EntityAddToWorldEvent event) {
        if (!featureFlags.isStellarityEnderStructuresEnabled()) {
            return;
        }
        final Entity entity = event.getEntity();
        if (entity.getType() != org.bukkit.entity.EntityType.MARKER) {
            return;
        }

        final Set<String> tagsBefore = entity.getScoreboardTags();
        DebugLog.log(plugin, "[DEBUG] onEntityAddToWorld: маркер на " + entity.getLocation().getBlockX() + ", " + entity.getLocation().getBlockY() + ", " + entity.getLocation().getBlockZ() + " с тегами сразу: " + tagsBefore);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!entity.isValid()) {
                return;
            }
            final Set<String> tags = entity.getScoreboardTags();
            DebugLog.log(plugin, "[DEBUG] onEntityAddToWorld: маркер на " + entity.getLocation().getBlockX() + ", " + entity.getLocation().getBlockY() + ", " + entity.getLocation().getBlockZ() + " с тегами: " + tags);

            if (tags.contains("stellarity.altar_of_the_light") || tags.contains("stellarity.altar_of_light")) {
                entity.remove();
                final Location loc = entity.getLocation().getBlock().getLocation().subtract(0, 1, 0);
                DebugLog.log(plugin, "[DEBUG] Перехвачен маркер Алтаря Света (смещён на -1 Y) на " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

                final Location center = loc.clone().add(0.5D, 0.5D, 0.5D);
                if (loc.getWorld() != null) {
                    for (final Entity nearby : loc.getWorld().getNearbyEntities(center, 1.5D, 1.5D, 1.5D)) {
                        if (nearby instanceof org.bukkit.entity.Display 
                                || nearby instanceof org.bukkit.entity.ArmorStand 
                                || nearby instanceof org.bukkit.entity.Marker) {
                            if (!nearby.equals(entity)) {
                                nearby.remove();
                            }
                        }
                    }
                }

                if (customBlockService != null) {
                    customBlockService.placeBlock(loc, "altar_of_the_sacred", org.bukkit.block.BlockFace.SELF);
                }
                if (structuresCleaner != null) {
                    structuresCleaner.cleanChapelOfLight(loc);
                }
                return;
            }

            if (!tags.contains("ke_trigger")) {
                return;
            }

            entity.remove();

            String fn = null;
            String face = "north";
            for (final String tag : tags) {
                if (tag == null) continue;
                if (tag.startsWith("ke_fn:")) {
                    fn = tag.substring("ke_fn:".length());
                } else if (tag.startsWith("ke_face:")) {
                    face = tag.substring("ke_face:".length());
                }
            }

            if (fn == null || fn.isBlank()) {
                return;
            }

            final String normalizedFn = fn.trim().toLowerCase(Locale.ROOT);
            final String normalizedFace = normalizeFace(face);

            final Location loc = entity.getLocation();
            final World world = loc.getWorld();
            if (world == null) {
                return;
            }

            DebugLog.log(plugin, "[DEBUG] Перехвачен маркер при добавлении в мир: " + normalizedFn + " (направление: " + normalizedFace + ") на " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

            final TriggerRecord trigger = new TriggerRecord(
                    world.getUID(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ(),
                    normalizedFn,
                    normalizedFace,
                    new BatchKey(world.getUID(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4)
            );

            final Queue<TriggerRecord> queue = new ArrayDeque<>();
            queue.add(trigger);
            int processed = 0;
            final int hardLimit = 4096;
            while (!queue.isEmpty()) {
                final TriggerRecord tr = queue.poll();
                if (tr == null) {
                    continue;
                }
                processed++;
                if (processed > hardLimit) {
                    break;
                }
                final List<TriggerRecord> nested = executeTrigger(world, tr);
                if (nested != null && !nested.isEmpty()) {
                    queue.addAll(nested);
                }
            }
        });
    }

    private void placeDecor1x(final Block block) {
        final World w = block.getWorld();
        block.getRelative(0, 1, 0).setType(Material.AIR, false);

        final int roll = random.nextInt(60) + 1;
        if (roll <= 17) {
            block.setType(Material.DECORATED_POT, false);
            final BlockData blockData = block.getBlockData();
            if (blockData instanceof Directional directional) {
                final org.bukkit.block.BlockFace face = switch (roll % 4) {
                    case 0 -> org.bukkit.block.BlockFace.EAST;
                    case 1 -> org.bukkit.block.BlockFace.WEST;
                    case 2 -> org.bukkit.block.BlockFace.NORTH;
                    default -> org.bukkit.block.BlockFace.SOUTH;
                };
                directional.setFacing(face);
                block.setBlockData(directional, false);
            }
            DebugLog.log(plugin, "[DEBUG] Размещен горшок на " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        } else if (roll <= 27) {
            block.setType(Material.BARREL, false);
            final BlockData blockData = block.getBlockData();
            if (blockData instanceof Directional directional) {
                directional.setFacing(org.bukkit.block.BlockFace.UP);
                block.setBlockData(directional, false);
            }
            DebugLog.log(plugin, "[DEBUG] Размещена бочка на " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        } else if (roll <= 33) {
            final Material anvilMat = switch (roll) {
                case 28, 29 -> Material.ANVIL;
                case 30, 31 -> Material.CHIPPED_ANVIL;
                default -> Material.DAMAGED_ANVIL;
            };
            block.setType(anvilMat, false);
            final BlockData blockData = block.getBlockData();
            if (blockData instanceof Directional directional) {
                directional.setFacing(roll % 2 == 0 ? org.bukkit.block.BlockFace.EAST : org.bukkit.block.BlockFace.NORTH);
                block.setBlockData(directional, false);
            }
            DebugLog.log(plugin, "[DEBUG] Размещена наковальня " + anvilMat + " на " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        } else if (roll == 34) {
            block.setType(Material.CAULDRON, false);
            DebugLog.log(plugin, "[DEBUG] Размещен котел на " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        } else if (roll <= 36) {
            block.setType(Material.WATER_CAULDRON, false);
            final BlockData blockData = block.getBlockData();
            if (blockData instanceof org.bukkit.block.data.Levelled levelled) {
                levelled.setLevel(roll == 35 ? 1 : 2);
                block.setBlockData(levelled, false);
            }
            DebugLog.log(plugin, "[DEBUG] Размещен котел с водой на " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        } else if (roll == 37) {
            block.setType(Material.LODESTONE, false);
            DebugLog.log(plugin, "[DEBUG] Размещен магнетит на " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        } else {
            block.setType(Material.AIR, false);
        }
    }

    private void placeBrewingStand(final Block block) {
        block.setType(Material.AIR, false);
        final Block standBlock = block.getRelative(0, -1, 0);
        standBlock.setType(Material.BREWING_STAND, false);

        final org.bukkit.block.BlockState state = standBlock.getState();
        if (state instanceof org.bukkit.block.BrewingStand stand) {
            stand.setFuelLevel(random.nextInt(16) + 5);

            final Material[] potTypes = {
                    Material.POTION,
                    Material.SPLASH_POTION,
                    Material.LINGERING_POTION
            };
            if (random.nextBoolean()) {
                stand.getInventory().setItem(0, new ItemStack(potTypes[random.nextInt(3)]));
            }
            if (random.nextBoolean()) {
                stand.getInventory().setItem(1, new ItemStack(potTypes[random.nextInt(3)]));
            }
            if (random.nextBoolean()) {
                stand.getInventory().setItem(2, new ItemStack(potTypes[random.nextInt(3)]));
            }
            stand.update(true, false);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final Block b = block.getRelative(dx, dy, dz);
                    if (b.getType() == Material.OBSERVER) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
        DebugLog.log(plugin, "[DEBUG] Размещена варочная стойка на " + standBlock.getX() + ", " + standBlock.getY() + ", " + standBlock.getZ());
    }

    private void clearObserversAround(final Block block) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                final Block b = block.getRelative(dx, 0, dz);
                if (b.getType() == Material.OBSERVER) {
                    b.setType(Material.AIR, false);
                }
            }
        }
    }

    private List<TriggerRecord> placeChains(final World world, final TriggerRecord source, final String templateName) {
        final Block at = world.getBlockAt(source.x(), source.y(), source.z());
        clearObserversAround(at);

        final NamespacedKey key = NamespacedKey.fromString(templateName);
        if (key != null) {
            final Structure structure = loadStructure(key);
            if (structure != null) {
                final Location loc = new Location(world, source.x(), source.y() - 2, source.z());
                structure.place(
                        loc,
                        true,
                        StructureRotation.NONE,
                        Mirror.NONE,
                        0,
                        1.0f,
                        random,
                        List.of(),
                        List.of()
                );
            }
        }

        final Material chainMat = resolveChainMaterial();
        if (chainMat != Material.AIR) {
            at.setType(chainMat, false);
        }
        DebugLog.log(plugin, "[DEBUG] Размещена цепь на " + at.getX() + ", " + at.getY() + ", " + at.getZ());
        return List.of();
    }

    private static Material resolveChainMaterial() {
        final Material ironChain = Material.matchMaterial("iron_chain");
        if (ironChain != null) {
            return ironChain;
        }
        final Material chain = Material.matchMaterial("chain");
        if (chain != null) {
            return chain;
        }
        return Material.AIR;
    }

    private void spawnCrystal(final Block block) {
        final World w = block.getWorld();
        final String structureKey = endCityCrystalService != null ? endCityCrystalService.getStructureKey(block.getLocation()) : null;
        if (structureKey != null && endCityCrystalService.isCityConquered(w, structureKey)) {
            DebugLog.log(plugin, "[DEBUG] Пропуск размещения Кристалла Края (Город " + structureKey + " уже освобождён)");
            return;
        }

        final Location loc = block.getLocation().add(0.5, -0.5, 0.5);
        if (!w.getNearbyEntitiesByType(org.bukkit.entity.EnderCrystal.class, loc, 1.0).isEmpty()) {
            DebugLog.log(plugin, "[DEBUG] Пропуск размещения Кристалла Края: кристалл уже существует на "
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
            return;
        }

        block.getRelative(0, 1, 0).setType(Material.AIR, false);
        block.setType(Material.AIR, false);
        block.getRelative(0, -1, 0).setType(Material.BEDROCK, false);
        final Entity entity = w.spawnEntity(loc, org.bukkit.entity.EntityType.END_CRYSTAL);
        if (entity instanceof org.bukkit.entity.EnderCrystal crystal && endCityCrystalService != null) {
            endCityCrystalService.prepareAndRegisterCrystal(crystal, "MAIN", block.getLocation(), structureKey);
        }
        DebugLog.log(plugin, "[DEBUG] Размещен Кристалл Края на " + block.getX() + ", " + block.getY() + ", " + block.getZ()
                + " (структура: " + structureKey + ")");
    }

    private void spawnCrystalSmallTower(final Block block) {
        final World w = block.getWorld();
        final String structureKey = endCityCrystalService != null ? endCityCrystalService.getStructureKey(block.getLocation()) : null;
        if (structureKey != null && endCityCrystalService.isCityConquered(w, structureKey)) {
            DebugLog.log(plugin, "[DEBUG] Пропуск размещения Кристалла Края малой башни (Город " + structureKey + " уже освобождён)");
            return;
        }

        final Location loc = block.getLocation().add(0.5, -0.5, 0.5);
        if (!w.getNearbyEntitiesByType(org.bukkit.entity.EnderCrystal.class, loc, 1.0).isEmpty()) {
            DebugLog.log(plugin, "[DEBUG] Пропуск размещения Кристалла Края малой башни: кристалл уже существует на "
                    + block.getX() + ", " + block.getY() + ", " + block.getZ());
            return;
        }

        block.getRelative(0, 1, 0).setType(Material.AIR, false);
        block.setType(Material.AIR, false);

        final int roll = random.nextInt(20) + 1;
        final Material mat;
        if (roll <= 4) {
            mat = Material.IRON_BLOCK;
        } else if (roll <= 7) {
            mat = Material.GOLD_BLOCK;
        } else if (roll == 8) {
            mat = Material.DIAMOND_BLOCK;
        } else if (roll <= 11) {
            mat = Material.EMERALD_BLOCK;
        } else {
            mat = Material.PURPUR_BLOCK;
        }
        block.getRelative(0, -1, 0).setType(mat, false);

        final Entity entity = w.spawnEntity(loc, org.bukkit.entity.EntityType.END_CRYSTAL);
        if (entity instanceof org.bukkit.entity.EnderCrystal crystal && endCityCrystalService != null) {
            endCityCrystalService.prepareAndRegisterCrystal(crystal, "SMALL_TOWER", block.getLocation(), structureKey);
        }
        DebugLog.log(plugin, "[DEBUG] Размещен Кристалл Края (малая башня, блок-поставка: " + mat + ") на "
                + block.getX() + ", " + block.getY() + ", " + block.getZ() + " (структура: " + structureKey + ")");
    }

    public void replaceCrystalWithVault(final Location loc, final String crystalType) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }

        Block targetBlock = loc.getBlock();
        if (targetBlock.getType().isAir() || !targetBlock.getType().isSolid()) {
            final Block below = targetBlock.getRelative(0, -1, 0);
            if (below.getType().isSolid()) {
                targetBlock = below;
            }
        }

        final String vaultType = "VAULT";
        final boolean ominous = false;

        final org.bukkit.block.BlockFace facing = determineVaultFacing(targetBlock);
        placeVault(targetBlock, facing, ominous, vaultType);
        clearAbove(targetBlock);

        // Превращение обсидиана на вершине башни в маяк
        activateTowerBeacon(targetBlock, crystalType);

        DebugLog.log(plugin, "[DEBUG] Защитный кристалл заменен на Vault (" + vaultType + ", ominous=" + ominous + ") на "
                + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ());
    }

    private static org.bukkit.block.BlockFace determineVaultFacing(final Block block) {
        if (block == null) {
            return org.bukkit.block.BlockFace.NORTH;
        }
        if (block.getRelative(0, 0, -1).getType().isAir() && block.getRelative(0, 0, 1).getType().isSolid()) {
            return org.bukkit.block.BlockFace.NORTH;
        }
        if (block.getRelative(0, 0, 1).getType().isAir() && block.getRelative(0, 0, -1).getType().isSolid()) {
            return org.bukkit.block.BlockFace.SOUTH;
        }
        if (block.getRelative(-1, 0, 0).getType().isAir() && block.getRelative(1, 0, 0).getType().isSolid()) {
            return org.bukkit.block.BlockFace.WEST;
        }
        if (block.getRelative(1, 0, 0).getType().isAir() && block.getRelative(-1, 0, 0).getType().isSolid()) {
            return org.bukkit.block.BlockFace.EAST;
        }
        return org.bukkit.block.BlockFace.NORTH;
    }

    private void activateTowerBeacon(final Block targetBlock, final String crystalType) {
        if ("SMALL_TOWER".equalsIgnoreCase(crystalType)) {
            return;
        }
        final Block beaconBlock = targetBlock.getRelative(0, 7, 0);
        final Block baseBlock = targetBlock.getRelative(0, 6, 0);
        if (baseBlock.getType() == Material.OBSIDIAN || beaconBlock.getType() == Material.GLASS
                || beaconBlock.getType() == Material.MAGENTA_STAINED_GLASS || beaconBlock.getType() == Material.TINTED_GLASS) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final Block b = targetBlock.getRelative(dx, 6, dz);
                    if (b.getType() == Material.OBSIDIAN) {
                        b.setType(Material.IRON_BLOCK, false);
                    }
                }
            }
            if (beaconBlock.getType() == Material.GLASS || beaconBlock.getType() == Material.AIR
                    || beaconBlock.getType() == Material.MAGENTA_STAINED_GLASS) {
                beaconBlock.setType(Material.BEACON, false);
                beaconBlock.getRelative(0, 1, 0).setType(Material.MAGENTA_STAINED_GLASS_PANE, false);
            }
        }
    }

    private boolean isEndShipLocation(final Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        final Block center = loc.getBlock();
        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    final Material type = center.getRelative(dx, dy, dz).getType();
                    if (type == Material.DRAGON_HEAD || type == Material.DRAGON_WALL_HEAD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void clearEntrance(final Block block, final String face) {
        final World w = block.getWorld();
        final int bx = block.getX();
        final int by = block.getY();
        final int bz = block.getZ();

        switch (face) {
            case "south" -> {
                fillAir(w, bx, by, bz - 2, bx, by + 1, bz);
                if (w.getBlockAt(bx, by + 2, bz - 2).getType() != Material.PURPUR_SLAB) {
                    fillAir(w, bx, by, bz - 2, bx, by + 2, bz - 3);
                }
            }
            case "north" -> {
                fillAir(w, bx, by, bz, bx, by + 1, bz + 2);
                if (w.getBlockAt(bx, by + 2, bz + 2).getType() != Material.PURPUR_SLAB) {
                    fillAir(w, bx, by, bz + 2, bx, by + 2, bz + 3);
                }
            }
            case "east" -> {
                fillAir(w, bx, by, bz, bx + 2, by + 1, bz);
                if (w.getBlockAt(bx + 2, by + 2, bz).getType() != Material.PURPUR_SLAB) {
                    fillAir(w, bx + 2, by, bz, bx + 3, by + 2, bz);
                }
            }
            case "west" -> {
                fillAir(w, bx - 2, by, bz, bx, by + 1, bz);
                if (w.getBlockAt(bx - 2, by + 2, bz).getType() != Material.PURPUR_SLAB) {
                    fillAir(w, bx - 2, by, bz, bx - 3, by + 2, bz);
                }
            }
        }
    }

    private void fillAir(final World w, final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
        final int minX = Math.min(x1, x2);
        final int maxX = Math.max(x1, x2);
        final int minY = Math.min(y1, y2);
        final int maxY = Math.max(y1, y2);
        final int minZ = Math.min(z1, z2);
        final int maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    w.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private record BatchKey(UUID worldId, int chunkX, int chunkZ) {
    }

    private static final class PendingBatch {
        private final List<TriggerRecord> triggers = new ArrayList<>();
        private final AtomicBoolean scheduled = new AtomicBoolean(false);
    }

    private record TriggerRecord(UUID worldId,
                                 int x,
                                 int y,
                                 int z,
                                 String fn,
                                 String face,
                                 BatchKey batchKey) {
    }
}
