package dev.cosmojar.stellaritypaper.data;

import dev.cosmojar.stellaritypaper.registry.KeyFactory;
import org.bukkit.NamespacedKey;

public final class PdcKeys {

    private final NamespacedKey itemId;
    private final NamespacedKey enchantsData;
    private final NamespacedKey enchantsSchema;
    private final NamespacedKey enchantsLocale;
    private final NamespacedKey harvesterAbilities;
    private final NamespacedKey harvesterAbilityProgress;
    private final NamespacedKey sessionId;

    public PdcKeys(final KeyFactory keyFactory) {
        this.itemId = keyFactory.stellarity("item_id");
        this.enchantsData = keyFactory.stellarity("enchants_data");
        this.enchantsSchema = keyFactory.stellarity("enchants_schema");
        this.enchantsLocale = keyFactory.stellarity("enchants_locale");
        this.harvesterAbilities = keyFactory.stellarity("harvester_abilities");
        this.harvesterAbilityProgress = keyFactory.stellarity("harvester_ability_progress");
        this.sessionId = keyFactory.stellarity("session_id");
    }

    public NamespacedKey itemId() {
        return itemId;
    }

    public NamespacedKey enchantsData() {
        return enchantsData;
    }

    public NamespacedKey enchantsSchema() {
        return enchantsSchema;
    }

    public NamespacedKey enchantsLocale() {
        return enchantsLocale;
    }

    public NamespacedKey harvesterAbilities() {
        return harvesterAbilities;
    }

    public NamespacedKey harvesterAbilityProgress() {
        return harvesterAbilityProgress;
    }

    public NamespacedKey sessionId() {
        return sessionId;
    }
}
