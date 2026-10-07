package dev.cosmojar.stellaritypaper.mechanics.loot;

public record CustomLootEntry(
        String itemKey,
        double chance,
        int weight
) {}
