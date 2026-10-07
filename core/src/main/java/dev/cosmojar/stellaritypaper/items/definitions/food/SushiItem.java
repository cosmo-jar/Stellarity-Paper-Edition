package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class SushiItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "sushi"; }
    @Override
    public String pdcItemId() { return "sushi"; }
    @Override
    public Material material() { return Material.TROPICAL_FISH; }
    @Override
    public String itemModelKey() { return "stellarity:sushi"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(4);
        builder.foodSaturation(2.4f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(1.0f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "sushi"); }
}
