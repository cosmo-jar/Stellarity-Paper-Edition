package dev.cosmojar.stellaritypaper.mechanics.trident;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public final class TridentReturnListener implements Listener {

    private final TridentReturnService service;

    public TridentReturnListener(final TridentReturnService service) {
        this.service = service;
    }

    @EventHandler
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        service.handleLaunch(event);
    }
}
