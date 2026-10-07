package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.List;

/**
 * реализация mighty_wind для трезубца.
 */
public final class EnchantTridentBehaviorService {

    private final EnchantActiveService activeService;

    public EnchantTridentBehaviorService(
            final org.bukkit.plugin.Plugin plugin,
            final EnchantActiveService activeService
    ) {
        this.activeService = activeService;
    }

    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }
        if (!(trident.getShooter() instanceof Player player)) {
            return;
        }
        final List<EnchantActiveService.ActiveEnchant> active = activeService.fromPlayerEquipment(player);
        final int level = activeService.highestLevel(active, EnchantIds.TECHNICAL_MIGHTY_WIND);
        if (level <= 0) {
            return;
        }

        trident.setGravity(false);
        trident.setVelocity(trident.getVelocity().multiply(1.25D));
    }

    public void clearAll() {
    }
}

