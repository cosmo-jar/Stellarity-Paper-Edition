package dev.cosmojar.stellaritypaper.bootstrap;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public final class StartupBanner {

    private static final String[] BANNER_LINES = {
            "  ▄▄▄▄▄           ▄▄   ▄▄                        ",
            " ██▀▀▀▀█▄ █▄       ██   ██               █▄      ",
            " ▀██▄  ▄▀▄██▄      ██   ██       ▄    ▀▀▄██▄     ",
            "   ▀██▄▄  ██ ▄█▀█▄ ██   ██ ▄▀▀█▄ ████▄██ ██ ██ ██",
            " ▄   ▀██▄ ██ ██▄█▀ ██   ██ ▄█▀██ ██   ██ ██ ██▄██",
            " ▀██████▀▄██▄▀█▄▄▄▄██▄ ▄██▄▀█▄██▄█▀  ▄██▄██▄▄▀██▀",
            "                                             ██  ",
            "                                           ▀▀▀   "
    };

    private static final String[][] GRADIENTS = {
            {"#7C3AED", "#C084FC"},
            {"#8B5CF6", "#D946EF"},
            {"#9333EA", "#E879F9"},
            {"#A855F7", "#F472B6"},
            {"#A855F7", "#FB7185"},
            {"#C084FC", "#FDA4AF"},
            {"#E879F9", "#FECDD3"},
            {"#F472B6", "#FFE4E6"}
    };

    private StartupBanner() {
    }

    public static void print(final String version) {
        final MiniMessage mm = MiniMessage.miniMessage();
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(""));
        for (int i = 0; i < BANNER_LINES.length; i++) {
            final String line = BANNER_LINES[i];
            final String[] gradient = GRADIENTS[i % GRADIENTS.length];
            Bukkit.getConsoleSender().sendMessage(
                    mm.deserialize("<gradient:" + gradient[0] + ":" + gradient[1] + ">" + line + "</gradient>")
            );
        }
        Bukkit.getConsoleSender().sendMessage(
                mm.deserialize("  <gradient:#A855F7:#F472B6>Stellarity ~ Paper Edition</gradient> <dark_gray>v" + version + "</dark_gray>")
        );
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(""));
    }
}
