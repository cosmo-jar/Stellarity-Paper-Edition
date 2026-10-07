package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class FluffyHammerItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "fluffy_hammer"; }
    @Override
    public String pdcItemId() { return "fluffy_hammer"; }
    @Override
    public Material material() { return Material.NETHERITE_AXE; }
    @Override
    public String itemModelKey() { return "stellarity:fluffy_hammer"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(1562);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 13));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "fluffy_hammer"); }
}
