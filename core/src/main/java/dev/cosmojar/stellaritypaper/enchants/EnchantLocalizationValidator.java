package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.config.ConfigService;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public final class EnchantLocalizationValidator {

    private final StellarityPaperPlugin plugin;
    private final ConfigService configService;

    public EnchantLocalizationValidator(
            final StellarityPaperPlugin plugin,
            final ConfigService configService
    ) {
        this.plugin = plugin;
        this.configService = configService;
    }

    public void validate(final EnchantDefinitionRegistry enchantRegistry) {
        final Set<String> locales = new LinkedHashSet<>();
        locales.add(normalizeLocale(configService.read().localization().defaultLocale()));
        locales.add(normalizeLocale(configService.read().localization().fallbackLocale()));

        for (final String locale : locales) {
            final File file = new File(plugin.getDataFolder(), "messages_" + locale + ".yml");
            if (!file.exists()) {
                throw new IllegalStateException("Missing locale bundle for enchants: " + file.getName());
            }

            final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (!yaml.contains("enchants.format.line")) {
                throw new IllegalStateException("Missing key enchants.format.line in " + file.getName());
            }

            for (final EnchantDefinition definition : enchantRegistry.all()) {
                final String nameKey = definition.messageKeyBase() + ".name";
                if (!yaml.contains(nameKey)) {
                    throw new IllegalStateException("Missing key " + nameKey + " in " + file.getName());
                }
            }
        }
    }

    private String normalizeLocale(final String locale) {
        if (locale == null || locale.isBlank()) {
            return "RU";
        }
        return locale.trim().toUpperCase();
    }
}
