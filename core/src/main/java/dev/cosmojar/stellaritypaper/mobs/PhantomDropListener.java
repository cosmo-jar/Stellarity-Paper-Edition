package dev.cosmojar.stellaritypaper.mobs;

import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class PhantomDropListener implements Listener {

    private final ItemDefinitionRegistry itemRegistry;
    private final CustomItemFactory itemFactory;

    public PhantomDropListener(
            final ItemDefinitionRegistry itemRegistry,
            final CustomItemFactory itemFactory
    ) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Phantom)) {
            return;
        }

        final Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        int lootingLevel = 0;
        final ItemStack hand = killer.getInventory().getItemInMainHand();
        if (hand != null && hand.hasItemMeta()) {
            lootingLevel = hand.getEnchantmentLevel(Enchantment.LOOTING);
        }

        final double chance = 0.003 + (lootingLevel * 0.001);
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }

        final Optional<CustomItemDefinition> definitionOpt = itemRegistry.findByCategoryAndName("armor", "phantom_wings");
        if (definitionOpt.isEmpty()) {
            return;
        }

        final ItemStack wings = itemFactory.create(definitionOpt.get());
        final ItemMeta meta = wings.getItemMeta();
        if (meta instanceof Damageable damageable) {
            final int max = damageable.hasMaxDamage() ? damageable.getMaxDamage() : wings.getType().getMaxDurability();
            
            final double damagePercent = 0.90 + (ThreadLocalRandom.current().nextDouble() * 0.05);
            int damageValue = (int) Math.round(max * damagePercent);
            if (damageValue >= max) {
                damageValue = max - 1;
            }
            damageable.setDamage(damageValue);
            wings.setItemMeta(damageable);
        }

        event.getDrops().add(wings);
    }
}
