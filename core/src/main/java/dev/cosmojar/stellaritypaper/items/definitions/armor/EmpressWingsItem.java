package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;

import org.bukkit.Material;

public final class EmpressWingsItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "empress_wings"; }
    @Override
    public String pdcItemId() { return "empress_wings"; }
    @Override
    public Material material() { return Material.ELYTRA; }
    @Override
    public String itemModelKey() { return "stellarity:empress_wings"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.equippable(new EquippableSpec("chest", "stellarity:empress_wings", null, false));
        builder.addEnchantInstance(new EnchantInstance("fall_damage_multiplier_add_multiplied_total_chest", 1));
        builder.addEnchantInstance(new EnchantInstance("gravity_add_multiplied_total_chest", 1));
        builder.addEnchantInstance(new EnchantInstance("movement_speed_add_multiplied_base_chest", 2));
        builder.addEnchantInstance(new EnchantInstance("safe_fall_distance_add_value_chest", 2));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "empress_wings"); }
}
