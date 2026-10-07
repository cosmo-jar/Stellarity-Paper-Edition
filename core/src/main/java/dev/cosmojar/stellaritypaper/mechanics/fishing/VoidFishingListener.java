package dev.cosmojar.stellaritypaper.mechanics.fishing;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class VoidFishingListener implements Listener {

    private final VoidFishingService service;

    public VoidFishingListener(final VoidFishingService service) {
        this.service = service;
    }

    @EventHandler
    public void onFish(final PlayerFishEvent event) {
        service.handleFishEvent(event);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        service.cancelSession(event.getPlayer().getUniqueId());
    }
}
