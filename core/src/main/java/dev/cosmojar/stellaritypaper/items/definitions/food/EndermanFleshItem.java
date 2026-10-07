package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;



public final class EndermanFleshItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "enderman_flesh"; }
    @Override
    public String pdcItemId() { return "enderman_flesh"; }
    @Override
    public Material material() { return Material.ROTTEN_FLESH; }
    @Override
    public String itemModelKey() { return "stellarity:enderman_flesh"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(4);
        builder.foodSaturation(0.8f);
        builder.foodCanAlwaysEat(false);
        builder.consumableSeconds(1.6f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "enderman_flesh"); }
}
