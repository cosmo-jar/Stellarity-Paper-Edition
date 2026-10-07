package dev.cosmojar.stellaritypaper.mobs.boss;

import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;

import dev.cosmojar.stellaritypaper.text.MessageService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер управления боссами плагина.
 * Отвечает за оптимизированный жизненный цикл боссов, перенаправление урона и обработку выгрузки чанков.
 */
public final class BossManager implements Listener {

    private final Plugin plugin;
    private final MessageService messageService;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final CustomItemFactory customItemFactory;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;
    private final Map<UUID, StellarityBoss> activeBosses = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    private static final double BOSSBAR_RANGE = 28.0;

    private BukkitTask tickTask = null;

    private final dev.cosmojar.stellaritypaper.enchants.CustomStatusEffectService customStatusEffectService;

    public BossManager(
            final Plugin plugin,
            final MessageService messageService,
            final ItemDefinitionRegistry itemDefinitionRegistry,
            final CustomItemFactory customItemFactory,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService,
            final dev.cosmojar.stellaritypaper.enchants.CustomStatusEffectService customStatusEffectService
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.customItemFactory = customItemFactory;
        this.advancementService = advancementService;
        this.customStatusEffectService = customStatusEffectService;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public dev.cosmojar.stellaritypaper.enchants.CustomStatusEffectService getCustomStatusEffectService() {
        return customStatusEffectService;
    }

    public MessageService getMessageService() {
        return messageService;
    }


    public void registerBoss(final StellarityBoss boss) {
        if (boss != null && boss.getBaseEntity() != null) {
            final UUID uid = boss.getBaseEntity().getUniqueId();
            activeBosses.put(uid, boss);

            if (!boss.getClass().getSimpleName().equals("StellarityDragon")) {
                final String barTitle;
                final BarColor barColor;
                if (boss instanceof EmpressOfLight empress) {
                    if (empress.isRadiant()) {
                        barTitle = messageService.raw("boss.radiant_empress_of_light.name");
                        barColor = BarColor.RED;
                    } else {
                        barTitle = messageService.raw("boss.empress_of_light.name");
                        barColor = BarColor.PINK;
                    }
                } else if (boss instanceof Shulking) {
                    barTitle = messageService.raw("boss.shulking.name");
                    barColor = BarColor.PURPLE;
                } else {
                    barTitle = boss.getBaseEntity().getCustomName() != null
                            ? boss.getBaseEntity().getCustomName() : "Босс";
                    barColor = BarColor.RED;
                }
                final BossBar bar = Bukkit.createBossBar(barTitle, barColor, BarStyle.SEGMENTED_10);
                bar.setProgress(1.0);
                bar.setVisible(true);
                bossBars.put(uid, bar);
            }

            checkTaskState();
        }
    }


    public StellarityBoss getBoss(final UUID uuid) { return activeBosses.get(uuid); }

    public void unregisterBoss(final UUID uuid) {
        final StellarityBoss boss = activeBosses.remove(uuid);
        if (boss != null) {
            boss.cleanup();
        }
        removeBossBar(uuid);
        checkTaskState();
    }

    public boolean isAltarActive(final Location altarLoc) {
        if (altarLoc == null || altarLoc.getWorld() == null) return false;
        for (final StellarityBoss boss : activeBosses.values()) {
            if (!boss.isAlive()) continue;
            final Location bossAltar = boss.getAltarLocation();
            if (bossAltar != null && bossAltar.getWorld() != null
                    && bossAltar.getWorld().equals(altarLoc.getWorld())
                    && bossAltar.getBlockX() == altarLoc.getBlockX()
                    && bossAltar.getBlockY() == altarLoc.getBlockY()
                    && bossAltar.getBlockZ() == altarLoc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    private void removeBossBar(final UUID uuid) {
        final BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }


    private void checkTaskState() {
        if (!activeBosses.isEmpty() && tickTask == null) {
            tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBosses, 1L, 1L);
        } else if (activeBosses.isEmpty() && tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void tickBosses() {
        activeBosses.entrySet().removeIf(entry -> {
            final UUID uid = entry.getKey();
            final StellarityBoss boss = entry.getValue();
            if (!boss.isAlive()) {
                boss.cleanup();
                removeBossBar(uid);
                return true;
            }
            try {
                boss.tick();
            } catch (final Exception e) {
                plugin.getLogger().severe("Tick exception [BossManager]: " + e.getMessage());
                e.printStackTrace();
            }
            final BossBar bar = bossBars.get(uid);
            if (bar != null) {
                tickBossBar(boss, bar);
            }
            return false;
        });
        checkTaskState();
    }

    private void tickBossBar(final StellarityBoss boss, final BossBar bar) {
        final LivingEntity base = boss.getBaseEntity();
        if (base == null) return;

        final double progress;
        if (boss instanceof EmpressOfLight empress) {
            progress = Math.max(0.0, Math.min(1.0, empress.getCurrentHealth() / empress.getMaxHealth()));
        } else {
            final var maxHpAttr = base.getAttribute(Attribute.MAX_HEALTH);
            final double maxHp = maxHpAttr != null ? maxHpAttr.getValue() : 1000.0;
            progress = Math.max(0.0, Math.min(1.0, base.getHealth() / maxHp));
        }
        bar.setProgress(progress);

        if (boss instanceof EmpressOfLight empress) {
            if (empress.isRadiant()) {
                if (progress < 0.25) bar.setColor(BarColor.PURPLE);
                else if (progress < 0.50) bar.setColor(BarColor.YELLOW);
                else bar.setColor(BarColor.RED);
            } else {
                if (progress < 0.25) bar.setColor(BarColor.RED);
                else if (progress < 0.50) bar.setColor(BarColor.YELLOW);
                else bar.setColor(BarColor.PINK);
            }
        } else if (boss instanceof Shulking) {
            if (progress < 0.25) bar.setColor(BarColor.RED);
            else if (progress < 0.50) bar.setColor(BarColor.YELLOW);
            else bar.setColor(BarColor.PURPLE);
        }

        final Location bossLoc = base.getLocation();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(bossLoc.getWorld())) {
                bar.removePlayer(player);
                continue;
            }
            final double dist = player.getLocation().distance(bossLoc);
            if (dist <= BOSSBAR_RANGE) {
                bar.addPlayer(player);
            } else {
                bar.removePlayer(player);
            }
        }
    }


    public void cleanupAll() {
        for (final StellarityBoss boss : activeBosses.values()) {
            boss.cleanup();
        }
        activeBosses.clear();

        for (final BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private boolean isAdministrativeDamage(final EntityDamageEvent event) {
        final double damage = event.getDamage();
        if (damage >= 190.0) return true;
        
        final String causeName = event.getCause().name();
        if (causeName.contains("VOID") || causeName.contains("KILL") || causeName.contains("COMMAND") || causeName.contains("SUICIDE")) {
            return true;
        }

        if (event instanceof EntityDamageByEntityEvent subEvent) {
            if (subEvent.getDamager() instanceof org.bukkit.entity.Player player) {
                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE && player.isOp() && player.isSneaking()) {
                    return true;
                }
            }
        }
        return false;
    }

    private StellarityBoss findBoss(final Entity entity) {
        if (entity == null) return null;
        final UUID uuid = entity.getUniqueId();
        if (activeBosses.containsKey(uuid)) {
            return activeBosses.get(uuid);
        }
        if (entity instanceof org.bukkit.entity.ComplexEntityPart part && part.getParent() != null) {
            final UUID parentUid = part.getParent().getUniqueId();
            if (activeBosses.containsKey(parentUid)) {
                return activeBosses.get(parentUid);
            }
        }
        final Entity vehicle = entity.getVehicle();
        if (vehicle != null && activeBosses.containsKey(vehicle.getUniqueId())) {
            return activeBosses.get(vehicle.getUniqueId());
        }
        final String bossUuidStr = entity.getPersistentDataContainer().get(new NamespacedKey(plugin, "boss_uuid"), org.bukkit.persistence.PersistentDataType.STRING);
        if (bossUuidStr != null) {
            try {
                final UUID bossUid = UUID.fromString(bossUuidStr);
                if (activeBosses.containsKey(bossUid)) {
                    return activeBosses.get(bossUid);
                }
            } catch (final IllegalArgumentException ignored) {}
        }
        if (entity.getScoreboardTags().contains("stellarity.shulking.body")
            || entity.getScoreboardTags().contains("stellarity.empress_of_light.hitbox")
            || entity.getScoreboardTags().contains("stellarity.shulking.rod_shulker")) {
            for (final StellarityBoss boss : activeBosses.values()) {
                if (boss.getBaseEntity().getWorld().equals(entity.getWorld()) 
                    && boss.getBaseEntity().getLocation().distance(entity.getLocation()) < 8.0) {
                    return boss;
                }
            }
        }
        return null;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onEntityDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();

        final StellarityBoss targetBoss = findBoss(entity);
        if (targetBoss != null && isAdministrativeDamage(event)) {
            event.setCancelled(true);
            targetBoss.cleanup();
            unregisterBoss(targetBoss.getBaseEntity().getUniqueId());
            return;
        }

        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }

        if (entity.getScoreboardTags().contains("stellarity.shulking.rod_shulker")) {
            event.setCancelled(true);
            return;
        }

        if (entity.getScoreboardTags().contains("stellarity.empress_of_light.hitbox") ||
            entity.getScoreboardTags().contains("stellarity.empress_of_light")) {
            event.setCancelled(true);
            return;
        }

        if (entity.getScoreboardTags().contains("stellarity.shulking.body")) {
            event.setCancelled(true);
            if (targetBoss instanceof Shulking shulking && !shulking.isInvulnerable()) {
                final LivingEntity base = shulking.getBaseEntity();
                if (base != null && base.isValid() && !base.isDead()) {
                    base.damage(event.getDamage());
                }
            }
            return;
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();
        
        final StellarityBoss targetBoss = findBoss(entity);
        if (targetBoss != null && isAdministrativeDamage(event)) {
            event.setCancelled(true);
            targetBoss.cleanup();
            unregisterBoss(targetBoss.getBaseEntity().getUniqueId());
            return;
        }

        if (entity.getScoreboardTags().contains("stellarity.shulking.rod_shulker")) {
            return;
        }

        if (entity.getScoreboardTags().contains("stellarity.shulking.body")) {
            event.setCancelled(true);
            if (targetBoss instanceof Shulking shulking) {
                if (shulking.isInvulnerable()) {
                    final Location loc = entity.getLocation().add(0, 1.0, 0);
                    loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                    loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2F, 0.8F);
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1);
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 6, 0.3, 0.3, 0.3, 0.05);
                } else {
                    final LivingEntity base = shulking.getBaseEntity();
                    if (base != null && base.isValid() && !base.isDead()) {
                        if (event.getDamager() instanceof Player player) {
                            player.attack(base);
                        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
                            base.damage(event.getDamage(), projectile);
                        } else {
                            base.damage(event.getDamage(), event.getDamager());
                        }
                        shulking.playHurtEffect();
                    }
                }
            }
            return;
        }

        if (entity instanceof ItemDisplay) {
            final Entity vehicle = entity.getVehicle();
            if (vehicle instanceof LivingEntity base && activeBosses.containsKey(vehicle.getUniqueId())) {
                event.setCancelled(true);
                final StellarityBoss boss = activeBosses.get(vehicle.getUniqueId());
                if (boss instanceof EmpressOfLight empress) {
                    if (empress.isDying() || empress.isSpawning()) return;
                    if (event.getDamager() instanceof Player player) {
                        player.attack(base);
                    } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
                        base.damage(event.getDamage(), projectile);
                    } else {
                        base.damage(event.getDamage(), event.getDamager());
                    }
                } else if (boss instanceof Shulking shulking) {
                    if (shulking.isInvulnerable()) {
                        final Location loc = entity.getLocation().add(0, 1.0, 0);
                        loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2F, 0.8F);
                        loc.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1);
                    } else {
                        shulking.getBaseEntity().damage(event.getDamage(), event.getDamager());
                    }
                }
                return;
            }
        }

        if (entity.getScoreboardTags().contains("stellarity.empress_of_light.hitbox")) {
            event.setCancelled(true);
            final StellarityBoss boss = findBoss(entity);
            if (boss instanceof EmpressOfLight empress) {
                if (empress.isDying() || empress.isSpawning()) return;
                final LivingEntity base = empress.getBaseEntity();
                if (base != null && base.isValid()) {
                    if (event.getDamager() instanceof Player player) {
                        player.attack(base);
                    } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
                        base.damage(event.getDamage(), projectile);
                    } else {
                        base.damage(event.getDamage(), event.getDamager());
                    }
                }
            }
            return;
        }

        final StellarityBoss boss = findBoss(entity);
        if (boss != null) {
            if (boss instanceof EmpressOfLight empress) {
                if (empress.isDying() || empress.isSpawning()) {
                    event.setCancelled(true);
                    return;
                }
                // чтоб ванильный кулдаун, критические удары и EnchantRuntimeListener сработали
                return;
            }
            if (boss instanceof Shulking shulking) {
                if (shulking.isInvulnerable()) {
                    event.setCancelled(true);
                    final Location loc = entity.getLocation().add(0, 1.0, 0);
                    loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                    loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2F, 0.8F);
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1);
                }
                return;
            }
            if (boss.isInvulnerable()) {
                event.setCancelled(true);
                boss.damageShield(event.getDamage());
            }
            return;
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamageMonitor(final EntityDamageByEntityEvent event) {
        final UUID uuid = event.getEntity().getUniqueId();
        if (activeBosses.containsKey(uuid)) {
            final StellarityBoss boss = activeBosses.get(uuid);
            if (boss instanceof EmpressOfLight empress) {
                if (empress.isDying() || empress.isSpawning()) return;
                final double finalDamage = event.getFinalDamage();
                if (finalDamage <= 0.0) return;
                empress.syncHealthAfterHit(finalDamage, event.getDamager());
            }
        }
    }


    @EventHandler
    public void onBoltHit(final EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemDisplay display)) return;
        if (!display.getScoreboardTags().contains(PrismaticBolt.BOLT_TAG)) return;
        if (!(event.getDamager() instanceof org.bukkit.entity.Player)) return;

        event.setCancelled(true);

        for (final StellarityBoss boss : activeBosses.values()) {
            if (!(boss instanceof EmpressOfLight empress)) continue;
            if (empress.deflectBoltByDisplay(display)) break;
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(final org.bukkit.event.player.PlayerInteractEntityEvent event) {
        final Entity entity = event.getRightClicked();
        final UUID uuid = entity.getUniqueId();

        if (activeBosses.containsKey(uuid)) {
            event.setCancelled(true);
            return;
        }

        final Entity vehicle = entity.getVehicle();
        if (vehicle != null && activeBosses.containsKey(vehicle.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (entity.getScoreboardTags().contains("stellarity.shulking.body") ||
            entity.getScoreboardTags().contains("stellarity.shulking.rod_shulker")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(final org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity() instanceof ShulkerBullet bullet) {
            if (bullet.getPersistentDataContainer().has(new NamespacedKey(plugin, "explosive_bullet"), org.bukkit.persistence.PersistentDataType.STRING)) {
                final Location loc = bullet.getLocation();
                final Entity source = bullet.getShooter() instanceof Entity shooter ? shooter : bullet;
                loc.getWorld().createExplosion(source, loc, 2.5f, false, false);
                EmpressOfLight.spawnParticleSafe(loc.getWorld(), Particle.EXPLOSION_EMITTER, loc, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event) {
        final Entity entity = event.getEntity();

        if (entity.getScoreboardTags().contains("stellarity.shulking.rod_shulker")
            || entity.getScoreboardTags().contains("stellarity.shulking.body")
            || entity.getScoreboardTags().contains("stellarity.shulking.minion")
            || entity.getScoreboardTags().contains("stellarity.shulking.minion_body")) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }

        final StellarityBoss boss = findBoss(entity);
        if (boss != null) {
            if (boss instanceof EmpressOfLight empress && empress.isDying()) {
                event.getDrops().clear();
                event.setDroppedExp(0);
                return;
            }
            if (entity.equals(boss.getBaseEntity())) {
                handleBossDeath(boss, entity.getLocation());
                unregisterBoss(boss.getBaseEntity().getUniqueId());
            }
        }
    }

    public void handleBossDeath(final StellarityBoss boss, final Location loc) {
        final org.bukkit.World world = loc.getWorld();
        if (boss instanceof EmpressOfLight empress) {
            final boolean isRadiant = empress.isRadiant();
            final boolean isDay = empress.isDaytime();

            if (isRadiant) {
                dropItem(loc, "radiant_jewel", 1);
                dropItem(loc, "empress_wings", 1);
                if (Math.random() < 0.25) {
                    dropItem(loc, "kaleidoscope", 1);
                }
                if (Math.random() < 0.15) {
                    dropItem(loc, "pixie_in_a_jar_radiant", 1);
                }

                spawnExperience(loc, 3600);
            } else if (isDay) {
                dropItem(loc, "kaleidoscope", 1);
                dropItem(loc, "soaring_insignia", 1);

                if (Math.random() < 0.5) {
                    dropItem(loc, "empress_wings", 1);
                }
                if (Math.random() < 0.3) {
                    dropItem(loc, "fluffy_hammer", 1);
                }
                if (Math.random() < 0.3) {
                    dropItem(loc, "music_disc_precipice_stereo", 1);
                }
                if (Math.random() < 0.3) {
                    dropRandomPixieJar(loc);
                }
                if (Math.random() < 0.33) {
                    dropItem(loc, "prismatic_pearl", 1);
                }

                spawnExperience(loc, 1800);
            } else {
                final java.util.List<String> weapons = java.util.List.of("prismember", "prismatic_punch", "starstruck_shield");
                final String guaranteedWeapon = weapons.get((int) (Math.random() * weapons.size()));
                dropItem(loc, guaranteedWeapon, 1);

                if (Math.random() < 0.2) {
                    dropItem(loc, "empress_wings", 1);
                }
                if (Math.random() < 0.2) {
                    dropItem(loc, "soaring_insignia", 1);
                }
                if (Math.random() < 0.2) {
                    dropRandomPixieJar(loc);
                }
                if (Math.random() < 0.2) {
                    dropItem(loc, "music_disc_precipice_stereo", 1);
                }
                if (Math.random() < 0.25) {
                    dropItem(loc, "prismatic_pearl", 1);
                }

                for (final String weapon : weapons) {
                    if (Math.random() < 0.5) {
                        dropItem(loc, weapon, 1);
                    }
                }

                spawnExperience(loc, 600);
            }

            final Location effectLoc = loc.clone().add(0.0, 1.0, 0.0);
            EmpressOfLight.spawnParticleSafe(world, Particle.END_ROD, effectLoc, 150, 0.6, 0.6, 0.6, 0.1);
            EmpressOfLight.spawnParticleSafe(world, Particle.FIREWORK, effectLoc, 150, 0.6, 0.6, 0.6, 0.1);
            EmpressOfLight.spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, effectLoc, 1, 0.0, 0.0, 0.0, 0.0);

            world.playSound(effectLoc, Sound.ENTITY_ALLAY_DEATH, 1.5F, 0.8F);
            world.playSound(effectLoc, Sound.ENTITY_VEX_DEATH, 1.5F, 0.7F);
            world.playSound(effectLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0F, 0.8F);

            if (isRadiant) {
                Bukkit.broadcast(messageService.message("boss.radiant_empress_of_light.death"));
            } else {
                Bukkit.broadcast(messageService.message("boss.empress_of_light.death"));
            }

            if (advancementService != null && (isRadiant || isDay)) {
                for (final org.bukkit.entity.Entity entity : world.getNearbyEntities(loc, 32.0D, 32.0D, 32.0D)) {
                    if (entity instanceof org.bukkit.entity.Player player) {
                        advancementService.grant(player, "stellarity:empress_of_light/fae_flayer");
                    }
                }
            }
        } else if (boss instanceof Shulking) {
            if (Math.random() < 0.1) {
                dropItem(loc, "book_of_obstruct", 1);
            }
            if (Math.random() < 0.1) {
                dropItem(loc, "book_of_obstruct", 1);
            }
            
            for (int i = 0; i < 8; i++) {
                final int amount = (int) (Math.random() * 3) + 1;
                if (Math.random() < 0.5) {
                    dropVanillaItem(loc, org.bukkit.Material.PURPUR_BLOCK, amount);
                } else {
                    dropItem(loc, "golden_chorus_fruit", amount);
                }
            }
            
            final java.util.List<String> shulkerPool = java.util.List.of(
                "shulker_pickaxe", "shulker_axe", "shulker_sword", "shulker_spear", "shulker_shovel", "shulker_hoe",
                "shulker_boots", "shulker_chestplate", "shulker_leggings", "shulker_boots"
            );
            final String drop1 = shulkerPool.get((int) (Math.random() * shulkerPool.size()));
            final String drop2 = shulkerPool.get((int) (Math.random() * shulkerPool.size()));
            dropItem(loc, drop1, 1);
            dropItem(loc, drop2, 1);
            
            spawnExperience(loc, 1000);
            
            EmpressOfLight.spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, loc, 5, 0.5, 0.5, 0.5, 0.0);
            world.playSound(loc, Sound.ENTITY_WITHER_DEATH, 1.5F, 1.0F);
        } else if (boss instanceof dev.cosmojar.stellaritypaper.mobs.boss.dragon.StellarityDragon) {
            if (!dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager.isTheEnd(world)) {
                dropDragonLoot(loc);
            }
        }
    }

    private static final java.util.List<String> PIXIE_JARS = java.util.List.of(
            "pixie_in_a_jar_yellow",
            "pixie_in_a_jar_magenta",
            "pixie_in_a_jar_lime",
            "pixie_in_a_jar_light_blue"
    );

    private void dropRandomPixieJar(final Location loc) {
        final String jarId = PIXIE_JARS.get((int) (Math.random() * PIXIE_JARS.size()));
        dropItem(loc, jarId, 1);
    }

    private void dropItem(final Location loc, final String pdcItemId, final int amount) {
        final ItemStack itemStack = createCustomItem(pdcItemId);
        if (itemStack != null) {
            itemStack.setAmount(amount);
            final org.bukkit.entity.Item itemEntity = loc.getWorld().dropItemNaturally(loc, itemStack);
            itemEntity.setGlowing(true);
            itemEntity.addScoreboardTag("stellarity.boss_drop");
            itemEntity.addScoreboardTag("stellarity.item");
        }
    }

    public void dropDragonLoot(final Location loc) {
        // 100%
        dropItem(loc, "life_crystal", 1);

        if (Math.random() < 0.33) {
            dropVanillaItem(loc, org.bukkit.Material.DRAGON_HEAD, 1);
        }

        // 12-38
        final int membraneCount = 12 + (int) (Math.random() * (38 - 12 + 1));
        dropVanillaItem(loc, org.bukkit.Material.PHANTOM_MEMBRANE, membraneCount);

        if (Math.random() < 0.25) {
            dropItem(loc, "dragon_wings", 1);
        }
    }

    public void dropVanillaItem(final Location loc, final org.bukkit.Material material, final int amount) {
        final ItemStack itemStack = new ItemStack(material, amount);
        final org.bukkit.entity.Item itemEntity = loc.getWorld().dropItemNaturally(loc, itemStack);
        itemEntity.setGlowing(true);
        itemEntity.addScoreboardTag("stellarity.boss_drop");
        itemEntity.addScoreboardTag("stellarity.item");
    }

    private void spawnExperience(final Location loc, final int amount) {
        loc.getWorld().spawn(loc, org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(amount));
    }

    private ItemStack createCustomItem(final String id) {
        if (itemDefinitionRegistry == null || customItemFactory == null || id == null) {
            return null;
        }
        Optional<CustomItemDefinition> defOpt = itemDefinitionRegistry.findByPdcItemId(id);
        if (defOpt.isEmpty()) {
            for (final String category : itemDefinitionRegistry.listCategories()) {
                final Optional<CustomItemDefinition> byCat = itemDefinitionRegistry.findByCategoryAndName(category, id);
                if (byCat.isPresent()) {
                    defOpt = byCat;
                    break;
                }
            }
        }
        return defOpt.map(customItemFactory::create).orElse(null);
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        activeBosses.values().removeIf(boss -> {
            if (boss.getBaseEntity().getLocation().getChunk().equals(event.getChunk())) {
                if (boss instanceof EmpressOfLight empress) {
                    if (empress.isRadiant()) {
                        Bukkit.broadcast(messageService.message("boss.radiant_empress_of_light.despawn"));
                    } else {
                        Bukkit.broadcast(messageService.message("boss.empress_of_light.despawn"));
                    }
                } else if (boss instanceof Shulking) {
                    Bukkit.broadcast(messageService.message("boss.shulking.despawn"));
                }
                boss.cleanup();
                return true;
            }
            return false;
        });
        checkTaskState();
    }


    public static void teleportWithPassengers(final Entity entity, final Location loc) {
        final java.util.List<Entity> passengers = new java.util.ArrayList<>(entity.getPassengers());
        for (final Entity passenger : passengers) {
            entity.removePassenger(passenger);
        }
        entity.teleport(loc);
        for (final Entity passenger : passengers) {
            passenger.teleport(loc);
            entity.addPassenger(passenger);
        }
    }
}
