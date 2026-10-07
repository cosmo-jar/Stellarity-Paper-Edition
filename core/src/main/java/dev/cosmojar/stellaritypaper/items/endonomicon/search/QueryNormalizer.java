package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import java.text.Normalizer;
import java.util.Locale;

public final class QueryNormalizer {

    public static String normalize(String input) {
        if (input == null) return "";
        String text = input.trim().toLowerCase(Locale.ROOT);
        text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        text = text.replaceAll("[_\\-.,!?:;\"'()]+", " ");
        return text.replaceAll("\\s+", " ").trim();
    }
}
