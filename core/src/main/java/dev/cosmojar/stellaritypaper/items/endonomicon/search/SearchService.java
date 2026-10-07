package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

import java.util.*;

public final class SearchService {
    private final List<SearchProvider> providers;

    public SearchService() {
        this.providers = List.of(
                new AliasSearchProvider(),
                new PrefixSearchProvider(),
                new IngredientSearchProvider(),
                new TagSearchProvider(),
                new FuzzySearchProvider()
        );
    }

    public SearchResult search(SearchContext context, List<RecipeViewModel> viewModels) {
        List<SearchMatch> allMatches = new ArrayList<>();
        for (SearchProvider provider : providers) {
            allMatches.addAll(provider.search(context, viewModels));
        }
        return SearchRanker.rank(context.getRawQuery(), allMatches);
    }
}
