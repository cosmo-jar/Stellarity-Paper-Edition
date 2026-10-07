package dev.cosmojar.stellaritypaper.items.definitions.trinkets;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;

import org.bukkit.Material;

public final class DecayedCloverItem implements StellarityItemDefinition {
    @Override
    public String category() { return "trinkets"; }
    @Override
    public String commandName() { return "decayed_clover"; }
    @Override
    public String pdcItemId() { return "decayed_clover"; }
    @Override
    public Material material() { return Material.POPPED_CHORUS_FRUIT; }
    @Override
    public String itemModelKey() { return "stellarity:decayed_clover"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.addEnchantInstance(new EnchantInstance("luck_add_multiplied_base_offhand", 1));
        builder.addEnchantInstance(new EnchantInstance("luck_add_value_offhand", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("trinkets", "decayed_clover"); }
}
