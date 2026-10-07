package dev.cosmojar.stellaritypaper.config;

public final class ConfigModels {

    private ConfigModels() {
    }

    public record StellarityConfig(Features features, Localization localization) {
    }

    public record Features(
            boolean stellarityEnderStructuresEnabled,
            boolean voidTotemEnabled,
            boolean tridentReturnEnabled,
            boolean voidFishingEnabled,
            boolean voidFishingBiomeAllocation,
            boolean cauldronCraftingEnabled,
            boolean consecrationEnabled,
            boolean accursedAltarEnabled,
            boolean showDescriptionLore
    ) {
    }

    public record Localization(
            String defaultLocale,
            String fallbackLocale
    ) {
    }
}
