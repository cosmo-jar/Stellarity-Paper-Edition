package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.config.ConfigService;
import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.ItemLoreBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EnchantItemService {

    private static final int ENCHANTS_SCHEMA_VERSION = 2;

    private final ConfigService configService;
    private final ItemStateRepository itemStateRepository;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final EnchantDataCodec codec;
    private final EnchantLoreService enchantLoreService;
    private final ItemLoreBuilder itemLoreBuilder;
    private final HarvesterLoreService harvesterLoreService;

    public EnchantItemService(
            final ConfigService configService,
            final ItemStateRepository itemStateRepository,
            final ItemDefinitionRegistry itemDefinitionRegistry,
            final EnchantDataCodec codec,
            final EnchantLoreService enchantLoreService,
            final ItemLoreBuilder itemLoreBuilder,
            final HarvesterLoreService harvesterLoreService
    ) {
        this.configService = configService;
        this.itemStateRepository = itemStateRepository;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.codec = codec;
        this.enchantLoreService = enchantLoreService;
        this.itemLoreBuilder = itemLoreBuilder;
        this.harvesterLoreService = harvesterLoreService;
    }

    public boolean ensureUpToDate(final ItemStack item) {
        return ensureUpToDate(item, currentLocale());
    }

    public boolean ensureUpToDate(final ItemStack item, final String locale) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        final CustomItemDefinition definition = itemStateRepository.getItemId(item)
                .flatMap(itemDefinitionRegistry::findByPdcItemId)
                .orElse(null);

        List<EnchantInstance> enchants = codec.decode(itemStateRepository.getEnchantsData(item).orElse(""));

        if (definition == null && enchants.isEmpty()) {
            return false;
        }

        final int schema = itemStateRepository.getEnchantsSchema(item).orElse(0);
        final String stampedLocale = itemStateRepository.getEnchantsLocale(item).orElse("");
        final String normalizedLocale = normalizeLocale(locale);

        boolean changed = false;
        if (enchants.isEmpty() && schema == 0 && definition != null) {
            final List<EnchantInstance> defEnchants = definition.itemDefinition().components().enchants();
            if (!defEnchants.isEmpty()) {
                enchants = new ArrayList<>(defEnchants);
                itemStateRepository.setEnchantsData(item, codec.encode(enchants));
                changed = true;
            }
        }

        if (schema != ENCHANTS_SCHEMA_VERSION || !normalizedLocale.equalsIgnoreCase(stampedLocale) || changed) {
            rebuildLore(item, definition, enchants);
            itemStateRepository.setEnchantsSchema(item, ENCHANTS_SCHEMA_VERSION);
            itemStateRepository.setEnchantsLocale(item, normalizedLocale);
            return true;
        }
        return false;
    }

    public boolean rebuildNow(final ItemStack item) {
        return rebuildNowInternal(item, false);
    }

    public boolean rebuildNowAlways(final ItemStack item) {
        return rebuildNowInternal(item, true);
    }

    private boolean rebuildNowInternal(final ItemStack item, final boolean forceAlwaysBaseLore) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        final CustomItemDefinition definition = itemStateRepository.getItemId(item)
                .flatMap(itemDefinitionRegistry::findByPdcItemId)
                .orElse(null);
        List<EnchantInstance> enchants = codec.decode(itemStateRepository.getEnchantsData(item).orElse(""));
        if (enchants.isEmpty() && definition != null) {
            enchants = new ArrayList<>(definition.itemDefinition().components().enchants());
            if (!enchants.isEmpty()) {
                itemStateRepository.setEnchantsData(item, codec.encode(enchants));
            }
        }
        rebuildLore(item, definition, enchants, forceAlwaysBaseLore);
        itemStateRepository.setEnchantsSchema(item, ENCHANTS_SCHEMA_VERSION);
        itemStateRepository.setEnchantsLocale(item, currentLocale());
        return true;
    }

    public void ensurePlayerEquipmentUpToDate(final Player player) {
        if (player == null) {
            return;
        }
        final EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return;
        }

        ensureUpToDate(equipment.getItemInMainHand());
        ensureUpToDate(equipment.getItemInOffHand());
        ensureUpToDate(equipment.getHelmet());
        ensureUpToDate(equipment.getChestplate());
        ensureUpToDate(equipment.getLeggings());
        ensureUpToDate(equipment.getBoots());
    }

    private void rebuildLore(
            final ItemStack item,
            final CustomItemDefinition definition,
            final List<EnchantInstance> enchants
    ) {
        rebuildLore(item, definition, enchants, false);
    }

    private void rebuildLore(
            final ItemStack item,
            final CustomItemDefinition definition,
            final List<EnchantInstance> enchants,
            final boolean forceAlwaysBaseLore
    ) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        for (final org.bukkit.enchantments.Enchantment activeEnch : new ArrayList<>(meta.getEnchants().keySet())) {
            if ("stellarity".equalsIgnoreCase(activeEnch.getKey().getNamespace())) {
                meta.removeEnchant(activeEnch);
            }
        }

        for (final EnchantInstance instance : enchants) {
            final String rawId = instance.id();
            if (rawId.startsWith("_technical/void_pendant/")) {
                final org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString("stellarity:" + rawId);
                if (key != null) {
                    final org.bukkit.enchantments.Enchantment vanillaEnchant = org.bukkit.Registry.ENCHANTMENT.get(key);
                    if (vanillaEnchant != null) {
                        meta.addEnchant(vanillaEnchant, instance.level(), true);
                    }
                }
            }
        }

        final String pdcItemId = itemStateRepository.getItemId(item).orElse("");
        final boolean isVoidPendant = "void_pendant".equalsIgnoreCase(pdcItemId);
        if (isVoidPendant) {
            meta.setItemModel(org.bukkit.NamespacedKey.fromString("stellarity:void_pendant"));
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        if (!enchants.isEmpty()) {
            try {
                meta.setEnchantmentGlintOverride(true);
            } catch (final Throwable ignored) {
            }
        } else {
            try {
                if (meta.hasEnchants()) {
                    meta.setEnchantmentGlintOverride(null);
                } else {
                    meta.setEnchantmentGlintOverride(false);
                }
            } catch (final Throwable ignored) {
            }
        }

        final List<Component> gemLore = enchantLoreService.buildLore(enchants);
        final List<Component> harvesterLore = harvesterLoreService.buildLore(item);
        final List<Component> baseLore = definition != null
                ? (forceAlwaysBaseLore ? itemLoreBuilder.buildAlways(definition, item) : itemLoreBuilder.buildBase(definition, item))
                : List.of();

        final List<Component> lore = new ArrayList<>();
        if (!gemLore.isEmpty()) {
            lore.addAll(gemLore);
        }
        if (!harvesterLore.isEmpty()) {
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }
            lore.addAll(harvesterLore);
        }
        if (!baseLore.isEmpty()) {
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }
            lore.addAll(baseLore);
        }

        meta.lore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    private String currentLocale() {
        return normalizeLocale(configService.read().localization().defaultLocale());
    }

    private String normalizeLocale(final String locale) {
        if (locale == null || locale.isBlank()) {
            return "RU";
        }
        return locale.trim().toUpperCase(Locale.ROOT);
    }
}
