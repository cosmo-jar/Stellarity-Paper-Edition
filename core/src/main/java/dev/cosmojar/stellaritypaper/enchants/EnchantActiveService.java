package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Считывает активные зачарования из PDC предметов экипировки игрока.
 */
public final class EnchantActiveService {

    private final EnchantDefinitionRegistry enchantRegistry;
    private final EnchantDataCodec codec;
    private final ItemStateRepository itemStateRepository;

    public EnchantActiveService(
            final EnchantDefinitionRegistry enchantRegistry,
            final EnchantDataCodec codec,
            final ItemStateRepository itemStateRepository
    ) {
        this.enchantRegistry = enchantRegistry;
        this.codec = codec;
        this.itemStateRepository = itemStateRepository;
    }

    public List<ActiveEnchant> fromPlayerEquipment(final Player player) {
        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return List.of();
        }

        final List<ActiveEnchant> values = new ArrayList<>();
        collect(values, equipment.getItemInMainHand(), "mainhand");
        collect(values, equipment.getItemInOffHand(), "offhand");
        collect(values, equipment.getHelmet(), "head");
        collect(values, equipment.getChestplate(), "chest");
        collect(values, equipment.getLeggings(), "legs");
        collect(values, equipment.getBoots(), "feet");
        return values;
    }

    public List<ActiveEnchant> fromItem(final ItemStack item, final String sourceSlot) {
        final List<ActiveEnchant> values = new ArrayList<>();
        collect(values, item, sourceSlot);
        return values;
    }

    public int highestLevel(final List<ActiveEnchant> activeEnchants, final String enchantId) {
        int max = 0;
        final String normalized = normalize(enchantId);
        for (final ActiveEnchant active : activeEnchants) {
            if (!normalize(active.instance().id()).equals(normalized)) {
                continue;
            }
            if (!slotMatches(active.definition().slot(), active.sourceSlot())) {
                continue;
            }
            if (active.instance().level() > max) {
                max = active.instance().level();
            }
        }
        return max;
    }

    public boolean hasAny(final List<ActiveEnchant> activeEnchants, final String enchantId) {
        return highestLevel(activeEnchants, enchantId) > 0;
    }

    public boolean slotMatches(final String targetSlot, final String sourceSlot) {
        final String normalizedTarget = normalize(targetSlot);
        final String normalizedSource = normalize(sourceSlot);
        if (normalizedTarget.equals(normalizedSource)) {
            return true;
        }
        if (normalizedTarget.equals("hand")) {
            return normalizedSource.equals("mainhand") || normalizedSource.equals("offhand");
        }
        return normalizedTarget.equals("body") && normalizedSource.equals("chest");
    }

    private void collect(final List<ActiveEnchant> output, final ItemStack item, final String sourceSlot) {
        if (item == null || item.getType().isAir() || item.getType() == org.bukkit.Material.ENCHANTED_BOOK || item.getType() == org.bukkit.Material.BOOK) {
            return;
        }
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        final List<EnchantInstance> instances = codec.decode(itemStateRepository.getEnchantsData(item).orElse(""));
        for (final EnchantInstance instance : instances) {
            final EnchantDefinition definition = enchantRegistry.findById(instance.id()).orElse(null);
            if (definition == null) {
                continue;
            }
            output.add(new ActiveEnchant(definition, instance, sourceSlot, item));
        }
    }

    private String normalize(final String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record ActiveEnchant(
            EnchantDefinition definition,
            EnchantInstance instance,
            String sourceSlot,
            ItemStack sourceItem
    ) {
    }
}
