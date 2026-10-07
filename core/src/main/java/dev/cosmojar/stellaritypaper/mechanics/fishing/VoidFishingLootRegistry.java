package dev.cosmojar.stellaritypaper.mechanics.fishing;

import org.bukkit.Material;

import java.util.List;

public final class VoidFishingLootRegistry {

    public record LootEntry(
            String id,
            boolean isCustom,
            int weight,
            int minCount,
            int maxCount,
            boolean isBook,
            boolean isFirework
    ) {
        public static LootEntry custom(String id, int weight) {
            return new LootEntry(id, true, weight, 1, 1, false, false);
        }

        public static LootEntry custom(String id, int weight, int minCount, int maxCount) {
            return new LootEntry(id, true, weight, minCount, maxCount, false, false);
        }

        public static LootEntry vanilla(Material material, int weight) {
            return new LootEntry(material.name(), false, weight, 1, 1, false, false);
        }

        public static LootEntry vanilla(Material material, int weight, int minCount, int maxCount) {
            return new LootEntry(material.name(), false, weight, minCount, maxCount, false, false);
        }

        public static LootEntry book(int weight, int minLevel, int maxLevel) {
            return new LootEntry("ENCHANTED_BOOK", false, weight, minLevel, maxLevel, true, false);
        }

        public static LootEntry firework(int weight) {
            return new LootEntry("FIREWORK_STAR", false, weight, 1, 1, false, true);
        }
    }

    public static final List<LootEntry> JUNK = List.of(
            LootEntry.vanilla(Material.CRYING_OBSIDIAN, 1, 1, 3),
            LootEntry.vanilla(Material.OBSIDIAN, 1, 1, 4),
            LootEntry.vanilla(Material.END_STONE, 1, 3, 9),
            LootEntry.vanilla(Material.CHORUS_FRUIT, 1, 1, 5),
            LootEntry.vanilla(Material.POPPED_CHORUS_FRUIT, 2, 2, 5),
            LootEntry.vanilla(Material.PURPUR_BLOCK, 1, 6, 12),
            LootEntry.vanilla(Material.PHANTOM_MEMBRANE, 3, 1, 3),
            LootEntry.vanilla(Material.ENDER_PEARL, 3, 1, 2),
            LootEntry.vanilla(Material.SHULKER_SHELL, 5, 1, 2),
            LootEntry.firework(1),
            LootEntry.firework(1),
            LootEntry.firework(1),
            LootEntry.vanilla(Material.PAPER, 3, 1, 5),
            LootEntry.vanilla(Material.END_ROD, 3, 1, 3),
            LootEntry.vanilla(Material.MAGENTA_DYE, 1),
            LootEntry.vanilla(Material.PURPLE_DYE, 1),
            LootEntry.vanilla(Material.BLACK_DYE, 1)
    );

    public static final List<LootEntry> TREASURE = List.of(
            LootEntry.vanilla(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, 7, 1, 2),
            LootEntry.vanilla(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 7, 1, 2),
            LootEntry.vanilla(Material.END_CRYSTAL, 5, 1, 2),
            LootEntry.vanilla(Material.END_CRYSTAL, 3, 2, 3),
            LootEntry.custom("enderite_smithing_template", 11),
            LootEntry.custom("winged_key", 11),
            LootEntry.book(2, 30, 40),
            LootEntry.book(5, 17, 29)
    );

    public static final List<LootEntry> AMETHYST_FOREST = List.of(
            LootEntry.custom("amethyst_budfish", 16),
            LootEntry.vanilla(Material.AMETHYST_SHARD, 5, 2, 4),
            LootEntry.vanilla(Material.QUARTZ, 5, 3, 6),
            LootEntry.vanilla(Material.AMETHYST_BLOCK, 3, 2, 3),
            LootEntry.vanilla(Material.BUDDING_AMETHYST, 1)
    );

    public static final List<LootEntry> FIERY_HILLS = List.of(
            LootEntry.custom("flarefin_koi", 20),
            LootEntry.vanilla(Material.BLAZE_ROD, 3, 1, 2),
            LootEntry.vanilla(Material.MAGMA_BLOCK, 6, 2, 4),
            LootEntry.vanilla(Material.GLOWSTONE_DUST, 7, 3, 7),
            LootEntry.vanilla(Material.GHAST_TEAR, 1)
    );

    public static final List<LootEntry> FLESH_TUNDRA = List.of(
            LootEntry.custom("crimson_tigerfish", 27),
            LootEntry.custom("fleshy_piranha", 19),
            LootEntry.vanilla(Material.ROTTEN_FLESH, 16, 3, 6),
            LootEntry.vanilla(Material.NETHER_WART_BLOCK, 12, 2, 3),
            LootEntry.vanilla(Material.GUNPOWDER, 12, 1, 5),
            LootEntry.vanilla(Material.BONE, 14, 1, 4)
    );

    public static final List<LootEntry> FROZEN_SPIKES = List.of(
            LootEntry.custom("frost_minnow", 7),
            LootEntry.vanilla(Material.ICE, 1, 2, 4),
            LootEntry.vanilla(Material.PACKED_ICE, 1, 2, 4),
            LootEntry.vanilla(Material.BLUE_ICE, 1, 2, 3),
            LootEntry.vanilla(Material.SNOW_BLOCK, 1, 2, 4),
            LootEntry.vanilla(Material.SNOWBALL, 1, 3, 8)
    );

    public static final List<LootEntry> PRISMARINE_FOREST = List.of(
            LootEntry.custom("bubblefish", 12),
            LootEntry.vanilla(Material.PRISMARINE, 6, 3, 5),
            LootEntry.vanilla(Material.PRISMARINE_CRYSTALS, 5, 2, 6),
            LootEntry.vanilla(Material.GLOW_INK_SAC, 5, 1, 2)
    );

    public static final List<LootEntry> THE_HALLOW = List.of(
            LootEntry.custom("prismite", 25),
            LootEntry.custom("starlight_soot", 9, 2, 4),
            LootEntry.vanilla(Material.CHERRY_LEAVES, 6, 2, 5),
            LootEntry.vanilla(Material.PINK_PETALS, 6, 3, 6)
    );

    public static final List<LootEntry> ENDLESS_DUNES = List.of(
            LootEntry.custom("potassifish", 4)
    );

    public static final List<LootEntry> WARPED_MARSH = List.of(
            LootEntry.custom("goosh", 10, 6, 8)
    );

    public static final List<LootEntry> ORIGINAL_BIOMES = List.of(
            LootEntry.custom("overgrown_cod", 8),
            LootEntry.vanilla(Material.CHORUS_FLOWER, 3, 1, 2),
            LootEntry.vanilla(Material.ENDER_EYE, 2, 1, 2),
            LootEntry.vanilla(Material.CHORUS_PLANT, 4, 3, 8)
    );

    public static final List<LootEntry> GLOBAL_FISH = List.of(
            LootEntry.custom("ender_koi", 15),
            LootEntry.custom("goosh", 5, 6, 8),
            LootEntry.custom("crystal_heartfish", 4)
    );
}
