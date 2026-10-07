package dev.cosmojar.stellaritypaper.items.definitions.trinkets;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class CopperElektraShieldItem implements StellarityItemDefinition {
    @Override
    public String category() { return "trinkets"; }
    @Override
    public String commandName() { return "copper_elektra_shield"; }
    @Override
    public String pdcItemId() { return "copper_elektra_shield"; }
    @Override
    public Material material() { return Material.SHIELD; }
    @Override
    public String itemModelKey() { return null; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.fireResistant(true);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("trinkets", "copper_elektra_shield"); }
}
