package dev.cosmojar.stellaritypaper.items.definitions.misc;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class PrecipiceStereoItem implements StellarityItemDefinition {
    @Override
    public String category() { return "misc"; }
    @Override
    public String commandName() { return "precipice_stereo"; }
    @Override
    public String pdcItemId() { return "music_disc_precipice_stereo"; }
    @Override
    public Material material() { return Material.MUSIC_DISC_PRECIPICE; }
    @Override
    public String itemModelKey() { return "stellarity:music_disc_precipice_stereo"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("misc", "precipice_stereo"); }
}
