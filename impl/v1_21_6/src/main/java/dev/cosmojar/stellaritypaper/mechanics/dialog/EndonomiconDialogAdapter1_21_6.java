package dev.cosmojar.stellaritypaper.mechanics.dialog;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.core.Holder;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.TextInput;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;

public final class EndonomiconDialogAdapter1_21_6 {

    private static final TextColor GOLD_ACCENT = TextColor.color(0xFFD700);

    private static Object invoke(Object target, String methodName, Object... args) {
        if (target == null) return null;
        try {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                    return m.invoke(target, args);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Component msg(Object service, String key) {
        Object ms = invoke(service, "getMessageService");
        if (ms == null) return Component.text(key);
        Boolean hasKey = (Boolean) invoke(ms, "hasKey", key);
        if (Boolean.TRUE.equals(hasKey)) {
            Component result = (Component) invoke(ms, "message", key);
            if (result != null) return result;
        }
        return Component.text(key);
    }

    private Component msg(Object service, String key, Map<String, String> placeholders) {
        Object ms = invoke(service, "getMessageService");
        if (ms == null) return Component.text(key);
        Boolean hasKey = (Boolean) invoke(ms, "hasKey", key);
        if (Boolean.TRUE.equals(hasKey)) {
            Component result = (Component) invoke(ms, "message", key, placeholders);
            if (result != null) return result;
        }
        return Component.text(key);
    }

    private String raw(Object service, String key) {
        Object ms = invoke(service, "getMessageService");
        if (ms == null) return key;
        String result = (String) invoke(ms, "raw", key);
        return result != null ? result : key;
    }

    public void playPageTurnSound(Player player) {
        try {
            player.playSound(player.getLocation(), "stellarity:item.endonomicon.open", 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        }
    }

    private ActionButton createActionButton(JavaPlugin plugin, Component label, Component tooltip, Runnable action) {
        ClickEvent advClick = ClickEvent.callback(audience -> {
            Bukkit.getScheduler().runTask(plugin, action);
        });
        Style advStyle = Style.style().clickEvent(advClick).build();
        net.minecraft.network.chat.Style nmsStyle = PaperAdventure.asVanilla(advStyle);
        net.minecraft.network.chat.ClickEvent nmsClick = nmsStyle.getClickEvent();

        StaticAction staticAction = new StaticAction(nmsClick);
        CommonButtonData buttonData = new CommonButtonData(
                PaperAdventure.asVanilla(label),
                tooltip != null ? Optional.of(PaperAdventure.asVanilla(tooltip)) : Optional.empty(),
                200
        );
        return new ActionButton(buttonData, Optional.of(staticAction));
    }

    public ItemStack resolveItemStack(Object service, String id, ItemStack fallbackVanilla) {
        if (id != null) {
            Object registry = invoke(service, "getItemRegistry");
            Object factory = invoke(service, "getCustomItemFactory");
            if (registry != null && factory != null) {
                Optional<?> defOpt = (Optional<?>) invoke(registry, "findByPdcItemId", id);
                if (defOpt != null && defOpt.isPresent()) {
                    Object def = defOpt.get();
                    ItemStack created = (ItemStack) invoke(factory, "createAlwaysLore", def);
                    if (created == null) {
                        created = (ItemStack) invoke(factory, "create", def);
                    }
                    if (created != null) return created;
                }
            }
            try {
                Material mat = Material.valueOf(id.toUpperCase(Locale.ROOT));
                return new ItemStack(mat);
            } catch (Exception ignored) {}
        }
        return fallbackVanilla != null ? fallbackVanilla.clone() : new ItemStack(Material.BOOK);
    }

    public Component resolveItemName(Object service, String id, ItemStack item) {
        if ("elytra_dyeing".equalsIgnoreCase(id)) {
            return msg(service, "endonomicon.items.elytra_dyeing");
        }
        if ("elytra_undyeing".equalsIgnoreCase(id)) {
            return msg(service, "endonomicon.items.elytra_undyeing");
        }
        if ("misc_empty_enchanted_book".equalsIgnoreCase(id)) {
            return msg(service, "endonomicon.items.misc_empty_enchanted_book");
        }
        if (id != null) {
            Object registry = invoke(service, "getItemRegistry");
            Object ms = invoke(service, "getMessageService");
            if (registry != null && ms != null) {
                Optional<?> defOpt = (Optional<?>) invoke(registry, "findByPdcItemId", id);
                if (defOpt != null && defOpt.isPresent()) {
                    Object def = defOpt.get();
                    Object itemText = invoke(def, "itemText");
                    if (itemText != null) {
                        String translateKey = (String) invoke(itemText, "nameTranslateKey");
                        if (translateKey != null) {
                            Boolean hasKey = (Boolean) invoke(ms, "hasKey", translateKey);
                            if (Boolean.TRUE.equals(hasKey)) {
                                Component msgComp = (Component) invoke(ms, "message", translateKey);
                                if (msgComp != null) return msgComp;
                            }
                        }
                    }
                }
            }
        }
        if (item != null && item.hasItemMeta()) {
            if (item.getItemMeta().hasItemName()) return item.getItemMeta().itemName();
            if (item.getItemMeta().hasDisplayName()) return item.getItemMeta().displayName();
        }
        if (item != null) {
            return Component.translatable(item.translationKey());
        }
        return id != null ? Component.text(id) : msg(service, "endonomicon.items.default_item_fallback");
    }

    public Map<String, Integer> buildInventoryMap(Object service, Inventory inventory) {
        Map<String, Integer> counts = new HashMap<>();
        if (inventory == null) return counts;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) continue;

            String customId = getCustomItemId(service, item);
            String key = (customId != null ? customId : item.getType().name()).toLowerCase(Locale.ROOT);

            counts.put(key, counts.getOrDefault(key, 0) + item.getAmount());
            if ("enchanted_book".equalsIgnoreCase(key)) {
                counts.put("misc_empty_enchanted_book", counts.getOrDefault("misc_empty_enchanted_book", 0) + item.getAmount());
            }
        }
        return counts;
    }

    private String getCustomItemId(Object service, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        Object repo = invoke(service, "getItemStateRepository");
        if (repo != null) {
            @SuppressWarnings("unchecked")
            Optional<String> idOpt = (Optional<String>) invoke(repo, "getItemId", item);
            if (idOpt != null && idOpt.isPresent() && !idOpt.get().isBlank()) {
                return idOpt.get();
            }
        }
        return null;
    }

    public void openMainMenu(Player player, Object service, JavaPlugin plugin, int favCount, int recCount) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;
        playPageTurnSound(player);

        try {
            ItemStack endonomiconStack = resolveItemStack(service, "endonomicon", null);
            Component title = msg(service, "endonomicon.main_menu.title");

            PlainMessage headerMsg = new PlainMessage(
                    PaperAdventure.asVanilla(msg(service, "endonomicon.main_menu.header")),
                    200
            );

            ItemBody itemBody = new ItemBody(
                    CraftItemStack.asNMSCopy(endonomiconStack),
                    Optional.of(headerMsg),
                    true,
                    true,
                    32,
                    32
            );

            CommonDialogData commonData = new CommonDialogData(
                    PaperAdventure.asVanilla(title),
                    Optional.empty(),
                    true,
                    false,
                    DialogAction.NONE,
                    List.of(itemBody),
                    List.of()
            );

            List<ActionButton> buttons = List.of(
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.categories.weapons_tools"), msg(service, "endonomicon.main_menu.tooltips.weapons_tools"), () -> invoke(service, "openCategory", player, "weapons_tools", 1)),
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.categories.armor"), msg(service, "endonomicon.main_menu.tooltips.armor"), () -> invoke(service, "openCategory", player, "armor", 1)),
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.categories.artifacts"), msg(service, "endonomicon.main_menu.tooltips.artifacts"), () -> invoke(service, "openCategory", player, "artifacts", 1)),
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.categories.upgrades"), msg(service, "endonomicon.main_menu.tooltips.upgrades"), () -> invoke(service, "openCategory", player, "upgrades", 1)),
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.personal.favorites", Map.of("count", String.valueOf(favCount))), msg(service, "endonomicon.main_menu.tooltips.favorites"), () -> invoke(service, "openFavorites", player, 1)),
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.personal.recent", Map.of("count", String.valueOf(recCount))), msg(service, "endonomicon.main_menu.tooltips.recent"), () -> invoke(service, "openRecent", player, 1)),
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.search"), msg(service, "endonomicon.main_menu.tooltips.search"), () -> invoke(service, "openSearchPrompt", player))
            );

            MultiActionDialog dialog = new MultiActionDialog(commonData, buttons, Optional.empty(), 2);
            craftPlayer.getHandle().openDialog(Holder.direct(dialog));

        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to open Endonomicon main menu: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void openCategoryView(Player player, Object service, JavaPlugin plugin, String category, Object matchesObj, int page, int totalPages) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;
        playPageTurnSound(player);
        List<?> matches = (List<?>) matchesObj;

        try {
            String catKey = category.equalsIgnoreCase("weapons") || category.equalsIgnoreCase("weapons_tools") ? "weapons_tools" :
                             category.equalsIgnoreCase("armor") ? "armor" :
                             category.equalsIgnoreCase("upgrades") ? "upgrades" : "artifacts";

            Component title = msg(service, "endonomicon.category.title." + catKey, Map.of(
                    "page", String.valueOf(page),
                    "total", String.valueOf(totalPages)
            ));

            List<?> pageMatches = getPageMatches(matches, page, 5);
            List<ActionButton> buttons = new ArrayList<>();
            Component bodyText;

            if (pageMatches == null || pageMatches.isEmpty()) {
                bodyText = msg(service, "endonomicon.search.no_results");
            } else {
                bodyText = msg(service, "endonomicon.main_menu.header");
                for (Object match : pageMatches) {
                    Object vm = invoke(match, "getViewModel");
                    Object recipe = invoke(vm, "getRecipe");
                    String resCustomId = (String) invoke(recipe, "getResultCustomId");
                    ItemStack resVanillaItem = (ItemStack) invoke(recipe, "getResultVanillaItem");
                    String vmId = (String) invoke(vm, "getId");
                    String stars = (String) invoke(vm, "getDifficultyStars");

                    Object meta = invoke(vm, "getMetadata");
                    String icon = meta != null ? (String) invoke(meta, "getIcon") : "•";

                    ItemStack item = resolveItemStack(service, resCustomId, resVanillaItem);
                    Component name = resolveItemName(service, resCustomId, item);

                    Component label = Component.text((icon != null ? icon : "•") + " ").append(name);
                    buttons.add(createActionButton(plugin, label, Component.text("★ " + (stars != null ? stars : "1")), () -> invoke(service, "openRecipe", player, vmId)));
                }
            }

            if (page > 1) {
                buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.prev_page"), null, () -> invoke(service, "openCategory", player, category, page - 1)));
            }
            if (page < totalPages) {
                buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.next_page"), null, () -> invoke(service, "openCategory", player, category, page + 1)));
            }
            buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.main_menu"), null, () -> invoke(service, "openMainMenu", player)));

            PlainMessage headerMsg = new PlainMessage(PaperAdventure.asVanilla(bodyText), 200);
            CommonDialogData commonData = new CommonDialogData(PaperAdventure.asVanilla(title), Optional.empty(), true, false, DialogAction.NONE, List.of(headerMsg), List.of());
            MultiActionDialog dialog = new MultiActionDialog(commonData, buttons, Optional.empty(), 2);
            craftPlayer.getHandle().openDialog(Holder.direct(dialog));

        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to open Endonomicon category view: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void openRecipeDetail(Player player, Object service, JavaPlugin plugin, Object vm, boolean isFavorite) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;
        playPageTurnSound(player);

        try {
            String vmId = (String) invoke(vm, "getId");
            Map<String, Integer> playerInv = buildInventoryMap(service, player.getInventory());

            Component favLabel = isFavorite
                    ? msg(service, "endonomicon.main_menu.buttons.remove_favorite")
                    : msg(service, "endonomicon.main_menu.buttons.add_favorite");

            if ("elytra_dyeing".equalsIgnoreCase(vmId)) {
                Component title = msg(service, "endonomicon.elytra_dyeing.title");
                List<DialogBody> bodies = new ArrayList<>();

                bodies.add(new PlainMessage(PaperAdventure.asVanilla(msg(service, "endonomicon.elytra_dyeing.header")), 200));

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
                        ? msg(service, "endonomicon.elytra_dyeing.elytra_ok", Map.of("current", String.valueOf(elytraCurrent)))
                        : msg(service, "endonomicon.elytra_dyeing.elytra_missing");

                bodies.add(new ItemBody(
                        CraftItemStack.asNMSCopy(elytraStack),
                        Optional.of(new PlainMessage(PaperAdventure.asVanilla(elytraText), 200)),
                        true, true, 32, 32
                ));

                ItemStack dyeStack = new ItemStack(Material.RED_DYE);
                Component dyeText = dyeCurrent >= 1
                        ? msg(service, "endonomicon.elytra_dyeing.dye_ok", Map.of("current", String.valueOf(dyeCurrent)))
                        : msg(service, "endonomicon.elytra_dyeing.dye_missing");

                bodies.add(new ItemBody(
                        CraftItemStack.asNMSCopy(dyeStack),
                        Optional.of(new PlainMessage(PaperAdventure.asVanilla(dyeText), 200)),
                        true, true, 32, 32
                ));

                bodies.add(new ItemBody(
                        CraftItemStack.asNMSCopy(elytraStack),
                        Optional.of(new PlainMessage(PaperAdventure.asVanilla(msg(service, "endonomicon.elytra_dyeing.description")), 200)),
                        true, true, 32, 32
                ));

                List<ActionButton> buttons = List.of(
                        createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.back"), msg(service, "endonomicon.main_menu.tooltips.back"), () -> invoke(service, "openCategory", player, "upgrades", 1)),
                        createActionButton(plugin, favLabel, null, () -> {
                            Object repo = invoke(service, "getUserDataRepo");
                            if (repo != null) {
                                if (isFavorite) {
                                    invoke(repo, "removeFavorite", player, vmId);
                                } else {
                                    invoke(repo, "addFavorite", player, vmId);
                                }
                            }
                            invoke(service, "openRecipe", player, vmId);
                        }),
                        createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.main_menu"), msg(service, "endonomicon.main_menu.tooltips.main_menu"), () -> invoke(service, "openMainMenu", player))
                );

                CommonDialogData commonData = new CommonDialogData(PaperAdventure.asVanilla(title), Optional.empty(), true, false, DialogAction.NONE, bodies, List.of());
                MultiActionDialog dialog = new MultiActionDialog(commonData, buttons, Optional.empty(), 2);
                craftPlayer.getHandle().openDialog(Holder.direct(dialog));
                return;
            }

            Object recipe = invoke(vm, "getRecipe");
            String resultCustomId = (String) invoke(recipe, "getResultCustomId");
            ItemStack resultVanillaItem = (ItemStack) invoke(recipe, "getResultVanillaItem");

            ItemStack resultItem = resolveItemStack(service, resultCustomId, resultVanillaItem);
            Component resultName = resolveItemName(service, resultCustomId, resultItem);

            Component title = Component.text("⚔ ").append(resultName);
            List<DialogBody> bodies = new ArrayList<>();

            String stars = (String) invoke(vm, "getDifficultyStars");
            Component header = MiniMessage.miniMessage().deserialize(
                    raw(service, "endonomicon.recipe.header"),
                    Placeholder.component("name", resultName),
                    Placeholder.parsed("stars", stars != null ? stars : "★")
            );
            bodies.add(new PlainMessage(PaperAdventure.asVanilla(header), 200));

            @SuppressWarnings("unchecked")
            Map<String, Integer> ingredients = (Map<String, Integer>) invoke(recipe, "getIngredients");
            if (ingredients != null) {
                for (Map.Entry<String, Integer> ing : ingredients.entrySet()) {
                    String ingId = ing.getKey();
                    int required = ing.getValue();
                    int current = playerInv.getOrDefault(ingId.toLowerCase(Locale.ROOT), 0);

                    ItemStack ingStack = resolveItemStack(service, ingId, null);
                    Component ingName = resolveItemName(service, ingId, ingStack);

                    Component ingText;
                    if (current >= required) {
                        ingText = msg(service, "endonomicon.recipe.ingredient_ok", Map.of(
                                "current", String.valueOf(current),
                                "required", String.valueOf(required)
                        )).append(ingName);
                    } else {
                        int missing = required - current;
                        ingText = msg(service, "endonomicon.recipe.ingredient_missing", Map.of(
                                "current", String.valueOf(current),
                                "required", String.valueOf(required)
                        )).append(ingName).append(msg(service, "endonomicon.recipe.ingredient_missing_suffix", Map.of(
                                "missing", String.valueOf(missing)
                        )));
                    }

                    bodies.add(new ItemBody(
                            CraftItemStack.asNMSCopy(ingStack),
                            Optional.of(new PlainMessage(PaperAdventure.asVanilla(ingText), 200)),
                            true, true, 32, 32
                    ));
                }
            }

            Component resultText = MiniMessage.miniMessage().deserialize(
                    raw(service, "endonomicon.recipe.result_header"),
                    Placeholder.component("name", resultName)
            );

            @SuppressWarnings("unchecked")
            List<String> usedIn = (List<String>) invoke(vm, "getUsedIn");
            if (usedIn != null && !usedIn.isEmpty()) {
                Component usedInComponents = Component.empty();
                for (int i = 0; i < usedIn.size(); i++) {
                    String usedId = usedIn.get(i);
                    ItemStack usedStack = resolveItemStack(service, usedId, null);
                    Component usedName = resolveItemName(service, usedId, usedStack);
                    if (i > 0) {
                        usedInComponents = usedInComponents.append(Component.text(", ", GOLD_ACCENT));
                    }
                    usedInComponents = usedInComponents.append(usedName);
                }

                resultText = resultText.append(MiniMessage.miniMessage().deserialize(
                        raw(service, "endonomicon.recipe.used_in"),
                        Placeholder.component("used_in", usedInComponents)
                ));
            }

            bodies.add(new ItemBody(
                    CraftItemStack.asNMSCopy(resultItem),
                    Optional.of(new PlainMessage(PaperAdventure.asVanilla(resultText), 200)),
                    true, true, 32, 32
            ));

            Object meta = invoke(vm, "getMetadata");
            String targetCategory = meta != null ? (String) invoke(meta, "getCategory") : "artifacts";

            List<ActionButton> buttons = List.of(
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.back"), msg(service, "endonomicon.main_menu.tooltips.back"), () -> invoke(service, "openCategory", player, targetCategory != null ? targetCategory : "artifacts", 1)),
                    createActionButton(plugin, favLabel, null, () -> {
                        Object repo = invoke(service, "getUserDataRepo");
                        if (repo != null) {
                            if (isFavorite) {
                                invoke(repo, "removeFavorite", player, vmId);
                            } else {
                                invoke(repo, "addFavorite", player, vmId);
                            }
                        }
                        invoke(service, "openRecipe", player, vmId);
                    }),
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.main_menu"), msg(service, "endonomicon.main_menu.tooltips.main_menu"), () -> invoke(service, "openMainMenu", player))
            );

            CommonDialogData commonData = new CommonDialogData(PaperAdventure.asVanilla(title), Optional.empty(), true, false, DialogAction.NONE, bodies, List.of());
            MultiActionDialog dialog = new MultiActionDialog(commonData, buttons, Optional.empty(), 2);
            craftPlayer.getHandle().openDialog(Holder.direct(dialog));

        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to open Endonomicon recipe detail: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void openFavoritesView(Player player, Object service, JavaPlugin plugin, Object matchesObj, int page, int totalPages) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;
        playPageTurnSound(player);
        List<?> matches = (List<?>) matchesObj;

        try {
            Component title = msg(service, "endonomicon.favorites.title", Map.of(
                    "page", String.valueOf(page),
                    "total", String.valueOf(totalPages)
            ));

            List<?> pageMatches = getPageMatches(matches, page, 5);
            List<ActionButton> buttons = new ArrayList<>();
            Component bodyText;

            if (pageMatches == null || pageMatches.isEmpty()) {
                bodyText = msg(service, "endonomicon.favorites.empty");
            } else {
                bodyText = msg(service, "endonomicon.main_menu.header");
                for (Object match : pageMatches) {
                    Object vm = invoke(match, "getViewModel");
                    Object recipe = invoke(vm, "getRecipe");
                    String resCustomId = (String) invoke(recipe, "getResultCustomId");
                    ItemStack resVanillaItem = (ItemStack) invoke(recipe, "getResultVanillaItem");
                    String vmId = (String) invoke(vm, "getId");
                    String stars = (String) invoke(vm, "getDifficultyStars");

                    Object meta = invoke(vm, "getMetadata");
                    String icon = meta != null ? (String) invoke(meta, "getIcon") : "•";

                    ItemStack item = resolveItemStack(service, resCustomId, resVanillaItem);
                    Component name = resolveItemName(service, resCustomId, item);

                    Component label = Component.text((icon != null ? icon : "•") + " ").append(name);
                    buttons.add(createActionButton(plugin, label, Component.text("★ " + (stars != null ? stars : "1")), () -> invoke(service, "openRecipe", player, vmId)));
                }
            }

            if (page > 1) {
                buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.prev_page"), null, () -> invoke(service, "openFavorites", player, page - 1)));
            }
            if (page < totalPages) {
                buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.next_page"), null, () -> invoke(service, "openFavorites", player, page + 1)));
            }
            buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.main_menu"), null, () -> invoke(service, "openMainMenu", player)));

            PlainMessage headerMsg = new PlainMessage(PaperAdventure.asVanilla(bodyText), 200);
            CommonDialogData commonData = new CommonDialogData(PaperAdventure.asVanilla(title), Optional.empty(), true, false, DialogAction.NONE, List.of(headerMsg), List.of());
            MultiActionDialog dialog = new MultiActionDialog(commonData, buttons, Optional.empty(), 2);
            craftPlayer.getHandle().openDialog(Holder.direct(dialog));

        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to open Endonomicon favorites view: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void openRecentView(Player player, Object service, JavaPlugin plugin, Object matchesObj, int page, int totalPages) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;
        playPageTurnSound(player);
        List<?> matches = (List<?>) matchesObj;

        try {
            Component title = msg(service, "endonomicon.recent.title", Map.of(
                    "page", String.valueOf(page),
                    "total", String.valueOf(totalPages)
            ));

            List<?> pageMatches = getPageMatches(matches, page, 5);
            List<ActionButton> buttons = new ArrayList<>();
            Component bodyText;

            if (pageMatches == null || pageMatches.isEmpty()) {
                bodyText = msg(service, "endonomicon.recent.empty");
            } else {
                bodyText = msg(service, "endonomicon.main_menu.header");
                for (Object match : pageMatches) {
                    Object vm = invoke(match, "getViewModel");
                    Object recipe = invoke(vm, "getRecipe");
                    String resCustomId = (String) invoke(recipe, "getResultCustomId");
                    ItemStack resVanillaItem = (ItemStack) invoke(recipe, "getResultVanillaItem");
                    String vmId = (String) invoke(vm, "getId");
                    String stars = (String) invoke(vm, "getDifficultyStars");

                    Object meta = invoke(vm, "getMetadata");
                    String icon = meta != null ? (String) invoke(meta, "getIcon") : "•";

                    ItemStack item = resolveItemStack(service, resCustomId, resVanillaItem);
                    Component name = resolveItemName(service, resCustomId, item);

                    Component label = Component.text((icon != null ? icon : "•") + " ").append(name);
                    buttons.add(createActionButton(plugin, label, Component.text("★ " + (stars != null ? stars : "1")), () -> invoke(service, "openRecipe", player, vmId)));
                }
            }

            if (page > 1) {
                buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.prev_page"), null, () -> invoke(service, "openRecent", player, page - 1)));
            }
            if (page < totalPages) {
                buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.next_page"), null, () -> invoke(service, "openRecent", player, page + 1)));
            }
            buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.main_menu"), null, () -> invoke(service, "openMainMenu", player)));

            PlainMessage headerMsg = new PlainMessage(PaperAdventure.asVanilla(bodyText), 200);
            CommonDialogData commonData = new CommonDialogData(PaperAdventure.asVanilla(title), Optional.empty(), true, false, DialogAction.NONE, List.of(headerMsg), List.of());
            MultiActionDialog dialog = new MultiActionDialog(commonData, buttons, Optional.empty(), 2);
            craftPlayer.getHandle().openDialog(Holder.direct(dialog));

        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to open Endonomicon recent view: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void openSearchResults(Player player, Object service, JavaPlugin plugin, Object resultObj, int page, int totalPages) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;
        playPageTurnSound(player);

        try {
            String query = (String) invoke(resultObj, "getQuery");
            List<?> matches = (List<?>) invoke(resultObj, "getMatches");

            Component title = msg(service, "endonomicon.search.results_title", Map.of(
                    "query", query != null ? query : "",
                    "page", String.valueOf(page),
                    "total", String.valueOf(totalPages)
            ));

            List<?> pageMatches = getPageMatches(matches, page, 5);
            List<ActionButton> buttons = new ArrayList<>();
            Component bodyText;

            if (pageMatches == null || pageMatches.isEmpty()) {
                bodyText = msg(service, "endonomicon.search.no_results");
            } else {
                bodyText = msg(service, "endonomicon.main_menu.header");
                for (Object match : pageMatches) {
                    Object vm = invoke(match, "getViewModel");
                    Object recipe = invoke(vm, "getRecipe");
                    String resCustomId = (String) invoke(recipe, "getResultCustomId");
                    ItemStack resVanillaItem = (ItemStack) invoke(recipe, "getResultVanillaItem");
                    String vmId = (String) invoke(vm, "getId");

                    Object meta = invoke(vm, "getMetadata");
                    String icon = meta != null ? (String) invoke(meta, "getIcon") : "•";

                    Object reason = invoke(match, "getMatchReason");
                    Object score = invoke(match, "getScore");

                    ItemStack item = resolveItemStack(service, resCustomId, resVanillaItem);
                    Component name = resolveItemName(service, resCustomId, item);

                    Component label = Component.text((icon != null ? icon : "•") + " ").append(name);
                    Component tooltip = Component.text((reason != null ? reason : "") + " | " + (score != null ? score : ""));

                    buttons.add(createActionButton(plugin, label, tooltip, () -> invoke(service, "openRecipe", player, vmId)));
                }
            }

            if (page > 1) {
                buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.prev_page"), null, () -> invoke(service, "search", player, query, page - 1)));
            }
            if (page < totalPages) {
                buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.next_page"), null, () -> invoke(service, "search", player, query, page + 1)));
            }
            buttons.add(createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.main_menu"), null, () -> invoke(service, "openMainMenu", player)));

            PlainMessage headerMsg = new PlainMessage(PaperAdventure.asVanilla(bodyText), 200);
            CommonDialogData commonData = new CommonDialogData(PaperAdventure.asVanilla(title), Optional.empty(), true, false, DialogAction.NONE, List.of(headerMsg), List.of());
            MultiActionDialog dialog = new MultiActionDialog(commonData, buttons, Optional.empty(), 2);
            craftPlayer.getHandle().openDialog(Holder.direct(dialog));

        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to open Endonomicon search results: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void openSearchPrompt(Player player, Object service, JavaPlugin plugin) {
        if (!(player instanceof CraftPlayer craftPlayer)) return;
        playPageTurnSound(player);

        try {
            Component title = msg(service, "endonomicon.search.prompt_title");
            Component bodyMsg = msg(service, "endonomicon.search.prompt_message");

            TextInput textInput = new TextInput(200, PaperAdventure.asVanilla(msg(service, "endonomicon.search.query_label")), true, "", 64, Optional.empty());
            Input input = new Input("query", textInput);

            List<ActionButton> buttons = List.of(
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.search"), msg(service, "endonomicon.main_menu.tooltips.search"), () -> {
                        invoke(service, "search", player, "", 1);
                    }),
                    createActionButton(plugin, msg(service, "endonomicon.main_menu.buttons.back"), msg(service, "endonomicon.main_menu.tooltips.back"), () -> invoke(service, "openMainMenu", player))
            );

            PlainMessage headerMsg = new PlainMessage(PaperAdventure.asVanilla(bodyMsg), 200);
            CommonDialogData commonData = new CommonDialogData(PaperAdventure.asVanilla(title), Optional.empty(), true, false, DialogAction.NONE, List.of(headerMsg), List.of(input));
            MultiActionDialog dialog = new MultiActionDialog(commonData, buttons, Optional.empty(), 2);
            craftPlayer.getHandle().openDialog(Holder.direct(dialog));

        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to open Endonomicon search prompt: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private List<?> getPageMatches(List<?> list, int page, int pageSize) {
        if (list == null || list.isEmpty() || page < 1) return Collections.emptyList();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= list.size()) return Collections.emptyList();
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return list.subList(fromIndex, toIndex);
    }
}
