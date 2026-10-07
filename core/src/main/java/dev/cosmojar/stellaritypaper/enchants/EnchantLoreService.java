package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.text.MessageService;
import dev.cosmojar.stellaritypaper.text.TextService;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EnchantLoreService {

    private final TextService textService;
    private final MessageService messageService;
    private final EnchantDefinitionRegistry enchantRegistry;

    public EnchantLoreService(
            final TextService textService,
            final MessageService messageService,
            final EnchantDefinitionRegistry enchantRegistry
    ) {
        this.textService = textService;
        this.messageService = messageService;
        this.enchantRegistry = enchantRegistry;
    }

    public List<Component> buildLore(final List<EnchantInstance> enchants) {
        final List<Component> lines = new ArrayList<>();
        for (final EnchantInstance instance : enchants) {
            final String id = instance.id();
            if (id.startsWith("_technical/void_pendant/")) {
                final String gem = id.substring(id.lastIndexOf('/') + 1);
                final net.kyori.adventure.text.format.NamedTextColor color = getGemColor(gem);
                
                final Component header = Component.empty()
                        .append(Component.translatable("item.stellarity.void_pendant." + gem + ".description.name").color(color).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                        .append(Component.text(": ").color(net.kyori.adventure.text.format.NamedTextColor.WHITE).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                        .append(Component.translatable("item.stellarity.void_pendant." + gem + ".description.1").color(net.kyori.adventure.text.format.NamedTextColor.WHITE).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, false).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                lines.add(header);

                if ("amethyst".equals(gem) || "copper".equals(gem) || "gold".equals(gem) || "netherite".equals(gem) || "quartz".equals(gem)) {
                    final Component desc2 = Component.translatable("item.stellarity.void_pendant." + gem + ".description.2")
                            .color(net.kyori.adventure.text.format.NamedTextColor.WHITE).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, false).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
                    lines.add(desc2);
                }
                continue;
            }

            final EnchantDefinition definition = enchantRegistry.findById(id).orElse(null);
            if (definition == null) {
                continue;
            }

            final String nameKey = definition.messageKeyBase() + ".name";
            final String levelKey = definition.messageKeyBase() + ".levels." + instance.level();

            final String name;
            final boolean hideLevel;
            if (messageService.hasKey(levelKey)) {
                name = messageService.raw(levelKey);
                hideLevel = true;
            } else {
                name = messageService.raw(nameKey);
                hideLevel = false;
            }

            final String romanLevel = hideLevel ? "" : " " + toRoman(instance.level());

            final String line = messageService.raw(
                    "enchants.format.line",
                    Map.of("name", name, "level", romanLevel)
            );
            lines.add(textService.mm(line));
        }
        return lines;
    }

    private net.kyori.adventure.text.format.NamedTextColor getGemColor(final String gem) {
        switch (gem) {
            case "amethyst": return net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
            case "copper": return net.kyori.adventure.text.format.NamedTextColor.GOLD;
            case "diamond": return net.kyori.adventure.text.format.NamedTextColor.AQUA;
            case "emerald": return net.kyori.adventure.text.format.NamedTextColor.GREEN;
            case "gold": return net.kyori.adventure.text.format.NamedTextColor.YELLOW;
            case "iron": return net.kyori.adventure.text.format.NamedTextColor.GRAY;
            case "lapis": return net.kyori.adventure.text.format.NamedTextColor.BLUE;
            case "netherite": return net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
            default: return net.kyori.adventure.text.format.NamedTextColor.WHITE;
        }
    }

    private String toRoman(final int number) {
        if (number <= 0) {
            return "I";
        }
        final int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        final String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int value = number;
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (value >= values[i]) {
                value -= values[i];
                result.append(symbols[i]);
            }
        }
        return result.toString();
    }
}
