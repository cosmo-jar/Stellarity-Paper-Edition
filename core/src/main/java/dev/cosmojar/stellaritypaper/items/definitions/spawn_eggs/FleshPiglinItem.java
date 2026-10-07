package dev.cosmojar.stellaritypaper.items.definitions.spawn_eggs;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class FleshPiglinItem implements StellarityItemDefinition {
    @Override
    public String category() { return "spawn_eggs"; }
    @Override
    public String commandName() { return "flesh_piglin"; }
    @Override
    public String pdcItemId() { return "flesh_piglin_spawn_egg"; }
    @Override
    public Material material() { return Material.PIGLIN_SPAWN_EGG; }
    @Override
    public String itemModelKey() { return "stellarity:flesh_piglin_spawn_egg"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("spawn_eggs", "flesh_piglin"); }
}
