package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

import java.util.*;

public final class TagSearchProvider implements SearchProvider {

    @Override
    public Collection<SearchMatch> search(SearchContext context, List<RecipeViewModel> viewModels) {
        String query = context.getNormalizedQuery();
        if (query.isEmpty()) return List.of();

        List<SearchMatch> matches = new ArrayList<>();
        for (RecipeViewModel vm : viewModels) {
            for (String tag : vm.getMetadata().getTags()) {
                String tagNorm = QueryNormalizer.normalize(tag);
                if (tagNorm.contains(query) || (query.startsWith("#") && tagNorm.contains(query.substring(1)))) {
                    matches.add(new SearchMatch(vm, 20, "Tag match: " + tag));
                }
            }
        }
        return matches;
    }
}
