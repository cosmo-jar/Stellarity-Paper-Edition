package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EnchantHorseRuntimeListener implements Listener {

    private final Plugin plugin;
    private final EnchantHorseBackendService horseBackendService;
    private final EnchantItemService enchantItemService;
    private final Set<UUID> pendingRecalculate = new HashSet<>();

    public EnchantHorseRuntimeListener(
            final Plugin plugin,
            final EnchantHorseBackendService horseBackendService,
            final EnchantItemService enchantItemService
    ) {
        this.plugin = plugin;
        this.horseBackendService = horseBackendService;
        this.enchantItemService = enchantItemService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof AbstractHorse horse)) {
            return;
        }
        ensureHorseArmorUpToDate(horse);
        scheduleRecalculate(horse);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        final Object holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof AbstractHorse horse)) {
            return;
        }
        scheduleRecalculate(horse);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        final Object holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof AbstractHorse horse)) {
            return;
        }
        scheduleRecalculate(horse);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(final InventoryCloseEvent event) {
        final Object holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractHorse horse)) {
            return;
        }
        scheduleRecalculate(horse);
    }

    private void ensureHorseArmorUpToDate(final AbstractHorse horse) {
        final EntityEquipment equipment = horse.getEquipment();
        if (equipment == null) {
            return;
        }
        final ItemStack bodyArmor = equipment.getItem(EquipmentSlot.BODY);
        enchantItemService.ensureUpToDate(bodyArmor);
    }

    private void scheduleRecalculate(final AbstractHorse horse) {
        if (!pendingRecalculate.add(horse.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!horse.isValid() || horse.isDead()) {
                    return;
                }
                ensureHorseArmorUpToDate(horse);
                horseBackendService.recalculate(horse);
            } finally {
                pendingRecalculate.remove(horse.getUniqueId());
            }
        });
    }
}
