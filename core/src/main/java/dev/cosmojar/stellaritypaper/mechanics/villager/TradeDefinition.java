package dev.cosmojar.stellaritypaper.mechanics.villager;

import org.bukkit.Material;
import org.bukkit.entity.Villager.Profession;

/**
 * Неизменяемое описание одного торгового предложения жителя Энда.
 * Содержит только данные; вся логика интерпретации — в {@link MerchantRecipeFactory}.
 *
 * <p>Поля с суффиксом {@code PdcItemId} ссылаются на ключи из
 * {@code ItemDefinitionRegistry.findByPdcItemId(...)} (например {@code "enderite_shard"}).
 * Ванильные предметы (coal, diamond, etc.) передаются через поле {@code Material},
 * а соответствующий {@code pdcItemId} в этом случае равен {@code null}.</p>
 */
public record TradeDefinition(
        Profession profession,
        int level,

        String buyAPdcItemId,
        Material buyAVanillaMat,
        int buyAMin,
        int buyAMax,

        String buyBPdcItemId,
        Material buyBVanillaMat,
        int buyBMin,
        int buyBMax,

        String sellPdcItemId,
        Material sellVanillaMat,
        int sellCount,

        int xp,
        int maxUses,
        float priceMultiplier,

        EnchantMode enchantMode,
        int enchantLevelMin,
        int enchantLevelMax,

        TrimMode trimMode
) {


    public enum EnchantMode {
        NONE,
        RANDOM_LEVEL
    }


    public enum TrimMode {
        NONE,
        RANDOM_EMERALD_SPIRE,
        RANDOM_EYE
    }
}
