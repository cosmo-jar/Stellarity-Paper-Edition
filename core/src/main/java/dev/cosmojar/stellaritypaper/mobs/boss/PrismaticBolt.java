package dev.cosmojar.stellaritypaper.mobs.boss;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import dev.cosmojar.stellaritypaper.integration.DamageSafetyHelper;

/**
 * Класс, управляющий самонаводящимся снарядом "Призматический болт" Императрицы Света.
 */
public final class PrismaticBolt {

    public static final String BOLT_TAG = "stellarity.empress_bolt";

    private final Plugin plugin;
    private final ItemDisplay display;
    private final Player target;
    private Vector velocity;
    private int ticksLived = 0;
    private final boolean isDaytime;
    private final double damage;

    /** Ссылка на векса Императрицы — нужна для отражения болта обратно, но че то он не летит вродь */
    private LivingEntity empress;

    private boolean deflected = false;

    public PrismaticBolt(
            final Plugin plugin,
            final LivingEntity empress,
            final Location startLoc,
            final Player target,
            final Vector initialDir,
            final boolean isDaytime,
            final double damage
    ) {
        this.plugin = plugin;
        this.empress = empress;
        this.target = target;
        this.isDaytime = isDaytime;
        this.damage = damage;
        this.velocity = initialDir.normalize().multiply(0.4);

        this.display = startLoc.getWorld().spawn(startLoc, ItemDisplay.class, d -> {
            d.setInvulnerable(false);
            d.setGravity(false);
            d.setPersistent(false);
            d.setItemStack(new ItemStack(Material.AMETHYST_SHARD));
            d.setTeleportDuration(1);
            d.addScoreboardTag(BOLT_TAG);
        });
    }

    public PrismaticBolt(
            final Plugin plugin,
            final Location startLoc,
            final Player target,
            final Vector initialDir,
            final boolean isDaytime,
            final double damage
    ) {
        this(plugin, null, startLoc, target, initialDir, isDaytime, damage);
    }

    public void setEmpress(final LivingEntity empress) {
        this.empress = empress;
    }

    public ItemDisplay getDisplay() {
        return display;
    }


    public boolean onPlayerHit() {
        if (deflected) return false;

        final Location loc = display.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2F, 1.4F);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 12, 0.2, 0.2, 0.2, 0.1);

        if (Math.random() < 0.5) {
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0F, 1.2F);
            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 8, 0.1, 0.1, 0.1, 0.05);
            cleanup();
            return false;
        } else {
            deflected = true;
            if (empress != null) {
                final Vector toEmpress = empress.getLocation().add(0, 1, 0).toVector()
                        .subtract(loc.toVector()).normalize().multiply(0.6);
                velocity = toEmpress;
            }
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 10, 0.1, 0.1, 0.1, 0.05);
            return true;
        }
    }


    public boolean tick() {
        ticksLived++;
        if (ticksLived > 120 || !display.isValid()) {
            cleanup();
            return false;
        }

        final Location loc = display.getLocation();

        if (deflected) {
            if (empress == null || !empress.isValid() || empress.isDead()) {
                cleanup();
                return false;
            }
            final Vector toEmpress = empress.getLocation().add(0, 1, 0).toVector()
                    .subtract(loc.toVector()).normalize();
            velocity = velocity.add(toEmpress.multiply(0.06)).normalize().multiply(0.6);

            loc.add(velocity);
            display.teleport(loc);

            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.02, 0.02, 0.02, 0.01);

            if (loc.getWorld().equals(empress.getLocation().getWorld()) && loc.distance(empress.getLocation()) <= 1.5) {
                final double reflectDamage = damage * (0.3 + Math.random() * 0.2);
                if (target != null && target.isValid()) {
                    empress.damage(reflectDamage, target);
                } else {
                    empress.damage(reflectDamage);
                }
                loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 0.8F);
                EmpressOfLight.spawnParticleSafe(loc.getWorld(), Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
                cleanup();
                return false;
            }
        } else {
            if (target == null || !target.isOnline() || target.isDead() || !loc.getWorld().equals(target.getWorld())) {
                cleanup();
                return false;
            }

            final Vector toTarget = target.getEyeLocation().toVector().subtract(loc.toVector()).normalize();
            velocity = velocity.add(toTarget.multiply(0.08)).normalize().multiply(0.5);

            loc.add(velocity);
            display.teleport(loc);

            final float hue = (ticksLived * 15) % 360 / 360.0f;
            final java.awt.Color awtColor = java.awt.Color.getHSBColor(hue, 1.0f, 1.0f);
            final Color color = Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            loc.getWorld().spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(color, 1.2f));

            if (loc.distance(target.getLocation()) <= 1.2) {
                if (DamageSafetyHelper.canDamage(target, empress)) {
                    final double finalDamage = isDaytime ? 10000.0 : damage;
                    final org.bukkit.damage.DamageType type = isDaytime
                            ? org.bukkit.damage.DamageType.MAGIC
                            : org.bukkit.damage.DamageType.MOB_PROJECTILE;
                    try {
                        final org.bukkit.damage.DamageSource.Builder builder = org.bukkit.damage.DamageSource.builder(type);
                        if (empress != null && empress.isValid()) {
                            builder.withCausingEntity(empress).withDirectEntity(empress);
                        }
                        target.damage(finalDamage, builder.build());
                    } catch (final Throwable ignored) {
                        if (empress != null && empress.isValid()) {
                            target.damage(finalDamage, empress);
                        } else {
                            target.damage(finalDamage);
                        }
                    }
                }
                loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.2F);
                cleanup();
                return false;
            }
        }
        return true;
    }

    public void cleanup() {
        if (display.isValid()) display.remove();
    }
}
