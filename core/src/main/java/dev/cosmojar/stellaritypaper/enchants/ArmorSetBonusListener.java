package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public final class ArmorSetBonusListener implements Listener {

    private final ArmorSetBonusService armorSetBonusService;

    public ArmorSetBonusListener(final ArmorSetBonusService armorSetBonusService) {
        this.armorSetBonusService = armorSetBonusService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity victim) {
            armorSetBonusService.onPlayerMeleeAttack(attacker, victim);
        }

        if (event.getEntity() instanceof Player victim) {
            armorSetBonusService.onPlayerTakeDamage(event, victim);
        }

        if (event.getDamager() instanceof Projectile projectile && event.getEntity() instanceof LivingEntity victim) {
            armorSetBonusService.onProjectileHit(event, projectile, victim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        armorSetBonusService.onProjectileLaunch(event);
    }
}
