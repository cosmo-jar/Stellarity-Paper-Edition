package dev.cosmojar.stellaritypaper.mechanics.advancements;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

public final class AdvancementService {

    public AdvancementService() {
    }

    /**
     * Awards all remaining criteria for the given advancement key.
     *
     * @param player The player
     * @param keyString Full namespaced key, e.g. "stellarity:altar_of_the_accursed/an_introduction_to_dark_magic"
     */
    public void grant(final Player player, final String keyString) {
        if (player == null || keyString == null || keyString.isBlank()) {
            return;
        }
        final NamespacedKey key = NamespacedKey.fromString(keyString);
        if (key == null) {
            return;
        }
        final Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) {
            return;
        }

        final AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (progress.isDone()) {
            return;
        }

        for (final String criteria : progress.getRemainingCriteria()) {
            progress.awardCriteria(criteria);
        }
    }

    /**
     * Awards a specific criteria for a multi-step advancement (e.g. biomes exploration).
     *
     * @param player The player
     * @param keyString Full namespaced key
     * @param criteria Criteria name
     */
    public void grantCriteria(final Player player, final String keyString, final String criteria) {
        if (player == null || keyString == null || criteria == null) {
            return;
        }
        final NamespacedKey key = NamespacedKey.fromString(keyString);
        if (key == null) {
            return;
        }
        final Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) {
            return;
        }

        final AdvancementProgress progress = player.getAdvancementProgress(advancement);
        progress.awardCriteria(criteria);
    }
}