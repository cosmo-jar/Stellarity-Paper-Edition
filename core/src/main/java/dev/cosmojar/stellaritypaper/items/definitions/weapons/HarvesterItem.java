package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class HarvesterItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "harvester"; }
    @Override
    public String pdcItemId() { return "harvester"; }
    @Override
    public Material material() { return Material.IRON_SWORD; }
    @Override
    public String itemModelKey() { return "stellarity:harvester"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.unbreakable(true);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 2));
        builder.addEnchantInstance(new EnchantInstance("entity_interaction_range_add_value_mainhand", 2));
        builder.addEnchantInstance(new EnchantInstance("movement_speed_add_multiplied_total_mainhand", 1));
        builder.addEnchantInstance(new EnchantInstance("technical_soul_harvest", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "harvester"); }
}
