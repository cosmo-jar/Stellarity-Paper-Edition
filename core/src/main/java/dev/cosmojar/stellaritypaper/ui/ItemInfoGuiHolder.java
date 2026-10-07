package dev.cosmojar.stellaritypaper.ui;

import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;

public final class ItemInfoGuiHolder implements InventoryHolder {

    private final CustomItemDefinition definition;
    private BukkitTask animTask;

    public ItemInfoGuiHolder(final CustomItemDefinition definition) {
        this.definition = definition;
    }

    public CustomItemDefinition getDefinition() {
        return definition;
    }

    public void setAnimTask(final BukkitTask animTask) {
        this.animTask = animTask;
    }

    public void cancelTask() {
        if (animTask != null) {
            animTask.cancel();
            animTask = null;
        }
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
