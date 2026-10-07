package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import org.bukkit.Material;

public final class ChampionHelmetItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "champion_helmet"; }
    @Override
    public String pdcItemId() { return "champion_helmet"; }
    @Override
    public Material material() { return Material.IRON_HELMET; }
    @Override
    public String itemModelKey() { return "stellarity:champion_helmet"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.equippable(new EquippableSpec("head", "stellarity:champion", "minecraft:item.armor.equip_netherite", true));
        builder.dyedColor(11041988);
        builder.addEnchantInstance(new EnchantInstance("armor_add_value_head", 1));
        builder.addEnchantInstance(new EnchantInstance("armor_toughness_add_value_head", 1));
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_multiplied_base_head", 1));
        builder.addEnchantInstance(new EnchantInstance("knockback_resistance_add_value_head", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "champion_helmet"); }
}
