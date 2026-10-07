package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;

import org.bukkit.Material;

public final class ReinforcedHorseArmorItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "reinforced_horse_armor"; }
    @Override
    public String pdcItemId() { return "reinforced_horse_armor"; }
    @Override
    public Material material() { return Material.LEATHER_HORSE_ARMOR; }
    @Override
    public String itemModelKey() { return "stellarity:reinforced_horse_armor"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.dyedColor(12219878);
        builder.addEnchantInstance(new EnchantInstance("movement_speed_add_multiplied_base_body", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "reinforced_horse_armor"); }
}
