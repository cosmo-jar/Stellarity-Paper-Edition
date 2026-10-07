package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Реализация technical_draconic:
 * стаки на цели, окно сброса, подготовка панча и рывок с уроном.
 */
public final class DragonbladeMechanicService implements org.bukkit.event.Listener {

    private static final String DRAGONBLADE_ITEM_ID = "dragonblade";
    private static final int STACK_RESET_TICKS = 320;
    private static final int PUNCH_COOLDOWN_TICKS = 160;

    private final org.bukkit.plugin.Plugin plugin;
    private final EnchantActiveService activeService;
    private final ItemStateRepository itemStateRepository;
    private final CustomStatusEffectService customStatusEffectService;
    private final StellaritySoundService soundService;
    private final dev.cosmojar.stellaritypaper.config.ItemsConfigService itemsConfigService;
    private final org.bukkit.NamespacedKey trueDamageKey;

    private final Map<UUID, TargetState> states = new HashMap<>();
    private BukkitTask tickTask;

    public DragonbladeMechanicService(
            final org.bukkit.plugin.Plugin plugin,
            final EnchantActiveService activeService,
            final ItemStateRepository itemStateRepository,
            final CustomStatusEffectService customStatusEffectService,
            final StellaritySoundService soundService,
            final dev.cosmojar.stellaritypaper.config.ItemsConfigService itemsConfigService
    ) {
        this.plugin = plugin;
        this.activeService = activeService;
        this.itemStateRepository = itemStateRepository;
        this.customStatusEffectService = customStatusEffectService;
        this.soundService = soundService;
        this.itemsConfigService = itemsConfigService;
        this.trueDamageKey = new org.bukkit.NamespacedKey(plugin, "stellarity_true_damage");
    }

    public void clearAll() {
        states.clear();
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    public void onDraconicMeleeHit(
            final EntityDamageByEntityEvent event,
            final Player attacker,
            final LivingEntity victim
    ) {
        if (!(event.getDamager() instanceof Player) || !isDragonbladeInMainHand(attacker)) {
            return;
        }

        if (isBoss(victim)) {
            event.setDamage(event.getDamage() * 1.75D);
            return;
        }

        final TargetState state = states.computeIfAbsent(victim.getUniqueId(), ignored -> new TargetState());
        if (state.cooldownTicks > 0 || state.untilStackResetTicks >= STACK_RESET_TICKS) {
            ensureTickTask();
            return;
        }

        if (state.stacks < 3) {
            state.stacks++;
        }
        state.untilStackResetTicks = STACK_RESET_TICKS;
        ensureTickTask();

        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 0.86F, 0.75F);
        if (state.stacks == 3) {
            if (!state.punchReady) {
                victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0F, 1.7F);
                soundService.play(victim, "stellarity:item.dragonblade.prepare_punch", 1.0F, 1.0F, null);
            }
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.4F, 1.0F);
            state.punchReady = true;
            state.stacks = 4;
        }
    }

    private void ensureTickTask() {
        if (tickTask == null) {
            tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    private void tick() {
        final Iterator<Map.Entry<UUID, TargetState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, TargetState> entry = iterator.next();
            final Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()) {
                iterator.remove();
                continue;
            }

            final TargetState state = entry.getValue();
            if (state.cooldownTicks > 0) {
                state.cooldownTicks--;
                target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0D, 1.0D, 0.0D), 2, 0.0D, 0.0D, 0.0D, 0.03D);
            }
            if (state.untilStackResetTicks > 0) {
                state.untilStackResetTicks--;
                if (state.untilStackResetTicks <= 0) {
                    resetStacks(state);
                } else {
                    spawnStackParticles(target, state.stacks);
                }
            }
            if (state.untilPunchResetTicks > 0) {
                state.untilPunchResetTicks--;
                if (state.untilPunchResetTicks == 0) {
                    state.punchProgress = 0;
                }
            }
            if (state.punchReady) {
                processPunchProgress(target, state);
            }

            final boolean idle = state.cooldownTicks <= 0
                    && state.untilStackResetTicks <= 0
                    && !state.punchReady
                    && state.punchProgress <= 0;
            if (idle) {
                iterator.remove();
            }
        }

        if (states.isEmpty() && tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void processPunchProgress(final LivingEntity target, final TargetState state) {
        final Player activator = findActivator(target);
        if (activator == null) {
            return;
        }

        state.punchTickGate++;
        if (state.punchTickGate % 3 != 0) {
            return;
        }
        state.punchProgress++;
        state.untilPunchResetTicks = 2;

        if (state.punchProgress == 1) {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.15F);
            soundService.play(target, "stellarity:item.dragonblade.prepare_punch", 1.0F, 1.0F, null);
        } else if (state.punchProgress == 5) {
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 0.75F);
            soundService.play(target, "stellarity:item.dragonblade.pre_punch", 1.0F, 1.0F, null);
        } else if (state.punchProgress >= 16) {
            executePunch(target, activator, state);
        }
    }

    private void executePunch(final LivingEntity target, final Player activator, final TargetState state) {
        final int knockbackLevel = readKnockbackLevel(activator.getInventory().getItemInMainHand());
        state.cooldownTicks = PUNCH_COOLDOWN_TICKS;
        resetStacks(state);
        state.punchProgress = 0;
        state.untilPunchResetTicks = 0;

        double maxDistance = (78.0D + (14.0D * knockbackLevel)) / 10.0D;
        final AttributeInstance knockResAttr = target.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        final double knockbackResistance = knockResAttr == null ? 0.0D : knockResAttr.getValue();
        maxDistance *= Math.max(0.0D, 1.0D - (knockbackResistance * 0.5D));

        Location cursor = target.getLocation().clone().add(0.0D, 0.5D, 0.0D);
        Vector direction = cursor.toVector().subtract(activator.getEyeLocation().toVector());
        if (direction.lengthSquared() < 1.0E-8D) {
            direction = activator.getLocation().getDirection();
        }
        direction = direction.normalize();

        boolean hitBlock = false;
        double travelled = 0.0D;
        while (travelled < maxDistance) {
            final Location next = cursor.clone().add(direction.clone().multiply(0.1D));
            if (!next.getBlock().isPassable() && !next.getBlock().isLiquid()) {
                hitBlock = true;
                break;
            }
            cursor = next;
            travelled += 0.1D;
            if (next.getBlock().isLiquid()) {
                travelled += 0.1D;
            }
        }

        target.teleport(cursor.clone().add(0.0D, -0.5D, 0.0D));
        target.getWorld().spawnParticle(Particle.DRAGON_BREATH, target.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.2D, 0.2D, 0.2D, 0.01D);
        soundService.play(target, "stellarity:item.dragonblade.punch", 1.0F, 1.0F, null);

        if (hitBlock) {
            applyPunchSplash(target, activator, knockbackLevel);
            applyPunchHitBlock(target, activator, knockbackLevel);
            return;
        }
        applyPunchNormal(target, activator, knockbackLevel);
    }

    private void applyPunchNormal(final LivingEntity target, final Player activator, final int knockbackLevel) {
        soundService.play(target, "stellarity:item.dragonblade.damage", 1.0F, 1.0F, null);
        if (!isBoss(target)) {
            customStatusEffectService.applyVoided(target, 160, 1);
        }
        final double damage = 70.0D + (knockbackLevel * 15.0D);
        final boolean trueDamageEnabled = itemsConfigService.getWeaponsConfig().getBoolean("dragonblade.true-damage-enabled", true);
        if (trueDamageEnabled) {
            target.getPersistentDataContainer().set(trueDamageKey, org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
        }
        target.damage(damage, activator);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 6, true, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 140, 0, true, false, true));
    }

    private void applyPunchHitBlock(final LivingEntity target, final Player activator, final int knockbackLevel) {
        soundService.play(target, "stellarity:item.dragonblade.damage_wall", 1.0F, 1.0F, null);
        if (!isBoss(target)) {
            customStatusEffectService.applyVoided(target, 240, 1);
        }
        final double damage = 240.0D + (knockbackLevel * 40.0D);
        final boolean trueDamageEnabled = itemsConfigService.getWeaponsConfig().getBoolean("dragonblade.true-damage-enabled", true);
        if (trueDamageEnabled) {
            target.getPersistentDataContainer().set(trueDamageKey, org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
        }
        target.damage(damage, activator);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 6, true, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 220, 0, true, false, true));
    }

    private void applyPunchSplash(final LivingEntity center, final Player activator, final int knockbackLevel) {
        final double damage = 40.0D + (knockbackLevel * 15.0D);
        final List<Entity> nearby = center.getNearbyEntities(2.75D, 2.75D, 2.75D);
        final boolean trueDamageEnabled = itemsConfigService.getWeaponsConfig().getBoolean("dragonblade.true-damage-enabled", true);
        for (final Entity entity : nearby) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }
            if (target.getUniqueId().equals(center.getUniqueId()) || target.getUniqueId().equals(activator.getUniqueId())) {
                continue;
            }
            if (trueDamageEnabled) {
                target.getPersistentDataContainer().set(trueDamageKey, org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
            }
            target.damage(damage, activator);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, false, true));
        }
    }

    private void spawnStackParticles(final LivingEntity target, final int stacks) {
        if (stacks <= 0) {
            return;
        }
        final Particle.DustOptions dust = switch (stacks) {
            case 1 -> new Particle.DustOptions(org.bukkit.Color.fromRGB(45, 0, 75), 1.5F);
            case 2 -> new Particle.DustOptions(org.bukkit.Color.fromRGB(101, 0, 168), 1.5F);
            default -> new Particle.DustOptions(org.bukkit.Color.fromRGB(153, 0, 255), 1.5F);
        };
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0D, 1.5D, 0.0D), 3, 0.18D, 0.18D, 0.18D, 0.0D, dust);
    }

    private void resetStacks(final TargetState state) {
        state.stacks = 0;
        state.untilStackResetTicks = 0;
        state.punchReady = false;
        state.punchProgress = 0;
        state.untilPunchResetTicks = 0;
        state.punchTickGate = 0;
    }

    private Player findActivator(final LivingEntity target) {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (final Entity entity : target.getNearbyEntities(5.0D, 5.0D, 5.0D)) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            if (!player.isOnline() || !player.isSneaking() || !isDragonbladeInMainHand(player)) {
                continue;
            }
            final int level = activeService.highestLevel(
                    activeService.fromItem(player.getInventory().getItemInMainHand(), "mainhand"),
                    EnchantIds.TECHNICAL_DRACONIC
            );
            if (level <= 0) {
                continue;
            }
            final double distance = target.getLocation().distanceSquared(player.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    private int readKnockbackLevel(final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        return item.getEnchantmentLevel(Enchantment.KNOCKBACK);
    }

    private boolean isDragonbladeInMainHand(final Player player) {
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        final String itemId = itemStateRepository.getItemId(mainHand).orElse("");
        if (!DRAGONBLADE_ITEM_ID.equalsIgnoreCase(itemId)) {
            return false;
        }
        final int level = activeService.highestLevel(activeService.fromItem(mainHand, "mainhand"), EnchantIds.TECHNICAL_DRACONIC);
        return level > 0;
    }

    private boolean isBoss(final LivingEntity target) {
        if (target == null) {
            return false;
        }
        final String name = target.getType().name();
        return name.contains("WITHER") || name.contains("DRAGON") || name.contains("WARDEN") || target.getScoreboardTags().contains("stellarity.boss");
    }

    @org.bukkit.event.EventHandler
    public void onItemSpawn(final org.bukkit.event.entity.ItemSpawnEvent event) {
        final org.bukkit.entity.Item itemEntity = event.getEntity();
        final ItemStack item = itemEntity.getItemStack();
        final String itemId = itemStateRepository.getItemId(item).orElse("");
        if (DRAGONBLADE_ITEM_ID.equalsIgnoreCase(itemId)) {
            itemEntity.setGlowing(true);
            
            final org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team purpleTeam = scoreboard.getTeam("stellarity_purple");
            if (purpleTeam == null) {
                purpleTeam = scoreboard.registerNewTeam("stellarity_purple");
                purpleTeam.color(net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE);
            }
            purpleTeam.addEntry(itemEntity.getUniqueId().toString());
            
            final org.bukkit.scoreboard.Team finalTeam = purpleTeam;
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (!itemEntity.isValid() || itemEntity.isDead()) {
                        finalTeam.removeEntry(itemEntity.getUniqueId().toString());
                        this.cancel();
                        return;
                    }
                    
                    final Location loc = itemEntity.getLocation();
                    final org.bukkit.World world = itemEntity.getWorld();
                    if (itemEntity.isOnGround()) {
                        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.1, 0), 1, 0.25, 0.05, 0.25, 0.02);
                        world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 0.1, 0), 2, 0.35, 0.05, 0.35, 0.02);
                    } else {
                        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.2, 0), 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }
            }.runTaskTimer(plugin, 0L, 5L);
        }
    }

    private static final class TargetState {
        private int stacks = 0;
        private int untilStackResetTicks = 0;
        private boolean punchReady = false;
        private int punchProgress = 0;
        private int untilPunchResetTicks = 0;
        private int cooldownTicks = 0;
        private int punchTickGate = 0;
    }
}
