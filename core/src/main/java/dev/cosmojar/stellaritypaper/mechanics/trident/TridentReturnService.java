package dev.cosmojar.stellaritypaper.mechanics.trident;

import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import dev.cosmojar.stellaritypaper.data.SessionTaskService;
import org.bukkit.World;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.UUID;

public final class TridentReturnService {

    private final FeatureFlags featureFlags;
    private final SessionTaskService sessionTaskService;

    public TridentReturnService(final FeatureFlags featureFlags, final SessionTaskService sessionTaskService) {
        this.featureFlags = featureFlags;
        this.sessionTaskService = sessionTaskService;
    }

    public void handleLaunch(final ProjectileLaunchEvent event) {
        if (!featureFlags.isTridentReturnEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }

        final UUID sessionId = UUID.randomUUID();
        sessionTaskService.startOrReplace(sessionId, () -> tickTrident(sessionId, trident), 20L);
    }

    private void tickTrident(final UUID sessionId, final Trident trident) {
        if (!trident.isValid() || trident.isDead()) {
            sessionTaskService.cancel(sessionId);
            return;
        }
        if (trident.getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }

        if (trident.getLocation().getY() <= 0) {
            trident.setHasDealtDamage(true);
            sessionTaskService.cancel(sessionId);
        }
    }
}
