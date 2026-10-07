package dev.cosmojar.stellaritypaper.mechanics.villager;

import dev.cosmojar.stellaritypaper.mechanics.villager.TradeDefinition.EnchantMode;
import dev.cosmojar.stellaritypaper.mechanics.villager.TradeDefinition.TrimMode;
import org.bukkit.Material;
import org.bukkit.entity.Villager.Profession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Реестр всех торговых предложений жителей Энда (Enlightened Rebalance).
 * <p>
 * Данные строго соответствуют CSV-таблице
 * {@code Enlightened Villager - Enlightened Rebalance.csv}.
 * Предметы с суффиксом PdcItemId ссылаются на {@code ItemDefinitionRegistry}.
 * </p>
 */
public final class EndVillagerTradeRegistry {

    private final List<TradeDefinition> trades;

    public EndVillagerTradeRegistry() {
        final List<TradeDefinition> list = new ArrayList<>();
        registerArmorer(list);
        registerButcher(list);
        registerCartographer(list);
        registerCleric(list);
        registerFarmer(list);
        registerFisherman(list);
        registerFletcher(list);
        registerLeatherworker(list);
        registerLibrarian(list);
        registerMason(list);
        registerShepherd(list);
        registerToolsmith(list);
        registerWeaponsmith(list);
        this.trades = Collections.unmodifiableList(list);
    }

    /** Возвращает все зарегистрированные торговые предложения (неизменяемый список). */
    public List<TradeDefinition> all() {
        return trades;
    }

    /** Возвращает предложения для конкретной профессии и уровня. */
    public List<TradeDefinition> getForLevel(final Profession profession, final int level) {
        final List<TradeDefinition> result = new ArrayList<>();
        for (final TradeDefinition def : trades) {
            if (def.profession() == profession && def.level() == level) {
                result.add(def);
            }
        }
        return result;
    }


    private static TradeDefinition def(Profession p, int lvl,
            String buyAPdc, int buyMin, int buyMax,
            String sellPdc, int sellCount,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                buyAPdc, null, buyMin, buyMax,
                null, null, 0, 0,
                sellPdc, null, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }

    private static TradeDefinition defVC(Profession p, int lvl,
            Material buyAMat, int buyMin, int buyMax,
            String sellPdc, int sellCount,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                null, buyAMat, buyMin, buyMax,
                null, null, 0, 0,
                sellPdc, null, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }

    private static TradeDefinition defCV(Profession p, int lvl,
            String buyAPdc, int buyMin, int buyMax,
            Material sellMat, int sellCount,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                buyAPdc, null, buyMin, buyMax,
                null, null, 0, 0,
                null, sellMat, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }

    private static TradeDefinition defVV(Profession p, int lvl,
            Material buyAMat, int buyMin, int buyMax,
            Material sellMat, int sellCount,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                null, buyAMat, buyMin, buyMax,
                null, null, 0, 0,
                null, sellMat, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }

    private static TradeDefinition defWithBuyBCC(Profession p, int lvl,
            String buyAPdc, int buyAMin, int buyAMax,
            String buyBPdc, int buyBMin, int buyBMax,
            String sellPdc, int sellCount,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                buyAPdc, null, buyAMin, buyAMax,
                buyBPdc, null, buyBMin, buyBMax,
                sellPdc, null, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }

    private static TradeDefinition defWithBuyBVC(Profession p, int lvl,
            Material buyAMat, int buyAMin, int buyAMax,
            String buyBPdc, int buyBMin, int buyBMax,
            String sellPdc, int sellCount,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                null, buyAMat, buyAMin, buyAMax,
                buyBPdc, null, buyBMin, buyBMax,
                sellPdc, null, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }

    private static TradeDefinition defWithBuyBVV(Profession p, int lvl,
            Material buyAMat, int buyAMin, int buyAMax,
            String buyBPdc, int buyBMin, int buyBMax,
            Material sellMat, int sellCount,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                null, buyAMat, buyAMin, buyAMax,
                buyBPdc, null, buyBMin, buyBMax,
                null, sellMat, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }

    private static TradeDefinition defEnchantV(Profession p, int lvl,
            String buyAPdc, int buyMin, int buyMax,
            Material sellMat, int sellCount,
            int xp, int maxUses, float pm,
            int enchLvlMin, int enchLvlMax) {
        return new TradeDefinition(p, lvl,
                buyAPdc, null, buyMin, buyMax,
                null, null, 0, 0,
                null, sellMat, sellCount,
                xp, maxUses, pm,
                EnchantMode.RANDOM_LEVEL, enchLvlMin, enchLvlMax, TrimMode.NONE);
    }

    private static TradeDefinition defEnchantBuyBV(Profession p, int lvl,
            Material buyAMat, int buyACount,
            String buyBPdc, int buyBMin, int buyBMax,
            Material sellMat, int sellCount,
            int xp, int maxUses, float pm,
            int enchLvlMin, int enchLvlMax) {
        return new TradeDefinition(p, lvl,
                null, buyAMat, buyACount, buyACount,
                buyBPdc, null, buyBMin, buyBMax,
                null, sellMat, sellCount,
                xp, maxUses, pm,
                EnchantMode.RANDOM_LEVEL, enchLvlMin, enchLvlMax, TrimMode.NONE);
    }

    private static TradeDefinition defArmorSpire(Profession p, int lvl,
            String buyAPdc, int buyMin, int buyMax,
            Material sellMat,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                buyAPdc, null, buyMin, buyMax,
                null, null, 0, 0,
                null, sellMat, 1,
                xp, maxUses, pm,
                EnchantMode.RANDOM_LEVEL, 15, 29, TrimMode.RANDOM_EMERALD_SPIRE);
    }

    private static TradeDefinition defArmorEye(Profession p, int lvl,
            String buyAPdc, int buyMin, int buyMax,
            Material sellMat,
            int xp, int maxUses, float pm,
            int enchLvlMin, int enchLvlMax) {
        return new TradeDefinition(p, lvl,
                buyAPdc, null, buyMin, buyMax,
                null, null, 0, 0,
                null, sellMat, 1,
                xp, maxUses, pm,
                EnchantMode.RANDOM_LEVEL, enchLvlMin, enchLvlMax, TrimMode.RANDOM_EYE);
    }

    private static void registerArmorer(final List<TradeDefinition> t) {
        final Profession P = Profession.ARMORER;

        t.add(defVC(P, 1, Material.COAL,     17, 20, "enderite_shard", 1, 2, 12, 0.05f));
        t.add(defVC(P, 1, Material.CHARCOAL,  20, 24, "enderite_shard", 1, 2, 10, 0.05f));
        t.add(defVC(P, 1, Material.BLAZE_ROD,  2,  4, "enderite_shard", 1, 1, 10, 0.05f));
        t.add(defArmorSpire(P, 1, "enderite_shard",  9, 13, Material.IRON_CHESTPLATE, 2, 6, 0.2f));
        t.add(defArmorSpire(P, 1, "enderite_shard",  7, 11, Material.IRON_HELMET,     2, 6, 0.2f));

        t.add(defArmorSpire(P, 2, "enderite_shard",  8, 12, Material.IRON_LEGGINGS,  8, 6, 0.2f));
        t.add(defArmorSpire(P, 2, "enderite_shard",  5,  8, Material.IRON_BOOTS,     8, 6, 0.2f));
        t.add(def(P, 2, "enderite_shard",  4,  6, "hallowed_ingot",  1, 10, 8, 0.2f));
        t.add(def(P, 2, "enderite_shard",  4,  6, "chorus_plating",  1, 10, 8, 0.2f));

        t.add(defVC(P, 3, Material.DIAMOND, 1, 1, "enderite_shard", 2, 12, 8, 0.05f));

        t.add(defWithBuyBVC(P, 3, Material.SHIELD, 1, 1, "enderite_shard", 45, 64, "copper_elektra_shield", 1, 40, 2, 0.2f));

        t.add(defWithBuyBVV(P, 4, Material.PURPUR_BLOCK, 20, 30, "enderite_shard", 28, 40, "enderite_smithing_template", 1, 10, 2, 0.2f));
        t.add(defArmorEye(P, 4, "enderite_shard", 28, 35, Material.DIAMOND_LEGGINGS, 15, 3, 0.2f, 15, 32));
        t.add(defArmorEye(P, 4, "enderite_shard", 21, 24, Material.DIAMOND_BOOTS,    15, 3, 0.2f, 15, 32));

        t.add(defArmorEye(P, 5, "enderite_shard", 23, 32, Material.DIAMOND_HELMET,      15, 3, 0.2f, 15, 32));
        t.add(defArmorEye(P, 5, "enderite_shard", 32, 41, Material.DIAMOND_CHESTPLATE,  15, 3, 0.2f, 15, 32));
    }

    private static void registerButcher(final List<TradeDefinition> t) {
        final Profession P = Profession.BUTCHER;

        t.add(defVC(P, 1, Material.ROTTEN_FLESH, 20, 30, "enderite_shard", 1, 1, 14, 0.05f));

        t.add(defVC(P, 1, Material.PHANTOM_MEMBRANE, 8, 16, "enderite_shard", 1, 1, 14, 0.05f));

        t.add(defVC(P, 2, Material.COAL,     17, 20, "enderite_shard", 1, 3, 12, 0.05f));
        t.add(defVC(P, 2, Material.CHARCOAL, 20, 24, "enderite_shard", 1, 3, 12, 0.05f));

        t.add(new TradeDefinition(P, 2,
                "enderman_flesh", null, 2, 2,
                "enderite_shard", null, 2, 2,
                "grilled_enderman_flesh", null, 3,
                5, 8, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(new TradeDefinition(P, 2,
                "enderman_flesh", null, 2, 2,
                "enderite_shard", null, 2, 2,
                "frozen_carpaccio", null, 3,
                5, 8, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));

        t.add(defVC(P, 3, Material.SHULKER_SHELL, 2, 2, "enderite_shard", 1, 12, 14, 0.05f));
        t.add(def(P, 3, "enderite_shard", 1, 1, "shulker_body", 1, 12, 14, 0.05f));
        t.add(defEnchantV(P, 3, "enderite_shard", 16, 24, Material.IRON_AXE, 1, 15, 3, 0.2f, 15, 25));

        t.add(new TradeDefinition(P, 4,
                null, Material.BEEF, 5, 10,
                null, Material.LEATHER, 5, 10,
                "enderite_shard", null, 1,
                20, 8, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(new TradeDefinition(P, 4,
                null, Material.MUTTON, 5, 10,
                null, Material.WHITE_WOOL, 5, 10,
                "enderite_shard", null, 1,
                20, 8, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(new TradeDefinition(P, 4,
                null, Material.CHICKEN, 5, 10,
                null, Material.FEATHER, 5, 10,
                "enderite_shard", null, 1,
                20, 8, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(defVC(P, 4, Material.PORKCHOP, 8, 15, "enderite_shard", 1, 20, 8, 0.05f));
        t.add(def(P, 4, "enderite_shard", 16, 23, "pho", 1, 25, 3, 0.05f));

        t.add(def(P, 5, "enderite_shard", 38, 50, "shepherds_pie", 1, 50, 2, 0.2f));
        t.add(defCV(P, 5, "enderite_shard", 3, 6, Material.DRIED_KELP_BLOCK, 1, 20, 12, 0.05f));
    }

    private static void registerCartographer(final List<TradeDefinition> t) {
        final Profession P = Profession.CARTOGRAPHER;

        t.add(defVC(P, 1, Material.PAPER, 24, 30, "enderite_shard", 1, 2, 12, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 6, 8, Material.MAP, 1, 1, 10, 0.05f));

        t.add(defVC(P, 2, Material.GLASS_PANE, 14, 20, "enderite_shard", 1, 8, 14, 0.05f));
        t.add(defWithBuyBVC(P, 2, Material.MAP, 1, 1, "enderite_shard", 40, 50, "MAP:end_city_explorer_map", 1, 40, 1, 0.2f));
        t.add(defCV(P, 3, "enderite_shard", 2, 2, Material.ITEM_FRAME, 4, 12, 8, 0.05f));
        t.add(defWithBuyBVC(P, 3, Material.MAP, 1, 1, "enderite_shard", 50, 60, "MAP:chapel_of_light_map", 1, 50, 1, 0.2f));
        t.add(defCV(P, 4, "enderite_shard", 3, 3, Material.GLOW_ITEM_FRAME, 2, 18, 8, 0.05f));
        t.add(def(P, 4, "enderite_shard", 4, 6, "phantom_item_frame", 3, 20, 4, 0.05f));
        t.add(defCV(P, 5, "enderite_shard", 15, 23, Material.MOJANG_BANNER_PATTERN, 1, 30, 8, 0.05f));
    }

    private static void registerCleric(final List<TradeDefinition> t) {
        final Profession P = Profession.CLERIC;
        t.add(defVC(P, 1, Material.PHANTOM_MEMBRANE, 12, 18, "enderite_shard", 1, 3,  8, 0.05f));
        t.add(defVC(P, 1, Material.BONE,              24, 33, "enderite_shard", 1, 2, 12, 0.05f));
        t.add(def (P, 1, "enderman_hand", 1, 1, "enderite_shard", 2, 3, 12, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 1, 1, Material.REDSTONE, 4, 1, 16, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 2, 2, Material.BLAZE_ROD, 1, 2,  8, 0.05f));
        t.add(defVC(P, 2, Material.GOLD_INGOT, 3, 3, "enderite_shard", 1, 5, 10, 0.05f));
        t.add(defCV(P, 2, "enderite_shard", 1, 1, Material.LAPIS_LAZULI, 3, 5, 10, 0.05f));
        t.add(defCV(P, 2, "enderite_shard", 1, 1, Material.NETHER_WART, 2,  3, 12, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 7, 11, Material.POTION, 1, 10, 3, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.GLOWSTONE, 1, 5, 12, 0.05f));
        t.add(new TradeDefinition(P, 4,
                null, Material.ENDER_PEARL, 16, 16,
                null, Material.ENDER_PEARL, 1, 16,
                "enderite_shard", null, 1,
                6, 12, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(defCV(P, 4, "enderite_shard", 7, 11, Material.POTION, 1, 15, 3, 0.2f));
        t.add(defCV(P, 5, "enderite_shard", 11, 18, Material.POTION, 1, 25, 2, 0.2f));
        t.add(defVC(P, 5, Material.DRAGON_BREATH, 3, 3, "enderite_shard", 1, 15, 10, 0.05f));
    }

    private static void registerFarmer(final List<TradeDefinition> t) {
        final Profession P = Profession.FARMER;
        t.add(defVC(P, 1, Material.WHEAT,    15, 30, "enderite_shard", 1, 2,  8, 0.05f));
        t.add(defVC(P, 1, Material.POTATO,   20, 35, "enderite_shard", 1, 3,  8, 0.05f));
        t.add(defVC(P, 1, Material.CARROT,   20, 35, "enderite_shard", 1, 3,  8, 0.05f));
        t.add(defVC(P, 1, Material.BEETROOT, 15, 25, "enderite_shard", 1, 2, 10, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 1, 1, Material.BREAD, 6, 2, 12, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 1, 1, Material.CAKE,  1, 2, 10, 0.05f));
        t.add(defVC(P, 2, Material.CHORUS_FRUIT,   20, 28, "enderite_shard", 1, 5,  6, 0.05f));
        t.add(defVC(P, 2, Material.CHORUS_FLOWER,   5,  7, "enderite_shard", 1, 8, 12, 0.05f));
        t.add(def (P, 2, "enderite_shard", 1, 1, "chorus_pie",  2, 15, 6, 0.2f));
        t.add(defCV(P, 2, "enderite_shard", 1, 1, Material.SHROOMLIGHT, 2, 4, 8, 0.05f));
        t.add(def(P, 3, "enderite_shard", 2, 2, "candied_chorus_fruit", 1, 20, 3, 0.2f));
        defWithBuyBVC(P, 3, Material.WHEAT_SEEDS, 12, 16, "enderite_shard", 1, 1, Material.TORCHFLOWER_SEEDS.name(), 4, 5, 12, 0.05f, t);
        defWithBuyBVC(P, 3, Material.WHEAT_SEEDS,  8, 11, "enderite_shard", 1, 1, Material.PITCHER_POD.name(), 3, 5, 8, 0.05f, t);
        t.add(def(P, 4, "enderite_shard", 3, 3, "chorus_juice",  1, 15, 8, 0.2f));
        t.add(def(P, 4, "enderite_shard", 4, 4, "chorus_stew",   1, 15, 6, 0.2f));
        t.add(def(P, 4, "enderite_shard", 5, 6, "fried_chorus_fruit", 2, 15, 8, 0.2f));
        t.add(def(P, 5, "enderite_shard", 12, 18, "pho", 1, 25, 3, 0.2f));
        t.add(new TradeDefinition(P, 5,
                null, Material.BREAD, 10, 10,
                "enderite_shard", null, 64, 64,
                "loaf_of_plenty", null, 1,
                50, 2, 0.2f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
    }

    private static void defWithBuyBVC(Profession p, int lvl,
            Material buyAMat, int buyAMin, int buyAMax,
            String buyBPdc, int buyBMin, int buyBMax,
            String sellMatName, int sellCount,
            int xp, int maxUses, float pm,
            List<TradeDefinition> t) {
        Material sellMat;
        try {
            sellMat = Material.valueOf(sellMatName);
        } catch (IllegalArgumentException e) {
            sellMat = Material.WHEAT_SEEDS;
        }
        t.add(new TradeDefinition(p, lvl,
                null, buyAMat, buyAMin, buyAMax,
                buyBPdc, null, buyBMin, buyBMax,
                null, sellMat, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE));
    }

    private static void registerFisherman(final List<TradeDefinition> t) {
        final Profession P = Profession.FISHERMAN;
        t.add(defVC(P, 1, Material.FISHING_ROD, 1, 1, "enderite_shard", 2, 10, 1, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 3, 3, Material.FISHING_ROD, 1, 4, 2, 0.05f));
        t.add(defVC(P, 1, Material.STRING, 20, 32, "enderite_shard", 1, 5, 6, 0.05f));
        t.add(defVC(P, 1, Material.COAL,   20, 26, "enderite_shard", 1, 5, 8, 0.05f));
        t.add(def(P, 2, "ender_koi",   4, 6, "enderite_shard", 1, 4, 8, 0.05f));
        t.add(def(P, 2, "overgrown_cod", 4, 6, "enderite_shard", 1, 4, 8, 0.05f));
        t.add(def(P, 2, "frost_minnow", 4, 6, "enderite_shard", 1, 4, 8, 0.05f));
        t.add(def(P, 2, "goosh",        4, 6, "enderite_shard", 1, 4, 8, 0.05f));
        t.add(defCV(P, 2, "enderite_shard", 1, 1, Material.CAMPFIRE, 1, 5, 8, 0.05f));
        t.add(defCV(P, 2, "enderite_shard", 2, 2, Material.SOUL_CAMPFIRE, 1, 8, 6, 0.05f));
        t.add(defEnchantV(P, 3, "enderite_shard", 5, 8, Material.FISHING_ROD, 1, 4, 2, 0.05f, 15, 29));
        t.add(def(P, 3, "crimson_tigerfish", 4, 6, "enderite_shard", 1, 4, 8, 0.05f));
        t.add(def(P, 3, "fleshy_piranha",    4, 6, "enderite_shard", 1, 4, 8, 0.05f));
        t.add(def(P, 3, "flarefin_koi",      4, 6, "enderite_shard", 1, 4, 8, 0.05f));
        t.add(def(P, 3, "potassifish",        4, 6, "enderite_shard", 1, 4, 8, 0.05f));
        t.add(defEnchantedBook(P, 4, "enderite_shard", 10, 14, org.bukkit.enchantments.Enchantment.LURE, 3, 10, 2, 0.2f));
        t.add(defEnchantedBook(P, 4, "enderite_shard", 10, 14, org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 3, 10, 2, 0.2f));
        t.add(def(P, 4, "enderite_shard", 14, 20, "fisher_of_voids",    1, 15, 6, 0.2f));
        t.add(def(P, 5, "crystal_heartfish", 1, 1, "enderite_shard", 5, 15, 3, 0.2f));
        t.add(def(P, 5, "enderite_shard", 14, 20, "prismatic_sushi", 1, 15, 4, 0.2f));
    }

    private static void registerFletcher(final List<TradeDefinition> t) {
        final Profession P = Profession.FLETCHER;
        t.add(defCV(P, 1, "enderite_shard", 2, 2, Material.SPECTRAL_ARROW, 10, 8, 12, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 1, 1, Material.ARROW, 16, 6, 8, 0.05f));
        t.add(defVC(P, 1, Material.STICK, 24, 36, "enderite_shard", 1, 2, 12, 0.05f));
        t.add(defVC(P, 2, Material.FLINT, 10, 20, "enderite_shard", 1, 2, 12, 0.05f));
        t.add(defEnchantV(P, 2, "enderite_shard", 8, 15, Material.BOW, 1, 10, 8, 0.05f, 15, 29));
        t.add(defVC(P, 3, Material.STRING,  12, 20, "enderite_shard", 1, 4, 12, 0.05f));
        t.add(defVC(P, 3, Material.FEATHER,  7, 15, "enderite_shard", 1, 6,  6, 0.05f));
        t.add(defEnchantV(P, 3, "enderite_shard", 10, 17, Material.CROSSBOW, 1, 12, 2, 0.2f, 15, 29));
        t.add(defWithBuyBVV(P, 4, Material.ARROW, 8, 8, "enderite_shard", 9, 17, Material.TIPPED_ARROW, 8, 8, 6, 0.05f));
        t.add(new TradeDefinition(P, 4,
                null, Material.ARROW, 8, 8,
                "enderite_shard", null, 64, 64,
                null, Material.TIPPED_ARROW, 8,
                8, 5, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(new TradeDefinition(P, 5,
                null, Material.BOW, 1, 1,
                "enderite_shard", null, 64, 64,
                "sharanga", null, 1,
                25, 2, 0.2f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
    }

    private static void registerLeatherworker(final List<TradeDefinition> t) {
        final Profession P = Profession.LEATHERWORKER;
        t.add(defVC(P, 1, Material.LEATHER, 8, 15, "enderite_shard", 1, 3, 8, 0.05f));
        t.add(defVC(P, 1, Material.FLINT,  10, 20, "enderite_shard", 1, 3, 6, 0.05f));
        t.add(defCV(P, 2, "enderite_shard", 5, 10, Material.SADDLE, 1, 5, 3, 0.05f));
        t.add(defCV(P, 2, "enderite_shard", 1, 1, Material.LEATHER, 3, 4, 6, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.CAULDRON, 1, 8, 6, 0.05f));
        t.add(defVC(P, 3, Material.RABBIT_HIDE, 4, 6, "enderite_shard", 1, 6, 8, 0.05f));
        t.add(new TradeDefinition(P, 4,
                null, Material.ITEM_FRAME, 2, 2,
                "enderite_shard", null, 1, 1,
                null, Material.GLOW_ITEM_FRAME, 2,
                8, 4, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(defCV(P, 4, "enderite_shard", 5, 10, Material.TURTLE_SCUTE, 1, 6, 8, 0.05f));
        t.add(defCV(P, 4, "enderite_shard", 7, 12, Material.ARMADILLO_SCUTE, 1, 6, 8, 0.05f));

        t.add(new TradeDefinition(P, 5,
                null, Material.DIAMOND_HORSE_ARMOR, 1, 1,
                "enderite_shard", null, 24, 32,
                "reinforced_horse_armor", null, 1,
                20, 2, 0.2f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
    }

    private static void registerLibrarian(final List<TradeDefinition> t) {
        final Profession P = Profession.LIBRARIAN;
        t.add(defVC(P, 1, Material.PAPER, 24, 36, "enderite_shard", 1, 2, 6, 0.05f));
        t.add(defVC(P, 1, Material.BOOK,   3,  3, "enderite_shard", 1, 3, 8, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 4, 6, Material.BOOKSHELF, 1, 4, 8, 0.05f));
        t.add(defEnchantBuyBV(P, 1, Material.BOOK, 1, "enderite_shard", 15, 24, Material.ENCHANTED_BOOK, 1, 8, 3, 0.2f, 15, 24));
        t.add(defCV(P, 2, "enderite_shard", 1, 1, Material.LANTERN, 3, 4, 8, 0.05f));
        t.add(defEnchantBuyBV(P, 2, Material.BOOK, 1, "enderite_shard", 25, 31, Material.ENCHANTED_BOOK, 1, 15, 3, 0.2f, 25, 31));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.INK_SAC, 2, 4, 8, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.GLOW_INK_SAC, 1, 5, 6, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.GLASS, 6, 3, 8, 0.05f));
        t.add(defEnchantBuyBV(P, 4, Material.BOOK, 1, "enderite_shard", 35, 43, Material.ENCHANTED_BOOK, 1, 20, 2, 0.2f, 35, 43));
        t.add(defCV(P, 4, "enderite_shard", 1, 1, Material.WRITABLE_BOOK, 1, 6, 6, 0.05f));
        t.add(defCV(P, 4, "enderite_shard", 7, 15, Material.NAME_TAG, 1, 6, 3, 0.05f));
        t.add(new TradeDefinition(P, 5,
                null, Material.BOOK, 1, 1,
                "enderite_shard", null, 64, 64,
                "book_of_updraft", null, 1,
                20, 1, 0.2f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(new TradeDefinition(P, 5,
                null, Material.BOOK, 1, 1,
                "enderite_shard", null, 64, 64,
                "book_of_light", null, 1,
                20, 1, 0.2f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(new TradeDefinition(P, 5,
                null, Material.BOOK, 1, 1,
                "enderite_shard", null, 64, 64,
                "book_of_obstruct", null, 1,
                20, 1, 0.2f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
    }

    private static void registerMason(final List<TradeDefinition> t) {
        final Profession P = Profession.MASON;
        t.add(defCV(P, 1, "enderite_shard", 1, 1, Material.PURPUR_BLOCK, 4, 2, 8, 0.05f));
        t.add(defCV(P, 1, "enderite_shard", 1, 1, Material.END_STONE_BRICKS, 4, 3, 8, 0.05f));
        t.add(def(P, 2, "amethyst_budfish", 1, 1, "amethyst_shard", 4, 4, 6, 0.05f));
        t.add(new TradeDefinition(P, 2,
                "amethyst_budfish", null, 1, 1,
                null, null, 0, 0,
                null, Material.AMETHYST_SHARD, 4,
                4, 6, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(new TradeDefinition(P, 2,
                "amethyst_budfish", null, 1, 1,
                null, null, 0, 0,
                null, Material.QUARTZ, 8,
                5, 6, 0.05f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
        t.add(defCV(P, 2, "enderite_shard", 2, 2, Material.OBSIDIAN, 3, 6, 8, 0.05f));
        t.add(defCV(P, 2, "enderite_shard", 2, 2, Material.SMOOTH_QUARTZ, 4, 6, 6, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.POLISHED_BLACKSTONE, 4, 6, 8, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.POLISHED_DEEPSLATE, 4, 6, 8, 0.05f));
        t.add(defVC(P, 3, Material.COBBLED_DEEPSLATE, 15, 25, "enderite_shard", 1, 6, 6, 0.05f));
        t.add(defVC(P, 3, Material.BLACKSTONE,        15, 25, "enderite_shard", 1, 6, 6, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.QUARTZ_BLOCK, 4, 8, 6, 0.05f));
        t.add(defCV(P, 3, "enderite_shard", 1, 1, Material.QUARTZ_PILLAR, 3, 8, 6, 0.05f));
        for (final Material mat : new Material[]{
                Material.MAGENTA_GLAZED_TERRACOTTA, Material.WHITE_GLAZED_TERRACOTTA,
                Material.PURPLE_GLAZED_TERRACOTTA, Material.BLACK_GLAZED_TERRACOTTA,
                Material.LIGHT_GRAY_GLAZED_TERRACOTTA, Material.PINK_GLAZED_TERRACOTTA,
                Material.YELLOW_GLAZED_TERRACOTTA, Material.RED_GLAZED_TERRACOTTA,
                Material.LIME_GLAZED_TERRACOTTA, Material.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Material.ORANGE_GLAZED_TERRACOTTA}) {
            t.add(defCV(P, 4, "enderite_shard", 1, 1, mat, 2, 12, 6, 0.05f));
        }
        for (final Material mat : new Material[]{
                Material.MAGENTA_TERRACOTTA, Material.WHITE_TERRACOTTA,
                Material.PURPLE_TERRACOTTA, Material.BLACK_TERRACOTTA,
                Material.LIGHT_GRAY_TERRACOTTA, Material.PINK_TERRACOTTA,
                Material.YELLOW_TERRACOTTA, Material.RED_TERRACOTTA,
                Material.LIME_TERRACOTTA, Material.LIGHT_BLUE_TERRACOTTA,
                Material.ORANGE_TERRACOTTA}) {
            t.add(defCV(P, 4, "enderite_shard", 1, 1, mat, 2, 12, 6, 0.05f));
        }
        t.add(defCV(P, 5, "enderite_shard", 4, 6, Material.CRYING_OBSIDIAN, 2, 16, 4, 0.05f));
        t.add(defCV(P, 5, "enderite_shard", 1, 1, Material.PURPUR_PILLAR, 4, 12, 6, 0.05f));
        t.add(defCV(P, 5, "enderite_shard", 1, 1, Material.CHISELED_QUARTZ_BLOCK, 2, 12, 6, 0.05f));
    }

    private static void registerShepherd(final List<TradeDefinition> t) {
        final Profession P = Profession.SHEPHERD;
        t.add(defCV(P, 1, "enderite_shard", 1, 1, Material.SHEARS, 1, 2, 4, 0.05f));
        t.add(defVC(P, 1, Material.WHITE_WOOL, 8, 16, "enderite_shard", 1, 3, 6, 0.05f));
        final Material[] dyes = {Material.RED_DYE, Material.ORANGE_DYE, Material.YELLOW_DYE,
                Material.LIME_DYE, Material.GREEN_DYE, Material.CYAN_DYE,
                Material.LIGHT_BLUE_DYE, Material.BLUE_DYE, Material.PURPLE_DYE,
                Material.MAGENTA_DYE, Material.WHITE_DYE, Material.LIGHT_GRAY_DYE,
                Material.GRAY_DYE, Material.BROWN_DYE, Material.BLACK_DYE, Material.PINK_DYE};
        for (final Material dye : dyes) {
            t.add(defCV(P, 2, "enderite_shard", 1, 1, dye, 3, 4, 8, 0.05f));
        }
        final Material[] wools = {Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
                Material.LIME_WOOL, Material.GREEN_WOOL, Material.CYAN_WOOL,
                Material.LIGHT_BLUE_WOOL, Material.BLUE_WOOL, Material.PURPLE_WOOL,
                Material.MAGENTA_WOOL, Material.WHITE_WOOL, Material.LIGHT_GRAY_WOOL,
                Material.GRAY_WOOL, Material.BROWN_WOOL, Material.BLACK_WOOL, Material.PINK_WOOL};
        for (final Material wool : wools) {
            t.add(defCV(P, 2, "enderite_shard", 1, 1, wool, 2, 4, 8, 0.05f));
        }
        final Material[] carpets = {Material.RED_CARPET, Material.ORANGE_CARPET, Material.YELLOW_CARPET,
                Material.LIME_CARPET, Material.GREEN_CARPET, Material.CYAN_CARPET,
                Material.LIGHT_BLUE_CARPET, Material.BLUE_CARPET, Material.PURPLE_CARPET,
                Material.MAGENTA_CARPET, Material.WHITE_CARPET, Material.LIGHT_GRAY_CARPET,
                Material.GRAY_CARPET, Material.BROWN_CARPET, Material.BLACK_CARPET, Material.PINK_CARPET};
        for (final Material carpet : carpets) {
            t.add(defCV(P, 3, "enderite_shard", 1, 1, carpet, 5, 6, 6, 0.05f));
        }
        for (final Material wool : wools) {
            t.add(defCV(P, 3, "enderite_shard", 1, 1, wool, 2, 6, 8, 0.05f));
        }
        final Material[] beds = {Material.RED_BED, Material.ORANGE_BED, Material.YELLOW_BED,
                Material.LIME_BED, Material.GREEN_BED, Material.CYAN_BED,
                Material.LIGHT_BLUE_BED, Material.BLUE_BED, Material.PURPLE_BED,
                Material.MAGENTA_BED, Material.WHITE_BED, Material.LIGHT_GRAY_BED,
                Material.GRAY_BED, Material.BROWN_BED, Material.BLACK_BED, Material.PINK_BED};
        for (final Material bed : beds) {
            t.add(defCV(P, 3, "enderite_shard", 1, 1, bed, 1, 10, 4, 0.05f));
        }
        final Material[] banners = {Material.RED_BANNER, Material.ORANGE_BANNER, Material.YELLOW_BANNER,
                Material.LIME_BANNER, Material.GREEN_BANNER, Material.CYAN_BANNER,
                Material.LIGHT_BLUE_BANNER, Material.BLUE_BANNER, Material.PURPLE_BANNER,
                Material.MAGENTA_BANNER, Material.WHITE_BANNER, Material.LIGHT_GRAY_BANNER,
                Material.GRAY_BANNER, Material.BROWN_BANNER, Material.BLACK_BANNER, Material.PINK_BANNER};
        for (final Material banner : banners) {
            t.add(defCV(P, 4, "enderite_shard", 1, 1, banner, 1, 8, 6, 0.05f));
        }
        for (final Material wool : wools) {
            t.add(defCV(P, 4, "enderite_shard", 1, 1, wool, 2, 6, 6, 0.05f));
        }
        for (final Material dye : dyes) {
            t.add(defCV(P, 4, "enderite_shard", 1, 1, dye, 3, 6, 6, 0.05f));
        }

        final String[] paintings = {
                "a_hop_and_a_skip_away", "dragonblade", "end",
                "end_blossom", "hourglass", "majestical_brew",
                "scheme", "shepherds_feast", "snare",
                "snatch", "the_obsidian_reliquary"};
        for (final String painting : paintings) {
            t.add(def(P, 5, "enderite_shard", 4, 6, "PAINTING:" + painting, 1, 20, 2, 0.2f));
        }
    }


    private static void registerToolsmith(final List<TradeDefinition> t) {
        final Profession P = Profession.TOOLSMITH;
        t.add(defVC(P, 1, Material.COAL,     17, 20, "enderite_shard", 1, 2, 12, 0.05f));
        t.add(defVC(P, 1, Material.CHARCOAL, 20, 24, "enderite_shard", 1, 3, 12, 0.05f));
        t.add(defVC(P, 1, Material.BLAZE_ROD, 2,  4, "enderite_shard", 1, 4,  6, 0.05f));
        t.add(defEnchantV(P, 1, "enderite_shard", 4, 7, Material.IRON_HOE,    1, 8, 3, 0.05f, 15, 29));
        t.add(defEnchantV(P, 1, "enderite_shard", 4, 7, Material.IRON_SHOVEL, 1, 8, 3, 0.05f, 15, 29));
        t.add(defEnchantV(P, 2, "enderite_shard", 6, 10, Material.IRON_AXE,     1, 10, 3, 0.05f, 15, 29));
        t.add(defEnchantV(P, 2, "enderite_shard", 6, 10, Material.IRON_PICKAXE, 1, 10, 3, 0.05f, 15, 29));
        t.add(def(P, 2, "enderite_shard", 3, 3, "hallowed_ingot",  1, 10, 6, 0.2f));
        t.add(def(P, 2, "enderite_shard", 3, 3, "chorus_plating",  1, 10, 8, 0.2f));
        t.add(defVC(P, 3, Material.DIAMOND, 1, 1, "enderite_shard", 2, 6, 12, 0.05f));
        t.add(defEnchantBuyBV(P, 3, Material.BOOK, 1, "enderite_shard", 18, 22, Material.ENCHANTED_BOOK, 1, 12, 4, 0.05f, 18, 22));
        t.add(defWithBuyBVV(P, 4, Material.PURPUR_BLOCK, 20, 30, "enderite_shard", 28, 40, "enderite_smithing_template", 1, 10, 2, 0.2f));
        t.add(defEnchantV(P, 4, "enderite_shard", 11, 18, Material.DIAMOND_HOE,    1, 20, 2, 0.2f, 21, 35));
        t.add(defEnchantV(P, 4, "enderite_shard", 13, 20, Material.DIAMOND_SHOVEL, 1, 20, 2, 0.2f, 21, 35));
        t.add(defEnchantV(P, 5, "enderite_shard", 15, 22, Material.DIAMOND_AXE,     1, 25, 2, 0.2f, 21, 35));
        t.add(defEnchantV(P, 5, "enderite_shard", 15, 22, Material.DIAMOND_PICKAXE, 1, 25, 2, 0.2f, 21, 35));
    }

    private static void registerWeaponsmith(final List<TradeDefinition> t) {
        final Profession P = Profession.WEAPONSMITH;
        t.add(defVC(P, 1, Material.COAL,     17, 20, "enderite_shard", 1, 2, 12, 0.05f));
        t.add(defVC(P, 1, Material.CHARCOAL, 20, 24, "enderite_shard", 1, 3, 12, 0.05f));
        t.add(defVC(P, 1, Material.BLAZE_ROD, 2,  4, "enderite_shard", 1, 4,  6, 0.05f));
        t.add(defEnchantV(P, 1, "enderite_shard", 10, 17, Material.IRON_SWORD, 1, 8, 3, 0.05f, 15, 29));
        t.add(defEnchantV(P, 2, "enderite_shard", 6, 10, Material.IRON_AXE, 1, 8, 3, 0.05f, 15, 29));
        t.add(def(P, 2, "enderite_shard", 3, 3, "hallowed_ingot", 1, 10, 6, 0.2f));
        t.add(def(P, 2, "enderite_shard", 3, 3, "chorus_plating", 1, 10, 8, 0.2f));
        t.add(defVC(P, 3, Material.DIAMOND, 1, 1, "enderite_shard", 2, 6, 12, 0.05f));
        t.add(defEnchantBuyBV(P, 3, Material.BOOK, 1, "enderite_shard", 20, 20, Material.ENCHANTED_BOOK, 1, 12, 4, 0.05f, 20, 20));
        t.add(defWithBuyBVV(P, 4, Material.PURPUR_BLOCK, 20, 30, "enderite_shard", 28, 40, "enderite_smithing_template", 1, 10, 2, 0.2f));
        t.add(defEnchantV(P, 4, "enderite_shard", 20, 25, Material.DIAMOND_SWORD, 1, 20, 2, 0.2f, 21, 35));
        t.add(defEnchantV(P, 5, "enderite_shard", 20, 25, Material.DIAMOND_AXE, 1, 20, 2, 0.2f, 21, 35));
        t.add(new TradeDefinition(P, 5,
                null, Material.GOLDEN_SWORD, 1, 1,
                "enderite_shard", null, 64, 64,
                "stellar_striker", null, 1,
                25, 1, 0.2f, EnchantMode.NONE, 0, 0, TrimMode.NONE));
    }

    private static void defWithBuyBVV(Profession p, int lvl,
            Material buyAMat, int buyAMin, int buyAMax,
            String buyBPdc, int buyBMin, int buyBMax,
            String sellPdc, int sellCount,
            int xp, int maxUses, float pm,
            List<TradeDefinition> t) {
        t.add(new TradeDefinition(p, lvl,
                null, buyAMat, buyAMin, buyAMax,
                buyBPdc, null, buyBMin, buyBMax,
                sellPdc, null, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE));
    }

    private static TradeDefinition defWithBuyBVV(Profession p, int lvl,
            Material buyAMat, int buyAMin, int buyAMax,
            String buyBPdc, int buyBMin, int buyBMax,
            String sellPdc, int sellCount,
            int xp, int maxUses, float pm) {
        return new TradeDefinition(p, lvl,
                null, buyAMat, buyAMin, buyAMax,
                buyBPdc, null, buyBMin, buyBMax,
                sellPdc, null, sellCount,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }


    private static TradeDefinition defEnchantedBook(Profession p, int lvl,
            String buyAPdc, int buyAMin, int buyAMax,
            org.bukkit.enchantments.Enchantment enchantment, int enchantLevel,
            int xp, int maxUses, float pm) {
        final String enchKey = enchantment.getKey().getKey();
        final String sellMarker = "ENCHANTED_BOOK:" + enchKey + ":" + enchantLevel;
        return new TradeDefinition(p, lvl,
                buyAPdc, null, buyAMin, buyAMax,
                null, null, 0, 0,
                sellMarker, null, 1,
                xp, maxUses, pm,
                EnchantMode.NONE, 0, 0, TrimMode.NONE);
    }
}
