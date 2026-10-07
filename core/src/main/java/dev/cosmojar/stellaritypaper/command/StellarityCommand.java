package dev.cosmojar.stellaritypaper.command;

import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class StellarityCommand implements CommandExecutor, TabCompleter {

    private final ReloadCommand reloadCommand;
    private final GiveCommand giveCommand;
    private final EnchantAdminCommand enchantAdminCommand;
    private final UninstallService uninstallService;
    private final ItemDefinitionRegistry itemRegistry;
    private final dev.cosmojar.stellaritypaper.enchants.EnchantDefinitionRegistry enchantRegistry;
    private final MessageService messageService;
    private final dev.cosmojar.stellaritypaper.data.ItemStateRepository itemStateRepository;
    private final dev.cosmojar.stellaritypaper.ui.ItemInfoGuiService itemInfoGuiService;

    public StellarityCommand(
            final ReloadCommand reloadCommand,
            final GiveCommand giveCommand,
            final EnchantAdminCommand enchantAdminCommand,
            final UninstallService uninstallService,
            final ItemDefinitionRegistry itemRegistry,
            final dev.cosmojar.stellaritypaper.enchants.EnchantDefinitionRegistry enchantRegistry,
            final MessageService messageService,
            final dev.cosmojar.stellaritypaper.data.ItemStateRepository itemStateRepository,
            final dev.cosmojar.stellaritypaper.ui.ItemInfoGuiService itemInfoGuiService
    ) {
        this.reloadCommand = reloadCommand;
        this.giveCommand = giveCommand;
        this.enchantAdminCommand = enchantAdminCommand;
        this.uninstallService = uninstallService;
        this.itemRegistry = itemRegistry;
        this.enchantRegistry = enchantRegistry;
        this.messageService = messageService;
        this.itemStateRepository = itemStateRepository;
        this.itemInfoGuiService = itemInfoGuiService;
    }

    @Override
    public boolean onCommand(
            final CommandSender sender,
            final Command command,
            final String label,
            final String[] args
    ) {
        if (args.length == 0) {
            sender.sendMessage(messageService.message("command.stellarity.usage-main"));
            return true;
        }

        final String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            if (!sender.hasPermission("stellarity.command.reload")) {
                sender.sendMessage(messageService.message("command.common.no-permission"));
                return true;
            }
            reloadCommand.execute(sender);
            return true;
        }

        if (sub.equals("give")) {
            if (!sender.hasPermission("stellarity.command.give")) {
                sender.sendMessage(messageService.message("command.common.no-permission"));
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(messageService.message("command.stellarity.usage-give"));
                return true;
            }

            giveCommand.execute(sender, args);
            return true;
        }

        if (sub.equals("enchant")) {
            if (!sender.hasPermission("stellarity.command.enchant")) {
                sender.sendMessage(messageService.message("command.common.no-permission"));
                return true;
            }
            final String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
            enchantAdminCommand.execute(sender, subArgs);
            return true;
        }

        if (sub.equals("item")) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("info")) {
                sender.sendMessage(messageService.message("command.item.usage"));
                return true;
            }
            if (!sender.hasPermission("stellarity.command.item.info")) {
                sender.sendMessage(messageService.message("command.common.no-permission"));
                return true;
            }
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(messageService.message("command.common.only-player"));
                return true;
            }

            final org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
            final String customId = itemStateRepository.getItemId(item).orElse(null);
            if (customId == null) {
                player.sendMessage(messageService.message("command.item.no-custom-item"));
                return true;
            }

            final dev.cosmojar.stellaritypaper.items.CustomItemDefinition definition = itemRegistry.findByPdcItemId(customId).orElse(null);
            if (definition == null) {
                player.sendMessage(messageService.message("command.item.no-custom-item"));
                return true;
            }

            itemInfoGuiService.open(player, item, definition);
            return true;
        }

        if (sub.equals("uninstall")) {
            if (!sender.hasPermission("stellarity.command.uninstall")) {
                sender.sendMessage(messageService.message("command.common.no-permission"));
                return true;
            }
            uninstallService.executeUninstall(sender);
            return true;
        }

        sender.sendMessage(messageService.message(
                "command.stellarity.unknown-subcommand",
                java.util.Map.of("subcommand", sub)
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(
            final CommandSender sender,
            final Command command,
            final String alias,
            final String[] args
    ) {
        if (args.length == 1) {
            return filterByPrefix(List.of("reload", "give", "enchant", "item", "uninstall"), args[0]);
        }

        if (args[0].equalsIgnoreCase("item")) {
            if (args.length == 2) {
                return filterByPrefix(List.of("info"), args[1]);
            }
            return List.of();
        }

        if (!args[0].equalsIgnoreCase("give")) {
            if (!args[0].equalsIgnoreCase("enchant")) {
                return List.of();
            }
            return enchantTabComplete(args);
        }

        if (args.length == 2) {
            final List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .sorted(String::compareToIgnoreCase)
                    .toList();
            return filterByPrefix(players, args[1]);
        }

        if (args.length == 3) {
            final List<String> categories = new ArrayList<>(itemRegistry.listCategories());
            categories.add("enchantments_books");
            return filterByPrefix(categories, args[2]);
        }

        if (args.length == 4) {
            if (args[2].equalsIgnoreCase("enchantments_books") || args[2].equalsIgnoreCase("books")) {
                final List<String> enchantIds = enchantRegistry.all().stream()
                        .map(dev.cosmojar.stellaritypaper.enchants.EnchantDefinition::id)
                        .filter(id -> !id.startsWith("_technical/") && !id.startsWith("technical_"))
                        .sorted(String::compareToIgnoreCase)
                        .toList();
                return filterByPrefix(enchantIds, args[3]);
            }
            final Set<String> items = itemRegistry.listItemsByCategory(args[2]);
            return filterByPrefix(new ArrayList<>(items), args[3]);
        }

        if (args.length == 5) {
            if (args[2].equalsIgnoreCase("enchantments_books") || args[2].equalsIgnoreCase("books")) {
                final String enchantId = args[3];
                final var defOpt = enchantRegistry.findById(enchantId);
                final int maxTier = defOpt.map(d -> d.tiers().size()).orElse(1);
                final List<String> levels = new ArrayList<>();
                for (int i = 1; i <= maxTier; i++) {
                    levels.add(String.valueOf(i));
                }
                return filterByPrefix(levels, args[4]);
            }
            if (args[2].equalsIgnoreCase("armor")) {
                return filterByPrefix(List.of("-material"), args[4]);
            }
        }

        if (args.length == 6 && args[2].equalsIgnoreCase("armor") && args[4].equalsIgnoreCase("-material")) {
            return filterByPrefix(List.of("chainmail", "iron", "gold", "diamond", "netherite"), args[5]);
        }

        return List.of();
    }

    private List<String> enchantTabComplete(final String[] args) {
        if (args.length == 2) {
            return filterByPrefix(List.of("list", "apply", "remove", "clear"), args[1]);
        }
        if (args.length == 3) {
            final List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .sorted(String::compareToIgnoreCase)
                    .toList();
            return filterByPrefix(players, args[2]);
        }
        if (args.length == 4 && (args[1].equalsIgnoreCase("apply") || args[1].equalsIgnoreCase("remove"))) {
            final List<String> ids = enchantRegistry.all().stream()
                    .map(dev.cosmojar.stellaritypaper.enchants.EnchantDefinition::id)
                    .filter(id -> !id.startsWith("_technical/") && !id.startsWith("technical_"))
                    .sorted(String::compareToIgnoreCase)
                    .toList();
            return filterByPrefix(ids, args[3]);
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("apply")) {
            final String enchantId = args[3];
            final var defOpt = enchantRegistry.findById(enchantId);
            final int maxTier = defOpt.map(d -> d.tiers().size()).orElse(1);
            final List<String> levels = new ArrayList<>();
            for (int i = 1; i <= maxTier; i++) {
                levels.add(String.valueOf(i));
            }
            return filterByPrefix(levels, args[4]);
        }
        return List.of();
    }

    private List<String> filterByPrefix(final List<String> values, final String rawPrefix) {
        final String prefix = rawPrefix == null ? "" : rawPrefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList());
    }
}
