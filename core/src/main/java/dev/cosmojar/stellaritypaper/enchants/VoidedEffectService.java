package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.registry.KeyFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Реализация статуса Voided: временное снижение max_health.
 */
public final class VoidedEffectService {

    private static final double PER_LEVEL_MULTIPLIER = -0.2D;
    private static final String MODIFIER_PATH = "effects.voided.max_health";

    private final Plugin plugin;
    private final NamespacedKey modifierKey;
    private final Map<UUID, BukkitTask> removeTasks = new HashMap<>();

    public VoidedEffectService(final Plugin plugin, final KeyFactory keyFactory) {
        this.plugin = plugin;
        this.modifierKey = keyFactory.stellarity(MODIFIER_PATH);
    }

    public void apply(final LivingEntity entity, final int durationTicks, final int level) {
        if (entity == null || !entity.isValid() || entity.isDead()) {
            return;
        }
        final AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        clear(entity);
        final double amount = PER_LEVEL_MULTIPLIER * Math.max(1, level);
        maxHealth.addModifier(new AttributeModifier(
                modifierKey,
                amount,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
        ));
        if (entity.getHealth() > maxHealth.getValue()) {
            entity.setHealth(Math.max(1.0D, maxHealth.getValue()));
        }

        final BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> clear(entity), Math.max(1, durationTicks));
        removeTasks.put(entity.getUniqueId(), task);
    }

    public void clear(final LivingEntity entity) {
        if (entity == null) {
            return;
        }
        final BukkitTask task = removeTasks.remove(entity.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        final AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        for (final AttributeModifier modifier : maxHealth.getModifiers()) {
            if (modifierKey.equals(modifier.getKey())) {
                maxHealth.removeModifier(modifier);
            }
        }
        if (entity.getHealth() > maxHealth.getValue()) {
            entity.setHealth(Math.max(1.0D, maxHealth.getValue()));
        }
    }

    public void clearAll() {
        for (final BukkitTask task : removeTasks.values()) {
            task.cancel();
        }
        removeTasks.clear();
    }
}
