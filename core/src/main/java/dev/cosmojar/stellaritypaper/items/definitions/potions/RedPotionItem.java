package dev.cosmojar.stellaritypaper.items.definitions.potions;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionEffectSpec;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionSpec;
import org.bukkit.Material;

import java.util.List;

public final class RedPotionItem implements StellarityItemDefinition {
    @Override
    public String category() { return "potions"; }
    @Override
    public String commandName() { return "red_potion"; }
    @Override
    public String pdcItemId() { return "red_potion"; }
    @Override
    public Material material() { return Material.POTION; }
    @Override
    public String itemModelKey() { return null; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.potion(new PotionSpec("minecraft:water", 16056354, List.of(new PotionEffectSpec("minecraft:darkness", -1, 9, false, true, true), new PotionEffectSpec("minecraft:slowness", -1, 9, false, true, true), new PotionEffectSpec("minecraft:mining_fatigue", -1, 9, false, true, true), new PotionEffectSpec("minecraft:nausea", -1, 0, false, true, true), new PotionEffectSpec("minecraft:blindness", -1, 0, false, true, true), new PotionEffectSpec("minecraft:hunger", -1, 9, false, true, true), new PotionEffectSpec("minecraft:weakness", -1, 9, false, true, true), new PotionEffectSpec("minecraft:poison", -1, 9, false, true, true), new PotionEffectSpec("minecraft:wither", -1, 9, false, true, true), new PotionEffectSpec("minecraft:glowing", -1, 0, false, true, true), new PotionEffectSpec("minecraft:unluck", -1, 9, false, true, true), new PotionEffectSpec("minecraft:bad_omen", 1200, 9, false, true, true))));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("potions", "red_potion"); }
}
