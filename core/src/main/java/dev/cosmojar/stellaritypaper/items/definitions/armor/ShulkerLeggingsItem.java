package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import org.bukkit.Material;

public final class ShulkerLeggingsItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "shulker_leggings"; }
    @Override
    public String pdcItemId() { return "shulker_leggings"; }
    @Override
    public Material material() { return Material.IRON_LEGGINGS; }
    @Override
    public String itemModelKey() { return "stellarity:shulker_leggings"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.equippable(new EquippableSpec("legs", "stellarity:shulker", "minecraft:item.armor.equip_netherite", true));
        builder.dyedColor(12219878);
        builder.maxDamage(755);
        builder.addEnchantInstance(new EnchantInstance("armor_add_value_legs", 1));
        builder.addEnchantInstance(new EnchantInstance("armor_toughness_add_value_legs", 3));
        builder.addEnchantInstance(new EnchantInstance("knockback_resistance_add_value_legs", 3));
        builder.addEnchantInstance(new EnchantInstance("movement_speed_add_multiplied_total_legs", 1));
        builder.addEnchantInstance(new EnchantInstance("attack_speed_add_multiplied_total_legs", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "shulker_leggings"); }
}
