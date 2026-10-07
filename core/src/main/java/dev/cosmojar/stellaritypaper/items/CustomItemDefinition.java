package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.items.catalog.ItemText;

public record CustomItemDefinition(
        StellarityItemDefinition itemDefinition,
        ItemText itemText
) {
}
