package dev.cosmojar.stellaritypaper.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WorldGuardHook {
    private static boolean enabled = false;

    public static void init() {
        enabled = org.bukkit.Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean canBuild(final Player player, final Location location) {
        if (player.isOp()) {
            return true;
        }
        if (!enabled) {
            return true;
        }
        try {
            return WorldGuardChecker.canBuild(player, location);
        } catch (final Throwable t) {
            return true;
        }
    }

    public static boolean canTakeMobDamage(final Player player, final Location location) {
        if (!enabled) {
            return true;
        }
        try {
            return WorldGuardChecker.canTakeMobDamage(player, location);
        } catch (final Throwable t) {
            return true;
        }
    }

    public static boolean isMobSpawningAllowed(final Location location) {
        if (!enabled) {
            return true;
        }
        try {
            return WorldGuardChecker.isMobSpawningAllowed(location);
        } catch (final Throwable t) {
            return true;
        }
    }

}
