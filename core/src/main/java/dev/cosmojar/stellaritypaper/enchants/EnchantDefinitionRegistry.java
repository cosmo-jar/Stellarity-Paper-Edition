package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EnchantDefinitionRegistry {

    private final Map<String, EnchantDefinition> byId = new HashMap<>();

    public EnchantDefinitionRegistry(final Collection<EnchantDefinition> definitions) {
        for (final EnchantDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }
            final String id = normalize(definition.id());
            if (id.isBlank()) {
                throw new IllegalStateException("Enchant definition has blank id");
            }
            if (definition.tiers().isEmpty()) {
                throw new IllegalStateException("Enchant definition has no tiers: " + definition.id());
            }
            if (byId.putIfAbsent(id, definition) != null) {
                throw new IllegalStateException("Duplicate enchant id: " + definition.id());
            }
        }
    }

    public Optional<EnchantDefinition> findById(final String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(normalize(id)));
    }

    public List<EnchantDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }

    public void validateItemBindings(final Collection<StellarityItemDefinition> itemDefinitions) {
        for (final StellarityItemDefinition itemDefinition : itemDefinitions) {
            for (final EnchantInstance instance : itemDefinition.components().enchants()) {
                final EnchantDefinition definition = findById(instance.id()).orElse(null);
                if (definition == null) {
                    throw new IllegalStateException("Unknown enchant id '" + instance.id()
                            + "' in item " + itemDefinition.category() + "/" + itemDefinition.commandName());
                }
                if (!definition.tiers().containsKey(instance.level())) {
                    throw new IllegalStateException("Unknown enchant level " + instance.level() + " for id '"
                            + instance.id() + "' in item " + itemDefinition.category() + "/" + itemDefinition.commandName());
                }
            }
        }
    }

    private String normalize(final String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
