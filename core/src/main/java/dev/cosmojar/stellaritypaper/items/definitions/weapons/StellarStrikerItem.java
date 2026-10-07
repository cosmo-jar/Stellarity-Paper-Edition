package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class StellarStrikerItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "stellar_striker"; }
    @Override
    public String pdcItemId() { return "stellar_striker"; }
    @Override
    public Material material() { return Material.NETHERITE_SWORD; }
    @Override
    public String itemModelKey() { return "stellarity:stellar_striker"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.unbreakable(true);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 8));
        builder.addEnchantInstance(new EnchantInstance("entity_interaction_range_add_value_mainhand", 2));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "stellar_striker"); }
}
