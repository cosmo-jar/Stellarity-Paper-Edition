package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public final class ElytraBehaviorListener implements Listener {

    private final ElytraBehaviorService elytraBehaviorService;

    public ElytraBehaviorListener(final ElytraBehaviorService elytraBehaviorService) {
        this.elytraBehaviorService = elytraBehaviorService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        elytraBehaviorService.onPlayerInteract(event);
    }
}
