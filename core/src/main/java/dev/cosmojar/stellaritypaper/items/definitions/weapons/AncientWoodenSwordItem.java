package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class AncientWoodenSwordItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "ancient_wooden_sword"; }
    @Override
    public String pdcItemId() { return "alpha_wooden_sword"; }
    @Override
    public Material material() { return Material.WOODEN_SWORD; }
    @Override
    public String itemModelKey() { return "stellarity:ancient_wooden_sword"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.unbreakable(true);
        builder.fireResistant(true);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 2));
        builder.addEnchantInstance(new EnchantInstance("attack_speed_add_value_mainhand", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "ancient_wooden_sword"); }
}
