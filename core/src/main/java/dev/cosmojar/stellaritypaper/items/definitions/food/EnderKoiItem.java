package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class EnderKoiItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "ender_koi"; }
    @Override
    public String pdcItemId() { return "ender_koi"; }
    @Override
    public Material material() { return Material.TROPICAL_FISH; }
    @Override
    public String itemModelKey() { return "stellarity:ender_koi"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(1);
        builder.foodSaturation(0.6f);
        builder.foodCanAlwaysEat(false);
        builder.consumableSeconds(1.6f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "ender_koi"); }
}
