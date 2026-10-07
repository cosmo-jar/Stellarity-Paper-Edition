package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.registry.KeyFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Мобильные зачары (dune_speed, plated, soaring).
 */
public final class EnchantMobilityBehaviorService {

    private final EnchantActiveService activeService;
    private final NamespacedKey duneSpeedKey;
    private final NamespacedKey platedArmorKey;
    private final NamespacedKey platedToughnessKey;
    private final NamespacedKey platedGravityKey;
    private final NamespacedKey soaringGravityKey;

    public EnchantMobilityBehaviorService(
            final EnchantActiveService activeService,
            final KeyFactory keyFactory
    ) {
        this.activeService = activeService;
        this.duneSpeedKey = keyFactory.stellarity("enchants.dune_speed");
        this.platedArmorKey = keyFactory.stellarity("enchants.plated.armor");
        this.platedToughnessKey = keyFactory.stellarity("enchants.plated.toughness");
        this.platedGravityKey = keyFactory.stellarity("enchants.plated.gravity");
        this.soaringGravityKey = keyFactory.stellarity("enchants.soaring.gravity");
    }

    public void onPlayerMove(final PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        final Player player = event.getPlayer();
        final List<EnchantActiveService.ActiveEnchant> active = activeService.fromPlayerEquipment(player);
        final int duneLevel = activeService.highestLevel(active, EnchantIds.DUNE_SPEED);
        final boolean shouldBoost = duneLevel > 0 && canUseDuneSpeed(player);
        final double bonus = shouldBoost ? 0.025D + (0.009D * (duneLevel - 1)) : 0.0D;
        setModifier(player, Attribute.MOVEMENT_SPEED, duneSpeedKey, bonus, AttributeModifier.Operation.ADD_NUMBER);

        if (shouldBoost && player.isOnGround() && ThreadLocalRandom.current().nextDouble() < (0.04D * duneLevel)) {
            damageBoots(player);
        }
    }

    public void recalculatePassives(final Player player) {
        final List<EnchantActiveService.ActiveEnchant> active = activeService.fromPlayerEquipment(player);
        final int platedLevel = activeService.highestLevel(active, EnchantIds.PLATED);
        final int soaringLevel = activeService.highestLevel(active, EnchantIds.SOARING);

        final double platedArmor = platedLevel <= 0 ? 0.0D : 2.0D + (platedLevel - 1);
        final double platedToughness = platedLevel >= 3 ? 1.0D : 0.0D;
        final double platedGravity = platedLevel <= 0 ? 0.0D : 0.04D + (0.03D * (platedLevel - 1));
        final double soaringGravity = soaringLevel <= 0 ? 0.0D : -0.05D * soaringLevel;

        setModifier(player, Attribute.ARMOR, platedArmorKey, platedArmor, AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.ARMOR_TOUGHNESS, platedToughnessKey, platedToughness, AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.GRAVITY, platedGravityKey, platedGravity, AttributeModifier.Operation.ADD_SCALAR);
        setModifier(player, Attribute.GRAVITY, soaringGravityKey, soaringGravity, AttributeModifier.Operation.ADD_SCALAR);
    }

    public void clear(final Player player) {
        removeModifier(player, Attribute.MOVEMENT_SPEED, duneSpeedKey);
        removeModifier(player, Attribute.ARMOR, platedArmorKey);
        removeModifier(player, Attribute.ARMOR_TOUGHNESS, platedToughnessKey);
        removeModifier(player, Attribute.GRAVITY, platedGravityKey);
        removeModifier(player, Attribute.GRAVITY, soaringGravityKey);
    }

    private boolean canUseDuneSpeed(final Player player) {
        if (!player.isOnGround() || player.isFlying() || player.getVehicle() != null) {
            return false;
        }
        final Material below = player.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType();
        return below == Material.SAND
                || below == Material.RED_SAND
                || below == Material.SUSPICIOUS_SAND
                || below == Material.GRAVEL
                || below == Material.SUSPICIOUS_GRAVEL
                || below.name().endsWith("CONCRETE_POWDER");
    }

    private void damageBoots(final Player player) {
        final ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType().isAir()) {
            return;
        }
        final ItemMeta meta = boots.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        damageable.setDamage(damageable.getDamage() + 1);
        boots.setItemMeta(damageable);
    }

    private void setModifier(
            final Player player,
            final Attribute attribute,
            final NamespacedKey key,
            final double amount,
            final AttributeModifier.Operation operation
    ) {
        final AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        for (final AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                instance.removeModifier(modifier);
            }
        }
        if (Math.abs(amount) < 1.0E-9D) {
            return;
        }
        instance.addModifier(new AttributeModifier(key, amount, operation));
    }

    private void removeModifier(final Player player, final Attribute attribute, final NamespacedKey key) {
        final AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        for (final AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                instance.removeModifier(modifier);
            }
        }
    }
}
