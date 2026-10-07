package dev.cosmojar.stellaritypaper.registry;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class KeyFactory {

    private final Plugin plugin;

    public KeyFactory(final Plugin plugin) {
        this.plugin = plugin;
    }

    public NamespacedKey stellarity(final String value) {
        return new NamespacedKey(plugin, value);
    }

    public NamespacedKey minecraft(final String value) {
        return NamespacedKey.minecraft(value);
    }
}
