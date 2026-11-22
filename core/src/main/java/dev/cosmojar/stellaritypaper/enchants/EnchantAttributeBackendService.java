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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EnchantAttributeBackendService {

    private final EnchantDefinitionRegistry enchantRegistry;
    private final EnchantDataCodec codec;
    private final ItemStateRepository itemStateRepository;
    private final Map<AttributeOperationKey, ResolvedAttributeSpec> resolvedSpecs;

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

        final Map<AttributeOperationKey, ResolvedAttributeSpec> specs = new LinkedHashMap<>();
        for (final EnchantDefinition definition : enchantRegistry.all()) {
            if (definition.backendType() != EnchantBackendType.ATTRIBUTE_BACKEND) {
                continue;
            }
            if ("body".equalsIgnoreCase(definition.slot())) {
                continue;
            }
            final AttributeOperationKey opKey = new AttributeOperationKey(
                    definition.attributeKey(),
                    definition.operation()
            );
            if (specs.containsKey(opKey)) {
                continue;
            }
            final NamespacedKey attributeKey = NamespacedKey.fromString(definition.attributeKey());
            if (attributeKey == null) {
                continue;
            }
            final Attribute attribute = registryService.find(Registry.ATTRIBUTE, attributeKey).orElse(null);
            if (attribute == null) {
                continue;
            }
            final AttributeModifier.Operation operation = parseOperation(definition.operation());
            if (operation == null) {
                continue;
            }
            final NamespacedKey modifierKey = keyFactory.stellarity(
                    "enchants."
                            + sanitizeKey(definition.attributeKey().replace("minecraft:", ""))
                            + "."
                            + sanitizeKey(definition.operation())
            );
            specs.put(opKey, new ResolvedAttributeSpec(attribute, operation, modifierKey));
        }
        this.resolvedSpecs = Map.copyOf(specs);
    }

    public void recalculate(final Player player) {
        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            clear(player);
            return;
        }

        final Map<AttributeOperationKey, Double> totals = new HashMap<>();
        collectFromItem(totals, equipment.getItemInMainHand(), "mainhand");
        collectFromItem(totals, equipment.getItemInOffHand(), "offhand");
        collectFromItem(totals, equipment.getHelmet(), "head");
        collectFromItem(totals, equipment.getChestplate(), "chest");
        collectFromItem(totals, equipment.getLeggings(), "legs");
        collectFromItem(totals, equipment.getBoots(), "feet");

        for (final Map.Entry<AttributeOperationKey, ResolvedAttributeSpec> entry : resolvedSpecs.entrySet()) {
            final ResolvedAttributeSpec spec = entry.getValue();
            final double desiredAmount = totals.getOrDefault(entry.getKey(), 0.0D);
            syncModifier(player, spec, desiredAmount);
        }
    }

    public void clear(final Player player) {
        for (final ResolvedAttributeSpec spec : resolvedSpecs.values()) {
            final AttributeInstance instance = player.getAttribute(spec.attribute());
            if (instance == null) {
                continue;
            }
            final AttributeModifier existing = instance.getModifier(spec.modifierKey());
            if (existing != null) {
                instance.removeModifier(existing);
            }
        }
    }

    private void collectFromItem(
            final Map<AttributeOperationKey, Double> totals,
            final ItemStack item,
            final String sourceSlot
    ) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()
                || item.getType() == org.bukkit.Material.ENCHANTED_BOOK || item.getType() == org.bukkit.Material.BOOK) {
            return;
        }

        final String raw = itemStateRepository.getEnchantsData(item).orElse("");
        if (raw.isEmpty()) {
            return;
        }
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

    private void syncModifier(
            final Player player,
            final ResolvedAttributeSpec spec,
            final double desiredAmount
    ) {
        final AttributeInstance instance = player.getAttribute(spec.attribute());
        if (instance == null) {
            return;
        }

        final AttributeModifier existing = instance.getModifier(spec.modifierKey());
        if (Math.abs(desiredAmount) < 1.0E-9D) {
            if (existing != null) {
                instance.removeModifier(existing);
            }
            return;
        }

        if (existing != null) {
            if (existing.getOperation() == spec.operation()
                    && Math.abs(existing.getAmount() - desiredAmount) < 1.0E-9D) {
                return;
            }
            instance.removeModifier(existing);
        }

        instance.addModifier(new AttributeModifier(spec.modifierKey(), desiredAmount, spec.operation()));
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

    private record ResolvedAttributeSpec(
            Attribute attribute,
            AttributeModifier.Operation operation,
            NamespacedKey modifierKey
    ) {
    }
}
