package dev.cosmojar.stellaritypaper.text;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.config.ConfigAutoUpdater;
import dev.cosmojar.stellaritypaper.config.ConfigModels;
import dev.cosmojar.stellaritypaper.config.ConfigService;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public final class MessageService {

    private static final String DEFAULT_LOCALE = "RU";
    private static final String DEFAULT_FILE = "messages_RU.yml";
    private static final String EN_FILE = "messages_EN.yml";

    private final StellarityPaperPlugin plugin;
    private final ConfigService configService;
    private final TextService textService;

    private YamlConfiguration bundle;

    public MessageService(
            final StellarityPaperPlugin plugin,
            final ConfigService configService,
            final TextService textService
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.textService = textService;

        saveDefaultBundleIfMissing();
        reload();
    }

    public void reload() {
        ConfigAutoUpdater.update(plugin, DEFAULT_FILE);
        ConfigAutoUpdater.update(plugin, EN_FILE);

        final ConfigModels.Localization localization = configService.read().localization();
        final String defaultLocale = normalizeLocale(localization.defaultLocale(), DEFAULT_LOCALE);
        final String fallbackLocale = normalizeLocale(localization.fallbackLocale(), DEFAULT_LOCALE);

        this.bundle = loadBundleOrFallback(defaultLocale, fallbackLocale);
    }

    public Component message(final String key) {
        return textService.mm(raw(key));
    }

    public Component message(final String key, final Map<String, String> placeholders) {
        return textService.mm(raw(key, placeholders));
    }

    public boolean hasKey(final String key) {
        return bundle != null && bundle.contains(key);
    }

    public String raw(final String key) {
        final String value = bundle.getString(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        plugin.getLogger().warning("The message key is missing: " + key);
        return "<red>The message key is missing: " + key + "</red>";
    }

    public String raw(final String key, final Map<String, String> placeholders) {
        String value = raw(key);
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            final String placeholder = "%" + entry.getKey() + "%";
            value = value.replace(placeholder, Objects.toString(entry.getValue(), ""));
        }
        return value;
    }

    private void saveDefaultBundleIfMissing() {
        saveBundleIfMissing(DEFAULT_FILE);
        saveBundleIfMissing(EN_FILE);
        ConfigAutoUpdater.update(plugin, DEFAULT_FILE);
        ConfigAutoUpdater.update(plugin, EN_FILE);
    }

    private void saveBundleIfMissing(final String fileName) {
        final File defaultBundle = new File(plugin.getDataFolder(), fileName);
        if (!defaultBundle.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private YamlConfiguration loadBundleOrFallback(final String locale, final String fallbackLocale) {
        String fileName = "messages_" + locale + ".yml";
        File directFile = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration config;

        if (directFile.exists()) {
            config = YamlConfiguration.loadConfiguration(directFile);
        } else {
            File fallbackFile = new File(plugin.getDataFolder(), "messages_" + fallbackLocale + ".yml");
            if (fallbackFile.exists()) {
                plugin.getLogger().warning(
                        "The localization file messages_" + locale + ".yml was not found. Used fallback: " + fallbackLocale
                );
                fileName = "messages_" + fallbackLocale + ".yml";
                config = YamlConfiguration.loadConfiguration(fallbackFile);
            } else {
                plugin.getLogger().warning(
                        "Fallback localization file messages_" + fallbackLocale + ".yml was not found. The default file is used."
                );
                saveDefaultBundleIfMissing();
                fileName = DEFAULT_FILE;
                config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), DEFAULT_FILE));
            }
        }

        try (java.io.InputStream defStream = plugin.getResource(fileName)) {
            if (defStream != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defStream, java.nio.charset.StandardCharsets.UTF_8)));
            }
        } catch (Exception ignored) {
        }

        return config;
    }

    private String normalizeLocale(final String locale, final String defaultValue) {
        if (locale == null || locale.isBlank()) {
            return defaultValue;
        }
        return locale.trim().toUpperCase();
    }
}
