package dev.cosmojar.stellaritypaper.bootstrap;

import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ListenerRegistrar {

    private final JavaPlugin plugin;

    public ListenerRegistrar(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(final Listener... listeners) {
        final PluginManager pluginManager = plugin.getServer().getPluginManager();
        for (final Listener listener : listeners) {
            pluginManager.registerEvents(listener, plugin);
        }
    }
}
