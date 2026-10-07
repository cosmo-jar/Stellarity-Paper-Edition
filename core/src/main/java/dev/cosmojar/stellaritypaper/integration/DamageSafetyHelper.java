package dev.cosmojar.stellaritypaper.integration;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public final class DamageSafetyHelper {

    private static final String[] GOD_METADATA_KEYS = {
            "god", "godmode", "invulnerable", "cmi_god", "essentials_god", "admin_god"
    };

    private DamageSafetyHelper() {}

    /**
     * Checks whether the player can be damaged by a boss or mob.
     * Returns false if the player is in Creative/Spectator, is invulnerable,
     * has god mode active (Essentials, metadata, etc.), or is protected by
     * WorldGuard (mob-damage deny, invincibility allow).
     *
     * @param player the victim player
     * @param damager the damager entity (e.g. boss), can be null
     * @return true if the player can be damaged, false if they are immune
     */
    public static boolean canDamage(final Player player, final Entity damager) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return false;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        if (player.isInvulnerable()) {
            return false;
        }

        if (hasGodMetadata(player)) {
            return false;
        }

        if (isEssentialsGodMode(player)) {
            return false;
        }

        final Location loc = player.getLocation();
        if (WorldGuardHook.isEnabled() && !WorldGuardHook.canTakeMobDamage(player, loc)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the player has any god-mode metadata flag set to true.
     */
    public static boolean hasGodMetadata(final Player player) {
        for (final String key : GOD_METADATA_KEYS) {
            if (player.hasMetadata(key)) {
                for (final org.bukkit.metadata.MetadataValue meta : player.getMetadata(key)) {
                    if (meta.asBoolean()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if Essentials / EssentialsX has god mode enabled for the player.
     */
    public static boolean isEssentialsGodMode(final Player player) {
        try {
            final Plugin essPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (essPlugin == null || !essPlugin.isEnabled()) {
                return false;
            }
            final Method getUserMethod = essPlugin.getClass().getMethod("getUser", Player.class);
            final Object user = getUserMethod.invoke(essPlugin, player);
            if (user != null) {
                final Method isGodMethod = user.getClass().getMethod("isGodModeEnabled");
                final Object result = isGodMethod.invoke(user);
                if (result instanceof Boolean b && b) {
                    return true;
                }
            }
        } catch (final Throwable ignored) {
        }
        return false;
    }
}
