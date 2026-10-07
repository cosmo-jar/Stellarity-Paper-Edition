package dev.cosmojar.stellaritypaper.mechanics.consecration;

import org.bukkit.Material;

public final class ConsecrationRuleRegistryAdapter1_21_4 {

    public void applyRules(final Object registry) {
        if (registry == null) return;
        try {
            var method = registry.getClass().getMethod("addBiRule", Material.class, Material.class);
            method.invoke(registry, Material.DEAD_BUSH, Material.SHORT_GRASS);
            method.invoke(registry, Material.PINK_PETALS, Material.DANDELION);
        } catch (Exception ignored) {}
    }
}
