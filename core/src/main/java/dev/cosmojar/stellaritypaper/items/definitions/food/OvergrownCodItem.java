package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class OvergrownCodItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "overgrown_cod"; }
    @Override
    public String pdcItemId() { return "overgrown_cod"; }
    @Override
    public Material material() { return Material.COOKED_COD; }
    @Override
    public String itemModelKey() { return "stellarity:overgrown_cod"; }
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
    public ItemTextRef textRef() { return new ItemTextRef("food", "overgrown_cod"); }
}
