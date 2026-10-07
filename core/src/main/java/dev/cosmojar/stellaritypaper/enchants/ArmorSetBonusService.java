package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.items.CustomItemMatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ArmorSetBonusService {

    private static final Set<Material> FLORAL_BLOCKS = Set.of(
            Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP,
            Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
            Material.PITCHER_PLANT, Material.PITCHER_CROP, Material.TORCHFLOWER, Material.TORCHFLOWER_CROP,
            Material.VINE, Material.CAVE_VINES, Material.CAVE_VINES_PLANT, Material.GLOW_LICHEN,
            Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES,
            Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES,
            Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
    );

    private final org.bukkit.plugin.Plugin plugin;
    private final CustomItemMatcher itemMatcher;
    private final ItemsConfigService itemsConfig;

    private final NamespacedKey championAttackKey;
    private final NamespacedKey championSweepingKey;
    private final NamespacedKey shulkerBulletKey;
    private final NamespacedKey floralDamageMultKey;
    private final NamespacedKey bloomArrowKey;

    private final Map<UUID, ChampionCombo> championCombos = new HashMap<>();
    private final Map<UUID, Long> hallowedShieldCooldowns = new HashMap<>();
    private final Map<UUID, BloomState> bloomStates = new HashMap<>();
    private final Map<UUID, HallowedState> hallowedStates = new HashMap<>();
    private BukkitTask task;

    public ArmorSetBonusService(
            final org.bukkit.plugin.Plugin plugin,
            final CustomItemMatcher itemMatcher,
            final ItemsConfigService itemsConfig
    ) {
        this.plugin = plugin;
        this.itemMatcher = itemMatcher;
        this.itemsConfig = itemsConfig;
        this.championAttackKey = new NamespacedKey(plugin, "armor.champion.attack");
        this.championSweepingKey = new NamespacedKey(plugin, "armor.champion.sweeping");
        this.shulkerBulletKey = new NamespacedKey(plugin, "armor.shulker_bullet");
        this.floralDamageMultKey = new NamespacedKey(plugin, "armor.floral_damage_mult");
        this.bloomArrowKey = new NamespacedKey(plugin, "armor.bloom_arrow");
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        championCombos.clear();
        hallowedShieldCooldowns.clear();
        bloomStates.clear();
        hallowedStates.clear();
    }

    public boolean isChampionComboEnabled() { return itemsConfig.getArmorConfig().getBoolean("champion.combo", true); }
    public boolean isFloralInvisibilityEnabled() { return itemsConfig.getArmorConfig().getBoolean("floral.invisibility", true); }
    public boolean isFloralArrowsEnabled() { return itemsConfig.getArmorConfig().getBoolean("floral.arrows-modification", true); }
    public boolean isFloralBloomEnabled() { return itemsConfig.getArmorConfig().getBoolean("floral.bloom-effect", true); }
    public boolean isHallowedShieldEnabled() { return itemsConfig.getArmorConfig().getBoolean("hallowed.shield", true); }
    public boolean isShulkerImmunitiesEnabled() { return itemsConfig.getArmorConfig().getBoolean("shulker.immunities", true); }
    public boolean isShulkerBulwarkEnabled() { return itemsConfig.getArmorConfig().getBoolean("shulker.bulwark", true); }
    public boolean isShulkerBulletsEnabled() { return itemsConfig.getArmorConfig().getBoolean("shulker.bullets", true); }

    public void syncPlayer(final Player player) {
        if (player == null) {
            return;
        }
        if (!isFullSetWorn(player, "champion")) {
            clearChampionModifiers(player);
            championCombos.remove(player.getUniqueId());
        }
    }

    public void clear(final Player player) {
        if (player == null) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        clearChampionModifiers(player);
        championCombos.remove(uuid);
        hallowedShieldCooldowns.remove(uuid);
        bloomStates.remove(uuid);
        hallowedStates.remove(uuid);
    }

    private void tick() {
        final long now = System.currentTimeMillis();

        bloomStates.entrySet().removeIf(entry -> {
            final BloomState state = entry.getValue();
            final LivingEntity entity = (LivingEntity) Bukkit.getEntity(state.targetUuid);
            if (entity == null || !entity.isValid()) {
                return true;
            }
            if (now >= state.expireTime) {
                if (isFloralBloomEnabled()) {
                    triggerBloomExplosion(entity, state);
                }
                return true;
            }
            entity.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    entity.getEyeLocation().add(0.0D, 0.4D, 0.0D),
                    3, 0.1D, 0.1D, 0.1D, 0.0D
            );
            return false;
        });

        for (final Player player : Bukkit.getOnlinePlayers()) {
            final UUID uuid = player.getUniqueId();

            if (isFullSetWorn(player, "floral") && isFloralInvisibilityEnabled()) {
                if (player.isSneaking() && isInsideVegetation(player)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, true, false, false));
                }
            }

            if (isFullSetWorn(player, "shulker")) {
                final EntityEquipment equipment = player.getEquipment();
                if (equipment != null) {
                    if (isShulkerImmunitiesEnabled()) {
                        if (isCustomItem(equipment.getLeggings(), "shulker_leggings") && player.hasPotionEffect(PotionEffectType.LEVITATION)) {
                            player.removePotionEffect(PotionEffectType.LEVITATION);
                        }
                        if (isCustomItem(equipment.getChestplate(), "shulker_chestplate") && player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
                            player.removePotionEffect(PotionEffectType.WEAKNESS);
                        }
                    }
                    if (isShulkerBulwarkEnabled() && player.getTicksLived() % 20 == 0) {
                        applyShulkerBulwark(player);
                    }
                }
            }

            final ChampionCombo combo = championCombos.get(uuid);
            if (combo != null) {
                final long expiry = itemsConfig.getArmorConfig().getLong("champion.combo-expiry-ms", 2000L);
                if (now - combo.lastHitTime >= expiry) {
                    clearChampionModifiers(player);
                    championCombos.remove(uuid);
                } else if (combo.combo > 0 && player.getTicksLived() % 2 == 0 && isMoving(player)) {
                    if ("kohara_".equalsIgnoreCase(player.getName())) {
                        player.getWorld().spawnParticle(
                                Particle.CHERRY_LEAVES,
                                player.getLocation().add(0.0D, 1.0D, 0.0D),
                                5, 0.25D, 0.44D, 0.25D, 0.01D
                        );
                    } else {
                        player.getWorld().spawnParticle(
                                Particle.DRAGON_BREATH,
                                player.getLocation().add(0.0D, 1.0D, 0.0D),
                                8, 0.25D, 0.44D, 0.25D, 0.015D
                        );
                    }
                }
            }

            if (isFullSetWorn(player, "hallowed") && isHallowedShieldEnabled()) {
                final long cooldownUntil = hallowedShieldCooldowns.getOrDefault(uuid, 0L);
                final HallowedState state = hallowedStates.computeIfAbsent(uuid, k -> new HallowedState());
                if (!state.shieldActive && now >= cooldownUntil) {
                    state.shieldActive = true;
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0.0D, 1.1D, 0.0D), 15, 0.0D, 0.0D, 0.0D, 0.046D);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.25F, 2.0F);
                }
            } else {
                hallowedStates.remove(uuid);
            }
        }
    }

    private boolean isInsideVegetation(final Player player) {
        final Location feet = player.getLocation();
        final Location eyes = player.getEyeLocation();
        return FLORAL_BLOCKS.contains(feet.getBlock().getType())
                || FLORAL_BLOCKS.contains(eyes.getBlock().getType());
    }

    private boolean isMoving(final Player player) {
        return player.getVelocity().lengthSquared() > 0.001D;
    }

    private void applyShulkerBulwark(final Player player) {
        final List<Entity> nearby = player.getNearbyEntities(5.0D, 5.0D, 5.0D);
        int monsters = 0;
        for (final Entity entity : nearby) {
            if (entity instanceof Monster) {
                monsters++;
            }
        }
        if (monsters >= 10) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 35, 2, true, true));
        } else if (monsters >= 6) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 35, 1, true, true));
        } else if (monsters >= 3) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 35, 0, true, true));
        }
    }

    public void onPlayerMeleeAttack(final Player attacker, final LivingEntity victim) {
        if (attacker == null || victim == null) {
            return;
        }

        if (isFullSetWorn(attacker, "champion") && isChampionComboEnabled()) {
            processChampionCombo(attacker);
        }
    }

    private void processChampionCombo(final Player attacker) {
        final UUID uuid = attacker.getUniqueId();
        final ChampionCombo combo = championCombos.computeIfAbsent(uuid, k -> new ChampionCombo());
        final long now = System.currentTimeMillis();

        clearChampionModifiers(attacker);

        if (now - combo.lastHitTime >= 500) {
            final int oldCombo = combo.combo;
            combo.combo = Math.min(4, combo.combo + 1);
            combo.lastHitTime = now;

            final int effectiveLevel = oldCombo + 1;
            final double dmgPerLevel = itemsConfig.getArmorConfig().getDouble("champion.damage-per-combo-level", 0.06D);
            final double dmgMultiplier = dmgPerLevel * effectiveLevel;
            setModifier(attacker, Attribute.ATTACK_DAMAGE, championAttackKey, dmgMultiplier, AttributeModifier.Operation.ADD_SCALAR);

            if (effectiveLevel >= 5) {
                final double sweepRatio = itemsConfig.getArmorConfig().getDouble("champion.sweeping-ratio-at-max-combo", 0.2D);
                setModifier(attacker, Attribute.SWEEPING_DAMAGE_RATIO, championSweepingKey, sweepRatio, AttributeModifier.Operation.ADD_NUMBER);
            } else {
                removeModifier(attacker, Attribute.SWEEPING_DAMAGE_RATIO, championSweepingKey);
            }

            final float anvilPitch = 0.6F + (0.1F * oldCombo);
            final float blazePitch = 0.5F + (0.1F * oldCombo);
            final float wardenPitch = 0.6F + (0.05F * oldCombo);

            attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5F, anvilPitch);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.25F, blazePitch);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.7F, wardenPitch);
        }
    }

    private void clearChampionModifiers(final Player player) {
        removeModifier(player, Attribute.ATTACK_DAMAGE, championAttackKey);
        removeModifier(player, Attribute.SWEEPING_DAMAGE_RATIO, championSweepingKey);
    }

    public void onPlayerTakeDamage(final EntityDamageEvent event, final Player victim) {
        if (victim == null) {
            return;
        }

        if (isFullSetWorn(victim, "hallowed") && isHallowedShieldEnabled()) {
            final UUID uuid = victim.getUniqueId();
            final long now = System.currentTimeMillis();
            final long cooldownUntil = hallowedShieldCooldowns.getOrDefault(uuid, 0L);

            if (now >= cooldownUntil) {
                event.setDamage(0.0D);
                event.setCancelled(true);

                final HallowedState state = hallowedStates.get(uuid);
                if (state != null) {
                    state.shieldActive = false;
                }

                victim.removePotionEffect(PotionEffectType.RESISTANCE);

                victim.getWorld().spawnParticle(Particle.END_ROD, victim.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.11D, 0.11D, 0.11D, 0.0D);
                victim.getWorld().spawnParticle(Particle.FIREWORK, victim.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.11D, 0.11D, 0.11D, 0.0D);
                victim.getWorld().spawnParticle(Particle.FLASH, victim.getLocation().add(0.0D, 1.0D, 0.0D), 1, 0.0D, 0.0D, 0.0D, 0.0D, org.bukkit.Color.WHITE);
                victim.getWorld().spawnParticle(Particle.POOF, victim.getLocation().add(0.0D, 1.0D, 0.0D), 22, 0.3D, 0.5D, 0.3D, 0.0D);

                victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6F, 1.3F);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.333F, 1.2F);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.88F, 1.1F);

                final int invulnDur = itemsConfig.getArmorConfig().getInt("hallowed.shield-invulnerability-duration-ticks", 40);
                final int speedDur = itemsConfig.getArmorConfig().getInt("hallowed.speed-duration-ticks", 80);
                final int fireDur = itemsConfig.getArmorConfig().getInt("hallowed.fire-resistance-duration-ticks", 120);
                final int waterDur = itemsConfig.getArmorConfig().getInt("hallowed.water-breathing-duration-ticks", 120);

                victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, invulnDur, 99, true, false, false));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDur, 1, true, true));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, fireDur, 0, true, true));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, waterDur, 0, true, true));

                final int shieldCooldownTicks = itemsConfig.getArmorConfig().getInt("hallowed.shield-cooldown-ticks", 540);
                hallowedShieldCooldowns.put(uuid, now + shieldCooldownTicks * 50L);
                return;
            }
        }

        if (event instanceof EntityDamageByEntityEvent subEvent && isFullSetWorn(victim, "shulker") && isShulkerBulletsEnabled()) {
            final double bulletChance = itemsConfig.getArmorConfig().getDouble("shulker.bullet-chance", 0.50D);
            if (ThreadLocalRandom.current().nextDouble() < bulletChance) {
                final int count = ThreadLocalRandom.current().nextInt(1, 4);
                final Entity damager = subEvent.getDamager();
                LivingEntity bulletTarget = null;
                if (damager instanceof LivingEntity living && living.isValid() && !(living instanceof Player)) {
                    bulletTarget = living;
                } else {
                    for (final Entity entity : victim.getNearbyEntities(8.0D, 8.0D, 8.0D)) {
                        if (entity instanceof Monster monster && monster.isValid()) {
                            bulletTarget = monster;
                            break;
                        }
                    }
                }
                if (bulletTarget != null) {
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1.0F, 1.0F);
                    for (int i = 0; i < count; i++) {
                        final ShulkerBullet bullet = (ShulkerBullet) victim.getWorld().spawnEntity(victim.getEyeLocation(), EntityType.SHULKER_BULLET);
                        bullet.setShooter(victim);
                        bullet.setTarget(bulletTarget);
                        bullet.getPersistentDataContainer().set(shulkerBulletKey, PersistentDataType.BOOLEAN, true);
                    }
                }
            }
        }
    }

    public void onProjectileLaunch(final org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getShooter() instanceof Player shooter)) {
            return;
        }

        final EntityEquipment eq = shooter.getEquipment();
        if (eq == null) {
            return;
        }

        double damageMult = 1.0D;
        boolean speedBoost = false;

        if (isCustomItem(eq.getChestplate(), "floral_chestplate") && isFloralArrowsEnabled()) {
            speedBoost = true;
            damageMult -= 0.20D;
        }
        if (isCustomItem(eq.getLeggings(), "floral_leggings") && isFloralArrowsEnabled()) {
            damageMult += 0.10D;
        }

        if (speedBoost) {
            arrow.setVelocity(arrow.getVelocity().multiply(1.25D));
        }

        if (damageMult != 1.0D) {
            arrow.getPersistentDataContainer().set(floralDamageMultKey, PersistentDataType.DOUBLE, damageMult);
        }

        if (isFullSetWorn(shooter, "floral") && isFloralBloomEnabled()) {
            arrow.getPersistentDataContainer().set(bloomArrowKey, PersistentDataType.STRING, shooter.getUniqueId().toString());
        }
    }

    public void onProjectileHit(final org.bukkit.event.entity.EntityDamageByEntityEvent event, final Projectile projectile, final LivingEntity victim) {
        if (projectile == null || victim == null) {
            return;
        }

        final Double mult = projectile.getPersistentDataContainer().get(floralDamageMultKey, PersistentDataType.DOUBLE);
        if (mult != null) {
            event.setDamage(event.getDamage() * mult);
        }

        if (isFloralBloomEnabled()) {
            final String shooterUuidStr = projectile.getPersistentDataContainer().get(bloomArrowKey, PersistentDataType.STRING);
            if (shooterUuidStr != null) {
                try {
                    final UUID shooterUuid = UUID.fromString(shooterUuidStr);
                    applyBloom(victim, shooterUuid);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (projectile instanceof ShulkerBullet && isShulkerBulletsEnabled()) {
            if (Boolean.TRUE.equals(projectile.getPersistentDataContainer().get(shulkerBulletKey, PersistentDataType.BOOLEAN))) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, ThreadLocalRandom.current().nextInt(60, 101), 0));
            }
        }
    }

    private void applyBloom(final LivingEntity victim, final UUID shooterUuid) {
        final UUID targetUuid = victim.getUniqueId();
        final long now = System.currentTimeMillis();
        final BloomState state = bloomStates.get(targetUuid);

        if (state == null) {
            final BloomState newState = new BloomState(targetUuid, shooterUuid, 4.0D, now + 3500L);
            bloomStates.put(targetUuid, newState);
            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 0.9F, 1.2F);
            victim.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, victim.getEyeLocation().add(0.0D, 0.4D, 0.0D), 8, 0.2D, 0.2D, 0.2D, 0.0D);
        } else {
            if (now - state.lastHitTime >= 800) {
                state.currentDamage = Math.min(50.0D, state.currentDamage + 2.0D);
                state.expireTime = Math.min(now + 10000L, state.expireTime + 1500L);
                state.lastHitTime = now;

                victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 1.0F, 1.5F);
                victim.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, victim.getEyeLocation().add(0.0D, 0.4D, 0.0D), 12, 0.25D, 0.25D, 0.25D, 0.0D);

                if (state.currentDamage >= 50.0D) {
                    triggerBloomExplosion(victim, state);
                    bloomStates.remove(targetUuid);
                }
            }
        }
    }

    private void triggerBloomExplosion(final LivingEntity victim, final BloomState state) {
        final Player shooter = Bukkit.getPlayer(state.attackerUuid);
        victim.damage(state.currentDamage, shooter);

        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.0F);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.2F);
        victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0.0D, 1.0D, 0.0D), 3, 0.3D, 0.3D, 0.3D, 0.0D);
        victim.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, victim.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.4D, 0.4D, 0.4D, 0.05D);
    }

    private boolean isFullSetWorn(final Player player, final String setName) {
        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return false;
        }
        return isCustomItem(equipment.getHelmet(), setName + "_helmet")
                && isCustomItem(equipment.getChestplate(), setName + "_chestplate")
                && isCustomItem(equipment.getLeggings(), setName + "_leggings")
                && isCustomItem(equipment.getBoots(), setName + "_boots");
    }

    private boolean isCustomItem(final ItemStack item, final String expectedId) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return itemMatcher.isCustom(item, expectedId);
    }

    private void setModifier(
            final Player player,
            final Attribute attribute,
            final NamespacedKey key,
            final double amount,
            final AttributeModifier.Operation operation
    ) {
        final AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        for (final AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                instance.removeModifier(modifier);
            }
        }
        if (Math.abs(amount) < 1.0E-9D) {
            return;
        }
        instance.addModifier(new AttributeModifier(key, amount, operation));
    }

    private void removeModifier(final Player player, final Attribute attribute, final NamespacedKey key) {
        final AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        for (final AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                instance.removeModifier(modifier);
            }
        }
    }

    private static final class ChampionCombo {
        private int combo = 0;
        private long lastHitTime = 0;
    }

    private static final class BloomState {
        private final UUID targetUuid;
        private final UUID attackerUuid;
        private double currentDamage;
        private long expireTime;
        private long lastHitTime;

        public BloomState(final UUID targetUuid, final UUID attackerUuid, final double currentDamage, final long expireTime) {
            this.targetUuid = targetUuid;
            this.attackerUuid = attackerUuid;
            this.currentDamage = currentDamage;
            this.expireTime = expireTime;
            this.lastHitTime = System.currentTimeMillis();
        }
    }

    private static final class HallowedState {
        private boolean shieldActive = false;
    }
}
