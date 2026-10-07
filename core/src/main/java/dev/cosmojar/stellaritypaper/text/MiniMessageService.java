package dev.cosmojar.stellaritypaper.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class MiniMessageService {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public Component deserialize(final String input) {
        return miniMessage.deserialize(input);
    }

    public String serialize(final Component component) {
        return miniMessage.serialize(component);
    }
}
