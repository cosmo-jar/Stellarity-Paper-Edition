package dev.cosmojar.stellaritypaper.mechanics.structures;

import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class EndCityCrystalListener implements Listener {

    private final EndCityCrystalService crystalService;
    private final EndStructuresService structuresService;

    public EndCityCrystalListener(
            final EndCityCrystalService crystalService,
            final EndStructuresService structuresService
    ) {
        this.crystalService = crystalService;
        this.structuresService = structuresService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystalDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) {
            return;
        }

        if (!crystalService.isEndCityCrystal(crystal)) {
            return;
        }

        event.setCancelled(true);

        if (crystalService.isDestroying(crystal)) {
            return;
        }

        Player attacker = null;
        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            if (byEntityEvent.getDamager() instanceof Player p) {
                attacker = p;
            } else if (byEntityEvent.getDamager() instanceof org.bukkit.entity.Projectile proj
                    && proj.getShooter() instanceof Player p) {
                attacker = p;
            }
        }

        crystalService.startDestructionSequence(crystal, attacker, structuresService);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystalInteract(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof EnderCrystal crystal)) {
            return;
        }

        if (!crystalService.isEndCityCrystal(crystal)) {
            return;
        }

        event.setCancelled(true);

        if (crystalService.isDestroying(crystal)) {
            return;
        }

        crystalService.startDestructionSequence(crystal, event.getPlayer(), structuresService);
    }
}
