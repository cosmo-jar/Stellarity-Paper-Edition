package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class ShulkerBodyItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "shulker_body"; }
    @Override
    public String pdcItemId() { return "shulker_body"; }
    @Override
    public Material material() { return Material.CHORUS_FRUIT; }
    @Override
    public String itemModelKey() { return "stellarity:shulker_body"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(4);
        builder.foodSaturation(0.8f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(1.6f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "shulker_body"); }
}
