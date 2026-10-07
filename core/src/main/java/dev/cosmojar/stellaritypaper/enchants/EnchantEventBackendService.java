package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class EnchantEventBackendService {

    private final EnchantDefinitionRegistry enchantRegistry;
    private final EnchantDataCodec enchantDataCodec;
    private final ItemStateRepository itemStateRepository;

    public EnchantEventBackendService(
            final EnchantDefinitionRegistry enchantRegistry,
            final EnchantDataCodec enchantDataCodec,
            final ItemStateRepository itemStateRepository
    ) {
        this.enchantRegistry = enchantRegistry;
        this.enchantDataCodec = enchantDataCodec;
        this.itemStateRepository = itemStateRepository;
    }

    public void onDamageByEntity(final EntityDamageByEntityEvent event) {
        applyOutgoingAttackDamage(event);
    }

    public void onEntityDamage(final EntityDamageEvent event) {
        applyFallDamageMultiplier(event);
    }

    private void applyOutgoingAttackDamage(final EntityDamageByEntityEvent event) {
        final Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        final double baseDamage = event.getDamage();
        double addValue = 0.0D;
        double addMultipliedBase = 0.0D;
        double addMultipliedTotalFactor = 1.0D;

        for (final ActiveEnchant active : activeEnchants(attacker)) {
            final EnchantDefinition definition = active.definition();
            if (definition.backendType() != EnchantBackendType.EVENT_BACKEND) {
                continue;
            }
            if (!"minecraft:attack_damage".equals(definition.attributeKey())) {
                continue;
            }
            if (!slotMatches(definition.slot(), active.sourceSlot())) {
                continue;
            }

            final double amount = definition.resolveAmount(active.instance().level());
            switch (definition.operation()) {
                case "add_value" -> addValue += amount;
                case "add_multiplied_base" -> addMultipliedBase += amount;
                case "add_multiplied_total" -> addMultipliedTotalFactor *= (1.0D + amount);
                default -> {
                }
            }
        }

        if (addValue == 0.0D && addMultipliedBase == 0.0D && addMultipliedTotalFactor == 1.0D) {
            return;
        }

        final double calculated = (baseDamage + addValue + (baseDamage * addMultipliedBase)) * addMultipliedTotalFactor;
        event.setDamage(Math.max(0.0D, calculated));
    }

    private void applyFallDamageMultiplier(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        final double baseDamage = event.getDamage();
        double addValue = 0.0D;
        double addMultipliedBase = 0.0D;
        double addMultipliedTotalFactor = 1.0D;

        for (final ActiveEnchant active : activeEnchants(victim)) {
            final EnchantDefinition definition = active.definition();
            if (definition.backendType() != EnchantBackendType.EVENT_BACKEND) {
                continue;
            }
            if (!"minecraft:fall_damage_multiplier".equals(definition.attributeKey())) {
                continue;
            }
            if (!slotMatches(definition.slot(), active.sourceSlot())) {
                continue;
            }

            final double amount = definition.resolveAmount(active.instance().level());
            switch (definition.operation()) {
                case "add_value" -> addValue += amount;
                case "add_multiplied_base" -> addMultipliedBase += amount;
                case "add_multiplied_total" -> addMultipliedTotalFactor *= (1.0D + amount);
                default -> {
                }
            }
        }

        if (addValue == 0.0D && addMultipliedBase == 0.0D && addMultipliedTotalFactor == 1.0D) {
            return;
        }

        final double calculated = (baseDamage + addValue + (baseDamage * addMultipliedBase)) * addMultipliedTotalFactor;
        event.setDamage(Math.max(0.0D, calculated));
    }

    private List<ActiveEnchant> activeEnchants(final Player player) {
        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return List.of();
        }

        final java.util.ArrayList<ActiveEnchant> values = new java.util.ArrayList<>();
        collect(values, equipment.getItemInMainHand(), "mainhand");
        collect(values, equipment.getItemInOffHand(), "offhand");
        collect(values, equipment.getHelmet(), "head");
        collect(values, equipment.getChestplate(), "chest");
        collect(values, equipment.getLeggings(), "legs");
        collect(values, equipment.getBoots(), "feet");
        return values;
    }

    private void collect(final List<ActiveEnchant> output, final ItemStack item, final String sourceSlot) {
        if (item == null || item.getType().isAir() || item.getType() == org.bukkit.Material.ENCHANTED_BOOK || item.getType() == org.bukkit.Material.BOOK) {
            return;
        }
        final List<EnchantInstance> instances = enchantDataCodec.decode(itemStateRepository.getEnchantsData(item).orElse(""));
        for (final EnchantInstance instance : instances) {
            final EnchantDefinition definition = enchantRegistry.findById(instance.id()).orElse(null);
            if (definition == null) {
                continue;
            }
            output.add(new ActiveEnchant(definition, instance, sourceSlot));
        }
    }

    private boolean slotMatches(final String targetSlot, final String sourceSlot) {
        if (targetSlot.equalsIgnoreCase(sourceSlot)) {
            return true;
        }
        return targetSlot.equalsIgnoreCase("hand")
                && (sourceSlot.equalsIgnoreCase("mainhand") || sourceSlot.equalsIgnoreCase("offhand"));
    }

    private Player resolveAttacker(final Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private record ActiveEnchant(
            EnchantDefinition definition,
            EnchantInstance instance,
            String sourceSlot
    ) {
    }
}
