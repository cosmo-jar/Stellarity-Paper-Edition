package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;



public final class FriedChorusFruitItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "fried_chorus_fruit"; }
    @Override
    public String pdcItemId() { return "fried_chorus_fruit"; }
    @Override
    public Material material() { return Material.CHORUS_FRUIT; }
    @Override
    public String itemModelKey() { return "stellarity:fried_chorus_fruit"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(7);
        builder.foodSaturation(11.2f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(1.6f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "fried_chorus_fruit"); }
}
