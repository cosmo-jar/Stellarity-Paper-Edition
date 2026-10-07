package dev.cosmojar.stellaritypaper.mechanics.cauldron;

import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.text.TextService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Управляет логикой варки в котле (Cauldron Crafting):
 *  – Активация дыханием дракона
 *  – Всасывание предметов
 *  – Проверка рецептов и крафт
 *  – Визуальные/звуковые эффекты
 *  – Оптимизированный кэш локаций
 */
public final class CauldronCraftService implements Listener {

    private final Plugin plugin;
    private final Logger logger;
    private final FeatureFlags featureFlags;
    private final CauldronRecipeRegistry recipeRegistry;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final CustomItemFactory customItemFactory;
    private final ItemStateRepository itemStateRepository;
    private final TextService textService;

    private final Map<Location, Integer> activeCauldrons = new HashMap<>();
    private final Map<Location, List<ItemStack>> cauldronIngredients = new HashMap<>();

    private final dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationService consecrationService;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;

    public CauldronCraftService(
            final Plugin plugin,
            final FeatureFlags featureFlags,
            final CauldronRecipeRegistry recipeRegistry,
            final ItemDefinitionRegistry itemDefinitionRegistry,
            final CustomItemFactory customItemFactory,
            final ItemStateRepository itemStateRepository,
            final TextService textService,
            final dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationService consecrationService,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.featureFlags = featureFlags;
        this.recipeRegistry = recipeRegistry;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.customItemFactory = customItemFactory;
        this.itemStateRepository = itemStateRepository;
        this.textService = textService;
        this.consecrationService = consecrationService;
        this.advancementService = advancementService;

        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    private void tick() {
        if (!featureFlags.isCauldronCraftEnabled()) {
            return;
        }

        for (final Location loc : new ArrayList<>(activeCauldrons.keySet())) {
            try {
                final World world = loc.getWorld();
                if (world == null || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    continue;
                }

                if (loc.getBlock().getType() != Material.WATER_CAULDRON) {
                    dropAllIngredients(loc);
                    activeCauldrons.remove(loc);
                    cauldronIngredients.remove(loc);
                    continue;
                }

                final boolean hasPlayersNearby = world.getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(loc) <= 400.0);
                if (!hasPlayersNearby) {
                    continue;
                }

                absorbNearbyItems(loc);

                checkRecipe(loc);

                spawnBoilingParticles(loc);
            } catch (final Exception e) {
                logger.warning("[CauldronCraft] Exception in tick at " + loc + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }



    private boolean tryActivateOrRechargeCauldron(final Item dragonBreathItem) {
        final Location itemLoc = dragonBreathItem.getLocation();
        final Block block = itemLoc.getBlock();
        if (block.getType() != Material.WATER_CAULDRON) {
            return false;
        }

        final Location cauldronLoc = block.getLocation();
        final int currentCharges = activeCauldrons.getOrDefault(cauldronLoc, 0);

        if (currentCharges >= 7) {
            return false;
        }

        final ItemStack stack = dragonBreathItem.getItemStack();
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
            dragonBreathItem.setItemStack(stack);
        } else {
            dragonBreathItem.remove();
        }

        activeCauldrons.put(cauldronLoc, Math.min(7, currentCharges + 7));
        cauldronIngredients.putIfAbsent(cauldronLoc, new ArrayList<>());

        final World world = cauldronLoc.getWorld();
        if (world != null) {
            final Location center = cauldronLoc.clone().add(0.5, 0.45, 0.5);
            world.playSound(center, Sound.ITEM_BOTTLE_EMPTY, 1.0F, 1.0F);
            world.playSound(center, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 0.75F, 1.0F);
            world.spawnParticle(Particle.POOF, center, 10, 0.25, 0.0, 0.25, 0.0);
            world.spawnParticle(Particle.DRAGON_BREATH, center, 12, 0.2, 0.2, 0.2, 0.01);
        }

        return true;
    }

    private void absorbNearbyItems(final Location cauldronLoc) {
        final List<ItemStack> ingredients = cauldronIngredients.get(cauldronLoc);
        if (ingredients == null || ingredients.size() >= 6) {
            return;
        }

        final World world = cauldronLoc.getWorld();
        if (world == null) {
            return;
        }

        final Location searchCenter = cauldronLoc.clone().add(0.5, 0.8, 0.5);
        final Collection<Entity> nearbyEntities = world.getNearbyEntities(searchCenter, 1.0, 1.2, 1.0);

        for (final Entity entity : nearbyEntities) {
            if (!(entity instanceof Item itemEntity)) {
                continue;
            }

            if (itemEntity.getScoreboardTags().contains("stellarity.brewing.ignore")) {
                continue;
            }

            if (consecrationService != null && consecrationService.isHallowBiome(cauldronLoc)) {
                if (consecrationService.tryConsecrateItem(itemEntity, cauldronLoc)) {
                    continue;
                }
            }

            if (ingredients.size() >= 6) {
                break;
            }

            absorbSingleItem(itemEntity, ingredients, cauldronLoc, world);
        }
    }

    private void tryAbsorbItemIntoNearbyCauldron(final Item itemEntity) {
        if (!itemEntity.isValid()) {
            return;
        }
        final Location itemLoc = itemEntity.getLocation();

        for (final Map.Entry<Location, Integer> entry : activeCauldrons.entrySet()) {
            final Location cauldronLoc = entry.getKey();
            if (!cauldronLoc.getWorld().equals(itemLoc.getWorld())) {
                continue;
            }
            final double dx = Math.abs(itemLoc.getX() - (cauldronLoc.getX() + 0.5));
            final double dz = Math.abs(itemLoc.getZ() - (cauldronLoc.getZ() + 0.5));
            final double dy = itemLoc.getY() - cauldronLoc.getY();
            if (dx > 1.0 || dz > 1.0 || dy < -0.5 || dy > 2.0) {
                continue;
            }

            final List<ItemStack> ingredients = cauldronIngredients.get(cauldronLoc);
            if (ingredients == null || ingredients.size() >= 6) {
                continue;
            }

            if (itemEntity.getScoreboardTags().contains("stellarity.brewing.ignore")) {
                return;
            }

            final World world = cauldronLoc.getWorld();
            absorbSingleItem(itemEntity, ingredients, cauldronLoc, world);
            return;
        }
    }

    private void absorbSingleItem(final Item itemEntity, final List<ItemStack> ingredients,
                                   final Location cauldronLoc, final World world) {
        final ItemStack stack = itemEntity.getItemStack();

        if (stack.getType() == Material.DRAGON_BREATH) {
            tryActivateOrRechargeCauldron(itemEntity);
            return;
        }
        final ItemStack singleItem = stack.clone();
        singleItem.setAmount(1);
        ingredients.add(singleItem);

        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
            itemEntity.setItemStack(stack);
        } else {
            itemEntity.remove();
        }

        final Location center = cauldronLoc.clone().add(0.5, 0.6, 0.5);
        world.spawnParticle(Particle.SPLASH, center, 13, 0.2, 0.0, 0.2, 0.0);
        world.spawnParticle(Particle.DUST_PLUME, center, 10, 0.0, 0.0, 0.0, 0.01);
        world.playSound(cauldronLoc, Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
        world.playSound(cauldronLoc, Sound.ENTITY_BREEZE_LAND, 0.5F, 1.0F);
    }

    private void checkRecipe(final Location cauldronLoc) {
        final List<ItemStack> ingredients = cauldronIngredients.get(cauldronLoc);
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }

        final Map<String, Integer> ingredientMap = buildIngredientMap(ingredients);

        final CauldronRecipeRegistry.CauldronRecipe recipe = recipeRegistry.findMatch(ingredientMap);
        if (recipe == null) {
            return;
        }

        final Integer breathLeft = activeCauldrons.get(cauldronLoc);
        if (breathLeft == null || breathLeft < recipe.breathCost()) {
            return;
        }

        final int newBreath = breathLeft - recipe.breathCost();
        activeCauldrons.put(cauldronLoc, newBreath);

        ingredients.clear();

        final ItemStack resultItem;
        if (recipe.isCustomResult()) {
            final Optional<CustomItemDefinition> def = itemDefinitionRegistry.findByPdcItemId(recipe.resultId());
            if (def.isPresent()) {
                resultItem = customItemFactory.create(def.get());
            } else {
                return;
            }
        } else {
            final Material mat = Material.matchMaterial(recipe.resultId());
            if (mat == null) {
                return;
            }
            resultItem = new ItemStack(mat, 1);
        }

        final World world = cauldronLoc.getWorld();
        if (world == null) {
            return;
        }
        final Location spawnLoc = cauldronLoc.clone().add(0.5, 0.75, 0.5);
        final double vx = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.26;
        final double vz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.26;
        final Item droppedItem = world.dropItem(spawnLoc, resultItem);
        droppedItem.setVelocity(new Vector(vx, 0.15, vz));
        droppedItem.addScoreboardTag("stellarity.brewing.ignore");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (droppedItem.isValid()) {
                droppedItem.removeScoreboardTag("stellarity.brewing.ignore");
            }
        }, 40L);

        world.spawnParticle(Particle.END_ROD, spawnLoc, 17, 0.25, 0.25, 0.25, 0.01);
        world.spawnParticle(Particle.POOF, cauldronLoc.clone().add(0.5, 0.45, 0.5), 10, 0.25, 0.0, 0.25, 0.0);
        world.playSound(cauldronLoc, Sound.ENTITY_GENERIC_SWIM, 0.75F, 0.7F);
        world.playSound(cauldronLoc, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1.0F, 0.85F);
        world.playSound(cauldronLoc, Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 0.8F);

        if (advancementService != null) {
            for (final Entity entity : world.getNearbyEntities(cauldronLoc, 8.0D, 8.0D, 8.0D)) {
                if (entity instanceof Player player) {
                    if (recipe.resultId().contains("dragon_breath") || recipe.breathCost() > 0) {
                        advancementService.grant(player, "stellarity:cauldron/dilute_breath");
                    }
                    if (recipe.resultId().contains("golden_chorus_fruit")) {
                        advancementService.grant(player, "stellarity:cauldron/balanced_diet_2");
                    }
                }
            }
        }

        final Block block = cauldronLoc.getBlock();
        if (block.getType() == Material.WATER_CAULDRON) {
            final org.bukkit.block.data.Levelled levelled = (org.bukkit.block.data.Levelled) block.getBlockData();
            levelled.setLevel(levelled.getMaximumLevel());
            block.setBlockData(levelled);
        }

        if (newBreath <= 0) {
            activeCauldrons.remove(cauldronLoc);
            cauldronIngredients.remove(cauldronLoc);
        }
    }

    private Map<String, Integer> buildIngredientMap(final List<ItemStack> ingredients) {
        final Map<String, Integer> map = new HashMap<>();
        for (final ItemStack item : ingredients) {
            final String key = getIngredientKey(item);
            map.merge(key, 1, Integer::sum);
        }
        return map;
    }

    private String getIngredientKey(final ItemStack item) {
        final Optional<String> customId = itemStateRepository.getItemId(item);
        if (customId.isPresent()) {
            String id = customId.get().toLowerCase(java.util.Locale.ROOT);
            if (id.startsWith("stellarity:")) {
                id = id.substring("stellarity:".length());
            }
            final String[] prefixes = {
                    "weapons_", "trinkets_", "potions_", "materials_", "food_fish_", "food_",
                    "keys_", "spellbooks_", "misc_", "tools_", "blocks_", "armor_"
            };
            for (final String prefix : prefixes) {
                if (id.startsWith(prefix)) {
                    id = id.substring(prefix.length());
                    break;
                }
            }
            return "custom:" + id;
        }
        return "vanilla:" + item.getType().getKey().getKey().toLowerCase(java.util.Locale.ROOT);
    }

    public void handleInteract(final org.bukkit.event.player.PlayerInteractEvent event) {
        if (!featureFlags.isCauldronCraftEnabled()) {
            return;
        }

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Player player = event.getPlayer();
        final Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.WATER_CAULDRON) {
            return;
        }

        final Location cauldronLoc = clicked.getLocation();
        if (!activeCauldrons.containsKey(cauldronLoc)) {
            return;
        }

        final ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.AIR) {
            return;
        }

        final List<ItemStack> ingredients = cauldronIngredients.get(cauldronLoc);
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }

        final ItemStack lastItem = ingredients.remove(ingredients.size() - 1);
        final World world = cauldronLoc.getWorld();
        if (world != null) {
            final Location dropLoc = cauldronLoc.clone().add(0.5, 1.0, 0.5);
            final Item dropped = world.dropItem(dropLoc, lastItem);
            dropped.addScoreboardTag("stellarity.brewing.ignore");
            dropped.setVelocity(new Vector(0.0, 0.15, 0.0));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (dropped.isValid()) {
                    dropped.removeScoreboardTag("stellarity.brewing.ignore");
                }
            }, 40L);

            world.playSound(cauldronLoc, Sound.ENTITY_GENERIC_SPLASH, 0.75F, 1.2F);
        }

        event.setCancelled(true);
    }

    private void spawnBoilingParticles(final Location cauldronLoc) {
        final World world = cauldronLoc.getWorld();
        if (world == null) {
            return;
        }

        final int breathLeft = activeCauldrons.getOrDefault(cauldronLoc, 0);
        final int particleCount = Math.max(1, Math.min(breathLeft, 5));
        world.spawnParticle(Particle.DRAGON_BREATH, cauldronLoc.clone().add(0.5, 0.55, 0.5),
                particleCount, 0.25, 0.05, 0.25, 0.00577, 1.0F);
    }

    private void dropAllIngredients(final Location cauldronLoc) {
        final List<ItemStack> ingredients = cauldronIngredients.get(cauldronLoc);
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }
        final World world = cauldronLoc.getWorld();
        if (world == null) {
            return;
        }
        final Location dropLoc = cauldronLoc.clone().add(0.5, 1.0, 0.5);
        for (final ItemStack item : ingredients) {
            final Item dropped = world.dropItem(dropLoc, item);
            dropped.addScoreboardTag("stellarity.brewing.ignore");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (dropped.isValid()) {
                    dropped.removeScoreboardTag("stellarity.brewing.ignore");
                }
            }, 40L);
        }
        ingredients.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.isCancelled() && event.getBlock().getType() != Material.AIR) {
            return;
        }
        final Location loc = event.getBlock().getLocation();
        if (activeCauldrons.containsKey(loc)) {
            dropAllIngredients(loc);
            activeCauldrons.remove(loc);
            cauldronIngredients.remove(loc);
        }
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        activeCauldrons.keySet().removeIf(loc -> {
            if (loc.getWorld().equals(event.getWorld())
                    && (loc.getBlockX() >> 4) == event.getChunk().getX()
                    && (loc.getBlockZ() >> 4) == event.getChunk().getZ()) {
                dropAllIngredients(loc);
                cauldronIngredients.remove(loc);
                return true;
            }
            return false;
        });
    }

    public void handleDrop(final org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!featureFlags.isCauldronCraftEnabled()) {
            return;
        }

        final Item droppedItem = event.getItemDrop();

        if (droppedItem.getItemStack().getType() == Material.DRAGON_BREATH) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (droppedItem.isValid()) {
                    tryActivateOrRechargeCauldron(droppedItem);
                }
            }, 20L);
        } else if (!activeCauldrons.isEmpty()) {
            final Location playerLoc = event.getPlayer().getLocation();
            boolean nearCauldron = false;
            for (final Location cauldronLoc : activeCauldrons.keySet()) {
                if (cauldronLoc.getWorld().equals(playerLoc.getWorld())
                        && cauldronLoc.distanceSquared(playerLoc) <= 25.0) {
                    nearCauldron = true;
                    break;
                }
            }
            if (!nearCauldron) {
                return;
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (droppedItem.isValid()) {
                    tryAbsorbItemIntoNearbyCauldron(droppedItem);
                    for (final Location loc : new ArrayList<>(activeCauldrons.keySet())) {
                        checkRecipe(loc);
                    }
                }
            }, 15L);
        }
    }

    public void handleItemSpawn(final org.bukkit.event.entity.ItemSpawnEvent event) {
        if (!featureFlags.isCauldronCraftEnabled()) {
            return;
        }

        final Item itemEntity = event.getEntity();
        if (itemEntity.getItemStack().getType() != Material.DRAGON_BREATH) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (itemEntity.isValid()) {
                tryActivateOrRechargeCauldron(itemEntity);
            }
        }, 10L);
    }
}
