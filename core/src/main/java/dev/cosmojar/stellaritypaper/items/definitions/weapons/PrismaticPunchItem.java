package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class PrismaticPunchItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "prismatic_punch"; }
    @Override
    public String pdcItemId() { return "prismatic_punch"; }
    @Override
    public Material material() { return Material.CROSSBOW; }
    @Override
    public String itemModelKey() { return "stellarity:prismatic_punch"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(745);
        builder.fireResistant(true);
        builder.addEnchantInstance(new EnchantInstance("technical_infernal_infusion", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "prismatic_punch"); }
}
