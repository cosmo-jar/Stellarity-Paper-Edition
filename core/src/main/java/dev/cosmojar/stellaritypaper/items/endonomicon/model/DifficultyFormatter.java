package dev.cosmojar.stellaritypaper.items.endonomicon.model;

public final class DifficultyFormatter {

    public static String formatStars(DifficultyScore score) {
        int val = score.getRawScore();
        if (val <= 20) return "⭐☆☆☆☆";
        if (val <= 40) return "⭐⭐☆☆☆";
        if (val <= 65) return "⭐⭐⭐☆☆";
        if (val <= 85) return "⭐⭐⭐⭐☆";
        return "⭐⭐⭐⭐⭐";
    }
}
