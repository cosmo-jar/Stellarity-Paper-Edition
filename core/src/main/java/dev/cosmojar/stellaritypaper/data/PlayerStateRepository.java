package dev.cosmojar.stellaritypaper.data;

import org.bukkit.entity.Player;

import java.util.Optional;

public final class PlayerStateRepository {

    private final PdcKeys keys;

    public PlayerStateRepository(final PdcKeys keys) {
        this.keys = keys;
    }

    public void setHarvesterAbilityProgress(final Player player, final String data) {
        player.getPersistentDataContainer().set(
                keys.harvesterAbilityProgress(),
                org.bukkit.persistence.PersistentDataType.STRING,
                data
        );
    }

    public Optional<String> getHarvesterAbilityProgress(final Player player) {
        final String value = player.getPersistentDataContainer().get(
                keys.harvesterAbilityProgress(),
                org.bukkit.persistence.PersistentDataType.STRING
        );
        return Optional.ofNullable(value);
    }
}
