package dev.cosmojar.stellaritypaper.mobs.boss.dragon;

import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager;
import dev.cosmojar.stellaritypaper.text.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class DragonRespawnManager {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final Location[] pedestals;
    private boolean ritualRunning = false;

    public DragonRespawnManager(JavaPlugin plugin, MessageService messageService) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.pedestals = new Location[4];
    }

    public boolean isRitualRunning() {
        return ritualRunning;
    }

    public static void spawnParticleSafe(World world, Particle particle, Location loc, int count, double ox, double oy, double oz, double speed) {
        Class<?> dataType = particle.getDataType();
        if (dataType == Float.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, 1.0f);
        } else if (dataType == Color.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, Color.fromRGB(180, 50, 220));
        } else if (dataType == Particle.DustOptions.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, new Particle.DustOptions(Color.fromRGB(180, 50, 220), 1.0f));
        } else if (dataType == Particle.DustTransition.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, new Particle.DustTransition(Color.fromRGB(180, 50, 220), Color.WHITE, 1.0f));
        } else {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed);
        }
    }

    public void tickPedestals(World world) {
        if (ritualRunning) return;

        if (!DragonEncounter.getEncounters().isEmpty() || !world.getEntitiesByClass(EnderDragon.class).isEmpty()) {
            return;
        }

        pedestals[0] = new Location(world, 4.5, 62.0, 0.5);
        pedestals[1] = new Location(world, -3.5, 62.0, 0.5);
        pedestals[2] = new Location(world, 0.5, 62.0, -3.5);
        pedestals[3] = new Location(world, 0.5, 62.0, 4.5);

        int crystalCount = 0;
        List<EnderCrystal> foundCrystals = new ArrayList<>();

        for (Location pedLoc : pedestals) {
            boolean hasCrystal = false;
            for (org.bukkit.entity.Entity entity : world.getNearbyEntities(pedLoc, 0.6, 1.0, 0.6)) {
                if (entity instanceof EnderCrystal crystal) {
                    hasCrystal = true;
                    crystalCount++;
                    foundCrystals.add(crystal);
                    break;
                }
            }

            if (!hasCrystal) {
                spawnParticleSafe(world, Particle.WAX_OFF, pedLoc, 1, 0.35, 0.15, 0.35, 0.15);
            }
        }

        if (crystalCount == 4) {
            startSummoningRitual(world, foundCrystals);
        }
    }

    public void startSummoningRitual(World world, List<EnderCrystal> portalCrystals) {
        this.ritualRunning = true;
        Location centerPortal = new Location(world, 0.5, 63.0, 0.5);

        world.playSound(centerPortal, Sound.ENTITY_ENDER_DRAGON_AMBIENT, SoundCategory.HOSTILE, 4.0f, 0.75f);
        world.playSound(centerPortal, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 4.0f, 0.75f);
        world.playSound(centerPortal, Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 4.0f, 0.5f);

        spawnParticleSafe(world, Particle.DRAGON_BREATH, centerPortal, 200, 0.9, 0.9, 0.9, 0.01);
        spawnParticleSafe(world, Particle.DRAGON_BREATH, centerPortal, 200, 0.6, 0.6, 0.6, 0.01);
        spawnParticleSafe(world, Particle.DRAGON_BREATH, centerPortal, 200, 0.3, 0.3, 0.3, 0.01);

        spawnParticleSafe(world, Particle.END_ROD, centerPortal, 60, 0.9, 0.9, 0.9, 0.01);
        spawnParticleSafe(world, Particle.END_ROD, centerPortal, 60, 0.6, 0.6, 0.6, 0.01);
        spawnParticleSafe(world, Particle.END_ROD, centerPortal, 60, 0.3, 0.3, 0.3, 0.01);

        for (org.bukkit.entity.Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(centerPortal) <= 10000) {
                final org.bukkit.advancement.Advancement adv = org.bukkit.Bukkit.getAdvancement(org.bukkit.NamespacedKey.fromString("stellarity:dragons_den/sacrificial_ritual"));
                if (adv != null) {
                    org.bukkit.advancement.AdvancementProgress prog = player.getAdvancementProgress(adv);
                    for (String crit : prog.getRemainingCriteria()) {
                        prog.awardCriteria(crit);
                    }
                }
            }
        }

        placeDeactivatedPortal(world);

        new BukkitRunnable() {
            private int tick = 0;
            private int currentTowerIndex = 0;

            @Override
            public void run() {
                tick++;

                if (tick >= 60 && tick <= 580 && tick % 45 == 0) {
                    if (currentTowerIndex < DragonEncounter.TOWER_COORDS.length) {
                        double[] tower = DragonEncounter.TOWER_COORDS[currentTowerIndex];
                        Location towerLoc = new Location(world, tower[0], tower[1], tower[2]);
                        for (EnderCrystal crystal : portalCrystals) {
                            if (crystal.isValid()) {
                                crystal.setBeamTarget(towerLoc);
                            }
                        }
                        world.playSound(towerLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 3.0f, 1.2f);
                        spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, towerLoc, 1, 0, 0, 0, 0);
                        currentTowerIndex++;
                    }
                }

                if (tick >= 120 && tick <= 560 && tick % 20 == 0) {
                    spawnPulseWave(world, centerPortal);
                }

                if (tick == 590) {
                    for (EnderCrystal crystal : portalCrystals) {
                        if (crystal.isValid()) crystal.setBeamTarget(new Location(world, 0.5, 100.0, 0.5));
                    }
                    world.getBlockAt(0, 63, 0).setType(Material.END_GATEWAY);
                    world.playSound(centerPortal, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.BLOCKS, 2.0f, 1.1f);
                    world.playSound(centerPortal, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.0f, 0.78f);
                    spawnParticleSafe(world, Particle.END_ROD, centerPortal, 35, 0.0, 0.15, 0.0, 0.1);

                    spawnSkySpiral(world, new Location(world, 0.5, 100.0, 0.5));
                }

                if (tick >= 590 && tick < 670) {
                    double angle = tick * 0.15;
                    spawnParticleSafe(world, Particle.DRAGON_BREATH, centerPortal.clone().add(0, 2.0, 0), 3, 0.15, 0.15, 0.15, 0.004);
                    spawnParticleSafe(world, Particle.END_ROD, centerPortal.clone().add(Math.cos(angle) * 0.66, 2.0, Math.sin(angle) * 0.66), 1, 0, 0, 0, 0);
                    spawnParticleSafe(world, Particle.END_ROD, centerPortal.clone().add(-Math.cos(angle) * 0.66, 2.0, -Math.sin(angle) * 0.66), 1, 0, 0, 0, 0);
                }

                if (tick >= 670) {
                    finishSummoningRitual(world, portalCrystals);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void spawnPulseWave(World world, Location center) {
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double x = Math.cos(angle) * 12.0;
            double z = Math.sin(angle) * 12.0;
            spawnParticleSafe(world, Particle.DRAGON_BREATH, center.clone().add(x, 0, z), 1, 0, 0, 0, 0.01);
        }
    }

    private void placeDeactivatedPortal(World world) {
        Structure portalStruct = EndIslandManager.loadExitPortalStructure(false);
        if (portalStruct != null) {
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    for (int y = 59; y <= 70; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == Material.BEDROCK || block.getType() == Material.END_PORTAL) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }

            portalStruct.place(
                new Location(world, -6, 59, -6),
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
    }

    private void finishSummoningRitual(World world, List<EnderCrystal> portalCrystals) {
        for (EnderCrystal crystal : portalCrystals) {
            if (crystal.isValid()) {
                crystal.setBeamTarget(null);
                spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, crystal.getLocation(), 1, 0, 0, 0, 0);
                crystal.remove();
            }
        }

        world.getBlockAt(0, 63, 0).setType(Material.BEDROCK);

        Location dragonSpawnLoc = new Location(world, 0.5, 100.0, 0.5);
        EnderDragon dragon = (EnderDragon) world.spawnEntity(dragonSpawnLoc, EntityType.ENDER_DRAGON);
        dragon.setPhase(EnderDragon.Phase.CIRCLING);

        dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().bindDragonToFight(world, dragon);

        double health = plugin.getConfig().getDouble("boss.ender_dragon.max-health", 300.0);
        DragonEncounter encounter = DragonEncounter.getEncounter(dragon.getUniqueId());
        if (encounter == null) {
            encounter = new DragonEncounter(plugin, dragon, health, messageService);
        }

        NamespacedKey crystalKey = new NamespacedKey(plugin, "dragon_crystal");
        NamespacedKey encKey = new NamespacedKey(plugin, "encounter_id");
        for (double[] coord : DragonEncounter.TOWER_COORDS) {
            Location loc = new Location(world, coord[0], coord[1], coord[2]);
            EnderCrystal towerCrystal = (EnderCrystal) world.spawnEntity(loc, EntityType.END_CRYSTAL);
            towerCrystal.setShowingBottom(true);
            towerCrystal.getPersistentDataContainer().set(crystalKey, PersistentDataType.BYTE, (byte) 1);
            towerCrystal.getPersistentDataContainer().set(encKey, PersistentDataType.STRING, encounter.getEncounterId().toString());
        }

        encounter.scanTowerCrystals();

        world.playSound(dragonSpawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 10.0f, 0.7f);
        world.playSound(dragonSpawnLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 10.0f, 0.0f);
        world.playSound(dragonSpawnLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 10.0f, 0.0f);

        Component spawnMsg = messageService.message("dragon.spawn");
        for (Player p : world.getPlayers()) {
            p.sendMessage(spawnMsg);
        }

        this.ritualRunning = false;
        dev.cosmojar.stellaritypaper.util.DebugLog.log(plugin, "[DEBUG] Extended dragon summoning ritual completed successfully.");
    }

    private void spawnSkySpiral(World world, Location center) {
        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick > 80) {
                    this.cancel();
                    return;
                }

                double baseAngle = tick * 0.2;
                for (double y = 60.0; y <= 130.0; y += 3.0) {
                    double radius = 3.0 + ((y - 60.0) / 70.0) * 25.0;
                    double x1 = Math.cos(baseAngle + (y * 0.08)) * radius;
                    double z1 = Math.sin(baseAngle + (y * 0.08)) * radius;

                    double x2 = Math.cos(baseAngle + Math.PI + (y * 0.08)) * radius;
                    double z2 = Math.sin(baseAngle + Math.PI + (y * 0.08)) * radius;

                    Location loc1 = new Location(world, 0.5 + x1, y, 0.5 + z1);
                    Location loc2 = new Location(world, 0.5 + x2, y, 0.5 + z2);

                    spawnParticleSafe(world, Particle.END_ROD, loc1, 2, 0.05, 0.05, 0.05, 0.01);
                    spawnParticleSafe(world, Particle.END_ROD, loc2, 2, 0.05, 0.05, 0.05, 0.01);
                    spawnParticleSafe(world, Particle.DRAGON_BREATH, loc1, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
