package dev.cosmojar.stellaritypaper.items.definitions.armor;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import org.bukkit.Material;

public final class PhantomWingsItem implements StellarityItemDefinition {
    @Override
    public String category() { return "armor"; }
    @Override
    public String commandName() { return "phantom_wings"; }
    @Override
    public String pdcItemId() { return "phantom_wings"; }
    @Override
    public Material material() { return Material.ELYTRA; }
    @Override
    public String itemModelKey() { return "stellarity:phantom_wings"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.equippable(new EquippableSpec("chest", "stellarity:phantom_wings", null, false));
        builder.maxDamage(70);
        builder.addEnchantInstance(new EnchantInstance("gravity_add_multiplied_total_chest", 2));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("armor", "phantom_wings"); }
}
