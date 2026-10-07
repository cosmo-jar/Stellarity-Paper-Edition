package dev.cosmojar.stellaritypaper.update;

public record UpdateInfo(
        String newVersion,
        String currentVersion,
        String downloadUrl,
        String sourceName
) {
}
