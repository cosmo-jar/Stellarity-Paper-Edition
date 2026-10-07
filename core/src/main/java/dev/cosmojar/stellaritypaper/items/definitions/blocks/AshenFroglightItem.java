package dev.cosmojar.stellaritypaper.items.definitions.blocks;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class AshenFroglightItem implements StellarityItemDefinition {
    @Override
    public String category() { return "blocks"; }
    @Override
    public String commandName() { return "ashen_froglight"; }
    @Override
    public String pdcItemId() { return "ashen_froglight"; }
    @Override
    public Material material() { return Material.PEARLESCENT_FROGLIGHT; }
    @Override
    public String itemModelKey() { return "stellarity:ashen_froglight"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("blocks", "ashen_froglight"); }
}
