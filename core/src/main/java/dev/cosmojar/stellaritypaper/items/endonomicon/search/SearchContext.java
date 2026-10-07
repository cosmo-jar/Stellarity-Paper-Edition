package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import org.bukkit.entity.Player;

import java.util.Set;

public final class SearchContext {
    private final String rawQuery;
    private final String normalizedQuery;
    private final Player player;
    private final String locale;
    private final Set<String> filters;
    private final int page;
    private final String sortMode;

    public SearchContext(String rawQuery, Player player, String locale, Set<String> filters, int page, String sortMode) {
        this.rawQuery = rawQuery != null ? rawQuery : "";
        this.normalizedQuery = QueryNormalizer.normalize(this.rawQuery);
        this.player = player;
        this.locale = locale != null ? locale : "en_us";
        this.filters = filters != null ? Set.copyOf(filters) : Set.of();
        this.page = Math.max(1, page);
        this.sortMode = sortMode != null ? sortMode : "relevance";
    }

    public String getRawQuery() { return rawQuery; }
    public String getNormalizedQuery() { return normalizedQuery; }
    public Player getPlayer() { return player; }
    public String getLocale() { return locale; }
    public Set<String> getFilters() { return filters; }
    public int getPage() { return page; }
    public String getSortMode() { return sortMode; }
}
