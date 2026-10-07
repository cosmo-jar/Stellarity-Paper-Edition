package dev.cosmojar.stellaritypaper.core;

import dev.cosmojar.stellaritypaper.config.ConfigService;

public final class FeatureFlags {

    private final ConfigService configService;

    public FeatureFlags(final ConfigService configService) {
        this.configService = configService;
    }

    public boolean isStellarityEnderStructuresEnabled() {
        return configService.read().features().stellarityEnderStructuresEnabled();
    }

    public boolean isVoidTotemEnabled() {
        return configService.read().features().voidTotemEnabled();
    }

    public boolean isTridentReturnEnabled() {
        return configService.read().features().tridentReturnEnabled();
    }

    public boolean isVoidFishingEnabled() {
        return configService.read().features().voidFishingEnabled();
    }

    public boolean isVoidFishingBiomeAllocation() {
        return configService.read().features().voidFishingBiomeAllocation();
    }

    public boolean isCauldronCraftEnabled() {
        return configService.read().features().cauldronCraftingEnabled();
    }

    public boolean isConsecrationEnabled() {
        return configService.read().features().consecrationEnabled();
    }

    public boolean isAccursedAltarEnabled() {
        return configService.read().features().accursedAltarEnabled();
    }
}
