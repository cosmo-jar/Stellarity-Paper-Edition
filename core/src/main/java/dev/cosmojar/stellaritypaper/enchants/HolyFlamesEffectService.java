package dev.cosmojar.stellaritypaper.enchants;

import net.kyori.adventure.util.TriState;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import dev.cosmojar.stellaritypaper.integration.DamageSafetyHelper;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Реализация Holy Flames
 * Период урона: 20 тиков (1 сек). Базовый урон: 4.0 (2 сердца).
 * 2x урон по монстрам, 2x урон днём.
 */
public final class HolyFlamesEffectService implements Listener {

    private static final int TICK_PERIOD = 20;
    private static final double BASE_DAMAGE = 4.0D;

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

    private static final Particle.DustOptions[] CYCLE_DUST = new Particle.DustOptions[] {
            new Particle.DustOptions(Color.fromRGB(250, 111, 60), 1.6F),
            new Particle.DustOptions(Color.fromRGB(250, 175, 62), 1.6F),
            new Particle.DustOptions(Color.fromRGB(250, 222, 62), 1.6F)
    };

    private static final Particle.DustOptions DAYLIGHT_DUST =
            new Particle.DustOptions(Color.fromRGB(255, 208, 0), 1.6F);

    private final Plugin plugin;
    private final Map<UUID, ActiveHolyFlames> active = new HashMap<>();

    public HolyFlamesEffectService(final Plugin plugin) {
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

        final ActiveHolyFlames state = new ActiveHolyFlames(
                source == null ? null : source.getUniqueId(),
                hadVisualFire,
                durationTicks
        );
        state.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tick(target), TICK_PERIOD, TICK_PERIOD);
        active.put(target.getUniqueId(), state);
    }

    public void clear(final LivingEntity target) {
        if (target == null) {
            return;
        }
        final ActiveHolyFlames state = active.remove(target.getUniqueId());
        if (state != null && state.task != null) {
            state.task.cancel();
        }

        if (target instanceof Player) {
            if (target.getVisualFire() == TriState.TRUE) {
                target.setVisualFire(TriState.NOT_SET);
            }
        } else {
            target.setVisualFire(state != null && state.hadVisualFire != null ? state.hadVisualFire : TriState.NOT_SET);
        }
    }

    public void clearAll() {
        for (final ActiveHolyFlames holyFlames : active.values()) {
            if (holyFlames.task != null) {
                holyFlames.task.cancel();
            }
        }
        active.clear();
    }

    private void tick(final LivingEntity target) {
        final ActiveHolyFlames state = active.get(target.getUniqueId());
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

    private void damageTick(final LivingEntity target, final ActiveHolyFlames state) {
        final Location location = target.getLocation();
        final World world = location.getWorld();
        if (world == null) {
            return;
        }

        double damage = BASE_DAMAGE;
        if (UNDEAD.contains(target.getType())) {
            damage *= 2.0D;
        }

        final boolean daylight = isInDaylight(target);
        if (daylight) {
            damage *= 2.0D;
            world.spawnParticle(
                    Particle.DUST,
                    location.clone().add(0.0D, 1.0D, 0.0D),
                    16,
                    0.4D,
                    0.55D,
                    0.4D,
                    0.0D,
                    DAYLIGHT_DUST
            );
            world.playSound(location, Sound.ITEM_FIRECHARGE_USE, 0.3F, 1.5F);
        } else {
            final Particle.DustOptions dust = CYCLE_DUST[state.particleCycle % CYCLE_DUST.length];
            state.particleCycle++;
            world.spawnParticle(
                    Particle.DUST,
                    location.clone().add(0.0D, 1.0D, 0.0D),
                    16,
                    0.4D,
                    0.55D,
                    0.4D,
                    0.0D,
                    dust
            );
        }

        world.playSound(location, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0F, 0.9F);
        world.playSound(location, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0F, 1.2F);

        final Entity source = state.sourceEntityId == null ? null : plugin.getServer().getEntity(state.sourceEntityId);
        if (target instanceof Player player && !DamageSafetyHelper.canDamage(player, source)) {
            return;
        }
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        clear(player);
        player.setVisualFire(TriState.NOT_SET);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        clear(player);
        player.setVisualFire(TriState.NOT_SET);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        clear(entity);
        if (entity instanceof Player player) {
            player.setVisualFire(TriState.NOT_SET);
        }
    }

    private static final class ActiveHolyFlames {
        private final UUID sourceEntityId;
        private final TriState hadVisualFire;
        private int remainingTicks;
        private int particleCycle;
        private BukkitTask task;

        private ActiveHolyFlames(final UUID sourceEntityId, final TriState hadVisualFire, final int remainingTicks) {
            this.sourceEntityId = sourceEntityId;
            this.hadVisualFire = hadVisualFire;
            this.remainingTicks = remainingTicks;
            this.particleCycle = 0;
        }
    }
}
