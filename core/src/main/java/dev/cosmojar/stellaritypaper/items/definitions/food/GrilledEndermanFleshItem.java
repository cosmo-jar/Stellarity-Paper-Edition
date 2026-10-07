package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class GrilledEndermanFleshItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "grilled_enderman_flesh"; }
    @Override
    public String pdcItemId() { return "grilled_enderman_flesh"; }
    @Override
    public Material material() { return Material.COOKED_BEEF; }
    @Override
    public String itemModelKey() { return "stellarity:grilled_enderman_flesh"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(6);
        builder.foodSaturation(9.6f);
        builder.foodCanAlwaysEat(false);
        builder.consumableSeconds(1.6f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "grilled_enderman_flesh"); }
}
