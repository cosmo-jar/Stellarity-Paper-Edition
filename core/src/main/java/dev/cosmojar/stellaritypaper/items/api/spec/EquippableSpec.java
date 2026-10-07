package dev.cosmojar.stellaritypaper.items.api.spec;

import org.jetbrains.annotations.Nullable;

public record EquippableSpec(
        @Nullable String slot,
        @Nullable String modelKey,
        @Nullable String equipSoundKey,
        @Nullable Boolean damageOnHurt
) {
}
