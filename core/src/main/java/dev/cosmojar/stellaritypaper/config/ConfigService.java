package dev.cosmojar.stellaritypaper.config;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigService {

    private final StellarityPaperPlugin plugin;

    public ConfigService(final StellarityPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public ConfigModels.StellarityConfig read() {
        final FileConfiguration config = plugin.getConfig();

        final ConfigModels.Features features = new ConfigModels.Features(
                config.getBoolean("features.stellarity-ender-structures-enabled", true),
                config.getBoolean("features.void-totem-enabled", true),
                config.getBoolean("features.trident-return-enabled", true),
                config.getBoolean("features.void-fishing-enabled", true),
                config.getBoolean("features.void-fishing-biome-allocation", false),
                config.getBoolean("features.cauldron-crafting-enabled", true),
                config.getBoolean("features.consecration-enabled", true),
                config.getBoolean("features.accursed-altar-enabled", true),
                config.getBoolean("features.show-description-lore", false)
        );

        final ConfigModels.Localization localization = new ConfigModels.Localization(
                config.getString("localization.default-locale", "RU"),
                config.getString("localization.fallback-locale", "RU")
        );

        return new ConfigModels.StellarityConfig(features, localization);
    }

    public void reload() {
        ConfigAutoUpdater.update(plugin, "config.yml");
        plugin.reloadConfig();
    }
}
