package dev.cosmojar.stellaritypaper.items.api;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public interface StellarityItemDefinition {

    String category();

    String commandName();

    String pdcItemId();

    Material material();

    @Nullable
    String itemModelKey();

    ItemComponents components();

    ItemTextRef textRef();
}
