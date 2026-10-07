package dev.cosmojar.stellaritypaper.items.definitions.weapons;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class BookOfUpdraftItem implements StellarityItemDefinition {
    @Override
    public String category() { return "weapons"; }
    @Override
    public String commandName() { return "book_of_updraft"; }
    @Override
    public String pdcItemId() { return "book_of_updraft"; }
    @Override
    public Material material() { return Material.WARPED_FUNGUS_ON_A_STICK; }
    @Override
    public String itemModelKey() { return "stellarity:book_of_updraft"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("weapons", "book_of_updraft"); }
}
