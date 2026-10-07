package dev.cosmojar.stellaritypaper.items.definitions.misc;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;




public final class EndonomiconItem implements StellarityItemDefinition {
    @Override
    public String category() { return "misc"; }
    @Override
    public String commandName() { return "endonomicon"; }
    @Override
    public String pdcItemId() { return "endonomicon"; }
    @Override
    public Material material() { return Material.BOOK; }
    @Override
    public String itemModelKey() { return "stellarity:endonomicon"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.fireResistant(true);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("misc", "endonomicon"); }
}
