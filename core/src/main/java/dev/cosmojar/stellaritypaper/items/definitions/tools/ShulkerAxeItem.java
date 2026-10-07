package dev.cosmojar.stellaritypaper.items.definitions.tools;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class ShulkerAxeItem implements StellarityItemDefinition {
    @Override
    public String category() { return "tools"; }
    @Override
    public String commandName() { return "shulker_axe"; }
    @Override
    public String pdcItemId() { return "shulker_axe"; }
    @Override
    public Material material() { return Material.NETHERITE_AXE; }
    @Override
    public String itemModelKey() { return "stellarity:shulker_axe"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(2701);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 12));
        builder.addEnchantInstance(new EnchantInstance("block_break_speed_add_multiplied_total_mainhand", 1));
        builder.addEnchantInstance(new EnchantInstance("attack_speed_add_multiplied_total_mainhand", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("tools", "shulker_axe"); }
}
