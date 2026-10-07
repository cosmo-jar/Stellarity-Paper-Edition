package dev.cosmojar.stellaritypaper.mobs.boss;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Allay;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import dev.cosmojar.stellaritypaper.integration.DamageSafetyHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс, реализующий босса Shulking.
 * Содержит летающий ИИ на базе эллея, щит из стержней Энда и взрывные атаки и т.д..
 */
public final class Shulking implements StellarityBoss {

    private final Plugin plugin;
    private final Allay allay;
    private final Shulker bodyShulker;
    private final BossManager bossManager;

    private final double maxHealth;
    private final double baseDamage;
    private final int maxRods;
    private final double rodHealth;

    private int attackTimer = 0;
    private double shieldAngle = 0.0;
    private Player currentTarget = null;
    private int ticksWithoutPlayers = 0;

    private static final class ShieldRod {
        final ItemDisplay display;
        final Shulker shulker;

        ShieldRod(final ItemDisplay display, final Shulker shulker) {
            this.display = display;
            this.shulker = shulker;
        }

        void cleanup() {
            if (display != null) display.remove();
            if (shulker != null) shulker.remove();
        }
    }

    private static final class ShulkheadMinion {
        final org.bukkit.entity.Vex vex;
        final ArmorStand armorStand;
        long lastAttackTime = 0L;

        ShulkheadMinion(final org.bukkit.entity.Vex vex, final ArmorStand armorStand) {
            this.vex = vex;
            this.armorStand = armorStand;
        }

        boolean isValid() {
            return vex != null && vex.isValid() && !vex.isDead()
                && armorStand != null && armorStand.isValid() && !armorStand.isDead();
        }

        void cleanup() {
            if (vex != null) vex.remove();
            if (armorStand != null) armorStand.remove();
        }
    }

    private final List<ShieldRod> shieldRods = new ArrayList<>();
    private final List<ShulkheadMinion> minions = new ArrayList<>();
    private final Location altarLocation;


    public Shulking(
            final Plugin plugin,
            final Location spawnLoc,
            final BossManager bossManager,
            final Location altarLocation
    ) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.altarLocation = altarLocation != null ? altarLocation.clone() : null;

        final org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        this.maxHealth = config.getDouble("boss.shulking.max-health", 900.0);
        this.baseDamage = config.getDouble("boss.shulking.base-damage", 8.0);
        this.maxRods = config.getInt("boss.shulking.shield-rods-count", 8);
        this.rodHealth = config.getDouble("boss.shulking.shield-rod-health", 50.0);

        this.allay = spawnLoc.getWorld().spawn(spawnLoc, Allay.class, a -> {
            a.setAI(false);
            a.setInvisible(true);
            a.setSilent(true);
            a.setGravity(false);
            a.setCustomName("§dShulking");
            a.setCustomNameVisible(true);
            a.setRemoveWhenFarAway(false);
            a.setPersistent(false);

            a.getAttribute(Attribute.SCALE).setBaseValue(2.0);
            a.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            a.setHealth(maxHealth);

            a.getPersistentDataContainer().set(new NamespacedKey(plugin, "boss_type"), org.bukkit.persistence.PersistentDataType.STRING, "shulking");
            a.getPersistentDataContainer().set(new NamespacedKey(plugin, "boss_uuid"), org.bukkit.persistence.PersistentDataType.STRING, a.getUniqueId().toString());
            a.getScoreboardTags().add("stellarity.shulking");
        });

        this.bodyShulker = spawnLoc.getWorld().spawn(spawnLoc, Shulker.class, s -> {
            s.setAI(false);
            s.setGravity(false);
            s.setCustomName("§dShulking");
            s.setCustomNameVisible(false);
            s.setRemoveWhenFarAway(false);
            s.setPersistent(false);
            s.getAttribute(Attribute.SCALE).setBaseValue(2.0);
            s.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            s.setHealth(maxHealth);

            s.getPersistentDataContainer().set(new NamespacedKey(plugin, "boss_uuid"), org.bukkit.persistence.PersistentDataType.STRING, allay.getUniqueId().toString());
            s.getScoreboardTags().add("stellarity.shulking.body");
        });

        this.allay.addPassenger(this.bodyShulker);

        spawnShieldRods(spawnLoc);

        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 1.5F, 1.0F);

        bossManager.registerBoss(this);
    }

    private void spawnShieldRods(final Location loc) {
        for (int i = 0; i < maxRods; i++) {
            final double angle = i * (2 * Math.PI / maxRods);
            final double x = Math.cos(angle) * 3.5;
            final double z = Math.sin(angle) * 3.5;
            final Location rodLoc = loc.clone().add(x, 1.0, z);

            final ItemDisplay rodDisplay = loc.getWorld().spawn(rodLoc, ItemDisplay.class, d -> {
                d.setInvulnerable(true);
                d.setGravity(false);
                d.setPersistent(false);
                d.setTeleportDuration(1);
                d.setItemStack(new ItemStack(Material.END_ROD));

                final Transformation trans = d.getTransformation();
                trans.getTranslation().set(0.0f, -0.5f, 0.0f);
                d.setTransformation(trans);

                d.getPersistentDataContainer().set(new NamespacedKey(plugin, "boss_uuid"), org.bukkit.persistence.PersistentDataType.STRING, allay.getUniqueId().toString());
                d.getScoreboardTags().add("stellarity.shulking.rod");
            });

            final Shulker rodShulker = loc.getWorld().spawn(rodLoc, Shulker.class, s -> {
                s.setAI(false);
                s.setGravity(false);
                s.setCustomNameVisible(false);
                s.setRemoveWhenFarAway(false);
                s.setPersistent(false);
                s.getAttribute(Attribute.SCALE).setBaseValue(0.35);
                s.getAttribute(Attribute.MAX_HEALTH).setBaseValue(rodHealth);
                s.setHealth(rodHealth);
                
                s.getPersistentDataContainer().set(new NamespacedKey(plugin, "boss_uuid"), org.bukkit.persistence.PersistentDataType.STRING, allay.getUniqueId().toString());
                s.getScoreboardTags().add("stellarity.shulking.rod_shulker");
            });

            rodDisplay.addPassenger(rodShulker);

            shieldRods.add(new ShieldRod(rodDisplay, rodShulker));
        }
    }

    @Override
    public LivingEntity getBaseEntity() {
        return allay;
    }

    @Override
    public ItemDisplay getDisplayEntity() {
        return null;
    }

    @Override
    public boolean isAlive() {
        return allay != null && allay.isValid() && !allay.isDead();
    }

    @Override
    public Location getAltarLocation() {
        return altarLocation;
    }

    @Override
    public void tick() {
        if (!isAlive()) {
            cleanup();
            return;
        }

        if (bodyShulker != null && bodyShulker.isValid()) {
            bodyShulker.setHealth(bodyShulker.getMaxHealth());
        }

        findNearestTarget();

        if (currentTarget != null && currentTarget.isOnline() && !currentTarget.isDead()) {
            flyTowardsTarget();
        }

        tickShieldRotation();

        attackTimer++;
        if (attackTimer >= 60) {
            attackTimer = 0;
            executeRandomAttack();
        }

        tickShieldHealthCheck();

        tickMinionsLogic();
    }

    private void tickMinionsLogic() {
        minions.removeIf(minion -> {
            if (!minion.isValid()) {
                minion.cleanup();
                return true;
            }
            return false;
        });

        if (minions.isEmpty()) {
            return;
        }

        final long now = System.currentTimeMillis();

        for (final ShulkheadMinion minion : minions) {
            final LivingEntity target = minion.vex.getTarget();
            if (target instanceof Player player) {
                if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL || player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                    if (minion.vex.getLocation().distance(player.getLocation()) <= 2.2D) {
                        if (now - minion.lastAttackTime >= 1000L) {
                            minion.lastAttackTime = now;
                            
                            if (DamageSafetyHelper.canDamage(player, minion.vex)) {
                                player.damage(4.0D, minion.vex);
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, 1.0F, 1.2F);
                                
                                final Vector push = player.getLocation().toVector().subtract(minion.vex.getLocation().toVector()).normalize().multiply(0.25D);
                                push.setY(0.15D);
                                player.setVelocity(push);
                            }
                        }
                    }
                }
            }
        }

        if (allay.getTicksLived() % 10 == 0) {
            final List<Player> targets = new ArrayList<>();
            for (final Player player : allay.getWorld().getPlayers()) {
                if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL || player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                    if (player.getLocation().distance(allay.getLocation()) <= 35.0D) {
                        targets.add(player);
                    }
                }
            }

            if (!targets.isEmpty()) {
                for (int i = 0; i < minions.size(); i++) {
                    final ShulkheadMinion minion = minions.get(i);
                    
                    if (minion.vex.getLocation().distance(allay.getLocation()) > 30.0D) {
                        BossManager.teleportWithPassengers(minion.vex, allay.getLocation().add((Math.random() - 0.5D) * 6.0D, 1.5D, (Math.random() - 0.5D) * 6.0D));
                    }

                    final Player target = targets.get(i % targets.size());
                    if (minion.vex.getTarget() == null || !minion.vex.getTarget().equals(target)) {
                        minion.vex.setTarget(target);
                    }
                }
            } else {
                for (final ShulkheadMinion minion : minions) {
                    minion.vex.setTarget(null);
                    if (minion.vex.getLocation().distance(allay.getLocation()) > 15.0D) {
                        BossManager.teleportWithPassengers(minion.vex, allay.getLocation().add((Math.random() - 0.5D) * 4.0D, 1.5D, (Math.random() - 0.5D) * 4.0D));
                    }
                }
            }
        }
    }

    private void findNearestTarget() {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        boolean playerNearby = false;

        for (final Player player : allay.getWorld().getPlayers()) {
            final double dist = player.getLocation().distance(allay.getLocation());
            if (dist <= 45.0D) {
                playerNearby = true;
            }

            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                continue;
            }

            if (dist < minDist && dist <= 45.0D) {
                minDist = dist;
                nearest = player;
            }
        }
        
        this.currentTarget = nearest;
        if (nearest != null) {
            allay.setTarget(nearest);
        }

        if (playerNearby) {
            ticksWithoutPlayers = 0;
        } else {
            ticksWithoutPlayers++;
            if (ticksWithoutPlayers >= 400) {
                Bukkit.broadcast(bossManager.getMessageService().message("boss.shulking.despawn"));
                cleanup();
            }
        }
    }

    private void flyTowardsTarget() {
        final Location current = allay.getLocation();
        final Location playerLoc = currentTarget.getLocation();
        
        final double groundY = playerLoc.getWorld().getHighestBlockYAt(current);
        
        double targetY = Math.max(playerLoc.getY() + 2.5D, groundY + 3.5D);
        
        if (targetY > playerLoc.getY() + 4.5D) {
            targetY = playerLoc.getY() + 4.5D;
        }

        final Location targetLoc = new Location(current.getWorld(), playerLoc.getX(), targetY, playerLoc.getZ());
        final Vector dir = targetLoc.toVector().subtract(current.toVector());
        final double dist = dir.length();

        if (dist > 0.25D) {
            final Vector step = dir.normalize().multiply(Math.min(dist, 0.12D));
            current.add(step);
            
            final float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
            current.setYaw(yaw);
            
            allay.teleport(current);
            if (bodyShulker != null && bodyShulker.isValid()) {
                bodyShulker.teleport(current);
                if (bodyShulker.getVehicle() == null) {
                    allay.addPassenger(bodyShulker);
                }
            }
        }
    }

    private void tickShieldRotation() {
        shieldAngle += Math.toRadians(2.5);
        final Location center = allay.getLocation();
        
        for (int i = 0; i < shieldRods.size(); i++) {
            final ShieldRod rod = shieldRods.get(i);
            if (rod.display == null || !rod.display.isValid()) {
                continue;
            }
            
            final double currentAngle = shieldAngle + (i * (2 * Math.PI / maxRods));
            final double x = Math.cos(currentAngle) * 3.5;
            final double z = Math.sin(currentAngle) * 3.5;
            final Location targetLoc = center.clone().add(x, 1.0, z);
            
            rod.display.teleport(targetLoc);
            if (rod.shulker != null && rod.shulker.isValid()) {
                rod.shulker.teleport(targetLoc);
                if (rod.shulker.getVehicle() == null) {
                    rod.display.addPassenger(rod.shulker);
                }
            }
        }
    }

    private void tickShieldHealthCheck() {
        final boolean hadShield = !shieldRods.isEmpty();
        shieldRods.removeIf(rod -> {
            if (rod.shulker == null || !rod.shulker.isValid() || rod.shulker.isDead() || rod.shulker.getHealth() <= 0.0) {
                final Location loc = rod.display != null && rod.display.isValid() ? rod.display.getLocation() : allay.getLocation();
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0.0, 0.0, 0.0, 0.0);
                loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.5F, 0.8F);
                rod.cleanup();
                return true;
            }
            return false;
        });

        if (hadShield && shieldRods.isEmpty()) {
            final Location loc = allay.getLocation().add(0, 1.5, 0);
            loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, 1.5F, 0.9F);
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5F, 0.7F);
            loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0.0, 0.0, 0.0, 0.0);
            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 60, 0.5, 0.5, 0.5, 0.15);
        }
    }

    private void executeRandomAttack() {
        if (currentTarget == null) return;

        final double rand = Math.random();
        if (rand < 0.35) {
            launchExplosiveBullet();
        } else if (rand < 0.70) {
            summonMinions();
        } else {
            triggerSpikeAttack();
        }
    }

    private void launchExplosiveBullet() {
        allay.getWorld().playSound(allay.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1.5F, 0.8F);
        
        final Location spawnLoc = allay.getLocation().add(0, 1.5, 0);
        final ShulkerBullet bullet = allay.getWorld().spawn(spawnLoc, ShulkerBullet.class, b -> {
            b.setTarget(currentTarget);
            b.setShooter(bodyShulker);
            b.getPersistentDataContainer().set(new NamespacedKey(plugin, "explosive_bullet"), org.bukkit.persistence.PersistentDataType.STRING, "true");
        });
    }

    private void summonMinions() {
        allay.getWorld().playSound(allay.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.2F, 1.2F);
        
        minions.removeIf(m -> !m.isValid());
        if (minions.size() >= 6) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            final Location loc = allay.getLocation().add((Math.random() - 0.5) * 8.0, 2.0, (Math.random() - 0.5) * 8.0);
            
            final org.bukkit.entity.Vex vexMinion = allay.getWorld().spawn(loc, org.bukkit.entity.Vex.class, v -> {
                v.setCustomName("§dShulkhead Minion Base");
                v.setCustomNameVisible(false);
                v.setTarget(currentTarget);
                v.getEquipment().clear();
                
                v.setInvisible(true);
                v.setSilent(true);
                v.setRemoveWhenFarAway(false);
                v.setPersistent(false);
                v.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 0, false, false, false));
                
                v.getScoreboardTags().add("stellarity.shulking.minion");
            });

            final ArmorStand armorStandBody = loc.getWorld().spawn(loc, ArmorStand.class, s -> {
                s.setCustomName("§dShulkhead Minion");
                s.setCustomNameVisible(false);
                s.setInvisible(true);
                s.setMarker(true);
                s.setSmall(true);
                s.setBasePlate(false);
                s.setArms(false);
                s.setGravity(false);
                s.setPersistent(false);
                
                s.getEquipment().setHelmet(new ItemStack(Material.PURPLE_SHULKER_BOX));
                
                s.getScoreboardTags().add("stellarity.shulking.minion_body");
            });

            vexMinion.addPassenger(armorStandBody);

            minions.add(new ShulkheadMinion(vexMinion, armorStandBody));
        }
    }

    private void triggerSpikeAttack() {
        allay.getWorld().playSound(allay.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.2F, 1.4F);
        
        final Location start = allay.getLocation();
        final Vector dir = currentTarget.getLocation().toVector().subtract(start.toVector()).normalize();

        for (int i = 1; i <= 8; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isAlive()) return;
                final Location spikeLoc = start.clone().add(dir.clone().multiply(index * 1.8));
                spikeLoc.setY(spikeLoc.getWorld().getHighestBlockYAt(spikeLoc) + 1.0);
                spikeLoc.getWorld().spawn(spikeLoc, EvokerFangs.class, f -> {
                    f.setOwner(bodyShulker);
                });
            }, i * 2L);
        }
    }

    @Override
    public void cleanup() {
        if (bodyShulker != null) bodyShulker.remove();
        if (allay != null) allay.remove();

        for (final ShieldRod rod : shieldRods) {
            rod.cleanup();
        }
        shieldRods.clear();

        for (final ShulkheadMinion minion : minions) {
            minion.cleanup();
        }
        minions.clear();
    }

    @Override
    public boolean isInvulnerable() {
        return !shieldRods.isEmpty();
    }

    @Override
    public void damageShield(double damage) {
        if (!shieldRods.isEmpty()) {
            final ShieldRod rod = shieldRods.get(0);
            if (rod.shulker != null && rod.shulker.isValid()) {
                rod.shulker.damage(damage);
                
                allay.getWorld().playSound(rod.shulker.getLocation(), Sound.ENTITY_SHULKER_HURT, 1.0F, 1.3F);
                
                allay.getWorld().spawnParticle(Particle.CRIT, rod.shulker.getLocation().add(0.0D, 0.5D, 0.0D), 5, 0.1D, 0.1D, 0.1D, 0.1D);
            }
        }
    }

    public void playHurtEffect() {
        if (bodyShulker != null && bodyShulker.isValid()) {
            try {
                bodyShulker.playHurtAnimation(0.0f);
            } catch (final Throwable ignored) {}
            allay.getWorld().playSound(bodyShulker.getLocation(), Sound.ENTITY_SHULKER_HURT, 1.0F, 0.8F);
        }
    }
}
