package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EnchantRuntimeListener implements Listener {

    private final Plugin plugin;
    private final EnchantRuntimeService enchantRuntimeService;
    private final EnchantItemService enchantItemService;
    private final ItemStateRepository itemStateRepository;
    private final dev.cosmojar.stellaritypaper.config.ItemsConfigService itemsConfigService;
    private final org.bukkit.NamespacedKey trueDamageKey;
    private final Set<UUID> pendingRecalculate = new HashSet<>();

    public EnchantRuntimeListener(
            final Plugin plugin,
            final EnchantRuntimeService enchantRuntimeService,
            final EnchantItemService enchantItemService,
            final ItemStateRepository itemStateRepository,
            final dev.cosmojar.stellaritypaper.config.ItemsConfigService itemsConfigService
    ) {
        this.plugin = plugin;
        this.enchantRuntimeService = enchantRuntimeService;
        this.enchantItemService = enchantItemService;
        this.itemStateRepository = itemStateRepository;
        this.itemsConfigService = itemsConfigService;
        this.trueDamageKey = new org.bukkit.NamespacedKey(plugin, "stellarity_true_damage");
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        enchantItemService.ensurePlayerEquipmentUpToDate(player);
        scheduleRecalculate(player);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        pendingRecalculate.remove(event.getPlayer().getUniqueId());
        enchantRuntimeService.cleanup(event.getPlayer());
    }

    @EventHandler
    public void onHeld(final PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        scheduleRecalculate(player);
    }

    @EventHandler
    public void onSwap(final PlayerSwapHandItemsEvent event) {
        final Player player = event.getPlayer();
        scheduleRecalculate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        enchantItemService.ensurePlayerEquipmentUpToDate(player);
        scheduleRecalculate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        enchantItemService.ensurePlayerEquipmentUpToDate(player);
        scheduleRecalculate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) {
                return;
            }
        }
        final Player player = event.getPlayer();
        enchantItemService.ensureUpToDate(player.getInventory().getItemInMainHand());
        enchantItemService.ensureUpToDate(player.getInventory().getItemInOffHand());
        enchantRuntimeService.onInteract(event);
        scheduleRecalculate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {
        enchantRuntimeService.onPlayerMove(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShootBow(final EntityShootBowEvent event) {
        enchantRuntimeService.onShootBow(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        enchantRuntimeService.onProjectileLaunch(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(final ProjectileHitEvent event) {
        enchantRuntimeService.onProjectileHit(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(final EntityDamageByEntityEvent event) {
        final Player player = resolvePlayerDamager(event.getDamager());
        if (player != null) {
            enchantItemService.ensurePlayerEquipmentUpToDate(player);
            scheduleRecalculate(player);
        }
        enchantRuntimeService.onDamage(event);
        handleArmorPenetrationAndTrueDamage(event, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            enchantItemService.ensurePlayerEquipmentUpToDate(player);
            scheduleRecalculate(player);
        }
        enchantRuntimeService.onEntityDamage(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(final EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            enchantRuntimeService.cleanup(player);
        }
        enchantRuntimeService.onEntityDeath(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityResurrect(final EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            enchantItemService.ensurePlayerEquipmentUpToDate(player);
            scheduleRecalculate(player);
        }
        enchantRuntimeService.onEntityResurrect(event);
    }

    private void scheduleRecalculate(final Player player) {
        if (!pendingRecalculate.add(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                enchantRuntimeService.recalculate(player);
            } finally {
                pendingRecalculate.remove(player.getUniqueId());
            }
        });
    }

    private Player resolvePlayerDamager(final Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private void handleArmorPenetrationAndTrueDamage(final EntityDamageByEntityEvent event, final Player player) {
        final org.bukkit.entity.Entity victim = event.getEntity();
        if (victim.getPersistentDataContainer().has(trueDamageKey, org.bukkit.persistence.PersistentDataType.BOOLEAN)) {
            victim.getPersistentDataContainer().remove(trueDamageKey);
            final double base = event.getDamage();
            final double finalDmg = event.getFinalDamage();
            if (base > 0.0D && finalDmg > 0.0D) {
                final double totalRatio = finalDmg / base;
                if (totalRatio > 0.001D) {
                    event.setDamage(base / totalRatio);
                }
            }
            return;
        }

        if (player == null) {
            return;
        }

        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        final String itemId = itemStateRepository.getItemId(mainHand).orElse("");

        if ("weapons_the_beginning".equalsIgnoreCase(itemId) || "spirit_dagger".equalsIgnoreCase(itemId)) {
            final boolean apEnabled = itemsConfigService.getWeaponsConfig().getBoolean("daggers.armor-penetration-enabled", true);
            if (!apEnabled) {
                return;
            }
            double ap = 0.3D;
            final ItemStack offHand = player.getInventory().getItemInOffHand();
            final String offHandId = itemStateRepository.getItemId(offHand).orElse("");
            if (("weapons_the_beginning".equalsIgnoreCase(itemId) && "spirit_dagger".equalsIgnoreCase(offHandId))
                    || ("spirit_dagger".equalsIgnoreCase(itemId) && "weapons_the_beginning".equalsIgnoreCase(offHandId))) {
                ap = 0.6D;
            }

            final double base = event.getDamage();
            if (base <= 0.0D) {
                return;
            }

            final double finalDmg = event.getFinalDamage();
            final double armorRatio = Math.max(0.01D, finalDmg / base);

            if (armorRatio < 0.99D) {
                final double regular = base * (1.0D - ap);
                final double pure = base * ap;
                final double newBase = regular + (pure / armorRatio);
                event.setDamage(newBase);
            }
        }
    }
}
