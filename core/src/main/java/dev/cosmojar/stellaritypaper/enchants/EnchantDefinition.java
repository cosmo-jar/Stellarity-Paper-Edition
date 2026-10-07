package dev.cosmojar.stellaritypaper.enchants;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EnchantDefinition(
        String id,
        String attributeKey,
        String operation,
        String slot,
        Map<Integer, Double> tiers,
        EnchantBackendType backendType,
        String messageKeyBase
) {
    public EnchantDefinition {
        tiers = Collections.unmodifiableMap(new LinkedHashMap<>(tiers));
    }

    public double resolveAmount(final int level) {
        final Double amount = tiers.get(level);
        if (amount == null) {
            throw new IllegalStateException("Unknown enchant tier level " + level + " for " + id);
        }
        return amount;
    }
}

