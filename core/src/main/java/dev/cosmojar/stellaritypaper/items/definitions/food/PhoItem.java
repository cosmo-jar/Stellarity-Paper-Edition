package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class PhoItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "pho"; }
    @Override
    public String pdcItemId() { return "pho"; }
    @Override
    public Material material() { return Material.MUSHROOM_STEW; }
    @Override
    public String itemModelKey() { return "stellarity:pho"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(13);
        builder.foodSaturation(20.0f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(3.5f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "pho"); }
}
