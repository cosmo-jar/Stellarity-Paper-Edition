package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.integration.WorldGuardHook;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.block.BlockBurnEvent;

public final class CustomBlockListener implements Listener {

    private final Plugin plugin;
    private final CustomBlockService customBlockService;
    private final ItemStateRepository itemStateRepository;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final CustomItemFactory customItemFactory;
    private final NamespacedKey phantomFrameKey;

    public CustomBlockListener(
            final Plugin plugin,
            final CustomBlockService customBlockService,
            final ItemStateRepository itemStateRepository,
            final ItemDefinitionRegistry itemDefinitionRegistry,
            final CustomItemFactory customItemFactory
    ) {
        this.plugin = plugin;
        this.customBlockService = customBlockService;
        this.itemStateRepository = itemStateRepository;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.customItemFactory = customItemFactory;
        this.phantomFrameKey = new NamespacedKey(plugin, "phantom_item_frame");

        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (final World world : org.bukkit.Bukkit.getWorlds()) {
                for (final ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
                    if (itemFrame.getPersistentDataContainer().has(phantomFrameKey, PersistentDataType.BYTE)) {
                        if (itemFrame.getItem().getType() == Material.AIR) {
                            final Location loc = itemFrame.getLocation();
                            world.spawnParticle(Particle.MYCELIUM, loc, 3, 0.25, 0.25, 0.25, 0.0);
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            final Block clicked = event.getClickedBlock();
            if (clicked != null) {
                final String blockId = getCustomBlockIdAt(clicked.getLocation());
                if (blockId != null && blockId.startsWith("pixie_in_a_jar_")) {
                    if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                        if (!WorldGuardHook.canBuild(event.getPlayer(), clicked.getLocation())) {
                            return;
                        }
                        event.setCancelled(true);
                        if (customBlockService.removeBlock(clicked.getLocation(), true)) {
                            clicked.setType(Material.AIR);
                            final Sound sound = getBreakSound(blockId);
                            clicked.getLocation().getWorld().playSound(clicked.getLocation(), sound, 1.0F, 0.8F);
                        }
                    }
                }
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Block clicked = event.getClickedBlock();
            if (clicked != null && clicked.getType() == Material.END_PORTAL_FRAME) {
                if (customBlockService.findCustomBlockDisplay(clicked.getLocation()).isPresent()) {
                    final String id = customBlockService.findCustomBlockDisplay(clicked.getLocation())
                            .map(customBlockService::getBlockId)
                            .orElse(null);
                    if ("altar_of_the_accursed".equals(id)) {
                        event.setCancelled(true);
                        final ItemStack handItem = event.getItem();
                        if (handItem != null && handItem.getType() == Material.ENDER_EYE) {
                            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                                handItem.setAmount(handItem.getAmount() - 1);
                                final Location dropLoc = clicked.getLocation().add(0.5D, 1.2D, 0.5D);
                                final org.bukkit.entity.Item dropped = dropLoc.getWorld().dropItem(dropLoc, new ItemStack(Material.ENDER_EYE));
                                dropped.setPickupDelay(10);
                                dropped.addScoreboardTag("stellarity.altar_of_the_accursed.skip");
                            }
                        }
                        return;
                    }
                }
            }
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        final String blockId = itemStateRepository.getItemId(item).orElse(null);
        if (blockId == null || !customBlockService.isCustomBlock(blockId)) {
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        final Location placeLoc = clickedBlock.getRelative(event.getBlockFace()).getLocation();
        final Block targetBlock = placeLoc.getBlock();

        if (!targetBlock.getType().isAir() && targetBlock.getType() != Material.WATER && targetBlock.getType() != Material.LAVA && !targetBlock.isReplaceable()) {
            return;
        }

        final Player player = event.getPlayer();

        if (!WorldGuardHook.canBuild(player, placeLoc)) {
            return;
        }

        event.setCancelled(true);

        if (customBlockService.placeBlock(placeLoc, blockId, event.getBlockFace())) {
            final Sound sound = getPlaceSound(blockId);
            placeLoc.getWorld().playSound(placeLoc, sound, 1.0F, 0.9F);

            if (player.getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Location loc = block.getLocation();

        if (customBlockService.removeBlock(loc, true)) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            
            final String blockId = customBlockService.findCustomBlockDisplay(loc)
                    .map(customBlockService::getBlockId)
                    .orElse("ender_dirt");
            final Sound sound = getBreakSound(blockId);
            loc.getWorld().playSound(loc, sound, 1.0F, 0.8F);
            return;
        }

        if (block.getType() == Material.END_STONE && event.getPlayer() != null && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.02D) {
                itemDefinitionRegistry.findByPdcItemId("enderite_shard").ifPresent(def -> {
                    final ItemStack shardItem = customItemFactory.create(def);
                    loc.getWorld().dropItemNaturally(loc.clone().add(0.5D, 0.5D, 0.5D), shardItem);
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(final HangingPlaceEvent event) {
        if (event.getEntity() instanceof ItemFrame itemFrame) {
            final ItemStack handItem = event.getItemStack();
            if (handItem != null) {
                final String id = itemStateRepository.getItemId(handItem).orElse(null);
                if ("phantom_item_frame".equalsIgnoreCase(id)) {
                    itemFrame.getPersistentDataContainer().set(phantomFrameKey, PersistentDataType.BYTE, (byte) 1);
                    itemFrame.setVisible(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame itemFrame) {
            if (itemFrame.getPersistentDataContainer().has(phantomFrameKey, PersistentDataType.BYTE)) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> updatePhantomFrameVisibility(itemFrame));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame itemFrame) {
            if (itemFrame.getPersistentDataContainer().has(phantomFrameKey, PersistentDataType.BYTE)) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> updatePhantomFrameVisibility(itemFrame));
            }
        }
    }

    private void updatePhantomFrameVisibility(final ItemFrame itemFrame) {
        if (itemFrame.isValid()) {
            final boolean hasItem = itemFrame.getItem().getType() != Material.AIR;
            itemFrame.setVisible(!hasItem);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(final HangingBreakByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame itemFrame) {
            if (itemFrame.getPersistentDataContainer().has(phantomFrameKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                final Location loc = itemFrame.getLocation();
                itemFrame.remove();

                customBlockService.removeBlock(loc, false);
                itemDefinitionRegistry.findByPdcItemId("phantom_item_frame").ifPresent(def -> {
                    final ItemStack item = customItemFactory.create(def);
                    loc.getWorld().dropItemNaturally(loc, item);
                });
            }
        }
    }

    private boolean isCustomBlockAt(final Location loc) {
        return customBlockService.findCustomBlockDisplay(loc).isPresent();
    }

    private String getCustomBlockIdAt(final Location loc) {
        return customBlockService.findCustomBlockDisplay(loc)
                .map(customBlockService::getBlockId)
                .orElse(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        final java.util.Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            final Location loc = block.getLocation();
            final String blockId = getCustomBlockIdAt(loc);
            if (blockId != null) {
                if ("altar_of_the_sacred".equals(blockId)) {
                    iterator.remove();
                } else {
                    iterator.remove();
                    customBlockService.removeBlock(loc, true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        final java.util.Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            final Location loc = block.getLocation();
            final String blockId = getCustomBlockIdAt(loc);
            if (blockId != null) {
                if ("altar_of_the_sacred".equals(blockId)) {
                    iterator.remove();
                } else {
                    iterator.remove();
                    customBlockService.removeBlock(loc, true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        for (final Block block : event.getBlocks()) {
            if (isCustomBlockAt(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        for (final Block block : event.getBlocks()) {
            if (isCustomBlockAt(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        final Block block = event.getBlock();
        final Location loc = block.getLocation();
        final String blockId = getCustomBlockIdAt(loc);
        if (blockId != null) {
            if ("altar_of_the_sacred".equals(blockId)) {
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
                customBlockService.removeBlock(loc, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        final Block block = event.getBlock();
        final Location loc = block.getLocation();
        final String blockId = getCustomBlockIdAt(loc);
        if (blockId != null) {
            if ("altar_of_the_sacred".equals(blockId)) {
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
                customBlockService.removeBlock(loc, true);
            }
        }
    }

    private Sound getPlaceSound(final String blockId) {
        switch (blockId) {
            case "ashen_froglight":
                return Sound.BLOCK_FROGLIGHT_PLACE;
            case "enderite_block":
                return Sound.BLOCK_NETHERITE_BLOCK_PLACE;
            case "altar_of_the_sacred":
                return Sound.BLOCK_STONE_PLACE;
            case "pixie_in_a_jar_yellow":
            case "pixie_in_a_jar_magenta":
            case "pixie_in_a_jar_lime":
            case "pixie_in_a_jar_light_blue":
            case "pixie_in_a_jar_radiant":
                return Sound.BLOCK_GLASS_PLACE;
            default:
                return Sound.BLOCK_GRASS_PLACE;
        }
    }

    private Sound getBreakSound(final String blockId) {
        switch (blockId) {
            case "ashen_froglight":
                return Sound.BLOCK_FROGLIGHT_BREAK;
            case "enderite_block":
                return Sound.BLOCK_NETHERITE_BLOCK_BREAK;
            case "altar_of_the_sacred":
                return Sound.BLOCK_STONE_BREAK;
            case "pixie_in_a_jar_yellow":
            case "pixie_in_a_jar_magenta":
            case "pixie_in_a_jar_lime":
            case "pixie_in_a_jar_light_blue":
            case "pixie_in_a_jar_radiant":
                return Sound.BLOCK_GLASS_BREAK;
            default:
                return Sound.BLOCK_GRASS_BREAK;
        }
    }
}
