package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class KaleidoscopeItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "kaleidoscope"; }
    @Override
    public String pdcItemId() { return "kaleidoscope"; }
    @Override
    public Material material() { return Material.NETHERITE_AXE; }
    @Override
    public String itemModelKey() { return "stellarity:kaleidoscope"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.unbreakable(true);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 11));
        builder.addEnchantInstance(new EnchantInstance("entity_interaction_range_add_value_mainhand", 1));
        builder.addEnchantInstance(new EnchantInstance("technical_daybroken", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "kaleidoscope"); }
}
