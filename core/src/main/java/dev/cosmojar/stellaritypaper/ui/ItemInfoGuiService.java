package dev.cosmojar.stellaritypaper.ui;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.enchants.EnchantDataCodec;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.ItemLoreBuilder;
import dev.cosmojar.stellaritypaper.text.MessageService;
import dev.cosmojar.stellaritypaper.text.TextService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class ItemInfoGuiService implements Listener {

    private final Plugin plugin;
    private final MessageService messageService;
    private final TextService textService;
    private final ItemLoreBuilder loreBuilder;
    private final ItemStateRepository itemStateRepository;
    private final EnchantDataCodec codec;

    private final Material[] colors = {
            Material.GRAY_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE,
            Material.BLACK_STAINED_GLASS_PANE
    };

    public ItemInfoGuiService(
            final Plugin plugin,
            final MessageService messageService,
            final TextService textService,
            final ItemLoreBuilder loreBuilder,
            final ItemStateRepository itemStateRepository,
            final EnchantDataCodec codec
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.textService = textService;
        this.loreBuilder = loreBuilder;
        this.itemStateRepository = itemStateRepository;
        this.codec = codec;
    }

    public void open(final Player player, final ItemStack itemInHand, final CustomItemDefinition definition) {
        final Component title = messageService.message("command.item.gui-title");
        final ItemInfoGuiHolder holder = new ItemInfoGuiHolder(definition);
        final Inventory inventory = Bukkit.createInventory(holder, 27, title);

        for (int i = 0; i < 27; i++) {
            if (i == 12 || i == 14) continue;
            inventory.setItem(i, createPane(colors[i % colors.length]));
        }

        final ItemStack centralItem = itemInHand.clone();
        final List<Component> baseLore = loreBuilder.buildAlways(definition);
        final List<Component> formattedLore = new ArrayList<>();

        for (final Component component : baseLore) {
            final Component styledLine = component
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
            formattedLore.add(styledLine);
        }

        final ItemMeta meta = centralItem.getItemMeta();
        if (meta != null) {
            meta.lore(formattedLore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            centralItem.setItemMeta(meta);
        }
        inventory.setItem(12, centralItem);

        final ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        final ItemMeta bookMeta = book.getItemMeta();
        if (bookMeta != null) {
            final Component bookTitle = messageService.message("command.item.book-title")
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
            bookMeta.displayName(bookTitle);

            final List<EnchantInstance> enchants = codec.decode(itemStateRepository.getEnchantsData(itemInHand).orElse(""));
            final List<Component> bookLore = new ArrayList<>();

            if (enchants.isEmpty()) {
                final Component noEnchants = messageService.message("command.item.no-enchants-lore")
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
                bookLore.add(noEnchants);
            } else {
                for (final EnchantInstance instance : enchants) {
                    final String id = instance.id();
                    final String localizedId;
                    if (id.startsWith("_technical/void_pendant/")) {
                        final String gem = id.substring(id.lastIndexOf('/') + 1);
                        localizedId = "technical_void_pendant_" + gem;
                    } else {
                        localizedId = id;
                    }

                    final String keyBase = "enchants.entries." + localizedId;
                    final String nameKey = keyBase + ".name";
                    final String levelKey = keyBase + ".levels." + instance.level();

                    final String name;
                    final boolean hideLevel;
                    if (messageService.hasKey(levelKey)) {
                        name = messageService.raw(levelKey);
                        hideLevel = true;
                    } else if (messageService.hasKey(nameKey)) {
                        name = messageService.raw(nameKey);
                        hideLevel = false;
                    } else {
                        name = id;
                        hideLevel = false;
                    }

                    final String romanLevel = hideLevel ? "" : " " + toRoman(instance.level());
                    final String format = messageService.raw("enchants.format.line");
                    final String formattedHeader = format.replace("%name%", name).replace("%level%", romanLevel);
                    final Component headerComponent = textService.mm(formattedHeader)
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);

                    if (!bookLore.isEmpty()) {
                        bookLore.add(Component.empty());
                    }
                    bookLore.add(headerComponent);

                    final String lvlDescKey = keyBase + ".levels-description." + instance.level();
                    if (messageService.hasKey(lvlDescKey)) {
                        final Component descComponent = textService.mm(messageService.raw(lvlDescKey))
                                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
                        bookLore.add(descComponent);
                    } else {
                        final String descKey = keyBase + ".description";
                        if (messageService.hasKey(descKey)) {
                            final Component descComponent = textService.mm(messageService.raw(descKey))
                                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
                            bookLore.add(descComponent);
                        }
                    }
                }
            }
            bookMeta.lore(bookLore);
            bookMeta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            book.setItemMeta(bookMeta);
        }
        inventory.setItem(14, book);

        player.openInventory(inventory);

        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int tickCount = 0;

            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory().getHolder() == holder) {
                    tickCount++;
                    final Inventory inv = player.getOpenInventory().getTopInventory();
                    for (int i = 0; i < 27; i++) {
                        if (i == 12 || i == 14) continue;
                        int colorIndex = (i + tickCount) % colors.length;
                        inv.setItem(i, createPane(colors[colorIndex]));
                    }
                } else {
                    holder.cancelTask();
                }
            }
        }, 5L, 5L);
        holder.setAnimTask(task);
    }

    private ItemStack createPane(final Material material) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    private String toRoman(final int number) {
        if (number <= 0) {
            return "I";
        }
        final int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        final String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int value = number;
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (value >= values[i]) {
                value -= values[i];
                result.append(symbols[i]);
            }
        }
        return result.toString();
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ItemInfoGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ItemInfoGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ItemInfoGuiHolder holder) {
            holder.cancelTask();
        }
    }
}
