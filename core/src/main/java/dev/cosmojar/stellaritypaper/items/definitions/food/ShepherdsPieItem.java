package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class ShepherdsPieItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "shepherds_pie"; }
    @Override
    public String pdcItemId() { return "shepherds_pie"; }
    @Override
    public Material material() { return Material.PUMPKIN_PIE; }
    @Override
    public String itemModelKey() { return "stellarity:shepherds_pie"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(20);
        builder.foodSaturation(20.0f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(5.0f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "shepherds_pie"); }
}
