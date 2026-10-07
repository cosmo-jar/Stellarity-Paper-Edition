package dev.cosmojar.stellaritypaper.items.definitions.food;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionEffectSpec;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionSpec;
import org.bukkit.Material;

import java.util.List;


public final class ChorusJuiceItem implements StellarityItemDefinition {
    @Override
    public String category() { return "food"; }
    @Override
    public String commandName() { return "chorus_juice"; }
    @Override
    public String pdcItemId() { return "chorus_juice"; }
    @Override
    public Material material() { return Material.POTION; }
    @Override
    public String itemModelKey() { return "stellarity:chorus_juice"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.potion(new PotionSpec(
                "minecraft:water",
                6494569,
                List.of(
                        new PotionEffectSpec("minecraft:speed", 1000, 0, false, true, true),
                        new PotionEffectSpec("minecraft:jump_boost", 1000, 0, false, true, true)
                )
        ));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("food", "chorus_juice"); }
}
