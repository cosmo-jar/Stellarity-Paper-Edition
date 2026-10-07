package dev.cosmojar.stellaritypaper.items.definitions.potions;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionEffectSpec;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionSpec;
import org.bukkit.Material;

import java.util.List;

public final class SpelunkerPotionIiItem implements StellarityItemDefinition {
    @Override
    public String category() { return "potions"; }
    @Override
    public String commandName() { return "spelunker_potion_ii"; }
    @Override
    public String pdcItemId() { return "spelunker_potion_ii"; }
    @Override
    public Material material() { return Material.POTION; }
    @Override
    public String itemModelKey() { return null; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.potion(new PotionSpec("minecraft:water", 16767232, List.of(new PotionEffectSpec("minecraft:night_vision", 1800, 0, false, true, true), new PotionEffectSpec("minecraft:haste", 1800, 1, false, true, true))));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("potions", "spelunker_potion_ii"); }
}
