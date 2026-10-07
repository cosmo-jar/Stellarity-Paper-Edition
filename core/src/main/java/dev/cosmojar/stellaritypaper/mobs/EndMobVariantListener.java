package dev.cosmojar.stellaritypaper.mobs;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class EndMobVariantListener implements Listener {

    private final EndMobVariantService service;

    public EndMobVariantListener(final EndMobVariantService service) {
        this.service = service;
    }

    @EventHandler
    public void onSpawn(final CreatureSpawnEvent event) {
        service.handleSpawn(event);
    }
}
