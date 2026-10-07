package dev.cosmojar.stellaritypaper.mobs.variants;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

import java.util.Optional;

public enum MobVariant {
    VOIDED_ZOMBIE("voided_zombie", "stellarity.voided_zombie", EntityType.ZOMBIE),
    VOIDED_SKELETON("voided_skeleton", "stellarity.voided_skeleton", EntityType.SKELETON),
    VOIDED_SLIME("voided_slime", "stellarity.voided_slime", EntityType.SLIME),
    FLESH_PIGLIN("flesh_piglin", "stellarity.flesh_piglin", EntityType.PIGLIN),
    VOIDED_SILVERFISH("voided_silverfish", "stellarity.voided_silverfish", EntityType.SILVERFISH),
    ENDER_CHICKEN("ender_chicken", "stellarity.ender_chicken", EntityType.CHICKEN),
    ENDER_COW("ender_cow", "stellarity.ender_cow", EntityType.COW),
    ENDER_PIG("ender_pig", "stellarity.ender_pig", EntityType.PIG),
    ENDER_CAT("ender_cat", "stellarity.ender_cat", EntityType.CAT),
    ENDER_WOLF("ender_wolf", "stellarity.ender_wolf", EntityType.WOLF);

    public static final NamespacedKey MOB_TYPE_KEY = new NamespacedKey("stellarity", "mob_type");

    private final String pdcId;
    private final String scoreboardTag;
    private final EntityType targetType;

    MobVariant(final String pdcId, final String scoreboardTag, final EntityType targetType) {
        this.pdcId = pdcId;
        this.scoreboardTag = scoreboardTag;
        this.targetType = targetType;
    }

    public String getPdcId() {
        return pdcId;
    }

    public String getScoreboardTag() {
        return scoreboardTag;
    }

    public EntityType getTargetType() {
        return targetType;
    }

    public static Optional<MobVariant> findByPdcId(final String pdcId) {
        if (pdcId == null || pdcId.isBlank()) {
            return Optional.empty();
        }
        for (final MobVariant v : values()) {
            if (v.pdcId.equalsIgnoreCase(pdcId)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }
}
