package dev.cosmojar.stellaritypaper.mechanics.villager;

import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.mechanics.loot.ExplorerMapService;
import dev.cosmojar.stellaritypaper.mechanics.villager.TradeDefinition.EnchantMode;
import dev.cosmojar.stellaritypaper.mechanics.villager.TradeDefinition.TrimMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Единственное место, где {@link TradeDefinition} преобразуется в {@link MerchantRecipe}.
 * <p>
 * Здесь происходит вся логика:
 * <ul>
 *   <li>Рандомизация количества предметов оплаты</li>
 *   <li>Случайное зачарование ({@link EnchantMode#RANDOM_LEVEL})</li>
 *   <li>Случайная отделка брони ({@link TrimMode})</li>
 *   <li>Обработка спецмаркеров в sellPdcItemId:
 *     <ul>
 *       <li>{@code "MAP:<map_key>"} — карта-исследователь через {@link ExplorerMapService}</li>
 *       <li>{@code "PAINTING:<variant_id>"} — ванильная картина (вариант задаётся через GUI при размещении)</li>
 *       <li>{@code "ENCHANTED_BOOK:<enchant_key>:<level>"} — зачарованная книга</li>
 *     </ul>
 *   </li>
 *   <li>Fail-safe: если кастомный предмет не найден — логирует предупреждение и возвращает {@code null}</li>
 * </ul>
 */
public final class MerchantRecipeFactory {

    private static final Random RANDOM = new Random();

    private final ItemDefinitionRegistry itemRegistry;
    private final CustomItemFactory itemFactory;
    private final ExplorerMapService explorerMapService;
    private final Logger logger;

    public MerchantRecipeFactory(
            final ItemDefinitionRegistry itemRegistry,
            final CustomItemFactory itemFactory,
            final ExplorerMapService explorerMapService,
            final Logger logger
    ) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.explorerMapService = explorerMapService;
        this.logger = logger;
    }

    /**
     * Создаёт экземпляр {@link MerchantRecipe} из {@link TradeDefinition}.
     *
     * @param def описание торговли
     * @return готовый рецепт, или {@code null} если предмет не найден (ошибка логируется)
     */
    public MerchantRecipe create(final TradeDefinition def) {
        final ItemStack sellItem = buildItem(def.sellPdcItemId(), def.sellVanillaMat(), def.sellCount(), "sell");
        if (sellItem == null) {
            logger.warning("[EndVillagerTrades] Не удалось создать предмет продажи для "
                    + def.profession() + " lv" + def.level()
                    + " (pdcItemId=" + def.sellPdcItemId() + ", mat=" + def.sellVanillaMat() + ")");
            return null;
        }

        applyEnchant(sellItem, def);

        applyTrim(sellItem, def.trimMode());

        final MerchantRecipe recipe = new MerchantRecipe(
                sellItem,
                0,
                def.maxUses(),
                true,
                def.xp(),
                def.priceMultiplier()
        );

        final int buyACount = randomBetween(def.buyAMin(), def.buyAMax());
        final ItemStack buyAItem = buildItem(def.buyAPdcItemId(), def.buyAVanillaMat(), buyACount, "buyA");
        if (buyAItem == null) {
            logger.warning("[EndVillagerTrades] Не удалось создать Buy A для "
                    + def.profession() + " lv" + def.level()
                    + " (pdcItemId=" + def.buyAPdcItemId() + ", mat=" + def.buyAVanillaMat() + ")");
            return null;
        }
        recipe.addIngredient(buyAItem);

        final boolean hasBuyB = def.buyBPdcItemId() != null || def.buyBVanillaMat() != null;
        if (hasBuyB) {
            final int buyBCount = randomBetween(def.buyBMin(), def.buyBMax());
            final ItemStack buyBItem = buildItem(def.buyBPdcItemId(), def.buyBVanillaMat(), buyBCount, "buyB");
            if (buyBItem == null) {
                logger.warning("[EndVillagerTrades] Не удалось создать Buy B для "
                        + def.profession() + " lv" + def.level()
                        + " (pdcItemId=" + def.buyBPdcItemId() + ", mat=" + def.buyBVanillaMat() + ")");
                return null;
            }
            recipe.addIngredient(buyBItem);
        }

        return recipe;
    }

    /**
     * Создаёт {@link ItemStack} по одному из нескольких режимов:
     * <ol>
     *   <li>Если {@code pdcItemId} начинается с {@code "MAP:"} — создаёт карту-исследователь</li>
     *   <li>Если {@code pdcItemId} начинается с {@code "PAINTING:"} — создаёт ванильную картину.
     *       Конкретный вариант выбирается игроком при размещении через GUI ({@code PaintingService}).</li>
     *   <li>Если {@code pdcItemId} начинается с {@code "ENCHANTED_BOOK:"} — создаёт зачарованную книгу</li>
     *   <li>Если {@code pdcItemId} задан без префикса — ищет в {@link ItemDefinitionRegistry}</li>
     *   <li>Если {@code vanillaMat} задан — создаёт ванильный {@link ItemStack}</li>
     * </ol>
     *
     * @param pdcItemId  идентификатор кастомного предмета (возможно с маркером), или {@code null}
     * @param vanillaMat ванильный материал, или {@code null}
     * @param count      количество
     * @param slotName   имя слота для логов
     * @return ItemStack, или {@code null} при ошибке
     */
    private ItemStack buildItem(final String pdcItemId, final Material vanillaMat, final int count, final String slotName) {
        if (pdcItemId != null) {
            if (pdcItemId.startsWith("MAP:")) {
                return buildMapItem(pdcItemId.substring(4), count);
            }
            if (pdcItemId.startsWith("PAINTING:")) {
                return new ItemStack(Material.PAINTING, count);
            }
            if (pdcItemId.startsWith("ENCHANTED_BOOK:")) {
                return buildEnchantedBookItem(pdcItemId.substring(15), count);
            }
            final var defOpt = itemRegistry.findByPdcItemId(pdcItemId);
            if (defOpt.isEmpty()) {
                return null;
            }
            final ItemStack item = itemFactory.create(defOpt.get());
            item.setAmount(count);
            return item;
        }
        if (vanillaMat != null) {
            return new ItemStack(vanillaMat, count);
        }
        return null;
    }

    /**
     * Создаёт карту-исследователь через {@link ExplorerMapService}.
     * Карта создаётся без привязки к конкретному миру/локации (world=null, origin=null):
     * ExplorerMapService сам обработает это и создаст базовую карту без маркера структуры.
     */
    private ItemStack buildMapItem(final String mapKey, final int count) {
        if (explorerMapService == null) {
            logger.warning("[EndVillagerTrades] ExplorerMapService недоступен для MAP:" + mapKey);
            return null;
        }
        final Optional<ItemStack> mapOpt = explorerMapService.createExplorerMap(mapKey, null, null);
        if (mapOpt.isEmpty()) {
            logger.warning("[EndVillagerTrades] ExplorerMapService не смог создать карту: " + mapKey);
            return null;
        }
        final ItemStack map = mapOpt.get();
        map.setAmount(count);
        return map;
    }

    /**
     * Создаёт зачарованную книгу ({@link Material#ENCHANTED_BOOK}) с конкретным зачарованием.
     * Формат аргумента: {@code "<enchant_key>:<level>"}, например {@code "lure:3"}.
     */
    private ItemStack buildEnchantedBookItem(final String enchantSpec, final int count) {
        final String[] parts = enchantSpec.split(":", 2);
        if (parts.length != 2) {
            logger.warning("[EndVillagerTrades] Неверный формат ENCHANTED_BOOK: '" + enchantSpec + "' (ожидается '<key>:<level>')");
            return null;
        }
        final String enchantKey = parts[0];
        final int level;
        try {
            level = Integer.parseInt(parts[1]);
        } catch (final NumberFormatException e) {
            logger.warning("[EndVillagerTrades] Неверный уровень зачарования в ENCHANTED_BOOK: '" + parts[1] + "'");
            return null;
        }

        final NamespacedKey key = NamespacedKey.minecraft(enchantKey);
        final Enchantment enchantment = Registry.ENCHANTMENT.get(key);
        if (enchantment == null) {
            logger.warning("[EndVillagerTrades] Зачарование не найдено: minecraft:" + enchantKey);
            return null;
        }

        final ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, count);
        final EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return book;
    }

    /**
     * Применяет случайное зачарование к предмету в соответствии с {@link EnchantMode}.
     */
    private void applyEnchant(final ItemStack item, final TradeDefinition def) {
        if (def.enchantMode() == EnchantMode.NONE) {
            return;
        }
        if (def.enchantMode() == EnchantMode.RANDOM_LEVEL) {
            final int level = randomBetween(def.enchantLevelMin(), def.enchantLevelMax());
            try {
                item.enchantWithLevels(level, true, RANDOM);
            } catch (final Exception e) {
                logger.fine("[EndVillagerTrades] enchantWithLevels недоступен: " + e.getMessage());
            }
        }
    }

    /**
     * Применяет случайную отделку брони в соответствии с {@link TrimMode}.
     * Если предмет не является бронёй (нет {@link ArmorMeta}) — пропускается без ошибки.
     */
    private void applyTrim(final ItemStack item, final TrimMode trimMode) {
        if (trimMode == TrimMode.NONE) {
            return;
        }
        if (!(item.getItemMeta() instanceof final ArmorMeta armorMeta)) {
            return;
        }

        final TrimPattern pattern;
        final TrimMaterial material;

        switch (trimMode) {
            case RANDOM_EMERALD_SPIRE -> {
                if (RANDOM.nextBoolean()) {
                    return;
                }
                pattern = Registry.TRIM_PATTERN.get(NamespacedKey.fromString("stellarity:emerald_spire"));
                material = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft("emerald"));
            }
            case RANDOM_EYE -> {
                final String[] materials = {"emerald", "amethyst", "enderite", "chorus"};
                final String chosenMat = materials[RANDOM.nextInt(materials.length)];
                if (chosenMat.equals("enderite") || chosenMat.equals("chorus")) {
                    material = Registry.TRIM_MATERIAL.get(NamespacedKey.fromString("stellarity:" + chosenMat));
                } else {
                    material = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(chosenMat));
                }
                pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft("eye"));
            }
            default -> {
                return;
            }
        }

        if (pattern == null || material == null) {
            logger.fine("[EndVillagerTrades] TrimPattern/TrimMaterial не найден для режима " + trimMode);
            return;
        }

        armorMeta.setTrim(new ArmorTrim(material, pattern));
        item.setItemMeta(armorMeta);
    }

    /** Возвращает случайное целое число в диапазоне [min, max] включительно. */
    private int randomBetween(final int min, final int max) {
        if (min >= max) {
            return min;
        }
        return min + RANDOM.nextInt(max - min + 1);
    }
}
