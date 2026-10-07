package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class TamarisItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "tamaris"; }
    @Override
    public String pdcItemId() { return "tamaris"; }
    @Override
    public Material material() { return Material.NETHERITE_SWORD; }
    @Override
    public String itemModelKey() { return "stellarity:tamaris"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.unbreakable(true);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 7));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "tamaris"); }
}
