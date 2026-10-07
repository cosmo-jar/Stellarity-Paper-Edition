package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Единая точка воспроизведения звуков Stellarity.
 * Сначала попытка проиграть namespaced звук из ресурс-пака, затем fallback на ванильный.
 */
public final class StellaritySoundService {

    public void play(
            final Entity source,
            final String soundKey,
            final float volume,
            final float pitch,
            final Sound fallback
    ) {
        if (source == null || !source.isValid()) {
            return;
        }
        play(source.getLocation(), soundKey, volume, pitch, fallback);
    }

    public void play(
            final Player player,
            final String soundKey,
            final float volume,
            final float pitch,
            final Sound fallback
    ) {
        if (player == null || !player.isOnline()) {
            return;
        }
        play(player.getLocation(), soundKey, volume, pitch, fallback);
    }

    public void play(
            final Location location,
            final String soundKey,
            final float volume,
            final float pitch,
            final Sound fallback
    ) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (soundKey != null && !soundKey.isBlank()) {
            location.getWorld().playSound(location, soundKey, volume, pitch);
            return;
        }
        if (fallback != null) {
            location.getWorld().playSound(location, fallback, volume, pitch);
        }
    }
}

