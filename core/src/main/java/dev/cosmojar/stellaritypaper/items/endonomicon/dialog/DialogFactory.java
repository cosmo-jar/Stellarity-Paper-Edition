package dev.cosmojar.stellaritypaper.items.endonomicon.dialog;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.endonomicon.EndonomiconService;
import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;
import dev.cosmojar.stellaritypaper.items.endonomicon.search.SearchMatch;
import dev.cosmojar.stellaritypaper.items.endonomicon.search.SearchResult;
import dev.cosmojar.stellaritypaper.text.MessageService;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.*;

public final class DialogFactory {

    private static final TextColor GOLD_ACCENT = TextColor.color(0xFFD700);

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final CustomItemFactory customItemFactory;
    private final ItemDefinitionRegistry itemRegistry;
    private final ItemStateRepository itemStateRepository;

    public DialogFactory(
            JavaPlugin plugin,
            MessageService messageService,
            CustomItemFactory customItemFactory,
            ItemDefinitionRegistry itemRegistry,
            ItemStateRepository itemStateRepository
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.customItemFactory = customItemFactory;
        this.itemRegistry = itemRegistry;
        this.itemStateRepository = itemStateRepository;
    }

    public void playPageTurnSound(Player player) {
        try {
            player.playSound(player.getLocation(), "stellarity:item.endonomicon.open", 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        }
    }

    private ActionButton createActionButton(Component label, Component tooltip, Runnable action) {
        ActionButton.Builder builder = ActionButton.builder(label);
        if (tooltip != null) {
            builder.tooltip(tooltip);
        }
        return builder.action(DialogAction.customClick((view, audience) -> {
            Bukkit.getScheduler().runTask(plugin, action);
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(60)).build())).build();
    }

    public ItemStack resolveItemStack(String id, ItemStack fallbackVanilla) {
        if (id != null) {
            var defOpt = itemRegistry.findByPdcItemId(id);
            if (defOpt.isPresent()) {
                return customItemFactory.createAlwaysLore(defOpt.get());
            }
            try {
                Material mat = Material.valueOf(id.toUpperCase(Locale.ROOT));
                return new ItemStack(mat);
            } catch (Exception ignored) {}
        }
        return fallbackVanilla != null ? fallbackVanilla.clone() : new ItemStack(Material.BOOK);
    }

    public Component resolveItemName(String id, ItemStack item) {
        if ("elytra_dyeing".equalsIgnoreCase(id)) {
            return messageService.message("endonomicon.items.elytra_dyeing");
        }
        if ("elytra_undyeing".equalsIgnoreCase(id)) {
            return messageService.message("endonomicon.items.elytra_undyeing");
        }
        if ("misc_empty_enchanted_book".equalsIgnoreCase(id)) {
            return messageService.message("endonomicon.items.misc_empty_enchanted_book");
        }
        if (id != null) {
            var defOpt = itemRegistry.findByPdcItemId(id);
            if (defOpt.isPresent()) {
                String translateKey = defOpt.get().itemText().nameTranslateKey();
                if (messageService.hasKey(translateKey)) {
                    return messageService.message(translateKey);
                }
            }
        }
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasItemName()) {
            return item.getItemMeta().itemName();
        }
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().displayName();
        }
        if (item != null) {
            return Component.translatable(item.translationKey());
        }
        return id != null ? Component.text(id) : messageService.message("endonomicon.items.default_item_fallback");
    }

    public Map<String, Integer> buildInventoryMap(Inventory inventory) {
        Map<String, Integer> counts = new HashMap<>();
        if (inventory == null) return counts;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) continue;

            String customId = getCustomItemId(item);
            String key = (customId != null ? customId : item.getType().name()).toLowerCase(Locale.ROOT);

            counts.put(key, counts.getOrDefault(key, 0) + item.getAmount());
            if ("enchanted_book".equalsIgnoreCase(key)) {
                counts.put("misc_empty_enchanted_book", counts.getOrDefault("misc_empty_enchanted_book", 0) + item.getAmount());
            }
        }
        return counts;
    }

    private String getCustomItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        if (itemStateRepository != null) {
            var idOpt = itemStateRepository.getItemId(item);
            if (idOpt.isPresent() && !idOpt.get().isBlank()) {
                return idOpt.get();
            }
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
            if ("item_id".equalsIgnoreCase(key.getKey()) || "item".equalsIgnoreCase(key.getKey())) {
                try {
                    String val = pdc.get(key, PersistentDataType.STRING);
                    if (val != null && !val.isBlank()) {
                        return val;
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public void openMainMenu(Player player, EndonomiconService service, int favoritesCount, int recentCount) {
        playPageTurnSound(player);

        ItemStack endonomiconStack = resolveItemStack("endonomicon", null);
        Component title = messageService.message("endonomicon.main_menu.title");

        Dialog dialog = Dialog.create(builderFactory -> builderFactory.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                DialogBody.item(endonomiconStack)
                                        .description(DialogBody.plainMessage(messageService.message("endonomicon.main_menu.header")))
                                        .showDecorations(true)
                                        .showTooltip(true)
                                        .build()
                        ))
                        .build())
                .type(DialogType.multiAction(List.of(
                        createActionButton(
                                messageService.message("endonomicon.main_menu.categories.weapons_tools"),
                                messageService.message("endonomicon.main_menu.tooltips.weapons_tools"),
                                () -> service.openCategory(player, "weapons_tools", 1)
                        ),
                        createActionButton(
                                messageService.message("endonomicon.main_menu.categories.armor"),
                                messageService.message("endonomicon.main_menu.tooltips.armor"),
                                () -> service.openCategory(player, "armor", 1)
                        ),
                        createActionButton(
                                messageService.message("endonomicon.main_menu.categories.artifacts"),
                                messageService.message("endonomicon.main_menu.tooltips.artifacts"),
                                () -> service.openCategory(player, "artifacts", 1)
                        ),
                        createActionButton(
                                messageService.message("endonomicon.main_menu.categories.upgrades"),
                                messageService.message("endonomicon.main_menu.tooltips.upgrades"),
                                () -> service.openCategory(player, "upgrades", 1)
                        ),
                        createActionButton(
                                messageService.message("endonomicon.main_menu.personal.favorites", Map.of("count", String.valueOf(favoritesCount))),
                                messageService.message("endonomicon.main_menu.tooltips.favorites"),
                                () -> service.openFavorites(player, 1)
                        ),
                        createActionButton(
                                messageService.message("endonomicon.main_menu.personal.recent", Map.of("count", String.valueOf(recentCount))),
                                messageService.message("endonomicon.main_menu.tooltips.recent"),
                                () -> service.openRecent(player, 1)
                        ),
                        createActionButton(
                                messageService.message("endonomicon.main_menu.search"),
                                messageService.message("endonomicon.main_menu.tooltips.search"),
                                () -> service.openSearchPrompt(player)
                        )
                )).build())
        );

        showDialog(player, dialog);
    }

    public void openRecipeDetail(Player player, EndonomiconService service, RecipeViewModel vm, boolean isFavorite) {
        playPageTurnSound(player);

        Map<String, Integer> playerInv = buildInventoryMap(player.getInventory());
        Component favLabel = isFavorite 
                ? messageService.message("endonomicon.main_menu.buttons.remove_favorite") 
                : messageService.message("endonomicon.main_menu.buttons.add_favorite");

        if ("elytra_dyeing".equalsIgnoreCase(vm.getId())) {
            Component title = messageService.message("endonomicon.elytra_dyeing.title");
            List<DialogBody> bodies = new ArrayList<>();

            bodies.add(DialogBody.plainMessage(messageService.message("endonomicon.elytra_dyeing.header")));

            int elytraCurrent = playerInv.getOrDefault("elytra", 0)
                    + playerInv.getOrDefault("empress_wings", 0)
                    + playerInv.getOrDefault("dragon_wings", 0)
                    + playerInv.getOrDefault("phantom_wings", 0);

            int dyeCurrent = 0;
            for (Map.Entry<String, Integer> entry : playerInv.entrySet()) {
                if (entry.getKey().endsWith("_dye")) {
                    dyeCurrent += entry.getValue();
                }
            }

            ItemStack elytraStack = new ItemStack(Material.ELYTRA);
            Component elytraText = elytraCurrent >= 1 
                    ? messageService.message("endonomicon.elytra_dyeing.elytra_ok", Map.of("current", String.valueOf(elytraCurrent)))
                    : messageService.message("endonomicon.elytra_dyeing.elytra_missing");

            bodies.add(DialogBody.item(elytraStack)
                    .description(DialogBody.plainMessage(elytraText))
                    .showDecorations(true)
                    .showTooltip(true)
                    .build());

            ItemStack dyeStack = new ItemStack(Material.RED_DYE);
            Component dyeText = dyeCurrent >= 1
                    ? messageService.message("endonomicon.elytra_dyeing.dye_ok", Map.of("current", String.valueOf(dyeCurrent)))
                    : messageService.message("endonomicon.elytra_dyeing.dye_missing");

            bodies.add(DialogBody.item(dyeStack)
                    .description(DialogBody.plainMessage(dyeText))
                    .showDecorations(true)
                    .showTooltip(true)
                    .build());

            bodies.add(DialogBody.item(elytraStack)
                    .description(DialogBody.plainMessage(messageService.message("endonomicon.elytra_dyeing.description")))
                    .showDecorations(true)
                    .showTooltip(true)
                    .build());

            Dialog dialog = Dialog.create(builderFactory -> builderFactory.empty()
                    .base(DialogBase.builder(title).body(bodies).build())
                    .type(DialogType.multiAction(List.of(
                            createActionButton(
                                    messageService.message("endonomicon.main_menu.buttons.back"),
                                    messageService.message("endonomicon.main_menu.tooltips.back"),
                                    () -> service.openCategory(player, "upgrades", 1)
                            ),
                            createActionButton(favLabel, null, () -> {
                                if (isFavorite) {
                                    service.getUserDataRepo().removeFavorite(player, vm.getId());
                                } else {
                                    service.getUserDataRepo().addFavorite(player, vm.getId());
                                }
                                service.openRecipe(player, vm.getId());
                            }),
                            createActionButton(
                                    messageService.message("endonomicon.main_menu.buttons.main_menu"),
                                    messageService.message("endonomicon.main_menu.tooltips.main_menu"),
                                    () -> service.openMainMenu(player)
                            )
                    )).build())
            );

            showDialog(player, dialog);
            return;
        }

        ItemStack resultItem = resolveItemStack(vm.getRecipe().getResultCustomId(), vm.getRecipe().getResultVanillaItem());
        Component resultName = resolveItemName(vm.getRecipe().getResultCustomId(), resultItem);

        Component title = Component.text("⚔ ").append(resultName);

        List<DialogBody> bodies = new ArrayList<>();

        Component header = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                messageService.raw("endonomicon.recipe.header"),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("name", resultName),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("stars", vm.getDifficultyStars())
        );

        bodies.add(DialogBody.plainMessage(header));

        for (Map.Entry<String, Integer> ing : vm.getRecipe().getIngredients().entrySet()) {
            String ingId = ing.getKey();
            int required = ing.getValue();
            int current = playerInv.getOrDefault(ingId.toLowerCase(Locale.ROOT), 0);

            ItemStack ingStack = resolveItemStack(ingId, null);
            Component ingName = resolveItemName(ingId, ingStack);

            Component ingText;
            if (current >= required) {
                ingText = messageService.message("endonomicon.recipe.ingredient_ok", Map.of(
                        "current", String.valueOf(current),
                        "required", String.valueOf(required)
                )).append(ingName);
            } else {
                int missing = required - current;
                ingText = messageService.message("endonomicon.recipe.ingredient_missing", Map.of(
                        "current", String.valueOf(current),
                        "required", String.valueOf(required)
                )).append(ingName).append(messageService.message("endonomicon.recipe.ingredient_missing_suffix", Map.of(
                        "missing", String.valueOf(missing)
                )));
            }

            bodies.add(DialogBody.item(ingStack)
                    .description(DialogBody.plainMessage(ingText))
                    .showDecorations(true)
                    .showTooltip(true)
                    .build());
        }

        Component resultText = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                messageService.raw("endonomicon.recipe.result_header"),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("name", resultName)
        );

        if (!vm.getUsedIn().isEmpty()) {
            Component usedInComponents = Component.empty();
            for (int i = 0; i < vm.getUsedIn().size(); i++) {
                String usedId = vm.getUsedIn().get(i);
                ItemStack usedStack = resolveItemStack(usedId, null);
                Component usedName = resolveItemName(usedId, usedStack);
                if (i > 0) {
                    usedInComponents = usedInComponents.append(Component.text(", ", GOLD_ACCENT));
                }
                usedInComponents = usedInComponents.append(usedName);
            }

            resultText = resultText.append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    messageService.raw("endonomicon.recipe.used_in"),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("used_in", usedInComponents)
            ));
        }

        bodies.add(DialogBody.item(resultItem)
                .description(DialogBody.plainMessage(resultText))
                .showDecorations(true)
                .showTooltip(true)
                .build());

        String targetCategory = (vm != null && vm.getMetadata() != null) ? vm.getMetadata().getCategory() : "artifacts";

        Dialog dialog = Dialog.create(builderFactory -> builderFactory.empty()
                .base(DialogBase.builder(title)
                        .body(bodies)
                        .build())
                .type(DialogType.multiAction(List.of(
                        createActionButton(
                                messageService.message("endonomicon.main_menu.buttons.back"),
                                messageService.message("endonomicon.main_menu.tooltips.back"),
                                () -> service.openCategory(player, targetCategory, 1)
                        ),
                        createActionButton(favLabel, null, () -> {
                            if (isFavorite) {
                                service.getUserDataRepo().removeFavorite(player, vm.getId());
                            } else {
                                service.getUserDataRepo().addFavorite(player, vm.getId());
                            }
                            service.openRecipe(player, vm.getId());
                        }),
                        createActionButton(
                                messageService.message("endonomicon.main_menu.buttons.main_menu"),
                                messageService.message("endonomicon.main_menu.tooltips.main_menu"),
                                () -> service.openMainMenu(player)
                        )
                )).build())
        );

        showDialog(player, dialog);
    }

    public void openCategoryView(Player player, EndonomiconService service, String category, List<SearchMatch> matches, int page, int totalPages) {
        playPageTurnSound(player);

        String catKey = category.equalsIgnoreCase("weapons") || category.equalsIgnoreCase("weapons_tools") ? "weapons_tools" : category.equalsIgnoreCase("armor") ? "armor" : category.equalsIgnoreCase("upgrades") ? "upgrades" : "artifacts";
        Component title = messageService.message("endonomicon.category.title." + catKey, Map.of(
                "page", String.valueOf(page),
                "total", String.valueOf(totalPages)
        ));

        Component bodyText;
        var pageMatches = PaginationService.getPage(matches, page, 5);

        List<ActionButton> buttons = new ArrayList<>();

        if (pageMatches.isEmpty()) {
            bodyText = messageService.message("endonomicon.search.no_results");
        } else {
            bodyText = messageService.message("endonomicon.main_menu.header");
            for (var match : pageMatches) {
                RecipeViewModel vm = match.getViewModel();
                ItemStack item = resolveItemStack(vm.getRecipe().getResultCustomId(), vm.getRecipe().getResultVanillaItem());
                Component name = resolveItemName(vm.getRecipe().getResultCustomId(), item);

                buttons.add(createActionButton(
                        Component.text(vm.getMetadata().getIcon() + " ").append(name),
                        Component.text("★ " + vm.getDifficultyStars()),
                        () -> service.openRecipe(player, vm.getId())
                ));
            }
        }

        if (page > 1) {
            buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.prev_page"), null, () -> service.openCategory(player, category, page - 1)));
        }
        if (page < totalPages) {
            buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.next_page"), null, () -> service.openCategory(player, category, page + 1)));
        }
        buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.main_menu"), null, () -> service.openMainMenu(player)));

        final Component finalBody = bodyText;
        Dialog dialog = Dialog.create(builderFactory -> builderFactory.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(DialogBody.plainMessage(finalBody)))
                        .build())
                .type(DialogType.multiAction(buttons).build())
        );

        showDialog(player, dialog);
    }

    public void openFavoritesView(Player player, EndonomiconService service, List<SearchMatch> matches, int page, int totalPages) {
        playPageTurnSound(player);

        Component title = messageService.message("endonomicon.favorites.title", Map.of(
                "page", String.valueOf(page),
                "total", String.valueOf(totalPages)
        ));

        Component bodyText;
        var pageMatches = PaginationService.getPage(matches, page, 5);

        List<ActionButton> buttons = new ArrayList<>();

        if (pageMatches.isEmpty()) {
            bodyText = messageService.message("endonomicon.favorites.empty");
        } else {
            bodyText = messageService.message("endonomicon.main_menu.header");
            for (var match : pageMatches) {
                RecipeViewModel vm = match.getViewModel();
                ItemStack item = resolveItemStack(vm.getRecipe().getResultCustomId(), vm.getRecipe().getResultVanillaItem());
                Component name = resolveItemName(vm.getRecipe().getResultCustomId(), item);

                buttons.add(createActionButton(
                        Component.text(vm.getMetadata().getIcon() + " ").append(name),
                        Component.text("★ " + vm.getDifficultyStars()),
                        () -> service.openRecipe(player, vm.getId())
                ));
            }
        }

        if (page > 1) {
            buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.prev_page"), null, () -> service.openFavorites(player, page - 1)));
        }
        if (page < totalPages) {
            buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.next_page"), null, () -> service.openFavorites(player, page + 1)));
        }
        buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.main_menu"), null, () -> service.openMainMenu(player)));

        final Component finalBody = bodyText;
        Dialog dialog = Dialog.create(builderFactory -> builderFactory.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(DialogBody.plainMessage(finalBody)))
                        .build())
                .type(DialogType.multiAction(buttons).build())
        );

        showDialog(player, dialog);
    }

    public void openRecentView(Player player, EndonomiconService service, List<SearchMatch> matches, int page, int totalPages) {
        playPageTurnSound(player);

        Component title = messageService.message("endonomicon.recent.title", Map.of(
                "page", String.valueOf(page),
                "total", String.valueOf(totalPages)
        ));

        Component bodyText;
        var pageMatches = PaginationService.getPage(matches, page, 5);

        List<ActionButton> buttons = new ArrayList<>();

        if (pageMatches.isEmpty()) {
            bodyText = messageService.message("endonomicon.recent.empty");
        } else {
            bodyText = messageService.message("endonomicon.main_menu.header");
            for (var match : pageMatches) {
                RecipeViewModel vm = match.getViewModel();
                ItemStack item = resolveItemStack(vm.getRecipe().getResultCustomId(), vm.getRecipe().getResultVanillaItem());
                Component name = resolveItemName(vm.getRecipe().getResultCustomId(), item);

                buttons.add(createActionButton(
                        Component.text(vm.getMetadata().getIcon() + " ").append(name),
                        Component.text("★ " + vm.getDifficultyStars()),
                        () -> service.openRecipe(player, vm.getId())
                ));
            }
        }

        if (page > 1) {
            buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.prev_page"), null, () -> service.openRecent(player, page - 1)));
        }
        if (page < totalPages) {
            buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.next_page"), null, () -> service.openRecent(player, page + 1)));
        }
        buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.main_menu"), null, () -> service.openMainMenu(player)));

        final Component finalBody = bodyText;
        Dialog dialog = Dialog.create(builderFactory -> builderFactory.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(DialogBody.plainMessage(finalBody)))
                        .build())
                .type(DialogType.multiAction(buttons).build())
        );

        showDialog(player, dialog);
    }

    public void openSearchResults(Player player, EndonomiconService service, SearchResult result, int page, int totalPages) {
        playPageTurnSound(player);

        Component title = messageService.message("endonomicon.search.results_title", Map.of(
                "query", result.getQuery(),
                "page", String.valueOf(page),
                "total", String.valueOf(totalPages)
        ));

        Component bodyText;
        var pageMatches = PaginationService.getPage(result.getMatches(), page, 5);

        List<ActionButton> buttons = new ArrayList<>();

        if (pageMatches.isEmpty()) {
            bodyText = messageService.message("endonomicon.search.no_results");
        } else {
            bodyText = messageService.message("endonomicon.main_menu.header");
            for (var match : pageMatches) {
                RecipeViewModel vm = match.getViewModel();
                ItemStack item = resolveItemStack(vm.getRecipe().getResultCustomId(), vm.getRecipe().getResultVanillaItem());
                Component name = resolveItemName(vm.getRecipe().getResultCustomId(), item);

                buttons.add(createActionButton(
                        Component.text(vm.getMetadata().getIcon() + " ").append(name),
                        Component.text(match.getMatchReason() + " | " + match.getScore()),
                        () -> service.openRecipe(player, vm.getId())
                ));
            }
        }

        if (page > 1) {
            buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.prev_page"), null, () -> service.search(player, result.getQuery(), page - 1)));
        }
        if (page < totalPages) {
            buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.next_page"), null, () -> service.search(player, result.getQuery(), page + 1)));
        }
        buttons.add(createActionButton(messageService.message("endonomicon.main_menu.buttons.main_menu"), null, () -> service.openMainMenu(player)));

        final Component finalBody = bodyText;
        Dialog dialog = Dialog.create(builderFactory -> builderFactory.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(DialogBody.plainMessage(finalBody)))
                        .build())
                .type(DialogType.multiAction(buttons).build())
        );

        showDialog(player, dialog);
    }

    public void openSearchPrompt(Player player, EndonomiconService service) {
        playPageTurnSound(player);

        Component title = messageService.message("endonomicon.search.prompt_title");

        Dialog dialog = Dialog.create(builderFactory -> builderFactory.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(DialogBody.plainMessage(messageService.message("endonomicon.search.prompt_message"))))
                        .inputs(List.of(
                                DialogInput.text("query", messageService.message("endonomicon.search.query_label"))
                                        .maxLength(64)
                                        .build()
                        ))
                        .build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(messageService.message("endonomicon.main_menu.search"))
                                .action(DialogAction.customClick((view, audience) -> {
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        String userQuery = view.getText("query");
                                        if (userQuery != null && !userQuery.isBlank()) {
                                            service.search(player, userQuery, 1);
                                        } else {
                                            openSearchPrompt(player, service);
                                        }
                                    });
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(60)).build()))
                                .build(),
                        createActionButton(messageService.message("endonomicon.main_menu.buttons.back"), null, () -> service.openMainMenu(player))
                )).build())
        );

        showDialog(player, dialog);
    }

    private void showDialog(final Player player, final Dialog dialog) {
        try {
            player.showDialog(dialog);
            return;
        } catch (final LinkageError error) {
            try {
                for (final java.lang.reflect.Method m : player.getClass().getMethods()) {
                    if (("showDialog".equals(m.getName()) || "openDialog".equals(m.getName())) && m.getParameterCount() == 1) {
                        if (m.getParameterTypes()[0].isAssignableFrom(dialog.getClass())) {
                            m.invoke(player, dialog);
                            return;
                        }
                    }
                }
                plugin.getLogger().warning("Could not find a valid showDialog method on " + player.getClass().getName() + " for dialog type " + dialog.getClass().getName());
            } catch (final Throwable t) {
                plugin.getLogger().severe("Failed to show dialog to player " + player.getName() + ": " + t.getMessage());
                t.printStackTrace();
            }
        } catch (final Throwable t) {
            plugin.getLogger().severe("Failed to show dialog to player " + player.getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }
}
