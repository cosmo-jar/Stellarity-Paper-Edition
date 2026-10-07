package dev.cosmojar.stellaritypaper.api;


public interface VersionAdapter {

    /**
     * Контракт выбора: вызывается строго 1 раз в VersionManager при загрузке плагина в onEnable.
     * @param version Текущая версия сервера
     * @return true, если данный адаптер поддерживают такую версию
     */
    boolean supports(ServerVersion version);

    /**
     * Возвращает наименование и информацию об адаптере версий.
     */
    String getAdapterName();

    /**
     * Регистрирует NMS зачарования для Void Pendant.
     */
    default void registerVoidPendantEnchantments() {}

    /**
     * Применяет нативный вариант мобов энда для версий ниже 1.21.11 (NMS 1.21.4 - 1.21.10).
     */
    default void applyNativeEndVariant(org.bukkit.entity.Entity entity) {}

    /**
     * Возвращает материал для Шалкерового копья (для версий ниже 1.21.11 подменяется на NETHERITE_SWORD).
     */
    default org.bukkit.Material getShulkerSpearMaterial() {
        return org.bukkit.Material.NETHERITE_SWORD;
    }

    /**
     * Получает значение GameRule sendCommandFeedback (для версий ниже 1.21.11 через GameRule.SEND_COMMAND_FEEDBACK).
     */
    default Boolean getSendCommandFeedback(org.bukkit.World world) {
        return null;
    }

    /**
     * Устанавливает значение GameRule sendCommandFeedback (для версий ниже 1.21.11 через GameRule.SEND_COMMAND_FEEDBACK).
     */
    default void setSendCommandFeedback(org.bukkit.World world, Boolean value) {}

    /**
     * Регистрирует версионно-специфичные правила освящения блоков (например Material.SULFUR_SPIKE на 26.2).
     */
    default void registerConsecrationRules(Object registry) {}

    /**
     * Очищает теги NBT в level.dat при анинсталле.
     */
    default void fixLevelDat(java.io.File levelDatFile) throws Exception {}

    /**
     * Очищает кастомные теги NBT в NMS CompoundTag чанка.
     */
    default boolean cleanChunkTag(Object chunkTag) { return false; }

    /**
     * Устанавливает координаты выходного портала Энда в бою с драконом.
     */
    default void setExitPortalLocation(org.bukkit.World world, int x, int y, int z) {}

    /**
     * Сбрасывает бой с драконом.
     */
    default void resetDragonFight(org.bukkit.World world) {}

    /**
     * Сбрасывает бой с драконом перед деинсталляцией плагина (для версий 26.X очищает NMS EnderDragonFight).
     * @param endWorld Мир с измерением THE_END
     */
    default void resetDragonFightForUninstall(org.bukkit.World endWorld) {}

    /**
     * Возвращает список дополнительных файлов данных для физического удаления при деинсталляции
     * (например, SavedData ender_dragon_fight.dat для версий 26.X).
     * @param endWorld Мир с измерением THE_END
     */
    default java.util.List<java.io.File> getExtraFilesToDeleteOnUninstall(org.bukkit.World endWorld) {
        return java.util.Collections.emptyList();
    }

    /**
     * Привязывает UUID дракона к NMS EndDragonFight (для версий 1.21.4 - 1.21.8).
     */
    default void bindDragonToFight(org.bukkit.World world, org.bukkit.entity.EnderDragon dragon) {}

    /**
     * Возвращает true, если текущая версия сервера поддерживает диалоги (начиная с 1.21.6).
     */
    default boolean supportsDialogs() {
        return true;
    }

    /**
     * Возвращает true, если версия использует собственную версионную реализацию диалоговых окон (от 1.21.6).
     */
    default boolean hasCustomDialogAdapter() {
        return false;
    }

    /**
     * Методы открытия страниц Эндономикона через кастомный версионный адаптер (например 1.21.6).
     */
    default boolean openEndonomiconMainMenu(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, int favCount, int recCount) { return false; }
    default boolean openEndonomiconRecipeDetail(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, Object vm, boolean isFavorite) { return false; }
    default boolean openEndonomiconCategoryView(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, String category, Object matchesList, int page, int totalPages) { return false; }
    default boolean openEndonomiconFavoritesView(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, Object matchesList, int page, int totalPages) { return false; }
    default boolean openEndonomiconRecentView(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, Object matchesList, int page, int totalPages) { return false; }
    default boolean openEndonomiconSearchResults(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, Object searchResult, int page, int totalPages) { return false; }
    default boolean openEndonomiconSearchPrompt(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin) { return false; }
}
