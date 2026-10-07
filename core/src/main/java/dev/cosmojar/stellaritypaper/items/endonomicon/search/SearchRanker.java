package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

import java.util.*;

public final class SearchRanker {

    public static SearchResult rank(String query, Collection<SearchMatch> rawMatches) {
        Map<String, List<SearchMatch>> matchesByRecipe = new LinkedHashMap<>();

        for (SearchMatch match : rawMatches) {
            String id = match.getViewModel().getId();
            matchesByRecipe.computeIfAbsent(id, k -> new ArrayList<>()).add(match);
        }

        List<SearchMatch> rankedList = new ArrayList<>();
        for (var entry : matchesByRecipe.values()) {
            RecipeViewModel vm = entry.get(0).getViewModel();
            int maxScore = 0;
            StringBuilder reasons = new StringBuilder();

            for (SearchMatch m : entry) {
                if (m.getScore() > maxScore) {
                    maxScore = m.getScore();
                }
                if (reasons.length() > 0) reasons.append(", ");
                reasons.append(m.getMatchReason());
            }

            int comboBonus = (entry.size() - 1) * 5;
            int finalScore = Math.min(100, maxScore + comboBonus);

            rankedList.add(new SearchMatch(vm, finalScore, reasons.toString()));
        }

        rankedList.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        return new SearchResult(query, rankedList);
    }
}
