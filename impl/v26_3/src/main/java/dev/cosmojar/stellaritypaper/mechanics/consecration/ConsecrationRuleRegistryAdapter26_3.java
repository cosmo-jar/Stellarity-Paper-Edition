package dev.cosmojar.stellaritypaper.mechanics.consecration;

import org.bukkit.Material;

public final class ConsecrationRuleRegistryAdapter26_3 {

    public void applyRules(final ConsecrationRuleRegistry registry) {
        // 26.2 Consecrations (Sulfur <-> Cinnabar, Pointed Dripstone <-> Sulfur Spike)
        registerPair(registry, "POINTED_DRIPSTONE", "SULFUR_SPIKE", "minecraft:pointed_dripstone", "minecraft:sulfur_spike");

        registerPair(registry, "SULFUR", "CINNABAR", "minecraft:sulfur", "minecraft:cinnabar");
        registerPair(registry, "SULFUR_SLAB", "CINNABAR_SLAB", "minecraft:sulfur_slab", "minecraft:cinnabar_slab");
        registerPair(registry, "SULFUR_STAIRS", "CINNABAR_STAIRS", "minecraft:sulfur_stairs", "minecraft:cinnabar_stairs");
        registerPair(registry, "SULFUR_WALL", "CINNABAR_WALL", "minecraft:sulfur_wall", "minecraft:cinnabar_wall");
        registerPair(registry, "POLISHED_SULFUR", "POLISHED_CINNABAR", "minecraft:polished_sulfur", "minecraft:polished_cinnabar");
        registerPair(registry, "POLISHED_SULFUR_SLAB", "POLISHED_CINNABAR_SLAB", "minecraft:polished_sulfur_slab", "minecraft:polished_cinnabar_slab");
        registerPair(registry, "POLISHED_SULFUR_STAIRS", "POLISHED_CINNABAR_STAIRS", "minecraft:polished_sulfur_stairs", "minecraft:polished_cinnabar_stairs");
        registerPair(registry, "POLISHED_SULFUR_WALL", "POLISHED_CINNABAR_WALL", "minecraft:polished_sulfur_wall", "minecraft:polished_cinnabar_wall");
        registerPair(registry, "SULFUR_BRICKS", "CINNABAR_BRICKS", "minecraft:sulfur_bricks", "minecraft:cinnabar_bricks");
        registerPair(registry, "SULFUR_BRICK_SLAB", "CINNABAR_BRICK_SLAB", "minecraft:sulfur_brick_slab", "minecraft:cinnabar_brick_slab");
        registerPair(registry, "SULFUR_BRICK_STAIRS", "CINNABAR_BRICK_STAIRS", "minecraft:sulfur_brick_stairs", "minecraft:cinnabar_brick_stairs");
        registerPair(registry, "SULFUR_BRICK_WALL", "CINNABAR_BRICK_WALL", "minecraft:sulfur_brick_wall", "minecraft:cinnabar_brick_wall");
        registerPair(registry, "CHISELED_SULFUR", "CHISELED_CINNABAR", "minecraft:chiseled_sulfur", "minecraft:chiseled_cinnabar");

        // 26.3 Consecrations: Poplar Leaves -> Cherry Leaves
        registerRule(registry, "ORANGE_POPLAR_LEAVES", Material.CHERRY_LEAVES, "minecraft:orange_poplar_leaves", "minecraft:cherry_leaves");
        registerRule(registry, "RED_POPLAR_LEAVES", Material.CHERRY_LEAVES, "minecraft:red_poplar_leaves", "minecraft:cherry_leaves");
        registerRule(registry, "YELLOW_POPLAR_LEAVES", Material.CHERRY_LEAVES, "minecraft:yellow_poplar_leaves", "minecraft:cherry_leaves");

        // 26.3 Consecrations: Poplar woods -> Cherry woods
        registerRule(registry, "POPLAR_WOOD", Material.CHERRY_WOOD, "minecraft:poplar_wood", "minecraft:cherry_wood");
        registerRule(registry, "POPLAR_LOG", Material.CHERRY_LOG, "minecraft:poplar_log", "minecraft:cherry_log");
        registerRule(registry, "STRIPPED_POPLAR_LOG", Material.STRIPPED_CHERRY_LOG, "minecraft:stripped_poplar_log", "minecraft:stripped_cherry_log");
        registerRule(registry, "STRIPPED_POPLAR_WOOD", Material.STRIPPED_CHERRY_WOOD, "minecraft:stripped_poplar_wood", "minecraft:stripped_cherry_wood");
        registerRule(registry, "POPLAR_SAPLING", Material.CHERRY_SAPLING, "minecraft:poplar_sapling", "minecraft:cherry_sapling");
    }

    private void registerRule(final ConsecrationRuleRegistry registry,
                              final String matNameInput, final Material output,
                              final String keyInput, final String keyOutput) {
        final Material input = Material.matchMaterial(matNameInput);
        if (input != null && output != null) {
            registry.addRule(input, output);
        } else if (input != null) {
            registry.addRule(input, keyOutput);
        } else if (output != null) {
            registry.addRule(keyInput, output);
        } else {
            registry.addRule(keyInput, keyOutput);
        }
    }

    private void registerPair(final ConsecrationRuleRegistry registry,
                              final String matNameA, final String matNameB,
                              final String keyA, final String keyB) {
        final Material a = Material.matchMaterial(matNameA);
        final Material b = Material.matchMaterial(matNameB);
        if (a != null && b != null) {
            registry.addBiRule(a, b);
        } else if (a != null) {
            registry.addBiRule(a, keyB);
        } else if (b != null) {
            registry.addBiRule(b, keyA);
        } else {
            registry.addBiRule(keyA, keyB);
        }
    }
}
