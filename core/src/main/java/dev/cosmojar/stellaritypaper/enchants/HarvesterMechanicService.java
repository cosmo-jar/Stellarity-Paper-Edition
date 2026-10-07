package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Parity-layer technical_soul_harvest:
 * frostburn, piercing_cold, embrittlement, echo,
 * anima_conduit, arctic_wind, cryonics, frost_barrier.
 */
public final class HarvesterMechanicService {

    private static final String HARVESTER_ITEM_ID = "harvester";
    private static final double KILL_MAX_DISTANCE = 6.5D;

    private static final int DAMAGE_EXTRA_MINOR = 3;
    private static final int DAMAGE_EXTRA_SMALL = 6;
    private static final int DAMAGE_EXTRA_BIG = 11;
    private static final int DAMAGE_EXTRA_LARGE = 14;
    private static final int DAMAGE_EXTRA_HUGE = 78;

    private static final Set<EntityType> FREEZE_HURTS_EXTRA_TYPES = EnumSet.of(
            EntityType.BLAZE,
            EntityType.STRIDER,
            EntityType.MAGMA_CUBE
    );
    private static final Set<EntityType> DAMAGE_BUCKET_SMALL = EnumSet.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.ENDERMAN,
            EntityType.HUSK,
            EntityType.STRAY,
            EntityType.PILLAGER,
            EntityType.DROWNED,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.PIGLIN,
            EntityType.VEX,
            EntityType.GUARDIAN
    );
    private static final Set<EntityType> DAMAGE_BUCKET_BIG = EnumSet.of(
            EntityType.WITHER_SKELETON,
            EntityType.BLAZE,
            EntityType.VINDICATOR,
            EntityType.WITCH,
            EntityType.PHANTOM,
            EntityType.SHULKER,
            EntityType.HOGLIN,
            EntityType.CREEPER,
            EntityType.PLAYER
    );
    private static final Set<EntityType> DAMAGE_BUCKET_LARGE = EnumSet.of(
            EntityType.EVOKER,
            EntityType.ILLUSIONER,
            EntityType.PIGLIN_BRUTE,
            EntityType.GHAST,
            EntityType.ZOGLIN,
            EntityType.RAVAGER,
            EntityType.WARDEN
    );
    private static final Set<EntityType> DAMAGE_BUCKET_HUGE = EnumSet.of(
            EntityType.ELDER_GUARDIAN,
            EntityType.WITHER,
            EntityType.GIANT,
            EntityType.ENDER_DRAGON
    );

    private static final String ABILITY_FROSTBURN = "frostburn";
    private static final String ABILITY_PIERCING_COLD = "piercing_cold";
    private static final String ABILITY_EMBRITTLEMENT = "embrittlement";
    private static final String ABILITY_ECHO = "echo";
    private static final String ABILITY_ANIMA_CONDUIT = "anima_conduit";
    private static final String ABILITY_ARCTIC_WIND = "arctic_wind";
    private static final String ABILITY_CRYONICS = "cryonics";
    private static final String ABILITY_FROST_BARRIER = "frost_barrier";

    private final org.bukkit.plugin.Plugin plugin;
    private final EnchantActiveService activeService;
    private final ItemStateRepository itemStateRepository;
    private final HarvesterAbilityCodec abilityCodec;
    private final StellaritySoundService soundService;

    private final NamespacedKey jinxArmorKey;
    private final NamespacedKey jinxToughnessKey;
    private final NamespacedKey jinxKnockbackKey;
    private final NamespacedKey jinxLuckKey;

    private final NamespacedKey cryonicsAttackDamageKey;
    private final NamespacedKey cryonicsKnockbackKey;
    private final NamespacedKey cryonicsFallDamageKey;
    private final NamespacedKey cryonicsJumpKey;
    private final NamespacedKey cryonicsSpeedKey;

    private final Particle.DustOptions arcticWindInnerDust;
    private final Particle.DustOptions arcticWindOuterDust;
    private final Particle.DustOptions echoDust;

    private final Map<UUID, FrostburnState> frostburnStates = new HashMap<>();
    private final Map<UUID, JinxState> jinxStates = new HashMap<>();
    private final Map<UUID, BrittleState> brittleStates = new HashMap<>();
    private final Map<UUID, EchoState> echoStates = new HashMap<>();
    private final Map<UUID, BukkitTask> arcticWindTasks = new HashMap<>();
    private final Map<UUID, CryonicsState> cryonicsStates = new HashMap<>();
    private final Map<UUID, Long> cryonicsCooldownUntilMillis = new HashMap<>();

    private BukkitTask tickTask;

    public HarvesterMechanicService(
            final org.bukkit.plugin.Plugin plugin,
            final EnchantActiveService activeService,
            final ItemStateRepository itemStateRepository,
            final HarvesterAbilityCodec abilityCodec,
            final StellaritySoundService soundService
    ) {
        this.plugin = plugin;
        this.activeService = activeService;
        this.itemStateRepository = itemStateRepository;
        this.abilityCodec = abilityCodec;
        this.soundService = soundService;

        this.jinxArmorKey = new NamespacedKey(plugin, "status.jinx.armor");
        this.jinxToughnessKey = new NamespacedKey(plugin, "status.jinx.armor_toughness");
        this.jinxKnockbackKey = new NamespacedKey(plugin, "status.jinx.knockback_resistance");
        this.jinxLuckKey = new NamespacedKey(plugin, "status.jinx.luck");

        this.cryonicsAttackDamageKey = new NamespacedKey(plugin, "status.cryonics.attack_damage");
        this.cryonicsKnockbackKey = new NamespacedKey(plugin, "status.cryonics.knockback_resistance");
        this.cryonicsFallDamageKey = new NamespacedKey(plugin, "status.cryonics.fall_damage_multiplier");
        this.cryonicsJumpKey = new NamespacedKey(plugin, "status.cryonics.jump_strength");
        this.cryonicsSpeedKey = new NamespacedKey(plugin, "status.cryonics.movement_speed");

        this.arcticWindInnerDust = new Particle.DustOptions(Color.fromRGB(77, 191, 227), 1.0F);
        this.arcticWindOuterDust = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0F);
        this.echoDust = new Particle.DustOptions(Color.fromRGB(179, 142, 243), 1.0F);


    }

    public void onSoulHarvestMeleeHit(final Player attacker, final LivingEntity victim) {
        if (attacker == null || victim == null) {
            return;
        }
        if (!isHarvesterInMainHand(attacker)) {
            return;
        }

        final Set<String> abilities = readHarvesterAbilities(attacker.getInventory().getItemInMainHand());
        if (abilities.contains(ABILITY_FROSTBURN)) {
            applyFrostburn(victim);
        }
        if (abilities.contains(ABILITY_PIERCING_COLD) && ThreadLocalRandom.current().nextDouble() < 0.25D) {
            applyJinx(victim);
        }
        if (abilities.contains(ABILITY_EMBRITTLEMENT)) {
            applyBrittle(victim);
        }
        if (abilities.contains(ABILITY_ECHO)) {
            applyEcho(victim);
        }
    }

    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        final BrittleState brittle = brittleStates.get(target.getUniqueId());
        if (brittle == null || brittle.remainingTicks <= 0) {
            return;
        }
        final double extraDamage = 10.0D * Math.max(1, brittle.level);
        event.setDamage(event.getDamage() + extraDamage);
    }

    public void onEntityDeath(final EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.isOnline()) {
            return;
        }
        if (!isHarvesterInMainHand(killer)) {
            return;
        }
        if (event.getEntity().getLocation().distance(killer.getLocation()) > KILL_MAX_DISTANCE) {
            return;
        }

        final Set<String> abilities = readHarvesterAbilities(killer.getInventory().getItemInMainHand());
        if (abilities.contains(ABILITY_ANIMA_CONDUIT)) {
            applyAnimaConduit(killer, event.getEntityType());
        }
        if (abilities.contains(ABILITY_FROST_BARRIER)) {
            applyFrostBarrier(killer);
        }
    }

    public void onEntityResurrect(final EntityResurrectEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isHarvesterInMainHand(player)) {
            return;
        }
        if (!hasHarvesterAbility(player, ABILITY_CRYONICS)) {
            return;
        }

        final long now = System.currentTimeMillis();
        final long cooldownUntil = cryonicsCooldownUntilMillis.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownUntil || cryonicsStates.containsKey(player.getUniqueId())) {
            return;
        }
        startCryonics(player);
    }

    public void syncPlayerPassives(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (isHarvesterInMainHand(player) && hasHarvesterAbility(player, ABILITY_ARCTIC_WIND)) {
            startArcticWindTask(player);
        } else {
            stopArcticWindTask(player.getUniqueId());
        }
    }

    public void clearPlayer(final Player player) {
        if (player == null) {
            return;
        }
        stopArcticWindTask(player.getUniqueId());
        if (cryonicsStates.remove(player.getUniqueId()) != null) {
            stopCryonics(player, false);
        }
    }

    public void clearAll() {
        for (final UUID uuid : jinxStates.keySet()) {
            final Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                removeJinxModifiers(living);
            }
        }
        for (final UUID uuid : cryonicsStates.keySet()) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeCryonicsModifiers(player);
                clearCryonicsEffects(player);
            }
        }
        for (final BukkitTask task : arcticWindTasks.values()) {
            task.cancel();
        }
        arcticWindTasks.clear();

        frostburnStates.clear();
        brittleStates.clear();
        echoStates.clear();
        jinxStates.clear();
        cryonicsStates.clear();
        cryonicsCooldownUntilMillis.clear();

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void applyFrostburn(final LivingEntity target) {
        frostburnStates.put(target.getUniqueId(), new FrostburnState(140, 0));
        ensureTickTask();
    }

    public void applyJinx(final LivingEntity target) {
        final JinxState previous = jinxStates.get(target.getUniqueId());
        final int level = previous == null ? 1 : previous.level + 1;
        final JinxState state = new JinxState(120, level);
        jinxStates.put(target.getUniqueId(), state);
        applyJinxModifiers(target, level);
        ensureTickTask();
    }

    private void applyBrittle(final LivingEntity target) {
        brittleStates.put(target.getUniqueId(), new BrittleState(120, 1));
        ensureTickTask();
    }

    private void applyEcho(final LivingEntity target) {
        final EchoState existing = echoStates.get(target.getUniqueId());
        final int addedDamage;
        if (existing == null || existing.damage < 1) {
            addedDamage = 15;
        } else if (existing.damage >= 15 && existing.damage <= 34) {
            addedDamage = 10;
        } else {
            addedDamage = 5;
        }
        final int nextDamage = (existing == null ? 0 : existing.damage) + addedDamage;
        echoStates.put(target.getUniqueId(), new EchoState(100, nextDamage));
        ensureTickTask();
    }

    private void applyAnimaConduit(final Player player, final EntityType killedType) {
        final int damageExtra = resolveDamageExtra(killedType);
        final int regenTicks = switch (damageExtra) {
            case DAMAGE_EXTRA_MINOR -> 1;
            case DAMAGE_EXTRA_SMALL -> 2;
            case DAMAGE_EXTRA_BIG -> 2;
            case DAMAGE_EXTRA_LARGE -> 2;
            case DAMAGE_EXTRA_HUGE -> 7;
            default -> 0;
        };
        if (regenTicks > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenTicks, 5, true, false, false));
        }

        final SaturationRoll saturationRoll = saturationFor(damageExtra);
        if (saturationRoll.durationTicks() > 0 && ThreadLocalRandom.current().nextDouble() <= saturationRoll.chance()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, saturationRoll.durationTicks(), 0, true, false, false));
        }

        player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().add(0.0D, 1.2D, 0.0D), 8, 0.3D, 0.35D, 0.3D, 0.01D);
        soundService.play(player, "stellarity:item.harvester.gain_damage", 1.0F, 1.0F, null);
    }

    private void applyFrostBarrier(final Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 7 * 20, 0, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 7 * 20, 0, false, true, true));
        final float pitch = 1.1F + (ThreadLocalRandom.current().nextFloat() * 0.4F);
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.8F, pitch);
    }

    private void ensureTickTask() {
        if (tickTask != null) {
            return;
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        tickFrostburn();
        tickJinx();
        tickBrittle();
        tickEcho();
        tickCryonics();

        if (frostburnStates.isEmpty()
                && jinxStates.isEmpty()
                && brittleStates.isEmpty()
                && echoStates.isEmpty()
                && cryonicsStates.isEmpty()) {
            if (tickTask != null) {
                tickTask.cancel();
                tickTask = null;
            }
        }
    }

    private void tickFrostburn() {
        final Iterator<Map.Entry<UUID, FrostburnState>> iterator = frostburnStates.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, FrostburnState> entry = iterator.next();
            final Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()) {
                iterator.remove();
                continue;
            }

            final FrostburnState state = entry.getValue();
            state.remainingTicks--;
            state.progressTicks++;
            if (state.progressTicks >= 20) {
                state.progressTicks = 0;
                double damage = 10.0D;
                if (FREEZE_HURTS_EXTRA_TYPES.contains(target.getType())) {
                    damage *= 2.0D;
                }
                target.damage(damage);
                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.35D, 0.5D, 0.35D, 0.02D);
            }
            if (state.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private void tickJinx() {
        final Iterator<Map.Entry<UUID, JinxState>> iterator = jinxStates.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, JinxState> entry = iterator.next();
            final Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()) {
                iterator.remove();
                continue;
            }
            final JinxState state = entry.getValue();
            state.remainingTicks--;
            if (state.remainingTicks % 4 == 0) {
                target.getWorld().spawnParticle(Particle.ENTITY_EFFECT, target.getLocation().add(0.0D, 1.2D, 0.0D), 1, 0.3D, 0.5D, 0.3D, 0.0D, Color.fromRGB(150, 0, 255));
            }
            if (state.remainingTicks <= 0) {
                removeJinxModifiers(target);
                iterator.remove();
            }
        }
    }

    private void tickBrittle() {
        final Iterator<Map.Entry<UUID, BrittleState>> iterator = brittleStates.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, BrittleState> entry = iterator.next();
            final Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()) {
                iterator.remove();
                continue;
            }
            final BrittleState state = entry.getValue();
            state.remainingTicks--;
            if (state.remainingTicks % 5 == 0) {
                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0.0D, 1.2D, 0.0D), 1, 0.25D, 0.5D, 0.25D, 0.0D);
            }
            if (state.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private void tickEcho() {
        final Iterator<Map.Entry<UUID, EchoState>> iterator = echoStates.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, EchoState> entry = iterator.next();
            final Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()) {
                iterator.remove();
                continue;
            }
            final EchoState state = entry.getValue();
            state.remainingTicks--;

            target.getWorld().spawnParticle(
                    Particle.DUST,
                    target.getLocation().add(0.0D, 1.2D, 0.0D),
                    1,
                    0.1D,
                    0.1D,
                    0.1D,
                    0.0D,
                    echoDust
            );
            if (state.remainingTicks <= 0) {
                target.damage(state.damage);
                target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0.0D, 1.2D, 0.0D), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                iterator.remove();
            }
        }
    }

    private void startArcticWindTask(final Player player) {
        if (arcticWindTasks.containsKey(player.getUniqueId())) {
            return;
        }
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                stopArcticWindTask(player.getUniqueId());
                return;
            }
            if (!isHarvesterInMainHand(player) || !hasHarvesterAbility(player, ABILITY_ARCTIC_WIND)) {
                stopArcticWindTask(player.getUniqueId());
                return;
            }
            applyArcticWindAura(player);
            if (player.isSneaking()) {
                playArcticWindProjection(player);
            }
        }, 1L, 3L);
        arcticWindTasks.put(player.getUniqueId(), task);
    }

    private void stopArcticWindTask(final UUID playerId) {
        final BukkitTask task = arcticWindTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void applyArcticWindAura(final Player player) {
        for (final Entity nearby : player.getNearbyEntities(9.0D, 4.0D, 9.0D)) {
            if (!(nearby instanceof LivingEntity target)) {
                continue;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            final double distance = target.getLocation().distance(player.getLocation());
            if (distance <= 0.01D || distance > 9.0D) {
                continue;
            }
            if (distance <= 4.0D) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, true, false, false));
            } else if (distance >= 2.51D) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, true, false, false));
            }
        }
    }

    private void playArcticWindProjection(final Player player) {
        final org.bukkit.World world = player.getWorld();
        final org.bukkit.Location origin = player.getLocation().add(0.0D, 0.25D, 0.0D);

        for (int i = 0; i < 36; i++) {
            final double angle = Math.toRadians(i * 10.0D);
            final double x = Math.cos(angle) * 4.0D;
            final double z = Math.sin(angle) * 4.0D;
            world.spawnParticle(Particle.DUST, origin.clone().add(x, 0.0D, z), 1, 0.0D, 0.0D, 0.0D, 0.0D, arcticWindInnerDust);
        }
        for (int i = 0; i < 72; i++) {
            final double angle = Math.toRadians(i * 5.0D);
            final double x = Math.cos(angle) * 9.0D;
            final double z = Math.sin(angle) * 9.0D;
            world.spawnParticle(Particle.DUST, origin.clone().add(x, 0.0D, z), 1, 0.0D, 0.0D, 0.0D, 0.0D, arcticWindOuterDust);
        }
    }

    private void startCryonics(final Player player) {
        cryonicsStates.put(player.getUniqueId(), new CryonicsState(160));
        applyCryonicsStartEffects(player);
        ensureTickTask();
    }

    private void applyCryonicsStartEffects(final Player player) {
        addModifier(player, Attribute.ATTACK_DAMAGE, cryonicsAttackDamageKey, -1.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        addModifier(player, Attribute.KNOCKBACK_RESISTANCE, cryonicsKnockbackKey, 1000.0D, AttributeModifier.Operation.ADD_NUMBER);
        addModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, cryonicsFallDamageKey, -1.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        addModifier(player, Attribute.JUMP_STRENGTH, cryonicsJumpKey, -1.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        addModifier(player, Attribute.MOVEMENT_SPEED, cryonicsSpeedKey, -1.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        applyCryonicsLoopEffects(player);

        player.getWorld().spawnParticle(
                Particle.BLOCK,
                player.getLocation().add(0.0D, 0.5D, 0.0D),
                40,
                0.5D,
                0.7D,
                0.5D,
                0.05D,
                org.bukkit.Material.ICE.createBlockData()
        );
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0F, 0.6F);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1.0F, 0.6F);
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0F, 0.8F);
    }

    private void applyCryonicsLoopEffects(final Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 4, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 250, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 250, true, false, false));
    }

    private void tickCryonics() {
        final Iterator<Map.Entry<UUID, CryonicsState>> iterator = cryonicsStates.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, CryonicsState> entry = iterator.next();
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || player.isDead()) {
                iterator.remove();
                continue;
            }
            final CryonicsState state = entry.getValue();
            state.remainingTicks--;
            state.particleGate++;
            state.effectGate++;

            if (state.effectGate >= 10) {
                state.effectGate = 0;
                applyCryonicsLoopEffects(player);
            }
            if (state.particleGate >= 4) {
                state.particleGate = 0;
                player.getWorld().spawnParticle(
                        Particle.BLOCK,
                        player.getLocation().add(0.0D, 0.7D, 0.0D),
                        6,
                        0.45D,
                        0.7D,
                        0.45D,
                        0.01D,
                        org.bukkit.Material.ICE.createBlockData()
                );
            }
            if (state.remainingTicks <= 0) {
                stopCryonics(player, true);
                iterator.remove();
            }
        }
    }

    private void stopCryonics(final Player player, final boolean explode) {
        removeCryonicsModifiers(player);
        clearCryonicsEffects(player);

        if (explode) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 12 * 20, 3, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 12 * 20, 2, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 45 * 20, 1, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 45 * 20, 0, true, false, false));

            player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation().add(0.0D, 0.5D, 0.0D), 1);
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.6F);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0F, 0.6F);
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0F, 0.8F);

            applyCryonicsExplosionDamage(player);
            cryonicsCooldownUntilMillis.put(player.getUniqueId(), System.currentTimeMillis() + 60_000L);
        }
    }

    private void applyCryonicsExplosionDamage(final Player player) {
        for (final Entity nearby : player.getNearbyEntities(7.0D, 4.0D, 7.0D)) {
            if (!(nearby instanceof LivingEntity target)) {
                continue;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            final double distance = target.getLocation().distance(player.getLocation());
            if (distance <= 4.0D) {
                target.damage(15.0D, player);
            } else if (distance >= 5.01D && distance <= 7.0D) {
                target.damage(4.0D, player);
            }
        }
    }

    private void clearCryonicsEffects(final Player player) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    private void applyJinxModifiers(final LivingEntity entity, final int level) {
        removeJinxModifiers(entity);

        final double armorScale = level * 0.1D;
        final double luckValue = level * 0.5D;
        addModifier(entity, Attribute.ARMOR, jinxArmorKey, -armorScale, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        addModifier(entity, Attribute.ARMOR_TOUGHNESS, jinxToughnessKey, -armorScale, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        addModifier(entity, Attribute.KNOCKBACK_RESISTANCE, jinxKnockbackKey, -armorScale, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        addModifier(entity, Attribute.LUCK, jinxLuckKey, -luckValue, AttributeModifier.Operation.ADD_NUMBER);
    }

    private void removeJinxModifiers(final LivingEntity entity) {
        removeModifier(entity, Attribute.ARMOR, jinxArmorKey);
        removeModifier(entity, Attribute.ARMOR_TOUGHNESS, jinxToughnessKey);
        removeModifier(entity, Attribute.KNOCKBACK_RESISTANCE, jinxKnockbackKey);
        removeModifier(entity, Attribute.LUCK, jinxLuckKey);
    }

    private void removeCryonicsModifiers(final Player player) {
        removeModifier(player, Attribute.ATTACK_DAMAGE, cryonicsAttackDamageKey);
        removeModifier(player, Attribute.KNOCKBACK_RESISTANCE, cryonicsKnockbackKey);
        removeModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, cryonicsFallDamageKey);
        removeModifier(player, Attribute.JUMP_STRENGTH, cryonicsJumpKey);
        removeModifier(player, Attribute.MOVEMENT_SPEED, cryonicsSpeedKey);
    }

    private void addModifier(
            final LivingEntity entity,
            final Attribute attribute,
            final NamespacedKey key,
            final double amount,
            final AttributeModifier.Operation operation
    ) {
        final AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        removeModifier(entity, attribute, key);
        instance.addModifier(new AttributeModifier(key, amount, operation));
    }

    private void removeModifier(
            final LivingEntity entity,
            final Attribute attribute,
            final NamespacedKey key
    ) {
        final AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        for (final AttributeModifier modifier : java.util.List.copyOf(instance.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                instance.removeModifier(modifier);
            }
        }
    }

    private boolean isHarvesterInMainHand(final Player player) {
        final ItemStack item = player.getInventory().getItemInMainHand();
        final String itemId = itemStateRepository.getItemId(item).orElse("");
        if (!HARVESTER_ITEM_ID.equalsIgnoreCase(itemId)) {
            return false;
        }
        final int level = activeService.highestLevel(activeService.fromItem(item, "mainhand"), EnchantIds.TECHNICAL_SOUL_HARVEST);
        return level > 0;
    }

    private boolean hasHarvesterAbility(final Player player, final String abilityId) {
        return readHarvesterAbilities(player.getInventory().getItemInMainHand()).contains(abilityId);
    }

    private Set<String> readHarvesterAbilities(final ItemStack item) {
        return Set.copyOf(abilityCodec.decodeAbilities(itemStateRepository.getHarvesterAbilities(item).orElse("")));
    }

    private int resolveDamageExtra(final EntityType type) {
        if (type == null) {
            return DAMAGE_EXTRA_MINOR;
        }
        if (DAMAGE_BUCKET_HUGE.contains(type)) {
            return DAMAGE_EXTRA_HUGE;
        }
        if (DAMAGE_BUCKET_LARGE.contains(type)) {
            return DAMAGE_EXTRA_LARGE;
        }
        if (DAMAGE_BUCKET_BIG.contains(type)) {
            return DAMAGE_EXTRA_BIG;
        }
        if (DAMAGE_BUCKET_SMALL.contains(type)) {
            return DAMAGE_EXTRA_SMALL;
        }
        return DAMAGE_EXTRA_MINOR;
    }

    private SaturationRoll saturationFor(final int damageExtra) {
        return switch (damageExtra) {
            case DAMAGE_EXTRA_MINOR -> new SaturationRoll(0.15D, 20);
            case DAMAGE_EXTRA_SMALL -> new SaturationRoll(0.20D, 20);
            case DAMAGE_EXTRA_BIG -> new SaturationRoll(0.25D, 40);
            case DAMAGE_EXTRA_LARGE -> new SaturationRoll(0.30D, 40);
            case DAMAGE_EXTRA_HUGE -> new SaturationRoll(1.00D, 120);
            default -> new SaturationRoll(0.0D, 0);
        };
    }

    private record SaturationRoll(double chance, int durationTicks) {
    }

    private static final class FrostburnState {
        private int remainingTicks;
        private int progressTicks;

        private FrostburnState(final int remainingTicks, final int progressTicks) {
            this.remainingTicks = remainingTicks;
            this.progressTicks = progressTicks;
        }
    }

    private static final class JinxState {
        private int remainingTicks;
        private int level;

        private JinxState(final int remainingTicks, final int level) {
            this.remainingTicks = remainingTicks;
            this.level = level;
        }
    }

    private static final class BrittleState {
        private int remainingTicks;
        private int level;

        private BrittleState(final int remainingTicks, final int level) {
            this.remainingTicks = remainingTicks;
            this.level = level;
        }
    }

    private static final class EchoState {
        private int remainingTicks;
        private int damage;

        private EchoState(final int remainingTicks, final int damage) {
            this.remainingTicks = remainingTicks;
            this.damage = damage;
        }
    }

    private static final class CryonicsState {
        private int remainingTicks;
        private int particleGate;
        private int effectGate;

        private CryonicsState(final int remainingTicks) {
            this.remainingTicks = remainingTicks;
            this.particleGate = 0;
            this.effectGate = 0;
        }
    }

    public void spawnHarvesterAmbientParticles(final Player player) {
        if (player == null || !player.isValid() || !isHarvesterInMainHand(player)) {
            return;
        }
        final ItemStack item = player.getInventory().getItemInMainHand();
        final int damageLevel = getHarvesterDamageLevel(item);
        
        final Location loc = player.getLocation();
        final org.bukkit.World world = player.getWorld();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        
        if (damageLevel <= 2) {
            world.spawnParticle(
                    Particle.FALLING_DUST,
                    loc.clone().add(0, 1.0, 0),
                    1,
                    0.35, 0.72, 0.35,
                    0.0,
                    org.bukkit.Bukkit.createBlockData(org.bukkit.Material.ICE)
            );
        } else if (damageLevel <= 5) {
            world.spawnParticle(
                    Particle.FALLING_DUST,
                    loc.clone().add(0, 1.0, 0),
                    1,
                    0.45, 0.72, 0.45,
                    0.0,
                    org.bukkit.Bukkit.createBlockData(org.bukkit.Material.ICE)
            );
            if (random.nextDouble() < 0.20) {
                final Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(244, 102, 204), 1.15F);
                world.spawnParticle(Particle.DUST, loc.clone().add(0, 1.0, 0), 1, 0.45, 0.72, 0.45, 0.0, pinkDust);
            }
        } else if (damageLevel <= 8) {
            world.spawnParticle(
                    Particle.FALLING_DUST,
                    loc.clone().add(0, 1.0, 0),
                    1,
                    0.45, 0.72, 0.45,
                    0.0,
                    org.bukkit.Bukkit.createBlockData(org.bukkit.Material.ICE)
            );
            if (random.nextDouble() < 0.40) {
                final Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(244, 102, 204), 1.15F);
                world.spawnParticle(Particle.DUST, loc.clone().add(0, 1.0, 0), 1, 0.45, 0.72, 0.45, 0.0, pinkDust);
            }
            if (random.nextDouble() < 0.25) {
                world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.5, 0), 1, 0.0, 0.0, 0.0, 1.0);
            }
        } else if (damageLevel <= 11) {
            world.spawnParticle(
                    Particle.FALLING_DUST,
                    loc.clone().add(0, 1.0, 0),
                    1,
                    0.45, 0.72, 0.45,
                    0.0,
                    org.bukkit.Bukkit.createBlockData(org.bukkit.Material.ICE)
            );
            if (random.nextDouble() < 0.60) {
                final Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(244, 102, 204), 1.15F);
                world.spawnParticle(Particle.DUST, loc.clone().add(0, 1.0, 0), 1, 0.45, 0.72, 0.45, 0.0, pinkDust);
            }
            if (random.nextDouble() < 0.50) {
                world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.5, 0), 1, 0.0, 0.0, 0.0, 1.0);
            }
        } else {
            world.spawnParticle(
                    Particle.FALLING_DUST,
                    loc.clone().add(0, 1.0, 0),
                    1,
                    0.45, 0.72, 0.45,
                    0.0,
                    org.bukkit.Bukkit.createBlockData(org.bukkit.Material.ICE)
            );
            if (random.nextDouble() < 0.80) {
                final Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(244, 102, 204), 1.15F);
                world.spawnParticle(Particle.DUST, loc.clone().add(0, 1.0, 0), 1, 0.45, 0.72, 0.45, 0.0, pinkDust);
            }
            if (random.nextDouble() < 0.75) {
                world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.5, 0), 1, 0.0, 0.0, 0.0, 1.0);
            }
            world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.5, 0), 1, 0.0, 0.0, 0.0, 1.0);
        }
    }

    private int getHarvesterDamageLevel(final ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        final ItemMeta meta = item.getItemMeta();
        final NamespacedKey key = new NamespacedKey("stellarity", "harvester.damage");
        if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            return meta.getPersistentDataContainer().getOrDefault(key, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
        }
        return 0;
    }
}
