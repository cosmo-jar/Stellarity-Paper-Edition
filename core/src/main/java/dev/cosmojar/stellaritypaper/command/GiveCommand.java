package dev.cosmojar.stellaritypaper.command;

import dev.cosmojar.stellaritypaper.enchants.CustomEnchantedBookFactory;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.mechanics.loot.ExplorerMapService;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

public final class GiveCommand {

    private final ItemDefinitionRegistry itemRegistry;
    private final CustomItemFactory itemFactory;
    private final CustomEnchantedBookFactory bookFactory;
    private final MessageService messageService;
    private final ExplorerMapService explorerMapService;

    public GiveCommand(
            final ItemDefinitionRegistry itemRegistry,
            final CustomItemFactory itemFactory,
            final CustomEnchantedBookFactory bookFactory,
            final MessageService messageService,
            final ExplorerMapService explorerMapService
    ) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.bookFactory = bookFactory;
        this.messageService = messageService;
        this.explorerMapService = explorerMapService;
    }

    public void execute(final CommandSender sender, final String[] args) {
        if (args.length < 4) {
            sender.sendMessage(messageService.message("command.stellarity.usage-give"));
            return;
        }
        final String targetPlayerName = args[1];
        final String rawCategory = args[2];
        final String rawItemName = args[3];

        final Player target = Bukkit.getPlayerExact(targetPlayerName);
        if (target == null) {
            sender.sendMessage(messageService.message(
                    "command.give.player-not-found",
                    java.util.Map.of("player", targetPlayerName)
            ));
            return;
        }

        final String category = rawCategory.toLowerCase(Locale.ROOT);
        final String itemName = rawItemName.toLowerCase(Locale.ROOT);

        if (category.equals("enchantments_books") || category.equals("books")) {
            int level = 1;
            if (args.length >= 5) {
                try {
                    level = Integer.parseInt(args[4]);
                } catch (NumberFormatException ignored) {
                }
            }
            final Optional<ItemStack> bookOpt = bookFactory.createBook(itemName, level);
            if (bookOpt.isEmpty()) {
                sender.sendMessage(messageService.message(
                        "command.give.unknown-item-in-category",
                        java.util.Map.of("category", rawCategory, "item", rawItemName)
                ));
                return;
            }
            final String levelSuffix = messageService.raw("command.give.level-suffix").replace("%level%", String.valueOf(level));
            target.getInventory().addItem(bookOpt.get());
            sender.sendMessage(messageService.message(
                    "command.give.success",
                    java.util.Map.of("player", target.getName(), "category", rawCategory, "item", itemName + levelSuffix)
            ));
            return;
        }

        if (explorerMapService != null && explorerMapService.isMapItemKey(itemName)) {
            final Optional<ItemStack> mapOpt = explorerMapService.createExplorerMap(itemName, target.getWorld(), target.getLocation());
            if (mapOpt.isPresent()) {
                target.getInventory().addItem(mapOpt.get());
                sender.sendMessage(messageService.message(
                        "command.give.success",
                        java.util.Map.of("player", target.getName(), "category", category, "item", itemName)
                ));
            } else {
                sender.sendMessage(messageService.message(
                        "command.give.unknown-item-in-category",
                        java.util.Map.of("category", rawCategory, "item", rawItemName)
                ));
            }
            return;
        }

        if (!itemRegistry.listCategories().contains(category)) {
            sender.sendMessage(messageService.message(
                    "command.give.unknown-category",
                    java.util.Map.of("category", rawCategory)
            ));
            return;
        }

        final Optional<CustomItemDefinition> definition = itemRegistry.findByCategoryAndName(category, itemName);
        if (definition.isEmpty()) {
            sender.sendMessage(messageService.message(
                    "command.give.unknown-item-in-category",
                    java.util.Map.of("category", rawCategory, "item", rawItemName)
            ));
            return;
        }

        String customMaterial = null;
        for (int i = 4; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-material") && i + 1 < args.length) {
                customMaterial = args[i + 1];
                break;
            }
        }

        final ItemStack item = itemFactory.create(definition.get(), customMaterial);
        target.getInventory().addItem(item);
        sender.sendMessage(messageService.message(
                "command.give.success",
                java.util.Map.of("player", target.getName(), "category", category, "item", itemName)
        ));
    }
}
