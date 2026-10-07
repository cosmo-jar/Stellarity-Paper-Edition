package dev.cosmojar.stellaritypaper.items.definitions.blocks;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class PhantomItemFrameItem implements StellarityItemDefinition {
    @Override
    public String category() { return "blocks"; }
    @Override
    public String commandName() { return "phantom_item_frame"; }
    @Override
    public String pdcItemId() { return "phantom_item_frame"; }
    @Override
    public Material material() { return Material.ITEM_FRAME; }
    @Override
    public String itemModelKey() { return "stellarity:phantom_item_frame"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("blocks", "phantom_item_frame"); }
}
