package dev.cosmojar.stellaritypaper.items.definitions.trinkets;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class LivingFleshItem implements StellarityItemDefinition {
    @Override
    public String category() { return "trinkets"; }
    @Override
    public String commandName() { return "living_flesh"; }
    @Override
    public String pdcItemId() { return "living_flesh"; }
    @Override
    public Material material() { return Material.ROTTEN_FLESH; }
    @Override
    public String itemModelKey() { return "stellarity:living_flesh"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("trinkets", "living_flesh"); }
}
