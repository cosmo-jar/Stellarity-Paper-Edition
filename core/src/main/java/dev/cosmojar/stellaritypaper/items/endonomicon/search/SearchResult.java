package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import java.util.List;

public final class SearchResult {
    private final String query;
    private final List<SearchMatch> matches;
    private final int totalResults;

    public SearchResult(String query, List<SearchMatch> matches) {
        this.query = query != null ? query : "";
        this.matches = matches != null ? List.copyOf(matches) : List.of();
        this.totalResults = this.matches.size();
    }

    public String getQuery() { return query; }
    public List<SearchMatch> getMatches() { return matches; }
    public int getTotalResults() { return totalResults; }
}
