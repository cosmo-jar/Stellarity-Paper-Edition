package dev.cosmojar.stellaritypaper.items.catalog;

import java.util.List;

public record ItemText(
        String nameTranslateKey,
        List<String> loreTranslateKeys,
        String nameColor
) {
    public ItemText(final String nameTranslateKey, final List<String> loreTranslateKeys) {
        this(nameTranslateKey, loreTranslateKeys, null);
    }

    public ItemText {
        loreTranslateKeys = loreTranslateKeys == null
                ? List.of()
                : List.copyOf(loreTranslateKeys);
    }
}
