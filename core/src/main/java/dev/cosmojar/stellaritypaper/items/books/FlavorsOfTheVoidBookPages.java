package dev.cosmojar.stellaritypaper.items.books;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.inventory.meta.BookMeta;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FlavorsOfTheVoidBookPages {

    private static final List<Component> CACHED_PAGES = new ArrayList<>();
    private static volatile boolean loaded = false;
    private static final Object LOCK = new Object();

    private FlavorsOfTheVoidBookPages() {}

    public static List<Component> getPages() {
        if (!loaded) {
            synchronized (LOCK) {
                if (!loaded) {
                    loadPages();
                    loaded = true;
                }
            }
        }
        return Collections.unmodifiableList(CACHED_PAGES);
    }

    public static void apply(final BookMeta bookMeta) {
        if (bookMeta == null) {
            return;
        }
        bookMeta.setTitle("");
        bookMeta.setAuthor("");
        final List<Component> pages = getPages();
        if (!pages.isEmpty()) {
            for (final Component page : pages) {
                bookMeta.addPages(page);
            }
        }
    }

    private static void loadPages() {
        try (final InputStream in = FlavorsOfTheVoidBookPages.class.getResourceAsStream("/books/flavors_of_the_void.json")) {
            if (in == null) {
                return;
            }
            try (final InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                final JsonArray pools = root.getAsJsonArray("pools");
                if (pools == null || pools.isEmpty()) {
                    return;
                }
                final JsonArray entries = pools.get(0).getAsJsonObject().getAsJsonArray("entries");
                if (entries == null || entries.isEmpty()) {
                    return;
                }
                final JsonArray modifiers = entries.get(0).getAsJsonObject().getAsJsonArray("modifier");
                if (modifiers == null || modifiers.isEmpty()) {
                    return;
                }
                final JsonObject components = modifiers.get(0).getAsJsonObject().getAsJsonObject("components");
                if (components == null) {
                    return;
                }
                final JsonObject bookContent = components.getAsJsonObject("minecraft:written_book_content");
                if (bookContent == null) {
                    return;
                }
                final JsonArray pagesArray = bookContent.getAsJsonArray("pages");
                if (pagesArray == null) {
                    return;
                }

                final GsonComponentSerializer serializer = GsonComponentSerializer.gson();
                for (final JsonElement pageElement : pagesArray) {
                    final Component pageComponent = serializer.deserializeFromTree(pageElement);
                    CACHED_PAGES.add(pageComponent);
                }
            }
        } catch (final Exception e) {
            org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.WARNING, "Failed to load Flavors of The Void book pages", e);
        }
    }
}
