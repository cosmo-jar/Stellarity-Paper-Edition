package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class TheEndItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "the_end"; }
    @Override
    public String pdcItemId() { return "spirit_dagger"; }
    @Override
    public Material material() { return Material.NETHERITE_SWORD; }
    @Override
    public String itemModelKey() { return "stellarity:the_end"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.unbreakable(true);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 14));
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_offhand", 1));
        builder.addEnchantInstance(new EnchantInstance("entity_interaction_range_add_value_mainhand", 1));
        builder.addEnchantInstance(new EnchantInstance("attack_speed_bonus_mainhand", 1));
        builder.addEnchantInstance(new EnchantInstance("movement_speed_add_multiplied_total_offhand", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "the_end"); }
}
