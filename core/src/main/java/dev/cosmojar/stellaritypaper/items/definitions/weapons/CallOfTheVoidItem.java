package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class CallOfTheVoidItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "call_of_the_void"; }
    @Override
    public String pdcItemId() { return "call_of_the_void"; }
    @Override
    public Material material() { return Material.BOW; }
    @Override
    public String itemModelKey() { return "stellarity:call_of_the_void"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(576);
        builder.fireResistant(true);
        builder.addEnchantInstance(new EnchantInstance("void_shot", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "call_of_the_void"); }
}
