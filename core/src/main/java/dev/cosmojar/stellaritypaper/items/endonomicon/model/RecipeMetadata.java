package dev.cosmojar.stellaritypaper.items.endonomicon.model;

import java.util.List;
import java.util.Set;

public final class RecipeMetadata {
    private final String id;
    private final String translationKey;
    private final List<String> aliases;
    private final Set<String> tags;
    private final String description;
    private final String icon;
    private final String loreNote;
    private final String category;

    public RecipeMetadata(String id, String translationKey, List<String> aliases, Set<String> tags, String description, String icon, String loreNote, String category) {
        this.id = id;
        this.translationKey = translationKey;
        this.aliases = aliases != null ? List.copyOf(aliases) : List.of();
        this.tags = tags != null ? Set.copyOf(tags) : Set.of();
        this.description = description != null ? description : "";
        this.icon = icon != null ? icon : "🔮";
        this.loreNote = loreNote != null ? loreNote : "";
        this.category = category != null ? category : "misc";
    }

    public String getId() { return id; }
    public String getTranslationKey() { return translationKey; }
    public List<String> getAliases() { return aliases; }
    public Set<String> getTags() { return tags; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public String getLoreNote() { return loreNote; }
    public String getCategory() { return category; }
}
