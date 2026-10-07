package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class GoldenChorusFruitItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "golden_chorus_fruit"; }
    @Override
    public String pdcItemId() { return "golden_chorus_fruit"; }
    @Override
    public Material material() { return Material.CHORUS_FRUIT; }
    @Override
    public String itemModelKey() { return "stellarity:golden_chorus_fruit"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(6);
        builder.foodSaturation(14.4f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(5.0f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "golden_chorus_fruit"); }
}
