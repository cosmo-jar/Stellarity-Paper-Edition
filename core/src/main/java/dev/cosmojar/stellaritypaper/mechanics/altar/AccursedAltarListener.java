package dev.cosmojar.stellaritypaper.mechanics.altar;

import dev.cosmojar.stellaritypaper.text.MessageService;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.plugin.Plugin;

public final class AccursedAltarListener implements Listener {

    private final Plugin plugin;
    private final AccursedAltarService service;
    private final MessageService messageService;
    private final ItemDefinitionRegistry itemRegistry;
    private final CustomItemFactory customItemFactory;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;

    public AccursedAltarListener(
            final Plugin plugin,
            final AccursedAltarService service,
            final MessageService messageService,
            final ItemDefinitionRegistry itemRegistry,
            final CustomItemFactory customItemFactory,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.plugin = plugin;
        this.service = service;
        this.messageService = messageService;
        this.itemRegistry = itemRegistry;
        this.customItemFactory = customItemFactory;
        this.advancementService = advancementService;
    }

    @EventHandler
    public void onDrop(final PlayerDropItemEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        service.handleDrop(event);
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        if (event.getRightClicked() instanceof Interaction hitbox) {
            if (hitbox.getScoreboardTags().contains("stellarity.altar_of_the_accursed.sword_hitbox")) {
                event.setCancelled(true);
                handleSwordHitboxAction(event.getPlayer(), hitbox);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        if (event.getEntity() instanceof Interaction hitbox) {
            if (hitbox.getScoreboardTags().contains("stellarity.altar_of_the_accursed.sword_hitbox")) {
                event.setCancelled(true);
                if (event.getDamager() instanceof Player player) {
                    handleSwordHitboxAction(player, hitbox);
                }
            }
        }
    }

    private void handleSwordHitboxAction(final Player player, final Interaction hitbox) {
        final World world = hitbox.getWorld();
        final org.bukkit.persistence.PersistentDataContainer pdc = world.getPersistentDataContainer();
        
        final NamespacedKey firstDragonDefeatedKey = new NamespacedKey(plugin, "stellarity_first_dragon_defeated");
        final NamespacedKey dragonbladeTakenKey = new NamespacedKey(plugin, "stellarity_dragonblade_taken");
        
        final boolean firstDragonDefeated = pdc.has(firstDragonDefeatedKey, PersistentDataType.BYTE) 
                && pdc.get(firstDragonDefeatedKey, PersistentDataType.BYTE) == (byte) 1;
        final boolean dragonbladeTaken = pdc.has(dragonbladeTakenKey, PersistentDataType.BYTE) 
                && pdc.get(dragonbladeTakenKey, PersistentDataType.BYTE) == (byte) 1;

        if (dragonbladeTaken) {
            hitbox.remove();
            return;
        }

        if (!firstDragonDefeated) {
            net.kyori.adventure.text.Component lockedMsg = net.kyori.adventure.text.Component.translatable("message.stellarity.altar_of_the_accursed_locked")
                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE);
            player.sendActionBar(lockedMsg);
            player.playSound(hitbox.getLocation(), Sound.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0F, 1.0F);
        } else {
            pdc.set(dragonbladeTakenKey, PersistentDataType.BYTE, (byte) 1);
            
            final CustomItemDefinition def = itemRegistry.findByPdcItemId("dragonblade").orElse(null);
            final ItemStack swordItem = def != null ? customItemFactory.create(def) : null;
            
            if (swordItem != null) {
                final org.bukkit.entity.Item dropped = world.dropItem(hitbox.getLocation().add(0, 0.5D, 0), swordItem);
                dropped.setPickupDelay(10);
                dropped.addScoreboardTag("stellarity.altar_of_the_accursed.skip");
            }
            
            world.playSound(hitbox.getLocation(), "stellarity:item.dragonblade.drop", SoundCategory.BLOCKS, 1.5F, 1.0F);
            world.playSound(hitbox.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.5F, 1.0F);
            
            if (advancementService != null) {
                advancementService.grant(player, "stellarity:dragons_den/obtain_dragonblade");
            }
            
            hitbox.remove();
            
            for (final Entity entity : world.getNearbyEntities(hitbox.getLocation(), 2.0D, 2.0D, 2.0D)) {
                if (entity.getScoreboardTags().contains("stellarity.altar_of_the_accursed.sword_holder")) {
                    entity.remove();
                }
            }
        }
    }
}
