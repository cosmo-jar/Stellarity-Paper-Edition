package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

public final class SearchMatch {
    private final RecipeViewModel viewModel;
    private final int score;
    private final String matchReason;

    public SearchMatch(RecipeViewModel viewModel, int score, String matchReason) {
        this.viewModel = viewModel;
        this.score = score;
        this.matchReason = matchReason;
    }

    public RecipeViewModel getViewModel() { return viewModel; }
    public int getScore() { return score; }
    public String getMatchReason() { return matchReason; }
}
