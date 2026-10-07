package dev.cosmojar.stellaritypaper.items.endonomicon;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class EndonomiconUserDataRepository {

    private final NamespacedKey favoritesKey;
    private final NamespacedKey recentKey;

    public EndonomiconUserDataRepository(JavaPlugin plugin) {
        this.favoritesKey = new NamespacedKey(plugin, "endonomicon_favorites");
        this.recentKey = new NamespacedKey(plugin, "endonomicon_recent");
    }

    public Set<String> getFavorites(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        String raw = pdc.get(favoritesKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(Arrays.asList(raw.split(",")));
    }

    public void addFavorite(Player player, String recipeId) {
        Set<String> set = getFavorites(player);
        set.add(recipeId);
        saveFavorites(player, set);
    }

    public void removeFavorite(Player player, String recipeId) {
        Set<String> set = getFavorites(player);
        set.remove(recipeId);
        saveFavorites(player, set);
    }

    public boolean isFavorite(Player player, String recipeId) {
        return getFavorites(player).contains(recipeId);
    }

    private void saveFavorites(Player player, Set<String> set) {
        String value = String.join(",", set);
        player.getPersistentDataContainer().set(favoritesKey, PersistentDataType.STRING, value);
    }

    public Deque<String> getRecentViewed(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        String raw = pdc.get(recentKey, PersistentDataType.STRING);
        Deque<String> deque = new ArrayDeque<>();
        if (raw != null && !raw.isBlank()) {
            for (String s : raw.split(",")) {
                if (!s.isBlank()) deque.add(s);
            }
        }
        return deque;
    }

    public void addRecentViewed(Player player, String recipeId) {
        Deque<String> deque = getRecentViewed(player);
        deque.remove(recipeId);
        deque.addFirst(recipeId);
        while (deque.size() > 10) {
            deque.removeLast();
        }
        String value = String.join(",", deque);
        player.getPersistentDataContainer().set(recentKey, PersistentDataType.STRING, value);
    }

    public void cleanupOrphans(Player player, Set<String> validRecipeIds) {
        Set<String> favs = getFavorites(player);
        if (favs.removeIf(id -> !validRecipeIds.contains(id))) {
            saveFavorites(player, favs);
        }

        Deque<String> recent = getRecentViewed(player);
        if (recent.removeIf(id -> !validRecipeIds.contains(id))) {
            String value = String.join(",", recent);
            player.getPersistentDataContainer().set(recentKey, PersistentDataType.STRING, value);
        }
    }
}
