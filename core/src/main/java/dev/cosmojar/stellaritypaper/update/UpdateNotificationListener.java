package dev.cosmojar.stellaritypaper.update;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class UpdateNotificationListener implements Listener {

    private final StellarityPaperPlugin plugin;
    private final UpdateCheckerService updateCheckerService;

    public UpdateNotificationListener(
            final StellarityPaperPlugin plugin,
            final UpdateCheckerService updateCheckerService
    ) {
        this.plugin = plugin;
        this.updateCheckerService = updateCheckerService;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!player.hasPermission(UpdateCheckerService.PERMISSION)) {
            return;
        }

        if (!updateCheckerService.hasUpdate()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateCheckerService.notifyPlayer(player);
            }
        }, 40L);
    }
}
