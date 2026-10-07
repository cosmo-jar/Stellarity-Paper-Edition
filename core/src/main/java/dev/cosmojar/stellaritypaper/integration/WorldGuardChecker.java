package dev.cosmojar.stellaritypaper.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WorldGuardChecker {
    public static boolean canBuild(final Player player, final Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }
        final LocalPlayer localPlayer = player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null;
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionQuery query = container.createQuery();
        return query.testState(BukkitAdapter.adapt(location), localPlayer, com.sk89q.worldguard.protection.flags.Flags.BUILD);
    }

    public static boolean canTakeMobDamage(final Player player, final Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }
        final LocalPlayer localPlayer = player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null;
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionQuery query = container.createQuery();
        final com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);

        if (query.testState(weLoc, localPlayer, com.sk89q.worldguard.protection.flags.Flags.INVINCIBILITY)) {
            return false;
        }

        return query.testState(weLoc, localPlayer, com.sk89q.worldguard.protection.flags.Flags.MOB_DAMAGE);
    }

    public static boolean isMobSpawningAllowed(final Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionQuery query = container.createQuery();
        final com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
        return query.testState(weLoc, (LocalPlayer) null, com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING);
    }

}
