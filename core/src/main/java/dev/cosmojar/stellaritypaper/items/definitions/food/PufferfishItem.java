package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class PufferfishItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "pufferfish"; }
    @Override
    public String pdcItemId() { return "pufferfish"; }
    @Override
    public Material material() { return Material.PUFFERFISH; }
    @Override
    public String itemModelKey() { return "stellarity:pufferfish"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(1);
        builder.foodSaturation(0.0f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(5.0f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "pufferfish"); }
}
