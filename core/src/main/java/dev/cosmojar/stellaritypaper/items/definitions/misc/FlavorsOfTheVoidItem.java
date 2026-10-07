package dev.cosmojar.stellaritypaper.items.definitions.misc;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class FlavorsOfTheVoidItem implements StellarityItemDefinition {
    @Override
    public String category() { return "misc"; }
    @Override
    public String commandName() { return "flavors_of_the_void"; }
    @Override
    public String pdcItemId() { return "flavors_of_the_void"; }
    @Override
    public Material material() { return Material.WRITTEN_BOOK; }
    @Override
    public String itemModelKey() { return "stellarity:flavors_of_the_void"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.fireResistant(true);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("misc", "flavors_of_the_void"); }
}
