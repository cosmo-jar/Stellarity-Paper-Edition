package dev.cosmojar.stellaritypaper.api;

import dev.cosmojar.stellaritypaper.api.exception.UnsupportedServerVersionException;

import java.util.ServiceLoader;

/**
 * Менеджер управления и обнаружения версионных адаптеров без зависимости от Bukkit (ну или Paper) API.
 */
public final class VersionManager {

    private static VersionAdapter currentAdapter;
    private static ServerVersion currentServerVersion;

    private VersionManager() {
    }

    /**
     * Инициализирует менеджер и единоразово находит совместимый адаптер при старте плагина.
     * @param serverVersion Текущая версия сервера
     * @throws UnsupportedServerVersionException Если адаптер под данную версию не обнаружен
     */
    public static synchronized void initialize(ServerVersion serverVersion) throws UnsupportedServerVersionException {
        currentServerVersion = serverVersion;
        final ServiceLoader<VersionAdapter> loader = ServiceLoader.load(VersionAdapter.class, VersionManager.class.getClassLoader());

        for (final VersionAdapter adapter : loader) {
            if (adapter != null && adapter.supports(serverVersion)) {
                currentAdapter = adapter;
                return;
            }
        }

        throw new UnsupportedServerVersionException(serverVersion);
    }

    /**
     * Прямая регистрация резервного адаптера (на будущее).
     */
    public static synchronized void registerAdapter(VersionAdapter adapter) {
        if (adapter == null) {
            return;
        }
        if (currentServerVersion != null && adapter.supports(currentServerVersion)) {
            currentAdapter = adapter;
        } else if (currentAdapter == null) {
            currentAdapter = adapter;
        }
    }

    /**
     * Возвращает закэшированный экземпляр адаптера версий без нагрузки. O(1).
     */
    public static VersionAdapter getAdapter() {
        return currentAdapter;
    }

    public static ServerVersion getServerVersion() {
        return currentServerVersion;
    }

    public static boolean isInitialized() {
        return currentAdapter != null;
    }
}
