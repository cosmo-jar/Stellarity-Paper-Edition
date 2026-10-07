package dev.cosmojar.stellaritypaper.items.definitions.blocks;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class AltarOfTheSacredItem implements StellarityItemDefinition {
    @Override
    public String category() { return "blocks"; }
    @Override
    public String commandName() { return "altar_of_the_sacred"; }
    @Override
    public String pdcItemId() { return "altar_of_the_sacred"; }
    @Override
    public Material material() { return Material.CRYING_OBSIDIAN; }
    @Override
    public String itemModelKey() { return "stellarity:altar_of_the_sacred"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("blocks", "altar_of_the_sacred"); }
}
