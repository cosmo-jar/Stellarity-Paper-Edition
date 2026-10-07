package dev.cosmojar.stellaritypaper.enchants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EnchantDataCodec {

    public String encode(final List<EnchantInstance> instances) {
        final JsonArray array = new JsonArray();
        instances.stream()
                .sorted(Comparator.comparing(EnchantInstance::id).thenComparingInt(EnchantInstance::level))
                .forEach(instance -> {
                    final JsonObject object = new JsonObject();
                    object.addProperty("id", instance.id());
                    object.addProperty("level", instance.level());
                    array.add(object);
                });
        return array.toString();
    }

    public List<EnchantInstance> decode(final String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        final JsonElement parsed = JsonParser.parseString(raw);
        if (!parsed.isJsonArray()) {
            return List.of();
        }

        final List<EnchantInstance> values = new ArrayList<>();
        for (final JsonElement element : parsed.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject object = element.getAsJsonObject();
            final JsonElement idElement = object.get("id");
            final JsonElement levelElement = object.get("level");
            if (idElement == null || levelElement == null || !idElement.isJsonPrimitive() || !levelElement.isJsonPrimitive()) {
                continue;
            }

            final String id = idElement.getAsString();
            final int level = levelElement.getAsInt();
            if (id == null || id.isBlank() || level <= 0) {
                continue;
            }
            values.add(new EnchantInstance(id, level));
        }
        return values.stream()
                .sorted(Comparator.comparing(EnchantInstance::id).thenComparingInt(EnchantInstance::level))
                .toList();
    }
}

