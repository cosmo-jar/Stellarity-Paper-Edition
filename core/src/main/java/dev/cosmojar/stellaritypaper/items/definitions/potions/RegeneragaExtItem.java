package dev.cosmojar.stellaritypaper.items.definitions.potions;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionEffectSpec;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionSpec;
import org.bukkit.Material;

import java.util.List;

public final class RegeneragaExtItem implements StellarityItemDefinition {
    @Override
    public String category() { return "potions"; }
    @Override
    public String commandName() { return "regeneraga_ext"; }
    @Override
    public String pdcItemId() { return "regeneraga_ext"; }
    @Override
    public Material material() { return Material.POTION; }
    @Override
    public String itemModelKey() { return null; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.potion(new PotionSpec(null, 16121975, List.of(new PotionEffectSpec("minecraft:instant_health", 1, 1, false, true, true), new PotionEffectSpec("minecraft:regeneration", 210, 2, false, true, true))));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("potions", "regeneraga_ext"); }
}
