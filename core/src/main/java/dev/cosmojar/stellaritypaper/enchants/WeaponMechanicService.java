package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Оружейный рантайм для не енчант механик и книг заклинаний.
 */
public final class WeaponMechanicService {

    private static final String ITEM_TAMARIS = "tamaris";
    private static final String ITEM_STELLAR_STRIKER = "stellar_striker";
    private static final String ITEM_PRISMEMBER = "prismember";
    private static final String ITEM_PRISMATIC_PUNCH = "prismatic_punch";
    private static final String ITEM_SHARANGA = "sharanga";
    private static final String ITEM_SPECTRAL_FURY = "spectral_fury";
    private static final String ITEM_SLAYER_CROSSBOW = "slayer_crossbow";
    private static final String ITEM_CALL_OF_THE_VOID = "call_of_the_void";
    private static final String ITEM_STARLESS_SCYTHE = "starless_scythe";
    private static final String ITEM_FLUFFY_HAMMER = "fluffy_hammer";
    private static final String ITEM_SANDSTORM_TRIDENT = "sandstorm_trident";
    private static final String ITEM_THE_BEGINNING = "weapons_the_beginning";
    private static final String ITEM_THE_END = "spirit_dagger";
    private static final String ITEM_BOOK_CONVEYANCE = "book_of_conveyance";
    private static final String ITEM_BOOK_JINX = "book_of_jinx";
    private static final String ITEM_BOOK_LIGHT = "book_of_light";
    private static final String ITEM_BOOK_OBSTRUCT = "book_of_obstruct";
    private static final String ITEM_BOOK_RETURN = "book_of_return";
    private static final String ITEM_BOOK_UPDRAFT = "book_of_updraft";
    private static final String ITEM_HARVESTER = "harvester";

    private final org.bukkit.plugin.Plugin plugin;
    private final ItemStateRepository itemStateRepository;
    private final CustomStatusEffectService customStatusEffectService;
    private final HarvesterMechanicService harvesterMechanicService;
    private final StellaritySoundService soundService;
    private final ItemsConfigService itemsConfig;
    private final dev.cosmojar.stellaritypaper.text.MessageService messageService;

    private final NamespacedKey projectileKindKey;
    private final NamespacedKey updraftGravityKey;
    private final NamespacedKey updraftFallDamageKey;
    private final NamespacedKey lightKnockbackKey;
    private final NamespacedKey obstructWallKey;
    private final NamespacedKey obstructOwnerKey;
    private final NamespacedKey updraftFloatKey;

    private final Map<UUID, State> states = new HashMap<>();
    private final Map<UUID, BukkitTask> playerTasks = new HashMap<>();
    private final Map<UUID, Long> tamarisCooldown = new HashMap<>();
    private final Map<UUID, Long> tamarisVictimSafeCooldown = new HashMap<>();
    private final Map<UUID, Long> fluffyCooldown = new HashMap<>();
    private final Map<UUID, Long> spellbookCooldown = new HashMap<>();

    private final Map<UUID, BukkitTask> activeReturnCasts = new HashMap<>();
    private final Map<UUID, List<List<Shulker>>> activeObstructWalls = new HashMap<>();
    private final ThreadLocal<Boolean> isProcessingSweep = ThreadLocal.withInitial(() -> false);
    private final RadiantJewelService radiantJewelService;

    public WeaponMechanicService(
            final org.bukkit.plugin.Plugin plugin,
            final ItemStateRepository itemStateRepository,
            final CustomStatusEffectService customStatusEffectService,
            final HarvesterMechanicService harvesterMechanicService,
            final StellaritySoundService soundService,
            final ItemsConfigService itemsConfig,
            final dev.cosmojar.stellaritypaper.text.MessageService messageService,
            final RadiantJewelService radiantJewelService
    ) {
        this.plugin = plugin;
        this.itemStateRepository = itemStateRepository;
        this.customStatusEffectService = customStatusEffectService;
        this.harvesterMechanicService = harvesterMechanicService;
        this.soundService = soundService;
        this.itemsConfig = itemsConfig;
        this.messageService = messageService;
        this.radiantJewelService = radiantJewelService;
        this.projectileKindKey = new NamespacedKey(plugin, "weapon_projectile_kind");
        this.updraftGravityKey = new NamespacedKey(plugin, "weapon.updraft.gravity");
        this.updraftFallDamageKey = new NamespacedKey(plugin, "weapon.updraft.falldamage");
        this.lightKnockbackKey = new NamespacedKey(plugin, "weapon.light.knockback");
        this.obstructWallKey = new NamespacedKey(plugin, "weapon.obstruct.wall");
        this.obstructOwnerKey = new NamespacedKey(plugin, "weapon.obstruct.owner");
        this.updraftFloatKey = new NamespacedKey(plugin, "weapon.updraft.float");
    }

    public void syncPlayer(final Player player) {
        if (player == null || !player.isOnline()) return;
        if (radiantJewelService != null) {
            radiantJewelService.rescan(player);
        }
        final String itemId = itemId(player);
        final boolean loop = ITEM_STELLAR_STRIKER.equalsIgnoreCase(itemId) 
                || ITEM_PRISMEMBER.equalsIgnoreCase(itemId)
                || ITEM_TAMARIS.equalsIgnoreCase(itemId)
                || ITEM_HARVESTER.equalsIgnoreCase(itemId);
        if (!loop) {
            stopTask(player.getUniqueId(), false);
            return;
        }
        playerTasks.computeIfAbsent(player.getUniqueId(), id -> Bukkit.getScheduler().runTaskTimer(plugin, () -> tickPlayer(id), 1L, 1L));
    }

    public void clear(final Player player) {
        if (player == null) return;
        final UUID uuid = player.getUniqueId();
        stopTask(uuid, true);
        tamarisCooldown.remove(uuid);
        tamarisVictimSafeCooldown.remove(uuid);
        fluffyCooldown.remove(uuid);
        spellbookCooldown.remove(uuid);

        final BukkitTask castTask = activeReturnCasts.remove(uuid);
        if (castTask != null) {
            castTask.cancel();
        }

        final List<List<Shulker>> walls = activeObstructWalls.remove(uuid);
        if (walls != null) {
            for (final List<Shulker> wall : walls) {
                for (final Shulker shulker : wall) {
                    if (shulker.isValid()) shulker.remove();
                }
            }
        }

        removeModifier(player, Attribute.GRAVITY, updraftGravityKey);
        removeModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, updraftFallDamageKey);
        removeModifier(player, Attribute.KNOCKBACK_RESISTANCE, lightKnockbackKey);
    }

    public void clearAll() {
        for (final BukkitTask task : playerTasks.values()) task.cancel();
        playerTasks.clear();
        states.clear();
        tamarisCooldown.clear();
        tamarisVictimSafeCooldown.clear();
        fluffyCooldown.clear();
        spellbookCooldown.clear();

        for (final BukkitTask castTask : activeReturnCasts.values()) {
            castTask.cancel();
        }
        activeReturnCasts.clear();

        for (final List<List<Shulker>> walls : activeObstructWalls.values()) {
            for (final List<Shulker> wall : walls) {
                for (final Shulker shulker : wall) {
                    if (shulker.isValid()) shulker.remove();
                }
            }
        }
        activeObstructWalls.clear();
    }

    public void onInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final String id = itemId(player);
        if (isRightClick(event.getAction())) {
            if (ITEM_FLUFFY_HAMMER.equalsIgnoreCase(id)) throwFluffyHammer(player);
            if (isSpiritCombo(player)) spiritDaggerBlink(player);
            if (isSpellbook(id)) castSpellbook(player, id);
        }
        if (isLeftClick(event.getAction()) && player.isSneaking() && ITEM_STELLAR_STRIKER.equalsIgnoreCase(id)) {
            launchStellar(player);
        }
    }

    public void onDamageByEntity(final EntityDamageByEntityEvent event) {
        if (isProcessingSweep.get()) return;
        final Player attacker = resolvePlayer(event.getDamager());
        if (attacker != null && event.getEntity() instanceof LivingEntity victim) {
            final String id = itemId(attacker);
            if (ITEM_STELLAR_STRIKER.equalsIgnoreCase(id)) state(attacker).stellarCollect = Math.min(10_000, state(attacker).stellarCollect + 200);
            
            isProcessingSweep.set(true);
            try {
                if (ITEM_PRISMEMBER.equalsIgnoreCase(id) && state(attacker).prismemberCharge >= 8) prismemberSpin(attacker);
                if (ITEM_STARLESS_SCYTHE.equalsIgnoreCase(id)) starlessSweep(event, attacker, victim);
            } finally {
                isProcessingSweep.set(false);
            }
        }

        if (event.getEntity() instanceof Shulker shulker && Boolean.TRUE.equals(shulker.getPersistentDataContainer().get(obstructWallKey, PersistentDataType.BOOLEAN))) {
            event.setCancelled(true);
            triggerObstructExplosion(shulker);
        }
    }

    public void onShootBow(final EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectile() instanceof Projectile projectile)) return;
        final String id = itemId(player);
        switch (id.toLowerCase(java.util.Locale.ROOT)) {
            case ITEM_SHARANGA -> {
                if (projectile instanceof AbstractArrow arrow) {
                    arrow.setPierceLevel(3);
                    arrow.setGravity(false);
                    new org.bukkit.scheduler.BukkitRunnable() {
                        int ticks = 0;
                        int stillTicks = 0;
                        Location lastLoc = null;
                        @Override
                        public void run() {
                            ticks++;
                            if (!arrow.isValid() || arrow.isDead()) {
                                cancel();
                                return;
                            }
                            final Location loc = arrow.getLocation();
                            boolean isMoving = true;
                            if (lastLoc != null) {
                                final double distSq = loc.distanceSquared(lastLoc);
                                if (distSq < 0.0001D) {
                                    stillTicks++;
                                    isMoving = false;
                                } else {
                                    stillTicks = 0;
                                }
                            }
                            lastLoc = loc.clone();

                            if (isMoving && ticks % 3 == 0) {
                                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1);
                                loc.getWorld().spawnParticle(Particle.GLOW, loc, 1);
                            }

                            if (stillTicks >= 3 && isReallyOnGround(arrow)) {
                                cancel();
                                return;
                            }

                            if (ticks > 60 || (stillTicks >= 3 && !isReallyOnGround(arrow))) {
                                loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0.0D, 0.0D, 0.0D, 0.0D, org.bukkit.Color.WHITE);
                                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 4, 0.1D, 0.1D, 0.1D, 0.05D);
                                loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7F, 1.6F);
                                loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5F, 1.8F);
                                arrow.remove();
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 1L, 1L);
                }
                setKind(projectile, "sharanga");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.2F, 2F);
            }
            case ITEM_SPECTRAL_FURY -> { setKind(projectile, "spectral"); soundService.play(player, "stellarity:item.spectral_fury.shoot", 1, 1, Sound.ENTITY_WARDEN_SONIC_BOOM); }
            case ITEM_CALL_OF_THE_VOID -> {
                setKind(projectile, "call");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 0.2F, 0.7F);
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!projectile.isValid() || projectile.isOnGround()) {
                            this.cancel();
                            return;
                        }
                        projectile.getWorld().spawnParticle(Particle.DRAGON_BREATH, projectile.getLocation(), 1, 0.0D, 0.0D, 0.0D, 0.01D);
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            }
            case ITEM_PRISMATIC_PUNCH -> { setKind(projectile, "prismatic"); soundService.play(player, "stellarity:item.prismatic_punch.shoot", 1, 1, Sound.ITEM_CROSSBOW_SHOOT); }
            case ITEM_SLAYER_CROSSBOW -> { projectile.remove(); slayerRay(player); }
            default -> {}
        }
    }

    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident) || !(trident.getShooter() instanceof Player player)) return;
        if (ITEM_SANDSTORM_TRIDENT.equalsIgnoreCase(itemId(player))) {
            setKind(trident, "sandstorm");
            trident.setGravity(false);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.5F, 1.0F);
            new org.bukkit.scheduler.BukkitRunnable() {
                int flightTicks = 0;
                @Override public void run() {
                    if (!trident.isValid() || trident.isDead() || trident.isInBlock() || ++flightTicks > 100) {
                        cancel();
                        return;
                    }
                    trident.getWorld().spawnParticle(Particle.CLOUD, trident.getLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }
    }

    public void onProjectileHit(final ProjectileHitEvent event) {
        final Projectile projectile = event.getEntity();
        final String kind = projectile.getPersistentDataContainer().getOrDefault(projectileKindKey, PersistentDataType.STRING, "");
        if ("prismatic".equals(kind)) explodePrismatic(projectile);
        if ("stellar".equals(kind)) explodeStellar(projectile, event.getHitEntity());
        if ("spectral".equals(kind)) spectralBurst(projectile);
        if ("call".equals(kind)) {
            final Location hitLoc = event.getHitEntity() != null ? event.getHitEntity().getLocation().add(0, 1, 0) : projectile.getLocation();
            callSplash(projectile, hitLoc);
        }
        if ("sandstorm".equals(kind)) {
            projectile.setGravity(true);
            sandstorm(projectile);
        }
    }

    private void tickPlayer(final UUID playerId) {
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) { stopTask(playerId, true); return; }
        final State s = state(player);
        if (s.stellarCooldown > 0) s.stellarCooldown--;
        if (s.prismemberCooldown > 0) s.prismemberCooldown--;
        final String id = itemId(player);
        if (ITEM_STELLAR_STRIKER.equalsIgnoreCase(id)) {
            if (s.stellarCooldown <= 0) s.stellarCollect = Math.min(10_000, s.stellarCollect + 4);
            player.sendActionBar(Component.text("• ✦ " + stars(s.stellarCollect) + "/5 ✦ •", NamedTextColor.YELLOW));
            return;
        }
        if (ITEM_PRISMEMBER.equalsIgnoreCase(id)) {
            if (player.isSneaking() && s.prismemberCooldown <= 0) {
                s.prismGate++;
                if (s.prismGate >= 2) {
                    s.prismGate = 0;
                    final int prevCharge = s.prismemberCharge;
                    s.prismemberCharge = Math.min(8, s.prismemberCharge + 1);
                    if (s.prismemberCharge > prevCharge) {
                        if (s.prismemberCharge == 8) {
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.15F, 2.0F);
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.05F, 1.5F);
                        } else {
                            final float pitch = 0.5F + (s.prismemberCharge - 1) * 0.142F;
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.05F, pitch);
                        }
                    }
                }
                s.prismReset = 2;
            } else if (s.prismReset > 0) {
                s.prismReset--;
            } else {
                s.prismemberCharge = 0;
            }
            if (s.prismemberCharge > 0) {
                player.sendActionBar(Component.text("• 🗡 " + s.prismemberCharge + "/8 🔥 •", NamedTextColor.RED));
            }
            return;
        }
        if (ITEM_TAMARIS.equalsIgnoreCase(id)) {
            final long now = System.currentTimeMillis();
            final long cd = tamarisCooldown.getOrDefault(player.getUniqueId(), 0L);
            if (now >= cd) {
                s.tamarisNoWarn = false;
            }

            if (player.isSneaking()) {
                if (now < cd) {
                    if (!s.tamarisNoWarn) {
                        player.sendActionBar(Component.translatable("item.stellarity.tamaris.disabled"));
                    }
                    s.tamarisTicks = 0;
                    return;
                }

                LivingEntity target = null;
                double bestDistance = Double.MAX_VALUE;
                for (final Entity entity : player.getNearbyEntities(10.0D, 10.0D, 10.0D)) {
                    if (entity instanceof LivingEntity living && !living.getUniqueId().equals(player.getUniqueId())) {
                        final String typeName = living.getType().name();
                        if (typeName.contains("WITHER") || typeName.contains("DRAGON") || typeName.contains("WARDEN") || living.getScoreboardTags().contains("stellarity.boss")) {
                            continue;
                        }
                        if (now < tamarisVictimSafeCooldown.getOrDefault(living.getUniqueId(), 0L)) {
                            continue;
                        }
                        
                        final AttributeInstance maxHealthAttr = living.getAttribute(Attribute.MAX_HEALTH);
                        final double maxHp = maxHealthAttr != null ? maxHealthAttr.getValue() : living.getHealth();
                        if (maxHp > 0 && living.getHealth() / maxHp <= 0.25D) {
                            final double dist = player.getLocation().distanceSquared(living.getLocation());
                            if (dist < bestDistance) {
                                bestDistance = dist;
                                target = living;
                            }
                        }
                    }
                }

                if (target == null) {
                    s.tamarisTicks = 0;
                    return;
                }

                s.tamarisTicks++;
                if (s.tamarisTicks == 2) {
                    soundService.play(player, "stellarity:item.tamaris.execute", 1, 1, Sound.ENTITY_EVOKER_PREPARE_ATTACK);
                }
                if (s.tamarisTicks >= 5) {
                    s.tamarisTicks = 0;

                    final Location targetLoc = target.getLocation();
                    
                    Vector dirToPlayer = player.getLocation().toVector().subtract(targetLoc.toVector()).setY(0);
                    if (dirToPlayer.lengthSquared() > 0) {
                        dirToPlayer.normalize();
                    } else {
                        dirToPlayer = new Vector(1, 0, 0);
                    }
                    final Location newLoc = targetLoc.clone().add(dirToPlayer.multiply(1.25D)).add(0.0D, 0.1D, 0.0D);
                    newLoc.setDirection(dirToPlayer.multiply(-1.0D));
                    player.teleport(newLoc);

                    target.getWorld().spawnParticle(Particle.PORTAL, targetLoc.clone().add(0, 1, 0), 30, 0.3D, 0.5D, 0.3D, 0.1D);
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, targetLoc.clone().add(0, 1.05, 0), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    
                    final org.bukkit.Color purpleFrom = org.bukkit.Color.fromRGB(153, 0, 255);
                    final org.bukkit.Color purpleTo = org.bukkit.Color.fromRGB(45, 0, 75);
                    final Particle.DustTransition transition = new Particle.DustTransition(purpleFrom, purpleTo, 2.0F);
                    target.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, targetLoc.clone().add(0, 1, 0), 40, 0.4D, 0.6D, 0.4D, 0.0D, transition);
                    
                    soundService.play(target, "stellarity:item.tamaris.execute_bg", 1, 1, null);
                    soundService.play(target, "stellarity:item.tamaris.execute", 1, 1, null);
                    
                    target.damage(999.0D, player);
                    tamarisCooldown.put(player.getUniqueId(), now + 1000L);
                    tamarisVictimSafeCooldown.entrySet().removeIf(entry -> now >= entry.getValue());
                    tamarisVictimSafeCooldown.put(target.getUniqueId(), now + 3000L);
                    s.tamarisNoWarn = true;
                }
            } else {
                s.tamarisTicks = 0;
            }
            return;
        }
        if (ITEM_HARVESTER.equalsIgnoreCase(id)) {
            s.harvesterAmbientTicks++;
            if (s.harvesterAmbientTicks >= 3) {
                s.harvesterAmbientTicks = 0;
                harvesterMechanicService.spawnHarvesterAmbientParticles(player);
            }
            return;
        }
        s.tamarisTicks = 0;
        s.harvesterAmbientTicks = 0;
        player.sendActionBar(Component.empty());
    }



    private void launchStellar(final Player player) {
        final State s = state(player);
        final int stars = stars(s.stellarCollect);
        if (stars <= 0 || s.stellarCooldown > 0) return;
        final float witherPitch = (float) (1.4D - stars * 0.1D);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, org.bukkit.SoundCategory.PLAYERS, 0.85F, witherPitch);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, org.bukkit.SoundCategory.PLAYERS, 0.75F, 1.1F);
        if (player.getName().equalsIgnoreCase("kohara_")) {
            player.getWorld().playSound(player.getLocation(), "stellarity:item.stellar_striker.thorn_apart", org.bukkit.SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        for (int i = 0; i < stars; i++) {
            final double off = (i - (stars - 1) / 2.0D) * 6.0D;
            final Vector dir = rotateYaw(player.getEyeLocation().getDirection().normalize(), off).multiply(1.4D);
            final Snowball star = player.launchProjectile(Snowball.class, dir); star.setGravity(false); setKind(star, "stellar");
        }
        s.stellarCollect = 0; s.stellarCooldown = 120;
    }

    private void prismemberSpin(final Player player) {
        final State s = state(player);
        s.prismemberCharge = 0;
        final boolean hasRadiant = radiantJewelService != null && radiantJewelService.hasRadiantJewel(player);
        s.prismemberCooldown = hasRadiant ? 10 : 20;
        
        final AttributeInstance attackDamageAttr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        final double baseDamage = attackDamageAttr != null ? attackDamageAttr.getValue() : 8.0D;

        for (final Entity e : player.getNearbyEntities(4.0D, 1.5D, 4.0D)) {
            if (e instanceof LivingEntity l && !l.getUniqueId().equals(player.getUniqueId())) {
                l.damage(baseDamage, player);
                customStatusEffectService.applyPrismaticInferno(l, 120, player);
            }
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks > 4 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                final Location loc = player.getLocation();
                final org.bukkit.World world = player.getWorld();

                if (ticks == 1) {
                    world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);
                    world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0F, 0.5F);
                    world.playSound(loc, Sound.ITEM_TRIDENT_THROW, 1.0F, 0.78F);

                    for (int i = 0; i < 16; i++) {
                        final double angle = Math.toRadians(i * 22.5D);
                        final Vector velocity = new Vector(Math.cos(angle), 0.0D, Math.sin(angle)).multiply(0.25D);
                        world.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.7, 0), 0, velocity.getX(), 0.0D, velocity.getZ(), 1.0D);
                    }
                } else if (ticks == 3) {
                    world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
                    world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);
                    world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0F, 1.0F);
                }

                final float yawStep1 = player.getLocation().getYaw() - (ticks * 90.0F);
                final float yawStep2 = yawStep1 - 45.0F;

                spawnSpinParticlesAtAngle(world, loc, yawStep1);
                spawnSpinParticlesAtAngle(world, loc, yawStep2);
            }

            private void spawnSpinParticlesAtAngle(final org.bukkit.World world, final Location baseLoc, final float yaw) {
                final Vector dir = calculateOffsetVector(yaw, 1.5D);
                final Location particleLoc = baseLoc.clone().add(dir).add(0.0D, 0.7D, 0.0D);

                world.spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                world.spawnParticle(Particle.FLAME, particleLoc, 5, 0.25D, 0.25D, 0.25D, 0.018D);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.25D, 0.25D, 0.25D, 0.01D);

                final org.bukkit.Color color;
                switch (ticks) {
                    case 1: color = (ThreadLocalRandom.current().nextBoolean()) ? org.bukkit.Color.fromRGB(250, 62, 62) : org.bukkit.Color.fromRGB(250, 175, 62); break;
                    case 2: color = (ThreadLocalRandom.current().nextBoolean()) ? org.bukkit.Color.fromRGB(250, 222, 62) : org.bukkit.Color.fromRGB(153, 250, 62); break;
                    case 3: color = (ThreadLocalRandom.current().nextBoolean()) ? org.bukkit.Color.fromRGB(62, 228, 250) : org.bukkit.Color.fromRGB(81, 62, 250); break;
                    default: color = (ThreadLocalRandom.current().nextBoolean()) ? org.bukkit.Color.fromRGB(250, 62, 234) : org.bukkit.Color.fromRGB(165, 62, 250); break;
                }
                final org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(color, 1.15F);
                world.spawnParticle(Particle.DUST, particleLoc, 6, 0.25D, 0.25D, 0.25D, 0.0D, dust);
            }

            private Vector calculateOffsetVector(final float yaw, final double distance) {
                final double radians = Math.toRadians(-yaw);
                final double x = Math.sin(radians) * distance;
                final double z = Math.cos(radians) * distance;
                return new Vector(x, 0.0D, z);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void starlessSweep(final EntityDamageByEntityEvent event, final Player attacker, final LivingEntity target) {
        if (attacker.getAttackCooldown() < 0.8D) return;
        soundService.play(attacker, "stellarity:item.starless_scythe.swing", 0.25F, 1.0F, null);
        final double sweep = event.getDamage() * 0.70D;
        int hits = 0;
        for (final Entity e : target.getNearbyEntities(1.5, 1.5, 1.5)) if (e instanceof LivingEntity l && !l.getUniqueId().equals(attacker.getUniqueId()) && !l.getUniqueId().equals(target.getUniqueId())) { l.damage(sweep, attacker); hits++; }
        final int fortune = attacker.getInventory().getItemInMainHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.FORTUNE);
        final double heal = Math.max(0, event.getDamage() * ((8 + fortune + (hits / 2)) / 100.0D));
        final AttributeInstance maxHealthAttr = attacker.getAttribute(Attribute.MAX_HEALTH);
        final double max = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0D;
        attacker.setHealth(Math.min(max, attacker.getHealth() + heal));
    }

    private void slayerRay(final Player player) {
        drawLaserBranch(player, player.getEyeLocation(), player.getEyeLocation().getDirection(), 36.0D, false);
    }

    private void drawLaserBranch(final Player player, final Location startLoc, final Vector direction, final double maxDistance, final boolean isBranch) {
        final Vector dir = direction.clone().normalize();
        final org.bukkit.World world = startLoc.getWorld();
        if (world == null) return;
        
        final java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        
        Location hitLoc = null;
        LivingEntity hitEntity = null;
        
        for (double d = 0.3D; d <= maxDistance; d += 0.3D) {
            final Location p = startLoc.clone().add(dir.clone().multiply(d));
            
            if (!p.getBlock().isPassable() && !p.getBlock().isLiquid()) {
                hitLoc = p;
                break;
            }
            
            final org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(143, 0, 156), 0.8F);
            world.spawnParticle(Particle.DUST, p, 1, 0.01, 0.01, 0.01, 0.0D, dust);
            
            if (random.nextDouble() < 0.10) {
                world.spawnParticle(Particle.WITCH, p, 1, 0.02, 0.02, 0.02, 0.0D);
            }
            
            for (final Entity e : world.getNearbyEntities(p, 0.45, 0.45, 0.45)) {
                if (e instanceof LivingEntity l && !l.getUniqueId().equals(player.getUniqueId()) && !l.getScoreboardTags().contains("laser")) {
                    hitLoc = p;
                    hitEntity = l;
                    break;
                }
            }
            if (hitLoc != null) {
                break;
            }
            
            if (!isBranch && d >= 1.2D && d <= 18.0D) {
                if (random.nextDouble() < 0.04) {
                    final Vector branchDir = dir.clone().rotateAroundY(Math.toRadians(random.nextDouble(-15, 15)))
                                                      .rotateAroundX(Math.toRadians(random.nextDouble(-10, 10)));
                    drawLaserBranch(player, p, branchDir, 8.0D, true);
                }
            }
        }
        
        if (hitLoc != null) {
            if (hitEntity != null) {
                final LivingEntity finalL = hitEntity;
                finalL.damage(9.0, player);
                finalL.addScoreboardTag("laser");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (finalL.isValid()) finalL.removeScoreboardTag("laser");
                }, 5L);
            }
            
            spawnPrismaticPunchExplosion(hitLoc);
        }
    }

    private void spawnPrismaticPunchExplosion(final Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        final org.bukkit.World world = loc.getWorld();

        final ItemDisplay spark = world.spawn(loc, ItemDisplay.class);
        spark.getPersistentDataContainer().set(new NamespacedKey(plugin, "temp_display"), PersistentDataType.BYTE, (byte) 1);
        spark.setPersistent(false);
        final ItemStack sparkItem = new ItemStack(Material.STICK);
        final ItemMeta sparkMeta = sparkItem.getItemMeta();
        if (sparkMeta != null) {
            sparkMeta.setItemModel(NamespacedKey.fromString("stellarity:_particle/prismatic_punch/spark"));
            sparkItem.setItemMeta(sparkMeta);
        }
        spark.setItemStack(sparkItem);
        spark.setBillboard(Display.Billboard.CENTER);
        spark.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(0, 0, 0, 1),
                new Vector3f(5.0f, 5.0f, 5.0f),
                new Quaternionf(0, 0, 0, 1)
        ));

        final ItemDisplay fireball = world.spawn(loc, ItemDisplay.class);
        fireball.getPersistentDataContainer().set(new NamespacedKey(plugin, "temp_display"), PersistentDataType.BYTE, (byte) 1);
        fireball.setPersistent(false);
        final ItemStack fireballItem = new ItemStack(Material.STICK);
        final ItemMeta fireballMeta = fireballItem.getItemMeta();
        if (fireballMeta != null) {
            fireballMeta.setItemModel(NamespacedKey.fromString("stellarity:_particle/prismatic_punch/fireball"));
            fireballItem.setItemMeta(fireballMeta);
        }
        fireball.setItemStack(fireballItem);
        fireball.setBillboard(Display.Billboard.CENTER);
        fireball.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(0, 0, 0, 1),
                new Vector3f(5.0f, 5.0f, 5.0f),
                new Quaternionf(0, 0, 0, 1)
        ));

        final Location shockLoc = loc.clone();
        shockLoc.setPitch(90.0F);
        shockLoc.setYaw(0.0F);
        final ItemDisplay shockwave = world.spawn(shockLoc, ItemDisplay.class);
        shockwave.getPersistentDataContainer().set(new NamespacedKey(plugin, "temp_display"), PersistentDataType.BYTE, (byte) 1);
        shockwave.setPersistent(false);
        final ItemStack shockItem = new ItemStack(Material.STICK);
        final ItemMeta shockMeta = shockItem.getItemMeta();
        if (shockMeta != null) {
            shockMeta.setItemModel(NamespacedKey.fromString("stellarity:_particle/prismatic_punch/shockwave"));
            shockItem.setItemMeta(shockMeta);
        }
        shockwave.setItemStack(shockItem);
        shockwave.setBillboard(Display.Billboard.FIXED);
        shockwave.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        shockwave.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(0, 0, 0, 1),
                new Vector3f(6.5f, 6.5f, 6.5f),
                new Quaternionf(0, 0, 0, 1)
        ));

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                
                if (spark.isValid()) {
                    if (ticks >= 6) {
                        spark.remove();
                    } else if (ticks >= 3) {
                        final ItemStack is = spark.getItemStack();
                        final ItemMeta m = is.getItemMeta();
                        if (m != null) {
                            m.setCustomModelData(ticks - 2);
                            is.setItemMeta(m);
                            spark.setItemStack(is);
                        }
                    }
                }

                if (fireball.isValid()) {
                    if (ticks >= 9) {
                        fireball.remove();
                    } else {
                        final ItemStack is = fireball.getItemStack();
                        final ItemMeta m = is.getItemMeta();
                        if (m != null) {
                            m.setCustomModelData(ticks);
                            is.setItemMeta(m);
                            fireball.setItemStack(is);
                        }
                    }
                }

                if (shockwave.isValid()) {
                    if (ticks >= 12) {
                        shockwave.remove();
                    } else {
                        final ItemStack is = shockwave.getItemStack();
                        final ItemMeta m = is.getItemMeta();
                        if (m != null) {
                            m.setCustomModelData(ticks);
                            is.setItemMeta(m);
                            shockwave.setItemStack(is);
                        }
                    }
                }

                if (ticks >= 12) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explodePrismatic(final Projectile projectile) {
        final Entity src = projectile.getShooter() instanceof Entity e ? e : projectile;
        final boolean hasRadiant = src instanceof Player p && radiantJewelService != null && radiantJewelService.hasRadiantJewel(p);
        final Location loc = projectile.getLocation().clone();
        doPrismaticBlast(loc, src);
        if (hasRadiant) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> doPrismaticBlast(loc, src), 2L);
        }
        projectile.remove();
    }

    private void doPrismaticBlast(final Location loc, final Entity src) {
        if (loc.getWorld() == null) return;
        for (final Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof LivingEntity l && !(src instanceof Player p && l.getUniqueId().equals(p.getUniqueId()))) {
                l.damage(9, src);
                customStatusEffectService.applyPrismaticInferno(l, 99, src);
            }
        }
        soundService.play(loc, "stellarity:item.prismatic_punch.explode", 2, 1, Sound.ENTITY_GENERIC_EXPLODE);
        spawnPrismaticPunchExplosion(loc);
    }

    private void explodeStellar(final Projectile projectile, final Entity direct) {
        final Entity src = projectile.getShooter() instanceof Entity e ? e : projectile;
        if (direct instanceof LivingEntity l) l.damage(9, src);
        for (final Entity e : projectile.getNearbyEntities(3, 3, 3)) if (e instanceof LivingEntity l && (direct == null || !l.getUniqueId().equals(direct.getUniqueId()))) l.damage(6, src);
        projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, org.bukkit.SoundCategory.PLAYERS, 0.5F, 1.8F);
        projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, org.bukkit.SoundCategory.PLAYERS, 0.5F, 1.0F);
        projectile.remove();
    }

    private void spectralBurst(final Projectile projectile) {
        final Entity src = projectile.getShooter() instanceof Entity e ? e : projectile;
        int hits = 0;
        for (final Entity e : projectile.getNearbyEntities(16, 8, 16)) if (e instanceof LivingEntity l && !(projectile.getShooter() instanceof Player p && l.getUniqueId().equals(p.getUniqueId()))) { l.damage(6.5, src); if (++hits >= 3) break; }
        projectile.remove();
    }

    private void callSplash(final Projectile projectile, final Location hitLoc) {
        final Entity shooter = projectile.getShooter() instanceof Entity e ? e : projectile;
        final org.bukkit.World world = hitLoc.getWorld();
        if (world == null) return;
        
        world.playSound(hitLoc, Sound.BLOCK_GLASS_BREAK, 0.75F, 1.0F);
        world.playSound(hitLoc, Sound.ENTITY_ARROW_HIT, 1.0F, 0.9F);
        
        final java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        final int shardCount = random.nextInt(9, 12);
        
        for (int i = 0; i < shardCount; i++) {
            final double theta = random.nextDouble() * 2 * Math.PI;
            final double phi = Math.acos(2 * random.nextDouble() - 1);
            final double dx = Math.sin(phi) * Math.cos(theta);
            final double dy = Math.sin(phi) * Math.sin(theta);
            final double dz = Math.cos(phi);
            final Vector shardDir = new Vector(dx, dy, dz).normalize();
            
            drawShard(shooter, hitLoc.clone(), shardDir, 3.0D);
        }
        projectile.remove();
    }

    private void drawShard(final Entity shooter, final Location start, final Vector dir, final double maxDist) {
        final org.bukkit.World world = start.getWorld();
        if (world == null) return;
        
        for (double d = 0.2D; d <= maxDist; d += 0.2D) {
            final Location p = start.clone().add(dir.clone().multiply(d));
            if (!p.getBlock().isPassable() && !p.getBlock().isLiquid()) {
                break;
            }
            
            world.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.01, 0.01, 0.01, 0.0D);
            
            for (final Entity e : world.getNearbyEntities(p, 0.35, 0.35, 0.35)) {
                if (e instanceof LivingEntity l) {
                    if (shooter instanceof Player player) {
                        if (l.getUniqueId().equals(player.getUniqueId())) continue;
                    } else {
                        if (l.getUniqueId().equals(shooter.getUniqueId())) continue;
                    }
                    
                    l.damage(2.0D, shooter);
                }
            }
        }
    }

    private void sandstorm(final Projectile projectile) {
        final org.bukkit.Location c = projectile.getLocation().clone();
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (++t > 60 || c.getWorld() == null) {
                    cancel();
                    return;
                }
                c.getWorld().spawnParticle(Particle.CLOUD, c, 8, 1.2, 0.4, 1.2, 0.01, null);
                c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, c, 1, 0.3, 0.1, 0.3, 0.0);
                if (ThreadLocalRandom.current().nextDouble() < 0.15D) {
                    c.getWorld().playSound(c, Sound.ENTITY_BREEZE_SLIDE, 0.5F, 1.0F);
                }
                for (final Entity e : c.getWorld().getNearbyEntities(c, 3.0, 2.0, 3.0)) {
                    if (!(e instanceof LivingEntity l)) continue;
                    if (projectile.getShooter() instanceof Player p && l.getUniqueId().equals(p.getUniqueId())) continue;

                    if (!isUnmovable(l)) {
                        final org.bukkit.util.Vector pull = c.toVector().subtract(l.getLocation().toVector());
                        final double dist = pull.length();
                        if (dist > 0.4) {
                            pull.normalize().multiply(0.2);
                            pull.setY(0.08);
                            l.setVelocity(l.getVelocity().multiply(0.5).add(pull));
                        }
                    }

                    l.damage(1, projectile.getShooter() instanceof Entity src ? src : projectile);
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private boolean isUnmovable(final LivingEntity entity) {
        if (entity instanceof Player player && player.isSneaking()) {
            return true;
        }
        if (entity instanceof org.bukkit.entity.ArmorStand) {
            return true;
        }
        if (entity instanceof org.bukkit.entity.EnderDragon
                || entity instanceof org.bukkit.entity.Wither
                || entity instanceof org.bukkit.entity.Warden
                || entity instanceof org.bukkit.entity.ElderGuardian
                || entity instanceof org.bukkit.entity.ComplexLivingEntity) {
            return true;
        }
        final java.util.Set<String> tags = entity.getScoreboardTags();
        return tags.contains("stellarity.boss")
                || tags.contains("stellarity.sandstorm_trident_unmovable")
                || tags.contains("stellarity.empress_of_light")
                || tags.contains("stellarity.shulking")
                || tags.contains("stellarity.in_wind_tunnel");
    }

    private void throwFluffyHammer(final Player player) {
        final long now = System.currentTimeMillis();
        if (now < fluffyCooldown.getOrDefault(player.getUniqueId(), 0L)) return;
        
        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) return;
        
        final ItemStack hammerStack = item.clone();
        
        player.getInventory().setItemInMainHand(null);
        
        final Location startLoc = player.getEyeLocation();
        final Vector startDir = player.getLocation().getDirection().normalize();
        
        final org.bukkit.entity.ArmorStand base = player.getWorld().spawn(startLoc, org.bukkit.entity.ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setMarker(true);
            stand.setAI(false);
            stand.setSilent(true);
        });
        
        final org.bukkit.entity.ItemDisplay display = player.getWorld().spawn(startLoc, org.bukkit.entity.ItemDisplay.class, d -> {
            d.setItemStack(hammerStack);
            d.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            d.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0f, 0f, 0f),
                    new org.joml.Quaternionf(0, 0, 0, 1),
                    new org.joml.Vector3f(1.0f, 1.0f, 1.0f),
                    new org.joml.Quaternionf(0, 0, 0, 1)
            ));
        });
        
        base.addPassenger(display);
        
        soundService.play(player, "stellarity:item.fluffy_hammer.throw", 1, 1, Sound.ITEM_TRIDENT_THROW);
        
        fluffyCooldown.put(player.getUniqueId(), now + 1000L);
        
        final boolean hasRadiant = radiantJewelService != null && radiantJewelService.hasRadiantJewel(player);
        final double step = hasRadiant ? 2.4D : 1.2D;
        final double returnStep = hasRadiant ? 2.7D : 1.35D;
        final int maxOutTicks = hasRadiant ? 17 : 33;

        new org.bukkit.scheduler.BukkitRunnable() {
            private int t = 0;
            private boolean returning = false;
            private Vector currentDir = startDir.clone();
            private final java.util.Set<UUID> hitEntities = new java.util.HashSet<>();
            
            @Override
            public void run() {
                t++;
                
                if (!base.isValid() || !display.isValid() || !player.isOnline()) {
                    cleanup();
                    return;
                }
                
                final float yaw = (t * 20.0F) % 360.0F;
                display.setRotation(yaw, 90.0F);
                
                if (t % 5 == 1) {
                    player.getWorld().playSound(base.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8F, 1.1F);
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, base.getLocation().add(0, 0.2, 0), 1, 0D, 0D, 0D, 0D);
                }
                
                if (!returning) {
                    final Location nextLoc = base.getLocation().clone().add(currentDir.clone().multiply(step));
                    if (!nextLoc.getBlock().isPassable() && !nextLoc.getBlock().isLiquid()) {
                        returning = true;
                    }
                    
                    base.teleport(base.getLocation().add(currentDir.clone().multiply(step)));
                    
                    if (base.getLocation().distanceSquared(startLoc) >= 1600.0D || t >= maxOutTicks) {
                        returning = true;
                    }
                } else {
                    final Vector toPlayer = player.getEyeLocation().toVector().subtract(base.getLocation().toVector());
                    final double distSq = toPlayer.lengthSquared();
                    
                    if (distSq <= 2.25D) {
                        cleanup();
                        
                        if (hammerStack.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
                            damageable.setDamage(damageable.getDamage() + 1);
                            hammerStack.setItemMeta(damageable);
                            
                            if (damageable.getDamage() >= hammerStack.getType().getMaxDurability()) {
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                                return;
                            }
                        }
                        
                        soundService.play(player, "stellarity:item.fluffy_hammer.return", 1, 1, Sound.ITEM_TRIDENT_RETURN);
                        
                        final java.util.HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(hammerStack);
                        if (!remaining.isEmpty()) {
                            for (final ItemStack drop : remaining.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                            }
                        }
                        return;
                    }
                    
                    currentDir = toPlayer.normalize();
                    base.teleport(base.getLocation().add(currentDir.clone().multiply(returnStep)));
                }
                
                for (final Entity e : base.getNearbyEntities(1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity l && !l.getUniqueId().equals(player.getUniqueId()) && !hitEntities.contains(l.getUniqueId())) {
                        l.damage(10.0D, player);
                        hitEntities.add(l.getUniqueId());
                        l.getWorld().spawnParticle(Particle.SWEEP_ATTACK, l.getLocation().add(0, 1, 0), 1, 0D, 0D, 0D, 0D);
                    }
                }
            }
            
            private void cleanup() {
                this.cancel();
                if (display.isValid()) display.remove();
                if (base.isValid()) base.remove();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void spiritDaggerBlink(final Player player) {
        if (!player.isSneaking()) return;
        final LivingEntity target = rayTarget(player, 12); if (target == null) return;
        final Vector back = target.getLocation().getDirection().normalize().multiply(-1.1);
        player.teleport(target.getLocation().clone().add(back).add(0, 0.1, 0));
        soundService.play(player, "stellarity:item.spirit_dagger.teleport", 1, 1, Sound.ITEM_CHORUS_FRUIT_TELEPORT);
        for (final Entity e : target.getNearbyEntities(3.8, 2, 3.8)) if (e instanceof LivingEntity l && !l.getUniqueId().equals(player.getUniqueId())) l.damage(5, player);
    }

    private boolean isSpellbook(final String itemId) {
        return ITEM_BOOK_CONVEYANCE.equalsIgnoreCase(itemId)
                || ITEM_BOOK_JINX.equalsIgnoreCase(itemId)
                || ITEM_BOOK_LIGHT.equalsIgnoreCase(itemId)
                || ITEM_BOOK_OBSTRUCT.equalsIgnoreCase(itemId)
                || ITEM_BOOK_RETURN.equalsIgnoreCase(itemId)
                || ITEM_BOOK_UPDRAFT.equalsIgnoreCase(itemId);
    }

    private void castSpellbook(final Player player, final String itemId) {
        if (!itemsConfig.getWeaponsConfig().getBoolean(itemId + ".enabled", true)) {
            player.sendMessage(messageService.message("spellbooks.disabled"));
            return;
        }

        final long now = System.currentTimeMillis();
        if (now < spellbookCooldown.getOrDefault(player.getUniqueId(), 0L)) {
            return;
        }

        if (ITEM_BOOK_OBSTRUCT.equalsIgnoreCase(itemId)) {
            final double[][] coords = {
                {0, 1.0, 3.0}, {0, -1.0, 3.0},
                {-1.0, 0.99, 3.0}, {-1.0, -0.99, 3.0},
                {-2.0, 0.98, 3.0}, {-2.0, -0.98, 3.0},
                {-3.0, 0.97, 3.0}, {-3.0, -0.97, 3.0},
                {1.0, 0.99, 3.0}, {1.0, -0.99, 3.0},
                {2.0, 0.98, 3.0}, {2.0, -0.98, 3.0},
                {3.0, 0.97, 3.0}, {3.0, -0.97, 3.0}
            };

            for (final List<List<Shulker>> playerWalls : activeObstructWalls.values()) {
                for (final List<Shulker> existingWall : playerWalls) {
                    for (final Shulker existingShulker : existingWall) {
                        if (existingShulker.isValid()) {
                            for (final double[] coord : coords) {
                                final Location spawnLoc = getLocalLocation(player, coord[0], coord[1], coord[2]);
                                if (spawnLoc.getWorld().equals(existingShulker.getWorld()) && spawnLoc.distance(existingShulker.getLocation()) < 3.0D) {
                                    player.sendMessage(messageService.message("spellbooks.obstruct.too_close"));
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            if (dev.cosmojar.stellaritypaper.integration.WorldGuardHook.isEnabled()) {
                if (!dev.cosmojar.stellaritypaper.integration.WorldGuardHook.canBuild(player, player.getLocation())) {
                    return;
                }
                for (final double[] coord : coords) {
                    final Location spawnLoc = getLocalLocation(player, coord[0], coord[1], coord[2]);
                    if (!dev.cosmojar.stellaritypaper.integration.WorldGuardHook.canBuild(player, spawnLoc)) {
                        return;
                    }
                }
            }

            for (final double[] coord : coords) {
                final Location spawnLoc = getLocalLocation(player, coord[0], coord[1], coord[2]);
                final org.bukkit.block.Block placedBlock = spawnLoc.getBlock();
                final org.bukkit.block.BlockState replacedBlockState = placedBlock.getState();
                final org.bukkit.block.Block placedAgainst = placedBlock.getRelative(org.bukkit.block.BlockFace.DOWN);
                final org.bukkit.inventory.ItemStack itemInHand = player.getInventory().getItemInMainHand();
                
                final org.bukkit.event.block.BlockPlaceEvent placeEvent = new org.bukkit.event.block.BlockPlaceEvent(
                    placedBlock,
                    replacedBlockState,
                    placedAgainst,
                    itemInHand,
                    player,
                    true,
                    org.bukkit.inventory.EquipmentSlot.HAND
                );
                org.bukkit.Bukkit.getPluginManager().callEvent(placeEvent);
                if (placeEvent.isCancelled()) {
                    return;
                }
            }
        }

        final double cooldownSec = itemsConfig.getWeaponsConfig().getDouble(itemId + ".cooldown-seconds", 7.0D);
        final long cooldownMs = (long) (cooldownSec * 1000.0D);
        final int cooldownTicks = (int) (cooldownSec * 20.0D);

        final UUID uuid = player.getUniqueId();
        spellbookCooldown.put(uuid, now + cooldownMs);
        player.setCooldown(Material.WARPED_FUNGUS_ON_A_STICK, cooldownTicks);
        soundService.play(player, "stellarity:item.spellbook_cast", 1.0F, 1.0F, Sound.ITEM_BOOK_PAGE_TURN);

        final BukkitTask[] actionTaskHolder = new BukkitTask[1];
        actionTaskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                if (actionTaskHolder[0] != null) actionTaskHolder[0].cancel();
                return;
            }
            final long currentNow = System.currentTimeMillis();
            final long end = spellbookCooldown.getOrDefault(uuid, 0L);
            if (currentNow >= end) {
                player.sendActionBar(messageService.message("spellbooks.ready"));
                if (actionTaskHolder[0] != null) actionTaskHolder[0].cancel();
                return;
            }
            double left = (end - currentNow) / 1000.0D;
            player.sendActionBar(messageService.message("spellbooks.cooldown_actionbar", Map.of("seconds", String.format(java.util.Locale.US, "%.1f", left))));
        }, 0L, 5L);

        if (ITEM_BOOK_CONVEYANCE.equalsIgnoreCase(itemId)) {
            final Location startLoc = player.getEyeLocation();
            final Vector startDir = player.getLocation().getDirection().normalize();
            
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 0.8F, 1.3F);

            final BukkitTask[] taskHolder = new BukkitTask[1];
            final double projSpeed = itemsConfig.getWeaponsConfig().getDouble("book_of_conveyance.projectile-speed", 1.1D);
            final double maxDistance = itemsConfig.getWeaponsConfig().getDouble("book_of_conveyance.max-distance", 20.0D);
            final double maxDistanceSq = maxDistance * maxDistance;
            
            taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                private final Location currentLoc = startLoc.clone();
                private Vector currentDir = startDir.clone();
                private int ticks = 0;
                
                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= 120) {
                        if (taskHolder[0] != null) taskHolder[0].cancel();
                        return;
                    }
                    ticks++;

                    final Vector targetDir = player.getLocation().getDirection().normalize();
                    currentDir = currentDir.multiply(0.85D).add(targetDir.multiply(0.15D)).normalize();
                    
                    currentLoc.add(currentDir.clone().multiply(projSpeed));

                    if (currentLoc.distanceSquared(startLoc) > maxDistanceSq) {
                        if (taskHolder[0] != null) taskHolder[0].cancel();
                        return;
                    }

                    final org.bukkit.Color fromColor = org.bukkit.Color.fromRGB(230, 31, 255);
                    final org.bukkit.Color toColor = org.bukkit.Color.fromRGB(111, 0, 255);
                    final Particle.DustTransition transition = new Particle.DustTransition(fromColor, toColor, 1.05F);
                    currentLoc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, currentLoc, 4, 0.1D, 0.1D, 0.1D, 0.0D, transition);

                    final Location nextLoc = currentLoc.clone().add(currentDir.clone().multiply(projSpeed));
                    if (nextLoc.getBlock().getType().isSolid()) {
                        final Location tpLoc = currentLoc.clone();
                        tpLoc.setDirection(player.getLocation().getDirection());

                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, true, false, true));
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                        player.teleport(tpLoc);
                        player.getWorld().playSound(tpLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                        
                        final Particle.DustTransition hitTransition = new Particle.DustTransition(fromColor, toColor, 1.0F);
                        tpLoc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, tpLoc.clone().add(0.0D, 1.0D, 0.0D), 80, 0.3D, 0.55D, 0.3D, 0.0D, hitTransition);
                        
                        if (taskHolder[0] != null) taskHolder[0].cancel();
                    }
                }
            }, 0L, 1L);
            return;
        }

        if (ITEM_BOOK_JINX.equalsIgnoreCase(itemId)) {
            final AreaEffectCloud cloud = (AreaEffectCloud) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.AREA_EFFECT_CLOUD);
            cloud.setRadius(1.5F);
            cloud.setRadiusOnUse(0.08F);
            cloud.setRadiusPerTick(0.04F);
            cloud.setDuration(80);
            cloud.setParticle(Particle.WITCH);
            cloud.setVelocity(player.getLocation().getDirection().normalize().multiply(0.4D));

            Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks++ > 16 || !cloud.isValid()) return;
                    for (final Entity entity : cloud.getNearbyEntities(cloud.getRadius(), 2.0D, cloud.getRadius())) {
                        if (entity instanceof LivingEntity target && !target.getUniqueId().equals(player.getUniqueId())) {
                            harvesterMechanicService.applyJinx(target);
                        }
                    }
                }
            }, 1L, 5L);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0F, 0.8F);
            return;
        }

        if (ITEM_BOOK_LIGHT.equalsIgnoreCase(itemId)) {
            final Location loc = player.getLocation().getBlock().getLocation().add(0.5D, 0.0D, 0.5D);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.4F);

            new org.bukkit.scheduler.BukkitRunnable() {
                int ticks = 0;
                final int durationSeconds = itemsConfig.getWeaponsConfig().getInt("book_of_light.beacon-duration-seconds", 20);
                final int maxTicks = durationSeconds * 20;
                
                @Override
                public void run() {
                    if (ticks++ > maxTicks || loc.getWorld() == null) {
                        this.cancel();
                        return;
                    }

                    if (ticks % 100 == 1) {
                        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.5F, 1.5F);
                    }
                    
                    final org.bukkit.World world = loc.getWorld();
                    final double angle1 = Math.toRadians(ticks * 2.5D);
                    final double angle2 = Math.toRadians(-ticks * 2.5D);
                    
                    final org.bukkit.Particle.DustOptions goldDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 221, 0), 1.25F);
                    
                    for (int i = 0; i < 3; i++) {
                        final double degree = angle1 + Math.toRadians(i * 120.0D);
                        final double x = Math.cos(degree) * 6.0D;
                        final double z = Math.sin(degree) * 6.0D;
                        final Location pLoc = loc.clone().add(x, 0.1D, z);
                        world.spawnParticle(Particle.END_ROD, pLoc, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        world.spawnParticle(Particle.DUST, pLoc, 2, 0.1D, 0.1D, 0.1D, 0.0D, goldDust);
                    }
                    
                    for (int i = 0; i < 3; i++) {
                        final double degree = angle2 + Math.toRadians(i * 120.0D);
                        final double x = Math.cos(degree) * 7.0D;
                        final double z = Math.sin(degree) * 7.0D;
                        final Location pLoc = loc.clone().add(x, 0.1D, z);
                        world.spawnParticle(Particle.END_ROD, pLoc, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        world.spawnParticle(Particle.DUST, pLoc, 2, 0.1D, 0.1D, 0.1D, 0.0D, goldDust);
                    }
                    
                    if (ticks % 2 == 0) {
                        final java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
                        for (int i = 0; i < 2; i++) {
                            final double r = random.nextDouble(0.0D, 7.0D);
                            final double theta = random.nextDouble() * 2 * Math.PI;
                            final double x = Math.cos(theta) * r;
                            final double z = Math.sin(theta) * r;
                            world.spawnParticle(Particle.DUST, loc.clone().add(x, 0.1D + random.nextDouble(-0.3, 1.2), z), 1, 0.0D, 0.0D, 0.0D, 0.0D, goldDust);
                        }
                        if (random.nextDouble() < 0.3) {
                            final double r = random.nextDouble(0.0D, 7.0D);
                            final double theta = random.nextDouble() * 2 * Math.PI;
                            final double x = Math.cos(theta) * r;
                            final double z = Math.sin(theta) * r;
                            world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, loc.clone().add(x, 0.1D, z), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        }
                    }
                    
                    if (ticks % 40 == 0) {
                        for (int step = 0; step < 4; step++) {
                            final int finalStep = step;
                            final double radius = 1.0D + finalStep * 2.0D;
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (loc.getWorld() == null) return;
                                for (int d = 0; d < 360; d += 5) {
                                    final double rad = Math.toRadians(d);
                                    final double x = Math.cos(rad) * radius;
                                    final double z = Math.sin(rad) * radius;
                                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x, 0.1D, z), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                                }
                            }, step * 3L);
                        }
                    }

                    if (ticks % 20 == 0) {
                        for (final Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(loc.getWorld()) && 
                                Math.pow(p.getLocation().getX() - loc.getX(), 2) + Math.pow(p.getLocation().getZ() - loc.getZ(), 2) <= 49.0D && 
                                Math.abs(p.getLocation().getY() - loc.getY()) <= 3.0D) {
                                
                                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, true));
                                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true));
                                p.removePotionEffect(PotionEffectType.WEAKNESS);
                                p.removePotionEffect(PotionEffectType.SLOWNESS);

                                setModifier(p, Attribute.KNOCKBACK_RESISTANCE, lightKnockbackKey, 0.15D, AttributeModifier.Operation.ADD_NUMBER);
                                
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    if (p.isOnline()) removeModifier(p, Attribute.KNOCKBACK_RESISTANCE, lightKnockbackKey);
                                }, 40L);
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);
            return;
        }

        if (ITEM_BOOK_OBSTRUCT.equalsIgnoreCase(itemId)) {
            final double[][] coords = {
                {0, 1.0, 3.0}, {0, -1.0, 3.0},
                {-1.0, 0.99, 3.0}, {-1.0, -0.99, 3.0},
                {-2.0, 0.98, 3.0}, {-2.0, -0.98, 3.0},
                {-3.0, 0.97, 3.0}, {-3.0, -0.97, 3.0},
                {1.0, 0.99, 3.0}, {1.0, -0.99, 3.0},
                {2.0, 0.98, 3.0}, {2.0, -0.98, 3.0},
                {3.0, 0.97, 3.0}, {3.0, -0.97, 3.0}
            };

            final List<Shulker> wall = new ArrayList<>();
            for (final double[] coord : coords) {
                final Location spawnLoc = getLocalLocation(player, coord[0], coord[1], coord[2]);
                final Shulker shulker = (Shulker) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SHULKER);
                shulker.setAI(false);
                shulker.setSilent(true);
                shulker.setInvisible(false);
                shulker.setInvulnerable(true);
                final AttributeInstance scaleAttr = shulker.getAttribute(Attribute.SCALE);
                if (scaleAttr != null) {
                    scaleAttr.setBaseValue(2.0D);
                }
                shulker.getPersistentDataContainer().set(obstructWallKey, PersistentDataType.BOOLEAN, true);
                shulker.getPersistentDataContainer().set(obstructOwnerKey, PersistentDataType.STRING, uuid.toString());
                
                spawnLoc.getWorld().spawnParticle(Particle.PORTAL, spawnLoc.add(0.0D, 0.5D, 0.0D), 6, 0.2D, 0.2D, 0.2D, 0.1D, null);
                wall.add(shulker);
            }

            activeObstructWalls.computeIfAbsent(uuid, k -> new ArrayList<>()).add(wall);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8F, 0.6F);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.0F, 1.2F);

            final int wallSec = itemsConfig.getWeaponsConfig().getInt("book_of_obstruct.wall-duration-seconds", 10);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                final List<List<Shulker>> playerWalls = activeObstructWalls.get(uuid);
                if (playerWalls != null) {
                    playerWalls.remove(wall);
                    if (playerWalls.isEmpty()) {
                        activeObstructWalls.remove(uuid);
                    }
                }
                for (final Shulker shulker : wall) {
                    if (shulker.isValid()) {
                        triggerObstructExplosion(shulker);
                    }
                }
            }, wallSec * 20L);
            return;
        }

        if (ITEM_BOOK_RETURN.equalsIgnoreCase(itemId)) {
            final Location startLoc = player.getLocation();
            
			float startPitch = 0.6F + java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 0.2F;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0F, startPitch);
            final int durationSec = itemsConfig.getWeaponsConfig().getInt("book_of_return.channeling-duration-seconds", 10);
            player.sendMessage(messageService.message("spellbooks.return.start", java.util.Map.of("duration", String.valueOf(durationSec))));

            final BukkitTask[] taskHolder = new BukkitTask[1];
            taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                double progress = 0.0D;
                final double step = 50.0D / durationSec;
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancelCast();
                        return;
                    }

                    final double dist = player.getLocation().distanceSquared(startLoc);
                    if (dist > 0.01D) {
                        player.sendMessage(messageService.message("spellbooks.return.cancelled"));
						float randomPitch = 0.8F + java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 0.4F;
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1.0F, randomPitch);
                        cancelCast();
                        return;
                    }

                    progress += step;
                    final int percentage = (int) (progress * 100.0D / 200.0D);
                    player.sendActionBar(messageService.message("spellbooks.return.actionbar_progress", java.util.Map.of("progress", String.valueOf(percentage))));

                    player.getWorld().spawnParticle(
                            Particle.PORTAL,
                            player.getLocation().add(0.0D, 0.5D, 0.0D),
                            12, 0.3D, 0.3D, 0.3D, 0.1D, null
                    );

                    final float pitch = 0.5F + (float) (progress / 200.0D);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4F, pitch);

                    if (progress >= 200) {
                        final Location respawn = player.getRespawnLocation();
                        final Location target = respawn != null ? respawn : player.getWorld().getSpawnLocation();
                        
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.8F, 1.3F);
                        player.teleport(target);
                        player.getWorld().playSound(target, Sound.BLOCK_PORTAL_TRAVEL, 0.1F, 1.0F);
                        player.sendMessage(messageService.message("spellbooks.return.success"));
                        cancelCast();
                    }
                }

                private void cancelCast() {
                    activeReturnCasts.remove(uuid);
                    if (taskHolder[0] != null) {
                        taskHolder[0].cancel();
                    }
                }
            }, 1L, 5L);

            activeReturnCasts.put(uuid, taskHolder[0]);
            return;
        }

        if (ITEM_BOOK_UPDRAFT.equalsIgnoreCase(itemId)) {
            player.setVelocity(player.getVelocity().setY(1.0D));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.8F, 1.0F);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0F, 0.8F);

            setModifier(player, Attribute.GRAVITY, updraftGravityKey, -0.95D, AttributeModifier.Operation.ADD_SCALAR);
            setModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, updraftFallDamageKey, -1.0D, AttributeModifier.Operation.ADD_SCALAR);
            player.getPersistentDataContainer().set(updraftFloatKey, PersistentDataType.BOOLEAN, true);

            for (int wave = 1; wave <= 3; wave++) {
                final double radius = 3.0D + (1.5D * (wave - 1));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;

                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.6F, 1.3F);
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, radius * 0.5D, 0.2D, radius * 0.5D, 0.05D, null);

                    for (final Entity entity : player.getNearbyEntities(radius, 3.0D, radius)) {
                        if (entity instanceof LivingEntity living && !living.getUniqueId().equals(player.getUniqueId())) {
                            living.setVelocity(living.getVelocity().setY(0.7D));
                        }
                    }
                }, wave * 2L);
            }
        }
    }

    private void triggerObstructExplosion(final Shulker shulker) {
        final Location loc = shulker.getLocation();
        shulker.remove();

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.9F, 1.2F);
        loc.getWorld().playSound(loc, Sound.ENTITY_SHULKER_DEATH, 0.8F, 0.8F);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.add(0.0D, 0.5D, 0.0D), 2, 0.2D, 0.2D, 0.2D, 0.0D, null);


        for (final Entity e : loc.getWorld().getNearbyEntities(loc, 4.0D, 3.0D, 4.0D)) {
            if (e instanceof LivingEntity living) {
                final Vector diff = living.getLocation().toVector().subtract(loc.toVector());
                final Vector knock;
                if (diff.lengthSquared() > 1.0E-4D) {
                    knock = diff.normalize().multiply(1.2D).setY(0.4D);
                } else {
                    knock = new Vector(1.0D, 0.0D, 0.0D).multiply(1.2D).setY(0.4D);
                }
                living.setVelocity(knock);
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
            }
        }
    }

    public void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final Boolean isFloating = player.getPersistentDataContainer().get(updraftFloatKey, PersistentDataType.BOOLEAN);
        if (Boolean.TRUE.equals(isFloating)) {
            if (player.isOnGround() || player.getLocation().getBlock().isLiquid()) {
                player.getPersistentDataContainer().set(updraftFloatKey, PersistentDataType.BOOLEAN, false);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        removeModifier(player, Attribute.GRAVITY, updraftGravityKey);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.8F, 1.2F);
                    }
                }, 2L);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.getPersistentDataContainer().remove(updraftFloatKey);
                        removeModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, updraftFallDamageKey);
                    }
                }, 20L);
            }
        }
    }

    public void onPlayerDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && Boolean.TRUE.equals(player.getPersistentDataContainer().get(updraftFloatKey, PersistentDataType.BOOLEAN))) {
            event.setCancelled(true);
            player.getPersistentDataContainer().remove(updraftFloatKey);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    removeModifier(player, Attribute.GRAVITY, updraftGravityKey);
                    removeModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, updraftFallDamageKey);
                }
            }, 20L);
        }
    }

    private Location getLocalLocation(final Player player, final double leftOffset, final double upOffset, final double forwardOffset) {
        final Location loc = player.getLocation().clone();
        loc.setPitch(0.0F);
        final Vector F = loc.getDirection().normalize();
        final Vector L = new Vector(-F.getZ(), 0.0D, F.getX()).normalize();
        return loc.add(L.multiply(leftOffset))
                  .add(0.0D, upOffset, 0.0D)
                  .add(F.multiply(forwardOffset));
    }

    private boolean isSpiritCombo(final Player player) {
        final String main = itemId(player);
        final String off = itemStateRepository.getItemId(player.getInventory().getItemInOffHand()).orElse("");
        return (ITEM_THE_BEGINNING.equalsIgnoreCase(main) && ITEM_THE_END.equalsIgnoreCase(off))
                || (ITEM_THE_BEGINNING.equalsIgnoreCase(off) && ITEM_THE_END.equalsIgnoreCase(main));
    }

    private String itemId(final Player player) { return itemStateRepository.getItemId(player.getInventory().getItemInMainHand()).orElse(""); }
    private void setKind(final Projectile projectile, final String kind) { projectile.getPersistentDataContainer().set(projectileKindKey, PersistentDataType.STRING, kind); }
    private LivingEntity rayTarget(final Player player, final double range) {
        final RayTraceResult hit = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range, 0.35,
                e -> e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId()));
        return hit != null && hit.getHitEntity() instanceof LivingEntity l ? l : null;
    }
    private Player resolvePlayer(final Entity damager) { return damager instanceof Player p ? p : damager instanceof Projectile pr && pr.getShooter() instanceof Player p ? p : null; }
    private int stars(final int collect) { int s = 0; if (collect >= 1000) s++; if (collect >= 2000) s++; if (collect >= 4500) s++; if (collect >= 7000) s++; if (collect >= 10000) s++; return s; }
    private Vector rotateYaw(final Vector v, final double deg) { final double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r); return new Vector((v.getX() * c) - (v.getZ() * s), v.getY(), (v.getX() * s) + (v.getZ() * c)).normalize(); }
    private boolean isRightClick(final Action a) { return a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK; }
    private boolean isLeftClick(final Action a) { return a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK; }
    private State state(final Player player) { return states.computeIfAbsent(player.getUniqueId(), k -> new State()); }
    private void stopTask(final UUID id, final boolean clear) { final BukkitTask task = playerTasks.remove(id); if (task != null) task.cancel(); if (clear) states.remove(id); }

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

    private boolean isReallyOnGround(final AbstractArrow arrow) {
        if (!arrow.isOnGround()) {
            return false;
        }
        final Location loc = arrow.getLocation();
        final org.bukkit.block.Block current = loc.getBlock();
        if (current.getType().isSolid()) {
            return true;
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (loc.clone().add(x * 0.3D, y * 0.3D, z * 0.3D).getBlock().getType().isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static final class State {
        int stellarCollect;
        int stellarCooldown;
        int prismemberCharge;
        int prismGate;
        int prismReset;
        int prismemberCooldown;
        int tamarisTicks;
        int harvesterAmbientTicks;
        boolean tamarisNoWarn;
    }
}
