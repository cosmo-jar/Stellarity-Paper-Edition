package dev.cosmojar.stellaritypaper.items.definitions.misc;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;




public final class EndCityItem implements StellarityItemDefinition {
    @Override
    public String category() { return "misc"; }
    @Override
    public String commandName() { return "end_city"; }
    @Override
    public String pdcItemId() { return "misc_end_city"; }
    @Override
    public Material material() { return Material.MAP; }
    @Override
    public String itemModelKey() { return null; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("misc", "end_city"); }
}
