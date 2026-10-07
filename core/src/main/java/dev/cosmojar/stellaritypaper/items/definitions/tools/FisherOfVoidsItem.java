package dev.cosmojar.stellaritypaper.items.definitions.tools;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class FisherOfVoidsItem implements StellarityItemDefinition {
    @Override
    public String category() { return "tools"; }
    @Override
    public String commandName() { return "fisher_of_voids"; }
    @Override
    public String pdcItemId() { return "fisher_of_voids"; }
    @Override
    public Material material() { return Material.FISHING_ROD; }
    @Override
    public String itemModelKey() { return "stellarity:fisher_of_voids"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(264);
        builder.fireResistant(true);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("tools", "fisher_of_voids"); }
}
