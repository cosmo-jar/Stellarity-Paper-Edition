package dev.cosmojar.stellaritypaper.enchants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Кодек данных unlock-системы Harvester в PDC.
 */
public final class HarvesterAbilityCodec {

    public String encodeAbilities(final List<String> abilityIds) {
        final Set<String> deduplicated = new LinkedHashSet<>();
        for (final String id : abilityIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            deduplicated.add(id.trim().toLowerCase(java.util.Locale.ROOT));
        }
        final JsonArray array = new JsonArray();
        for (final String id : deduplicated) {
            array.add(id);
        }
        return array.toString();
    }

    public List<String> decodeAbilities(final String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        final JsonElement parsed = JsonParser.parseString(raw);
        if (!parsed.isJsonArray()) {
            return List.of();
        }
        final List<String> values = new ArrayList<>();
        for (final JsonElement element : parsed.getAsJsonArray()) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            final String value = element.getAsString();
            if (value == null || value.isBlank()) {
                continue;
            }
            values.add(value.trim().toLowerCase(java.util.Locale.ROOT));
        }
        return values.stream().distinct().toList();
    }

    public String encodeProgress(final Map<String, Integer> progress) {
        final JsonObject object = new JsonObject();
        progress.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null && entry.getValue() > 0)
                .sorted(Comparator.comparing(entry -> entry.getKey().toLowerCase(java.util.Locale.ROOT)))
                .forEach(entry -> object.addProperty(entry.getKey().toLowerCase(java.util.Locale.ROOT), entry.getValue()));
        return object.toString();
    }

    public Map<String, Integer> decodeProgress(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        final JsonElement parsed = JsonParser.parseString(raw);
        if (!parsed.isJsonObject()) {
            return Map.of();
        }
        final Map<String, Integer> values = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (!entry.getValue().isJsonPrimitive()) {
                continue;
            }
            final int progress = entry.getValue().getAsInt();
            if (progress <= 0) {
                continue;
            }
            values.put(entry.getKey().trim().toLowerCase(java.util.Locale.ROOT), progress);
        }
        return values;
    }
}

