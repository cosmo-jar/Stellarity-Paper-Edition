package dev.cosmojar.stellaritypaper.mechanics.painting;

import org.bukkit.entity.Painting;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class PaintingInventoryHolder implements InventoryHolder {

    private final Painting painting;

    public PaintingInventoryHolder(final Painting painting) {
        this.painting = painting;
    }

    public Painting getPainting() {
        return painting;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
