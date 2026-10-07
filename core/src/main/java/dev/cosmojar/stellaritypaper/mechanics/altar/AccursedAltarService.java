package dev.cosmojar.stellaritypaper.mechanics.altar;

import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.enchants.EnchantDataCodec;
import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.enchants.EnchantItemService;
import dev.cosmojar.stellaritypaper.items.CustomBlockService;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.text.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class AccursedAltarService implements Listener {

    public static final class AccursedRecipe {
        public final String resultCustomId;
        public final ItemStack resultVanillaItem;
        public final Map<String, Integer> ingredients;
        public final String parentIngredient;

        AccursedRecipe(final String resultCustomId, final Map<String, Integer> ingredients, final String parentIngredient) {
            this.resultCustomId = resultCustomId;
            this.resultVanillaItem = null;
            this.ingredients = ingredients;
            this.parentIngredient = parentIngredient;
        }

        AccursedRecipe(final ItemStack resultVanillaItem, final Map<String, Integer> ingredients, final String parentIngredient) {
            this.resultCustomId = null;
            this.resultVanillaItem = resultVanillaItem;
            this.ingredients = ingredients;
            this.parentIngredient = parentIngredient;
        }

        public String getResultCustomId() { return resultCustomId; }
        public ItemStack getResultVanillaItem() { return resultVanillaItem; }
        public Map<String, Integer> getIngredients() { return ingredients; }
        public String getParentIngredient() { return parentIngredient; }
    }

    private final Plugin plugin;
    private final FeatureFlags featureFlags;
    private final CustomBlockService customBlockService;
    private final ItemStateRepository itemStateRepository;
    private final CustomItemFactory customItemFactory;
    private final ItemDefinitionRegistry itemRegistry;
    private final MessageService messageService;
    private final EnchantDataCodec enchantDataCodec;
    private final EnchantItemService enchantItemService;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;

    private final Set<Location> staticAltars = new HashSet<>();
    private final List<AccursedRecipe> recipes = new ArrayList<>();

    public AccursedAltarService(
            final Plugin plugin,
            final FeatureFlags featureFlags,
            final CustomBlockService customBlockService,
            final ItemStateRepository itemStateRepository,
            final CustomItemFactory customItemFactory,
            final ItemDefinitionRegistry itemRegistry,
            final MessageService messageService,
            final EnchantDataCodec enchantDataCodec,
            final EnchantItemService enchantItemService,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.plugin = plugin;
        this.featureFlags = featureFlags;
        this.customBlockService = customBlockService;
        this.itemStateRepository = itemStateRepository;
        this.customItemFactory = customItemFactory;
        this.itemRegistry = itemRegistry;
        this.messageService = messageService;
        this.enchantDataCodec = enchantDataCodec;
        this.enchantItemService = enchantItemService;
        this.advancementService = advancementService;

        addShulkerRecipe("shulker_pickaxe", "netherite_pickaxe");
        addShulkerRecipe("shulker_axe", "netherite_axe");
        addShulkerRecipe("shulker_shovel", "netherite_shovel");
        addShulkerRecipe("shulker_hoe", "netherite_hoe");
        addShulkerRecipe("shulker_sword", "netherite_sword");
        addShulkerRecipe("shulker_spear", "netherite_spear");

        addShulkerRecipe("shulker_helmet", "netherite_helmet");
        addShulkerRecipe("shulker_chestplate", "netherite_chestplate");
        addShulkerRecipe("shulker_leggings", "netherite_leggings");
        addShulkerRecipe("shulker_boots", "netherite_boots");

        addHallowedRecipe("hallowed_helmet", "netherite_helmet");
        addHallowedRecipe("hallowed_chestplate", "netherite_chestplate");
        addHallowedRecipe("hallowed_leggings", "netherite_leggings");
        addHallowedRecipe("hallowed_boots", "netherite_boots");

        addChampionRecipe("champion_helmet", "netherite_helmet");
        addChampionRecipe("champion_chestplate", "netherite_chestplate");
        addChampionRecipe("champion_leggings", "netherite_leggings");
        addChampionRecipe("champion_boots", "netherite_boots");

        addFloralRecipe("floral_helmet", "netherite_helmet");
        addFloralRecipe("floral_chestplate", "netherite_chestplate");
        addFloralRecipe("floral_leggings", "netherite_leggings");
        addFloralRecipe("floral_boots", "netherite_boots");

        recipes.add(new AccursedRecipe("tamaris", Map.of(
                "enderite_smithing_template", 1,
                "netherite_sword", 1,
                "enderite_shard", 8,
                "wither_skeleton_skull", 1
        ), "netherite_sword"));

        recipes.add(new AccursedRecipe("spectral_fury", Map.of(
                "enderite_smithing_template", 1,
                "diamond", 3,
                "phantom_membrane", 8,
                "sharanga", 1
        ), "sharanga"));

        recipes.add(new AccursedRecipe("sandstorm_trident", Map.of(
                "enderite_smithing_template", 1,
                "sand_rune", 1,
                "trident", 1,
                "breeze_rod", 2
        ), "trident"));

        recipes.add(new AccursedRecipe("crest_of_the_end", Map.of(
                "enderite_smithing_template", 1,
                "shield", 1,
                "netherite_ingot", 1
        ), "shield"));

        recipes.add(new AccursedRecipe("chorus_plating", Map.of(
                "iron_ingot", 1,
                "popped_chorus_fruit", 2
        ), null));

        recipes.add(new AccursedRecipe("book_of_jinx", Map.of(
                "book", 1,
                "chorus_plating", 8,
                "enderite_shard", 10
        ), null));

        recipes.add(new AccursedRecipe("book_of_conveyance", Map.of(
                "book", 1,
                "ender_pearl", 16,
                "enderite_shard", 32
        ), null));

        recipes.add(new AccursedRecipe("satchel_of_voids", Map.of(
                "bundle", 1,
                "nether_star", 2,
                "enderite_shard", 64,
                "starlight_soot", 64
        ), null));

        recipes.add(new AccursedRecipe("endonomicon", Map.of(
                "misc_empty_enchanted_book", 1
        ), null));

        recipes.add(new AccursedRecipe("empress_wings", Map.of(
                "elytra", 1,
                "starlight_soot", 32
        ), "elytra"));

        recipes.add(new AccursedRecipe("dragon_wings", Map.of(
                "elytra", 1,
                "dragon_breath", 8,
                "enderite_shard", 16
        ), "elytra"));

        recipes.add(new AccursedRecipe("phantom_wings", Map.of(
                "elytra", 1,
                "phantom_membrane", 32
        ), "elytra"));

        recipes.add(new AccursedRecipe("elytra_dyeing", Map.of(
                "elytra", 1,
                "red_dye", 1
        ), "elytra"));

        Bukkit.getScheduler().runTaskLater(plugin, this::loadLoadedAltars, 20L);

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAltarLogic, 20L, 20L);
    }

    public List<AccursedRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public boolean isEnabled() {
        return featureFlags.isAccursedAltarEnabled();
    }

    private void addShulkerRecipe(final String result, final String parent) {
        recipes.add(new AccursedRecipe(result, Map.of(
                "enderite_smithing_template", 1,
                "shulker_shell", 4,
                parent, 1
        ), parent));
    }

    private void addHallowedRecipe(final String result, final String parent) {
        recipes.add(new AccursedRecipe(result, Map.of(
                "enderite_smithing_template", 1,
                "hallowed_ingot", 4,
                parent, 1
        ), parent));
    }

    private void addChampionRecipe(final String result, final String parent) {
        recipes.add(new AccursedRecipe(result, Map.of(
                "enderite_smithing_template", 1,
                "chorus_plating", 4,
                parent, 1
        ), parent));
    }

    private void addFloralRecipe(final String result, final String parent) {
        recipes.add(new AccursedRecipe(result, Map.of(
                "enderite_smithing_template", 1,
                "cherry_leaves", 8,
                parent, 1
        ), parent));
    }

    public void handleDrop(final PlayerDropItemEvent event) {
    }

    private void tickAltarLogic() {
        if (!featureFlags.isAccursedAltarEnabled()) {
            return;
        }

        for (final Location loc : new ArrayList<>(staticAltars)) {
            if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                if (loc.getBlock().getType() != org.bukkit.Material.END_PORTAL_FRAME) {
                    staticAltars.remove(loc);
                    continue;
                }
                processAltarLocation(loc.clone().add(0.5D, 1.1D, 0.5D), false);
            }
        }

        for (final World world : Bukkit.getWorlds()) {
            for (final org.bukkit.entity.Marker marker : world.getEntitiesByClass(org.bukkit.entity.Marker.class)) {
                if (marker.getScoreboardTags().contains("stellarity.altar_of_the_accursed")) {
                    processAltarLocation(marker.getLocation(), true);
                }
            }
        }
    }

    private void processAltarLocation(final Location loc, final boolean isMobile) {
        if (!featureFlags.isAccursedAltarEnabled()) {
            return;
        }

        final World world = loc.getWorld();
        if (world == null) {
            return;
        }

        final Collection<Entity> entities = world.getNearbyEntities(loc, 1.5D, 1.5D, 1.5D);
        final List<Item> items = new ArrayList<>();
        
        for (final Entity entity : entities) {
            if (entity instanceof Item itemEntity) {
                final ItemStack stack = itemEntity.getItemStack();
                if (stack.getType() != Material.AIR) {
                    items.add(itemEntity);
                }
            }
        }

        if (items.isEmpty()) {
            return;
        }

        for (final Item itemEntity : items) {
            if (!itemEntity.getScoreboardTags().contains("stellarity.altar_of_the_accursed.skip") 
                    && itemEntity.getItemStack().getType() != Material.ENDER_EYE) {
                itemEntity.setGlowing(true);
                itemEntity.setPickupDelay(30);
            }
        }

        boolean playerSneaking = false;
        for (final Entity nearby : world.getNearbyEntities(loc, 3.0D, 3.0D, 3.0D)) {
            if (nearby instanceof Player player && player.isSneaking()) {
                playerSneaking = true;
                break;
            }
        }

        if (playerSneaking) {
            for (final Item itemEntity : items) {
                itemEntity.setGlowing(false);
                itemEntity.setPickupDelay(0);
            }
            return;
        }

        final Map<String, List<Item>> grouped = new HashMap<>();
        final Map<String, Integer> availableCounts = new HashMap<>();

        for (final Item itemEntity : items) {
            final ItemStack stack = itemEntity.getItemStack();
            final String customId = itemStateRepository.getItemId(stack).orElse(null);
            String key = (customId != null) ? customId : stack.getType().name().toLowerCase(Locale.ROOT);
            
            if (key.endsWith("_bundle") || "bundle".equals(key)) {
                key = "bundle";
            }
            if ("enchanted_book".equals(key)) {
                key = "misc_empty_enchanted_book";
            }
            
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(itemEntity);
            availableCounts.put(key, availableCounts.getOrDefault(key, 0) + stack.getAmount());
        }

        if (availableCounts.getOrDefault("void_pendant", 0) >= 1 && availableCounts.getOrDefault("enderite_shard", 0) >= 1) {
            final List<String> gems = List.of(
                    "amethyst_shard", "copper_ingot", "diamond", "emerald",
                    "gold_ingot", "iron_ingot", "lapis_lazuli", "netherite_ingot", "quartz"
            );
            String foundGem = null;
            for (final String gem : gems) {
                if (availableCounts.getOrDefault(gem, 0) >= 1) {
                    foundGem = gem;
                    break;
                }
            }

            if (foundGem != null) {
                final String gemName = switch (foundGem) {
                    case "amethyst_shard" -> "amethyst";
                    case "copper_ingot" -> "copper";
                    case "gold_ingot" -> "gold";
                    case "iron_ingot" -> "iron";
                    case "lapis_lazuli" -> "lapis";
                    case "netherite_ingot" -> "netherite";
                    default -> foundGem;
                };
                final Item pendantEntity = grouped.get("void_pendant").get(0);
                final ItemStack pendantStack = pendantEntity.getItemStack();
                
                consumeIngredient(grouped, "void_pendant", 1);
                consumeIngredient(grouped, "enderite_shard", 1);
                consumeIngredient(grouped, foundGem, 1);

                final ItemStack resultPendant = pendantStack.clone();
                resultPendant.setAmount(1);

                applyVoidPendantUpgrade(resultPendant, gemName);
                spawnResult(loc, resultPendant);
                playAltarEffects(loc);
                return;
            }
        }

        final String elytraKey;
        if (availableCounts.getOrDefault("elytra", 0) >= 1) {
            elytraKey = "elytra";
        } else if (availableCounts.getOrDefault("empress_wings", 0) >= 1) {
            elytraKey = "empress_wings";
        } else {
            elytraKey = null;
        }

        if (elytraKey != null) {
            if (availableCounts.getOrDefault("water_bucket", 0) >= 1) {
                final Item elytraEntity = grouped.get(elytraKey).get(0);
                final ItemStack elytraStack = elytraEntity.getItemStack();
                
                if (elytraStack.hasItemMeta() && elytraStack.getItemMeta().getPersistentDataContainer()
                        .has(new org.bukkit.NamespacedKey(plugin, "elytra_color"), PersistentDataType.STRING)) {
                    
                    consumeIngredient(grouped, elytraKey, 1);
                    consumeIngredient(grouped, "water_bucket", 1);

                    final ItemStack resultElytra = elytraStack.clone();
                    resultElytra.setAmount(1);
                    final ItemMeta meta = resultElytra.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().remove(new org.bukkit.NamespacedKey(plugin, "elytra_color"));
                        final List<Component> lore = meta.lore();
                        if (lore != null && !lore.isEmpty()) {
                            lore.removeIf(comp -> comp instanceof net.kyori.adventure.text.TranslatableComponent tc 
                                    && "item.dyed".equals(tc.key()));
                            meta.lore(lore);
                        }
                        resultElytra.setItemMeta(meta);
                    }

                    spawnResult(loc, resultElytra);
                    spawnResult(loc, new ItemStack(Material.BUCKET));
                    
                    loc.getWorld().spawnParticle(Particle.SPLASH, loc, 32, 0.25D, 0.25D, 0.25D, 1.0D);
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
                    return;
                }
            }

            String foundDye = null;
            for (final String key : availableCounts.keySet()) {
                if (key.endsWith("_dye") && availableCounts.get(key) >= 1) {
                    foundDye = key;
                    break;
                }
            }

            if (foundDye != null) {
                final Item elytraEntity = grouped.get(elytraKey).get(0);
                final ItemStack elytraStack = elytraEntity.getItemStack();

                consumeIngredient(grouped, elytraKey, 1);
                consumeIngredient(grouped, foundDye, 1);

                final ItemStack resultElytra = elytraStack.clone();
                resultElytra.setAmount(1);

                final String colorName = foundDye.replace("_dye", "");
                final String hexColor = getDyeHexColor(colorName);

                final ItemMeta meta = resultElytra.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "elytra_color"), PersistentDataType.STRING, colorName);
                    
                    List<Component> lore = meta.lore();
                    if (lore == null) {
                        lore = new ArrayList<>();
                    } else {
                        lore.removeIf(comp -> comp instanceof net.kyori.adventure.text.TranslatableComponent tc 
                                && "item.dyed".equals(tc.key()));
                    }
                    
                    final Component dyedLine = Component.translatable("item.dyed")
                            .color(TextColor.fromHexString(hexColor))
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, true);
                    lore.add(0, dyedLine);
                    meta.lore(lore);
                    resultElytra.setItemMeta(meta);
                }

                spawnResult(loc, resultElytra);
                playAltarEffects(loc);
                return;
            }
        }

        for (final AccursedRecipe recipe : recipes) {
            if (isMobile && "satchel_of_voids".equals(recipe.resultCustomId)) {
                continue;
            }
            boolean matched = true;
            for (final Map.Entry<String, Integer> entry : recipe.ingredients.entrySet()) {
                if (availableCounts.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                ItemStack parentItemStack = null;
                if (recipe.parentIngredient != null) {
                    final List<Item> parentList = grouped.get(recipe.parentIngredient);
                    if (parentList != null && !parentList.isEmpty()) {
                        parentItemStack = parentList.get(0).getItemStack().clone();
                    }
                }

                for (final Map.Entry<String, Integer> entry : recipe.ingredients.entrySet()) {
                    consumeIngredient(grouped, entry.getKey(), entry.getValue());
                }

                ItemStack resultItem;
                if (recipe.resultCustomId != null) {
                    final CustomItemDefinition def = itemRegistry.findByPdcItemId(recipe.resultCustomId).orElse(null);
                    if (def != null) {
                        resultItem = customItemFactory.create(def);
                    } else {
                        continue;
                    }
                } else if (recipe.resultVanillaItem != null) {
                    final CustomItemDefinition def = itemRegistry.findByPdcItemId("enderite_smithing_template").orElse(null);
                    if (def != null) {
                        resultItem = customItemFactory.create(def);
                        resultItem.setAmount(2);
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }

                if (parentItemStack != null) {
                    final ItemMeta meta = resultItem.getItemMeta();
                    final ItemMeta parentMeta = parentItemStack.getItemMeta();
                    if (meta != null && parentMeta != null) {
                        for (final Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : parentMeta.getEnchants().entrySet()) {
                            meta.addEnchant(entry.getKey(), entry.getValue(), true);
                        }
                        
                        if (meta instanceof Damageable damageable && parentMeta instanceof Damageable parentDamageable) {
                            damageable.setDamage(parentDamageable.getDamage());
                        }

                        if (meta instanceof ArmorMeta armorMeta && parentMeta instanceof ArmorMeta parentArmorMeta) {
                            if (parentArmorMeta.hasTrim()) {
                                armorMeta.setTrim(parentArmorMeta.getTrim());
                            }
                        }

                        if (parentMeta.hasDisplayName()) {
                            meta.displayName(parentMeta.displayName());
                        }

                        resultItem.setItemMeta(meta);
                    }
                }

                spawnResult(loc, resultItem);
                playAltarEffects(loc);
                return;
            }
        }
    }

    private void consumeIngredient(final Map<String, List<Item>> grouped, final String key, final int count) {
        final List<Item> list = grouped.get(key);
        if (list == null) {
            return;
        }

        int remaining = count;
        final Iterator<Item> it = list.iterator();
        while (it.hasNext() && remaining > 0) {
            final Item itemEntity = it.next();
            final ItemStack stack = itemEntity.getItemStack();
            final int amt = stack.getAmount();

            if (amt <= remaining) {
                remaining -= amt;
                itemEntity.remove();
                it.remove();
            } else {
                stack.setAmount(amt - remaining);
                itemEntity.setItemStack(stack);
                remaining = 0;
            }
        }
    }

    private void applyVoidPendantUpgrade(final ItemStack pendant, final String gem) {
        final List<EnchantInstance> enchants = new ArrayList<>(enchantDataCodec.decode(itemStateRepository.getEnchantsData(pendant).orElse("")));
        enchants.removeIf(instance -> instance.id().startsWith("_technical/void_pendant/"));
        enchants.add(new EnchantInstance("_technical/void_pendant/" + gem, 1));
        itemStateRepository.setEnchantsData(pendant, enchantDataCodec.encode(enchants));
        enchantItemService.rebuildNow(pendant);
    }

    private void spawnResult(final Location loc, final ItemStack item) {
        final Item spawned = loc.getWorld().dropItem(loc.clone().add(0, 0.5D, 0), item);
        spawned.setPickupDelay(10);
        spawned.addScoreboardTag("stellarity.altar_of_the_accursed.skip");
    }

    private void playAltarEffects(final Location loc) {
        playAltarEffects(loc, false);
    }

    private void playAltarEffects(final Location loc, final boolean isElytraDyeing) {
        final World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.WITCH, loc, 32, 0.25D, 0.25D, 0.25D, 0.0D);
        world.spawnParticle(Particle.PORTAL, loc, 32, 0.25D, 0.25D, 0.25D, 0.1D);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.2F);
        
        if (ThreadLocalRandom.current().nextBoolean()) {
            world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.5F, 1.5F);
        }

        if (advancementService != null) {
            for (final Entity entity : world.getNearbyEntities(loc, 10.0D, 10.0D, 10.0D)) {
                if (entity instanceof Player player) {
                    advancementService.grant(player, "stellarity:altar_of_the_accursed/an_introduction_to_dark_magic");
                    advancementService.grant(player, "stellarity:altar_of_the_accursed/cursed_crafting");
                    if (isElytraDyeing) {
                        advancementService.grant(player, "stellarity:altar_of_the_accursed/dye_elytra");
                    }
                }
            }
        }
    }

    private String getDyeHexColor(final String dyeColor) {
        return switch (dyeColor.toLowerCase(Locale.ROOT)) {
            case "white" -> "#FFFFFF";
            case "light_gray" -> "#C9C9C9";
            case "gray" -> "#646464";
            case "black" -> "#1D1D21";
            case "brown" -> "#814500";
            case "red" -> "#FF0000";
            case "orange" -> "#FF8800";
            case "yellow" -> "#FFFF00";
            case "lime" -> "#00E90C";
            case "green" -> "#147900";
            case "cyan" -> "#00B9B0";
            case "light_blue" -> "#00D9FF";
            case "blue" -> "#003BB9";
            case "purple" -> "#8E00B9";
            case "magenta" -> "#E900A3";
            case "pink" -> "#FF6DCE";
            default -> "#FFFFFF";
        };
    }

    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent event) {
        for (final Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemDisplay display) {
                final String blockId = customBlockService.getBlockId(display);
                if ("altar_of_the_accursed".equals(blockId)) {
                    staticAltars.add(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation());
                }
            }
        }
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        staticAltars.removeIf(loc -> loc.getWorld().equals(event.getWorld()) && 
            (loc.getBlockX() >> 4) == event.getChunk().getX() && 
            (loc.getBlockZ() >> 4) == event.getChunk().getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.isCancelled() && event.getBlock().getType() != org.bukkit.Material.AIR) {
            return;
        }
        staticAltars.remove(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(final org.bukkit.event.entity.EntitySpawnEvent event) {
        if (event.getEntity() instanceof ItemDisplay display) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (display.isValid()) {
                    final String blockId = customBlockService.getBlockId(display);
                    if ("altar_of_the_accursed".equals(blockId)) {
                        staticAltars.add(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation());
                    }
                }
            });
        }
    }

    private void loadLoadedAltars() {
        for (final World world : Bukkit.getWorlds()) {
            for (final org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (final Entity entity : chunk.getEntities()) {
                    if (entity instanceof ItemDisplay display) {
                        final String blockId = customBlockService.getBlockId(display);
                        if ("altar_of_the_accursed".equals(blockId)) {
                            staticAltars.add(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation());
                        }
                    }
                }
            }
        }
    }
}
