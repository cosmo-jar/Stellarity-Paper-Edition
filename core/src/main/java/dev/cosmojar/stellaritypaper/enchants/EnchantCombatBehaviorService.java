package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Боевые event-driven чары
 */
public final class EnchantCombatBehaviorService {

    private final EnchantActiveService activeService;
    private final EnchantDataCodec codec;
    private final ItemStateRepository itemStateRepository;
    private final CustomStatusEffectService customStatusEffectService;
    private final KaleidoscopeMechanicService kaleidoscopeMechanicService;
    private final DragonbladeMechanicService dragonbladeMechanicService;
    private final HarvesterMechanicService harvesterMechanicService;
    private final StellaritySoundService soundService;
    private final NamespacedKey projectileEnchantsKey;

    public EnchantCombatBehaviorService(
            final org.bukkit.plugin.Plugin plugin,
            final EnchantActiveService activeService,
            final EnchantDataCodec codec,
            final ItemStateRepository itemStateRepository,
            final CustomStatusEffectService customStatusEffectService,
            final KaleidoscopeMechanicService kaleidoscopeMechanicService,
            final DragonbladeMechanicService dragonbladeMechanicService,
            final HarvesterMechanicService harvesterMechanicService,
            final StellaritySoundService soundService
    ) {
        this.projectileEnchantsKey = new NamespacedKey(plugin, "projectile_enchants");
        this.activeService = activeService;
        this.codec = codec;
        this.itemStateRepository = itemStateRepository;
        this.customStatusEffectService = customStatusEffectService;
        this.kaleidoscopeMechanicService = kaleidoscopeMechanicService;
        this.dragonbladeMechanicService = dragonbladeMechanicService;
        this.harvesterMechanicService = harvesterMechanicService;
        this.soundService = soundService;
    }

    public void onShootBow(final EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }

        final List<EnchantActiveService.ActiveEnchant> active = activeService.fromPlayerEquipment(player);
        final List<EnchantInstance> projectileEnchants = new ArrayList<>();
        addIfPresent(projectileEnchants, active, EnchantIds.LEVITATION_SHOT);
        addIfPresent(projectileEnchants, active, EnchantIds.VOID_SHOT);
        addIfPresent(projectileEnchants, active, EnchantIds.TECHNICAL_INFERNAL_INFUSION);

        if (projectileEnchants.isEmpty()) {
            return;
        }
        projectile.getPersistentDataContainer().set(
                projectileEnchantsKey,
                PersistentDataType.STRING,
                codec.encode(projectileEnchants)
        );
    }

    public void onProjectileHit(final ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(event.getHitEntity() instanceof LivingEntity victim)) {
            return;
        }

        final String raw = arrow.getPersistentDataContainer().get(
                projectileEnchantsKey,
                PersistentDataType.STRING
        );
        final List<EnchantInstance> enchants = codec.decode(raw == null ? "" : raw);
        if (enchants.isEmpty()) {
            return;
        }

        final Player shooter = arrow.getShooter() instanceof Player p ? p : null;
        for (final EnchantInstance enchant : enchants) {
            switch (enchant.id()) {
                case EnchantIds.LEVITATION_SHOT -> applyLevitationShot(victim, enchant.level());
                case EnchantIds.VOID_SHOT -> applyVoidShot(victim, shooter, arrow);
                case EnchantIds.TECHNICAL_INFERNAL_INFUSION ->
                        customStatusEffectService.applyPrismaticInferno(victim, 99, shooter == null ? arrow : shooter);
                default -> {
                }
            }
        }
    }

    public void onDamageByEntity(final EntityDamageByEntityEvent event) {
        final Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        final Entity target = event.getEntity();
        if (!(target instanceof LivingEntity victim)) {
            return;
        }
        final List<EnchantActiveService.ActiveEnchant> active = activeService.fromPlayerEquipment(attacker);

        applyAmbush(event, attacker, target, activeService.highestLevel(active, EnchantIds.AMBUSH));
        applyCriticalStrike(event, activeService.highestLevel(active, EnchantIds.CRITICAL_STRIKE));
        applyVoidStrike(event, victim, activeService.highestLevel(active, EnchantIds.VOID_STRIKE));
        applyInfernalInfusion(event, attacker, victim, activeService.highestLevel(active, EnchantIds.TECHNICAL_INFERNAL_INFUSION));
        final int draconicLevel = activeService.highestLevel(active, EnchantIds.TECHNICAL_DRACONIC);
        if (draconicLevel > 0) {
            dragonbladeMechanicService.onDraconicMeleeHit(event, attacker, victim);
        }
        final int daybrokenLevel = activeService.highestLevel(active, EnchantIds.TECHNICAL_DAYBROKEN);
        if (daybrokenLevel > 0) {
            kaleidoscopeMechanicService.onDaybrokenMeleeHit(event, attacker, victim);
        }
        final int soulHarvestLevel = activeService.highestLevel(active, EnchantIds.TECHNICAL_SOUL_HARVEST);
        if (soulHarvestLevel > 0) {
            harvesterMechanicService.onSoulHarvestMeleeHit(attacker, victim);
        }
    }

    private void applyAmbush(final EntityDamageByEntityEvent event, final Player attacker, final Entity target, final int level) {
        if (level <= 0 || !(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget instanceof Player) {
            return;
        }
        if (livingTarget instanceof Mob mob && !mob.hasAI()) {
            return;
        }
        if (livingTarget instanceof Mob mob && attacker.equals(mob.getTarget())) {
            return;
        }

        final double multiplier = switch (Math.min(level, 3)) {
            case 1 -> 1.5D;
            case 2 -> 2.0D;
            default -> 2.5D;
        };
        event.setDamage(event.getDamage() * multiplier);
        final String soundKey = switch (Math.min(level, 3)) {
            case 1 -> "stellarity:enchantment.ambush.level_1";
            case 2 -> "stellarity:enchantment.ambush.level_2";
            default -> "stellarity:enchantment.ambush.level_3";
        };
        soundService.play(attacker, soundKey, 1.0F, 1.0F, null);
        attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 0.75F, 1.0F);
    }

    private void applyCriticalStrike(final EntityDamageByEntityEvent event, final int level) {
        if (level <= 0 || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        final double chance = 0.1D + ((Math.min(level, 3) - 1) * 0.1D);
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        event.setDamage(event.getDamage() * 2.0D);
        attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0.0D, 1.0D, 0.0D), 16, 0.25D, 0.25D, 0.25D, 0.1D);
        soundService.play(attacker, "stellarity:enchantment.critical_strike.crit", 1.0F, 1.0F, null);
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
    }

    private void applyVoidStrike(final EntityDamageByEntityEvent event, final LivingEntity victim, final int level) {
        if (level <= 0 || !(event.getDamager() instanceof Player)) {
            return;
        }
        customStatusEffectService.applyVoided(victim, 160, level);
    }

    private void applyInfernalInfusion(
            final EntityDamageByEntityEvent event,
            final Player attacker,
            final LivingEntity victim,
            final int level
    ) {
        if (level <= 0) {
            return;
        }
        final boolean ranged = event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player;
        final boolean melee = event.getDamager() instanceof Player;
        if (!ranged && !melee) {
            return;
        }
        customStatusEffectService.applyPrismaticInferno(victim, 99, attacker);
    }

    private void applyLevitationShot(final LivingEntity victim, final int level) {
        final double numerator = 15.0D + ((Math.max(level, 1) - 1) * 10.0D);
        final double denominator = 100.0D + ((Math.max(level, 1) - 1) * 12.0D);
        if (ThreadLocalRandom.current().nextDouble() > (numerator / denominator)) {
            return;
        }
        final int min = (int) Math.floor(4.0D + ((Math.max(level, 1) - 1) * 0.5D));
        final int max = (int) Math.floor(7.0D + ((Math.max(level, 1) - 1) * 0.5D));
        final int seconds = ThreadLocalRandom.current().nextInt(min, Math.max(min + 1, max + 1));
        victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION, seconds * 20, 0));
    }

    private void applyVoidShot(final LivingEntity victim, final Player shooter, final AbstractArrow arrow) {
        customStatusEffectService.applyVoided(victim, 160, 1);
        if (shooter == null) {
            return;
        }

        final ItemStack mainHand = shooter.getInventory().getItemInMainHand();
        final String itemId = itemStateRepository.getItemId(mainHand).orElse("");
        if (!"call_of_the_void".equalsIgnoreCase(itemId)) {
            return;
        }

        for (final Entity nearby : victim.getNearbyEntities(2.5D, 2.0D, 2.5D)) {
            if (!(nearby instanceof LivingEntity living) || living.getUniqueId().equals(victim.getUniqueId())) {
                continue;
            }
            if (living.getUniqueId().equals(shooter.getUniqueId())) {
                continue;
            }
            living.damage(2.0D, shooter);
        }
        arrow.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, victim.getLocation().add(0.0D, 1.0D, 0.0D), 16, 0.3D, 0.3D, 0.3D, 0.05D);
    }

    private void addIfPresent(
            final List<EnchantInstance> output,
            final List<EnchantActiveService.ActiveEnchant> active,
            final String enchantId
    ) {
        final int level = activeService.highestLevel(active, enchantId);
        if (level > 0) {
            output.add(new EnchantInstance(enchantId, level));
        }
    }

    private Player resolveAttacker(final Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
