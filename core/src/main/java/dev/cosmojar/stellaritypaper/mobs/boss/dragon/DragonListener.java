package dev.cosmojar.stellaritypaper.mobs.boss.dragon;

import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager;
import dev.cosmojar.stellaritypaper.mobs.boss.BossManager;
import dev.cosmojar.stellaritypaper.text.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;

import java.util.List;
import java.util.Random;

public class DragonListener implements Listener {

    private final Plugin plugin;
    private final BossManager bossManager;
    private final MessageService messageService;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;
    private final NamespacedKey encounterKey;
    private final NamespacedKey shulkerKey;
    private final NamespacedKey crystalKey;
    private final NamespacedKey dragonBreathKey;

    public DragonListener(
            Plugin plugin,
            BossManager bossManager,
            MessageService messageService,
            dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.messageService = messageService;
        this.advancementService = advancementService;
        this.encounterKey = new NamespacedKey(plugin, "encounter_id");
        this.shulkerKey = new NamespacedKey(plugin, "dragon_shulker");
        this.crystalKey = new NamespacedKey(plugin, "dragon_crystal");
        this.dragonBreathKey = new NamespacedKey(plugin, "dragon_breath");
    }

    @EventHandler
    public void onDragonSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            EnderDragon newDragon = (EnderDragon) event.getEntity();
            World world = newDragon.getWorld();
            if (EndIslandManager.isTheEnd(world)) {
                boolean duplicateFound = false;
                for (Entity e : world.getEntitiesByClass(EnderDragon.class)) {
                    if (e instanceof EnderDragon existing && !existing.getUniqueId().equals(newDragon.getUniqueId()) && existing.isValid() && !existing.isDead()) {
                        duplicateFound = true;
                        break;
                    }
                }
                if (!duplicateFound && !DragonEncounter.getEncounters().isEmpty()) {
                    for (DragonEncounter enc : DragonEncounter.getEncounters().values()) {
                        EnderDragon encDragon = enc.getDragon();
                        if (encDragon != null && !encDragon.getUniqueId().equals(newDragon.getUniqueId()) && encDragon.isValid() && !encDragon.isDead()) {
                            duplicateFound = true;
                            break;
                        }
                    }
                }

                if (duplicateFound) {
                    event.setCancelled(true);
                    newDragon.remove();
                    plugin.getLogger().warning("[DragonListener] Cancelled and removed duplicate Ender Dragon spawned at " + newDragon.getLocation());
                    return;
                }
                wrapDragon(newDragon);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.getType() == EntityType.ENDER_DRAGON) {
                wrapDragon((EnderDragon) entity);
            }
        }
    }

    private void wrapDragon(EnderDragon dragon) {
        if (EndIslandManager.isTheEnd(dragon.getWorld())) {
            if (bossManager.getBoss(dragon.getUniqueId()) != null) return;
            new StellarityDragon(plugin, dragon, bossManager, messageService);
            dev.cosmojar.stellaritypaper.util.DebugLog.log(plugin, "[DEBUG] Wrapped Ender Dragon into StellarityDragon encounter.");
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onDragonDamageGeneral(org.bukkit.event.entity.EntityDamageEvent event) {
        Entity victim = event.getEntity();
        EnderDragon dragon = null;
        if (victim instanceof EnderDragon d) {
            dragon = d;
        } else if (victim instanceof org.bukkit.entity.ComplexEntityPart part && part.getParent() instanceof EnderDragon d) {
            dragon = d;
        }

        if (dragon != null) {
            DragonEncounter encounter = DragonEncounter.getEncounter(dragon.getUniqueId());
            if (encounter != null && encounter.isInvulnerable()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onDragonDamageThreshold(org.bukkit.event.entity.EntityDamageEvent event) {
        Entity victim = event.getEntity();
        EnderDragon dragon = null;
        if (victim instanceof EnderDragon d) {
            dragon = d;
        } else if (victim instanceof org.bukkit.entity.ComplexEntityPart part && part.getParent() instanceof EnderDragon d) {
            dragon = d;
        }

        if (dragon != null) {
            DragonEncounter encounter = DragonEncounter.getEncounter(dragon.getUniqueId());
            if (encounter != null && !encounter.isInvulnerable() && !encounter.isReviveCrystalsUsed()) {
                org.bukkit.attribute.AttributeInstance hpAttr = dragon.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                double maxHp = hpAttr != null ? hpAttr.getValue() : 300.0;
                double halfHp = maxHp * 0.50;
                double currentHp = dragon.getHealth();

                if (currentHp - event.getFinalDamage() < halfHp) {
                    double allowedDamage = Math.max(0.0, currentHp - halfHp);
                    event.setDamage(allowedDamage);
                    encounter.triggerReviveCrystals();
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof ShulkerBullet bullet) {
            PersistentDataContainer pdc = bullet.getPersistentDataContainer();
            if (pdc.has(encounterKey, PersistentDataType.STRING)) {
                player.removePotionEffect(PotionEffectType.LEVITATION);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, false, true));
            }
        }
    }

    @EventHandler
    public void onCrystalDestroy(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal) {
            PersistentDataContainer pdc = crystal.getPersistentDataContainer();
            if (pdc.has(crystalKey, PersistentDataType.BYTE)) {
                String encIdStr = pdc.get(encounterKey, PersistentDataType.STRING);
                if (encIdStr != null) {
                    for (DragonEncounter encounter : DragonEncounter.getEncounters().values()) {
                        encounter.onCrystalDestroyed(crystal);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON && EndIslandManager.isTheEnd(event.getEntity().getWorld())) {
            EnderDragon dragon = (EnderDragon) event.getEntity();
            Location deathLoc = dragon.getLocation();
            World world = deathLoc.getWorld();
            if (world == null) return;

            DragonEncounter encounter = DragonEncounter.getEncounter(dragon.getUniqueId());
            if (encounter != null) {
                encounter.cleanup();
            }

            for (org.bukkit.entity.AreaEffectCloud cloud : world.getEntitiesByClass(org.bukkit.entity.AreaEffectCloud.class)) {
                if (cloud.getPersistentDataContainer().has(dragonBreathKey, PersistentDataType.BYTE)
                        || cloud.getPersistentDataContainer().has(encounterKey, PersistentDataType.STRING)) {
                    cloud.remove();
                }
            }

            EndIslandManager.setExitPortalLocationNMS(world, 10000, 0, 10000);

            world.playSound(deathLoc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 8.0f, 0.7f);
            world.playSound(deathLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 8.0f, 0.0f);

            DragonRespawnManager.spawnParticleSafe(world, Particle.DRAGON_BREATH, deathLoc, 300, 5, 5, 5, 0.1);
            DragonRespawnManager.spawnParticleSafe(world, Particle.END_ROD, deathLoc, 300, 5, 5, 5, 0.2);
            DragonRespawnManager.spawnParticleSafe(world, Particle.CLOUD, deathLoc, 300, 5, 5, 5, 0.2);

            Component deathMsg = messageService.message("dragon.death");
            for (Player p : world.getPlayers()) {
                p.sendMessage(deathMsg);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                EndIslandManager.setExitPortalLocationNMS(world, 10000, 0, 10000);

                Structure structure = EndIslandManager.loadExitPortalStructure(true);
                if (structure != null) {
                    structure.place(
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

                world.getBlockAt(0, 68, 0).setType(Material.DRAGON_EGG);
                DragonRespawnManager.spawnParticleSafe(world, Particle.POOF, new Location(world, 0.5, 68.5, 0.5), 50, 0.3, 0.3, 0.3, 0.0);

                final Location dropLoc = new Location(world, 0.5, 69.5, 0.5);
                DragonRespawnManager.spawnParticleSafe(world, Particle.DRAGON_BREATH, dropLoc, 100, 0.5, 0.5, 0.5, 0.8);
                DragonRespawnManager.spawnParticleSafe(world, Particle.END_ROD, dropLoc, 100, 0.5, 0.5, 0.5, 0.8);
                DragonRespawnManager.spawnParticleSafe(world, Particle.CLOUD, dropLoc, 100, 0.5, 0.5, 0.5, 0.8);
                world.playSound(dropLoc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 8.0f, 0.7f);
                world.playSound(dropLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 8.0f, 0.0f);

                bossManager.dropDragonLoot(dropLoc);
            }, 202L);

            NamespacedKey firstDragonDefeatedKey = new NamespacedKey(plugin, "stellarity_first_dragon_defeated");
            PersistentDataContainer pdc = world.getPersistentDataContainer();
            boolean alreadyDefeated = pdc.has(firstDragonDefeatedKey, PersistentDataType.BYTE)
                    && pdc.get(firstDragonDefeatedKey, PersistentDataType.BYTE) == (byte) 1;

            if (!alreadyDefeated) {
                pdc.set(firstDragonDefeatedKey, PersistentDataType.BYTE, (byte) 1);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location altarLoc = new Location(world, 50.5D, 76.5D, -38.5D);
                    world.playSound(altarLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 5.0F, 1.0F);
                }, 100L);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location altarLoc = new Location(world, 50.5D, 76.5D, -38.5D);
                    DragonRespawnManager.spawnParticleSafe(world, Particle.EXPLOSION, altarLoc, 1, 0, 0, 0, 1.0);
                    DragonRespawnManager.spawnParticleSafe(world, Particle.PORTAL, altarLoc, 80, 0.5, 0.5, 0.5, 0.1);
                    DragonRespawnManager.spawnParticleSafe(world, Particle.DRAGON_BREATH, altarLoc, 240, 0.5, 0.5, 0.5, 0.1);
                    DragonRespawnManager.spawnParticleSafe(world, Particle.END_ROD, altarLoc, 240, 0.5, 0.5, 0.5, 0.1);

                    world.playSound(altarLoc, Sound.ENTITY_WITHER_DEATH, SoundCategory.BLOCKS, 5.0F, 1.2F);
                    world.playSound(altarLoc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.BLOCKS, 5.0F, 0.0F);

                    Component unlockMsg = messageService.message("altar_of_the_accursed.unlocked");
                    for (Player player : world.getPlayers()) {
                        player.sendActionBar(unlockMsg);
                        if (advancementService != null) {
                            advancementService.grant(player, "stellarity:dragons_den/god_slayer");
                        }
                    }
                }, 160L);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        World world = event.getPlayer().getWorld();
        if (EndIslandManager.isTheEnd(world)) {
            checkAndWrapDragons(world);
        }
    }

    @EventHandler
    public void onPlayerWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (EndIslandManager.isTheEnd(world)) {
            checkAndWrapDragons(world);
        }
        for (DragonEncounter encounter : DragonEncounter.getEncounters().values()) {
            encounter.hideFromPlayer(player);
        }
    }

    @EventHandler
    public void onWorldLoad(org.bukkit.event.world.WorldLoadEvent event) {
        World world = event.getWorld();
        if (EndIslandManager.isTheEnd(world)) {
            checkAndWrapDragons(world);
        }
    }

    public void checkAndWrapDragons(World world) {
        if (world != null && EndIslandManager.isTheEnd(world)) {
            for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                if (dragon.isValid() && !dragon.isDead()) {
                    wrapDragon(dragon);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (DragonEncounter encounter : DragonEncounter.getEncounters().values()) {
            encounter.hideFromPlayer(player);
        }
    }
}
