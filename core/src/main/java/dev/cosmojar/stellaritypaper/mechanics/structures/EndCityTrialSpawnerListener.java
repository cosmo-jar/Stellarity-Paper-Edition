package dev.cosmojar.stellaritypaper.mechanics.structures;

import dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.TrialSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class EndCityTrialSpawnerListener implements Listener {

    private static final double TRIGGER_RADIUS = 5.0;

    private final Plugin plugin;
    private final CustomLootService customLootService;

    private final NamespacedKey activeSpawnerKey;
    private final NamespacedKey spawnerTypeKey;
    private final NamespacedKey mobSpawnerTagKey;
    private final NamespacedKey spawnerOminousKey;

    private final Set<Location> activeSpawnerLocations = new HashSet<>();
    private final Map<String, Set<UUID>> activeWaves = new HashMap<>();

    public EndCityTrialSpawnerListener(final Plugin plugin, final CustomLootService customLootService) {
        this.plugin = plugin;
        this.customLootService = customLootService;

        this.activeSpawnerKey = new NamespacedKey(plugin, "active_spawner");
        this.spawnerTypeKey = new NamespacedKey(plugin, "spawner_type");
        this.mobSpawnerTagKey = new NamespacedKey(plugin, "mob_spawner_tag");
        this.spawnerOminousKey = new NamespacedKey(plugin, "spawner_ominous");

        startProximityCheckTask();
        startActiveVisualTask();
    }

    private void startProximityCheckTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (final World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() != World.Environment.THE_END) {
                    continue;
                }
                for (final Player player : world.getPlayers()) {
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }
                    final Location pLoc = player.getLocation();
                    final int startX = (pLoc.getBlockX() - 10) >> 4;
                    final int endX = (pLoc.getBlockX() + 10) >> 4;
                    final int startZ = (pLoc.getBlockZ() - 10) >> 4;
                    final int endZ = (pLoc.getBlockZ() + 10) >> 4;

                    for (int cx = startX; cx <= endX; cx++) {
                        for (int cz = startZ; cz <= endZ; cz++) {
                            if (world.isChunkLoaded(cx, cz)) {
                                scanChunkForTrialSpawners(world, cx, cz, player);
                            }
                        }
                    }
                }
            }

            cleanOrResetUnloadedWaves();
        }, 20L, 20L);
    }

    private void scanChunkForTrialSpawners(final World world, final int cx, final int cz, final Player player) {
        final org.bukkit.Chunk chunk = world.getChunkAt(cx, cz);
        for (final org.bukkit.block.BlockState state : chunk.getTileEntities(false)) {
            if (state instanceof TileState tileState && state.getType() == Material.TRIAL_SPAWNER) {
                final Location loc = tileState.getLocation();
                if (loc.distanceSquared(player.getLocation()) <= TRIGGER_RADIUS * TRIGGER_RADIUS) {
                    if (world.hasStructureAt(loc, Structure.END_CITY)) {
                        checkAndTriggerSpawner(tileState.getBlock(), player);
                    }
                }
            }
        }
    }

    private void cleanOrResetUnloadedWaves() {
        final Iterator<Map.Entry<String, Set<UUID>>> iterator = activeWaves.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, Set<UUID>> entry = iterator.next();
            final String locKey = entry.getKey();
            final Set<UUID> mobUuids = entry.getValue();

            final String[] parts = locKey.split(":");
            if (parts.length < 2) continue;
            final World world = Bukkit.getWorld(parts[0]);
            if (world == null) continue;

            final String[] coords = parts[1].split(",");
            if (coords.length < 3) continue;

            final int x = Integer.parseInt(coords[0]);
            final int y = Integer.parseInt(coords[1]);
            final int z = Integer.parseInt(coords[2]);

            mobUuids.removeIf(uuid -> {
                final Entity entity = Bukkit.getEntity(uuid);
                return entity == null || !entity.isValid() || entity.isDead();
            });

            if (mobUuids.isEmpty()) {
                final Block block = world.getBlockAt(x, y, z);
                if (block.getState() instanceof TileState tileState) {
                    final String state = tileState.getPersistentDataContainer().getOrDefault(activeSpawnerKey, PersistentDataType.STRING, "IDLE");
                    if (!"COMPLETED".equalsIgnoreCase(state) && !"EJECTING".equalsIgnoreCase(state)) {
                        tileState.getPersistentDataContainer().set(activeSpawnerKey, PersistentDataType.STRING, "IDLE");
                        tileState.getPersistentDataContainer().remove(spawnerOminousKey);
                        tileState.update(true);
                        if (block.getState() instanceof org.bukkit.block.TrialSpawner trialSpawner) {
                            trialSpawner.setOminous(false);
                            trialSpawner.update(true);
                        }
                        if (block.getBlockData() instanceof org.bukkit.block.data.type.TrialSpawner trialSpawnerData) {
                            trialSpawnerData.setOminous(false);
                            block.setBlockData(trialSpawnerData, false);
                        }
                        activeSpawnerLocations.remove(block.getLocation());
                        iterator.remove();
                    }
                }
            }
        }
    }

    private void startActiveVisualTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (final Location loc : activeSpawnerLocations) {
                if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    final Location center = loc.clone().add(0.5, 0.5, 0.5);
                    final Block b = loc.getBlock();
                    boolean isOminous = false;
                    if (b.getState() instanceof TileState ts && ts.getPersistentDataContainer().has(spawnerOminousKey, PersistentDataType.BYTE)) {
                        isOminous = ts.getPersistentDataContainer().getOrDefault(spawnerOminousKey, PersistentDataType.BYTE, (byte) 0) == 1;
                    }
                    if (isOminous) {
                        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 4, 0.25, 0.25, 0.25, 0.02);
                        loc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, center, 6, 0.3, 0.3, 0.3, 0.02);
                    } else {
                        loc.getWorld().spawnParticle(Particle.FLAME, center, 4, 0.25, 0.25, 0.25, 0.02);
                        loc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, center, 6, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }
        }, 10L, 10L);
    }

    private void checkAndTriggerSpawner(final Block block, final Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (!(block.getState() instanceof TileState tileState)) {
            return;
        }

        final PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        final String state = pdc.getOrDefault(activeSpawnerKey, PersistentDataType.STRING, "IDLE");

        if (!"IDLE".equalsIgnoreCase(state)) {
            return;
        }

        final Location loc = block.getLocation();
        final String locKey = loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

        final String taggedType = pdc.get(spawnerTypeKey, PersistentDataType.STRING);
        final boolean isShip = "ship".equalsIgnoreCase(taggedType) || isEndShipLocation(loc);

        boolean playerOmen = false;
        try {
            if (player.hasPotionEffect(PotionEffectType.TRIAL_OMEN) || player.hasPotionEffect(PotionEffectType.BAD_OMEN)) {
                playerOmen = true;
            }
        } catch (Throwable ignored) {}

        boolean isOminous = isShip || playerOmen;
        if (block.getState() instanceof org.bukkit.block.TrialSpawner trialSpawner) {
            isOminous = isOminous || trialSpawner.isOminous();
            if (isOminous) {
                trialSpawner.setOminous(true);
                trialSpawner.update(true);
            }
        }
        if (block.getBlockData() instanceof org.bukkit.block.data.type.TrialSpawner tsd) {
            isOminous = isOminous || tsd.isOminous();
            if (isOminous) {
                tsd.setOminous(true);
                block.setBlockData(tsd, false);
            }
        }

        pdc.set(activeSpawnerKey, PersistentDataType.STRING, "ACTIVE");
        pdc.set(spawnerOminousKey, PersistentDataType.BYTE, (byte) (isOminous ? 1 : 0));
        tileState.update(true);
        activeSpawnerLocations.add(loc);

        if (isOminous) {
            loc.getWorld().playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.0f, 1.0f);
            loc.getWorld().spawnParticle(Particle.TRIAL_OMEN, loc.clone().add(0.5, 1.0, 0.5), 30, 0.4, 0.4, 0.4, 0.05);
            loc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, loc.clone().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.05);
        } else {
            loc.getWorld().playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.0f, 1.0f);
            loc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, loc.clone().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.05);
        }

        final Set<UUID> mobUuids = new HashSet<>();
        final EntityType[] mobTypes;

        if (isShip) {
            mobTypes = new EntityType[]{
                    EntityType.SKELETON, EntityType.ZOMBIE,
                    EntityType.SKELETON, EntityType.ZOMBIE,
                    EntityType.SKELETON, EntityType.ZOMBIE
            };
        } else if (isOminous) {
            mobTypes = new EntityType[]{
                    EntityType.SKELETON, EntityType.ZOMBIE,
                    EntityType.SKELETON, EntityType.ZOMBIE,
                    EntityType.SKELETON, EntityType.ZOMBIE,
                    EntityType.SKELETON, EntityType.ZOMBIE
            };
        } else {
            final Random rand = new Random();
            final int roll = rand.nextInt(10);
            if (roll < 2) {
                mobTypes = new EntityType[]{
                        EntityType.SILVERFISH, EntityType.SILVERFISH, EntityType.SILVERFISH,
                        EntityType.SILVERFISH, EntityType.SILVERFISH, EntityType.SILVERFISH,
                        EntityType.SILVERFISH, EntityType.SILVERFISH, EntityType.SILVERFISH
                };
            } else if (roll < 4) {
                mobTypes = new EntityType[]{
                        EntityType.SLIME, EntityType.SLIME, EntityType.SLIME, EntityType.SLIME, EntityType.SLIME
                };
            } else if (roll < 7) {
                mobTypes = new EntityType[]{
                        EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE,
                        EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE
                };
            } else {
                mobTypes = new EntityType[]{
                        EntityType.SKELETON, EntityType.SKELETON, EntityType.SKELETON,
                        EntityType.SKELETON, EntityType.SKELETON, EntityType.SKELETON
                };
            }
        }

        for (final EntityType type : mobTypes) {
            final Location spawnLoc = loc.clone().add((Math.random() - 0.5) * 3, 1, (Math.random() - 0.5) * 3);
            final Entity mob = loc.getWorld().spawnEntity(spawnLoc, type);
            if (mob instanceof Mob m) {
                m.getPersistentDataContainer().set(mobSpawnerTagKey, PersistentDataType.STRING, locKey);
                if (m instanceof Slime slime) {
                    slime.setSize(1);
                }
                equipEndMobArmor(m, isOminous);
                mobUuids.add(m.getUniqueId());
            }
        }

        activeWaves.put(locKey, mobUuids);
    }

    private void equipEndMobArmor(final Mob mob, final boolean isOminous) {
        if (!(mob instanceof Zombie) && !(mob instanceof Skeleton)) {
            return;
        }

        final EntityEquipment eq = mob.getEquipment();
        if (eq == null) return;

        final float armorDropChance = (float) plugin.getConfig().getDouble(
                isOminous ? "trial_spawner.end_city.ominous-armor-drop-chance"
                          : "trial_spawner.end_city.armor-drop-chance",
                isOminous ? 0.10D : 0.085D);

        final float weaponDropChance = (float) plugin.getConfig().getDouble(
                isOminous ? "trial_spawner.end_city.ominous-weapon-drop-chance"
                          : "trial_spawner.end_city.weapon-drop-chance",
                isOminous ? 0.10D : 0.085D);

        eq.setHelmetDropChance(armorDropChance);
        eq.setChestplateDropChance(armorDropChance);
        eq.setLeggingsDropChance(armorDropChance);
        eq.setBootsDropChance(armorDropChance);
        eq.setItemInMainHandDropChance(0.0f);
        eq.setItemInOffHandDropChance(0.0f);

        final Random rand = new Random();

        final TrimMaterial trimMat = isOminous
                ? (rand.nextBoolean() ? TrimMaterial.GOLD : TrimMaterial.AMETHYST)
                : getRandomTrimMaterial(rand);

        final Material helmetMat = isOminous
                ? (rand.nextBoolean() ? Material.NETHERITE_HELMET : Material.DIAMOND_HELMET)
                : (rand.nextBoolean() ? Material.DIAMOND_HELMET : Material.IRON_HELMET);

        final Material chestMat = isOminous
                ? (rand.nextBoolean() ? Material.NETHERITE_CHESTPLATE : Material.DIAMOND_CHESTPLATE)
                : (rand.nextBoolean() ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE);

        final Material legMat = isOminous
                ? (rand.nextBoolean() ? Material.NETHERITE_LEGGINGS : Material.DIAMOND_LEGGINGS)
                : (rand.nextBoolean() ? Material.DIAMOND_LEGGINGS : Material.IRON_LEGGINGS);

        final Material bootMat = isOminous
                ? (rand.nextBoolean() ? Material.NETHERITE_BOOTS : Material.DIAMOND_BOOTS)
                : (rand.nextBoolean() ? Material.DIAMOND_BOOTS : Material.IRON_BOOTS);

        final int protLevel = isOminous ? 4 : 2;

        eq.setHelmet(createTrimmedArmor(helmetMat, trimMat, TrimPattern.SPIRE, protLevel));
        eq.setChestplate(createTrimmedArmor(chestMat, trimMat, TrimPattern.SPIRE, protLevel));
        eq.setLeggings(createTrimmedArmor(legMat, trimMat, TrimPattern.SPIRE, protLevel));
        eq.setBoots(createTrimmedArmor(bootMat, trimMat, TrimPattern.SPIRE, protLevel));

        if (mob instanceof Zombie zombie) {
            final Material weaponMat;
            if (isOminous) {
                weaponMat = switch (rand.nextInt(4)) {
                    case 0 -> Material.NETHERITE_SWORD;
                    case 1 -> Material.NETHERITE_AXE;
                    case 2 -> Material.DIAMOND_AXE;
                    default -> Material.DIAMOND_SWORD;
                };
            } else {
                weaponMat = rand.nextBoolean() ? Material.DIAMOND_SWORD : Material.IRON_SWORD;
            }
            final ItemStack weapon = new ItemStack(weaponMat);
            weapon.addUnsafeEnchantment(Enchantment.SHARPNESS, isOminous ? 4 : 2);
            weapon.addUnsafeEnchantment(Enchantment.KNOCKBACK, isOminous ? 2 : 1);
            if (isOminous) {
                weapon.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            }
            eq.setItemInMainHand(weapon);
            eq.setItemInMainHandDropChance(weaponDropChance);

            final double health = isOminous ? 50.0 : 35.0;
            if (zombie.getAttribute(Attribute.MAX_HEALTH) != null) {
                zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
                zombie.setHealth(health);
            }
            if (zombie.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(isOminous ? 8.0 : 5.0);
            }
        } else if (mob instanceof Skeleton skeleton) {
            final ItemStack bow = new ItemStack(Material.BOW);
            bow.addUnsafeEnchantment(Enchantment.POWER, isOminous ? 4 : 2);
            bow.addUnsafeEnchantment(Enchantment.PUNCH, isOminous ? 2 : 1);
            bow.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
            if (isOminous) {
                bow.addUnsafeEnchantment(Enchantment.FLAME, 1);
            }
            eq.setItemInMainHand(bow);
            eq.setItemInMainHandDropChance(weaponDropChance);

            final double health = isOminous ? 45.0 : 30.0;
            if (skeleton.getAttribute(Attribute.MAX_HEALTH) != null) {
                skeleton.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
                skeleton.setHealth(health);
            }
        }
    }

    private TrimMaterial getRandomTrimMaterial(final Random rand) {
        return switch (rand.nextInt(6)) {
            case 0 -> TrimMaterial.GOLD;
            case 1 -> TrimMaterial.AMETHYST;
            case 2 -> TrimMaterial.COPPER;
            case 3 -> TrimMaterial.EMERALD;
            case 4 -> TrimMaterial.DIAMOND;
            default -> TrimMaterial.IRON;
        };
    }

    private ItemStack createTrimmedArmor(final Material material, final TrimMaterial trimMaterial, final TrimPattern pattern, final int protLevel) {
        final ItemStack stack = new ItemStack(material);
        if (stack.getItemMeta() instanceof ArmorMeta meta) {
            meta.setTrim(new ArmorTrim(trimMaterial, pattern));
            stack.setItemMeta(meta);
        }
        if (protLevel > 0) {
            stack.addUnsafeEnchantment(Enchantment.PROTECTION, protLevel);
        }
        return stack;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(mobSpawnerTagKey, PersistentDataType.STRING)) {
            return;
        }

        final String locKey = pdc.get(mobSpawnerTagKey, PersistentDataType.STRING);
        if (locKey == null) {
            return;
        }

        final Set<UUID> mobSet = activeWaves.get(locKey);
        if (mobSet != null) {
            mobSet.remove(entity.getUniqueId());
            if (mobSet.isEmpty()) {
                activeWaves.remove(locKey);
                completeSpawnerWave(locKey);
            }
        }
    }

    private void completeSpawnerWave(final String locKey) {
        final String[] parts = locKey.split(":");
        if (parts.length < 2) return;
        final World world = Bukkit.getWorld(parts[0]);
        if (world == null) return;

        final String[] coords = parts[1].split(",");
        if (coords.length < 3) return;

        final int x = Integer.parseInt(coords[0]);
        final int y = Integer.parseInt(coords[1]);
        final int z = Integer.parseInt(coords[2]);

        final Block block = world.getBlockAt(x, y, z);
        boolean isOminous = false;

        if (block.getState() instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(activeSpawnerKey, PersistentDataType.STRING, "COMPLETED");
            final Byte ominousByte = tileState.getPersistentDataContainer().get(spawnerOminousKey, PersistentDataType.BYTE);
            if (ominousByte != null && ominousByte == (byte) 1) {
                isOminous = true;
            }
            tileState.getPersistentDataContainer().remove(spawnerOminousKey);
            tileState.update(true);
        }

        final Location loc = block.getLocation();
        activeSpawnerLocations.remove(loc);

        if (block.getState() instanceof org.bukkit.block.TrialSpawner trialSpawner) {
            isOminous = isOminous || trialSpawner.isOminous();
            trialSpawner.setOminous(false);
            trialSpawner.update(true);
        }
        if (block.getBlockData() instanceof org.bukkit.block.data.type.TrialSpawner trialSpawnerData) {
            isOminous = isOminous || trialSpawnerData.isOminous();
            trialSpawnerData.setOminous(false);
            block.setBlockData(trialSpawnerData, false);
        }

        if (isOminous) {
            world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.0f, 0.8f);
            world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, loc.clone().add(0.5, 0.5, 0.5), 35, 0.4, 0.4, 0.4, 0.05);
        } else {
            world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.0f, 1.0f);
            world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, loc.clone().add(0.5, 0.5, 0.5), 35, 0.4, 0.4, 0.4, 0.05);
        }

        boolean isShip = false;
        if (block.getState() instanceof TileState tileState) {
            final String taggedType = tileState.getPersistentDataContainer().get(spawnerTypeKey, PersistentDataType.STRING);
            isShip = "ship".equalsIgnoreCase(taggedType) || isEndShipLocation(loc);
        } else {
            isShip = isEndShipLocation(loc);
        }

        final String spawnerPath;
        if (isShip) {
            spawnerPath = "ship";
        } else {
            spawnerPath = isOminous ? "city.ominous" : "city.normal";
        }

        final List<ItemStack> rewards = customLootService.getTrialSpawnerLoot("EndCity", spawnerPath);
        if (rewards.isEmpty() && isOminous) {
            customLootService.createItemStack("gilded_purpur_key").ifPresent(rewards::add);
        }
        if (!rewards.isEmpty()) {
            ejectTrialLootSequentially(loc, rewards);
        }
    }

    private void ejectTrialLootSequentially(final Location blockLoc, final List<ItemStack> rewards) {
        final World world = blockLoc.getWorld();
        if (world == null) return;

        final Location ejectLoc = blockLoc.clone().add(0.5, 1.2, 0.5);
        final int taskPeriodTicks = 6;

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (rewards.isEmpty()) {
                task.cancel();
                return;
            }

            final ItemStack reward = rewards.remove(0);
            final Item itemEntity = world.dropItem(ejectLoc, reward.clone());
            itemEntity.setVelocity(new Vector((Math.random() - 0.5) * 0.12, 0.28, (Math.random() - 0.5) * 0.12));

            world.playSound(ejectLoc, Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, 1.0f, 1.0f);
            world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, ejectLoc, 15, 0.2, 0.2, 0.2, 0.05);
        }, 6L, taskPeriodTicks);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTrialSpawnerLootGenerate(final LootGenerateEvent event) {
        final Location loc = event.getLootContext() != null ? event.getLootContext().getLocation() : null;
        if (loc == null) {
            return;
        }

        final World world = loc.getWorld();
        if (world == null || !world.hasStructureAt(loc, Structure.END_CITY)) {
            return;
        }

        final Block block = loc.getBlock();
        if (block.getType() != Material.TRIAL_SPAWNER && block.getType() != Material.SPAWNER) {
            final String tableKey = event.getLootTable() != null ? event.getLootTable().getKey().toString().toLowerCase() : "";
            if (!tableKey.contains("spawner")) {
                return;
            }
        }

        boolean isShip = false;
        if (block.getState() instanceof TileState tileState) {
            final String taggedType = tileState.getPersistentDataContainer().get(spawnerTypeKey, PersistentDataType.STRING);
            isShip = "ship".equalsIgnoreCase(taggedType) || isEndShipLocation(loc);
        } else {
            isShip = isEndShipLocation(loc);
        }

        final String spawnerPath;
        if (isShip) {
            spawnerPath = "ship";
        } else {
            boolean isOminous = false;
            if (block.getState() instanceof TileState tileState) {
                final Byte ominousByte = tileState.getPersistentDataContainer().get(spawnerOminousKey, PersistentDataType.BYTE);
                if (ominousByte != null && ominousByte == (byte) 1) {
                    isOminous = true;
                }
            }
            if (block.getState() instanceof org.bukkit.block.TrialSpawner trialSpawner) {
                isOminous = isOminous || trialSpawner.isOminous();
            }
            if (block.getBlockData() instanceof org.bukkit.block.data.type.TrialSpawner trialSpawnerData) {
                isOminous = isOminous || trialSpawnerData.isOminous();
            }
            spawnerPath = isOminous ? "city.ominous" : "city.normal";
        }

        final List<ItemStack> rewards = customLootService.getTrialSpawnerLoot("EndCity", spawnerPath);
        if (rewards.isEmpty() && !isShip && spawnerPath.contains("ominous")) {
            customLootService.createItemStack("gilded_purpur_key").ifPresent(rewards::add);
        }
        if (!rewards.isEmpty()) {
            event.setLoot(rewards);
        }
    }

    private boolean isEndShipLocation(final Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        final Block center = loc.getBlock();
        for (int dx = -20; dx <= 20; dx++) {
            for (int dy = -15; dy <= 15; dy++) {
                for (int dz = -20; dz <= 20; dz++) {
                    if (center.getRelative(dx, dy, dz).getType() == Material.DRAGON_HEAD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
