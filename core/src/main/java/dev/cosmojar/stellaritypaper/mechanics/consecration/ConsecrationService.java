package dev.cosmojar.stellaritypaper.mechanics.consecration;

import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import dev.cosmojar.stellaritypaper.text.TextService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public final class ConsecrationService {

    private final org.bukkit.plugin.Plugin plugin;
    private final FeatureFlags featureFlags;
    private final ConsecrationRuleRegistry ruleRegistry;
    private final TextService textService;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;

    private static final Set<String> HALLOW_BIOMES = Set.of(
            "stellarity:the_hallow",
            "stellarity:prismatic_dunes",
            "stellarity:hallowed_tundra"
    );

    public ConsecrationService(
            final org.bukkit.plugin.Plugin plugin,
            final FeatureFlags featureFlags,
            final ConsecrationRuleRegistry ruleRegistry,
            final TextService textService,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.plugin = plugin;
        this.featureFlags = featureFlags;
        this.ruleRegistry = ruleRegistry;
        this.textService = textService;
        this.advancementService = advancementService;

        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    private void tick() {
        if (!featureFlags.isConsecrationEnabled()) {
            return;
        }

        for (final org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            final World world = player.getWorld();
            if (world == null || world.getEnvironment() != World.Environment.THE_END) {
                continue;
            }

            final Location pLoc = player.getLocation();
            if (!isHallowBiome(pLoc)) {
                continue;
            }

            if (advancementService != null) {
                advancementService.grant(player, "stellarity:exploration/enter_hallow");
            }

            final java.util.Collection<org.bukkit.entity.Entity> nearby = world.getNearbyEntities(pLoc, 16.0, 8.0, 16.0);
            for (final org.bukkit.entity.Entity entity : nearby) {
                if (!(entity instanceof Item itemEntity) || !itemEntity.isValid() || itemEntity.isDead()) {
                    continue;
                }

                if (itemEntity.getScoreboardTags().contains("stellarity.brewing.ignore")) {
                    continue;
                }

                final Location itemLoc = itemEntity.getLocation();
                if (isWaterOrCauldron(itemLoc)) {
                    tryConsecrateItem(itemEntity, itemLoc);
                }
            }
        }
    }

    private boolean isWaterOrCauldron(final Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        Material m = loc.getBlock().getType();
        if (m == Material.WATER || m == Material.WATER_CAULDRON || m == Material.CAULDRON) {
            return true;
        }
        m = loc.clone().add(0, -0.4, 0).getBlock().getType();
        if (m == Material.WATER || m == Material.WATER_CAULDRON || m == Material.CAULDRON) {
            return true;
        }
        m = loc.clone().add(0, 0.4, 0).getBlock().getType();
        return m == Material.WATER || m == Material.WATER_CAULDRON || m == Material.CAULDRON;
    }

    public boolean isHallowBiome(final Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        if (loc.getWorld().getEnvironment() != World.Environment.THE_END) {
            return false;
        }
        final Biome biome = loc.getBlock().getBiome();
        final String fullKey = biome.getKey().toString().toLowerCase(java.util.Locale.ROOT);
        final String subKey = biome.getKey().getKey().toLowerCase(java.util.Locale.ROOT);

        return HALLOW_BIOMES.contains(fullKey)
                || HALLOW_BIOMES.contains("stellarity:" + subKey)
                || fullKey.contains("hallow") || fullKey.contains("prismatic")
                || subKey.contains("hallow") || subKey.contains("prismatic");
    }

    public void handleDrop(final PlayerDropItemEvent event) {
        if (!featureFlags.isConsecrationEnabled()) {
            return;
        }

        final Item dropped = event.getItemDrop();
        if (dropped == null || !dropped.isValid()) {
            return;
        }

        final Location initialLoc = dropped.getLocation();
        if (initialLoc.getWorld() == null || initialLoc.getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 3;
                if (ticks > 100 || !dropped.isValid() || dropped.isDead() || dropped.getScoreboardTags().contains("stellarity.brewing.ignore")) {
                    cancel();
                    return;
                }

                final Location loc = dropped.getLocation();
                if (!isHallowBiome(loc)) {
                    return;
                }

                if (isWaterOrCauldron(loc)) {
                    if (tryConsecrateItem(dropped, loc)) {
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 2L, 3L);
    }

    public boolean tryConsecrateItem(final Item itemEntity, final Location loc) {
        if (itemEntity == null || !itemEntity.isValid()) {
            return false;
        }

        final ItemStack stack = itemEntity.getItemStack();
        final String rawType = stack.getType().name().toLowerCase(java.util.Locale.ROOT);
        final String targetKey = ruleRegistry.resolve(rawType);

        if (targetKey == null || targetKey.isBlank()) {
            return false;
        }

        Material targetMaterial = Material.matchMaterial(targetKey);
        if (targetMaterial == null) {
            final String stripped = targetKey.contains(":") ? targetKey.split(":")[1] : targetKey;
            targetMaterial = Material.matchMaterial(stripped);
        }

        if (targetMaterial == null) {
            return false;
        }

        final int amount = stack.getAmount();
        final ItemStack newStack = new ItemStack(targetMaterial, amount);
        itemEntity.setItemStack(newStack);
        itemEntity.getScoreboardTags().add("stellarity.brewing.ignore");

        final World world = loc.getWorld();
        if (world != null) {
            final Location center = loc.clone().add(0.5, 0.5, 0.5);
            world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
            world.playSound(center, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.5F, 1.4F);
            world.spawnParticle(Particle.END_ROD, center, 12, 0.25, 0.25, 0.25, 0.05);
            world.spawnParticle(Particle.DUST_PLUME, center, 10, 0.2, 0.2, 0.2, 0.01);

            if (advancementService != null) {
                for (final org.bukkit.entity.Entity entity : world.getNearbyEntities(loc, 10.0, 10.0, 10.0)) {
                    if (entity instanceof org.bukkit.entity.Player player) {
                        advancementService.grant(player, "stellarity:exploration/shimmer/transmute_item");
                    }
                }
            }
        }
        return true;
    }
}

