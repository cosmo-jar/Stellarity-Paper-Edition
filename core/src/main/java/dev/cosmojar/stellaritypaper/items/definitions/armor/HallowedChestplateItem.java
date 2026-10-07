package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import org.bukkit.Material;

public final class HallowedChestplateItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "hallowed_chestplate"; }
    @Override
    public String pdcItemId() { return "hallowed_chestplate"; }
    @Override
    public Material material() { return Material.IRON_CHESTPLATE; }
    @Override
    public String itemModelKey() { return "stellarity:hallowed_chestplate"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.equippable(new EquippableSpec("chest", "stellarity:hallowed", "minecraft:item.armor.equip_netherite", true));
        builder.dyedColor(16768338);
        builder.maxDamage(592);
        builder.addEnchantInstance(new EnchantInstance("armor_add_value_chest", 2));
        builder.addEnchantInstance(new EnchantInstance("armor_toughness_add_value_chest", 2));
        builder.addEnchantInstance(new EnchantInstance("knockback_resistance_add_value_chest", 2));
        builder.addEnchantInstance(new EnchantInstance("safe_fall_distance_add_value_chest", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "hallowed_chestplate"); }
}
