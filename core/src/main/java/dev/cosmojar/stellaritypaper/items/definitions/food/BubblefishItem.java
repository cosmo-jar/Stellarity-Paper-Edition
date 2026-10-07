package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;



public final class BubblefishItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "bubblefish"; }
    @Override
    public String pdcItemId() { return "bubblefish"; }
    @Override
    public Material material() { return Material.PUFFERFISH; }
    @Override
    public String itemModelKey() { return "stellarity:bubblefish"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(1);
        builder.foodSaturation(0.2f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(0.5f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "bubblefish"); }
}
