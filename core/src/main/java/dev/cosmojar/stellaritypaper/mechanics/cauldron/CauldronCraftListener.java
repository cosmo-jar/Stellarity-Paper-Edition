package dev.cosmojar.stellaritypaper.mechanics.cauldron;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CauldronCraftListener implements Listener {

    private final CauldronCraftService service;

    public CauldronCraftListener(final CauldronCraftService service) {
        this.service = service;
    }

    @EventHandler
    public void onDrop(final PlayerDropItemEvent event) {
        service.handleDrop(event);
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        service.handleInteract(event);
    }

    @EventHandler
    public void onItemSpawn(final ItemSpawnEvent event) {
        service.handleItemSpawn(event);
    }
}
