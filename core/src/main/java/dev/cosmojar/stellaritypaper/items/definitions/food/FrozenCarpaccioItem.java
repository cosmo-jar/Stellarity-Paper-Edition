package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;


public final class FrozenCarpaccioItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "frozen_carpaccio"; }
    @Override
    public String pdcItemId() { return "frozen_carpaccio"; }
    @Override
    public Material material() { return Material.COOKED_BEEF; }
    @Override
    public String itemModelKey() { return "stellarity:frozen_carpaccio"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.foodNutrition(7);
        builder.foodSaturation(8.4f);
        builder.foodCanAlwaysEat(true);
        builder.consumableSeconds(1.6f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "frozen_carpaccio"); }
}
