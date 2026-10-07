package dev.cosmojar.stellaritypaper.data;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionTaskService {

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public SessionTaskService(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startOrReplace(final UUID sessionId, final Runnable runnable, final long periodTicks) {
        cancel(sessionId);
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, 1L, periodTicks);
        tasks.put(sessionId, task);
    }

    public void cancel(final UUID sessionId) {
        final BukkitTask task = tasks.remove(sessionId);
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAll() {
        for (final BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }
}
