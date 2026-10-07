package dev.cosmojar.stellaritypaper.items.definitions.spawn_eggs;

import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import org.bukkit.Material;

public final class VoidedSilverfishItem implements StellarityItemDefinition {
    @Override
    public String category() { return "spawn_eggs"; }
    @Override
    public String commandName() { return "voided_silverfish"; }
    @Override
    public String pdcItemId() { return "voided_silverfish_spawn_egg"; }
    @Override
    public Material material() { return Material.SILVERFISH_SPAWN_EGG; }
    @Override
    public String itemModelKey() { return "stellarity:voided_silverfish_spawn_egg"; }
    @Override
    public ItemComponents components() {
        final ItemComponents.Builder builder = ItemComponents.builder();
        return builder.build();
    }
    @Override
    public ItemTextRef textRef() { return new ItemTextRef("spawn_eggs", "voided_silverfish"); }
}
