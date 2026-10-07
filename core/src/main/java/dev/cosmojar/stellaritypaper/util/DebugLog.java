package dev.cosmojar.stellaritypaper.util;

import org.bukkit.plugin.Plugin;

public final class DebugLog {
    
    private DebugLog() {}


    public static void log(Plugin plugin, String message) {
        if (plugin != null && plugin.getConfig().getBoolean("debug-log", false)) {
            if (!message.startsWith("[DEBUG]")) {
                plugin.getLogger().info("[DEBUG] " + message);
            } else {
                plugin.getLogger().info(message);
            }
        }
    }
}
