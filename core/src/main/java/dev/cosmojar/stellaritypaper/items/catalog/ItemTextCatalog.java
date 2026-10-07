package dev.cosmojar.stellaritypaper.items.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.items.api.ItemTextRef;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class ItemTextCatalog {

    private static final String CATALOG_PATH = "items/items.json";

    private final Map<String, ItemText> byCategoryAndName = new HashMap<>();
    private final Map<String, Set<String>> itemsByCategory = new HashMap<>();
    private final Set<String> categories = new TreeSet<>();

    public ItemTextCatalog(final StellarityPaperPlugin plugin) {
        load(plugin);
    }

    public ItemText get(final ItemTextRef ref) {
        return get(ref.category(), ref.commandName());
    }

    public ItemText get(final String category, final String commandName) {
        return find(category, commandName)
                .orElseThrow(() -> new IllegalStateException(
                        "Item text is missing in catalog for: " + normalize(category) + "/" + normalize(commandName)
                ));
    }

    public Optional<ItemText> find(final String category, final String commandName) {
        final String key = toCategoryItemKey(normalize(category), normalize(commandName));
        return Optional.ofNullable(byCategoryAndName.get(key));
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

    private void load(final StellarityPaperPlugin plugin) {
        final InputStream stream = plugin.getResource(CATALOG_PATH);
        if (stream == null) {
            throw new IllegalStateException("Catalog not found in resources: " + CATALOG_PATH);
        }

        try (InputStream input = stream;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            final JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                throw new IllegalStateException("Catalog root must be object grouped by category: " + CATALOG_PATH);
            }
            loadCategories(root.getAsJsonObject());
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to load item text catalog: " + CATALOG_PATH, ex);
        }
    }

    private void loadCategories(final JsonObject root) {
        int categoryIndex = 0;
        for (final Map.Entry<String, JsonElement> categoryEntry : root.entrySet()) {
            categoryIndex++;
            final String category = normalize(categoryEntry.getKey());
            final JsonElement itemsElement = categoryEntry.getValue();
            if (!itemsElement.isJsonObject()) {
                throw new IllegalStateException("Category '" + category + "' must be object at index " + categoryIndex);
            }

            final JsonObject itemsObject = itemsElement.getAsJsonObject();
            for (final Map.Entry<String, JsonElement> itemEntry : itemsObject.entrySet()) {
                final String commandName = normalize(itemEntry.getKey());
                if (!itemEntry.getValue().isJsonObject()) {
                    throw new IllegalStateException(
                            "Item '" + category + "/" + commandName + "' must be object in catalog"
                    );
                }

                final JsonObject textObject = itemEntry.getValue().getAsJsonObject();
                final String nameTranslateKey = requiredString(
                        textObject,
                        "name_translate_key",
                        category,
                        commandName
                );
                final List<String> loreKeys = stringArray(
                        textObject,
                        "lore_translate_keys",
                        category,
                        commandName
                );
                final String nameColor = optionalString(textObject, "name_color");

                register(category, commandName, new ItemText(nameTranslateKey, loreKeys, nameColor));
            }
        }
    }

    private void register(final String category, final String commandName, final ItemText text) {
        final String key = toCategoryItemKey(category, commandName);
        if (byCategoryAndName.containsKey(key)) {
            throw new IllegalStateException("Duplicate catalog entry for: " + key);
        }

        byCategoryAndName.put(key, text);
        categories.add(category);
        itemsByCategory.computeIfAbsent(category, ignored -> new TreeSet<>()).add(commandName);
    }

    private String requiredString(
            final JsonObject object,
            final String field,
            final String category,
            final String commandName
    ) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            throw new IllegalStateException("Missing '" + field + "' for " + category + "/" + commandName);
        }
        final String value = object.get(field).getAsString();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Blank '" + field + "' for " + category + "/" + commandName);
        }
        return value;
    }

    private String optionalString(
            final JsonObject object,
            final String field
    ) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            return null;
        }
        final String value = object.get(field).getAsString();
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private List<String> stringArray(
            final JsonObject object,
            final String field,
            final String category,
            final String commandName
    ) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            return List.of();
        }
        final JsonElement element = object.get(field);
        if (!element.isJsonArray()) {
            throw new IllegalStateException("Field '" + field + "' must be array for " + category + "/" + commandName);
        }

        final List<String> values = new ArrayList<>();
        for (final JsonElement line : element.getAsJsonArray()) {
            if (!line.isJsonPrimitive()) {
                continue;
            }
            final String value = line.getAsString();
            if (value != null) {
                values.add(value.trim());
            }
        }
        return Collections.unmodifiableList(values);
    }

    private String normalize(final String input) {
        return input.toLowerCase(Locale.ROOT).trim();
    }

    private String toCategoryItemKey(final String category, final String commandName) {
        return category + "|" + commandName;
    }
}
