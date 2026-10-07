package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.registry.KeyFactory;
import dev.cosmojar.stellaritypaper.registry.MinecraftRegistryService;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EnchantAttributeBackendService {

    private final EnchantDefinitionRegistry enchantRegistry;
    private final EnchantDataCodec codec;
    private final ItemStateRepository itemStateRepository;
    private final MinecraftRegistryService registryService;
    private final KeyFactory keyFactory;
    private final String namespace;

    public EnchantAttributeBackendService(
            final EnchantDefinitionRegistry enchantRegistry,
            final EnchantDataCodec codec,
            final ItemStateRepository itemStateRepository,
            final MinecraftRegistryService registryService,
            final KeyFactory keyFactory
    ) {
        this.enchantRegistry = enchantRegistry;
        this.codec = codec;
        this.itemStateRepository = itemStateRepository;
        this.registryService = registryService;
        this.keyFactory = keyFactory;
        this.namespace = keyFactory.stellarity("marker").getNamespace();
    }

    public void recalculate(final Player player) {
        clear(player);
        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return;
        }

        final Map<AttributeOperationKey, Double> totals = new HashMap<>();
        collectFromItem(totals, equipment.getItemInMainHand(), "mainhand");
        collectFromItem(totals, equipment.getItemInOffHand(), "offhand");
        collectFromItem(totals, equipment.getHelmet(), "head");
        collectFromItem(totals, equipment.getChestplate(), "chest");
        collectFromItem(totals, equipment.getLeggings(), "legs");
        collectFromItem(totals, equipment.getBoots(), "feet");

        for (final Map.Entry<AttributeOperationKey, Double> entry : totals.entrySet()) {
            if (Math.abs(entry.getValue()) < 1.0E-9) {
                continue;
            }
            applyModifier(player, entry.getKey(), entry.getValue());
        }
    }

    public void clear(final Player player) {
        for (final EnchantDefinition definition : enchantRegistry.all()) {
            final NamespacedKey attributeKey = NamespacedKey.fromString(definition.attributeKey());
            if (attributeKey == null) {
                continue;
            }
            final Attribute attribute = registryService.find(Registry.ATTRIBUTE, attributeKey).orElse(null);
            if (attribute == null) {
                continue;
            }

            final AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            final List<AttributeModifier> modifiers = new ArrayList<>(instance.getModifiers());
            for (final AttributeModifier modifier : modifiers) {
                if (isOurModifier(modifier)) {
                    instance.removeModifier(modifier);
                }
            }
        }
    }

    private void collectFromItem(
            final Map<AttributeOperationKey, Double> totals,
            final ItemStack item,
            final String sourceSlot
    ) {
        if (item == null || item.getType().isAir() || item.getType() == org.bukkit.Material.ENCHANTED_BOOK || item.getType() == org.bukkit.Material.BOOK) {
            return;
        }
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        final String raw = itemStateRepository.getEnchantsData(item).orElse("");
        final List<EnchantInstance> instances = codec.decode(raw);
        for (final EnchantInstance instance : instances) {
            final EnchantDefinition definition = enchantRegistry.findById(instance.id()).orElse(null);
            if (definition == null || definition.backendType() != EnchantBackendType.ATTRIBUTE_BACKEND) {
                continue;
            }
            if (!slotMatches(definition.slot(), sourceSlot)) {
                continue;
            }

            final AttributeOperationKey aggregateKey = new AttributeOperationKey(
                    definition.attributeKey(),
                    definition.operation()
            );
            totals.merge(aggregateKey, definition.resolveAmount(instance.level()), Double::sum);
        }
    }

    private void applyModifier(
            final Player player,
            final AttributeOperationKey key,
            final double amount
    ) {
        final NamespacedKey attributeKey = NamespacedKey.fromString(key.attributeKey());
        if (attributeKey == null) {
            return;
        }
        final Attribute attribute = registryService.find(Registry.ATTRIBUTE, attributeKey).orElse(null);
        if (attribute == null) {
            return;
        }

        final AttributeModifier.Operation operation = parseOperation(key.operation());
        if (operation == null) {
            return;
        }

        final AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        final NamespacedKey modifierKey = keyFactory.stellarity(
                "enchants."
                        + sanitizeKey(key.attributeKey().replace("minecraft:", ""))
                        + "."
                        + sanitizeKey(key.operation())
        );
        final AttributeModifier modifier = new AttributeModifier(modifierKey, amount, operation);
        instance.addModifier(modifier);
    }

    private boolean slotMatches(final String targetSlot, final String sourceSlot) {
        final String normalizedTarget = normalize(targetSlot);
        final String normalizedSource = normalize(sourceSlot);
        if (normalizedTarget.equals(normalizedSource)) {
            return true;
        }
        return normalizedTarget.equals("hand") && (
                normalizedSource.equals("mainhand")
                        || normalizedSource.equals("offhand")
        );
    }

    private AttributeModifier.Operation parseOperation(final String rawOperation) {
        final String normalized = normalize(rawOperation);
        return switch (normalized) {
            case "add_value" -> AttributeModifier.Operation.ADD_NUMBER;
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_SCALAR;
            case "add_multiplied_total" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> null;
        };
    }

    private boolean isOurModifier(final AttributeModifier modifier) {
        final NamespacedKey key = modifier.getKey();
        return key != null
                && namespace.equalsIgnoreCase(key.getNamespace())
                && key.getKey().startsWith("enchants.");
    }

    private String sanitizeKey(final String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    private String normalize(final String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private record AttributeOperationKey(String attributeKey, String operation) {
    }
}
