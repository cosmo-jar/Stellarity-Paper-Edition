package dev.cosmojar.stellaritypaper.items.definitions.trinkets;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;


public final class DuskberryItem implements StellarityItemDefinition {
    @Override
    public String category() { return "trinkets"; }
    @Override
    public String commandName() { return "duskberry"; }
    @Override
    public String pdcItemId() { return "duskberry"; }
    @Override
    public Material material() { return Material.SWEET_BERRIES; }
    @Override
    public String itemModelKey() { return "stellarity:duskberry"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("trinkets", "duskberry"); }
}
