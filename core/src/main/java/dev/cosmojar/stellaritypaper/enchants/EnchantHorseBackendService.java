package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.registry.KeyFactory;
import dev.cosmojar.stellaritypaper.registry.MinecraftRegistryService;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EnchantHorseBackendService {

    private final EnchantDefinitionRegistry enchantRegistry;
    private final EnchantDataCodec codec;
    private final ItemStateRepository itemStateRepository;
    private final MinecraftRegistryService registryService;
    private final KeyFactory keyFactory;
    private final String namespace;

    public EnchantHorseBackendService(
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

    public void recalculate(final AbstractHorse horse) {
        clear(horse);
        final ItemStack bodyItem = getBodyItem(horse);
        if (bodyItem == null || bodyItem.getType().isAir()) {
            return;
        }
        final ItemMeta meta = bodyItem.getItemMeta();
        if (meta == null) {
            return;
        }

        final String raw = itemStateRepository.getEnchantsData(bodyItem).orElse("");
        final List<EnchantInstance> instances = codec.decode(raw);
        final Map<AttributeOperationKey, Double> totals = new HashMap<>();

        for (final EnchantInstance instance : instances) {
            final EnchantDefinition definition = enchantRegistry.findById(instance.id()).orElse(null);
            if (definition == null || definition.backendType() != EnchantBackendType.ATTRIBUTE_BACKEND) {
                continue;
            }
            if (!"body".equalsIgnoreCase(definition.slot())) {
                continue;
            }

            final AttributeOperationKey key = new AttributeOperationKey(
                    definition.attributeKey(),
                    definition.operation()
            );
            totals.merge(key, definition.resolveAmount(instance.level()), Double::sum);
        }

        for (final Map.Entry<AttributeOperationKey, Double> entry : totals.entrySet()) {
            if (Math.abs(entry.getValue()) < 1.0E-9D) {
                continue;
            }
            applyModifier(horse, entry.getKey(), entry.getValue());
        }
    }

    public void clear(final AbstractHorse horse) {
        for (final EnchantDefinition definition : enchantRegistry.all()) {
            if (!"body".equalsIgnoreCase(definition.slot())) {
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
            final AttributeInstance instance = horse.getAttribute(attribute);
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

    private ItemStack getBodyItem(final AbstractHorse horse) {
        final EntityEquipment equipment = horse.getEquipment();
        if (equipment == null) {
            return null;
        }
        return equipment.getItem(EquipmentSlot.BODY);
    }

    private void applyModifier(
            final AbstractHorse horse,
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
        final AttributeInstance instance = horse.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        final AttributeModifier.Operation operation = parseOperation(key.operation());
        if (operation == null) {
            return;
        }

        final NamespacedKey modifierKey = keyFactory.stellarity(
                "enchants.horse."
                        + sanitizeKey(key.attributeKey().replace("minecraft:", ""))
                        + "."
                        + sanitizeKey(key.operation())
        );
        final AttributeModifier modifier = new AttributeModifier(modifierKey, amount, operation);
        instance.addModifier(modifier);
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
                && key.getKey().startsWith("enchants.horse.");
    }

    private String normalize(final String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeKey(final String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    private record AttributeOperationKey(String attributeKey, String operation) {
    }
}
