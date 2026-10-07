package dev.cosmojar.stellaritypaper.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ItemsConfigService {

    private final Plugin plugin;
    private final Map<String, YamlConfiguration> configs = new HashMap<>();

    public ItemsConfigService(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configs.clear();
        final File folder = new File(plugin.getDataFolder(), "ItemsSettings");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        final String[] files = {"armor.yml", "trinkets.yml", "weapons.yml", "tools.yml", "misc.yml", "blocks.yml"};
        for (final String file : files) {
            final String resourcePath = "ItemsSettings/" + file;
            final File targetFile = new File(folder, file);
            if (!targetFile.exists()) {
                plugin.saveResource(resourcePath, false);
            } else {
                ConfigAutoUpdater.update(plugin, resourcePath);
            }

            final YamlConfiguration config = YamlConfiguration.loadConfiguration(targetFile);

            final InputStream defStream = plugin.getResource("ItemsSettings/" + file);
            if (defStream != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8)));
            }

            configs.put(file, config);
        }
    }

    public YamlConfiguration getArmorConfig() {
        return configs.get("armor.yml");
    }

    public YamlConfiguration getTrinketsConfig() {
        return configs.get("trinkets.yml");
    }

    public YamlConfiguration getWeaponsConfig() {
        return configs.get("weapons.yml");
    }

    public YamlConfiguration getToolsConfig() {
        return configs.get("tools.yml");
    }

    public YamlConfiguration getMiscConfig() {
        return configs.get("misc.yml");
    }

    public YamlConfiguration getBlocksConfig() {
        return configs.get("blocks.yml");
    }
}
