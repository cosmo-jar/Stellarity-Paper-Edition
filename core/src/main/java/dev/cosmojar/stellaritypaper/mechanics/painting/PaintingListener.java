package dev.cosmojar.stellaritypaper.mechanics.painting;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class PaintingListener implements Listener {

    private final PaintingService paintingService;

    public PaintingListener(final PaintingService paintingService) {
        this.paintingService = paintingService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPaintingInteract(final PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Painting painting)) {
            return;
        }

        final Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        if (!player.hasPermission("stellarity.paintings.select")) {
            return;
        }

        event.setCancelled(true);
        paintingService.openSelector(player, painting);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PaintingInventoryHolder holder)) {
            return;
        }

        event.setCancelled(true);

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PAINTING) {
            return;
        }

        final ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }

        final String keyStr = meta.getPersistentDataContainer().get(
                paintingService.getVariantPdcKey(), 
                PersistentDataType.STRING
        );
        if (keyStr == null) {
            return;
        }

        final NamespacedKey key = NamespacedKey.fromString(keyStr);
        if (key != null) {
            paintingService.applyVariant(holder.getPainting(), key);
        }

        event.getWhoClicked().closeInventory();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PaintingInventoryHolder) {
            event.setCancelled(true);
        }
    }
}
