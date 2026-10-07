package dev.cosmojar.stellaritypaper.mechanics.villager;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Слушатель, который добавляет кастомные торговые предложения жителям Энда
 * при каждом событии получения новой торговли ({@link VillagerAcquireTradeEvent}).
 *
 * <p><b>Идентификация жителя Энда:</b>
 * <ul>
 *   <li>Наличие тега scoreboard {@code stellarity.end_villager} — явная маркировка из датапака; <i>или</i></li>
 *   <li>Мир с {@link World.Environment#THE_END} — все жители, живущие в Энде.</li>
 * </ul>
 * </p>
 *
 * <p><b>Синхронизация уровня через PDC:</b>
 * Ключ {@code stellarity:end_trade_sync_level} хранит последний уровень, для которого
 * уже были добавлены кастомные торги. Это предотвращает дублирование при повторных
 * вызовах события на одном уровне (например, если ванилла вызывает его несколько раз).</p>
 *
 * <p><b>Принцип «не заменять, а дополнять»:</b>
 * Кастомные торги <em>добавляются после ванильных</em>, никогда не заменяют их.</p>
 */
public final class EndVillagerTradeService implements Listener {

    /** Тег scoreboard, который ставится на жителей-эндерцев датапаком или при спавне. */
    private static final String END_VILLAGER_TAG = "stellarity.end_villager";

    /**
     * PDC-ключ для хранения последнего уровня, на котором кастомные торги уже были добавлены.
     * Значение — {@code int}, соответствующий {@link Villager#getVillagerLevel()}.
     */
    private static final String PDC_SYNC_LEVEL_KEY = "end_trade_sync_level";

    private final Plugin plugin;
    private final EndVillagerTradeRegistry registry;
    private final MerchantRecipeFactory recipeFactory;
    private final NamespacedKey syncLevelKey;
    private final Logger logger;

    public EndVillagerTradeService(
            final Plugin plugin,
            final EndVillagerTradeRegistry registry,
            final MerchantRecipeFactory recipeFactory
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.recipeFactory = recipeFactory;
        this.syncLevelKey = new NamespacedKey(plugin, PDC_SYNC_LEVEL_KEY);
        this.logger = plugin.getLogger();
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVillagerAcquireTrade(final VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof final Villager villager)) {
            return;
        }
        if (!isEndVillager(villager)) {
            return;
        }

        final int currentLevel = villager.getVillagerLevel();
        final int lastSyncedLevel = getLastSyncedLevel(villager);

        boolean addedAny = false;
        for (int lvl = lastSyncedLevel + 1; lvl <= currentLevel; lvl++) {
            addedAny |= addTradesForLevel(villager, lvl);
        }

        if (addedAny) {
            setLastSyncedLevel(villager, currentLevel);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerReplenish(final VillagerReplenishTradeEvent event) {
        if (!(event.getEntity() instanceof final Villager villager)) {
            return;
        }
        if (!isEndVillager(villager)) {
            return;
        }

        final int currentLevel = villager.getVillagerLevel();
        final int lastSyncedLevel = getLastSyncedLevel(villager);

        if (lastSyncedLevel < currentLevel) {
            boolean addedAny = false;
            for (int lvl = lastSyncedLevel + 1; lvl <= currentLevel; lvl++) {
                addedAny |= addTradesForLevel(villager, lvl);
            }
            if (addedAny) {
                setLastSyncedLevel(villager, currentLevel);
            }
        }
    }


    private boolean isEndVillager(final Villager villager) {
        if (villager.getScoreboardTags().contains(END_VILLAGER_TAG)) {
            return true;
        }
        return villager.getWorld().getEnvironment() == World.Environment.THE_END;
    }


    private boolean addTradesForLevel(final Villager villager, final int level) {
        final List<TradeDefinition> defs = registry.getForLevel(villager.getProfession(), level);
        if (defs.isEmpty()) {
            return false;
        }

        final List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        boolean added = false;

        for (final TradeDefinition def : defs) {
            final MerchantRecipe recipe = recipeFactory.create(def);
            if (recipe == null) {
                continue;
            }
            if (!containsEquivalentRecipe(recipes, recipe)) {
                recipes.add(recipe);
                added = true;
            }
        }

        if (added) {

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (villager.isValid() && !villager.isDead()) {
                    villager.setRecipes(recipes);
                }
            });
        }
        return added;
    }

    private boolean containsEquivalentRecipe(
            final List<MerchantRecipe> existing,
            final MerchantRecipe candidate
    ) {
        final var candidateSell = candidate.getResult();

        for (final MerchantRecipe existing1 : existing) {
            final var existingSell = existing1.getResult();

            if (existingSell.getType() == candidateSell.getType()
                    && existingSell.getAmount() == candidateSell.getAmount()
                    && buyItemsMatch(existing1, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean buyItemsMatch(final MerchantRecipe a, final MerchantRecipe b) {
        final var aIng = a.getIngredients();
        final var bIng = b.getIngredients();
        if (aIng.size() != bIng.size()) {
            return false;
        }
        for (int i = 0; i < aIng.size(); i++) {
            if (aIng.get(i).getType() != bIng.get(i).getType()) {
                return false;
            }
        }
        return true;
    }

    private int getLastSyncedLevel(final Villager villager) {
        final Integer stored = villager.getPersistentDataContainer()
                .get(syncLevelKey, PersistentDataType.INTEGER);
        return stored != null ? stored : 0;
    }

    private void setLastSyncedLevel(final Villager villager, final int level) {
        villager.getPersistentDataContainer()
                .set(syncLevelKey, PersistentDataType.INTEGER, level);
    }
}
