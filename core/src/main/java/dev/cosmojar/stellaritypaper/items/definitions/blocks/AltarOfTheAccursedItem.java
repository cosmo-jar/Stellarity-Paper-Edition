package dev.cosmojar.stellaritypaper.items.definitions.blocks;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class AltarOfTheAccursedItem implements StellarityItemDefinition {
    @Override
    public String category() { return "blocks"; }
    @Override
    public String commandName() { return "altar_of_the_accursed"; }
    @Override
    public String pdcItemId() { return "altar_of_the_accursed"; }
    @Override
    public Material material() { return Material.END_PORTAL_FRAME; }
    @Override
    public String itemModelKey() { return "stellarity:altar_of_the_accursed"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("blocks", "altar_of_the_accursed"); }
}
