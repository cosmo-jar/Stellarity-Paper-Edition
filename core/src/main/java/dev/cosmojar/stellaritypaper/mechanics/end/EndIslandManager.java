package dev.cosmojar.stellaritypaper.mechanics.end;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionManager;
import dev.cosmojar.stellaritypaper.util.DebugLog;
import dev.cosmojar.stellaritypaper.items.CustomBlockService;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.text.MessageService;
import net.minecraft.core.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Marker;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.block.structure.Mirror;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class EndIslandManager implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey initializedKey;
    private static final NamespacedKey THE_END = NamespacedKey.minecraft("the_end");
    private boolean isSummoning = false;

    private final CustomBlockService customBlockService;
    private final CustomItemFactory customItemFactory;
    private final ItemDefinitionRegistry itemRegistry;
    private final MessageService messageService;
    private final dev.cosmojar.stellaritypaper.mobs.boss.dragon.DragonRespawnManager dragonRespawnManager;

    public EndIslandManager(
            JavaPlugin plugin,
            CustomBlockService customBlockService,
            CustomItemFactory customItemFactory,
            ItemDefinitionRegistry itemRegistry,
            MessageService messageService
    ) {
        this.plugin = plugin;
        this.customBlockService = customBlockService;
        this.customItemFactory = customItemFactory;
        this.itemRegistry = itemRegistry;
        this.messageService = messageService;
        this.dragonRespawnManager = new dev.cosmojar.stellaritypaper.mobs.boss.dragon.DragonRespawnManager(plugin, messageService);
        this.initializedKey = new NamespacedKey(plugin, "island_initialized");
        startCustomSummonMonitor();
    }

    public boolean isSummoning() {
        return isSummoning;
    }

    public void setSummoning(boolean summoning) {
        this.isSummoning = summoning;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        checkAndInitialize(event.getWorld(), 100L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        checkAndInitialize(event.getPlayer().getWorld(), 20L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkAndInitialize(event.getPlayer().getWorld(), 20L);
    }

    private void checkAndInitialize(World world, long delay) {
        if (!isTheEnd(world)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            forceLoadEndChunksAsync(world, () -> {
                Byte isInitialized = world.getPersistentDataContainer().get(initializedKey, PersistentDataType.BYTE);

                if (isInitialized == null || isInitialized == 0) {
                    restoreCustomIsland(world);
                    world.getPersistentDataContainer().set(initializedKey, PersistentDataType.BYTE, (byte) 1);
                    DebugLog.log(plugin, "[DEBUG] Stellarity End Island initialized for world: " + world.getName());
                }
            });
        }, delay);
    }

    public static boolean isTheEnd(World world) {
        return THE_END.equals(world.getKey());
    }

    /**
     * Asynchronously loads chunks from (-7, -7) to (7, 7) (225 chunks covering the entire central island)
     * off the main thread to avoid stalling server tick thread during heavy worldgen (e.g. Nullscape).
     */
    private void forceLoadEndChunksAsync(World world, Runnable onComplete) {
        final List<java.util.concurrent.CompletableFuture<org.bukkit.Chunk>> futures = new java.util.ArrayList<>();
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (!world.isChunkLoaded(x, z)) {
                    futures.add(world.getChunkAtAsync(x, z, true));
                }
            }
        }

        if (futures.isEmpty()) {
            if (onComplete != null) onComplete.run();
        } else {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (onComplete != null) onComplete.run();
                });
            });
        }
    }

    private void forceLoadEndChunks(World world) {
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (!world.isChunkLoaded(x, z)) {
                    world.getChunkAtAsync(x, z, true);
                }
            }
        }
    }

    /**
     * Periodically checks if the Ender Dragon is dead and players place 4 crystals on the custom pedestals (distance 4).
     * Also checks and corrects the exit portal if vanilla overrides it.
     */
    private void startCustomSummonMonitor() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                if (isTheEnd(world)) {
                    checkAndCorrectPortal(world);
                    checkCustomRespawn(world);
                }
            }
        }, 20L, 20L);
    }

    private boolean isDragonActive(org.bukkit.boss.DragonBattle battle) {
        if (battle == null) return false;
        org.bukkit.entity.EnderDragon dragon = battle.getEnderDragon();
        return dragon != null && !dragon.isDead() && dragon.isValid();
    }

    /**
     * Checks if the vanilla exit portal has generated (by looking for Bedrock at radius 3, e.g. at (3, y, 0) for y in [61, 63, 64])
     * and overrides it with our custom template strictly at Y=59.
     */
    private void checkAndCorrectPortal(World world) {
        if (isSummoning) {
            return;
        }

        boolean playerNearby = false;
        org.bukkit.Location centerLoc = new org.bukkit.Location(world, 0, 60, 0);
        for (org.bukkit.entity.Player p : world.getPlayers()) {
            if (p.getWorld().equals(world) && p.getLocation().distanceSquared(centerLoc) <= 22500) {
                playerNearby = true;
                break;
            }
        }
        if (!playerNearby) return;

        org.bukkit.boss.DragonBattle battle = world.getEnderDragonBattle();
        if (battle != null && battle.getRespawnPhase() != org.bukkit.boss.DragonBattle.RespawnPhase.NONE) {
            return;
        }

        boolean vanillaDetected = false;
        if (world.getBlockAt(3, 61, 0).getType() == org.bukkit.Material.BEDROCK ||
            world.getBlockAt(3, 63, 0).getType() == org.bukkit.Material.BEDROCK ||
            world.getBlockAt(3, 64, 0).getType() == org.bukkit.Material.BEDROCK) {
            vanillaDetected = true;
        }

        if (vanillaDetected) {
            boolean dragonAlive = isDragonActive(battle);
            
            if (dragonAlive) {
                plugin.getLogger().info("Stellarity: Detected vanilla exit portal override. Restoring custom deactivated portal.");
            } else {
                plugin.getLogger().info("Stellarity: Detected vanilla exit portal override. Restoring custom activated portal.");
            }

            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    for (int y = 59; y <= 70; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == org.bukkit.Material.BEDROCK || block.getType() == org.bukkit.Material.END_PORTAL) {
                            block.setType(org.bukkit.Material.AIR, false);
                        }
                    }
                }
            }

            Structure structure = loadExitPortalStructure(!dragonAlive);
            if (structure != null) {
                forceLoadEndChunks(world);
                structure.place(
                    new org.bukkit.Location(world, -6, 59, -6),
                    true,
                    StructureRotation.NONE,
                    Mirror.NONE,
                    0,
                    1.0f,
                    new Random(),
                    List.of(),
                    List.of()
                );
            }

            fillObsidianBorder(world);

            org.bukkit.Location centerCrystalLoc = new org.bukkit.Location(world, 0.5, 60.0, 0.5);
            for (org.bukkit.entity.Entity entity : world.getNearbyEntities(centerCrystalLoc, 2.0, 3.0, 2.0)) {
                if (entity instanceof org.bukkit.entity.EnderCrystal) {
                    entity.remove();
                }
            }

            setExitPortalLocationNMS(world, 10000, 0, 10000);
        }
    }

    private void checkCustomRespawn(World world) {
        if (dragonRespawnManager.isRitualRunning()) {
            return;
        }

        org.bukkit.boss.DragonBattle battle = world.getEnderDragonBattle();
        if (battle != null) {
            if (battle.getEnderDragon() != null || battle.getRespawnPhase() != org.bukkit.boss.DragonBattle.RespawnPhase.NONE) {
                return;
            }
        } else {
            if (!world.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).isEmpty()) {
                return;
            }
        }

        boolean playerNearby = false;
        org.bukkit.Location centerLoc = new org.bukkit.Location(world, 0, 60, 0);
        for (org.bukkit.entity.Player p : world.getPlayers()) {
            if (p.getWorld().equals(world) && p.getLocation().distanceSquared(centerLoc) <= 22500) {
                playerNearby = true;
                break;
            }
        }
        if (!playerNearby) return;

        dragonRespawnManager.tickPedestals(world);
    }

    private void bindDragonToFightNMS(World world, org.bukkit.entity.EnderDragon bukkitDragon) {
        try {
            Object craftWorld = world.getClass().getMethod("getHandle").invoke(world);
            Object dragonFight = craftWorld.getClass().getMethod("getDragonFight").invoke(craftWorld);
            if (dragonFight != null) {
                try {
                    java.lang.reflect.Field field = dragonFight.getClass().getDeclaredField("dragonKilled");
                    field.setAccessible(true);
                    field.set(dragonFight, false);
                } catch (NoSuchFieldException e) {
                    for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                        if (field.getType() == boolean.class) {
                            field.setAccessible(true);
                            field.set(dragonFight, false);
                        }
                    }
                }

                UUID uuid = bukkitDragon.getUniqueId();
                try {
                    java.lang.reflect.Field uuidField = dragonFight.getClass().getDeclaredField("dragonUUID");
                    uuidField.setAccessible(true);
                    uuidField.set(dragonFight, uuid);
                } catch (NoSuchFieldException e) {
                    for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                        if (field.getType() == UUID.class) {
                            field.setAccessible(true);
                            field.set(dragonFight, uuid);
                        }
                    }
                }

                Object nmsDragon = bukkitDragon.getClass().getMethod("getHandle").invoke(bukkitDragon);
                for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                    if (field.getType().isAssignableFrom(nmsDragon.getClass()) || field.getType().getName().endsWith("EnderDragon")) {
                        field.setAccessible(true);
                        field.set(dragonFight, nmsDragon);
                    }
                }

                for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                    if (field.getType().isEnum() && (field.getType().getName().contains("SpawnState") || field.getType().getName().contains("Respawn"))) {
                        field.setAccessible(true);
                        field.set(dragonFight, null);
                    }
                }

                for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                    if (field.getType().getName().contains("Boss") || field.getType().getName().contains("ServerBossEvent")) {
                        field.setAccessible(true);
                        Object bossEvent = field.get(dragonFight);
                        if (bossEvent != null) {
                            bossEvent.getClass().getMethod("setVisible", boolean.class).invoke(bossEvent, true);
                        }
                    }
                }

                try {
                    for (java.lang.reflect.Method m : dragonFight.getClass().getDeclaredMethods()) {
                        if (m.getName().equals("setUnsaved") || m.getName().equals("saveData") || m.getName().equals("setDirty")) {
                            m.setAccessible(true);
                            m.invoke(dragonFight);
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to bind dragon to fight via reflection: " + e.getMessage());
        }
    }

    public static void setExitPortalLocationNMS(World world, int x, int y, int z) {
        try {
            VersionManager.getAdapter().setExitPortalLocation(world, x, y, z);
        } catch (Throwable ignored) {}

        try {
            Object craftWorld = world.getClass().getMethod("getHandle").invoke(world);
            Object dragonFight = craftWorld.getClass().getMethod("getDragonFight").invoke(craftWorld);
            if (dragonFight != null) {
                try {
                    java.lang.reflect.Field f = dragonFight.getClass().getDeclaredField("portalLocation");
                    f.setAccessible(true);
                    f.set(dragonFight, new BlockPos(x, y, z));
                    return;
                } catch (Exception ignored) {}

                try {
                    java.lang.reflect.Field f = dragonFight.getClass().getDeclaredField("exitPortalLocation");
                    f.setAccessible(true);
                    f.set(dragonFight, new BlockPos(x, y, z));
                    return;
                } catch (Exception ignored) {}

                for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                    if (field.getType().equals(BlockPos.class) && !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            field.set(dragonFight, new BlockPos(x, y, z));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void fillObsidianBorder(World world) {
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (x == -7 || x == 7 || z == -7 || z == 7) {
                    world.getBlockAt(x, 58, z).setType(org.bukkit.Material.OBSIDIAN, false);
                    world.getBlockAt(x, 59, z).setType(org.bukkit.Material.OBSIDIAN, false);
                }
            }
        }
    }

    /**
     * Restores the custom towers, native altar template, and native loot chest.
     */
    public void restoreCustomIsland(World world) {
        forceLoadEndChunks(world);
        setExitPortalLocationNMS(world, 10000, 0, 10000);

        Structure portalStruct = loadExitPortalStructure(false);
        if (portalStruct != null) {
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    for (int y = 59; y <= 70; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        block.setType(org.bukkit.Material.AIR, false);
                    }
                }
            }

            portalStruct.place(
                new org.bukkit.Location(world, -6, 59, -6),
                true,
                StructureRotation.NONE,
                Mirror.NONE,
                0,
                1.0f,
                new Random(),
                List.of(),
                List.of()
            );
            DebugLog.log(plugin, "[DEBUG] Placed Deactivated Exit Portal natively on initialization.");
        }

        fillObsidianBorder(world);

        org.bukkit.Location centerCrystalLoc = new org.bukkit.Location(world, 0.5, 60.0, 0.5);
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(centerCrystalLoc, 2.0, 3.0, 2.0)) {
            if (entity instanceof org.bukkit.entity.EnderCrystal) {
                entity.remove();
            }
        }

        NamespacedKey altarKey = NamespacedKey.fromString("stellarity:altar_of_the_accursed");
        Structure altarStruct = Bukkit.getStructureManager().loadStructure(altarKey);
        if (altarStruct != null) {
            altarStruct.place(
                new org.bukkit.Location(world, 42, 67, -48),
                true,
                StructureRotation.NONE,
                Mirror.NONE,
                0,
                1.0f,
                new Random(),
                List.of(),
                List.of()
            );
            DebugLog.log(plugin, "[DEBUG] Placed Altar of the Accursed natively.");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location altarBlockLoc = new Location(world, 50, 76, -39);
                if (altarBlockLoc.getBlock().getType() == org.bukkit.Material.END_PORTAL_FRAME) {
                    for (org.bukkit.entity.Entity entity : world.getNearbyEntities(altarBlockLoc.clone().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)) {
                        if (entity instanceof ItemDisplay || entity instanceof Marker) {
                            entity.remove();
                        }
                    }
                    customBlockService.placeBlock(altarBlockLoc, "altar_of_the_accursed", BlockFace.SELF);
                    syncAltarSwordState(world);
                }
            }, 5L);
        } else {
            plugin.getLogger().warning("Failed to load structure template 'stellarity:altar_of_the_accursed'");
        }

        Block chestBlock = world.getBlockAt(7, 60, 0);
        chestBlock.setType(org.bukkit.Material.CHEST);
        org.bukkit.block.data.type.Chest chestData = (org.bukkit.block.data.type.Chest) chestBlock.getBlockData();
        chestData.setFacing(org.bukkit.block.BlockFace.EAST);
        chestBlock.setBlockData(chestData);

        Chest chestState = (Chest) chestBlock.getState();
        chestState.setLootTable(Bukkit.getLootTable(NamespacedKey.fromString("stellarity:dragons_den")));
        chestState.update(true);

        itemRegistry.findByPdcItemId("misc_empty_enchanted_book").ifPresent(def -> {
            ItemStack emptyBook = customItemFactory.create(def);
            chestState.getInventory().addItem(emptyBook);
        });
        DebugLog.log(plugin, "[DEBUG] Placed Loot Chest natively with Empty Enchanted Book.");

        String dimensionKey = world.getKey().toString();

        Boolean originalFeedback = null;
        org.bukkit.GameRule<Boolean> feedbackRule = null;

        if (ServerVersion.getCurrent() != ServerVersion.V1_21_11 && ServerVersion.getCurrent() != ServerVersion.UNKNOWN) {
            originalFeedback = VersionManager.getAdapter().getSendCommandFeedback(world);
            VersionManager.getAdapter().setSendCommandFeedback(world, false);
        } else {
            feedbackRule = org.bukkit.GameRules.SEND_COMMAND_FEEDBACK;
            originalFeedback = feedbackRule != null ? world.getGameRuleValue(feedbackRule) : null;
            if (feedbackRule != null) {
                world.setGameRule(feedbackRule, false);
            }
        }

        final String spikeRingFeature = (ServerVersion.getCurrent() == ServerVersion.V26_3)
                ? "stellarity:dragons_den/end_spike/ring"
                : "far_end:general/end_spike/ring";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "execute in " + dimensionKey + " positioned 0 60 0 run place feature " + spikeRingFeature);
                
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "execute in " + dimensionKey + " positioned 0 60 0 run place feature stellarity:dragons_den/obsidian_spike_decor");

        if (ServerVersion.getCurrent() != ServerVersion.V1_21_11 && ServerVersion.getCurrent() != ServerVersion.UNKNOWN) {
            VersionManager.getAdapter().setSendCommandFeedback(world, originalFeedback != null ? originalFeedback : true);
        } else {
            if (feedbackRule != null) {
                world.setGameRule(feedbackRule, originalFeedback != null ? originalFeedback : true);
            }
        }

        if (ServerVersion.getCurrent() != ServerVersion.V1_21_11 && ServerVersion.getCurrent() != ServerVersion.UNKNOWN) {
            VersionManager.getAdapter().resetDragonFight(world);
        } else {
            org.bukkit.boss.DragonBattle battle = world.getEnderDragonBattle();
            if (battle != null && battle.getEnderDragon() == null) {
                forceLoadEndChunks(world);
                org.bukkit.entity.EnderDragon existingDragon = world.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).stream()
                        .filter(d -> d.isValid() && !d.isDead())
                        .findFirst()
                        .orElse(null);

                if (existingDragon != null) {
                    bindDragonToFightNMS(world, existingDragon);
                    DebugLog.log(plugin, "[DEBUG] Found existing Ender Dragon on initialization. Bound to fight instead of spawning new entity.");
                } else {
                    org.bukkit.entity.EnderDragon dragon = world.spawn(new org.bukkit.Location(world, 0.5, 100.0, 0.5), org.bukkit.entity.EnderDragon.class);
                    dragon.setPhase(org.bukkit.entity.EnderDragon.Phase.CIRCLING);
                    bindDragonToFightNMS(world, dragon);
                    DebugLog.log(plugin, "[DEBUG] Spawned and bound Ender Dragon on first entry.");
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (isTheEnd(event.getWorld()) && event.getChunk().getX() == 3 && event.getChunk().getZ() == -3) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                syncAltarSwordState(event.getWorld());
            }, 10L);
        }
    }

    public void syncAltarSwordState(World world) {
        if (!isTheEnd(world)) return;
        
        Location altarBlockLoc = new Location(world, 50, 76, -39);
        if (!world.isChunkLoaded(3, -3)) return;

        org.bukkit.persistence.PersistentDataContainer pdc = world.getPersistentDataContainer();
        NamespacedKey dragonbladeTakenKey = new NamespacedKey(plugin, "stellarity_dragonblade_taken");
        
        boolean dragonbladeTaken = pdc.has(dragonbladeTakenKey, PersistentDataType.BYTE) 
                && pdc.get(dragonbladeTakenKey, PersistentDataType.BYTE) == (byte) 1;

        ArmorStand swordHolder = null;
        Interaction swordHitbox = null;
        
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(altarBlockLoc.clone().add(0.5, 0.5, 0.5), 2.0, 3.0, 2.0)) {
            if (entity.getScoreboardTags().contains("stellarity.altar_of_the_accursed.sword_holder")) {
                if (entity instanceof ArmorStand) {
                    swordHolder = (ArmorStand) entity;
                }
            } else if (entity.getScoreboardTags().contains("stellarity.altar_of_the_accursed.sword_hitbox")) {
                if (entity instanceof Interaction) {
                    swordHitbox = (Interaction) entity;
                }
            }
        }

        if (dragonbladeTaken) {
            if (swordHolder != null) {
                swordHolder.remove();
            }
            if (swordHitbox != null) {
                swordHitbox.remove();
            }
        } else {
            if (swordHolder == null || !swordHolder.isValid()) {
                Location standLoc = new Location(world, 51.004507D, 76.225848D, -38.842306D);
                standLoc.setYaw(-90.0F);
                standLoc.setPitch(0.0F);
                
                final dev.cosmojar.stellaritypaper.items.CustomItemDefinition def = itemRegistry.findByPdcItemId("dragonblade").orElse(null);
                final ItemStack swordItem = def != null ? customItemFactory.create(def) : null;
                
                swordHolder = world.spawn(standLoc, ArmorStand.class, stand -> {
                    stand.setInvisible(true);
                    stand.setGravity(false);
                    stand.setBasePlate(false);
                    stand.setArms(true);
                    stand.setMarker(true);
                    stand.setInvulnerable(true);
                    stand.setRightArmPose(new org.bukkit.util.EulerAngle(Math.toRadians(80.0D), 0.0D, 0.0D));
                    
                    stand.addScoreboardTag("stellarity.altar_of_the_accursed.sword_holder");
                    stand.addScoreboardTag("smithed.entity");
                    stand.addScoreboardTag("smithed.strict");
                    
                    if (swordItem != null) {
                        stand.getEquipment().setItemInMainHand(swordItem, true);
                    }
                });
            }

            if (swordHitbox == null || !swordHitbox.isValid()) {
                Location hitboxLoc = new Location(world, 50.5D, 76.7D, -38.5D);
                swordHitbox = world.spawn(hitboxLoc, Interaction.class, interaction -> {
                    interaction.setInteractionWidth(0.4F);
                    interaction.setInteractionHeight(1.0F);
                    interaction.setResponsive(true);
                    
                    interaction.addScoreboardTag("stellarity.altar_of_the_accursed.sword_hitbox");
                    interaction.addScoreboardTag("smithed.entity");
                    interaction.addScoreboardTag("smithed.strict");
                });
            }
        }
    }

    public static NamespacedKey getExitPortalKey(boolean activated) {
        final String name = activated ? "exit_portal/activated" : "exit_portal/deactivated";
        if (ServerVersion.getCurrent() == ServerVersion.V26_3) {
            return NamespacedKey.fromString("stellarity:" + name);
        }
        return NamespacedKey.fromString("far_end:" + name);
    }

    public static Structure loadExitPortalStructure(boolean activated) {
        final NamespacedKey key = getExitPortalKey(activated);
        Structure s = Bukkit.getStructureManager().loadStructure(key);
        if (s == null) {
            final String fallbackNs = "stellarity".equals(key.getNamespace()) ? "far_end" : "stellarity";
            final String name = activated ? "exit_portal/activated" : "exit_portal/deactivated";
            s = Bukkit.getStructureManager().loadStructure(NamespacedKey.fromString(fallbackNs + ":" + name));
        }
        return s;
    }

    public void cleanup() {
    }
}
