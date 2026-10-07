package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.items.catalog.ItemText;
import dev.cosmojar.stellaritypaper.items.catalog.ItemTextCatalog;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class ItemDefinitionRegistry {

    private final Map<String, CustomItemDefinition> byCategoryAndName = new HashMap<>();
    private final Map<String, CustomItemDefinition> byPdcItemId = new HashMap<>();
    private final Map<String, Set<String>> itemsByCategory = new HashMap<>();
    private final Set<String> categories = new TreeSet<>();

    public ItemDefinitionRegistry(
            final ItemTextCatalog itemTextCatalog,
            final Collection<StellarityItemDefinition> definitions
    ) {
        registerAll(itemTextCatalog, definitions);
        validateNoOrphanCatalogEntries(itemTextCatalog);
    }

    public Optional<CustomItemDefinition> findByCategoryAndName(final String category, final String commandName) {
        final String normCat = normalize(category);
        final String normCmd = normalize(commandName);
        CustomItemDefinition def = byCategoryAndName.get(toCategoryItemKey(normCat, normCmd));
        if (def == null && "trinkets".equals(normCat) && "endermans_hand".equals(normCmd)) {
            def = byCategoryAndName.get(toCategoryItemKey(normCat, "enderman_hand"));
        }
        return Optional.ofNullable(def);
    }

    public Optional<CustomItemDefinition> findByPdcItemId(final String pdcItemId) {
        if (pdcItemId == null) {
            return Optional.empty();
        }
        final String norm = normalize(pdcItemId);
        CustomItemDefinition def = byPdcItemId.get(norm);
        if (def == null && "endermans_hand".equals(norm)) {
            def = byPdcItemId.get("enderman_hand");
        }
        return Optional.ofNullable(def);
    }

    public Set<String> listCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public Set<String> listItemsByCategory(final String category) {
        final Set<String> values = itemsByCategory.get(normalize(category));
        if (values == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(values);
    }

    public int size() {
        return byCategoryAndName.size();
    }

    private void registerAll(final ItemTextCatalog itemTextCatalog, final Collection<StellarityItemDefinition> definitions) {
        int index = 0;
        for (final StellarityItemDefinition definition : definitions) {
            index++;
            if (definition == null) {
                throw new IllegalStateException("Null item definition at index " + index);
            }

            final String category = normalize(definition.category());
            final String commandName = normalize(definition.commandName());
            final String pdcItemId = normalize(definition.pdcItemId());
            final String key = toCategoryItemKey(category, commandName);

            if (byCategoryAndName.containsKey(key)) {
                throw new IllegalStateException("Duplicate definition category+command: " + key);
            }
            if (byPdcItemId.containsKey(pdcItemId)) {
                throw new IllegalStateException("Duplicate definition pdc_item_id: " + pdcItemId);
            }

            final ItemText text = itemTextCatalog.find(category, commandName)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing text catalog entry for item definition: " + key
                    ));

            final CustomItemDefinition runtimeDefinition = new CustomItemDefinition(definition, text);
            byCategoryAndName.put(key, runtimeDefinition);
            byPdcItemId.put(pdcItemId, runtimeDefinition);
            categories.add(category);
            itemsByCategory.computeIfAbsent(category, ignored -> new TreeSet<>()).add(commandName);
        }
    }

    private void validateNoOrphanCatalogEntries(final ItemTextCatalog itemTextCatalog) {
        for (final String category : itemTextCatalog.listCategories()) {
            for (final String commandName : itemTextCatalog.listItemsByCategory(category)) {
                final String key = toCategoryItemKey(category, commandName);
                if (!byCategoryAndName.containsKey(key)) {
                    throw new IllegalStateException("Catalog has item without definition class: " + key);
                }
            }
        }
    }

    private String normalize(final String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private String toCategoryItemKey(final String category, final String commandName) {
        return category + "|" + commandName;
    }
}
