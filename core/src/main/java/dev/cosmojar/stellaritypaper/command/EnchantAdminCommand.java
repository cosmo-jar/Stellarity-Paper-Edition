package dev.cosmojar.stellaritypaper.command;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.enchants.EnchantBehaviorRegistry;
import dev.cosmojar.stellaritypaper.enchants.EnchantDataCodec;
import dev.cosmojar.stellaritypaper.enchants.EnchantDefinition;
import dev.cosmojar.stellaritypaper.enchants.EnchantDefinitionRegistry;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.enchants.EnchantItemService;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EnchantAdminCommand {

    private final EnchantDefinitionRegistry enchantRegistry;
    private final EnchantBehaviorRegistry enchantBehaviorRegistry;
    private final EnchantDataCodec codec;
    private final ItemStateRepository itemStateRepository;
    private final EnchantItemService enchantItemService;
    private final MessageService messageService;

    public EnchantAdminCommand(
            final EnchantDefinitionRegistry enchantRegistry,
            final EnchantBehaviorRegistry enchantBehaviorRegistry,
            final EnchantDataCodec codec,
            final ItemStateRepository itemStateRepository,
            final EnchantItemService enchantItemService,
            final MessageService messageService
    ) {
        this.enchantRegistry = enchantRegistry;
        this.enchantBehaviorRegistry = enchantBehaviorRegistry;
        this.codec = codec;
        this.itemStateRepository = itemStateRepository;
        this.enchantItemService = enchantItemService;
        this.messageService = messageService;
    }

    public void execute(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messageService.message("command.stellarity.usage-enchant"));
            return;
        }

        final String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "list" -> list(sender);
            case "apply" -> apply(sender, args);
            case "remove" -> remove(sender, args);
            case "clear" -> clear(sender, args);
            default -> sender.sendMessage(messageService.message("command.stellarity.usage-enchant"));
        }
    }

    private void list(final CommandSender sender) {
        final String ids = enchantRegistry.all().stream()
                .sorted(Comparator.comparing(EnchantDefinition::id))
                .map(def -> def.id() + "(max:" + def.tiers().size() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");
        sender.sendMessage(messageService.message("command.enchant.list", Map.of("enchants", ids)));
    }

    private void apply(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sender.sendMessage(messageService.message("command.stellarity.usage-enchant"));
            return;
        }
        final Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(messageService.message("command.give.player-not-found", Map.of("player", args[1])));
            return;
        }

        final String id = normalizeEnchantId(args[2]);
        final Optional<EnchantDefinition> definition = enchantRegistry.findById(id);
        if (definition.isEmpty()) {
            sender.sendMessage(messageService.message("command.enchant.unknown-enchant", Map.of("enchant", args[2])));
            return;
        }

        final int level = parseLevel(args.length >= 4 ? args[3] : "1", definition.get().tiers().size());
        if (level <= 0) {
            sender.sendMessage(messageService.message("command.enchant.invalid-level"));
            return;
        }

        final ItemStack mainHand = target.getInventory().getItemInMainHand();
        if (!enchantBehaviorRegistry.canApplyToItem(id, mainHand)) {
            sender.sendMessage(messageService.message("command.enchant.incompatible-item", Map.of("enchant", id)));
            return;
        }

        final List<EnchantInstance> values = new ArrayList<>(codec.decode(itemStateRepository.getEnchantsData(mainHand).orElse("")));
        values.removeIf(instance -> instance.id().equalsIgnoreCase(id));
        values.add(new EnchantInstance(id, level));
        itemStateRepository.setEnchantsData(mainHand, codec.encode(values));
        itemStateRepository.setEnchantsSchema(mainHand, 0);
        enchantItemService.rebuildNow(mainHand);
        sender.sendMessage(messageService.message(
                "command.enchant.apply-success",
                Map.of("player", target.getName(), "enchant", id, "level", Integer.toString(level))
        ));
    }

    private void remove(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sender.sendMessage(messageService.message("command.stellarity.usage-enchant"));
            return;
        }
        final Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(messageService.message("command.give.player-not-found", Map.of("player", args[1])));
            return;
        }
        final String id = normalizeEnchantId(args[2]);
        final ItemStack mainHand = target.getInventory().getItemInMainHand();
        final List<EnchantInstance> values = new ArrayList<>(codec.decode(itemStateRepository.getEnchantsData(mainHand).orElse("")));
        final boolean removed = values.removeIf(instance -> instance.id().equalsIgnoreCase(id));
        if (!removed) {
            sender.sendMessage(messageService.message("command.enchant.not-applied", Map.of("enchant", id)));
            return;
        }

        itemStateRepository.setEnchantsData(mainHand, codec.encode(values));
        itemStateRepository.setEnchantsSchema(mainHand, 0);
        enchantItemService.rebuildNow(mainHand);
        sender.sendMessage(messageService.message(
                "command.enchant.remove-success",
                Map.of("player", target.getName(), "enchant", id)
        ));
    }

    private void clear(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage(messageService.message("command.stellarity.usage-enchant"));
            return;
        }
        final Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(messageService.message("command.give.player-not-found", Map.of("player", args[1])));
            return;
        }
        final ItemStack mainHand = target.getInventory().getItemInMainHand();
        itemStateRepository.setEnchantsData(mainHand, "[]");
        itemStateRepository.setEnchantsSchema(mainHand, 0);
        enchantItemService.rebuildNow(mainHand);
        sender.sendMessage(messageService.message(
                "command.enchant.clear-success",
                Map.of("player", target.getName())
        ));
    }

    private String normalizeEnchantId(final String raw) {
        final String normalized = raw.toLowerCase(Locale.ROOT);
        final String withoutNamespace = normalized.startsWith("stellarity:")
                ? normalized.substring("stellarity:".length())
                : normalized;
        return switch (withoutNamespace) {
            case "_technical/daybroken" -> "technical_daybroken";
            case "_technical/draconic" -> "technical_draconic";
            case "_technical/infernal_infusion" -> "technical_infernal_infusion";
            case "_technical/mighty_wind" -> "technical_mighty_wind";
            case "_technical/soul_harvest" -> "technical_soul_harvest";
            case "_technical/void_pendant/amethyst", "technical_void_pendant_amethyst" -> "_technical/void_pendant/amethyst";
            case "_technical/void_pendant/copper", "technical_void_pendant_copper" -> "_technical/void_pendant/copper";
            case "_technical/void_pendant/diamond", "technical_void_pendant_diamond" -> "_technical/void_pendant/diamond";
            case "_technical/void_pendant/emerald", "technical_void_pendant_emerald" -> "_technical/void_pendant/emerald";
            case "_technical/void_pendant/gold", "technical_void_pendant_gold" -> "_technical/void_pendant/gold";
            case "_technical/void_pendant/iron", "technical_void_pendant_iron" -> "_technical/void_pendant/iron";
            case "_technical/void_pendant/lapis", "technical_void_pendant_lapis" -> "_technical/void_pendant/lapis";
            case "_technical/void_pendant/netherite", "technical_void_pendant_netherite" -> "_technical/void_pendant/netherite";
            case "_technical/void_pendant/quartz", "technical_void_pendant_quartz" -> "_technical/void_pendant/quartz";
            default -> withoutNamespace;
        };
    }

    private int parseLevel(final String raw, final int max) {
        try {
            final int value = Integer.parseInt(raw);
            return value < 1 || value > max ? -1 : value;
        } catch (final NumberFormatException ignored) {
            return -1;
        }
    }
}
