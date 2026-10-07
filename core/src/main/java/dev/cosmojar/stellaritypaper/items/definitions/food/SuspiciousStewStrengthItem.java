package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;



public final class SuspiciousStewStrengthItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "suspicious_stew_strength"; }
    @Override
    public String pdcItemId() { return "food_suspicious_stew_strength"; }
    @Override
    public Material material() { return Material.SUSPICIOUS_STEW; }
    @Override
    public String itemModelKey() { return null; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "suspicious_stew_strength"); }
}
