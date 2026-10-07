package dev.cosmojar.stellaritypaper.enchants;

import net.kyori.adventure.util.TriState;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Локальная реализация DoT-эффекта Prismatic Inferno.
 */
public final class PrismaticInfernoEffectService {

    private static final int TICK_PERIOD = 20;
    private static final double BASE_DAMAGE = 10.0D;
    private static final Set<EntityType> UNDEAD = EnumSet.of(
            EntityType.ZOMBIE,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.WITHER_SKELETON,
            EntityType.PHANTOM
    );

    private final Plugin plugin;
    private final Map<UUID, ActiveInferno> active = new HashMap<>();

    public PrismaticInfernoEffectService(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void apply(final LivingEntity target, final int durationTicks, final Entity source) {
        if (target == null || !target.isValid() || target.isDead()) {
            return;
        }
        clear(target);

        final boolean isPlayer = target instanceof Player;
        final TriState hadVisualFire = target.getVisualFire();
        if (!isPlayer && hadVisualFire != TriState.FALSE) {
            target.setVisualFire(TriState.TRUE);
        }

        final ActiveInferno state = new ActiveInferno(source == null ? null : source.getUniqueId(), hadVisualFire, durationTicks);
        state.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tick(target), TICK_PERIOD, TICK_PERIOD);
        active.put(target.getUniqueId(), state);
    }

    public void clear(final LivingEntity target) {
        if (target == null) {
            return;
        }
        final ActiveInferno state = active.remove(target.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.task != null) {
            state.task.cancel();
        }
        if (!(target instanceof Player)) {
            target.setVisualFire(state.hadVisualFire != null ? state.hadVisualFire : TriState.NOT_SET);
        } else if (target.getVisualFire() == TriState.TRUE) {
            target.setVisualFire(TriState.NOT_SET);
        }
    }

    public void clearAll() {
        for (final ActiveInferno inferno : active.values()) {
            if (inferno.task != null) {
                inferno.task.cancel();
            }
        }
        active.clear();
    }

    private void tick(final LivingEntity target) {
        final ActiveInferno state = active.get(target.getUniqueId());
        if (state == null) {
            return;
        }
        if (!target.isValid() || target.isDead()) {
            clear(target);
            return;
        }

        state.remainingTicks -= TICK_PERIOD;
        damageTick(target, state);
        if (target.isDead() || state.remainingTicks <= 0) {
            clear(target);
        }
    }

    private void damageTick(final LivingEntity target, final ActiveInferno state) {
        final Location location = target.getLocation();
        final World world = location.getWorld();
        if (world == null) {
            return;
        }

        double damage = BASE_DAMAGE;
        if (UNDEAD.contains(target.getType())) {
            damage *= 2.0D;
        }
        if (isInDaylight(target)) {
            damage *= 2.0D;
            world.spawnParticle(
                    Particle.GLOW,
                    location.clone().add(0.0D, 1.0D, 0.0D),
                    16,
                    0.4D,
                    0.55D,
                    0.4D,
                    0.0D,
                    null
            );
            world.playSound(location, Sound.ITEM_FIRECHARGE_USE, 0.3F, 1.5F);
        } else {
            world.spawnParticle(Particle.END_ROD, location.clone().add(0.0D, 1.0D, 0.0D), 4, 0.3D, 0.4D, 0.3D, 0.02D, null);
            world.spawnParticle(Particle.FLAME, location.clone().add(0.0D, 1.0D, 0.0D), 4, 0.3D, 0.4D, 0.3D, 0.01D, null);
        }

        final Entity source = state.sourceEntityId == null ? null : plugin.getServer().getEntity(state.sourceEntityId);
        if (source instanceof LivingEntity livingSource) {
            target.damage(damage, livingSource);
        } else {
            target.damage(damage);
        }
        target.setFireTicks(Math.max(target.getFireTicks(), 20));
    }

    private boolean isInDaylight(final LivingEntity entity) {
        final World world = entity.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        final boolean clearWeather = !world.hasStorm() && !world.isThundering();
        if (!clearWeather) {
            return false;
        }
        final long dayTime = world.getTime();
        final boolean isDay = dayTime < 12786L || dayTime > 23460L;
        return isDay && world.getHighestBlockYAt(entity.getLocation()) <= entity.getLocation().getBlockY();
    }

    private static final class ActiveInferno {
        private final UUID sourceEntityId;
        private final TriState hadVisualFire;
        private int remainingTicks;
        private BukkitTask task;

        private ActiveInferno(final UUID sourceEntityId, final TriState hadVisualFire, final int remainingTicks) {
            this.sourceEntityId = sourceEntityId;
            this.hadVisualFire = hadVisualFire;
            this.remainingTicks = remainingTicks;
        }
    }
}
