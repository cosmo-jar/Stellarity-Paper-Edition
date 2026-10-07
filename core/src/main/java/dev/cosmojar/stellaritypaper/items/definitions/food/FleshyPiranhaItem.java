package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class FleshyPiranhaItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "fleshy_piranha"; }
    @Override
    public String pdcItemId() { return "fleshy_piranha"; }
    @Override
    public Material material() { return Material.COOKED_COD; }
    @Override
    public String itemModelKey() { return "stellarity:fleshy_piranha"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(1);
        builder.foodSaturation(0.2f);
        builder.foodCanAlwaysEat(false);
        builder.consumableSeconds(1.6f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "fleshy_piranha"); }
}
