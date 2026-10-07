package dev.cosmojar.stellaritypaper.mechanics.totem;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class VoidTotemListener implements Listener {

    private final VoidTotemService service;

    public VoidTotemListener(final VoidTotemService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {
        service.handleMove(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(final org.bukkit.event.entity.EntityDamageEvent event) {
        service.handleDamage(event);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        service.handleQuit(event);
    }
}

