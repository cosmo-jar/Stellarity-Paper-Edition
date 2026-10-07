package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

import java.util.*;

public final class AliasSearchProvider implements SearchProvider {

    @Override
    public Collection<SearchMatch> search(SearchContext context, List<RecipeViewModel> viewModels) {
        String query = context.getNormalizedQuery();
        if (query.isEmpty()) return List.of();

        List<SearchMatch> matches = new ArrayList<>();
        for (RecipeViewModel vm : viewModels) {
            for (String alias : vm.getMetadata().getAliases()) {
                String aliasNorm = QueryNormalizer.normalize(alias);
                if (aliasNorm.equalsIgnoreCase(query)) {
                    matches.add(new SearchMatch(vm, 100, "Exact alias match"));
                } else if (aliasNorm.contains(query)) {
                    matches.add(new SearchMatch(vm, 90, "Alias contains query"));
                }
            }
        }
        return matches;
    }
}
