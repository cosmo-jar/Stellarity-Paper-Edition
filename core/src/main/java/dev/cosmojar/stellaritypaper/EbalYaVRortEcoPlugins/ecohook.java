package dev.cosmojar.stellaritypaper.EbalYaVRortEcoPlugins;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.ItemLoreBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * A special class that acts as a protector against the lawlessness of Auxilor ecosystem plugins (eco, EcoEnchants, libreforge).
 * All calls are moved to isolated classes: ProtocolLibZalupaIntercepter and EcoZalupaIntercepter.
 */
public final class ecohook {

    private static boolean aktivirovano = false;
    private static boolean protoLibAktivirovan = false;
    private static ItemDefinitionRegistry reestrPredmetov;
    private static ItemLoreBuilder stroitelLora;
    private static ItemStateRepository hranilischePdc;
    private static BukkitTask periodicTaskInstance;

    private ecohook() {
    }

    /**
     * Checks whether Eco plugins are present on the server.
     */
    public static boolean estLiTutEcoXyinya() {
        try {
            final PluginManager pm = Bukkit.getPluginManager();
            return pm.getPlugin("EcoEnchants") != null
                    || pm.getPlugin("eco") != null
                    || pm.getPlugin("libreforge") != null;
        } catch (final Throwable ignored) {
            return false;
        }
    }

    /**
     * Checks whether ProtocolLib is enabled.
     */
    public static boolean estLiProtocolLib() {
        try {
            final PluginManager pm = Bukkit.getPluginManager();
            return pm.getPlugin("ProtocolLib") != null && pm.isPluginEnabled("ProtocolLib");
        } catch (final Throwable ignored) {
            return false;
        }
    }

    /**
     * Entry point: saves services and registers deferred initialization.
     */
    public static void vkluchitZashituBlyat(
            final StellarityPaperPlugin plugin,
            final ItemDefinitionRegistry itemRegistry,
            final ItemLoreBuilder loreBuilder,
            final ItemStateRepository itemStateRepository
    ) {
        reestrPredmetov = itemRegistry;
        stroitelLora = loreBuilder;
        hranilischePdc = itemStateRepository;

        final boolean protoLibConfig = plugin.getConfig().getBoolean("fix-lore-translate-protocol-lib", false);

        // If the plugins are already loaded
        if (estLiTutEcoXyinya() || protoLibConfig) {
            poehaliKarasikiBlyat(plugin);
            return;
        }

        // The ServerLoadEvent listener is registered (when ALL plugins are loaded)
        final Listener startupListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onServerLoad(final ServerLoadEvent event) {
                poehaliKarasikiBlyat(plugin);
            }
        };
        plugin.getServer().getPluginManager().registerEvents(startupListener, plugin);

        // An additional timer set for 1 tick, in case ServerLoadEvent has already occurred
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!aktivirovano && (estLiTutEcoXyinya() || protoLibConfig)) {
                poehaliKarasikiBlyat(plugin);
            }
        });
    }

    /**
     * Launching all defense levels after the server and all plugins have fully loaded
     */
    public static synchronized void poehaliKarasikiBlyat(final StellarityPaperPlugin plugin) {
        if (aktivirovano) {
            if (estLiTutEcoXyinya()) {
                EcoZalupaIntercepter.obnovitZashituDisplay();
            }
            return;
        }

        final boolean protoLibConfig = plugin.getConfig().getBoolean("fix-lore-translate-protocol-lib", false);
        final boolean ecoNashelsya = estLiTutEcoXyinya();

        if (!ecoNashelsya && !protoLibConfig) {
            return;
        }

        aktivirovano = true;

        if (ecoNashelsya) {
            plugin.getLogger().info("AUXILOR ECO PLUGINS HAVE BEEN DETECTED!");
            
            // Level 1: Injection of ZashchitnikOtEcoEnchants directly into Display.REGISTERED_MODULES of the Eco core
            EcoZalupaIntercepter.vstavitZalupuVEcoDisplay(plugin);

            // task checker (0 load)
            periodicTaskInstance = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                EcoZalupaIntercepter.obnovitZashituDisplay();
            }, 40L, 40L);
        } else {
            plugin.getLogger().info("Eco‑plugins were not detected.");
        }

        // Уровень 2: ProtocolLib
        if (protoLibConfig) {
            poprobovatProtocolLib(plugin);
        }

        plugin.getLogger().info("Lore protection is enabled");
    }

    /**
     * safety initialization of ProtocolLib
     * This is executed only if the "fix-lore-translate-protocol-lib" parameter is set to true in config!
     */
    private static void poprobovatProtocolLib(final StellarityPaperPlugin plugin) {
        if (!estLiProtocolLib()) {
            return;
        }

        try {
            // Dynamic check for the presence of the class before linking ProtocolLibZalupaIntercepter
            Class.forName("com.comphenix.protocol.events.PacketListener", false, ecohook.class.getClassLoader());
            ProtocolLibZalupaIntercepter.vkluchit(plugin);
            protoLibAktivirovan = true;
        } catch (final Throwable t) {
            plugin.getLogger().warning("ProtocolLib API is not available in the ClassLoader. (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ").");
        }
    }

    /**
     * Вырубает хук при выключении плагина
     */
    public static synchronized void svalitVZakhat() {
        if (!aktivirovano) {
            return;
        }

        if (periodicTaskInstance != null) {
            try {
                periodicTaskInstance.cancel();
            } catch (final Throwable ignored) {
            }
            periodicTaskInstance = null;
        }

        if (protoLibAktivirovan) {
            try {
                ProtocolLibZalupaIntercepter.vykluchit();
            } catch (final Throwable ignored) {
            }
            protoLibAktivirovan = false;
        }

        aktivirovano = false;
    }

    /**
     * Checks whether the item belongs to Stellarity
     */
    public static boolean etoPredmetStellarity(final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        try {
            if (!item.hasItemMeta()) {
                return false;
            }
            final ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            }

            final PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (final NamespacedKey key : pdc.getKeys()) {
                if ("stellarity".equals(key.getNamespace())) {
                    return true;
                }
            }

            final Component displayName = meta.displayName();
            if (displayName instanceof TranslatableComponent tc) {
                if (tc.key().contains("stellarity")) {
                    return true;
                }
            }

            if (meta.hasLore()) {
                final List<Component> lore = meta.lore();
                if (lore != null) {
                    for (final Component comp : lore) {
                        if (comp instanceof TranslatableComponent tc) {
                            if (tc.key().contains("stellarity")) {
                                return true;
                            }
                        }
                        final String plain = PlainTextComponentSerializer.plainText().serialize(comp).trim();
                        if (etoKlyuchPerevodaStellarity(plain)) {
                            return true;
                        }
                    }
                }
            }
        } catch (final Throwable ignored) {
        }
        return false;
    }

    /**
     * Checks whether this item needs to be repaired due to the mischief of eco‑plugins.....
     */
    public static boolean nadoLiChinitEtuHuynu(final ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        final ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }

        final List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }

        for (final Component line : lore) {
            final String plain = PlainTextComponentSerializer.plainText().serialize(line).trim();
            // if there is EcoEnchants trash
            if (plain.startsWith("§z") || plain.startsWith("\u00a7z")) {
                return true;
            }
            // if a string should be translatable, but it has become plain text
            if (!(line instanceof TranslatableComponent) && etoKlyuchPerevodaStellarity(plain)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the string is a key for Stellarity translation
     */
    public static boolean etoKlyuchPerevodaStellarity(final String plain) {
        if (plain == null || plain.isEmpty()) {
            return false;
        }
        return plain.startsWith("item.stellarity.")
                || plain.startsWith("items.stellarity.")
                || plain.startsWith("block.stellarity.")
                || plain.startsWith("item.minecraft.potion.effect.stellarity.")
                || plain.startsWith("item.minecraft.lingering_potion.effect.stellarity.frost_cloud")
                || plain.startsWith("item.minecraft.splash_potion.effect.stellarity.entanglement")
                || "Stellarity".equals(plain);
    }

    /**
     * Fixes the item’s lore: removes §z prefixes and restores TranslatableComponent
     */
    public static ItemStack pochiniLoreBlyat(final ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return item;
        }

        final ItemStack fixed = item.clone();
        final ItemMeta meta = fixed.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return fixed;
        }

        final List<Component> currentLore = meta.lore();
        if (currentLore == null || currentLore.isEmpty()) {
            return fixed;
        }

        CustomItemDefinition definition = null;
        if (hranilischePdc != null && reestrPredmetov != null) {
            final String pdcId = hranilischePdc.getItemId(fixed).orElse(null);
            if (pdcId != null) {
                definition = reestrPredmetov.findByPdcItemId(pdcId).orElse(null);
            }
        }

        final List<Component> baseLore = (definition != null && stroitelLora != null)
                ? stroitelLora.buildAlways(definition)
                : null;

        final List<Component> healedLore = new ArrayList<>(currentLore.size());
        int baseLoreIndex = 0;

        for (final Component line : currentLore) {
            final String plain = PlainTextComponentSerializer.plainText().serialize(line).trim();

            if (plain.startsWith("§z") || plain.startsWith("\u00a7z")) {
                continue;
            }

            if (line instanceof TranslatableComponent) {
                healedLore.add(line);
                continue;
            }

            if (etoKlyuchPerevodaStellarity(plain)) {
                if (baseLore != null) {
                    Component matched = null;
                    for (int b = baseLoreIndex; b < baseLore.size(); b++) {
                        final Component candidate = baseLore.get(b);
                        if (candidate instanceof TranslatableComponent tc && tc.key().equals(plain)) {
                            matched = candidate;
                            baseLoreIndex = b + 1;
                            break;
                        }
                    }
                    if (matched != null) {
                        healedLore.add(matched);
                    } else {
                        healedLore.add(Component.translatable(plain)
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                } else {
                    healedLore.add(Component.translatable(plain)
                            .color(NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false));
                }
            } else {
                healedLore.add(line);
            }
        }

        meta.lore(healedLore);
        fixed.setItemMeta(meta);
        return fixed;
    }
}
