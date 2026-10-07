package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import org.bukkit.Material;

public final class ChampionLeggingsItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "champion_leggings"; }
    @Override
    public String pdcItemId() { return "champion_leggings"; }
    @Override
    public Material material() { return Material.IRON_LEGGINGS; }
    @Override
    public String itemModelKey() { return "stellarity:champion_leggings"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.equippable(new EquippableSpec("legs", "stellarity:champion", "minecraft:item.armor.equip_netherite", true));
        builder.dyedColor(11041988);
        builder.addEnchantInstance(new EnchantInstance("armor_add_value_legs", 1));
        builder.addEnchantInstance(new EnchantInstance("armor_toughness_add_value_legs", 1));
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_multiplied_base_legs", 1));
        builder.addEnchantInstance(new EnchantInstance("knockback_resistance_add_value_legs", 1));
        builder.addEnchantInstance(new EnchantInstance("sweeping_damage_ratio_add_value_legs", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "champion_leggings"); }
}
