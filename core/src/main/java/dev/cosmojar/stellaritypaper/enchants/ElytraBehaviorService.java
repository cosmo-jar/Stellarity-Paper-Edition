package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.items.CustomItemMatcher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public final class ElytraBehaviorService {

    private final org.bukkit.plugin.Plugin plugin;
    private final CustomItemMatcher itemMatcher;
    private final ItemsConfigService itemsConfig;
    private BukkitTask task;

    public ElytraBehaviorService(
            final org.bukkit.plugin.Plugin plugin,
            final CustomItemMatcher itemMatcher,
            final ItemsConfigService itemsConfig
    ) {
        this.plugin = plugin;
        this.itemMatcher = itemMatcher;
        this.itemsConfig = itemsConfig;
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isDragonWingsResistanceEnabled() { return itemsConfig.getMiscConfig().getBoolean("elytra.dragon-wings-resistance", true); }
    public boolean isElytraBoostNerfEnabled() { return itemsConfig.getMiscConfig().getBoolean("elytra.boost-durability-nerf", true); }

    private void tick() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.isGliding()) {
                final ItemStack chest = player.getEquipment().getChestplate();
                if (chest == null || chest.getType() != Material.ELYTRA) {
                    continue;
                }

                final boolean isDragon = itemMatcher.isCustom(chest, "dragon_wings");
                final boolean isEmpress = itemMatcher.isCustom(chest, "empress_wings");
                final boolean isPhantom = itemMatcher.isCustom(chest, "phantom_wings");

                if (isDragonWingsResistanceEnabled() && isDragon) {
                    if (player.getTicksLived() % 10 == 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 2, true, false, true));
                    }
                }
                
                if (isEmpress) {
                    spawnEmpressWingsParticles(player, chest);
                } else if (!isDragon && !isPhantom) {
                    spawnVanillaElytraParticles(player, chest);
                }
            }
        }
    }

    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (!isElytraBoostNerfEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        if (!player.isGliding()) {
            return;
        }

        final Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) {
            return;
        }

        final ItemStack chest = player.getEquipment().getChestplate();
        if (chest == null || chest.getType() == Material.AIR) {
            return;
        }

        final Material type = chest.getType();
        if (type == Material.ELYTRA) {
            final ItemMeta meta = chest.getItemMeta();
            if (meta instanceof Damageable damageable) {
                final int unbreaking = chest.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
                final int baseDmg = itemsConfig.getMiscConfig().getInt("elytra.boost-durability-damage", 8);
                final int loss = Math.max(1, baseDmg / (unbreaking + 1));
                
                final int newDamage = damageable.getDamage() + loss;
                final int maxDurability = type.getMaxDurability();
                
                if (newDamage >= maxDurability) {
                    damageable.setDamage(maxDurability - 1);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.9F);
                } else {
                    damageable.setDamage(newDamage);
                }
                
                chest.setItemMeta(meta);
            }
        }
    }

    private String getElytraDyeColor(final ItemStack elytra) {
        if (elytra == null || !elytra.hasItemMeta()) {
            return null;
        }
        final org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "elytra_color");
        return elytra.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
    }

    private org.bukkit.Color getBukkitColor(final String colorName) {
        if (colorName == null) {
            return null;
        }
        return switch (colorName.toLowerCase(java.util.Locale.ROOT)) {
            case "white" -> org.bukkit.Color.fromRGB(255, 255, 255);
            case "light_gray" -> org.bukkit.Color.fromRGB(201, 201, 201);
            case "gray" -> org.bukkit.Color.fromRGB(100, 100, 100);
            case "black" -> org.bukkit.Color.fromRGB(29, 29, 33);
            case "brown" -> org.bukkit.Color.fromRGB(129, 69, 0);
            case "red" -> org.bukkit.Color.fromRGB(255, 0, 0);
            case "orange" -> org.bukkit.Color.fromRGB(255, 136, 0);
            case "yellow" -> org.bukkit.Color.fromRGB(255, 255, 0);
            case "lime" -> org.bukkit.Color.fromRGB(0, 233, 12);
            case "green" -> org.bukkit.Color.fromRGB(20, 121, 0);
            case "cyan" -> org.bukkit.Color.fromRGB(0, 185, 176);
            case "light_blue" -> org.bukkit.Color.fromRGB(0, 217, 255);
            case "blue" -> org.bukkit.Color.fromRGB(0, 59, 185);
            case "purple" -> org.bukkit.Color.fromRGB(142, 0, 185);
            case "magenta" -> org.bukkit.Color.fromRGB(233, 0, 163);
            case "pink" -> org.bukkit.Color.fromRGB(255, 109, 206);
            default -> org.bukkit.Color.fromRGB(255, 255, 255);
        };
    }

    private void spawnEmpressWingsParticles(final Player player, final ItemStack chest) {
        final float pitch = player.getLocation().getPitch();
        final double xVal;
        final double zVal;
        if (pitch <= 10) {
            xVal = 1.4;
            zVal = -1.1;
        } else if (pitch <= 30) {
            xVal = 1.2;
            zVal = -1.0;
        } else if (pitch <= 50) {
            xVal = 1.0;
            zVal = -0.9;
        } else if (pitch <= 70) {
            xVal = 0.8;
            zVal = -0.8;
        } else if (pitch <= 80) {
            xVal = 0.6;
            zVal = -0.7;
        } else {
            xVal = 0.4;
            zVal = -0.5;
        }

        final long time = player.getWorld().getTime();
        final boolean isDaytime = player.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL 
                && (time < 12786 || time > 23460);

        final org.bukkit.Color color;
        if (isDaytime) {
            final String dyeColor = getElytraDyeColor(chest);
            if (dyeColor != null) {
                color = getBukkitColor(dyeColor);
            } else {
                color = org.bukkit.Color.fromRGB(255, 208, 0);
            }
        } else {
            final int index = (player.getTicksLived() / 2) % 14;
            switch (index) {
                case 0: color = org.bukkit.Color.fromRGB(250, 62, 62); break;
                case 1: color = org.bukkit.Color.fromRGB(250, 112, 62); break;
                case 2: color = org.bukkit.Color.fromRGB(250, 175, 62); break;
                case 3: color = org.bukkit.Color.fromRGB(250, 203, 62); break;
                case 4: color = org.bukkit.Color.fromRGB(250, 222, 62); break;
                case 5: color = org.bukkit.Color.fromRGB(206, 250, 62); break;
                case 6: color = org.bukkit.Color.fromRGB(100, 250, 62); break;
                case 7: color = org.bukkit.Color.fromRGB(62, 228, 250); break;
                case 8: color = org.bukkit.Color.fromRGB(62, 156, 250); break;
                case 9: color = org.bukkit.Color.fromRGB(81, 62, 250); break;
                case 10: color = org.bukkit.Color.fromRGB(165, 62, 250); break;
                case 11: color = org.bukkit.Color.fromRGB(209, 62, 250); break;
                case 12: color = org.bukkit.Color.fromRGB(250, 62, 234); break;
                case 13: default: color = org.bukkit.Color.fromRGB(250, 62, 109); break;
            }
        }

        final org.bukkit.Particle.DustOptions options = new org.bukkit.Particle.DustOptions(color, 1.5f);

        final org.bukkit.Location leftLoc = getLocalLocation(player, -xVal, 0.6, zVal);
        player.getWorld().spawnParticle(org.bukkit.Particle.DUST, leftLoc, 2, 0.07, 0.07, 0.07, 0.0, options);

        final org.bukkit.Location rightLoc = getLocalLocation(player, xVal, 0.6, zVal);
        player.getWorld().spawnParticle(org.bukkit.Particle.DUST, rightLoc, 2, 0.07, 0.07, 0.07, 0.0, options);
    }

    private void spawnVanillaElytraParticles(final Player player, final ItemStack chest) {
        final String dyeColor = getElytraDyeColor(chest);
        if (dyeColor == null) {
            return;
        }

        final float pitch = player.getLocation().getPitch();
        final double xVal;
        final double zVal;
        if (pitch <= 10) {
            xVal = 1.4;
            zVal = -1.1;
        } else if (pitch <= 30) {
            xVal = 1.2;
            zVal = -1.0;
        } else if (pitch <= 50) {
            xVal = 1.0;
            zVal = -0.9;
        } else if (pitch <= 70) {
            xVal = 0.8;
            zVal = -0.8;
        } else if (pitch <= 80) {
            xVal = 0.6;
            zVal = -0.7;
        } else {
            xVal = 0.4;
            zVal = -0.5;
        }

        final org.bukkit.Color color = getBukkitColor(dyeColor);
        final org.bukkit.Particle.DustOptions options = new org.bukkit.Particle.DustOptions(color, 1.5f);

        final org.bukkit.Location leftLoc = getLocalLocation(player, -xVal, 0.6, zVal);
        player.getWorld().spawnParticle(org.bukkit.Particle.DUST, leftLoc, 2, 0.07, 0.07, 0.07, 0.0, options);

        final org.bukkit.Location rightLoc = getLocalLocation(player, xVal, 0.6, zVal);
        player.getWorld().spawnParticle(org.bukkit.Particle.DUST, rightLoc, 2, 0.07, 0.07, 0.07, 0.0, options);
    }

    private org.bukkit.Location getLocalLocation(final Player player, final double localX, final double localY, final double localZ) {
        final org.bukkit.Location loc = player.getLocation();
        final org.bukkit.util.Vector direction = loc.getDirection().normalize();
        
        org.bukkit.util.Vector right = new org.bukkit.util.Vector(-direction.getZ(), 0, direction.getX());
        if (right.lengthSquared() == 0) {
            right = new org.bukkit.util.Vector(1, 0, 0);
        } else {
            right.normalize();
        }
        
        final org.bukkit.util.Vector up = direction.getCrossProduct(right).normalize();
        
        final org.bukkit.util.Vector offset = right.multiply(localX)
                .add(up.multiply(localY))
                .add(direction.multiply(localZ));
                
        return loc.add(offset);
    }
}
