package dev.cosmojar.stellaritypaper.items.definitions.misc;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;




public final class GildedPurpurKeyItem implements StellarityItemDefinition {
    @Override
    public String category() { return "misc"; }
    @Override
    public String commandName() { return "gilded_purpur_key"; }
    @Override
    public String pdcItemId() { return "gilded_purpur_key"; }
    @Override
    public Material material() { return Material.OMINOUS_TRIAL_KEY; }
    @Override
    public String itemModelKey() { return "stellarity:gilded_purpur_key"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("misc", "gilded_purpur_key"); }
}
