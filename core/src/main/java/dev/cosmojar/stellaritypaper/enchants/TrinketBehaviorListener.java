package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class TrinketBehaviorListener implements Listener {

    private final TrinketBehaviorService trinketBehaviorService;

    public TrinketBehaviorListener(final TrinketBehaviorService trinketBehaviorService) {
        this.trinketBehaviorService = trinketBehaviorService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTarget(final EntityTargetLivingEntityEvent event) {
        trinketBehaviorService.onEntityTarget(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        trinketBehaviorService.onPlayerInteract(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerConsume(final PlayerItemConsumeEvent event) {
        trinketBehaviorService.onPlayerConsume(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(final EntityDamageEvent event) {
        trinketBehaviorService.onPlayerDamage(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player defender) {
            if (defender.isBlocking()) {
                trinketBehaviorService.onPlayerShieldBlock(defender);
                trinketBehaviorService.onStarstruckBlock(defender);
            }
        }

        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity victim) {
            trinketBehaviorService.onPlayerMeleeAttack(event, attacker, victim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(final ProjectileHitEvent event) {
        trinketBehaviorService.onPearlHit(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        trinketBehaviorService.onBlockBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(final org.bukkit.event.block.BlockPlaceEvent event) {
        trinketBehaviorService.onBlockPlace(event);
    }
}
