package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import org.bukkit.Material;

public final class DragonWingsItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "dragon_wings"; }
    @Override
    public String pdcItemId() { return "dragon_wings"; }
    @Override
    public Material material() { return Material.ELYTRA; }
    @Override
    public String itemModelKey() { return "stellarity:dragon_wings"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.equippable(new EquippableSpec("chest", "stellarity:dragon_wings", null, false));
        builder.maxDamage(648);
        builder.addEnchantInstance(new EnchantInstance("armor_add_value_chest", 1));
        builder.addEnchantInstance(new EnchantInstance("armor_toughness_add_value_chest", 1));
        builder.addEnchantInstance(new EnchantInstance("movement_speed_add_multiplied_base_chest", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "dragon_wings"); }
}
