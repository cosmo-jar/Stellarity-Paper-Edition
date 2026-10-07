package dev.cosmojar.stellaritypaper.mechanics.painting;

public enum StellarityPainting {

    A_HOP_AND_A_SKIP_AWAY("a_hop_and_a_skip_away", 4, 4),
    DRAGONBLADE("dragonblade", 2, 2),
    END("end", 5, 5),
    END_BLOSSOM("end_blossom", 4, 4),
    HOURGLASS("hourglass", 2, 1),
    MAJESTICAL_BREW("majestical_brew", 3, 3),
    SCHEME("scheme", 3, 3),
    SHEPHERDS_FEAST("shepherds_feast", 2, 2),
    SNARE("snare", 1, 1),
    SNATCH("snatch", 3, 2),
    THE_OBSIDIAN_RELIQUARY("the_obsidian_reliquary", 2, 2);

    private final String id;
    private final int width;
    private final int height;
    private final String titleKey;
    private final String authorKey;

    StellarityPainting(final String id, final int width, final int height) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.titleKey = "painting.stellarity." + id + ".title";
        this.authorKey = "painting.stellarity." + id + ".author";
    }

    public String getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getAuthorKey() {
        return authorKey;
    }
}
