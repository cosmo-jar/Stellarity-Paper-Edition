package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * O(1) сервис отслеживания наличия Radiant Jewel в инвентаре игроков.
 * Кэширует состояние в Set<UUID> и обновляет его строго по событиям инвентаря.
 */
public final class RadiantJewelService implements Listener {

    private final Plugin plugin;
    private final ItemStateRepository itemStateRepository;
    private final Set<UUID> playersWithRadiantJewel = new HashSet<>();

    public RadiantJewelService(final Plugin plugin, final ItemStateRepository itemStateRepository) {
        this.plugin = plugin;
        this.itemStateRepository = itemStateRepository;
    }

    /**
     * Быстрая проверка наличия Radiant Jewel в инвентаре игрока.
     */
    public boolean hasRadiantJewel(final Player player) {
        if (player == null) {
            return false;
        }
        return playersWithRadiantJewel.contains(player.getUniqueId());
    }

    /**
     * Мгновенное сканирование инвентаря игрока.
     */
    public void rescan(final Player player) {
        if (player == null || !player.isOnline()) {
            if (player != null) {
                playersWithRadiantJewel.remove(player.getUniqueId());
            }
            return;
        }

        boolean found = false;
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                if (isRadiantJewel(item)) {
                    found = true;
                    break;
                }
            }
        }

        if (found) {
            playersWithRadiantJewel.add(player.getUniqueId());
        } else {
            playersWithRadiantJewel.remove(player.getUniqueId());
        }
    }

    private boolean isRadiantJewel(final ItemStack item) {
        return itemStateRepository.getItemId(item)
                .map("radiant_jewel"::equalsIgnoreCase)
                .orElse(false);
    }

    private void scheduleRescan(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> rescan(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRescan(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRescan(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            scheduleRescan(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        scheduleRescan(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        scheduleRescan(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        scheduleRescan(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        playersWithRadiantJewel.remove(event.getPlayer().getUniqueId());
    }

    public void clear() {
        playersWithRadiantJewel.clear();
    }
}
