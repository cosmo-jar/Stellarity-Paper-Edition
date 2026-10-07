package dev.cosmojar.stellaritypaper.items.definitions.tools;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionManager;
import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import org.bukkit.Material;

public final class ShulkerSpearItem implements StellarityItemDefinition {
    @Override
    public String category() { return "tools"; }
    @Override
    public String commandName() { return "shulker_spear"; }
    @Override
    public String pdcItemId() { return "shulker_spear"; }
    @Override
    public Material material() {
        if (ServerVersion.getCurrent() != ServerVersion.V1_21_11 && ServerVersion.getCurrent() != ServerVersion.UNKNOWN) {
            return VersionManager.getAdapter().getShulkerSpearMaterial();
        }
        return Material.NETHERITE_SPEAR;
    }
    @Override
    public String itemModelKey() { return "stellarity:shulker_spear"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(2701);
        builder.addEnchantInstance(new EnchantInstance("attack_damage_add_value_mainhand", 4));
        builder.addEnchantInstance(new EnchantInstance("attack_speed_add_multiplied_total_mainhand", 1));
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("tools", "shulker_spear"); }
}
