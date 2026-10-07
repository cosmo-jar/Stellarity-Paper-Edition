package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class PrismiteItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "prismite"; }
    @Override
    public String pdcItemId() { return "prismite"; }
    @Override
    public Material material() { return Material.COOKED_COD; }
    @Override
    public String itemModelKey() { return "stellarity:prismite"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(3);
        builder.foodSaturation(1.8f);
        builder.foodCanAlwaysEat(false);
        builder.consumableSeconds(1.6f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "prismite"); }
}
