package dev.cosmojar.stellaritypaper.api;

import org.bukkit.Bukkit;

public enum ServerVersion {
    V1_20_5("1.20.5"),
    V1_20_6("1.20.6"),
    V1_21("1.21"),
    V1_21_1("1.21.1"),
    V1_21_3("1.21.3"),
    V1_21_4("1.21.4"),
    V1_21_5("1.21.5"),
    V1_21_6("1.21.6"),
    V1_21_7("1.21.7"),
    V1_21_8("1.21.8"),
    V1_21_9("1.21.9"),
    V1_21_10("1.21.10"),
    V1_21_11("1.21.11"),
    V26_1_1("26.1.1"),
    V26_1_2("26.1.2"),
    V26_2("26.2"),
    V26_3("26.3"),
    UNKNOWN("unknown");

    private final String versionString;

    ServerVersion(String versionString) {
        this.versionString = versionString;
    }

    public String getVersionString() {
        return versionString;
    }

    public static ServerVersion getCurrent() {
        try {
            String bukkitVersion = Bukkit.getMinecraftVersion();
            if (bukkitVersion != null) {
                bukkitVersion = bukkitVersion.trim();
                for (ServerVersion sv : values()) {
                    if (sv != UNKNOWN && sv.versionString.equalsIgnoreCase(bukkitVersion)) {
                        return sv;
                    }
                }
                for (ServerVersion sv : values()) {
                    if (sv != UNKNOWN && bukkitVersion.startsWith(sv.versionString)) {
                        return sv;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return UNKNOWN;
    }
}
