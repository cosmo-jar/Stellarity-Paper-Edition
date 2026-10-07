package dev.cosmojar.stellaritypaper.items.definitions.misc;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import org.bukkit.Material;

public final class EnderiteSmithingTemplateItem implements StellarityItemDefinition {
    @Override
    public String category() { return "misc"; }
    @Override
    public String commandName() { return "enderite_smithing_template"; }
    @Override
    public String pdcItemId() { return "enderite_smithing_template"; }
    @Override
    public Material material() { return Material.JIGSAW; }
    @Override
    public String itemModelKey() { return "stellarity:enderite_smithing_template"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("misc", "enderite_smithing_template"); }
}
