package dev.cosmojar.stellaritypaper.items.endonomicon;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class EndonomiconListener implements Listener {

    private final EndonomiconService service;

    public EndonomiconListener(EndonomiconService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            try {
                String val = pdc.get(key, PersistentDataType.STRING);
                if ("endonomicon".equalsIgnoreCase(val)) {
                    event.setCancelled(true);
                    if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs()) {
                        return;
                    }
                    service.openMainMenu(event.getPlayer());
                    return;
                }
            } catch (Exception ignored) {}
        }
    }
}
