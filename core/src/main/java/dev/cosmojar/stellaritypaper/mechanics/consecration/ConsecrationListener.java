package dev.cosmojar.stellaritypaper.mechanics.consecration;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public final class ConsecrationListener implements Listener {

    private final ConsecrationService service;

    public ConsecrationListener(final ConsecrationService service) {
        this.service = service;
    }

    @EventHandler
    public void onDrop(final PlayerDropItemEvent event) {
        service.handleDrop(event);
    }
}
