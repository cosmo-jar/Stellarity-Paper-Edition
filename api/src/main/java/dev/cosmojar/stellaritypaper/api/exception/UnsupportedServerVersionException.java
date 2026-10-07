package dev.cosmojar.stellaritypaper.api.exception;

import dev.cosmojar.stellaritypaper.api.ServerVersion;

public class UnsupportedServerVersionException extends Exception {

    private final ServerVersion version;

    public UnsupportedServerVersionException(ServerVersion version) {
        super("Server version '" + (version != null ? version.getVersionString() : "unknown") + "' not supported.");
        this.version = version;
    }

    public ServerVersion getVersion() {
        return version;
    }
}
