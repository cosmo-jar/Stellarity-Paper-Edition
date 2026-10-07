package dev.cosmojar.stellaritypaper.items.endonomicon.model;

import dev.cosmojar.stellaritypaper.mechanics.altar.AccursedAltarService.AccursedRecipe;

import java.util.List;

public final class RecipeViewModel {
    private final String id;
    private final AccursedRecipe recipe;
    private final RecipeMetadata metadata;
    private final DifficultyScore difficultyScore;
    private final String difficultyStars;
    private final List<String> usedIn;
    private final List<String> craftedFrom;

    public RecipeViewModel(
            String id,
            AccursedRecipe recipe,
            RecipeMetadata metadata,
            DifficultyScore difficultyScore,
            String difficultyStars,
            List<String> usedIn,
            List<String> craftedFrom
    ) {
        this.id = id;
        this.recipe = recipe;
        this.metadata = metadata;
        this.difficultyScore = difficultyScore;
        this.difficultyStars = difficultyStars;
        this.usedIn = usedIn != null ? List.copyOf(usedIn) : List.of();
        this.craftedFrom = craftedFrom != null ? List.copyOf(craftedFrom) : List.of();
    }

    public String getId() { return id; }
    public AccursedRecipe getRecipe() { return recipe; }
    public RecipeMetadata getMetadata() { return metadata; }
    public DifficultyScore getDifficultyScore() { return difficultyScore; }
    public String getDifficultyStars() { return difficultyStars; }
    public List<String> getUsedIn() { return usedIn; }
    public List<String> getCraftedFrom() { return craftedFrom; }
}
