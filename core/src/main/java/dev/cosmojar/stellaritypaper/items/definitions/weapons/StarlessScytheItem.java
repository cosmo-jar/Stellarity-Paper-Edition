package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class StarlessScytheItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "starless_scythe"; }
    @Override
    public String pdcItemId() { return "starless_scythe"; }
    @Override
    public Material material() { return Material.WOODEN_HOE; }
    @Override
    public String itemModelKey() { return "stellarity:starless_scythe"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.unbreakable(true);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 11));
        builder.addEnchantInstance(new EnchantInstance("entity_interaction_range_add_value_mainhand", 2));
        builder.addEnchantInstance(new EnchantInstance("sweeping_damage_ratio_add_value_mainhand", 2));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "starless_scythe"); }
}
