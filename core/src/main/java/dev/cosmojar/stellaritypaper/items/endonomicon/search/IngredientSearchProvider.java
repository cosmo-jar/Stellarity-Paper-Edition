package dev.cosmojar.stellaritypaper.items.endonomicon.search;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;

import java.util.*;

public final class IngredientSearchProvider implements SearchProvider {

    @Override
    public Collection<SearchMatch> search(SearchContext context, List<RecipeViewModel> viewModels) {
        String query = context.getNormalizedQuery();
        if (query.isEmpty()) return List.of();

        List<SearchMatch> matches = new ArrayList<>();
        for (RecipeViewModel vm : viewModels) {
            for (String ingredientId : vm.getRecipe().getIngredients().keySet()) {
                String ingNorm = QueryNormalizer.normalize(ingredientId);
                String ruNorm = QueryNormalizer.normalize(translateIngredientToRussian(ingredientId));

                if (ingNorm.contains(query) || ruNorm.contains(query)) {
                    matches.add(new SearchMatch(vm, 60, "Ingredient match: " + ingredientId));
                }
            }
        }
        return matches;
    }

    private String translateIngredientToRussian(String ingId) {
        if (ingId == null) return "";
        String key = ingId.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "elytra" -> "элитры";
            case "red_dye" -> "краситель";
            case "netherite_sword" -> "незеритовый меч";
            case "netherite_pickaxe" -> "незеритовая кирка";
            case "netherite_axe" -> "незеритовый топор";
            case "netherite_shovel" -> "незеритовая лопата";
            case "netherite_hoe" -> "незеритовая мотыга";
            case "netherite_spear" -> "незеритовое копьё копье";
            case "netherite_helmet" -> "незеритовый шлем";
            case "netherite_chestplate" -> "незеритовый нагрудник";
            case "netherite_leggings" -> "незеритовые поножи";
            case "netherite_boots" -> "незеритовые ботинки";
            case "wither_skeleton_skull" -> "череп иссушителя";
            case "enderite_shard" -> "осколок эндерита";
            case "enderite_smithing_template" -> "шаблон эндерита";
            case "starlight_soot" -> "звёздная сажа";
            case "dragon_breath" -> "дыхание дракона";
            case "phantom_membrane" -> "мембрана фантома";
            case "popped_chorus_fruit" -> "жареный корус";
            case "sand_rune" -> "песчаная руна";
            case "breeze_rod" -> "стержень бриза";
            case "trident" -> "трезубец";
            case "shield" -> "щит";
            case "bundle" -> "мешок";
            case "book" -> "книга";
            default -> key;
        };
    }
}
