package dev.cosmojar.stellaritypaper.core;

import java.util.HashMap;
import java.util.Map;

public final class ServiceContainer {

    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void register(final Class<T> type, final T instance) {
        services.put(type, instance);
    }

    public <T> T require(final Class<T> type) {
        final Object service = services.get(type);
        if (service == null) {
            throw new IllegalStateException("Service not found: " + type.getName());
        }
        return type.cast(service);
    }
}
