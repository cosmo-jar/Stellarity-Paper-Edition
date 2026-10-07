package dev.cosmojar.stellaritypaper.text;

import net.kyori.adventure.text.Component;

public final class TextService {

    private final MiniMessageService miniMessageService;

    public TextService(final MiniMessageService miniMessageService) {
        this.miniMessageService = miniMessageService;
    }

    public Component mm(final String miniMessage) {
        return miniMessageService.deserialize(miniMessage);
    }

    public Component tr(final String translationKey) {
        return Component.translatable(translationKey);
    }
}
