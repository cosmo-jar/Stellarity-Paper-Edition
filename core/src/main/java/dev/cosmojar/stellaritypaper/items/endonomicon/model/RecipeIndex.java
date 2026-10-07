package dev.cosmojar.stellaritypaper.items.endonomicon.model;

import dev.cosmojar.stellaritypaper.mechanics.altar.AccursedAltarService.AccursedRecipe;

import java.util.*;

public final class RecipeIndex {
    private final Map<String, AccursedRecipe> idToRecipe;

    public RecipeIndex(List<AccursedRecipe> recipes) {
        Map<String, AccursedRecipe> mapById = new HashMap<>();
        Map<String, List<AccursedRecipe>> mapByIngredient = new HashMap<>();

        for (AccursedRecipe recipe : recipes) {
            String key = getRecipeId(recipe);
            mapById.put(key, recipe);

            for (String ingredient : recipe.getIngredients().keySet()) {
                mapByIngredient.computeIfAbsent(ingredient.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(recipe);
            }
        }

        this.idToRecipe = Collections.unmodifiableMap(mapById);
        Map<String, List<AccursedRecipe>> unmodIngredients = new HashMap<>();
        for (Map.Entry<String, List<AccursedRecipe>> entry : mapByIngredient.entrySet()) {
            unmodIngredients.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
    }

    public static String getRecipeId(AccursedRecipe recipe) {
        if (recipe.getResultCustomId() != null) {
            return recipe.getResultCustomId();
        } else if (recipe.getResultVanillaItem() != null) {
            return recipe.getResultVanillaItem().getType().name().toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }

    public AccursedRecipe getRecipe(String id) {
        if (id == null) return null;
        return idToRecipe.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, AccursedRecipe> getAllRecipes() {
        return idToRecipe;
    }
}
