package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class SlayerCrossbowItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "slayer_crossbow"; }
    @Override
    public String pdcItemId() { return "slayer_crossbow"; }
    @Override
    public Material material() { return Material.CROSSBOW; }
    @Override
    public String itemModelKey() { return "stellarity:slayer_crossbow"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.maxDamage(702);
        builder.fireResistant(true);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "slayer_crossbow"); }
}
