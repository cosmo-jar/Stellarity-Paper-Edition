package dev.cosmojar.stellaritypaper.mechanics.structures;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Optional;

public final class EndCityVaultListener implements Listener {

    private final Plugin plugin;
    private final CustomLootService customLootService;
    private final ItemStateRepository itemStateRepository;
    private final MessageService messageService;
    private final EndCityCrystalService endCityCrystalService;
    private final NamespacedKey openedPlayersKey;
    private final NamespacedKey vaultTypeKey;

    public EndCityVaultListener(
            final Plugin plugin,
            final CustomLootService customLootService,
            final ItemStateRepository itemStateRepository,
            final MessageService messageService,
            final EndCityCrystalService endCityCrystalService
    ) {
        this.plugin = plugin;
        this.customLootService = customLootService;
        this.itemStateRepository = itemStateRepository;
        this.messageService = messageService;
        this.endCityCrystalService = endCityCrystalService;
        this.openedPlayersKey = new NamespacedKey(plugin, "opened_players");
        this.vaultTypeKey = new NamespacedKey(plugin, "vault_type");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVaultInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.VAULT) {
            return;
        }

        final Player player = event.getPlayer();
        final EquipmentSlot hand = event.getHand();
        final ItemStack heldItem = hand == EquipmentSlot.OFF_HAND ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();

        if (heldItem == null || heldItem.getType().isAir()) {
            return;
        }

        final Location loc = block.getLocation();
        final World world = loc.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.THE_END) {
            return;
        }
        if (!world.hasStructureAt(loc, Structure.END_CITY) && (endCityCrystalService == null || endCityCrystalService.getStructureKey(loc) == null)) {
            return;
        }

        final Optional<String> pdcIdOpt = itemStateRepository.getItemId(heldItem);
        if (pdcIdOpt.isEmpty()) {
            if (heldItem.getType() == Material.TRIAL_KEY || heldItem.getType() == Material.OMINOUS_TRIAL_KEY) {
                event.setCancelled(true);
                player.sendActionBar(messageService.message("mechanics.vault.wrong_key"));
                world.playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            }
            return;
        }

        final String heldKeyPdcId = pdcIdOpt.get();
        final String requiredVaultType = determineVaultType(block);

        final String expectedKeyId = switch (requiredVaultType) {
            case "VAULT_ELYTRA" -> "winged_key";
            case "VAULT_OMINOUS" -> "gilded_purpur_key";
            default -> "purpur_key";
        };

        if (!expectedKeyId.equalsIgnoreCase(heldKeyPdcId)) {
            if ("purpur_key".equalsIgnoreCase(heldKeyPdcId) || "gilded_purpur_key".equalsIgnoreCase(heldKeyPdcId) || "winged_key".equalsIgnoreCase(heldKeyPdcId)) {
                event.setCancelled(true);
                player.sendActionBar(messageService.message("mechanics.vault.wrong_key"));
                world.playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            }
            return;
        }

        // Блокировка хранилища корабля в энд сити (VAULT_ELYTRA) защитными кристаллами
        if ("VAULT_ELYTRA".equalsIgnoreCase(requiredVaultType)) {
            String structureKey = null;
            if (block.getState() instanceof TileState tileState) {
                structureKey = endCityCrystalService.readStructureKey(tileState);
            }
            if (structureKey == null || structureKey.isBlank()) {
                structureKey = endCityCrystalService.getStructureKey(loc);
            }

            // защита - если не удалось определить структуру, замок не открывается
            if (structureKey == null || structureKey.isBlank()) {
                event.setCancelled(true);
                dev.cosmojar.stellaritypaper.util.DebugLog.log(plugin, "[WARN] VAULT_ELYTRA at "
                        + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                        + " has no structure_key! Interaction blocked.");
                player.sendActionBar(messageService.message("mechanics.vault.locked_by_crystals"));
                world.playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                return;
            }

            if (!endCityCrystalService.isCityConquered(world, structureKey)) {
                event.setCancelled(true);
                player.sendActionBar(messageService.message("mechanics.vault.locked_by_crystals"));
                world.playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                return;
            }
        }

        event.setCancelled(true);

        if (block.getState() instanceof TileState tileState) {
            final PersistentDataContainer pdc = tileState.getPersistentDataContainer();
            final String openedData = pdc.getOrDefault(openedPlayersKey, PersistentDataType.STRING, "");
            final String playerUuid = player.getUniqueId().toString();

            if (openedData.contains(playerUuid)) {
                player.sendActionBar(messageService.message("mechanics.vault.already_opened"));
                world.playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                return;
            }

            final List<ItemStack> rewards = customLootService.getVaultLoot("EndCity", requiredVaultType);
            if (rewards.isEmpty()) {
                return;
            }

            final String newData = openedData.isEmpty() ? playerUuid : openedData + "," + playerUuid;
            pdc.set(openedPlayersKey, PersistentDataType.STRING, newData);
            tileState.update(true);

            heldItem.setAmount(heldItem.getAmount() - 1);

            world.playSound(loc, Sound.BLOCK_VAULT_OPEN_SHUTTER, 1.0f, 1.0f);
            world.playSound(loc, Sound.BLOCK_VAULT_INSERT_ITEM, 1.0f, 1.0f);
            world.spawnParticle(Particle.VAULT_CONNECTION, loc.clone().add(0.5, 0.5, 0.5), 25, 0.3, 0.3, 0.3, 0.05);

            ejectRewardsSequentially(loc, rewards);
        }
    }

    private String determineVaultType(final Block block) {
        if (block.getState() instanceof TileState tileState) {
            final String taggedType = tileState.getPersistentDataContainer().get(vaultTypeKey, PersistentDataType.STRING);
            if (taggedType != null && !taggedType.isEmpty()) {
                return taggedType;
            }
        }

        final boolean ominous = block.getBlockData() instanceof org.bukkit.block.data.type.Vault vaultData && vaultData.isOminous();
        if (ominous) {
            if (isEndShipLocation(block.getLocation())) {
                return "VAULT_ELYTRA";
            }
            return "VAULT_OMINOUS";
        }
        return "VAULT";
    }

    private boolean isEndShipLocation(final Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        final Block center = loc.getBlock();
        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    final Material type = center.getRelative(dx, dy, dz).getType();
                    if (type == Material.DRAGON_HEAD || type == Material.DRAGON_WALL_HEAD) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void ejectRewardsSequentially(final Location blockLoc, final List<ItemStack> rewards) {
        final World world = blockLoc.getWorld();
        if (world == null) {
            return;
        }
        final Location ejectLoc = blockLoc.clone().add(0.5, 1.1, 0.5);
        final int taskPeriodTicks = 6;

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (rewards.isEmpty()) {
                task.cancel();
                return;
            }

            final ItemStack reward = rewards.remove(0);
            final Item itemEntity = world.dropItem(ejectLoc, reward.clone());
            itemEntity.setVelocity(new Vector((Math.random() - 0.5) * 0.12, 0.28, (Math.random() - 0.5) * 0.12));

            world.playSound(ejectLoc, Sound.BLOCK_VAULT_EJECT_ITEM, 1.0f, 1.0f);
            world.spawnParticle(Particle.VAULT_CONNECTION, ejectLoc, 15, 0.2, 0.2, 0.2, 0.05);
        }, 8L, taskPeriodTicks);
    }
}
