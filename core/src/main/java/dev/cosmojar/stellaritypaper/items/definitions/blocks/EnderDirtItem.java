package dev.cosmojar.stellaritypaper.items.definitions.blocks;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class EnderDirtItem implements StellarityItemDefinition {
    @Override
    public String category() { return "blocks"; }
    @Override
    public String commandName() { return "ender_dirt"; }
    @Override
    public String pdcItemId() { return "ender_dirt"; }
    @Override
    public Material material() { return Material.COARSE_DIRT; }
    @Override
    public String itemModelKey() { return "stellarity:ender_dirt"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("blocks", "ender_dirt"); }
}
