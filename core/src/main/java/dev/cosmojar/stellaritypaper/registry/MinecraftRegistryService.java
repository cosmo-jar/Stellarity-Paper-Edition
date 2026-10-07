package dev.cosmojar.stellaritypaper.registry;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Optional;

public final class MinecraftRegistryService {

    public <T extends Keyed> Optional<T> find(final Registry<T> registry, final NamespacedKey key) {
        return Optional.ofNullable(registry.get(key));
    }
}
