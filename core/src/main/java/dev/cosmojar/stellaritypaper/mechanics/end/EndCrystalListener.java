package dev.cosmojar.stellaritypaper.mechanics.end;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class EndCrystalListener implements Listener {

    private final JavaPlugin plugin;
    private final EndIslandManager islandManager;
    private final List<BukkitRunnable> activeCrystalTasks = new ArrayList<>();

    public EndCrystalListener(JavaPlugin plugin, EndIslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onCrystalPlace(EntityPlaceEvent event) {
        if (event.getEntityType() != EntityType.END_CRYSTAL) return;
        
        EnderCrystal crystal = (EnderCrystal) event.getEntity();
        Location loc = crystal.getLocation();
        World world = loc.getWorld();

        if (world == null || !EndIslandManager.isTheEnd(world)) return;

        if (Math.abs(loc.getBlockX()) <= 12 && Math.abs(loc.getBlockZ()) <= 12) {
            Location below = loc.clone().subtract(0, 1, 0);
            if (below.getBlock().getType() == Material.BEDROCK) {
                
                crystal.setGlowing(true);

                crystal.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "respawn_crystal"),
                    org.bukkit.persistence.PersistentDataType.BYTE,
                    (byte) 1
                );

                startCrystalParticles(crystal);
            }
        }
    }

    private void startCrystalParticles(EnderCrystal crystal) {
        BukkitRunnable particleTask = new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!crystal.isValid() || crystal.isDead()) {
                    activeCrystalTasks.remove(this);
                    this.cancel();
                    return;
                }

                ticks++;
                Location loc = crystal.getLocation().clone().add(0, 0.5, 0);
                
                double radius = 1.5;
                double angle = (ticks * 10) * Math.PI / 180;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                
                World world = crystal.getWorld();
                if (Particle.DRAGON_BREATH.getDataType() == Float.class) {
                    world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0, 1.0f);
                    world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(-x, 0, -z), 1, 0, 0, 0, 0, 1.0f);
                } else {
                    world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(-x, 0, -z), 1, 0, 0, 0, 0);
                }
            }
        };

        particleTask.runTaskTimer(plugin, 1L, 1L);
        activeCrystalTasks.add(particleTask);
    }


    public void cleanup() {
        for (BukkitRunnable task : activeCrystalTasks) {
            task.cancel();
        }
        activeCrystalTasks.clear();
    }
}
