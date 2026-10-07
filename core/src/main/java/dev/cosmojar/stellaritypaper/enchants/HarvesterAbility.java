package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.entity.EntityType;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Реестр разблокированных способностей Harvester.
 */
public enum HarvesterAbility {
    ANIMA_CONDUIT("anima_conduit", "item.stellarity.harvester.ability.anima_conduit", 1, EnumSet.of(EntityType.WITHER)),
    ARCTIC_WIND("arctic_wind", "item.stellarity.harvester.ability.arctic_wind", 1, EnumSet.of(EntityType.BREEZE)),
    CRYONICS("cryonics", "item.stellarity.harvester.ability.cryonics", 1, EnumSet.of(EntityType.EVOKER, EntityType.ENDER_DRAGON)),
    ECHO("echo", "item.stellarity.harvester.ability.echo", 1, EnumSet.of(EntityType.WARDEN)),
    EMBRITTLEMENT("embrittlement", "item.stellarity.harvester.ability.ebrittlement", 1, EnumSet.of(EntityType.WITHER_SKELETON)),
    FROSTBURN("frostburn", "item.stellarity.harvester.ability.frostburn", 5, EnumSet.of(EntityType.BLAZE, EntityType.MAGMA_CUBE)),
    FROST_BARRIER("frost_barrier", "item.stellarity.harvester.ability.frost_barrier", 1, EnumSet.of(EntityType.PIGLIN_BRUTE, EntityType.IRON_GOLEM, EntityType.SHULKER)),
    PIERCING_COLD("piercing_cold", "item.stellarity.harvester.ability.piercing_cold", 1, EnumSet.of(EntityType.WITCH, EntityType.VINDICATOR));

    /**
     * Порядок отображения в лоре совпадает [construct_description.mcfunction].
     */
    public static final List<HarvesterAbility> LORE_DISPLAY_ORDER = List.of(
            PIERCING_COLD,
            ARCTIC_WIND,
            FROSTBURN,
            EMBRITTLEMENT,
            FROST_BARRIER,
            ANIMA_CONDUIT,
            ECHO,
            CRYONICS
    );

    private final String id;
    private final String translateKey;
    private final int requiredProgress;
    private final Set<EntityType> entityTypes;

    HarvesterAbility(
            final String id,
            final String translateKey,
            final int requiredProgress,
            final Set<EntityType> entityTypes
    ) {
        this.id = id;
        this.translateKey = translateKey;
        this.requiredProgress = requiredProgress;
        this.entityTypes = entityTypes;
    }

    public String id() {
        return id;
    }

    public String translateKey() {
        return translateKey;
    }

    public int requiredProgress() {
        return requiredProgress;
    }


    public static Optional<HarvesterAbility> fromEntityType(final EntityType type) {
        if (type == null) {
            return Optional.empty();
        }
        for (final HarvesterAbility ability : values()) {
            if (ability.entityTypes.contains(type)) {
                return Optional.of(ability);
            }
        }
        return Optional.empty();
    }
}

