package dev.cosmojar.stellaritypaper.mechanics.end;

import org.bukkit.GameRule;
import org.bukkit.World;

/**
 * Адаптер вызовов EndIslandManager под версию Minecraft 1.21.4.
 */
public class EndIslandManagerAdapter1_21_4 {

    public Boolean getSendCommandFeedback(World world) {
        GameRule<Boolean> rule = GameRule.SEND_COMMAND_FEEDBACK;
        return rule != null ? world.getGameRuleValue(rule) : null;
    }

    public void setSendCommandFeedback(World world, Boolean value) {
        GameRule<Boolean> rule = GameRule.SEND_COMMAND_FEEDBACK;
        if (rule != null && value != null) {
            world.setGameRule(rule, value);
        }
    }

    public void bindDragonToFight(World world, org.bukkit.entity.EnderDragon bukkitDragon) {
        if (world == null || bukkitDragon == null) return;
        try {
            Object craftWorld = world.getClass().getMethod("getHandle").invoke(world);
            Object dragonFight = craftWorld.getClass().getMethod("getDragonFight").invoke(craftWorld);
            if (dragonFight != null) {
                try {
                    java.lang.reflect.Field field = dragonFight.getClass().getDeclaredField("dragonKilled");
                    field.setAccessible(true);
                    field.set(dragonFight, false);
                } catch (NoSuchFieldException e) {
                    for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                        if (field.getType() == boolean.class) {
                            field.setAccessible(true);
                            field.set(dragonFight, false);
                        }
                    }
                }

                java.util.UUID uuid = bukkitDragon.getUniqueId();
                try {
                    java.lang.reflect.Field uuidField = dragonFight.getClass().getDeclaredField("dragonUUID");
                    uuidField.setAccessible(true);
                    uuidField.set(dragonFight, uuid);
                } catch (NoSuchFieldException e) {
                    for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                        if (field.getType() == java.util.UUID.class) {
                            field.setAccessible(true);
                            field.set(dragonFight, uuid);
                        }
                    }
                }

                Object nmsDragon = bukkitDragon.getClass().getMethod("getHandle").invoke(bukkitDragon);
                for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                    if (field.getType().isAssignableFrom(nmsDragon.getClass()) || field.getType().getName().endsWith("EnderDragon")) {
                        field.setAccessible(true);
                        field.set(dragonFight, nmsDragon);
                    }
                }

                for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                    if (field.getType().isEnum() && (field.getType().getName().contains("SpawnState") || field.getType().getName().contains("Respawn"))) {
                        field.setAccessible(true);
                        field.set(dragonFight, null);
                    }
                }

                for (java.lang.reflect.Field field : dragonFight.getClass().getDeclaredFields()) {
                    if (field.getType().getName().contains("Boss") || field.getType().getName().contains("ServerBossEvent")) {
                        field.setAccessible(true);
                        Object bossEvent = field.get(dragonFight);
                        if (bossEvent != null) {
                            bossEvent.getClass().getMethod("setVisible", boolean.class).invoke(bossEvent, true);
                        }
                    }
                }

                try {
                    for (java.lang.reflect.Method m : dragonFight.getClass().getDeclaredMethods()) {
                        if (m.getName().equals("setUnsaved") || m.getName().equals("saveData") || m.getName().equals("setDirty")) {
                            m.setAccessible(true);
                            m.invoke(dragonFight);
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
