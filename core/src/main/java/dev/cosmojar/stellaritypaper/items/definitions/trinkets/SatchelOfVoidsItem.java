package dev.cosmojar.stellaritypaper.items.definitions.trinkets;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class SatchelOfVoidsItem implements StellarityItemDefinition {
    @Override
    public String category() { return "trinkets"; }
    @Override
    public String commandName() { return "satchel_of_voids"; }
    @Override
    public String pdcItemId() { return "satchel_of_voids"; }
    @Override
    public Material material() { return Material.WARPED_FUNGUS_ON_A_STICK; }
    @Override
    public String itemModelKey() { return "stellarity:satchel_of_voids"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("trinkets", "satchel_of_voids"); }
}
