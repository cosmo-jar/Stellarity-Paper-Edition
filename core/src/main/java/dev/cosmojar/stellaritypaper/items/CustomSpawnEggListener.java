package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.mobs.EndMobVariantService;
import dev.cosmojar.stellaritypaper.mobs.variants.MobVariant;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class CustomSpawnEggListener implements Listener {

    private final CustomItemMatcher itemMatcher;
    private final EndMobVariantService endMobVariantService;

    public CustomSpawnEggListener(final CustomItemMatcher itemMatcher, final EndMobVariantService endMobVariantService) {
        this.itemMatcher = itemMatcher;
        this.endMobVariantService = endMobVariantService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        final String customId = itemMatcher.resolveRawId(item).orElse("");
        if (customId.isEmpty()) {
            return;
        }

        final MobVariant mobVariant;

        switch (customId.toLowerCase()) {
            case "voided_zombie_spawn_egg":
                mobVariant = MobVariant.VOIDED_ZOMBIE;
                break;
            case "voided_skeleton_spawn_egg":
                mobVariant = MobVariant.VOIDED_SKELETON;
                break;
            case "voided_slime_spawn_egg":
                mobVariant = MobVariant.VOIDED_SLIME;
                break;
            case "flesh_piglin_spawn_egg":
                mobVariant = MobVariant.FLESH_PIGLIN;
                break;
            case "voided_silverfish_spawn_egg":
                mobVariant = MobVariant.VOIDED_SILVERFISH;
                break;
            default:
                return;
        }

        event.setCancelled(true);

        final Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        final Location spawnLoc = block.getRelative(event.getBlockFace()).getLocation().add(0.5, 0.0, 0.5);
        final Player player = event.getPlayer();

        final Entity spawned = spawnLoc.getWorld().spawnEntity(spawnLoc, mobVariant.getTargetType(), CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        endMobVariantService.setVariant(spawned, mobVariant);

        if (event.getHand() == EquipmentSlot.HAND) {
            player.getInventory().getItemInMainHand().subtract(1);
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            player.getInventory().getItemInOffHand().subtract(1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final EquipmentSlot hand = event.getHand();
        final ItemStack item = hand == EquipmentSlot.HAND ? 
                player.getInventory().getItemInMainHand() : 
                player.getInventory().getItemInOffHand();
        
        if (item == null || item.getType().isAir()) {
            return;
        }
        
        final String customId = itemMatcher.resolveRawId(item).orElse("");
        if (customId.isEmpty()) {
            return;
        }

        if (customId.contains("spawn_egg")) {
            event.setCancelled(true);
        }
    }
}
