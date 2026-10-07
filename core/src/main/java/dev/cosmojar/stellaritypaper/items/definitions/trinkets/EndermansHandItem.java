package dev.cosmojar.stellaritypaper.items.definitions.trinkets;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class EndermansHandItem implements StellarityItemDefinition {
    @Override
    public String category() { return "trinkets"; }
    @Override
    public String commandName() { return "enderman_hand"; }
    @Override
    public String pdcItemId() { return "enderman_hand"; }
    @Override
    public Material material() { return Material.POPPED_CHORUS_FRUIT; }
    @Override
    public String itemModelKey() { return "stellarity:enderman_hand"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.fireResistant(true);
        builder.addEnchantInstance(new EnchantInstance("block_interaction_range_add_value_hand", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("trinkets", "enderman_hand"); }
}
