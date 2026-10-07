package dev.cosmojar.stellaritypaper.items.endonomicon.model;

import dev.cosmojar.stellaritypaper.mechanics.altar.AccursedAltarService.AccursedRecipe;

import java.util.*;

public final class RecipeGraph {
    private final Map<String, List<String>> usedInMap;
    private final Map<String, List<String>> craftedFromMap;

    public RecipeGraph(RecipeIndex index) {
        Map<String, Set<String>> usedInTemp = new HashMap<>();
        Map<String, Set<String>> craftedFromTemp = new HashMap<>();

        for (Map.Entry<String, AccursedRecipe> entry : index.getAllRecipes().entrySet()) {
            String recipeId = entry.getKey();
            AccursedRecipe recipe = entry.getValue();

            Set<String> ingredients = new HashSet<>(recipe.getIngredients().keySet());
            if (recipe.getParentIngredient() != null) {
                ingredients.add(recipe.getParentIngredient());
            }

            for (String ing : ingredients) {
                String ingKey = ing.toLowerCase(Locale.ROOT);
                usedInTemp.computeIfAbsent(ingKey, k -> new LinkedHashSet<>()).add(recipeId);

                if (index.getRecipe(ingKey) != null) {
                    craftedFromTemp.computeIfAbsent(recipeId, k -> new LinkedHashSet<>()).add(ingKey);
                }
            }
        }

        Map<String, List<String>> usedInUnmod = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : usedInTemp.entrySet()) {
            usedInUnmod.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.usedInMap = Collections.unmodifiableMap(usedInUnmod);

        Map<String, List<String>> craftedFromUnmod = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : craftedFromTemp.entrySet()) {
            craftedFromUnmod.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.craftedFromMap = Collections.unmodifiableMap(craftedFromUnmod);
    }

    public List<String> getUsedIn(String recipeOrItemId) {
        if (recipeOrItemId == null) return List.of();
        return usedInMap.getOrDefault(recipeOrItemId.toLowerCase(Locale.ROOT), List.of());
    }

    public List<String> getCraftedFrom(String recipeId) {
        if (recipeId == null) return List.of();
        return craftedFromMap.getOrDefault(recipeId.toLowerCase(Locale.ROOT), List.of());
    }
}
