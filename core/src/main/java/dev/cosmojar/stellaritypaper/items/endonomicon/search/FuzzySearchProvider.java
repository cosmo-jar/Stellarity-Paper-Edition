package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

import java.util.*;

public final class FuzzySearchProvider implements SearchProvider {

    @Override
    public Collection<SearchMatch> search(SearchContext context, List<RecipeViewModel> viewModels) {
        String query = context.getNormalizedQuery();
        if (query.length() < 3) return List.of();

        List<SearchMatch> matches = new ArrayList<>();
        for (RecipeViewModel vm : viewModels) {
            String idNorm = QueryNormalizer.normalize(vm.getId());
            int dist = levenshtein(query, idNorm);
            if (dist <= 2 || (query.length() > 5 && dist <= 3)) {
                matches.add(new SearchMatch(vm, 40, "Fuzzy match (distance: " + dist + ")"));
                continue;
            }

            for (String alias : vm.getMetadata().getAliases()) {
                String aliasNorm = QueryNormalizer.normalize(alias);
                int aliasDist = levenshtein(query, aliasNorm);
                if (aliasDist <= 2 || (query.length() > 5 && aliasDist <= 3)) {
                    matches.add(new SearchMatch(vm, 40, "Fuzzy alias match (distance: " + aliasDist + ")"));
                    break;
                }
            }
        }
        return matches;
    }

    public static int levenshtein(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[len1][len2];
    }
}
