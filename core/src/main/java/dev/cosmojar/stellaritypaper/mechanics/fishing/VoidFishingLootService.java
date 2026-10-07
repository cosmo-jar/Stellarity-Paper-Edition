package dev.cosmojar.stellaritypaper.mechanics.fishing;

import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.mechanics.fishing.VoidFishingLootRegistry.LootEntry;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class VoidFishingLootService {

    private final FeatureFlags featureFlags;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final CustomItemFactory customItemFactory;
    private final dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService customLootService;

    public VoidFishingLootService(
            final FeatureFlags featureFlags,
            final ItemDefinitionRegistry itemDefinitionRegistry,
            final CustomItemFactory customItemFactory,
            final dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService customLootService
    ) {
        this.featureFlags = featureFlags;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.customItemFactory = customItemFactory;
        this.customLootService = customLootService;
    }

    /**
     * Роллит улов на основе уровня удачи игрока и биома.
     */
    public ItemStack rollLoot(final int luckLevel, final Biome biome) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        final boolean catchFisher = random.nextDouble() <= 0.03D;
        final boolean catchPuffer = random.nextDouble() <= 0.01D;

        if (catchFisher && catchPuffer) {
            final Optional<ItemStack> treasure = customLootService != null
                    ? customLootService.rollWeightedEntry(customLootService.getGlobalTreasure())
                    : Optional.empty();
            return treasure.orElseGet(() -> buildItemStack(rollEntry(VoidFishingLootRegistry.TREASURE, random), random));
        }
        if (catchFisher) {
            final Optional<CustomItemDefinition> def = itemDefinitionRegistry.findByPdcItemId("fisher_of_voids");
            return def.map(customItemFactory::create).orElseGet(() -> new ItemStack(Material.FISHING_ROD));
        }
        if (catchPuffer) {
            return new ItemStack(Material.PUFFERFISH);
        }

        final String category = rollCategory(luckLevel, random);

        switch (category) {
            case "JUNK":
                return buildItemStack(rollEntry(VoidFishingLootRegistry.JUNK, random), random);
            case "TREASURE":
                if (customLootService != null && !customLootService.getGlobalTreasure().isEmpty()) {
                    final Optional<ItemStack> itemOpt = customLootService.rollWeightedEntry(customLootService.getGlobalTreasure());
                    if (itemOpt.isPresent()) {
                        return itemOpt.get();
                    }
                }
                return buildItemStack(rollEntry(VoidFishingLootRegistry.TREASURE, random), random);
            case "FISH":
            default:
                return rollFish(biome, random);
        }
    }

    private String rollCategory(final int luckLevel, final ThreadLocalRandom random) {
        final int clampedLuck = Math.max(0, Math.min(luckLevel, 10));

        final int junkWeight = Math.max(5, 15 - clampedLuck);
        final int treasureWeight = Math.min(14, 4 + clampedLuck);
        final int fishWeight = Math.max(60, 70 - clampedLuck);

        final int total = junkWeight + treasureWeight + fishWeight;
        final int roll = random.nextInt(total);

        if (roll < junkWeight) {
            return "JUNK";
        } else if (roll < junkWeight + treasureWeight) {
            return "TREASURE";
        } else {
            return "FISH";
        }
    }

    private LootEntry rollEntry(final List<LootEntry> entries, final ThreadLocalRandom random) {
        int totalWeight = 0;
        for (final LootEntry entry : entries) {
            totalWeight += entry.weight();
        }
        int roll = random.nextInt(totalWeight);
        for (final LootEntry entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry;
            }
        }
        return entries.get(0);
    }

    private ItemStack rollFish(final Biome biome, final ThreadLocalRandom random) {
        final boolean allocationMode = featureFlags.isVoidFishingBiomeAllocation();

        if (allocationMode) {
            final String biomeKey = biome != null ? biome.getKey().getKey().toLowerCase(Locale.ROOT) : "original_biomes";
            if (customLootService != null) {
                final var biomeEntries = customLootService.getBiomeFish(biomeKey);
                if (!biomeEntries.isEmpty()) {
                    final Optional<ItemStack> itemOpt = customLootService.rollWeightedEntry(biomeEntries);
                    if (itemOpt.isPresent()) {
                        final ItemStack item = itemOpt.get();
                        applySpecialFishingStack(item, random);
                        return item;
                    }
                }
            }
            final List<LootEntry> biomePool = getBiomeLootPool(biome);
            final List<LootEntry> merged = new ArrayList<>();
            merged.addAll(VoidFishingLootRegistry.GLOBAL_FISH);
            merged.addAll(biomePool);
            return buildItemStack(rollEntry(merged, random), random);
        } else {
            if (customLootService != null && !customLootService.getGlobalFish().isEmpty()) {
                final Optional<ItemStack> itemOpt = customLootService.rollWeightedEntry(customLootService.getGlobalFish());
                if (itemOpt.isPresent()) {
                    final ItemStack item = itemOpt.get();
                    applySpecialFishingStack(item, random);
                    return item;
                }
            }
            final int roll = random.nextInt(49);
            if (roll < 19) {
                return buildItemStack(rollEntry(VoidFishingLootRegistry.GLOBAL_FISH, random), random);
            } else {
                final List<List<LootEntry>> allBiomes = List.of(
                        VoidFishingLootRegistry.AMETHYST_FOREST,
                        VoidFishingLootRegistry.FIERY_HILLS,
                        VoidFishingLootRegistry.FLESH_TUNDRA,
                        VoidFishingLootRegistry.FROZEN_SPIKES,
                        VoidFishingLootRegistry.PRISMARINE_FOREST,
                        VoidFishingLootRegistry.THE_HALLOW,
                        VoidFishingLootRegistry.ENDLESS_DUNES,
                        VoidFishingLootRegistry.WARPED_MARSH,
                        VoidFishingLootRegistry.ORIGINAL_BIOMES
                );
                final List<LootEntry> selectedBiome = allBiomes.get(random.nextInt(allBiomes.size()));
                return buildItemStack(rollEntry(selectedBiome, random), random);
            }
        }
    }

    private List<LootEntry> getBiomeLootPool(final Biome biome) {
        if (biome == null) {
            return VoidFishingLootRegistry.ORIGINAL_BIOMES;
        }
        final String name = biome.getKey().getKey().toUpperCase(java.util.Locale.ROOT);
        if (name.contains("AMETHYST") || name.contains("CRYSTAL_CRAGS")) {
            return VoidFishingLootRegistry.AMETHYST_FOREST;
        } else if (name.contains("FIERY") || name.contains("FIRE")) {
            return VoidFishingLootRegistry.FIERY_HILLS;
        } else if (name.contains("FLESH")) {
            return VoidFishingLootRegistry.FLESH_TUNDRA;
        } else if (name.contains("FROZEN") || name.contains("ICE")) {
            return VoidFishingLootRegistry.FROZEN_SPIKES;
        } else if (name.contains("PRISMARINE")) {
            return VoidFishingLootRegistry.PRISMARINE_FOREST;
        } else if (name.contains("HALLOW") || name.contains("CHERRY")) {
            return VoidFishingLootRegistry.THE_HALLOW;
        } else if (name.contains("DUNES")) {
            return VoidFishingLootRegistry.ENDLESS_DUNES;
        } else if (name.contains("MARSH")) {
            return VoidFishingLootRegistry.WARPED_MARSH;
        } else {
            return VoidFishingLootRegistry.ORIGINAL_BIOMES;
        }
    }

    private void applySpecialFishingStack(final ItemStack item, final ThreadLocalRandom random) {
        if (item == null) {
            return;
        }
        if (isCustomItem(item, "goosh")) {
            item.setAmount(random.nextInt(6, 9));
        } else if (isCustomItem(item, "ender_koi") && random.nextDouble() <= 0.125D) {
            item.setAmount(random.nextInt(2, 5));
        }
    }

    private boolean isCustomItem(final ItemStack item, final String expectedPdcId) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        final ItemMeta meta = item.getItemMeta();
        final String val = meta.getPersistentDataContainer().get(
                new NamespacedKey("stellarity", "item"),
                PersistentDataType.STRING
        );
        return expectedPdcId.equalsIgnoreCase(val);
    }

    private ItemStack buildItemStack(final LootEntry entry, final ThreadLocalRandom random) {
        final int count = (entry.minCount() == entry.maxCount())
                ? entry.minCount()
                : random.nextInt(entry.minCount(), entry.maxCount() + 1);

        if (entry.isCustom()) {
            if ("ender_koi".equals(entry.id())) {
                final int finalCount = (random.nextDouble() <= 0.125D) ? random.nextInt(2, 5) : 1;
                final Optional<CustomItemDefinition> def = itemDefinitionRegistry.findByPdcItemId("ender_koi");
                if (def.isPresent()) {
                    final ItemStack item = customItemFactory.create(def.get());
                    item.setAmount(finalCount);
                    return item;
                }
            }
            if ("goosh".equals(entry.id())) {
                final int finalCount = random.nextInt(6, 9);
                final Optional<CustomItemDefinition> def = itemDefinitionRegistry.findByPdcItemId("goosh");
                if (def.isPresent()) {
                    final ItemStack item = customItemFactory.create(def.get());
                    item.setAmount(finalCount);
                    return item;
                }
            }

            final Optional<CustomItemDefinition> def = itemDefinitionRegistry.findByPdcItemId(entry.id());
            if (def.isPresent()) {
                final ItemStack item = customItemFactory.create(def.get());
                item.setAmount(count);
                return item;
            }
            return new ItemStack(Material.CHORUS_FRUIT, count);
        }

        if (entry.isBook()) {
            return generateEnchantedBook(entry.minCount(), entry.maxCount(), random);
        }

        if (entry.isFirework()) {
            return generateFireworkStar(random);
        }

        final Material mat = Material.matchMaterial(entry.id());
        if (mat == null) {
            return new ItemStack(Material.AIR);
        }

        return new ItemStack(mat, count);
    }

    private ItemStack generateEnchantedBook(final int minLevel, final int maxLevel, final ThreadLocalRandom random) {
        final ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        final EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }

        final List<Enchantment> enchantments = new ArrayList<>();
        for (final Enchantment ench : Registry.ENCHANTMENT) {
            if (ench.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)) {
                enchantments.add(ench);
            }
        }

        if (enchantments.isEmpty()) {
            return book;
        }

        final int enchantCount = (maxLevel >= 30) ? random.nextInt(1, 4) : random.nextInt(1, 3);
        for (int i = 0; i < enchantCount; i++) {
            final Enchantment ench = enchantments.get(random.nextInt(enchantments.size()));
            final int maxLvl = ench.getMaxLevel();
            int level = 1;
            if (maxLvl > 1) {
                if (maxLevel >= 30) {
                    level = random.nextInt(Math.max(1, maxLvl / 2), maxLvl + 1);
                } else {
                    level = random.nextInt(1, Math.max(2, maxLvl));
                }
            }
            meta.addStoredEnchant(ench, level, true);
        }

        book.setItemMeta(meta);
        return book;
    }

    private ItemStack generateFireworkStar(final ThreadLocalRandom random) {
        final ItemStack star = new ItemStack(Material.FIREWORK_STAR);
        final FireworkEffectMeta meta = (FireworkEffectMeta) star.getItemMeta();
        if (meta == null) {
            return star;
        }

        final Color color = (random.nextBoolean()) ? Color.fromRGB(8073150) : Color.fromRGB(12801229);
        final Color fade = (color.asRGB() == 8073150) ? Color.fromRGB(12801229) : Color.fromRGB(8073150);

        final FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(color)
                .withFade(fade)
                .trail(random.nextBoolean())
                .flicker(random.nextBoolean())
                .build();

        meta.setEffect(effect);
        star.setItemMeta(meta);
        return star;
    }
}
