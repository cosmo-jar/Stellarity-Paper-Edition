package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;



public final class LoafOfPlentyItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "loaf_of_plenty"; }
    @Override
    public String pdcItemId() { return "loaf_of_plenty"; }
    @Override
    public Material material() { return Material.BREAD; }
    @Override
    public String itemModelKey() { return "stellarity:loaf_of_plenty"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(5);
        builder.foodSaturation(6.0f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(1.6f);
        builder.useCooldownSeconds(10.0f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "loaf_of_plenty"); }
}
