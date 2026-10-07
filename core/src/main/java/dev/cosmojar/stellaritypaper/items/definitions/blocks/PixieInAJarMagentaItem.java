package dev.cosmojar.stellaritypaper.items.definitions.blocks;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class PixieInAJarMagentaItem implements StellarityItemDefinition {
    @Override
    public String category() { return "blocks"; }
    @Override
    public String commandName() { return "pixie_in_a_jar_magenta"; }
    @Override
    public String pdcItemId() { return "pixie_in_a_jar_magenta"; }
    @Override
    public Material material() { return Material.STRUCTURE_VOID; }
    @Override
    public String itemModelKey() { return "stellarity:pixie_in_a_jar"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("blocks", "pixie_in_a_jar_magenta"); }
}
