package dev.cosmojar.stellaritypaper.items.definitions.misc;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;




public final class SandRuneItem implements StellarityItemDefinition {
    @Override
    public String category() { return "misc"; }
    @Override
    public String commandName() { return "sand_rune"; }
    @Override
    public String pdcItemId() { return "sand_rune"; }
    @Override
    public Material material() { return Material.JIGSAW; }
    @Override
    public String itemModelKey() { return "stellarity:sand_rune"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("misc", "sand_rune"); }
}
