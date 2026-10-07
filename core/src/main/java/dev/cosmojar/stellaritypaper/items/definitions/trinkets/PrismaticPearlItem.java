package dev.cosmojar.stellaritypaper.items.definitions.trinkets;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class PrismaticPearlItem implements StellarityItemDefinition {
    @Override
    public String category() { return "trinkets"; }
    @Override
    public String commandName() { return "prismatic_pearl"; }
    @Override
    public String pdcItemId() { return "prismatic_pearl"; }
    @Override
    public Material material() { return Material.ENDER_PEARL; }
    @Override
    public String itemModelKey() { return "stellarity:prismatic_pearl"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        builder.fireResistant(true);
        builder.useCooldownSeconds(4.9f);
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("trinkets", "prismatic_pearl"); }
}
