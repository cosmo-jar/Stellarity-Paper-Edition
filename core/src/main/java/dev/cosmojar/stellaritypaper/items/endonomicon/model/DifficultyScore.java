package dev.cosmojar.stellaritypaper.items.endonomicon.model;

import dev.cosmojar.stellaritypaper.mechanics.altar.AccursedAltarService.AccursedRecipe;

import java.util.Locale;

public final class DifficultyScore {
    private final int rawScore;

    public DifficultyScore(AccursedRecipe recipe) {
        int score = 0;
        int totalItems = 0;

        for (var entry : recipe.getIngredients().entrySet()) {
            String id = entry.getKey().toLowerCase(Locale.ROOT);
            int count = entry.getValue();
            totalItems += count;

            if (id.contains("nether_star")) {
                score += 35 * count;
            } else if (id.contains("enderite_smithing_template")) {
                score += 25 * count;
            } else if (id.contains("enderite_shard")) {
                score += 3 * count;
            } else if (id.contains("wither_skeleton_skull") || id.contains("wither_skull")) {
                score += 20 * count;
            } else if (id.contains("shulker_shell")) {
                score += 10 * count;
            } else if (id.contains("elytra")) {
                score += 30;
            } else {
                score += 2 * count;
            }
        }

        if (recipe.getParentIngredient() != null) {
            score += 15;
        }

        if (totalItems > 10) score += 10;
        if (totalItems > 30) score += 15;

        this.rawScore = Math.min(100, score);
    }

    public int getRawScore() {
        return rawScore;
    }
}
