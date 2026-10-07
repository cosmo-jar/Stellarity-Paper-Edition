package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class SandstormTridentItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "sandstorm_trident"; }
    @Override
    public String pdcItemId() { return "sandstorm_trident"; }
    @Override
    public Material material() { return Material.TRIDENT; }
    @Override
    public String itemModelKey() { return "stellarity:sandstorm_trident"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(702);
        builder.fireResistant(true);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 5));
        builder.addEnchantInstance(new EnchantInstance("technical_mighty_wind", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "sandstorm_trident"); }
}
