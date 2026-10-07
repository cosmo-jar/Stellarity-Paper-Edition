package dev.cosmojar.stellaritypaper.mobs.boss;

import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import dev.cosmojar.stellaritypaper.integration.DamageSafetyHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс-контроллер, реализующий Empress of Light.
 * Управляет поведением, фазами, перемещением через невидимый Vindicator anchor и всеми типами атак.
 */
public final class EmpressOfLight implements StellarityBoss {

    private final Plugin plugin;
    private final Vindicator anchor;
    public static final class BoneSpec {
        public final String name;
        public final float tx, ty, tz;
        public final float sx, sy, sz;

        public BoneSpec(String name, float tx, float ty, float tz, float sx, float sy, float sz) {
            this.name = name;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
        }
    }

    private static final BoneSpec[] BONE_SPECS = new BoneSpec[] {
        new BoneSpec("bone7", -0.125f, 0.75f, 0.0f, 1.0f, 1.0f, 1.0f),
        new BoneSpec("bone8", 0.125f, 0.75f, 0.0f, 1.0f, 1.0f, 1.0f),
        new BoneSpec("bone3", 0.3562f, 1.375f, 0.0f, 1.0f, 1.0f, 1.0f),
        new BoneSpec("bone4", -0.3688f, 1.375f, 0.0f, 1.0f, 1.0f, 1.0f),
        new BoneSpec("bone10", 0.0f, 1.5f, 0.0f, 1.0f, 1.0f, 1.0f),
        new BoneSpec("bone9", 0.0562f, 0.5875f, 0.1875f, 1.0f, 1.0f, 1.0f),
        new BoneSpec("bone1", 0.0f, 1.25f, -0.4375f, 2.375f, 2.375f, 2.375f),
        new BoneSpec("bone", 0.0f, 1.1563f, -0.375f, 2.375f, 2.375f, 2.375f)
    };

    private final java.util.List<ItemDisplay> bones = new java.util.ArrayList<>(8);
    private boolean hasEnteredPhase2 = false;
    private final ItemDisplay display;
    private final org.bukkit.entity.Interaction hitbox;
    private final BossManager bossManager;
    private final Location altarLocation;

    private final double maxHealth;
    private final double baseDamage;
    private final double speed;
    private double currentHealth;

    // Состояние босса
    private Location currentPosition;
    private Vector velocity = new Vector(0, 0, 0);
    private float visualYaw = 0.0f;
    private int animationTick = 0;

    private int phase = 1;
    private int attackCycle = 1;
    private int attackTimer = 0;
    private boolean isDaytime = false;
    private Player currentTarget = null;
    private int ticksWithoutPlayers = 0;

    private final List<PrismaticBolt> activeBolts = new ArrayList<>();
    private final List<EtherealLance> activeLances = new ArrayList<>();

    private boolean isDashing = false;
    private int dashTicks = 0;
    private Vector dashVector = null;
    private boolean isSpawning = true;
    private boolean isDying = false;
    private int deathTicks = 0;

    // Stage 2 Healing (Spirit Shield & Stagger)
    private boolean isSpiritShieldActive = false;
    private double spiritShieldHealth = 0.0;
    private int healingUses = 0;
    private int healingCooldownTicks = 0;
    private int healingTimerTicks = 0;

    public enum Variant {
        NIGHT,
        DAYTIME,
        RADIANT
    }

    private final Variant variant;

    public EmpressOfLight(
            final Plugin plugin,
            final Location spawnLoc,
            final BossManager bossManager,
            final Location altarLocation,
            final Variant variant
    ) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.altarLocation = altarLocation != null ? altarLocation.clone() : null;

        final boolean isOverworld = spawnLoc.getWorld() != null && spawnLoc.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL;
        final long time = spawnLoc.getWorld() != null ? spawnLoc.getWorld().getTime() : 0L;
        final boolean dayCheck = isOverworld && (time < 12786 || time > 23460);

        if (variant != null) {
            this.variant = variant;
            this.isDaytime = (variant == Variant.DAYTIME);
        } else {
            this.variant = dayCheck ? Variant.DAYTIME : Variant.NIGHT;
            this.isDaytime = dayCheck;
        }

        final org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        switch (this.variant) {
            case RADIANT -> this.maxHealth = config.getDouble("boss.empress_of_light.max-health-radiant", 850.0);
            case DAYTIME -> this.maxHealth = config.getDouble("boss.empress_of_light.max-health-daytime", 800.0);
            case NIGHT -> this.maxHealth = config.getDouble("boss.empress_of_light.max-health-night", 750.0);
            default -> this.maxHealth = 750.0;
        }
        this.currentHealth = this.maxHealth;
        this.baseDamage = config.getDouble("boss.empress_of_light.base-damage", 10.0);
        this.speed = config.getDouble("boss.empress_of_light.speed", 0.15);

        this.currentPosition = spawnLoc.clone();
        this.visualYaw = spawnLoc.getYaw();

        // 1. Невидимый Vindicator без ИИ как серверный якорь для хитбокса и HP
        this.anchor = spawnLoc.getWorld().spawn(spawnLoc, Vindicator.class, v -> {
            v.setAI(false);
            v.setGravity(false);
            v.setInvisible(true);
            v.setSilent(true);
            v.setCollidable(false);
            v.setVisualFire(TriState.FALSE);
            v.setCanPickupItems(false);
            v.setRemoveWhenFarAway(false);
            v.setPersistent(false);
            final double bufferHealth = Math.min(1024.0, Math.max(100.0, this.maxHealth));
            v.getAttribute(Attribute.MAX_HEALTH).setBaseValue(bufferHealth);
            v.setHealth(bufferHealth);
            v.getEquipment().clear();

            if (isRadiant()) {
                v.setCustomName("§c§lRadiant Empress of Light");
                v.getPersistentDataContainer().set(new NamespacedKey(plugin, "boss_type"), org.bukkit.persistence.PersistentDataType.STRING, "radiant_empress_of_light");
                v.getScoreboardTags().add("stellarity.radiant_empress_of_light");
                v.getScoreboardTags().add("stellarity.empress_of_light.radiant");
            } else {
                v.setCustomName(isDaytime ? "§eEmpress of Light" : "§dEmpress of Light");
                v.getPersistentDataContainer().set(new NamespacedKey(plugin, "boss_type"), org.bukkit.persistence.PersistentDataType.STRING, "empress_of_light");
            }
            v.setCustomNameVisible(false);
            v.getScoreboardTags().add("stellarity.empress_of_light");
        });

        // 2. Модель Императрицы (8 костей Animated Java)
        ItemDisplay mainBody = null;
        final String initialVariant = getVariantModelString(false);

        for (final BoneSpec spec : BONE_SPECS) {
            final ItemDisplay bone = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
                d.setInvulnerable(true);
                d.setGravity(false);
                d.setPersistent(false);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
                d.setTeleportDuration(0);
                d.setInterpolationDuration(1);
                d.addScoreboardTag("stellarity.empress_of_light.bone." + spec.name);
                d.addScoreboardTag("stellarity.empress_of_light.model");

                final ItemStack item = new ItemStack(Material.WHITE_DYE);
                final ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setItemModel(NamespacedKey.fromString("stellarity:blueprint/util/animated_java/eol/" + spec.name));
                    final org.bukkit.inventory.meta.components.CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
                    cmd.setStrings(java.util.List.of(initialVariant));
                    meta.setCustomModelDataComponent(cmd);
                    item.setItemMeta(meta);
                }
                d.setItemStack(item);

                final Transformation trans = new Transformation(
                    new org.joml.Vector3f(spec.tx, spec.ty, spec.tz),
                    new org.joml.Quaternionf().rotationY((float) Math.PI),
                    new org.joml.Vector3f(spec.sx, spec.sy, spec.sz),
                    new org.joml.Quaternionf()
                );
                d.setTransformation(trans);
            });

            this.bones.add(bone);
            this.anchor.addPassenger(bone);

            if ("bone9".equals(spec.name)) {
                mainBody = bone;
            }
        }

        this.display = mainBody != null ? mainBody : (bones.isEmpty() ? null : bones.get(0));
        if (this.display != null) {
            this.display.addScoreboardTag("stellarity.empress_of_light.display");
        }

        // 5. Отдельный логический хитбокс Interaction (2.5 x 3.0) — НЕ пассажир!
        this.hitbox = spawnLoc.getWorld().spawn(spawnLoc, org.bukkit.entity.Interaction.class, i -> {
            i.setInteractionWidth(2.5f);
            i.setInteractionHeight(3.0f);
            i.setResponsive(true);
            i.setPersistent(false);
            i.getPersistentDataContainer().set(new NamespacedKey(plugin, "boss_uuid"), org.bukkit.persistence.PersistentDataType.STRING, anchor.getUniqueId().toString());
            i.getScoreboardTags().add("stellarity.empress_of_light.hitbox");
        });

        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5F, 1.2F);

        bossManager.registerBoss(this);
    }

    @Override
    public LivingEntity getBaseEntity() {
        return anchor;
    }

    @Override
    public ItemDisplay getDisplayEntity() {
        return display;
    }

    public boolean isSpawning() {
        return isSpawning;
    }

    public void setSpawning(final boolean spawning) {
        this.isSpawning = spawning;
    }

    public boolean isDying() {
        return isDying;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    @Override
    public Location getAltarLocation() {
        return altarLocation;
    }

    public void syncHealthAfterHit(final double amount, final org.bukkit.entity.Entity damager) {
        if (isDying || isSpawning) return;

        if (isSpiritShieldActive) {
            spiritShieldHealth -= amount;
            final Location loc = currentPosition != null ? currentPosition : anchor.getLocation();
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.2F, 1.4F);
            loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.1);

            if (spiritShieldHealth <= 0.0) {
                // Boss is STAGGERED! Spirit shield breaks, healing interrupted
                isSpiritShieldActive = false;
                spiritShieldHealth = 0.0;
                healingTimerTicks = 0;
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.8F, 0.8F);
                loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, 1.8F, 0.9F);
                spawnParticleSafe(loc.getWorld(), Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
                // Stun boss for 2 seconds (40 ticks)
                attackTimer = -40;
                velocity.zero();
            }
            return;
        }

        this.currentHealth -= amount;

        final Location loc = currentPosition != null ? currentPosition : anchor.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ALLAY_HURT, org.bukkit.SoundCategory.HOSTILE, 1.0F, 0.75F);
        loc.getWorld().playSound(loc, Sound.ENTITY_VEX_HURT, org.bukkit.SoundCategory.HOSTILE, 1.2F, 1.1F);
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0.0, 1.0, 0.0), 10, 0.3, 0.5, 0.3, 0.2);

        flashHurt();

        if (this.currentHealth <= 0.0) {
            this.currentHealth = 0.0;
            anchor.setInvulnerable(true);
            startDeathAnimation();
        } else {
            final var maxAttr = anchor.getAttribute(Attribute.MAX_HEALTH);
            final double maxHp = maxAttr != null ? maxAttr.getValue() : 1000.0;
            anchor.setHealth(Math.min(maxHp, 1024.0));
        }
    }

    public String getVariantModelString(final boolean isHurt) {
        if (isRadiant()) {
            return isHurt ? "radiant_hurt" : "radiant";
        } else if (isDaytime) {
            return isHurt ? "daylight_hurt" : "daylight";
        } else {
            final boolean isPhase2 = currentHealth < (maxHealth * 0.5) || hasEnteredPhase2;
            if (isPhase2) {
                return isHurt ? "default_phase2_hurt" : "default_phase2";
            } else {
                return isHurt ? "default_hurt" : "default";
            }
        }
    }

    public void updateBoneVariants() {
        final String variantString = getVariantModelString(false);
        for (final ItemDisplay bone : bones) {
            if (bone != null && bone.isValid()) {
                final ItemStack stack = bone.getItemStack();
                if (stack != null) {
                    final ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        final org.bukkit.inventory.meta.components.CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
                        cmd.setStrings(java.util.List.of(variantString));
                        meta.setCustomModelDataComponent(cmd);
                        stack.setItemMeta(meta);
                        bone.setItemStack(stack);
                    }
                }
            }
        }
    }

    public void flashHurt() {
        if (bones.isEmpty()) return;
        final String hurtVariant = getVariantModelString(true);

        for (final ItemDisplay bone : bones) {
            if (bone != null && bone.isValid()) {
                final ItemStack stack = bone.getItemStack();
                if (stack != null) {
                    final ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        final org.bukkit.inventory.meta.components.CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
                        cmd.setStrings(java.util.List.of(hurtVariant));
                        meta.setCustomModelDataComponent(cmd);
                        stack.setItemMeta(meta);
                        bone.setItemStack(stack);
                    }
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateBoneVariants();
        }, 3L);
    }

    public void damage(final double amount, final org.bukkit.entity.Entity damager) {
        syncHealthAfterHit(amount, damager);
    }

    public void startDeathAnimation() {
        if (isDying) return;
        this.isDying = true;
        this.deathTicks = 0;
        this.isDashing = false;
        this.dashVector = null;
        this.velocity.zero();

        for (final PrismaticBolt bolt : activeBolts) {
            bolt.cleanup();
        }
        activeBolts.clear();
        for (final EtherealLance lance : activeLances) {
            lance.cleanup();
        }
        activeLances.clear();

        final Location loc = currentPosition != null ? currentPosition : anchor.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ALLAY_DEATH, org.bukkit.SoundCategory.HOSTILE, 1.5F, 1.15F);
        loc.getWorld().playSound(loc, Sound.ENTITY_VEX_DEATH, org.bukkit.SoundCategory.HOSTILE, 1.5F, 1.15F);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, org.bukkit.SoundCategory.HOSTILE, 0.2F, 0.0F);
        loc.getWorld().playSound(loc, Sound.ENTITY_VEX_DEATH, org.bukkit.SoundCategory.HOSTILE, 2.2F, 1.0F);
    }

    @Override
    public boolean isAlive() {
        if (isDying) {
            return deathTicks < 60 && display != null && display.isValid();
        }
        return anchor != null && anchor.isValid() && !anchor.isDead() && display != null && display.isValid();
    }

    @Override
    public void tick() {
        if (!isAlive()) {
            cleanup();
            return;
        }

        if (isDying) {
            deathTicks++;
            tickWings();

            currentPosition.add(0.0, 0.04, 0.0);
            currentPosition.setYaw(visualYaw);
            currentPosition.setPitch(0.0f);
            anchor.teleport(currentPosition);
            hitbox.teleport(currentPosition.clone().subtract(0.0, 0.5, 0.0));

            spawnAuroraParticles(currentPosition.clone().add(0.0, 1.0, 0.0), isDaytime);

            currentPosition.getWorld().spawnParticle(Particle.END_ROD, currentPosition.clone().add(0.0, 0.5, 0.0), 3, 0.2, 0.2, 0.2, 0.05);

            if (deathTicks == 20 || deathTicks == 40 || deathTicks == 50 || deathTicks == 55) {
                final Location flashLoc = currentPosition.clone().add(0.0, 1.0, 0.0);
                currentPosition.getWorld().spawnParticle(Particle.EXPLOSION, flashLoc, 1, 0.0, 0.0, 0.0, 0.0);
                currentPosition.getWorld().spawnParticle(Particle.END_ROD, flashLoc, 20, 0.0, 0.0, 0.0, 0.1);
                currentPosition.getWorld().playSound(flashLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, org.bukkit.SoundCategory.HOSTILE, 1.0F, 1.3F);
                currentPosition.getWorld().playSound(flashLoc, Sound.BLOCK_CHAIN_BREAK, org.bukkit.SoundCategory.HOSTILE, 1.0F, 0.0F);
            }

            if (deathTicks >= 60) {
                final Location finalLoc = currentPosition.clone().add(0.0, 1.0, 0.0);
                spawnParticleSafe(finalLoc.getWorld(), Particle.EXPLOSION_EMITTER, finalLoc, 1, 0.0, 0.0, 0.0, 0.0, null, true);
                finalLoc.getWorld().spawnParticle(Particle.END_ROD, finalLoc, 150, 0.6, 0.6, 0.6, 1.0);
                finalLoc.getWorld().spawnParticle(Particle.FIREWORK, finalLoc, 150, 0.6, 0.6, 0.6, 1.0);

                finalLoc.getWorld().playSound(finalLoc, Sound.ENTITY_ALLAY_DEATH, org.bukkit.SoundCategory.HOSTILE, 1.5F, 0.8F);
                finalLoc.getWorld().playSound(finalLoc, Sound.ENTITY_VEX_DEATH, org.bukkit.SoundCategory.HOSTILE, 1.5F, 0.7F);
                finalLoc.getWorld().playSound(finalLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, org.bukkit.SoundCategory.HOSTILE, 1.0F, 0.8F);

                bossManager.handleBossDeath(this, currentPosition);
                bossManager.unregisterBoss(anchor.getUniqueId());
            }
            return;
        }

        if (isSpawning) {
            tickWings();
            return;
        }

        animationTick++;

        if (this.variant != Variant.RADIANT) {
            final boolean isOverworld = anchor.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL;
            final long time = anchor.getWorld().getTime();
            this.isDaytime = isOverworld && (time < 12786 || time > 23460);
        }

        if (healingCooldownTicks > 0) {
            healingCooldownTicks--;
        }

        if (isSpiritShieldActive) {
            tickHealingShield();
        }

        if (phase == 1 && currentHealth <= (maxHealth / 2.0)) {
            phase = 2;
            hasEnteredPhase2 = true;
            attackCycle = 1;
            attackTimer = 0;
            anchor.getWorld().playSound(currentPosition, Sound.ENTITY_WITHER_SPAWN, 1.5F, 1.5F);
            spawnParticleSafe(anchor.getWorld(), Particle.EXPLOSION_EMITTER, currentPosition, 3, 0.5, 0.5, 0.5, 0.0);
            updateBoneVariants();
        }

        findNearestTarget();

        // 1. Расчёт перемещения и угла visualYaw
        if (isDashing) {
            tickDash();
        } else {
            flyTowardsTarget();
            calculateVisualYaw();
        }

        // 2. Применение перемещения к текущей позиции
        currentPosition.add(velocity);

        // 3. Явный инвариант контроллера: наклон pitch = 0.0f, поворот yaw = visualYaw
        currentPosition.setYaw(visualYaw);
        currentPosition.setPitch(0.0f);
        anchor.teleport(currentPosition);

        // 4. Синхронизация хитбокса Interaction (следует точно за позицией)
        hitbox.teleport(currentPosition.clone().subtract(0.0, 0.5, 0.0));

        // 5. Обновление анимации взмахов крыльев через матрицу трансформации
        tickWings();

        // 6. Выполнение цикла атак
        tickAttackCycle();

        activeBolts.removeIf(bolt -> !bolt.tick());
        activeLances.removeIf(lance -> !lance.tick());
    }

    private void tickWings() {
        if (bones.size() < 8) return;

        final double flapSpeed = 0.35D;
        final float angleOffset = (float) (Math.sin(animationTick * flapSpeed) * Math.toRadians(18.0D));

        // bone1 (index 6): left main wing
        final ItemDisplay bone1 = bones.get(6);
        if (bone1 != null && bone1.isValid()) {
            final Transformation trans = bone1.getTransformation();
            trans.getLeftRotation().set(new org.joml.Quaternionf().rotationY((float) Math.PI - angleOffset));
            bone1.setTransformation(trans);
        }

        // bone (index 7): right main wing
        final ItemDisplay bone = bones.get(7);
        if (bone != null && bone.isValid()) {
            final Transformation trans = bone.getTransformation();
            trans.getLeftRotation().set(new org.joml.Quaternionf().rotationY((float) Math.PI + angleOffset));
            bone.setTransformation(trans);
        }

        // bone7 (index 0): left secondary wing
        final ItemDisplay bone7 = bones.get(0);
        if (bone7 != null && bone7.isValid()) {
            final Transformation trans = bone7.getTransformation();
            trans.getLeftRotation().set(new org.joml.Quaternionf().rotationY((float) Math.PI - angleOffset * 0.7f).rotateZ(angleOffset * 0.4f));
            bone7.setTransformation(trans);
        }

        // bone8 (index 1): right secondary wing
        final ItemDisplay bone8 = bones.get(1);
        if (bone8 != null && bone8.isValid()) {
            final Transformation trans = bone8.getTransformation();
            trans.getLeftRotation().set(new org.joml.Quaternionf().rotationY((float) Math.PI + angleOffset * 0.7f).rotateZ(-angleOffset * 0.4f));
            bone8.setTransformation(trans);
        }

        // bone3 (index 2) & bone4 (index 3): bottom wings
        final ItemDisplay bone3 = bones.get(2);
        if (bone3 != null && bone3.isValid()) {
            final Transformation trans = bone3.getTransformation();
            trans.getLeftRotation().set(new org.joml.Quaternionf().rotationY((float) Math.PI - angleOffset * 0.5f));
            bone3.setTransformation(trans);
        }
        final ItemDisplay bone4 = bones.get(3);
        if (bone4 != null && bone4.isValid()) {
            final Transformation trans = bone4.getTransformation();
            trans.getLeftRotation().set(new org.joml.Quaternionf().rotationY((float) Math.PI + angleOffset * 0.5f));
            bone4.setTransformation(trans);
        }
    }

    private void findNearestTarget() {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        boolean playerNearby = false;

        for (final Player player : anchor.getWorld().getPlayers()) {
            final double dist = player.getLocation().distance(currentPosition);
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
        if (nearest == null) {
            isDashing = false;
            dashVector = null;
            velocity.zero();
        }

        if (playerNearby) {
            ticksWithoutPlayers = 0;
        } else {
            ticksWithoutPlayers++;
            if (ticksWithoutPlayers >= 400) {
                if (isRadiant()) {
                    Bukkit.broadcast(bossManager.getMessageService().message("boss.radiant_empress_of_light.despawn"));
                } else {
                    Bukkit.broadcast(bossManager.getMessageService().message("boss.empress_of_light.despawn"));
                }
                cleanup();
            }
        }
    }

    private void calculateVisualYaw() {
        if (currentTarget != null && currentTarget.isOnline() && !currentTarget.isDead()) {
            final Vector lookDir = currentTarget.getEyeLocation().toVector().subtract(currentPosition.toVector());
            final float targetYaw = (float) Math.toDegrees(Math.atan2(-lookDir.getX(), lookDir.getZ()));

            float diff = targetYaw - visualYaw;
            while (diff < -180.0f) diff += 360.0f;
            while (diff > 180.0f) diff -= 360.0f;

            visualYaw += diff * 0.25f;
        }
    }

    private void flyTowardsTarget() {
        if (currentTarget != null && currentTarget.isOnline() && !currentTarget.isDead()) {
            final Location targetLoc = currentTarget.getLocation().add(0, 6, 0);
            final Vector dir = targetLoc.toVector().subtract(currentPosition.toVector());
            final double dist = dir.length();
            if (dist > 0.5) {
                velocity = dir.normalize().multiply(Math.min(speed * 2.0, dist * 0.04));
            } else {
                velocity.multiply(0.8);
            }
        } else {
            velocity.multiply(0.8);
        }
    }

    private void tickAttackCycle() {
        attackTimer++;
        if (attackTimer >= 50) {
            attackTimer = 0;
            executeCycleStep();
        }
    }

    private void executeCycleStep() {
        if (currentTarget == null) {
            return;
        }

        if (phase == 1) {
            switch (attackCycle) {
                case 1:
                case 3:
                case 5:
                case 9:
                case 11:
                case 13:
                case 15:
                case 18:
                    teleportToTarget();
                    break;
                case 2:
                case 8:
                case 17:
                    launchPrismaticBolts();
                    break;
                case 4:
                case 10:
                case 14:
                case 19:
                    startDash();
                    break;
                case 6:
                case 16:
                    if (isDaytime) {
                        launchPrismaticBoltsPattern2();
                    } else {
                        launchPrismaticBolts();
                    }
                    break;
                case 7:
                case 12:
                    launchEtherealLances();
                    break;
            }
            attackCycle++;
            if (attackCycle > 19) {
                attackCycle = 1;
            }
        } else {
            switch (attackCycle) {
                case 1:
                case 4:
                case 6:
                case 9:
                case 11:
                case 13:
                case 16:
                case 19:
                case 21:
                case 24:
                case 26:
                case 30:
                case 34:
                case 36:
                case 38:
                case 40:
                case 43:
                case 45:
                    teleportToTarget();
                    break;
                case 2:
                case 17:
                case 33:
                case 41:
                    launchEtherealLancesPattern2();
                    break;
                case 3:
                case 8:
                case 18:
                case 23:
                case 46:
                    launchPrismaticBolts();
                    break;
                case 5:
                case 10:
                case 14:
                case 20:
                case 25:
                case 31:
                case 35:
                case 37:
                case 39:
                case 44:
                    startDash();
                    break;
                case 7:
                case 12:
                case 27:
                    launchEtherealLances();
                    break;
                case 22:
                    if (isRadiant() && healingUses < 3 && healingCooldownTicks <= 0) {
                        startHealingShield();
                    } else {
                        launchEtherealLances();
                    }
                    break;
                case 15:
                case 28:
                case 29:
                case 32:
                case 47:
                    launchPrismaticBoltsPattern2();
                    break;
                case 42:
                    if (isRadiant() && healingUses < 3 && healingCooldownTicks <= 0) {
                        startHealingShield();
                    } else {
                        launchPrismaticBoltsPattern2();
                    }
                    break;
            }
            attackCycle++;
            if (attackCycle > 48) {
                attackCycle = 1;
            }
        }
    }

    private void startHealingShield() {
        isSpiritShieldActive = true;
        spiritShieldHealth = 60.0;
        healingUses++;
        healingCooldownTicks = 400; // 20 sec cooldown
        healingTimerTicks = 80; // 4 seconds duration

        final Location loc = currentPosition != null ? currentPosition : anchor.getLocation();
        anchor.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.5F, 1.2F);
        anchor.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2F, 1.5F);
        anchor.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5F, 1.0F);

        spawnParticleSafe(loc.getWorld(), Particle.FLASH, loc.clone().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.0);
    }

    private void tickHealingShield() {
        healingTimerTicks--;
        final Location center = (currentPosition != null ? currentPosition : anchor.getLocation()).clone().add(0.0, 1.0, 0.0);
        final World world = center.getWorld();
        if (world != null) {
            for (int i = 0; i < 4; i++) {
                final double theta = Math.random() * 2 * Math.PI;
                final double phi = Math.random() * Math.PI;
                final double r = 1.8;
                final double x = r * Math.sin(phi) * Math.cos(theta);
                final double y = r * Math.cos(phi);
                final double z = r * Math.sin(phi) * Math.sin(theta);
                world.spawnParticle(Particle.END_ROD, center.clone().add(x, y, z), 1, 0, 0, 0, 0.01);
                world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, new Particle.DustOptions(Color.fromRGB(255, 100, 200), 1.2F));
            }

            if (healingTimerTicks % 10 == 0) {
                final double healAmount = this.maxHealth * 0.025;
                this.currentHealth = Math.min(this.maxHealth, this.currentHealth + healAmount);
                if (anchor != null && !anchor.isDead()) {
                    final double bufferHealth = Math.min(1024.0, Math.max(1.0, this.currentHealth));
                    anchor.setHealth(Math.min(anchor.getAttribute(Attribute.MAX_HEALTH).getValue(), bufferHealth));
                }
                world.spawnParticle(Particle.HEART, center, 3, 0.5, 0.5, 0.5, 0.0);
                world.playSound(center, Sound.ENTITY_ALLAY_ITEM_GIVEN, 1.0F, 1.4F);
            }

            if (healingTimerTicks <= 0) {
                isSpiritShieldActive = false;
                spiritShieldHealth = 0.0;
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0F, 1.2F);
            }
        }
    }

    private void teleportToTarget() {
        if (currentTarget == null) return;
        final double angle = Math.random() * 2 * Math.PI;
        final double radius = 8.0 + Math.random() * 4.0;
        final double x = Math.cos(angle) * radius;
        final double z = Math.sin(angle) * radius;

        final Location newLoc = currentTarget.getLocation().add(x, 5.0, z);
        visualYaw = (float) Math.toDegrees(angle + Math.PI);
        newLoc.setYaw(visualYaw);
        newLoc.setPitch(0.0f);

        currentPosition = newLoc.clone();
        velocity.zero();

        BossManager.teleportWithPassengers(anchor, newLoc);
        hitbox.teleport(newLoc.clone().subtract(0.0, 0.5, 0.0));

        currentPosition.getWorld().spawnParticle(Particle.PORTAL, currentPosition, 30, 0.5, 1.0, 0.5, 0.1);
        currentPosition.getWorld().playSound(currentPosition, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2F, 1.2F);
    }

    private void launchPrismaticBolts() {
        if (currentTarget == null) return;
        anchor.getWorld().playSound(currentPosition, Sound.ENTITY_ALLAY_ITEM_TAKEN, 1.5F, 1.4F);

        final Location start = currentPosition.clone().add(0, 1.5, 0);
        final Vector toTarget = currentTarget.getEyeLocation().toVector().subtract(start.toVector()).normalize();

        for (int i = 0; i < 8; i++) {
            final double offsetAngle = (i - 3.5) * 15.0;
            final Vector rotatedDir = rotateVectorY(toTarget, offsetAngle);
            final PrismaticBolt bolt = new PrismaticBolt(plugin, anchor, start, currentTarget, rotatedDir, isDaytime, baseDamage);
            activeBolts.add(bolt);
        }
    }

    private void launchPrismaticBoltsPattern2() {
        if (currentTarget == null) return;
        anchor.getWorld().playSound(currentPosition, Sound.ENTITY_ALLAY_ITEM_TAKEN, 1.5F, 1.6F);

        final Location start = currentPosition.clone().add(0, 1.5, 0);
        for (int i = 0; i < 12; i++) {
            final double angle = (i * 30.0) * (Math.PI / 180.0);
            final Vector dir = new Vector(Math.cos(angle), 0.2, Math.sin(angle));
            final PrismaticBolt bolt = new PrismaticBolt(plugin, anchor, start, currentTarget, dir, isDaytime, baseDamage);
            activeBolts.add(bolt);
        }
    }

    private void launchEtherealLances() {
        if (currentTarget == null) return;
        anchor.getWorld().playSound(currentPosition, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.2F, 1.6F);

        final Location center = currentTarget.getLocation();
        for (int i = 0; i < 6; i++) {
            final double angle = i * 60.0;
            final double rad = Math.toRadians(angle);
            final Location start = center.clone().add(Math.cos(rad) * 15.0, 8.0, Math.sin(rad) * 15.0);
            final Vector dir = center.toVector().subtract(start.toVector()).normalize();
            activeLances.add(new EtherealLance(plugin, anchor, start, currentTarget, dir, isRadiant(), isDaytime, baseDamage * 1.5));
        }
    }

    private void launchEtherealLancesPattern2() {
        if (currentTarget == null) return;
        anchor.getWorld().playSound(currentPosition, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.2F, 1.8F);

        final Location center = currentTarget.getLocation();
        for (int i = 0; i < 12; i++) {
            final double xOffset = (Math.random() - 0.5) * 20.0;
            final double zOffset = (Math.random() - 0.5) * 20.0;
            final Location start = center.clone().add(xOffset, 12.0, zOffset);
            final Vector dir = new Vector(0, -1, 0);
            activeLances.add(new EtherealLance(plugin, anchor, start, currentTarget, dir, isRadiant(), isDaytime, baseDamage * 1.8));
        }
    }

    private void startDash() {
        if (currentTarget == null) return;
        isDashing = true;
        dashTicks = 0;

        dashVector = currentTarget.getLocation().toVector().subtract(currentPosition.toVector()).normalize();
        visualYaw = (float) Math.toDegrees(Math.atan2(-dashVector.getX(), dashVector.getZ()));

        anchor.getWorld().playSound(currentPosition, Sound.ENTITY_PHANTOM_BITE, 1.5F, 1.5F);
        spawnParticleSafe(anchor.getWorld(), Particle.FLASH, currentPosition.clone().add(0.0D, 1.0D, 0.0D), 1, 0.0D, 0.0D, 0.0D, 0.0D, Color.WHITE);
    }

    private void tickDash() {
        dashTicks++;
        if (dashTicks > 12 || dashVector == null) {
            isDashing = false;
            dashVector = null;
            velocity.zero();
            return;
        }

        velocity = dashVector.clone().multiply(1.4);

        if (currentTarget != null && currentPosition.getY() <= currentTarget.getLocation().getY() + 1.0 && velocity.getY() < 0) {
            velocity.setY(0);
        }

        // Во время дэша visualYaw ориентирован строго по вектору рывка!
        visualYaw = (float) Math.toDegrees(Math.atan2(-dashVector.getX(), dashVector.getZ()));

        currentPosition.getWorld().spawnParticle(Particle.CLOUD, currentPosition, 5, 0.2, 0.2, 0.2, 0.02);

        if (isRadiant()) {
            currentPosition.getWorld().spawnParticle(Particle.FIREWORK, currentPosition, 3, 0.2, 0.2, 0.2, 0.05);

            // Radiant Dashes leave a trail of Ethereal Lances and Prismatic Bolts!
            if (dashTicks == 3 || dashTicks == 9) {
                final Vector spread = new Vector((Math.random() - 0.5) * 0.4, 0.1, (Math.random() - 0.5) * 0.4);
                final Vector boltDir = dashVector.clone().multiply(-0.6).add(spread).normalize();
                activeBolts.add(new PrismaticBolt(plugin, anchor, currentPosition.clone().add(0, 1, 0), currentTarget, boltDir, isDaytime, baseDamage));
            } else if (dashTicks == 6) {
                final Vector lanceDir = currentTarget != null
                        ? currentTarget.getLocation().clone().add(0, 1, 0).toVector().subtract(currentPosition.toVector()).normalize()
                        : dashVector.clone();
                activeLances.add(new EtherealLance(plugin, anchor, currentPosition.clone().add(0, 1, 0), currentTarget, lanceDir, isRadiant(), isDaytime, baseDamage * 1.5));
            }
        }

        for (final Player player : anchor.getWorld().getPlayers()) {
            if (!DamageSafetyHelper.canDamage(player, anchor)) {
                continue;
            }
            if (player.getLocation().distance(currentPosition) <= 2.2) {
                if (isRadiant()) {
                    // Пробивание брони (Armor Penetration) и Holy Flames
                    final double damage = baseDamage * 2.5;
                    try {
                        final org.bukkit.damage.DamageSource src = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.MAGIC)
                                .withCausingEntity(anchor)
                                .withDirectEntity(anchor)
                                .build();
                        player.damage(damage, src);
                    } catch (final Throwable ignored) {
                        player.damage(damage, anchor);
                    }
                    if (bossManager != null && bossManager.getCustomStatusEffectService() != null) {
                        bossManager.getCustomStatusEffectService().applyHolyFlames(player, 100, anchor);
                    }
                } else if (isDaytime) {
                    try {
                        final org.bukkit.damage.DamageSource src = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.MAGIC)
                                .withCausingEntity(anchor)
                                .withDirectEntity(anchor)
                                .build();
                        player.damage(10000.0, src);
                    } catch (final Throwable ignored) {
                        player.damage(10000.0, anchor);
                    }
                } else {
                    try {
                        final org.bukkit.damage.DamageSource src = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.MOB_ATTACK)
                                .withCausingEntity(anchor)
                                .withDirectEntity(anchor)
                                .build();
                        player.damage(baseDamage * 1.2, src);
                    } catch (final Throwable ignored) {
                        player.damage(baseDamage * 1.2, anchor);
                    }
                }
            }
        }
    }

    public boolean deflectBoltByDisplay(final ItemDisplay display) {
        for (final PrismaticBolt bolt : activeBolts) {
            if (bolt.getDisplay().equals(display)) {
                bolt.onPlayerHit();
                return true;
            }
        }
        return false;
    }

    private Vector rotateVectorY(final Vector v, final double angleDegrees) {
        final double rad = Math.toRadians(angleDegrees);
        final double cos = Math.cos(rad);
        final double sin = Math.sin(rad);
        final double x = v.getX() * cos - v.getZ() * sin;
        final double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }

    private void spawnAuroraParticles(final Location center, final boolean daytime) {
        final org.bukkit.World world = center.getWorld();
        if (world == null) return;
        final double yawRad = Math.toRadians((deathTicks * 3) % 360);

        for (int angle = 0; angle < 360; angle += 30) {
            final double rad = Math.toRadians(angle) + yawRad;

            final double cos = Math.cos(rad);
            final double sin = Math.sin(rad);

            final Color color;
            if (isRadiant()) {
                color = Color.fromRGB(236, 52, 64);
            } else if (daytime) {
                color = Color.fromRGB(255, 208, 0);
            } else {
                color = getAuroraColor(angle);
            }

            final Particle.DustOptions dust = new Particle.DustOptions(color, 1.45F);

            final Location pLoc4 = center.clone().add(cos * 3.5D, 0.3D, sin * 3.5D);
            world.spawnParticle(Particle.DUST, pLoc4, 2, 0.4D, 0.4D, 0.4D, 0.0D, dust);

            final Location pLoc3 = center.clone().add(cos * 2.5D, 0.3D, sin * 2.5D);
            world.spawnParticle(Particle.DUST, pLoc3, 2, 0.3D, 0.3D, 0.3D, 0.0D, dust);
        }
    }

    private Color getAuroraColor(final int angle) {
        switch (angle) {
            case 0: return Color.fromRGB(255, 0, 0);
            case 30: return Color.fromRGB(255, 72, 0);
            case 60: return Color.fromRGB(255, 136, 0);
            case 90: return Color.fromRGB(255, 208, 0);
            case 120: return Color.fromRGB(255, 255, 0);
            case 150: return Color.fromRGB(187, 255, 0);
            case 180: return Color.fromRGB(0, 255, 42);
            case 210: return Color.fromRGB(0, 255, 136);
            case 240: return Color.fromRGB(0, 255, 234);
            case 270: return Color.fromRGB(38, 132, 255);
            case 300: return Color.fromRGB(157, 51, 255);
            case 330: return Color.fromRGB(255, 0, 128);
            default: return Color.WHITE;
        }
    }

    @Override
    public void cleanup() {
        for (final ItemDisplay bone : bones) {
            if (bone != null && bone.isValid()) {
                bone.remove();
            }
        }
        bones.clear();
        if (hitbox != null) {
            hitbox.remove();
        }
        if (anchor != null) {
            anchor.remove();
        }
        for (final PrismaticBolt bolt : activeBolts) {
            bolt.cleanup();
        }
        activeBolts.clear();
        for (final EtherealLance lance : activeLances) {
            lance.cleanup();
        }
        activeLances.clear();
    }

    @Override
    public boolean isInvulnerable() {
        return isDying;
    }

    @Override
    public void damageShield(double damage) {
        if (isSpiritShieldActive) {
            syncHealthAfterHit(damage, null);
        }
    }

    public boolean isDaytime() {
        return isDaytime;
    }

    public boolean isRadiant() {
        return variant == Variant.RADIANT;
    }

    public static void spawnParticleSafe(final World world, final Particle particle, final Location loc, final int count, final double ox, final double oy, final double oz, final double speed, final Object preferredData, final boolean force) {
        if (world == null || loc == null || particle == null) {
            return;
        }
        final Class<?> dataType = particle.getDataType();
        if (dataType == Void.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, null, force);
        } else if (preferredData != null && dataType.isInstance(preferredData)) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, preferredData, force);
        } else if (dataType == Float.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, 1.0F, force);
        } else if (dataType == Color.class) {
            final Color col = (preferredData instanceof Color c) ? c : (preferredData instanceof Particle.DustOptions d) ? d.getColor() : Color.WHITE;
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, col, force);
        } else if (dataType == Particle.DustOptions.class) {
            final Color col = (preferredData instanceof Color c) ? c : Color.WHITE;
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, new Particle.DustOptions(col, 1.0F), force);
        } else if (dataType == Particle.DustTransition.class) {
            final Color col = (preferredData instanceof Color c) ? c : Color.WHITE;
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, new Particle.DustTransition(col, Color.WHITE, 1.0F), force);
        } else {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, null, force);
        }
    }

    public static void spawnParticleSafe(final World world, final Particle particle, final Location loc, final int count, final double ox, final double oy, final double oz, final double speed, final Object preferredData) {
        if (world == null || loc == null || particle == null) {
            return;
        }
        final Class<?> dataType = particle.getDataType();
        if (dataType == Void.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed);
        } else if (preferredData != null && dataType.isInstance(preferredData)) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, preferredData);
        } else if (dataType == Float.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, 1.0F);
        } else if (dataType == Color.class) {
            final Color col = (preferredData instanceof Color c) ? c : (preferredData instanceof Particle.DustOptions d) ? d.getColor() : Color.WHITE;
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, col);
        } else if (dataType == Particle.DustOptions.class) {
            final Color col = (preferredData instanceof Color c) ? c : Color.WHITE;
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, new Particle.DustOptions(col, 1.0F));
        } else if (dataType == Particle.DustTransition.class) {
            final Color col = (preferredData instanceof Color c) ? c : Color.WHITE;
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, new Particle.DustTransition(col, Color.WHITE, 1.0F));
        } else {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed);
        }
    }

    public static void spawnParticleSafe(final World world, final Particle particle, final Location loc, final int count, final double ox, final double oy, final double oz, final double speed) {
        spawnParticleSafe(world, particle, loc, count, ox, oy, oz, speed, null);
    }
}
