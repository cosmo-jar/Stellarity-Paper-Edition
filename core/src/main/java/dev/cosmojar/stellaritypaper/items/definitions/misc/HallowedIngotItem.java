package dev.cosmojar.stellaritypaper.items.definitions.misc;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;




public final class HallowedIngotItem implements StellarityItemDefinition {
    @Override
    public String category() { return "misc"; }
    @Override
    public String commandName() { return "hallowed_ingot"; }
    @Override
    public String pdcItemId() { return "hallowed_ingot"; }
    @Override
    public Material material() { return Material.IRON_INGOT; }
    @Override
    public String itemModelKey() { return "stellarity:hallowed_ingot"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("misc", "hallowed_ingot"); }
}
