package dev.cosmojar.stellaritypaper.items.definitions.tools;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class ShulkerSwordItem implements StellarityItemDefinition {
    @Override
    public String category() { return "tools"; }
    @Override
    public String commandName() { return "shulker_sword"; }
    @Override
    public String pdcItemId() { return "shulker_sword"; }
    @Override
    public Material material() { return Material.NETHERITE_SWORD; }
    @Override
    public String itemModelKey() { return "stellarity:shulker_sword"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(2701);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 9));
        builder.addEnchantInstance(new EnchantInstance("block_break_speed_add_multiplied_total_mainhand", 1));
        builder.addEnchantInstance(new EnchantInstance("attack_speed_add_multiplied_total_mainhand", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("tools", "shulker_sword"); }
}
