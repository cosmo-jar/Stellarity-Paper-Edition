package dev.cosmojar.stellaritypaper.mechanics.pixie;

import dev.cosmojar.stellaritypaper.items.CustomBlockService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class PixieInAJarService implements Listener {

    private final Plugin plugin;
    private final CustomBlockService customBlockService;
    private final Map<Location, String> jarLocations = new HashMap<>();

    public PixieInAJarService(final Plugin plugin, final CustomBlockService customBlockService) {
        this.plugin = plugin;
        this.customBlockService = customBlockService;

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickParticles, 1L, 1L);
        loadLoadedJars();
    }

    private void tickParticles() {
        for (final Map.Entry<Location, String> entry : new ArrayList<>(jarLocations.entrySet())) {
            final Location loc = entry.getKey();
            final World world = loc.getWorld();
            if (world == null || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }

            if (loc.getBlock().getType() != org.bukkit.Material.STRUCTURE_VOID) {
                jarLocations.remove(loc);
                continue;
            }

            final boolean hasPlayersNearby = world.getPlayers().stream()
                    .anyMatch(p -> p.getLocation().distanceSquared(loc) <= 400.0);
            if (!hasPlayersNearby) {
                continue;
            }

            final String colorVariant = entry.getValue();
            final Color color1;
            final Color color2;

            switch (colorVariant) {
                case "yellow":
                    color1 = Color.fromRGB(236, 187, 81);
                    color2 = Color.fromRGB(254, 255, 167);
                    break;
                case "magenta":
                    color1 = Color.fromRGB(251, 167, 255);
                    color2 = Color.fromRGB(236, 81, 222);
                    break;
                case "lime":
                    color1 = Color.fromRGB(126, 236, 81);
                    color2 = Color.fromRGB(191, 255, 167);
                    break;
                case "radiant":
                    color1 = Color.fromRGB(236, 52, 64);
                    color2 = Color.fromRGB(255, 150, 150);
                    break;
                case "light_blue":
                default:
                    color1 = Color.fromRGB(167, 255, 250);
                    color2 = Color.fromRGB(81, 213, 236);
                    break;
            }

            final Location particleLoc = loc.clone().add(0.5D, 0.25D, 0.5D);
            final Particle.DustOptions dust1 = new Particle.DustOptions(color1, 0.8F);
            final Particle.DustOptions dust2 = new Particle.DustOptions(color2, 0.8F);

            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust1);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust2);
        }
    }

    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent event) {
        for (final Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemDisplay display) {
                final String blockId = customBlockService.getBlockId(display);
                if (blockId != null && blockId.startsWith("pixie_in_a_jar_")) {
                    final String variant = blockId.substring("pixie_in_a_jar_".length());
                    jarLocations.put(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation(), variant);
                }
            }
        }
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        jarLocations.keySet().removeIf(loc -> loc.getWorld().equals(event.getWorld()) && 
            (loc.getBlockX() >> 4) == event.getChunk().getX() && 
            (loc.getBlockZ() >> 4) == event.getChunk().getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.isCancelled() && event.getBlock().getType() != org.bukkit.Material.AIR) {
            return;
        }
        jarLocations.remove(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(final EntitySpawnEvent event) {
        if (event.getEntity() instanceof ItemDisplay display) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (display.isValid()) {
                    final String blockId = customBlockService.getBlockId(display);
                    if (blockId != null && blockId.startsWith("pixie_in_a_jar_")) {
                        final String variant = blockId.substring("pixie_in_a_jar_".length());
                        jarLocations.put(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation(), variant);
                    }
                }
            });
        }
    }

    private void loadLoadedJars() {
        for (final World world : Bukkit.getWorlds()) {
            for (final org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (final Entity entity : chunk.getEntities()) {
                    if (entity instanceof ItemDisplay display) {
                        final String blockId = customBlockService.getBlockId(display);
                        if (blockId != null && blockId.startsWith("pixie_in_a_jar_")) {
                            final String variant = blockId.substring("pixie_in_a_jar_".length());
                            jarLocations.put(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation(), variant);
                        }
                    }
                }
            }
        }
    }
}
