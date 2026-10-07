package dev.cosmojar.stellaritypaper.mobs.boss;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import dev.cosmojar.stellaritypaper.integration.DamageSafetyHelper;

/**
 * Класс, управляющий копьем Ethereal Lance Императрицы Света.
 * Содержит фазу предупреждения (линия частиц) и фазу быстрого запуска снаряда.
 */
public final class EtherealLance {

    private final Plugin plugin;
    private final org.bukkit.entity.LivingEntity empress;
    private final Location startLoc;
    private final Vector direction;
    private final Player target;
    private final boolean isRadiant;
    private final boolean isDaytime;
    private final double damage;

    private ItemDisplay display = null;
    private int timer = 0;
    private boolean launched = false;
    private int blocksFlown = 0;

    public EtherealLance(
            final Plugin plugin,
            final org.bukkit.entity.LivingEntity empress,
            final Location startLoc,
            final Player target,
            final Vector direction,
            final boolean isRadiant,
            final boolean isDaytime,
            final double damage
    ) {
        this.plugin = plugin;
        this.empress = empress;
        this.startLoc = startLoc;
        this.target = target;
        this.direction = direction.normalize();
        this.isRadiant = isRadiant;
        this.isDaytime = isDaytime;
        this.damage = damage;
    }

    public EtherealLance(
            final Plugin plugin,
            final Location startLoc,
            final Player target,
            final Vector direction,
            final boolean isRadiant,
            final boolean isDaytime,
            final double damage
    ) {
        this(plugin, null, startLoc, target, direction, isRadiant, isDaytime, damage);
    }

    public EtherealLance(
            final Plugin plugin,
            final Location startLoc,
            final Player target,
            final Vector direction,
            final boolean isDaytime,
            final double damage
    ) {
        this(plugin, null, startLoc, target, direction, false, isDaytime, damage);
    }

    public boolean tick() {
        timer++;

        if (!launched) {
            if (timer % 3 == 0) {
                final Color warnColor = isRadiant ? Color.fromRGB(245, 42, 39) : (isDaytime ? Color.fromRGB(245, 207, 39) : Color.fromRGB(255, 60, 200));
                for (double d = 0; d < 35; d += 1.5) {
                    final Location p = startLoc.clone().add(direction.clone().multiply(d));
                    EmpressOfLight.spawnParticleSafe(p.getWorld(), Particle.DUST, p, 1, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(warnColor, 0.7f));
                }
            }

            if (timer >= 15) {
                launched = true;
                timer = 0;

                final Location spawnAt = startLoc.clone();
                spawnAt.setDirection(direction);

                display = spawnAt.getWorld().spawn(spawnAt, ItemDisplay.class, d -> {
                    d.setInvulnerable(true);
                    d.setGravity(false);
                    d.setPersistent(false);
                    d.setTeleportDuration(1);

                    final ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
                    final ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setItemModel(NamespacedKey.fromString("stellarity:_technical/ethereal_lance"));
                        if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
                            final Color lanceColor;
                            if (isRadiant) {
                                lanceColor = Color.fromRGB(245, 42, 39);
                            } else if (isDaytime) {
                                lanceColor = Color.fromRGB(245, 207, 39);
                            } else {
                                final Color[] rainbow = new Color[] {
                                    Color.fromRGB(245, 42, 39),
                                    Color.fromRGB(245, 104, 39),
                                    Color.fromRGB(245, 207, 39),
                                    Color.fromRGB(73, 245, 39),
                                    Color.fromRGB(39, 180, 245),
                                    Color.fromRGB(39, 87, 245),
                                    Color.fromRGB(204, 39, 245),
                                    Color.fromRGB(128, 39, 245)
                                };
                                lanceColor = rainbow[java.util.concurrent.ThreadLocalRandom.current().nextInt(rainbow.length)];
                            }
                            leatherMeta.setColor(lanceColor);
                        }
                        item.setItemMeta(meta);
                    }
                    d.setItemStack(item);
                });

                startLoc.getWorld().playSound(startLoc, Sound.ENTITY_ARROW_SHOOT, 1.2F, 1.5F);
            }
            return true;
        }

        if (display == null || !display.isValid()) {
            cleanup();
            return false;
        }

        final Location current = display.getLocation();
        current.add(direction.clone().multiply(1.5));
        current.setDirection(direction);
        display.teleport(current);
        blocksFlown += 1.5;

        EmpressOfLight.spawnParticleSafe(current.getWorld(), Particle.END_ROD, current, 1, 0.0, 0.0, 0.0, 0.0);

        if (target != null && target.isOnline() && !target.isDead() && current.getWorld().equals(target.getWorld())) {
            if (current.distance(target.getLocation()) <= 1.5) {
                if (DamageSafetyHelper.canDamage(target, empress)) {
                    final double finalDamage = isDaytime ? 10000.0 : damage;
                    final org.bukkit.damage.DamageType type = (isDaytime || isRadiant)
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
                current.getWorld().playSound(current, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0F, 1.8F);
                cleanup();
                return false;
            }
        }

        if (blocksFlown >= 45) {
            cleanup();
            return false;
        }

        return true;
    }

    public void cleanup() {
        if (display != null) {
            display.remove();
        }
    }
}
