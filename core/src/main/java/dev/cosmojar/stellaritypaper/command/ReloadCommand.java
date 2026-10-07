package dev.cosmojar.stellaritypaper.command;

import dev.cosmojar.stellaritypaper.config.ConfigService;
import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.command.CommandSender;

public final class ReloadCommand {

    private final ConfigService configService;
    private final ItemsConfigService itemsConfigService;
    private final MessageService messageService;
    private final dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService customLootService;

    public ReloadCommand(
            final ConfigService configService,
            final ItemsConfigService itemsConfigService,
            final MessageService messageService,
            final dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService customLootService
    ) {
        this.configService = configService;
        this.itemsConfigService = itemsConfigService;
        this.messageService = messageService;
        this.customLootService = customLootService;
    }

    public void execute(final CommandSender sender) {
        configService.reload();
        itemsConfigService.load();
        messageService.reload();
        if (customLootService != null) {
            customLootService.reload();
        }
        sender.sendMessage(messageService.message("command.reload.success"));
    }
}
