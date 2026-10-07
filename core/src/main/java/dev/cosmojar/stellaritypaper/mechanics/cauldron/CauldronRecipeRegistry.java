package dev.cosmojar.stellaritypaper.mechanics.cauldron;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CauldronRecipeRegistry {

    public record CauldronRecipe(
            String id,
            Map<String, Integer> ingredients,
            String resultId,
            boolean isCustomResult,
            int breathCost
    ) {}

    private final List<CauldronRecipe> recipes = new ArrayList<>();

    public CauldronRecipeRegistry() {

        register("chorus_stew", Map.of(v(Material.CHORUS_FRUIT), 1, v(Material.BOWL), 1, v(Material.CHORUS_FLOWER), 1), "chorus_stew", true, 2);

        register("experience_bottle", Map.of(v(Material.GLASS_BOTTLE), 1, v(Material.LAPIS_LAZULI), 2), Material.EXPERIENCE_BOTTLE.getKey().getKey(), false, 1);

        register("golden_chorus_fruit", Map.of(v(Material.CHORUS_FRUIT), 1, v(Material.GOLD_BLOCK), 1), "golden_chorus_fruit", true, 3);

        register("candied_chorus_fruit", Map.of(v(Material.CHORUS_FRUIT), 1, v(Material.SUGAR), 2), "candied_chorus_fruit", true, 1);

        register("fried_chorus_fruit", Map.of(v(Material.CHORUS_FRUIT), 1, v(Material.WHEAT), 1, v(Material.BLAZE_POWDER), 1), "fried_chorus_fruit", true, 1);

        register("grilled_enderman_flesh", Map.of(c("enderman_flesh"), 1, v(Material.BLAZE_POWDER), 1), "grilled_enderman_flesh", true, 1);

        register("frozen_carpaccio", Map.of(c("enderman_flesh"), 1, v(Material.ICE), 1), "frozen_carpaccio", true, 1);

        register("chorus_pie", Map.of(v(Material.CHORUS_FRUIT), 1, v(Material.SUGAR), 1, c("ender_egg"), 1), "chorus_pie", true, 2);

        register("pho", Map.of(c("golden_chorus_fruit"), 1, v(Material.BOWL), 1, c("grilled_enderman_flesh"), 1), "pho", true, 4);

        register("shepherds_pie", Map.of(c("shulker_body"), 1, v(Material.GOLDEN_CARROT), 1, v(Material.BAKED_POTATO), 1, v(Material.CHORUS_FRUIT), 2), "shepherds_pie", true, 5);

        register("sushi", Map.of(c("ender_koi"), 1, v(Material.DRIED_KELP), 1), "sushi", true, 1);

        register("prismatic_sushi", Map.of(c("prismite"), 1, v(Material.DRIED_KELP), 1), "prismatic_sushi", true, 2);

        register("chorus_juice", Map.of(v(Material.CHORUS_FRUIT), 2, v(Material.GLASS_BOTTLE), 1), "chorus_juice", true, 2);


        register("blind_rage_potion", Map.of(c("fleshy_piranha"), 1, v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1, v(Material.GLOWSTONE_DUST), 1), "blind_rage_potion", true, 2);

        register("blind_rage_potion_ext", Map.of(c("blind_rage_potion"), 1, v(Material.REDSTONE), 1), "blind_rage_potion_ext", true, 3);

        register("endurance_potion", Map.of(v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1, v(Material.IRON_INGOT), 1, v(Material.POPPED_CHORUS_FRUIT), 1), "endurance_potion", true, 2);

        register("endurance_potion_ext", Map.of(c("endurance_potion"), 1, v(Material.REDSTONE), 1), "endurance_potion_ext", true, 3);

        register("endurance_potion_ii", Map.of(c("endurance_potion"), 1, v(Material.GLOWSTONE_DUST), 1), "endurance_potion_ii", true, 3);

        register("entanglement_potion", Map.of(v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1, c("overgrown_cod"), 1, v(Material.GUNPOWDER), 1), "entanglement_potion", true, 2);

        register("entanglement_potion_ext", Map.of(c("entanglement_potion"), 1, v(Material.REDSTONE), 1), "entanglement_potion_ext", true, 3);

        register("entanglement_potion_ii", Map.of(c("entanglement_potion"), 1, v(Material.GLOWSTONE_DUST), 1), "entanglement_potion_ii", true, 3);

        register("frost_cloud_potion", Map.of(c("frost_minnow"), 1, v(Material.GLASS_BOTTLE), 1, v(Material.PACKED_ICE), 2, v(Material.NETHER_WART), 1), "frost_cloud_potion", true, 4);

        register("hellfire_treader_potion", Map.of(v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1, c("flarefin_koi"), 1, v(Material.SUGAR), 1), "hellfire_treader_potion", true, 2);

        register("hellfire_treader_potion_ext", Map.of(c("hellfire_treader_potion"), 1, v(Material.REDSTONE), 1), "hellfire_treader_potion_ext", true, 3);

        register("hellfire_treader_potion_ii", Map.of(c("hellfire_treader_potion"), 1, v(Material.GLOWSTONE_DUST), 1), "hellfire_treader_potion_ii", true, 3);

        register("lifeforce_potion", Map.of(v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1, c("prismite"), 1, c("starlight_soot"), 1), "lifeforce_potion", true, 3);

        register("lifeforce_potion_ext", Map.of(c("lifeforce_potion"), 1, v(Material.REDSTONE), 1), "lifeforce_potion_ext", true, 4);

        register("lifeforce_potion_ii", Map.of(c("lifeforce_potion"), 1, v(Material.GLOWSTONE_DUST), 1), "lifeforce_potion_ii", true, 3);

        register("spelunker_potion_brown", Map.of(v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1, v(Material.BROWN_MUSHROOM), 1, v(Material.GOLD_INGOT), 1), "spelunker_potion", true, 2);

        register("spelunker_potion_red", Map.of(v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1, v(Material.RED_MUSHROOM), 1, v(Material.GOLD_INGOT), 1), "spelunker_potion", true, 2);

        register("spelunker_potion_ext", Map.of(c("spelunker_potion"), 1, v(Material.REDSTONE), 1), "spelunker_potion_ext", true, 3);

        register("spelunker_potion_ii", Map.of(c("spelunker_potion"), 1, v(Material.GLOWSTONE_DUST), 1), "spelunker_potion_ii", true, 3);

        register("luck_potion", Map.of(v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1, v(Material.SHORT_GRASS), 2, v(Material.DIAMOND), 1), "luck_potion", true, 5);

        register("poseidons_nectar", Map.of(c("bubblefish"), 1, v(Material.NAUTILUS_SHELL), 1, v(Material.PRISMARINE_SHARD), 1, v(Material.HONEY_BOTTLE), 1, v(Material.KELP), 1), "poseidons_nectar", true, 6);

        register("red_potion", Map.of(v(Material.WITHER_ROSE), 1, v(Material.WITHER_SKELETON_SKULL), 1, v(Material.RED_DYE), 1, v(Material.SWEET_BERRIES), 1, v(Material.NETHER_WART_BLOCK), 1, v(Material.GLASS_BOTTLE), 1), "red_potion", true, 7);

        register("regeneraga", Map.of(c("crimson_tigerfish"), 1, c("prismite"), 1, v(Material.GLISTERING_MELON_SLICE), 1, v(Material.GHAST_TEAR), 1, v(Material.NETHER_WART), 1, v(Material.GLASS_BOTTLE), 1), "regeneraga", true, 4);

        register("regeneraga_ii", Map.of(c("regeneraga"), 1, c("starlight_soot"), 1), "regeneraga_ii", true, 4);

        register("royal_jelly", Map.of(v(Material.HONEY_BOTTLE), 1, c("starlight_soot"), 1, v(Material.AMETHYST_SHARD), 2), "royal_jelly", true, 5);

        register("royal_jelly_ii", Map.of(c("royal_jelly"), 1, c("starlight_soot"), 1), "royal_jelly_ii", true, 3);


        register("ender_dirt", Map.of(v(Material.END_STONE), 1, v(Material.DIRT), 1), "ender_dirt", true, 2);

        register("rooted_ender_dirt", Map.of(v(Material.END_STONE), 1, v(Material.ROOTED_DIRT), 1), "rooted_ender_dirt", true, 2);

        register("ender_grass_block", Map.of(v(Material.END_STONE), 1, v(Material.GRASS_BLOCK), 1), "ender_grass_block", true, 2);
    }

    private static String v(final Material material) {
        return "vanilla:" + material.getKey().getKey();
    }

    private static String c(final String customId) {
        return "custom:" + customId;
    }

    private void register(final String id, final Map<String, Integer> ingredients, final String resultId, final boolean isCustomResult, final int breathCost) {
        recipes.add(new CauldronRecipe(id, new HashMap<>(ingredients), resultId, isCustomResult, breathCost));
    }


    public CauldronRecipe findMatch(final Map<String, Integer> currentIngredients) {
        for (final CauldronRecipe recipe : recipes) {
            if (recipe.ingredients().equals(currentIngredients)) {
                return recipe;
            }
        }
        return null;
    }
}
