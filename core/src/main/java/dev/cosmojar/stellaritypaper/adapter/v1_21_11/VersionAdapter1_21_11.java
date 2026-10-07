package dev.cosmojar.stellaritypaper.adapter.v1_21_11;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;

/**
 * Просто нужен и всё.
 */
public class VersionAdapter1_21_11 implements VersionAdapter {

    @Override
    public boolean supports(ServerVersion version) {
        return version == ServerVersion.V1_21_11 || version == ServerVersion.UNKNOWN;
    }

    @Override
    public String getAdapterName() {
        return "Paper-1.21.11-Adapter";
    }
}
