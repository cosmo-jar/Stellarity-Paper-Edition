package dev.cosmojar.stellaritypaper.mechanics.advancements;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.mechanics.pixie.PixieEntityService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class AdvancementListener implements Listener {

    private final AdvancementService advancementService;
    private final ItemStateRepository itemStateRepository;
    private final PixieEntityService pixieEntityService;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final CustomItemFactory customItemFactory;
    private final NamespacedKey collectedPixiesKey;

    private static final Set<String> ALL_PIXIE_VARIANTS = Set.of(
            "yellow",
            "magenta",
            "lime",
            "light_blue",
            "radiant"
    );

    private static final Set<String> COLD_BIOMES = Set.of(
            "stellarity:frosted_valley",
            "stellarity:frozen_marsh",
            "stellarity:frozen_shrubland",
            "stellarity:frozen_spikes",
            "stellarity:hallowed_tundra",
            "frosted_valley",
            "frozen_marsh",
            "frozen_shrubland",
            "frozen_spikes",
            "hallowed_tundra"
    );

    public AdvancementListener(
            final Plugin plugin,
            final AdvancementService advancementService,
            final ItemStateRepository itemStateRepository,
            final PixieEntityService pixieEntityService,
            final ItemDefinitionRegistry itemDefinitionRegistry,
            final CustomItemFactory customItemFactory
    ) {
        this.advancementService = advancementService;
        this.itemStateRepository = itemStateRepository;
        this.pixieEntityService = pixieEntityService;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.customItemFactory = customItemFactory;
        this.collectedPixiesKey = new NamespacedKey(plugin, "collected_pixies");

        // Периодический чек биомов и скорости полета на элитрах (раз в 20 тиков / 1 секунду)
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickPlayerExploration, 20L, 20L);
    }

    private void tickPlayerExploration() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }

            final Location loc = player.getLocation();
            final World world = player.getWorld();

            if (world.getEnvironment() == World.Environment.THE_END) {
                final String biomeKey = loc.getBlock().getBiome().getKey().toString().toLowerCase(Locale.ROOT);
                final String biomeName = biomeKey.contains(":") ? biomeKey.split(":")[1] : biomeKey;

                // Исследование всех биомов
                advancementService.grantCriteria(player, "stellarity:exploration/discover_all_biomes", biomeName);

                // Согревание в холодном биоме
                if (COLD_BIOMES.contains(biomeKey) || COLD_BIOMES.contains(biomeName)) {
                    if (isNearHeatSource(loc)) {
                        advancementService.grant(player, "stellarity:exploration/keep_warm");
                    }
                }
            }

            // Полет на элитрах на высокой скорости (> 40 блоков/сек)
            if (player.isGliding()) {
                final double speed = player.getVelocity().length();
                if (speed >= 1.8D) {
                    advancementService.grant(player, "stellarity:end_city/super_sonic");
                }
            }
        }
    }

    private boolean isNearHeatSource(final Location loc) {
        final World world = loc.getWorld();
        if (world == null) return false;
        final int px = loc.getBlockX();
        final int py = loc.getBlockY();
        final int pz = loc.getBlockZ();

        for (int x = px - 3; x <= px + 3; x++) {
            for (int y = py - 2; y <= py + 3; y++) {
                for (int z = pz - 3; z <= pz + 3; z++) {
                    final Material type = world.getBlockAt(x, y, z).getType();
                    if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE
                            || type == Material.FIRE || type == Material.SOUL_FIRE
                            || type == Material.LAVA || type == Material.MAGMA_BLOCK
                            || type == Material.TORCH || type == Material.WALL_TORCH
                            || type == Material.SOUL_TORCH || type == Material.SOUL_WALL_TORCH) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(final PlayerItemConsumeEvent event) {
        final ItemStack item = event.getItem();
        final Optional<String> itemId = itemStateRepository.getItemId(item);
        final Player player = event.getPlayer();

        if (itemId.isPresent()) {
            final String id = itemId.get().toLowerCase(Locale.ROOT);
            if (id.contains("golden_chorus_fruit")) {
                advancementService.grant(player, "stellarity:cauldron/eat_golden_chorus_fruit");
            } else if (id.contains("duskberry")) {
                advancementService.grantCriteria(player, "stellarity:exploration/duskberry/poor_life_choices", "eat_duskberry");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final Entity clicked = event.getRightClicked();
        final ItemStack handItem = player.getInventory().getItem(event.getHand());

        if (handItem.getType().isAir()) {
            return;
        }

        // Кормление даскберри
        final Optional<String> customId = itemStateRepository.getItemId(handItem);
        if (customId.isPresent() && customId.get().equalsIgnoreCase("duskberry")) {
            advancementService.grantCriteria(player, "stellarity:exploration/duskberry/poor_life_choices", "feed_duskberry");
        }

        // Поимка Пикси в банку стеклянной бутылкой
        if (pixieEntityService != null && pixieEntityService.isPixie(clicked)) {
            if (handItem.getType() == Material.GLASS_BOTTLE) {
                event.setCancelled(true);

                if (handItem.getAmount() == 1) {
                    player.getInventory().setItem(event.getHand(), null);
                } else {
                    handItem.setAmount(handItem.getAmount() - 1);
                }

                final Optional<CustomItemDefinition> jarDef = itemDefinitionRegistry.findByPdcItemId("pixie_in_a_jar_light_blue");
                final ItemStack jarItem = jarDef.map(customItemFactory::createAlwaysLore)
                        .orElseGet(() -> new ItemStack(Material.GLASS_BOTTLE));

                player.getInventory().addItem(jarItem);
                checkItemAdvancements(player, jarItem);
                clicked.remove();

                final Location loc = clicked.getLocation();
                final World world = loc.getWorld();
                if (world != null) {
                    world.playSound(loc, Sound.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1.0F, 1.2F);
                    world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.NEUTRAL, 1.0F, 1.8F);
                    world.spawnParticle(Particle.DUST, loc.clone().add(0, 0.35, 0), 15, 0.2, 0.2, 0.2,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(167, 255, 250), 1.2f));
                }

                advancementService.grant(player, "stellarity:altar_of_the_accursed/can_i_keep_it");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItemInHand();
        final Block block = event.getBlockPlaced();

        final Optional<String> customId = itemStateRepository.getItemId(item);
        if (customId.isPresent() && customId.get().equalsIgnoreCase("duskberry")) {
            advancementService.grantCriteria(player, "stellarity:exploration/duskberry/poor_life_choices", "plant_duskberry");
        }

        if (block.getType() == Material.CHORUS_FLOWER) {
            final World.Environment env = block.getWorld().getEnvironment();
            if (env == World.Environment.NORMAL) {
                advancementService.grantCriteria(player, "stellarity:minecraft/husbandry/otherwordly_botanist", "plant_chorus_fruit_overworld");
            } else if (env == World.Environment.NETHER) {
                advancementService.grantCriteria(player, "stellarity:minecraft/husbandry/otherwordly_botanist", "plant_chorus_fruit_nether");
            } else if (env == World.Environment.THE_END) {
                advancementService.grantCriteria(player, "stellarity:minecraft/husbandry/otherwordly_botanist", "plant_chorus_fruit_end");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack hand = event.getItem();

        // Проигрывание диска в Энде
        if (block.getType() == Material.JUKEBOX && block.getWorld().getEnvironment() == World.Environment.THE_END) {
            if (hand != null && hand.getType().isRecord()) {
                advancementService.grant(player, "stellarity:minecraft/adventure/champions_ballad");
            }
        }

        // Зарядка якоря возрождения в Энде
        if (block.getType() == Material.RESPAWN_ANCHOR && block.getWorld().getEnvironment() == World.Environment.THE_END) {
            if (hand != null && hand.getType() == Material.GLOWSTONE) {
                advancementService.grant(player, "stellarity:minecraft/nether/charge_anchor_end");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkItemAdvancements(player, event.getItem().getItemStack());
            checkFullArmorSet(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getCurrentItem() != null) {
                checkItemAdvancements(player, event.getCurrentItem());
            }
            final ItemStack cursor = event.getCursor();
            if (!cursor.getType().isAir()) {
                checkItemAdvancements(player, cursor);
            }
            checkFullArmorSet(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(final CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            final ItemStack result = event.getRecipe().getResult();
            checkItemAdvancements(player, result);

            if (result.getType().name().endsWith("SHULKER_BOX")) {
                advancementService.grant(player, "stellarity:end_city/portable_storage");
                advancementService.grant(player, "stellarity:end_city/color_a_shulker_box");
            }
        }
    }

    private void checkItemAdvancements(final Player player, final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        if (item.getType() == Material.ENDER_PEARL) {
            advancementService.grant(player, "stellarity:minecraft/story/obtain_an_ender_pearl");
        }

        final Optional<String> customId = itemStateRepository.getItemId(item);
        if (customId.isEmpty()) {
            return;
        }

        final String id = customId.get().toLowerCase(Locale.ROOT);
        if (id.startsWith("pixie_in_a_jar_")) {
            final String variant = id.substring("pixie_in_a_jar_".length());
            if (ALL_PIXIE_VARIANTS.contains(variant)) {
                checkPixieCollectionAdvancement(player, variant);
            }
        }
        switch (id) {
            case "duskberry" -> advancementService.grant(player, "stellarity:exploration/duskberry/discover");
            case "dragonblade" -> advancementService.grant(player, "stellarity:dragons_den/obtain_dragonblade");
            case "harvester" -> advancementService.grant(player, "stellarity:exploration/harvester/frozen_reaper");
            case "satchel_of_voids" -> advancementService.grant(player, "stellarity:altar_of_the_accursed/obtain_satchel_of_voids");
            case "enderite_smithing_template" -> advancementService.grant(player, "stellarity:altar_of_the_accursed/obtain_enderite_upgrade_smithing_template");
            case "shulker_hoe" -> advancementService.grant(player, "stellarity:minecraft/husbandry/absolute_devotion");
        }
    }

    private void checkPixieCollectionAdvancement(final Player player, final String variant) {
        advancementService.grantCriteria(player, "stellarity:empress_of_light/prismatic_collection", variant);

        final PersistentDataContainer pdc = player.getPersistentDataContainer();
        final String current = pdc.getOrDefault(collectedPixiesKey, PersistentDataType.STRING, "");
        final Set<String> collected = current.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(Arrays.asList(current.split(",")));

        if (collected.add(variant)) {
            pdc.set(collectedPixiesKey, PersistentDataType.STRING, String.join(",", collected));
        }

        if (collected.size() >= ALL_PIXIE_VARIANTS.size() && collected.containsAll(ALL_PIXIE_VARIANTS)) {
            advancementService.grant(player, "stellarity:empress_of_light/prismatic_collection");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        final Entity entity = event.getEntity();
        final World world = entity.getWorld();

        if (entity instanceof Phantom phantom && world.getEnvironment() == World.Environment.THE_END) {
            advancementService.grant(killer, "stellarity:minecraft/adventure/night_sky_stalkers");
            if (phantom.getSize() >= 10) {
                advancementService.grant(killer, "stellarity:minecraft/adventure/kill_large_phantom");
            }
        }

        if (entity instanceof Wither && world.getEnvironment() == World.Environment.THE_END) {
            advancementService.grant(killer, "stellarity:minecraft/nether/the_beginning");
        }

        if (entity instanceof Shulker) {
            advancementService.grant(killer, "stellarity:end_city/defeat_shulker");
        }

        if (entity.getType() == EntityType.ENDER_DRAGON) {
            final ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (itemStateRepository.getItemId(weapon).orElse("").equalsIgnoreCase("dragonblade")) {
                advancementService.grant(killer, "stellarity:dragons_den/kaliyah");
            }
        }
    }

    public void checkFullArmorSet(final Player player) {
        final ItemStack helmet = player.getInventory().getHelmet();
        final ItemStack chestplate = player.getInventory().getChestplate();
        final ItemStack leggings = player.getInventory().getLeggings();
        final ItemStack boots = player.getInventory().getBoots();

        if (helmet == null || chestplate == null || leggings == null || boots == null) {
            return;
        }

        if (isArmorType(helmet, "shulker") && isArmorType(chestplate, "shulker")
                && isArmorType(leggings, "shulker") && isArmorType(boots, "shulker")) {
            advancementService.grant(player, "stellarity:altar_of_the_accursed/craft_full_shulker_armor");
        }

        if (isArmorType(helmet, "champion") && isArmorType(chestplate, "champion")
                && isArmorType(leggings, "champion") && isArmorType(boots, "champion")) {
            advancementService.grant(player, "stellarity:altar_of_the_accursed/craft_full_champion_armor");
        }

        if (isArmorType(helmet, "floral") && isArmorType(chestplate, "floral")
                && isArmorType(leggings, "floral") && isArmorType(boots, "floral")) {
            advancementService.grant(player, "stellarity:altar_of_the_accursed/craft_full_floral_armor");
        }

        if (isArmorType(helmet, "hallowed") && isArmorType(chestplate, "hallowed")
                && isArmorType(leggings, "hallowed") && isArmorType(boots, "hallowed")) {
            advancementService.grant(player, "stellarity:altar_of_the_accursed/craft_full_hallowed_armor");
        }
    }

    private boolean isArmorType(final ItemStack item, final String prefix) {
        return itemStateRepository.getItemId(item)
                .map(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix + "_"))
                .orElse(false);
    }
}