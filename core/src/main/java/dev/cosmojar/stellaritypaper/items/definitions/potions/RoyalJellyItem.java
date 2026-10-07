package dev.cosmojar.stellaritypaper.items.definitions.potions;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class RoyalJellyItem implements StellarityItemDefinition {
    @Override
    public String category() { return "potions"; }
    @Override
    public String commandName() { return "royal_jelly"; }
    @Override
    public String pdcItemId() { return "royal_jelly"; }
    @Override
    public Material material() { return Material.HONEY_BOTTLE; }
    @Override
    public String itemModelKey() { return null; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(6);
        builder.foodSaturation(3.6f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(1.5f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("potions", "royal_jelly"); }
}
