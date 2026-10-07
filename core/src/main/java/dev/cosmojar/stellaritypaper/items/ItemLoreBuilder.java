package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.config.ConfigService;
import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.text.MessageService;
import dev.cosmojar.stellaritypaper.text.TextService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemLoreBuilder {

    private static final Key FONT_STELLARITY_TOOLTIP = Key.key("stellarity:tooltip");
    private static final TextColor COLOR_DEFAULT_LORE = TextColor.color(0xEEEEEE);
    private static final TextColor COLOR_TIP = TextColor.color(0x727272);
    private static final TextColor COLOR_DONATOR = TextColor.color(0xF96854);
    private static final TextColor COLOR_DEVELOPER = TextColor.color(0xBA02D7);
    private static final TextColor COLOR_MEMBER_666 = TextColor.color(0x76389B);
    private static final TextColor COLOR_ARMOR_BONUS = TextColor.color(0xFF9952);
    private static final TextColor COLOR_COOLDOWN = TextColor.color(0x9E9E9E);
    private static final TextColor COLOR_STELLARITY = TextColor.color(0xEAA7FF);

    private final TextService textService;
    private final ConfigService configService;
    private final ItemsConfigService itemsConfigService;
    private final MessageService messageService;

    public ItemLoreBuilder(final TextService textService, final ConfigService configService, final ItemsConfigService itemsConfigService) {
        this(textService, configService, itemsConfigService, null);
    }

    public ItemLoreBuilder(final TextService textService, final ConfigService configService, final ItemsConfigService itemsConfigService, final MessageService messageService) {
        this.textService = textService;
        this.configService = configService;
        this.itemsConfigService = itemsConfigService;
        this.messageService = messageService;
    }

    public List<Component> buildAlways(final CustomItemDefinition definition) {
        if (definition != null && "loaf_of_plenty".equalsIgnoreCase(definition.itemDefinition().pdcItemId())) {
            return buildLoafOfPlentyLore(1);
        }
        final List<Component> lore = new ArrayList<>();
        final Map<String, Integer> counts = new HashMap<>();
        String previousKey = null;

        for (final String key : definition.itemText().loreTranslateKeys()) {
            if (key == null || key.isBlank() || "empty".equalsIgnoreCase(key)) {
                addEmptyLineIfNeeded(lore);
                previousKey = null;
                continue;
            }

            final int index = counts.getOrDefault(key, 0);
            counts.put(key, index + 1);

            if (shouldPrependEmptyLine(key, previousKey, lore)) {
                addEmptyLineIfNeeded(lore);
            }

            lore.add(resolveTranslation(key, index, definition));
            previousKey = key;
        }
        return lore;
    }

    public List<Component> buildAlways(final CustomItemDefinition definition, final ItemStack item) {
        if (definition != null && "loaf_of_plenty".equalsIgnoreCase(definition.itemDefinition().pdcItemId())) {
            int step = 1;
            if (item != null && item.hasItemMeta()) {
                final ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    final NamespacedKey finalKey = new NamespacedKey("stellarity", "loaf_of_plenty_final");
                    if (meta.getPersistentDataContainer().has(finalKey, PersistentDataType.BOOLEAN)) {
                        return buildLoafOfPlentyLore(15);
                    }
                    final NamespacedKey stepKey = new NamespacedKey("stellarity", "loaf_step");
                    if (meta.getPersistentDataContainer().has(stepKey, PersistentDataType.INTEGER)) {
                        step = meta.getPersistentDataContainer().getOrDefault(stepKey, PersistentDataType.INTEGER, 1);
                    }
                }
            }
            return buildLoafOfPlentyLore(step);
        }
        return buildAlways(definition);
    }

    public List<Component> buildBase(final CustomItemDefinition definition) {
        if (!configService.read().features().showDescriptionLore()) {
            return new ArrayList<>();
        }
        return buildAlways(definition);
    }

    public List<Component> buildBase(final CustomItemDefinition definition, final ItemStack item) {
        if (!configService.read().features().showDescriptionLore()) {
            return new ArrayList<>();
        }
        return buildAlways(definition, item);
    }

    private boolean shouldPrependEmptyLine(final String key, final String previousKey, final List<Component> lore) {
        if (lore.isEmpty()) {
            return false;
        }
        if (isFlavorText(key)) {
            return previousKey == null || !isFlavorText(previousKey);
        }
        if (isTip(key)) {
            return previousKey != null && !isFlavorText(previousKey) && !isTip(previousKey);
        }
        if (isBadge(key)) {
            return true;
        }
        if (isArmorBonusHeader(key)) {
            return true;
        }
        if ("Stellarity".equals(key)) {
            return true;
        }
        return false;
    }

    private boolean isFlavorText(final String key) {
        return key.endsWith(".flavor_text") || key.contains(".flavor_text.");
    }

    private boolean isTip(final String key) {
        return key.endsWith(".tip") || key.contains(".tip.");
    }

    private boolean isBadge(final String key) {
        return "items.stellarity.donator".equals(key)
                || "items.stellarity.developer".equals(key)
                || "- Member #666 Item -".equals(key);
    }

    private boolean isArmorBonusHeader(final String key) {
        return "items.stellarity.armor.set_bonus".equals(key)
                || "items.stellarity.armor.piece_bonus".equals(key);
    }

    private void addEmptyLineIfNeeded(final List<Component> lore) {
        if (!lore.isEmpty() && !Component.empty().equals(lore.get(lore.size() - 1))) {
            lore.add(Component.empty());
        }
    }

    private Component resolveTranslation(final String key, final int occurrenceIndex, final CustomItemDefinition definition) {
        final String pdcItemId = definition.itemDefinition().pdcItemId();

        if ("book.byAuthor".equals(key)) {
            final String author = getStringFromConfigs(pdcItemId, "author", "Ashley");
            return Component.translatable("book.byAuthor", Component.text(author)
                    .color(NamedTextColor.GRAY)
                    .font(org.bukkit.NamespacedKey.fromString("illageralt")))
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("items.stellarity.spellbooks.cooldown".equals(key)) {
            final double seconds = getCooldownSecondsFromConfigs(pdcItemId);
            return Component.translatable("items.stellarity.spellbooks.cooldown",
                    Component.text(formatDouble(seconds)).color(NamedTextColor.WHITE))
                    .color(COLOR_COOLDOWN)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("items.stellarity.spellbooks.cooldown_long".equals(key)) {
            final double seconds = getCooldownSecondsFromConfigs(pdcItemId);
            final double minutes = seconds / 60.0;
            return Component.translatable("items.stellarity.spellbooks.cooldown_long",
                    Component.text(formatDouble(minutes)).color(NamedTextColor.WHITE))
                    .color(COLOR_COOLDOWN)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("block.stellarity.altar_of_the_sacred.description.3".equals(key)) {
            return Component.translatable("block.stellarity.altar_of_the_sacred.description.3",
                    Component.translatable("item.stellarity.starlight_soot")
                            .color(TextColor.color(0xFFD655))
                            .decoration(TextDecoration.ITALIC, false)
            ).color(COLOR_DEFAULT_LORE)
             .decoration(TextDecoration.ITALIC, false);
        }
        if ("item.stellarity.spirit_dagger.description.2".equals(key)) {
            final String pairedItemId = getStringFromConfigs(pdcItemId, "paired-item", "weapons_the_beginning".equalsIgnoreCase(pdcItemId) ? "spirit_dagger" : "weapons_the_beginning");
            final String translateKey = "weapons_the_beginning".equalsIgnoreCase(pairedItemId) ? "item.stellarity.spirit_dagger.1" : "item.stellarity.spirit_dagger.2";
            return Component.translatable("item.stellarity.spirit_dagger.description.2",
                    Component.translatable(translateKey)
                            .color(TextColor.color(0x68C6F5))
                            .decoration(TextDecoration.ITALIC, false)
            ).color(COLOR_DEFAULT_LORE)
             .decoration(TextDecoration.ITALIC, false);
        }
        if ("item.stellarity.enderite_smithing_template.ingredients".equals(key)) {
            final List<?> ingredients = getListFromConfigs(pdcItemId, "ingredients");
            if (ingredients != null && occurrenceIndex < ingredients.size()) {
                final Object element = ingredients.get(occurrenceIndex);
                if (element instanceof Map<?, ?> ing) {
                    final Object countObj = ing.get("count");
                    final Object itemObj = ing.get("item");
                    if (countObj instanceof Number countNum && itemObj != null) {
                        return Component.translatable("item.stellarity.enderite_smithing_template.ingredients",
                                Component.translatable("item.stellarity.enderite_smithing_template.ingredients.count." + countNum.intValue())
                                        .color(COLOR_DEFAULT_LORE)
                                        .decoration(TextDecoration.ITALIC, false),
                                Component.translatable(itemObj.toString())
                                        .color(COLOR_DEFAULT_LORE)
                                        .decoration(TextDecoration.ITALIC, false)
                        ).color(COLOR_DEFAULT_LORE)
                         .decoration(TextDecoration.ITALIC, false);
                    }
                }
            }
            if (occurrenceIndex == 0) {
                return Component.translatable("item.stellarity.enderite_smithing_template.ingredients",
                        Component.translatable("item.stellarity.enderite_smithing_template.ingredients.count.4")
                                .color(COLOR_DEFAULT_LORE)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.translatable("item.stellarity.hallowed_ingot")
                                .color(COLOR_DEFAULT_LORE)
                                .decoration(TextDecoration.ITALIC, false)
                ).color(COLOR_DEFAULT_LORE)
                 .decoration(TextDecoration.ITALIC, false);
            } else if (occurrenceIndex == 1) {
                return Component.translatable("item.stellarity.enderite_smithing_template.ingredients",
                        Component.translatable("item.stellarity.enderite_smithing_template.ingredients.count.4")
                                .color(COLOR_DEFAULT_LORE)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.translatable("item.stellarity.chorus_plating")
                                .color(COLOR_DEFAULT_LORE)
                                .decoration(TextDecoration.ITALIC, false)
                ).color(COLOR_DEFAULT_LORE)
                 .decoration(TextDecoration.ITALIC, false);
            } else if (occurrenceIndex == 2) {
                return Component.translatable("item.stellarity.enderite_smithing_template.ingredients",
                        Component.translatable("item.stellarity.enderite_smithing_template.ingredients.count.4")
                                .color(COLOR_DEFAULT_LORE)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.translatable("item.minecraft.shulker_shell")
                                .color(COLOR_DEFAULT_LORE)
                                .decoration(TextDecoration.ITALIC, false)
                ).color(COLOR_DEFAULT_LORE)
                 .decoration(TextDecoration.ITALIC, false);
            } else if (occurrenceIndex == 3) {
                return Component.translatable("item.stellarity.enderite_smithing_template.ingredients",
                        Component.translatable("item.stellarity.enderite_smithing_template.ingredients.count.8")
                                .color(COLOR_DEFAULT_LORE)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.translatable("block.minecraft.cherry_leaves")
                                .color(COLOR_DEFAULT_LORE)
                                .decoration(TextDecoration.ITALIC, false)
                ).color(COLOR_DEFAULT_LORE)
                 .decoration(TextDecoration.ITALIC, false);
            }
        }
        if ("color.minecraft.yellow".equals(key)) {
            return Component.translatable("color.minecraft.yellow")
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("color.minecraft.magenta".equals(key)) {
            return Component.translatable("color.minecraft.magenta")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("color.minecraft.lime".equals(key)) {
            return Component.translatable("color.minecraft.lime")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("color.minecraft.light_blue".equals(key)) {
            return Component.translatable("color.minecraft.light_blue")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("Radiant".equals(key)) {
            return Component.translatable("Radiant")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("Stellarity".equals(key)) {
            return Component.translatable("Stellarity")
                    .color(COLOR_STELLARITY)
                    .font(FONT_STELLARITY_TOOLTIP)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if (isFlavorText(key)) {
            TextColor flavorColor = COLOR_DEFAULT_LORE;
            if ("item.stellarity.endonomicon.flavor_text".equals(key)) {
                flavorColor = TextColor.color(0xBB6BDE);
            } else if ("item.stellarity.flavors_of_the_void.flavor_text.1".equals(key)
                    || "item.stellarity.flavors_of_the_void.flavor_text.2".equals(key)) {
                flavorColor = TextColor.color(0xDEC36B);
            }
            return textService.tr(key)
                    .color(flavorColor)
                    .decoration(TextDecoration.ITALIC, true);
        }
        if (isTip(key)) {
            return textService.tr(key)
                    .color(COLOR_TIP)
                    .decoration(TextDecoration.ITALIC, true);
        }
        if ("items.stellarity.donator".equals(key)) {
            return textService.tr(key)
                    .color(COLOR_DONATOR)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("items.stellarity.developer".equals(key)) {
            return textService.tr(key)
                    .color(COLOR_DEVELOPER)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("- Member #666 Item -".equals(key)) {
            return Component.text("- Member #666 Item -")
                    .color(COLOR_MEMBER_666)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if (isArmorBonusHeader(key)) {
            return textService.tr(key)
                    .color(COLOR_ARMOR_BONUS)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("items.stellarity.music_discs.requires_addon".equals(key)) {
            return textService.tr(key)
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if (key.endsWith(".usage")) {
            return textService.tr(key)
                    .color(NamedTextColor.BLUE)
                    .decoration(TextDecoration.ITALIC, false);
        }
        if ("item.minecraft.smithing_template.applies_to".equals(key)
                || "item.minecraft.smithing_template.ingredients".equals(key)
                || "item.stellarity.enderite_smithing_template.upgrade".equals(key)
                || "book.generation.3".equals(key)) {
            return textService.tr(key)
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false);
        }
        return textService.tr(key)
                .color(COLOR_DEFAULT_LORE)
                .decoration(TextDecoration.ITALIC, false);
    }

    private double getCooldownSecondsFromConfigs(final String pdcItemId) {
        if (itemsConfigService != null) {
            for (final org.bukkit.configuration.file.YamlConfiguration config : List.of(
                    itemsConfigService.getWeaponsConfig(),
                    itemsConfigService.getMiscConfig(),
                    itemsConfigService.getToolsConfig(),
                    itemsConfigService.getTrinketsConfig(),
                    itemsConfigService.getArmorConfig()
            )) {
                if (config != null && config.contains(pdcItemId + ".cooldown-seconds")) {
                    return config.getDouble(pdcItemId + ".cooldown-seconds");
                }
            }
        }
        if ("book_of_conveyance".equalsIgnoreCase(pdcItemId)) return 20.0;
        if ("book_of_jinx".equalsIgnoreCase(pdcItemId)) return 12.0;
        if ("book_of_light".equalsIgnoreCase(pdcItemId)) return 3.0;
        if ("book_of_obstruct".equalsIgnoreCase(pdcItemId)) return 15.0;
        if ("book_of_return".equalsIgnoreCase(pdcItemId)) return 60.0;
        if ("book_of_updraft".equalsIgnoreCase(pdcItemId)) return 15.0;
        return 7.0;
    }

    private String getStringFromConfigs(final String pdcItemId, final String key, final String defaultValue) {
        if (itemsConfigService != null) {
            for (final org.bukkit.configuration.file.YamlConfiguration config : List.of(
                    itemsConfigService.getMiscConfig(),
                    itemsConfigService.getWeaponsConfig(),
                    itemsConfigService.getToolsConfig(),
                    itemsConfigService.getTrinketsConfig(),
                    itemsConfigService.getArmorConfig()
            )) {
                if (config != null && config.contains(pdcItemId + "." + key)) {
                    return config.getString(pdcItemId + "." + key);
                }
            }
        }
        return defaultValue;
    }

    private List<?> getListFromConfigs(final String pdcItemId, final String key) {
        if (itemsConfigService != null) {
            for (final org.bukkit.configuration.file.YamlConfiguration config : List.of(
                    itemsConfigService.getMiscConfig(),
                    itemsConfigService.getWeaponsConfig(),
                    itemsConfigService.getToolsConfig(),
                    itemsConfigService.getTrinketsConfig(),
                    itemsConfigService.getArmorConfig()
            )) {
                if (config != null && config.contains(pdcItemId + "." + key)) {
                    return config.getList(pdcItemId + "." + key);
                }
            }
        }
        return null;
    }

    private String formatDouble(final double value) {
        if (value == (long) value) {
            return String.format(java.util.Locale.US, "%d", (long) value);
        } else {
            return String.format(java.util.Locale.US, "%.1f", value);
        }
    }

    public List<Component> buildLoafOfPlentyLore(final int step) {
        final List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (step < 15) {
            for (final int lineNum : getLoafLinesForStep(step)) {
                final Component translated;
                if (messageService != null) {
                    translated = messageService.message("loaf_of_plenty.description." + lineNum);
                } else {
                    translated = Component.text("loaf_of_plenty.description." + lineNum);
                }
                lore.add(translated
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
        }
        lore.add(Component.text("Donator Item", TextColor.color(0xF96854))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Stellarity", TextColor.color(0xEAA7FF))
                .font(FONT_STELLARITY_TOOLTIP)
                .decoration(TextDecoration.ITALIC, false));
        return lore;
    }

    public static List<Integer> getLoafLinesForStep(final int step) {
        switch (step) {
            case 1: return List.of(1, 2);
            case 2: return List.of(3, 4);
            case 3: return List.of(5, 6);
            case 4: return List.of(7);
            case 5: return List.of(8, 9);
            case 6: return List.of(10);
            case 7: return List.of(11, 12);
            case 8: return List.of(13, 14);
            case 9: return List.of(15);
            case 10: return List.of(16, 17);
            case 11: return List.of(18, 19);
            case 12: return List.of(20, 21);
            case 13: return List.of(22, 23);
            case 14: return List.of(24, 25);
            default: return List.of();
        }
    }

    public List<Component> build(final CustomItemDefinition definition) {
        return buildAlways(definition);
    }
}
