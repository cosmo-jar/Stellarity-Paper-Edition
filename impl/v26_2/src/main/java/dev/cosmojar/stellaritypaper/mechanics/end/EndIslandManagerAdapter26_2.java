package dev.cosmojar.stellaritypaper.mechanics.end;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderDragon;

import java.util.Collections;

public final class EndIslandManagerAdapter26_2 {

    public void setExitPortalLocation(World world, int x, int y, int z) {
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle != null) {
            try {
                java.lang.reflect.Method setPortal = battle.getClass().getMethod("setEndPortalLocation", Location.class);
                if (x == 0 && y == 0 && z == 0) {
                    setPortal.invoke(battle, (Location) null);
                } else {
                    setPortal.invoke(battle, new Location(world, x, y, z));
                }
            } catch (Exception ignored) {}
        }
    }

    public void resetDragonFight(World world) {
        try {
            DragonBattle battle = world.getEnderDragonBattle();
            if (battle != null) {
                battle.setPreviouslyKilled(false);
                try {
                    java.lang.reflect.Method setPortal = battle.getClass().getMethod("setEndPortalLocation", Location.class);
                    setPortal.invoke(battle, (Location) null);
                } catch (Throwable ignored) {}
                boolean initiated = false;
                try {
                    initiated = battle.initiateRespawn(Collections.emptyList());
                } catch (Throwable ignored) {}

                EnderDragon dragon = battle.getEnderDragon();
                if (dragon == null && !initiated) {
                    Location spawnLoc = new Location(world, 0.5, 100.0, 0.5);
                    dragon = world.spawn(spawnLoc, EnderDragon.class);
                    dragon.setPhase(EnderDragon.Phase.CIRCLING);
                }
            } else {
                Location spawnLoc = new Location(world, 0.5, 100.0, 0.5);
                EnderDragon dragon = world.spawn(spawnLoc, EnderDragon.class);
                dragon.setPhase(EnderDragon.Phase.CIRCLING);
            }
        } catch (Throwable ignored) {}
    }

    public Boolean getSendCommandFeedback(World world) {
        org.bukkit.GameRule<Boolean> rule = org.bukkit.GameRules.SEND_COMMAND_FEEDBACK;
        return rule != null ? world.getGameRuleValue(rule) : null;
    }

    public void setSendCommandFeedback(World world, Boolean value) {
        org.bukkit.GameRule<Boolean> rule = org.bukkit.GameRules.SEND_COMMAND_FEEDBACK;
        if (rule != null && value != null) {
            world.setGameRule(rule, value);
        }
    }

    public void resetDragonFightForUninstall(World world) {
        if (world == null) return;
        try {
            DragonBattle battle = world.getEnderDragonBattle();
            if (battle != null) {
                try { battle.setPreviouslyKilled(false); } catch (Throwable ignored) {}
                try {
                    java.lang.reflect.Method setPortal = battle.getClass().getMethod("setEndPortalLocation", Location.class);
                    setPortal.invoke(battle, (Location) null);
                } catch (Throwable ignored) {}
            }
            Object craftWorld = world.getClass().getMethod("getHandle").invoke(world);
            Object dragonFight = craftWorld.getClass().getMethod("getDragonFight").invoke(craftWorld);
            if (dragonFight != null) {
                for (java.lang.reflect.Field f : dragonFight.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    String name = f.getName().toLowerCase(java.util.Locale.ROOT);
                    if (name.contains("portal") && f.getType().getName().endsWith("BlockPos")) {
                        f.set(dragonFight, null);
                    } else if (name.equals("needsstatescanning") || name.equals("needsstatefix")) {
                        f.setBoolean(dragonFight, true);
                    } else if (name.equals("dragonkilled") || name.equals("haspreviouslykilleddragon") || name.equals("previouslykilled")) {
                        f.setBoolean(dragonFight, false);
                    } else if (name.equals("dragonuuid") || name.equals("respawnstage")) {
                        f.set(dragonFight, null);
                    }
                }
                try {
                    java.lang.reflect.Method setDirty = dragonFight.getClass().getMethod("setDirty");
                    setDirty.invoke(dragonFight);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public java.util.List<java.io.File> getExtraFilesToDeleteOnUninstall(World world) {
        java.util.List<java.io.File> list = new java.util.ArrayList<>();
        if (world == null) return list;
        java.io.File worldFolder = world.getWorldFolder();
        if (worldFolder != null && worldFolder.exists()) {
            collectDragonSavedData(new java.io.File(worldFolder, "data"), list);
            collectDragonSavedData(new java.io.File(new java.io.File(worldFolder, "DIM1"), "data"), list);
        }
        return list;
    }

    private static void collectDragonSavedData(java.io.File dir, java.util.List<java.io.File> list) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        for (java.io.File file : files) {
            String name = file.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.contains("dragon") && name.contains("fight") && (name.endsWith(".dat") || name.endsWith(".dat_old"))) {
                list.add(file);
            }
        }
    }
}
