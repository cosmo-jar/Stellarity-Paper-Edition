package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;



public final class CrystalHeartfishItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "crystal_heartfish"; }
    @Override
    public String pdcItemId() { return "crystal_heartfish"; }
    @Override
    public Material material() { return Material.COOKED_COD; }
    @Override
    public String itemModelKey() { return "stellarity:crystal_heartfish"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(0);
        builder.foodSaturation(0.0f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(3.5f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "crystal_heartfish"); }
}
