package dev.cosmojar.stellaritypaper.mechanics.loot;

import dev.cosmojar.stellaritypaper.enchants.CustomEnchantedBookFactory;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class CustomLootService {

    public record StructureSettings(int maxCustomItems, boolean preventDuplicates) {}
    public record StructureInstanceKey(UUID worldId, String structureName, int startChunkX, int startChunkZ) {}

    private static final class StructureTracker {
        int itemsSpawned = 0;
        final Set<String> spawnedItemKeys = new HashSet<>();
    }

    private final Plugin plugin;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final CustomItemFactory customItemFactory;
    private final CustomEnchantedBookFactory bookFactory;
    private final ExplorerMapService explorerMapService;

    private final Map<String, Map<String, List<CustomLootEntry>>> structureLoot = new HashMap<>();

    private final Map<String, StructureSettings> structureSettings = new HashMap<>();

    private final Map<String, Map<String, List<CustomLootEntry>>> structureVaults = new HashMap<>();

    private final Map<String, Map<String, List<CustomLootEntry>>> structureTrialSpawners = new HashMap<>();

    private final Map<StructureInstanceKey, StructureTracker> instanceTrackers = new ConcurrentHashMap<>();

    private List<CustomLootEntry> globalFish = new ArrayList<>();
    private List<CustomLootEntry> globalTreasure = new ArrayList<>();
    private final Map<String, List<CustomLootEntry>> biomeFish = new HashMap<>();

    public CustomLootService(
            final Plugin plugin,
            final ItemDefinitionRegistry itemDefinitionRegistry,
            final CustomItemFactory customItemFactory,
            final CustomEnchantedBookFactory bookFactory,
            final ExplorerMapService explorerMapService
    ) {
        this.plugin = plugin;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.customItemFactory = customItemFactory;
        this.bookFactory = bookFactory;
        this.explorerMapService = explorerMapService;
        loadAll();
    }

    public void loadAll() {
        structureLoot.clear();
        structureSettings.clear();
        structureVaults.clear();
        structureTrialSpawners.clear();
        instanceTrackers.clear();

        globalFish.clear();
        globalTreasure.clear();
        biomeFish.clear();

        final File baseDir = new File(plugin.getDataFolder(), "CustomLootTables");
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        saveDefaultConfig("CustomLootTables/Stronghold.yml");
        saveDefaultConfig("CustomLootTables/EndCity.yml");
        saveDefaultConfig("CustomLootTables/EndVillage.yml");
        saveDefaultConfig("CustomLootTables/Shipwreck.yml");
        saveDefaultConfig("CustomLootTables/FloatingTreasure.yml");
        saveDefaultConfig("CustomLootTables/Campsite.yml");
        saveDefaultConfig("CustomLootTables/Chapel.yml");
        saveDefaultConfig("CustomLootTables/EndDungeon.yml");
        saveDefaultConfig("CustomLootTables/FishermanHut.yml");
        saveDefaultConfig("CustomLootTables/DesertRuin.yml");

        final File voidDir = new File(baseDir, "VoidFishing");
        if (!voidDir.exists()) {
            voidDir.mkdirs();
        }
        saveDefaultConfig("CustomLootTables/VoidFishing/VoidFishing.yml");

        loadStructureConfig(new File(baseDir, "Stronghold.yml"), "Stronghold");
        loadStructureConfig(new File(baseDir, "EndCity.yml"), "EndCity");
        loadStructureConfig(new File(baseDir, "EndVillage.yml"), "EndVillage");
        loadStructureConfig(new File(baseDir, "Shipwreck.yml"), "Shipwreck");
        loadStructureConfig(new File(baseDir, "FloatingTreasure.yml"), "FloatingTreasure");
        loadStructureConfig(new File(baseDir, "Campsite.yml"), "Campsite");
        loadStructureConfig(new File(baseDir, "Chapel.yml"), "Chapel");
        loadStructureConfig(new File(baseDir, "EndDungeon.yml"), "EndDungeon");
        loadStructureConfig(new File(baseDir, "FishermanHut.yml"), "FishermanHut");
        loadStructureConfig(new File(baseDir, "DesertRuin.yml"), "DesertRuin");

        loadVoidFishingConfig(new File(voidDir, "VoidFishing.yml"));
    }

    public void reload() {
        loadAll();
    }

    private void saveDefaultConfig(final String resourcePath) {
        final File outFile = new File(plugin.getDataFolder(), resourcePath);
        if (!outFile.exists()) {
            try {
                plugin.saveResource(resourcePath, false);
            } catch (final Exception e) {
                plugin.getLogger().warning("Could not save default resource " + resourcePath + ": " + e.getMessage());
            }
        } else {
            dev.cosmojar.stellaritypaper.config.ConfigAutoUpdater.update(plugin, resourcePath);
        }
    }

    private void loadStructureConfig(final File file, final String structureName) {
        if (!file.exists()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        final String sNameLower = structureName.toLowerCase(Locale.ROOT);

        final ConfigurationSection settingsSec = config.getConfigurationSection("settings");
        final int maxItems = settingsSec != null ? settingsSec.getInt("max_custom_items", 10) : 10;
        final boolean preventDuplicates = settingsSec != null && settingsSec.getBoolean("prevent_duplicates", true);
        structureSettings.put(sNameLower, new StructureSettings(maxItems, preventDuplicates));

        final ConfigurationSection containerSec = config.getConfigurationSection("containers");
        if (containerSec != null) {
            final Map<String, List<CustomLootEntry>> categoryMap = new HashMap<>();

            for (final String catKey : containerSec.getKeys(false)) {
                final List<Map<?, ?>> rawList = containerSec.getMapList(catKey);
                final List<CustomLootEntry> entries = new ArrayList<>();
                for (final Map<?, ?> map : rawList) {
                    final String itemKey = Objects.toString(map.get("item"), "");
                    final double chance = map.containsKey("chance") ? Double.parseDouble(map.get("chance").toString()) : 100.0;
                    final int weight = map.containsKey("weight") ? Integer.parseInt(map.get("weight").toString()) : 1;
                    if (!itemKey.isBlank()) {
                        entries.add(new CustomLootEntry(itemKey, chance, weight));
                    }
                }
                categoryMap.put(catKey.toUpperCase(Locale.ROOT), entries);
            }

            structureLoot.put(sNameLower, categoryMap);
        }

        final ConfigurationSection vaultsSec = config.getConfigurationSection("vaults");
        if (vaultsSec != null) {
            final Map<String, List<CustomLootEntry>> vaultsMap = new HashMap<>();
            for (final String vKey : vaultsSec.getKeys(false)) {
                final ConfigurationSection singleVault = vaultsSec.getConfigurationSection(vKey);
                if (singleVault != null) {
                    final List<CustomLootEntry> entries = parseLootEntries(singleVault.getMapList("loot"));
                    vaultsMap.put(vKey.toUpperCase(Locale.ROOT), entries);
                }
            }
            structureVaults.put(sNameLower, vaultsMap);
        }

        final ConfigurationSection trialSec = config.getConfigurationSection("trial_spawners");
        if (trialSec != null) {
            final Map<String, List<CustomLootEntry>> trialMap = new HashMap<>();
            parseTrialSpawnersRecursive(trialSec, "", trialMap);
            structureTrialSpawners.put(sNameLower, trialMap);
        }
    }

    private void parseTrialSpawnersRecursive(final ConfigurationSection section, final String prefix, final Map<String, List<CustomLootEntry>> outMap) {
        for (final String key : section.getKeys(false)) {
            final String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                final ConfigurationSection child = section.getConfigurationSection(key);
                if (child != null) {
                    if (child.contains("items")) {
                        final List<CustomLootEntry> entries = parseLootEntries(child.getMapList("items"));
                        outMap.put(fullKey.toLowerCase(Locale.ROOT), entries);
                    } else {
                        parseTrialSpawnersRecursive(child, fullKey, outMap);
                    }
                }
            }
        }
    }

    private void loadVoidFishingConfig(final File file) {
        if (!file.exists()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        globalFish = parseLootEntries(config.getMapList("global_fish"));
        globalTreasure = parseLootEntries(config.getMapList("global_treasure"));

        final ConfigurationSection biomesSec = config.getConfigurationSection("biomes");
        if (biomesSec != null) {
            for (final String biomeKey : biomesSec.getKeys(false)) {
                biomeFish.put(biomeKey.toLowerCase(Locale.ROOT), parseLootEntries(biomesSec.getMapList(biomeKey)));
            }
        }
    }

    private List<CustomLootEntry> parseLootEntries(final List<Map<?, ?>> rawList) {
        final List<CustomLootEntry> entries = new ArrayList<>();
        if (rawList == null) {
            return entries;
        }
        for (final Map<?, ?> map : rawList) {
            final String itemKey = Objects.toString(map.get("item"), "");
            final double chance = map.containsKey("chance") ? Double.parseDouble(map.get("chance").toString()) : 100.0;
            final int weight = map.containsKey("weight") ? Integer.parseInt(map.get("weight").toString()) : 1;
            if (!itemKey.isBlank()) {
                entries.add(new CustomLootEntry(itemKey, chance, weight));
            }
        }
        return entries;
    }

    public boolean hasStructureCategory(final String structureName, final String containerCategory) {
        if (structureName == null || containerCategory == null) {
            return false;
        }
        final Map<String, List<CustomLootEntry>> categoryMap = structureLoot.get(structureName.toLowerCase(Locale.ROOT));
        if (categoryMap == null) {
            return false;
        }
        final List<CustomLootEntry> entries = categoryMap.get(containerCategory.toUpperCase(Locale.ROOT));
        return entries != null && !entries.isEmpty();
    }

    public List<ItemStack> rollStructureLoot(final String structureName, final String containerCategory, final StructureInstanceKey instanceKey) {
        return rollStructureLoot(structureName, containerCategory, instanceKey, null);
    }

    public List<ItemStack> rollStructureLoot(final String structureName, final String containerCategory, final StructureInstanceKey instanceKey, final org.bukkit.Location originLoc) {
        final String sNameLower = structureName.toLowerCase(Locale.ROOT);
        final Map<String, List<CustomLootEntry>> categoryMap = structureLoot.get(sNameLower);
        if (categoryMap == null) {
            return List.of();
        }
        final List<CustomLootEntry> entries = categoryMap.get(containerCategory.toUpperCase(Locale.ROOT));
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        final StructureSettings settings = structureSettings.getOrDefault(sNameLower, new StructureSettings(10, true));
        final StructureTracker tracker = instanceKey != null
                ? instanceTrackers.computeIfAbsent(instanceKey, k -> new StructureTracker())
                : new StructureTracker();

        final int effectiveMax = settings.maxCustomItems();

        if (tracker.itemsSpawned >= effectiveMax) {
            return List.of();
        }

        final List<ItemStack> result = new ArrayList<>();
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        for (final CustomLootEntry entry : entries) {
            if (tracker.itemsSpawned >= effectiveMax) {
                break;
            }
            if (settings.preventDuplicates() && tracker.spawnedItemKeys.contains(entry.itemKey())) {
                continue;
            }

            final double roll = random.nextDouble(100.0);
            if (roll <= entry.chance()) {
                final org.bukkit.World w = originLoc != null ? originLoc.getWorld() : null;
                final Optional<ItemStack> itemOpt = createItemStack(entry.itemKey(), w, originLoc);
                if (itemOpt.isPresent()) {
                    result.add(itemOpt.get());
                    tracker.itemsSpawned++;
                    tracker.spawnedItemKeys.add(entry.itemKey());
                }
            }
        }
        return result;
    }

    public List<ItemStack> getVaultLoot(final String structureName, final String vaultCategory) {
        final Map<String, List<CustomLootEntry>> vaults = structureVaults.get(structureName.toLowerCase(Locale.ROOT));
        if (vaults == null) {
            return List.of();
        }
        final List<CustomLootEntry> entries = vaults.get(vaultCategory.toUpperCase(Locale.ROOT));
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        final List<ItemStack> result = new ArrayList<>();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (final CustomLootEntry entry : entries) {
            if (entry.chance() >= 100.0 || random.nextDouble(100.0) <= entry.chance()) {
                createItemStack(entry.itemKey()).ifPresent(result::add);
            }
        }
        return result;
    }

    public List<ItemStack> getTrialSpawnerLoot(final String structureName, final String spawnerPath) {
        final Map<String, List<CustomLootEntry>> trialMap = structureTrialSpawners.get(structureName.toLowerCase(Locale.ROOT));
        if (trialMap == null) {
            return List.of();
        }
        final List<CustomLootEntry> entries = trialMap.get(spawnerPath.toLowerCase(Locale.ROOT));
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        final List<ItemStack> result = new ArrayList<>();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (final CustomLootEntry entry : entries) {
            if (entry.chance() >= 100.0 || random.nextDouble(100.0) <= entry.chance()) {
                createItemStack(entry.itemKey()).ifPresent(result::add);
            }
        }
        return result;
    }

    public Optional<ItemStack> rollWeightedEntry(final List<CustomLootEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }
        int totalWeight = 0;
        for (final CustomLootEntry e : entries) {
            totalWeight += e.weight();
        }
        if (totalWeight <= 0) {
            return Optional.empty();
        }
        final int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int current = 0;
        for (final CustomLootEntry e : entries) {
            current += e.weight();
            if (roll < current) {
                return createItemStack(e.itemKey());
            }
        }
        return createItemStack(entries.get(0).itemKey());
    }

    public List<CustomLootEntry> getGlobalFish() {
        return globalFish;
    }

    public List<CustomLootEntry> getGlobalTreasure() {
        return globalTreasure;
    }

    public List<CustomLootEntry> getBiomeFish(final String biomeKey) {
        return biomeFish.getOrDefault(biomeKey.toLowerCase(Locale.ROOT), List.of());
    }

    public Optional<ItemStack> createItemStack(final String itemKey) {
        return createItemStack(itemKey, null, null);
    }

    public Optional<ItemStack> createItemStack(final String itemKey, final org.bukkit.World world, final org.bukkit.Location originLoc) {
        if (itemKey == null || itemKey.isBlank()) {
            return Optional.empty();
        }

        String effectiveKey = itemKey;

        int requestedAmount = 1;
        if (!effectiveKey.startsWith("enchantments_books:") && !effectiveKey.startsWith("trimmed_armor:")) {
            final int lastColon = effectiveKey.lastIndexOf(':');
            if (lastColon > 0) {
                final String possibleAmountStr = effectiveKey.substring(lastColon + 1);
                try {
                    final int parsed = Integer.parseInt(possibleAmountStr);
                    if (parsed > 0) {
                        requestedAmount = parsed;
                        effectiveKey = effectiveKey.substring(0, lastColon);
                    }
                } catch (final NumberFormatException ignored) {
                }
            }
        }

        Optional<ItemStack> stackOpt = Optional.empty();

        if (explorerMapService != null && explorerMapService.isMapItemKey(effectiveKey)) {
            stackOpt = explorerMapService.createExplorerMap(effectiveKey, world, originLoc);
        } else if (effectiveKey.startsWith("enchantments_books:")) {
            final String[] parts = effectiveKey.split(":");
            if (parts.length >= 2) {
                final String enchantId = parts[1];
                final int level = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
                stackOpt = bookFactory.createBook(enchantId, level);
                if (stackOpt.isEmpty()) {
                    final NamespacedKey key = enchantId.contains(":") ? NamespacedKey.fromString(enchantId) : NamespacedKey.minecraft(enchantId);
                    if (key != null) {
                        final org.bukkit.enchantments.Enchantment vanillaEnchant = org.bukkit.enchantments.Enchantment.getByKey(key);
                        if (vanillaEnchant != null) {
                            final ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                            if (book.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta meta) {
                                meta.addStoredEnchant(vanillaEnchant, level, true);
                                book.setItemMeta(meta);
                            }
                            stackOpt = Optional.of(book);
                        }
                    }
                }
            }
        } else if (effectiveKey.startsWith("trimmed_armor:")) {
            final String spec = effectiveKey.substring("trimmed_armor:".length());
            stackOpt = createTrimmedArmor(spec);
        } else if (effectiveKey.endsWith(":spire") || effectiveKey.endsWith(":spire_trim")) {
            final String raw = effectiveKey.replace(":spire_trim", "").replace(":spire", "");
            stackOpt = createTrimmedArmor(raw + ":spire");
        } else if (effectiveKey.startsWith("enchanted_equipment:")) {
            final String spec = effectiveKey.substring("enchanted_equipment:".length());
            stackOpt = createEnchantedEquipment(spec);
        } else if (effectiveKey.startsWith("tipped_arrow:levitation") || effectiveKey.equals("tipped_arrow_levitation")) {
            stackOpt = createTippedArrow("levitation", requestedAmount);
        } else if (effectiveKey.startsWith("ominous_bottle")) {
            int amp = 0;
            if (effectiveKey.contains(":")) {
                try {
                    amp = Integer.parseInt(effectiveKey.substring(effectiveKey.lastIndexOf(':') + 1));
                } catch (final NumberFormatException ignored) {
                }
            }
            stackOpt = createOminousBottle(amp, requestedAmount);
        } else {
            Optional<CustomItemDefinition> def = itemDefinitionRegistry.findByPdcItemId(effectiveKey);
            if (def.isPresent()) {
                stackOpt = Optional.of(customItemFactory.create(def.get(), null));
            } else {
                final String strippedKey = stripCategoryPrefix(effectiveKey);
                def = itemDefinitionRegistry.findByPdcItemId(strippedKey);
                if (def.isPresent()) {
                    stackOpt = Optional.of(customItemFactory.create(def.get(), null));
                } else {
                    final String rawMat = effectiveKey.toLowerCase(Locale.ROOT).replace("minecraft:", "");
                    if ("iron_spear".equals(rawMat)) {
                        final org.bukkit.Material m = org.bukkit.Material.matchMaterial("iron_spear");
                        final org.bukkit.Material fallback = m != null ? m : org.bukkit.Material.TRIDENT;
                        stackOpt = Optional.of(new ItemStack(fallback));
                    } else if ("diamond_spear".equals(rawMat)) {
                        final org.bukkit.Material m = org.bukkit.Material.matchMaterial("diamond_spear");
                        final org.bukkit.Material fallback = m != null ? m : org.bukkit.Material.TRIDENT;
                        stackOpt = Optional.of(new ItemStack(fallback));
                    } else if ("enderite_spear".equals(rawMat)) {
                        final Optional<CustomItemDefinition> spearDef = itemDefinitionRegistry.findByPdcItemId("shulker_spear");
                        if (spearDef.isPresent()) {
                            stackOpt = Optional.of(customItemFactory.create(spearDef.get(), null));
                        } else {
                            final org.bukkit.Material m = org.bukkit.Material.matchMaterial("enderite_spear");
                            final org.bukkit.Material fallback = m != null ? m : org.bukkit.Material.TRIDENT;
                            stackOpt = Optional.of(new ItemStack(fallback));
                        }
                    } else {
                        final org.bukkit.Material mat = org.bukkit.Material.matchMaterial(rawMat);
                        if (mat != null && mat != org.bukkit.Material.AIR) {
                            stackOpt = Optional.of(new ItemStack(mat));
                        }
                    }
                }
            }
        }

        if (stackOpt.isPresent()) {
            final ItemStack itemStack = stackOpt.get();
            if (requestedAmount > 1) {
                itemStack.setAmount(requestedAmount);
            }
            return Optional.of(itemStack);
        }

        plugin.getLogger().warning("[CustomLootService] Unknown item key or material: " + itemKey);
        return Optional.empty();
    }

    private Optional<ItemStack> createTrimmedArmor(final String spec) {
        final String[] parts = spec.split(":");
        if (parts.length < 2) {
            return Optional.empty();
        }
        final String matName = parts[0].toLowerCase(Locale.ROOT).replace("minecraft:", "");
        final Material mat = Material.matchMaterial(matName);
        if (mat == null) {
            return Optional.empty();
        }

        final ItemStack stack = new ItemStack(mat);
        final String patternName = parts[1].toLowerCase(Locale.ROOT);
        final TrimPattern pattern = resolveTrimPattern(patternName);

        final TrimMaterial trimMat;
        if (parts.length >= 3) {
            trimMat = resolveTrimMaterial(parts[2].toLowerCase(Locale.ROOT));
        } else {
            trimMat = getRandomStellarityTrimMaterial();
        }

        if (stack.getItemMeta() instanceof ArmorMeta armorMeta) {
            if (pattern != null && trimMat != null) {
                armorMeta.setTrim(new ArmorTrim(trimMat, pattern));
            }
            stack.setItemMeta(armorMeta);
        }

        try {
            final int level = ThreadLocalRandom.current().nextInt(20, 40);
            stack.enchantWithLevels(level, true, ThreadLocalRandom.current());
        } catch (final Throwable ignored) {
        }

        return Optional.of(stack);
    }

    private TrimPattern resolveTrimPattern(final String name) {
        if ("spire".equalsIgnoreCase(name)) {
            return TrimPattern.SPIRE;
        }
        try {
            final NamespacedKey key = name.contains(":") ? NamespacedKey.fromString(name) : NamespacedKey.minecraft(name);
            if (key != null) {
                final TrimPattern p = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).get(key);
                if (p != null) return p;
            }
        } catch (final Throwable ignored) {
        }
        return TrimPattern.SPIRE;
    }

    private TrimMaterial resolveTrimMaterial(final String name) {
        try {
            final NamespacedKey key = name.contains(":") ? NamespacedKey.fromString(name) : (
                    name.equals("enderite") || name.equals("chorus") ? new NamespacedKey("stellarity", name) : NamespacedKey.minecraft(name)
            );
            if (key != null) {
                final TrimMaterial m = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).get(key);
                if (m != null) return m;
            }
        } catch (final Throwable ignored) {
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "gold" -> TrimMaterial.GOLD;
            case "amethyst" -> TrimMaterial.AMETHYST;
            case "diamond" -> TrimMaterial.DIAMOND;
            case "copper" -> TrimMaterial.COPPER;
            case "emerald" -> TrimMaterial.EMERALD;
            case "netherite" -> TrimMaterial.NETHERITE;
            case "iron" -> TrimMaterial.IRON;
            default -> TrimMaterial.AMETHYST;
        };
    }

    private TrimMaterial getRandomStellarityTrimMaterial() {
        final int roll = ThreadLocalRandom.current().nextInt(5);
        return switch (roll) {
            case 0 -> resolveTrimMaterial("amethyst");
            case 1 -> resolveTrimMaterial("enderite");
            case 2 -> resolveTrimMaterial("gold");
            case 3 -> resolveTrimMaterial("chorus");
            default -> resolveTrimMaterial("diamond");
        };
    }

    private Optional<ItemStack> createEnchantedEquipment(final String matName) {
        final String rawMat = matName.toLowerCase(Locale.ROOT).replace("minecraft:", "");
        final Material mat = Material.matchMaterial(rawMat);
        if (mat == null) {
            return Optional.empty();
        }
        final ItemStack stack = new ItemStack(mat);
        try {
            final int level = ThreadLocalRandom.current().nextInt(20, 40);
            stack.enchantWithLevels(level, true, ThreadLocalRandom.current());
        } catch (final Throwable ignored) {
        }
        return Optional.of(stack);
    }

    private Optional<ItemStack> createTippedArrow(final String effectName, final int amount) {
        final ItemStack stack = new ItemStack(Material.TIPPED_ARROW, Math.max(1, amount));
        if (stack.getItemMeta() instanceof PotionMeta meta) {
            if ("levitation".equalsIgnoreCase(effectName)) {
                meta.addCustomEffect(new PotionEffect(PotionEffectType.LEVITATION, 1800, 0), true);
            }
            stack.setItemMeta(meta);
        }
        return Optional.of(stack);
    }

    private Optional<ItemStack> createOminousBottle(final int amplifier, final int amount) {
        final Material mat = Material.matchMaterial("ominous_bottle");
        if (mat == null) {
            return Optional.empty();
        }
        final ItemStack stack = new ItemStack(mat, Math.max(1, amount));
        if (stack.getItemMeta() instanceof OminousBottleMeta meta) {
            meta.setAmplifier(Math.max(0, Math.min(4, amplifier)));
            stack.setItemMeta(meta);
        }
        return Optional.of(stack);
    }

    private String stripCategoryPrefix(final String key) {
        final String[] prefixes = {
                "weapons_", "trinkets_", "potions_", "materials_", "food_fish_", "food_",
                "keys_", "spellbooks_", "misc_", "tools_", "blocks_", "armor_"
        };
        for (final String prefix : prefixes) {
            if (key.startsWith(prefix)) {
                return key.substring(prefix.length());
            }
        }
        return key;
    }
}
