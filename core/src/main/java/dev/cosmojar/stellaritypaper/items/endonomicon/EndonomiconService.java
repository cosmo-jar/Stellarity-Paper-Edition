package dev.cosmojar.stellaritypaper.items.endonomicon;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.endonomicon.dialog.DialogFactory;
import dev.cosmojar.stellaritypaper.items.endonomicon.events.EndonomiconOpenEvent;
import dev.cosmojar.stellaritypaper.items.endonomicon.events.RecipeViewedEvent;
import dev.cosmojar.stellaritypaper.items.endonomicon.model.*;
import dev.cosmojar.stellaritypaper.items.endonomicon.search.*;
import dev.cosmojar.stellaritypaper.mechanics.altar.AccursedAltarService;
import dev.cosmojar.stellaritypaper.text.MessageService;
import dev.cosmojar.stellaritypaper.util.DebugLog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class EndonomiconService {

    private final JavaPlugin plugin;
    private final AccursedAltarService altarService;
    private final ItemDefinitionRegistry itemRegistry;
    private final MessageService messageService;
    private final EndonomiconUserDataRepository userDataRepo;
    private final CustomItemFactory customItemFactory;
    private final ItemStateRepository itemStateRepository;
    private final SearchService searchService;
    private DialogFactory dialogFactory;

    private Map<String, RecipeViewModel> viewModelCache = Map.of();

    public EndonomiconService(
            JavaPlugin plugin,
            AccursedAltarService altarService,
            ItemDefinitionRegistry itemRegistry,
            MessageService messageService,
            CustomItemFactory customItemFactory,
            ItemStateRepository itemStateRepository
    ) {
        this.plugin = plugin;
        this.altarService = altarService;
        this.itemRegistry = itemRegistry;
        this.messageService = messageService;
        this.customItemFactory = customItemFactory;
        this.itemStateRepository = itemStateRepository;
        this.userDataRepo = new EndonomiconUserDataRepository(plugin);
        this.searchService = new SearchService();

        rebuildAsync();
    }

    public CompletableFuture<Void> rebuildAsync() {
        return CompletableFuture.runAsync(() -> {
            List<AccursedAltarService.AccursedRecipe> rawRecipes = altarService.getRecipes();
            RecipeIndex newIndex = new RecipeIndex(rawRecipes);
            RecipeGraph newGraph = new RecipeGraph(newIndex);

            Map<String, RecipeViewModel> newCache = new HashMap<>();
            for (var entry : newIndex.getAllRecipes().entrySet()) {
                String id = entry.getKey();
                var recipe = entry.getValue();

                RecipeMetadata meta = createMetadata(id);
                RecipeViewModel vm = RecipeViewModelBuilder.build(recipe, meta, newGraph);
                newCache.put(id, vm);
            }

            this.viewModelCache = Collections.unmodifiableMap(newCache);
        }).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                DebugLog.log(plugin, "[DEBUG] Endonomicon: System rebuilt with " + viewModelCache.size() + " recipes.");
            });
        });
    }

    private RecipeMetadata createMetadata(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        String category;
        Set<String> tags = new HashSet<>(Set.of("#altar", "#end"));
        List<String> aliases = new ArrayList<>(List.of(key, key.replace("_", " ")));

        if (key.contains("chestplate") || key.contains("helmet") || key.contains("leggings") || key.contains("boots")
                || key.startsWith("hallowed_") || key.startsWith("champion_") || key.startsWith("floral_")
                || key.equals("shulker_helmet") || key.equals("shulker_chestplate") || key.equals("shulker_leggings") || key.equals("shulker_boots")) {
            category = "armor";
            tags.add("#armor");
            tags.add("#броня");
            tags.add("#доспехи");
        } else if (key.contains("sword") || key.contains("pickaxe") || key.contains("shovel") || key.contains("hoe")
                || key.contains("axe") || key.contains("spear") || key.contains("trident") || key.contains("bow")
                || key.contains("tamaris") || key.contains("scythe") || key.contains("dragonblade") || key.contains("harvester")
                || key.contains("inferno") || key.contains("kaliedoscope") || key.contains("kaleidoscope") || key.contains("fury")
                || key.contains("weapon") || key.contains("sharanga")) {
            category = "weapons_tools";
            tags.add("#weapon");
            tags.add("#tool");
            tags.add("#оружие");
            tags.add("#инструменты");
        } else if (key.contains("elytra") || key.contains("wings") || key.contains("pendant") || key.contains("upgrade")
                || key.contains("locket") || key.contains("trail")) {
            if ("satchel_of_voids".equals(key)) {
                category = "artifacts";
                tags.add("#artifact");
                tags.add("#артефакт");
            } else {
                category = "upgrades";
                tags.add("#upgrade");
                tags.add("#elytra");
                tags.add("#элитры");
                tags.add("#крылья");
            }
        } else {
            category = "artifacts";
            tags.add("#artifact");
            tags.add("#артефакт");
        }

        if (key.equals("shulker_pickaxe")) {
            aliases.addAll(List.of("шалкеровая кирка", "кирка шалкера", "шалки кирка", "кирка"));
            tags.addAll(Set.of("#кирка", "#инструмент", "#шалки"));
        } else if (key.equals("shulker_sword")) {
            aliases.addAll(List.of("шалкеровый меч", "меч шалкера", "шалки меч", "меч"));
            tags.addAll(Set.of("#меч", "#оружие", "#шалки"));
        } else if (key.equals("shulker_axe")) {
            aliases.addAll(List.of("шалкеровый топор", "топор шалкера", "шалки топор", "топор"));
            tags.addAll(Set.of("#топор", "#инструмент", "#шалки"));
        } else if (key.equals("shulker_shovel")) {
            aliases.addAll(List.of("шалкеровая лопата", "лопата шалкера", "шалки лопата", "лопата"));
            tags.addAll(Set.of("#лопата", "#инструмент", "#шалки"));
        } else if (key.equals("shulker_hoe")) {
            aliases.addAll(List.of("шалкеровая мотыга", "мотыга шалкера", "шалки мотыга", "мотыга"));
            tags.addAll(Set.of("#мотыга", "#инструмент", "#шалки"));
        } else if (key.equals("shulker_spear")) {
            aliases.addAll(List.of("шалкеровое копьё", "шалкеровое копье", "копьё шалкера", "копье шалкера", "копьё", "копье"));
            tags.addAll(Set.of("#копьё", "#копье", "#оружие", "#шалки"));
        } else if (key.equals("shulker_helmet")) {
            aliases.addAll(List.of("шалкеровый шлем", "шлем шалкера", "шлем", "шалки шлем"));
            tags.addAll(Set.of("#шлем", "#броня", "#шалки"));
        } else if (key.equals("shulker_chestplate")) {
            aliases.addAll(List.of("шалкеровый нагрудник", "нагрудник шалкера", "панцирь", "нагрудник", "шалки панцирь"));
            tags.addAll(Set.of("#нагрудник", "#панцирь", "#броня", "#шалки"));
        } else if (key.equals("shulker_leggings")) {
            aliases.addAll(List.of("шалкеровые поножи", "поножи шалкера", "поножи", "штаны"));
            tags.addAll(Set.of("#поножи", "#броня", "#шалки"));
        } else if (key.equals("shulker_boots")) {
            aliases.addAll(List.of("шалкеровые ботинки", "ботинки шалкера", "ботинки", "сапоги"));
            tags.addAll(Set.of("#ботинки", "#сапоги", "#броня", "#шалки"));
        } else if (key.startsWith("hallowed_")) {
            aliases.addAll(List.of("святой сет", "святая броня", "святой шлем", "святой нагрудник", "святые поножи", "святые ботинки", "святое"));
            tags.addAll(Set.of("#святой", "#броня"));
        } else if (key.startsWith("champion_")) {
            aliases.addAll(List.of("сет чемпиона", "броня чемпиона", "шлем чемпиона", "нагрудник чемпиона", "поножи чемпиона", "ботинки чемпиона", "чемпион"));
            tags.addAll(Set.of("#чемпион", "#броня"));
        } else if (key.startsWith("floral_")) {
            aliases.addAll(List.of("цветочный сет", "цветочная броня", "цветочный шлем", "цветочный нагрудник", "цветочные поножи", "цветочные ботинки", "цветочный"));
            tags.addAll(Set.of("#цветочный", "#броня"));
        } else if (key.equals("elytra_dyeing")) {
            aliases.addAll(List.of("покраска шлейфа элитр", "покраска элитр", "покраска", "шлейф", "след элитр", "цветной шлейф", "элитры", "краситель"));
            tags.addAll(Set.of("#элитры", "#шлейф", "#покраска", "#краситель"));
        } else if (key.equals("empress_wings")) {
            aliases.addAll(List.of("крылья императрицы", "императрица", "крылья", "элитры"));
            tags.addAll(Set.of("#крылья", "#элитры", "#императрица"));
        } else if (key.equals("dragon_wings")) {
            aliases.addAll(List.of("крылья дракона", "дракон", "крылья", "элитры"));
            tags.addAll(Set.of("#крылья", "#элитры", "#дракон"));
        } else if (key.equals("phantom_wings")) {
            aliases.addAll(List.of("крылья фантома", "фантом", "крылья", "элитры"));
            tags.addAll(Set.of("#крылья", "#элитры", "#фантом"));
        } else if (key.equals("tamaris")) {
            aliases.addAll(List.of("тамарис", "меч тамарис", "меч"));
            tags.addAll(Set.of("#меч", "#оружие", "#тамарис"));
        } else if (key.equals("spectral_fury")) {
            aliases.addAll(List.of("спектральная ярость", "ярость", "спектральный лук", "лук"));
            tags.addAll(Set.of("#лук", "#оружие", "#ярость"));
        } else if (key.equals("sandstorm_trident")) {
            aliases.addAll(List.of("трезубец песчаной бури", "трезубец", "буря", "трезубец бури"));
            tags.addAll(Set.of("#трезубец", "#оружие", "#буря"));
        } else if (key.equals("satchel_of_voids")) {
            aliases.addAll(List.of("мешок бездны", "мешок", "бездна"));
            tags.addAll(Set.of("#мешок", "#бездна", "#артефакт"));
        } else if (key.equals("crest_of_the_end")) {
            aliases.addAll(List.of("герб энда", "герб", "щит"));
            tags.addAll(Set.of("#герб", "#щит", "#артефакт"));
        } else if (key.equals("chorus_plating")) {
            aliases.addAll(List.of("пластина коруса", "пластина", "корус"));
            tags.addAll(Set.of("#пластина", "#корус", "#артефакт"));
        } else if (key.equals("book_of_jinx")) {
            aliases.addAll(List.of("книга сглаза", "сглаз", "книга"));
            tags.addAll(Set.of("#книга", "#сглаз", "#артефакт"));
        } else if (key.equals("book_of_conveyance")) {
            aliases.addAll(List.of("книга перемещения", "перемещение", "телепорт", "книга"));
            tags.addAll(Set.of("#книга", "#телепорт", "#артефакт"));
        } else if (key.equals("endonomicon")) {
            aliases.addAll(List.of("гримуар эндономикон", "эндономикон", "гримуар", "книга"));
            tags.addAll(Set.of("#гримуар", "#эндономикон", "#книга", "#артефакт"));
        }

        String icon = category.equals("weapons_tools") ? "⚔" : category.equals("armor") ? "🛡" : category.equals("upgrades") ? "🧪" : "🔮";

        return new RecipeMetadata(
                key,
                "item.stellarity." + key,
                aliases,
                tags,
                "",
                icon,
                "",
                category
        );
    }

    public void openMainMenu(Player player) {
        if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs()) {
            return;
        }
        Bukkit.getPluginManager().callEvent(new EndonomiconOpenEvent(player));
        userDataRepo.cleanupOrphans(player, viewModelCache.keySet());

        int favCount = userDataRepo.getFavorites(player).size();
        int recCount = userDataRepo.getRecentViewed(player).size();

        if (dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().hasCustomDialogAdapter()) {
            dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().openEndonomiconMainMenu(player, this, plugin, favCount, recCount);
            return;
        }

        DialogFactory df = getDialogFactory();
        if (df != null) {
            df.openMainMenu(player, this, favCount, recCount);
        }
    }

    public void openSearchPrompt(Player player) {
        if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs()) {
            return;
        }
        if (dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().hasCustomDialogAdapter()) {
            dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().openEndonomiconSearchPrompt(player, this, plugin);
            return;
        }
        DialogFactory df = getDialogFactory();
        if (df != null) {
            df.openSearchPrompt(player, this);
        }
    }

    public void openRecipe(Player player, String recipeId) {
        if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs() || recipeId == null) return;
        RecipeViewModel vm = viewModelCache.get(recipeId.toLowerCase(Locale.ROOT));
        if (vm == null) {
            player.sendMessage("§cРецепт не найден: " + recipeId);
            return;
        }

        userDataRepo.addRecentViewed(player, vm.getId());
        Bukkit.getPluginManager().callEvent(new RecipeViewedEvent(player, vm));

        boolean isFav = userDataRepo.isFavorite(player, vm.getId());

        if (dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().hasCustomDialogAdapter()) {
            dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().openEndonomiconRecipeDetail(player, this, plugin, vm, isFav);
            return;
        }

        DialogFactory df = getDialogFactory();
        if (df != null) {
            df.openRecipeDetail(player, this, vm, isFav);
        }
    }

    public void search(Player player, String query, int page) {
        if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs()) {
            return;
        }
        if ("prompt".equalsIgnoreCase(query)) {
            openSearchPrompt(player);
            return;
        }
        SearchContext context = new SearchContext(query, player, player.getLocale(), Set.of(), page, "relevance");
        SearchResult result = searchService.search(context, new ArrayList<>(viewModelCache.values()));
        int totalPages = dev.cosmojar.stellaritypaper.items.endonomicon.dialog.PaginationService.getTotalPages(result.getTotalResults(), 5);

        if (dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().hasCustomDialogAdapter()) {
            dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().openEndonomiconSearchResults(player, this, plugin, result, page, totalPages);
            return;
        }

        DialogFactory df = getDialogFactory();
        if (df != null) {
            df.openSearchResults(player, this, result, page, totalPages);
        }
    }

    public void openCategory(Player player, String category, int page) {
        if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs()) {
            return;
        }
        List<SearchMatch> matches = new ArrayList<>();
        for (RecipeViewModel vm : viewModelCache.values()) {
            if (category.equalsIgnoreCase(vm.getMetadata().getCategory())) {
                matches.add(new SearchMatch(vm, 100, "Категория: " + category));
            }
        }
        int totalPages = dev.cosmojar.stellaritypaper.items.endonomicon.dialog.PaginationService.getTotalPages(matches.size(), 5);

        if (dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().hasCustomDialogAdapter()) {
            dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().openEndonomiconCategoryView(player, this, plugin, category, matches, page, totalPages);
            return;
        }

        DialogFactory df = getDialogFactory();
        if (df != null) {
            df.openCategoryView(player, this, category, matches, page, totalPages);
        }
    }

    public void openFavorites(Player player, int page) {
        if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs()) {
            return;
        }
        Set<String> favIds = userDataRepo.getFavorites(player);
        List<SearchMatch> matches = new ArrayList<>();
        for (String id : favIds) {
            RecipeViewModel vm = viewModelCache.get(id.toLowerCase(Locale.ROOT));
            if (vm != null) {
                matches.add(new SearchMatch(vm, 100, "Избранное ⭐"));
            }
        }
        int totalPages = dev.cosmojar.stellaritypaper.items.endonomicon.dialog.PaginationService.getTotalPages(matches.size(), 5);

        if (dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().hasCustomDialogAdapter()) {
            dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().openEndonomiconFavoritesView(player, this, plugin, matches, page, totalPages);
            return;
        }

        DialogFactory df = getDialogFactory();
        if (df != null) {
            df.openFavoritesView(player, this, matches, page, totalPages);
        }
    }

    public void openRecent(Player player, int page) {
        if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs()) {
            return;
        }
        Deque<String> recentIds = userDataRepo.getRecentViewed(player);
        List<SearchMatch> matches = new ArrayList<>();
        for (String id : recentIds) {
            RecipeViewModel vm = viewModelCache.get(id.toLowerCase(Locale.ROOT));
            if (vm != null) {
                matches.add(new SearchMatch(vm, 100, "Недавние 🕓"));
            }
        }
        int totalPages = dev.cosmojar.stellaritypaper.items.endonomicon.dialog.PaginationService.getTotalPages(matches.size(), 5);

        if (dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().hasCustomDialogAdapter()) {
            dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().openEndonomiconRecentView(player, this, plugin, matches, page, totalPages);
            return;
        }

        DialogFactory df = getDialogFactory();
        if (df != null) {
            df.openRecentView(player, this, matches, page, totalPages);
        }
    }

    public EndonomiconUserDataRepository getUserDataRepo() { return userDataRepo; }
    public MessageService getMessageService() { return messageService; }
    public CustomItemFactory getCustomItemFactory() { return customItemFactory; }
    public ItemDefinitionRegistry getItemRegistry() { return itemRegistry; }
    public ItemStateRepository getItemStateRepository() { return itemStateRepository; }
    public synchronized DialogFactory getDialogFactory() {
        if (!dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().supportsDialogs()) {
            return null;
        }
        if (dialogFactory == null && !dev.cosmojar.stellaritypaper.api.VersionManager.getAdapter().hasCustomDialogAdapter()) {
            dialogFactory = new DialogFactory(plugin, messageService, customItemFactory, itemRegistry, itemStateRepository);
        }
        return dialogFactory;
    }
}
