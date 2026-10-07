package dev.cosmojar.stellaritypaper.items.api.spec;

public record PotionEffectSpec(
        String effectKey,
        int durationTicks,
        int amplifier,
        boolean ambient,
        boolean showParticles,
        boolean showIcon
) {
}
