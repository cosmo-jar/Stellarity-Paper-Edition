package dev.cosmojar.stellaritypaper.items.endonomicon.model;

import dev.cosmojar.stellaritypaper.mechanics.altar.AccursedAltarService.AccursedRecipe;

import java.util.List;
import java.util.Set;

public final class RecipeViewModelBuilder {

    public static RecipeViewModel build(AccursedRecipe recipe, RecipeMetadata metadata, RecipeGraph graph) {
        String id = RecipeIndex.getRecipeId(recipe);

        if (metadata == null) {
            metadata = new RecipeMetadata(
                    id,
                    "item.stellarity." + id,
                    List.of(id),
                    Set.of("#misc"),
                    "Предмет Алтаря Проклятых",
                    "🔮",
                    "",
                    "misc"
            );
        }

        DifficultyScore score = new DifficultyScore(recipe);
        String stars = DifficultyFormatter.formatStars(score);
        List<String> usedIn = graph != null ? graph.getUsedIn(id) : List.of();
        List<String> craftedFrom = graph != null ? graph.getCraftedFrom(id) : List.of();

        return new RecipeViewModel(id, recipe, metadata, score, stars, usedIn, craftedFrom);
    }
}
