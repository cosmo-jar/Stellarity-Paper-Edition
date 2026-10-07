package dev.cosmojar.stellaritypaper.items.api.spec;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record PotionSpec(
        @Nullable String basePotionKey,
        @Nullable Integer colorRgb,
        List<PotionEffectSpec> effects
) {
    public PotionSpec {
        effects = effects == null ? List.of() : List.copyOf(effects);
    }
}
