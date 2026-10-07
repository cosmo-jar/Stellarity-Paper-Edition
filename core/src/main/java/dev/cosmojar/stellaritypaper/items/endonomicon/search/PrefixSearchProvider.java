package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

import java.util.*;

public final class PrefixSearchProvider implements SearchProvider {

    @Override
    public Collection<SearchMatch> search(SearchContext context, List<RecipeViewModel> viewModels) {
        String query = context.getNormalizedQuery();
        if (query.isEmpty()) return List.of();

        List<SearchMatch> matches = new ArrayList<>();
        for (RecipeViewModel vm : viewModels) {
            String idNorm = QueryNormalizer.normalize(vm.getId());
            if (idNorm.startsWith(query)) {
                matches.add(new SearchMatch(vm, 80, "Prefix match"));
            }
        }
        return matches;
    }
}
