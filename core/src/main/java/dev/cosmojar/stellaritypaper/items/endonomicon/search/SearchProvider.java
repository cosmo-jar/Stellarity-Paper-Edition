package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

import java.util.Collection;
import java.util.List;

public interface SearchProvider {
    Collection<SearchMatch> search(SearchContext context, List<RecipeViewModel> viewModels);
}
