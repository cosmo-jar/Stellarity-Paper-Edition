package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import org.bukkit.Material;

public final class FloralHelmetItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "floral_helmet"; }
    @Override
    public String pdcItemId() { return "floral_helmet"; }
    @Override
    public Material material() { return Material.IRON_HELMET; }
    @Override
    public String itemModelKey() { return "stellarity:floral_helmet"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.equippable(new EquippableSpec("head", "stellarity:floral", "minecraft:item.armor.equip_netherite", true));
        builder.dyedColor(16552659);
        builder.maxDamage(407);
        builder.addEnchantInstance(new EnchantInstance("armor_add_value_head", 1));
        builder.addEnchantInstance(new EnchantInstance("armor_toughness_add_value_head", 2));
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_multiplied_base_head", 2));
        builder.addEnchantInstance(new EnchantInstance("knockback_resistance_add_value_head", 2));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "floral_helmet"); }
}
