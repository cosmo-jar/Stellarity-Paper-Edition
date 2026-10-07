package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents vanilla crafting recipes from consuming Stellarity custom items
 * whose Bukkit Material intentionally matches a vanilla ingredient
 * while preserving legitimate custom crafting flows.
 */
public final class CustomVanillaCraftProtectionListener implements Listener {

    private final ItemStateRepository itemStateRepository;

    public CustomVanillaCraftProtectionListener(final ItemStateRepository itemStateRepository) {
        this.itemStateRepository = itemStateRepository;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(final PrepareItemCraftEvent event) {
        final CraftingInventory inventory = event.getInventory();
        final ItemStack result = inventory.getResult();
        if (result == null || result.getType().isAir()) {
            return;
        }

        // If the result is already a custom item (e.g. custom wings repaired by CustomWingsCraftListener),
        // allow the custom craft to proceed.
        if (itemStateRepository.isCustomItem(result)) {
            return;
        }

        // If any matrix ingredient is a Stellarity custom item and the result is vanilla, block the craft.
        if (containsCustomItem(inventory.getMatrix())) {
            inventory.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(final CraftItemEvent event) {
        final CraftingInventory inventory = event.getInventory();
        final ItemStack result = inventory.getResult();

        // If the crafted result is a custom item, allow it.
        if (result != null && itemStateRepository.isCustomItem(result)) {
            return;
        }

        // desync protection: if any ingredient is a custom item and result is vanilla, cancel.
        if (containsCustomItem(inventory.getMatrix())) {
            event.setCancelled(true);
        }
    }

    private boolean containsCustomItem(final ItemStack[] matrix) {
        if (matrix == null) {
            return false;
        }
        for (final ItemStack item : matrix) {
            if (item != null && !item.getType().isAir() && itemStateRepository.isCustomItem(item)) {
                return true;
            }
        }
        return false;
    }
}
