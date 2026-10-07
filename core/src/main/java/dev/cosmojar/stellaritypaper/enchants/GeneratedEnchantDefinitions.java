package dev.cosmojar.stellaritypaper.enchants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedEnchantDefinitions {
    private GeneratedEnchantDefinitions() {}

    public static List<EnchantDefinition> create() {
        return List.of(
                new EnchantDefinition("armor_add_value_chest", "minecraft:armor", "add_value", "chest", tiers(3, 8), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.armor_add_value_chest"),
                new EnchantDefinition("armor_add_value_feet", "minecraft:armor", "add_value", "feet", tiers(3), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.armor_add_value_feet"),
                new EnchantDefinition("armor_add_value_head", "minecraft:armor", "add_value", "head", tiers(3), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.armor_add_value_head"),
                new EnchantDefinition("armor_add_value_legs", "minecraft:armor", "add_value", "legs", tiers(6), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.armor_add_value_legs"),
                new EnchantDefinition("armor_toughness_add_value_chest", "minecraft:armor_toughness", "add_value", "chest", tiers(2, 3, 4), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.armor_toughness_add_value_chest"),
                new EnchantDefinition("armor_toughness_add_value_feet", "minecraft:armor_toughness", "add_value", "feet", tiers(2, 3, 4), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.armor_toughness_add_value_feet"),
                new EnchantDefinition("armor_toughness_add_value_head", "minecraft:armor_toughness", "add_value", "head", tiers(2, 3, 4), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.armor_toughness_add_value_head"),
                new EnchantDefinition("armor_toughness_add_value_legs", "minecraft:armor_toughness", "add_value", "legs", tiers(2, 3, 4), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.armor_toughness_add_value_legs"),
                new EnchantDefinition("attack_damage_add_multiplied_base_chest", "minecraft:attack_damage", "add_multiplied_base", "chest", tiers(0.025, -0.05), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_damage_add_multiplied_base_chest"),
                new EnchantDefinition("attack_damage_add_multiplied_base_feet", "minecraft:attack_damage", "add_multiplied_base", "feet", tiers(0.025, -0.05), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_damage_add_multiplied_base_feet"),
                new EnchantDefinition("attack_damage_add_multiplied_base_head", "minecraft:attack_damage", "add_multiplied_base", "head", tiers(0.025, -0.05), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_damage_add_multiplied_base_head"),
                new EnchantDefinition("attack_damage_add_multiplied_base_legs", "minecraft:attack_damage", "add_multiplied_base", "legs", tiers(0.025, -0.05), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_damage_add_multiplied_base_legs"),
                new EnchantDefinition("attack_damage_add_multiplied_total_hand", "minecraft:attack_damage", "add_multiplied_total", "hand", tiers(0.05), EnchantBackendType.EVENT_BACKEND, "enchants.entries.attack_damage_add_multiplied_total_hand"),
                new EnchantDefinition("attack_damage_add_value_mainhand", "minecraft:attack_damage", "add_value", "mainhand", tiers(0.5, 3, 4, 4.5, 5, 5.5, 6, 7, 7.5, 8, 9, 9.5, 14, -2.0), EnchantBackendType.EVENT_BACKEND, "enchants.entries.attack_damage_add_value_mainhand"),
                new EnchantDefinition("attack_damage_add_value_offhand", "minecraft:attack_damage", "add_value", "offhand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.attack_damage_add_value_offhand"),
                new EnchantDefinition("block_break_speed_add_multiplied_total_mainhand", "minecraft:block_break_speed", "add_multiplied_total", "mainhand", tiers(0.2), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.block_break_speed_add_multiplied_total_mainhand"),
                new EnchantDefinition("block_interaction_range_add_value_hand", "minecraft:block_interaction_range", "add_value", "hand", tiers(3), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.block_interaction_range_add_value_hand"),
                new EnchantDefinition("entity_interaction_range_add_value_mainhand", "minecraft:entity_interaction_range", "add_value", "mainhand", tiers(-0.3, 0.3, 0.5), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.entity_interaction_range_add_value_mainhand"),
                new EnchantDefinition("fall_damage_multiplier_add_multiplied_total_chest", "minecraft:fall_damage_multiplier", "add_multiplied_total", "chest", tiers(-0.25), EnchantBackendType.EVENT_BACKEND, "enchants.entries.fall_damage_multiplier_add_multiplied_total_chest"),
                new EnchantDefinition("gravity_add_multiplied_total_chest", "minecraft:gravity", "add_multiplied_total", "chest", tiers(-0.35, 0.65), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.gravity_add_multiplied_total_chest"),
                new EnchantDefinition("jump_strength_add_multiplied_base_legs", "minecraft:jump_strength", "add_multiplied_base", "legs", tiers(0.15), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.jump_strength_add_multiplied_base_legs"),
                new EnchantDefinition("knockback_resistance_add_value_chest", "minecraft:knockback_resistance", "add_value", "chest", tiers(0.08, 0.1, 0.2), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.knockback_resistance_add_value_chest"),
                new EnchantDefinition("knockback_resistance_add_value_feet", "minecraft:knockback_resistance", "add_value", "feet", tiers(0.08, 0.1, 0.2), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.knockback_resistance_add_value_feet"),
                new EnchantDefinition("knockback_resistance_add_value_head", "minecraft:knockback_resistance", "add_value", "head", tiers(0.08, 0.1, 0.2), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.knockback_resistance_add_value_head"),
                new EnchantDefinition("knockback_resistance_add_value_legs", "minecraft:knockback_resistance", "add_value", "legs", tiers(0.08, 0.1, 0.2), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.knockback_resistance_add_value_legs"),
                new EnchantDefinition("luck_add_multiplied_base_offhand", "minecraft:luck", "add_multiplied_base", "offhand", tiers(0.1), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.luck_add_multiplied_base_offhand"),
                new EnchantDefinition("luck_add_value_offhand", "minecraft:luck", "add_value", "offhand", tiers(1), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.luck_add_value_offhand"),
                new EnchantDefinition("movement_speed_add_multiplied_base_chest", "minecraft:movement_speed", "add_multiplied_base", "chest", tiers(-0.08, 0.1), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_base_chest"),
                new EnchantDefinition("movement_speed_add_multiplied_base_body", "minecraft:movement_speed", "add_multiplied_base", "body", tiers(0.3), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_base_body"),
                new EnchantDefinition("movement_speed_add_multiplied_base_feet", "minecraft:movement_speed", "add_multiplied_base", "feet", tiers(0.07, 0.1), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_base_feet"),
                new EnchantDefinition("movement_speed_add_multiplied_total_chest", "minecraft:movement_speed", "add_multiplied_total", "chest", tiers(-0.03), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_total_chest"),
                new EnchantDefinition("movement_speed_add_multiplied_total_feet", "minecraft:movement_speed", "add_multiplied_total", "feet", tiers(-0.03), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_total_feet"),
                new EnchantDefinition("movement_speed_add_multiplied_total_head", "minecraft:movement_speed", "add_multiplied_total", "head", tiers(-0.03), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_total_head"),
                new EnchantDefinition("movement_speed_add_multiplied_total_legs", "minecraft:movement_speed", "add_multiplied_total", "legs", tiers(-0.03), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_total_legs"),
                new EnchantDefinition("movement_speed_add_multiplied_total_mainhand", "minecraft:movement_speed", "add_multiplied_total", "mainhand", tiers(0), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_total_mainhand"),
                new EnchantDefinition("movement_speed_add_multiplied_total_offhand", "minecraft:movement_speed", "add_multiplied_total", "offhand", tiers(0.05), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.movement_speed_add_multiplied_total_offhand"),
                new EnchantDefinition("safe_fall_distance_add_value_chest", "minecraft:safe_fall_distance", "add_value", "chest", tiers(1, 4), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.safe_fall_distance_add_value_chest"),
                new EnchantDefinition("safe_fall_distance_add_value_head", "minecraft:safe_fall_distance", "add_value", "head", tiers(1), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.safe_fall_distance_add_value_head"),
                new EnchantDefinition("sneaking_speed_add_multiplied_base_legs", "minecraft:sneaking_speed", "add_multiplied_base", "legs", tiers(0.25), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.sneaking_speed_add_multiplied_base_legs"),
                new EnchantDefinition("step_height_add_value_feet", "minecraft:step_height", "add_value", "feet", tiers(0.5), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.step_height_add_value_feet"),
                new EnchantDefinition("sweeping_damage_ratio_add_value_legs", "minecraft:sweeping_damage_ratio", "add_value", "legs", tiers(0.15), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.sweeping_damage_ratio_add_value_legs"),
                new EnchantDefinition("sweeping_damage_ratio_add_value_mainhand", "minecraft:sweeping_damage_ratio", "add_value", "mainhand", tiers(0.5, 0.6), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.sweeping_damage_ratio_add_value_mainhand"),

                new EnchantDefinition("attack_speed_add_multiplied_base_chest", "minecraft:attack_speed", "add_multiplied_base", "chest", tiers(0.15), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_speed_add_multiplied_base_chest"),
                new EnchantDefinition("attack_speed_add_multiplied_total_mainhand", "minecraft:attack_speed", "add_multiplied_total", "mainhand", tiers(0.1), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_speed_add_multiplied_total_mainhand"),
                new EnchantDefinition("attack_speed_add_value_mainhand", "minecraft:attack_speed", "add_value", "mainhand", tiers(2133.0), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_speed_add_value_mainhand"),
                new EnchantDefinition("attack_speed_add_multiplied_total_chest", "minecraft:attack_speed", "add_multiplied_total", "chest", tiers(-0.03), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_speed_add_multiplied_total_chest"),
                new EnchantDefinition("attack_speed_add_multiplied_total_feet", "minecraft:attack_speed", "add_multiplied_total", "feet", tiers(-0.03), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_speed_add_multiplied_total_feet"),
                new EnchantDefinition("attack_speed_add_multiplied_total_head", "minecraft:attack_speed", "add_multiplied_total", "head", tiers(-0.03), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_speed_add_multiplied_total_head"),
                new EnchantDefinition("attack_speed_add_multiplied_total_legs", "minecraft:attack_speed", "add_multiplied_total", "legs", tiers(-0.03), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_speed_add_multiplied_total_legs"),
                new EnchantDefinition("attack_speed_bonus_mainhand", "minecraft:attack_speed", "add_value", "mainhand", tiers(0.4), EnchantBackendType.ATTRIBUTE_BACKEND, "enchants.entries.attack_speed_bonus_mainhand"),


                new EnchantDefinition("critical_strike", "", "", "mainhand", tiers(1, 2, 3), EnchantBackendType.EVENT_BACKEND, "enchants.entries.critical_strike"),
                new EnchantDefinition("levitation_shot", "", "", "mainhand", tiers(1, 2, 3, 4, 5), EnchantBackendType.EVENT_BACKEND, "enchants.entries.levitation_shot"),
                new EnchantDefinition("plated", "", "", "body", tiers(1, 2, 3, 4), EnchantBackendType.EVENT_BACKEND, "enchants.entries.plated"),
                new EnchantDefinition("soaring", "", "", "body", tiers(1, 2, 3, 4), EnchantBackendType.EVENT_BACKEND, "enchants.entries.soaring"),
                new EnchantDefinition("void_strike", "", "", "mainhand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.void_strike"),
                new EnchantDefinition("void_shot", "", "", "mainhand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.void_shot"),
                new EnchantDefinition("ambush", "", "", "mainhand", tiers(1, 2, 3), EnchantBackendType.EVENT_BACKEND, "enchants.entries.ambush"),
                new EnchantDefinition("dune_speed", "", "", "feet", tiers(1, 2, 3), EnchantBackendType.EVENT_BACKEND, "enchants.entries.dune_speed"),
                new EnchantDefinition("technical_daybroken", "", "", "mainhand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_daybroken"),
                new EnchantDefinition("technical_draconic", "", "", "mainhand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_draconic"),
                new EnchantDefinition("technical_infernal_infusion", "", "", "mainhand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_infernal_infusion"),
                new EnchantDefinition("technical_mighty_wind", "", "", "mainhand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_mighty_wind"),
                new EnchantDefinition("technical_soul_harvest", "", "", "mainhand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_soul_harvest"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_AMETHYST, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_amethyst"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_COPPER, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_copper"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_DIAMOND, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_diamond"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_EMERALD, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_emerald"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_GOLD, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_gold"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_IRON, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_iron"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_LAPIS, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_lapis"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_NETHERITE, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_netherite"),
                new EnchantDefinition(EnchantIds.TECHNICAL_VOID_PENDANT_QUARTZ, "", "", "hand", tiers(1), EnchantBackendType.EVENT_BACKEND, "enchants.entries.technical_void_pendant_quartz")
        );
    }

    private static Map<Integer, Double> tiers(final double... values) {
        final Map<Integer, Double> tiers = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            tiers.put(i + 1, values[i]);
        }
        return tiers;
    }
}
