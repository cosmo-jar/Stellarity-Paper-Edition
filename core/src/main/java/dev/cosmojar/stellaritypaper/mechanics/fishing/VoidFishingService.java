package dev.cosmojar.stellaritypaper.mechanics.fishing;

import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.text.TextService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class VoidFishingService {

    private final Plugin plugin;
    private final FeatureFlags featureFlags;
    private final VoidFishingLootService lootService;
    private final ItemStateRepository itemStateRepository;
    private final TextService textService;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;

    private final Map<UUID, VoidFishingSession> sessions = new ConcurrentHashMap<>();

    public VoidFishingService(
            final Plugin plugin,
            final FeatureFlags featureFlags,
            final VoidFishingLootService lootService,
            final ItemStateRepository itemStateRepository,
            final TextService textService,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.plugin = plugin;
        this.featureFlags = featureFlags;
        this.lootService = lootService;
        this.itemStateRepository = itemStateRepository;
        this.textService = textService;
        this.advancementService = advancementService;

        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void handleFishEvent(final PlayerFishEvent event) {
        if (!featureFlags.isVoidFishingEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        final FishHook hook = event.getHook();

        if (event.getState() == PlayerFishEvent.State.FISHING) {
            startSession(player, hook);
            return;
        }

        if (event.getState() == PlayerFishEvent.State.REEL_IN || event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            resolveSession(player, hook);
        }
    }

    public void cancelSession(final UUID playerUuid) {
        final VoidFishingSession session = sessions.remove(playerUuid);
        if (session != null) {
            session.setActive(false);
        }
    }

    public void cancelAll() {
        for (final VoidFishingSession session : sessions.values()) {
            session.setActive(false);
        }
        sessions.clear();
    }

    private void startSession(final Player player, final FishHook hook) {
        final World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) {
            return;
        }

        final long now = System.currentTimeMillis();

        final VoidFishingSession session = new VoidFishingSession(
                player.getUniqueId(),
                hook,
                now
        );

        sessions.put(player.getUniqueId(), session);
    }

    private void resolveSession(final Player player, final FishHook hook) {
        final VoidFishingSession session = sessions.remove(player.getUniqueId());
        if (session == null || !session.isActive()) {
            return;
        }

        if (!session.isBit()) {
            return;
        }

        final World world = hook.getWorld();
        final Location hookLoc = hook.getLocation();
        final Location playerLoc = player.getLocation();

        final ItemStack loot = session.pendingLoot();

        final Map<Integer, ItemStack> leftover = player.getInventory().addItem(loot.clone());

        if (leftover.isEmpty()) {

            final Item visualItem = world.dropItemNaturally(hookLoc, loot.clone());
            visualItem.setPickupDelay(32767);
            
            final Vector direction = playerLoc.toVector().subtract(hookLoc.toVector());
            final double distance = direction.length();
            if (distance > 0.1) {
                direction.normalize().multiply(1.15D);
                direction.setY(0.38D + (distance * 0.05D));
                visualItem.setVelocity(direction);
            }
            
            Bukkit.getScheduler().runTaskLater(plugin, visualItem::remove, 15L);
            
            player.playSound(playerLoc, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.2F);
        } else {
            for (final ItemStack itemToDrop : leftover.values()) {
                final Item physicalItem = world.dropItemNaturally(hookLoc, itemToDrop);
                
                final Vector direction = playerLoc.toVector().subtract(hookLoc.toVector());
                final double distance = direction.length();
                if (distance > 0.1) {
                    direction.normalize().multiply(1.25D);
                    direction.setY(0.42D + (distance * 0.05D));
                    physicalItem.setVelocity(direction);
                }
            }
        }

        final int xp = ThreadLocalRandom.current().nextInt(1, 7);
        world.spawn(hookLoc, ExperienceOrb.class, orb -> orb.setExperience(xp));

        world.playSound(hookLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);

        if (advancementService != null) {
            advancementService.grant(player, "stellarity:void_fishing/void_reels");
            if (itemStateRepository.getItemId(loot).isPresent()) {
                advancementService.grant(player, "stellarity:void_fishing/topped_off");
            }
        }
    }

    private void tick() {
        if (!featureFlags.isVoidFishingEnabled()) {
            return;
        }

        final long now = System.currentTimeMillis();

        for (final VoidFishingSession session : new ArrayList<>(sessions.values())) {
            final FishHook hook = session.hook();

            if (hook == null || !hook.isValid()) {
                sessions.remove(session.playerUuid());
                continue;
            }

            final Player player = Bukkit.getPlayer(session.playerUuid());
            if (player == null || !player.isOnline()) {
                sessions.remove(session.playerUuid());
                continue;
            }

            final Location hookLoc = hook.getLocation();
            final World world = hook.getWorld();

            if (hookLoc.getY() <= 0) {
                player.sendActionBar(textService.tr("message.stellarity.void_fishing_too_deep"));
                sessions.remove(session.playerUuid());
                continue;
            }

            if (hookLoc.getBlock().getType().isSolid() || hookLoc.clone().add(0, -0.1D, 0).getBlock().getType().isSolid()) {
                sessions.remove(session.playerUuid());
                continue;
            }

            if (!session.isInitialized()) {
                if (hookLoc.getY() < player.getLocation().getY() - 1.5D && !hookLoc.getBlock().isLiquid()) {
                    if (!isOverVoid(hookLoc)) {
                        sessions.remove(session.playerUuid());
                        continue;
                    }

                    final Biome biome = hookLoc.getBlock().getBiome();
                    if (biome == Biome.THE_END) {
                        player.sendActionBar(textService.tr("message.stellarity.no_void_fish"));
                        sessions.remove(session.playerUuid());
                        continue;
                    }

                    initializeSession(session, player, hook, now, biome);
                }
            } else {
                if (!session.isBit()) {
                    hook.setVelocity(new Vector(0, 0, 0));

                    if (Math.abs(hookLoc.getY() - session.originalY()) > 0.02D) {
                        final Location targetLoc = hookLoc.clone();
                        targetLoc.setY(session.originalY());
                        hook.teleport(targetLoc);
                    }

                    final long timeLeft = session.biteTime() - now;

                    if (timeLeft <= 4000L) {
                        spawnApproachingParticles(world, hookLoc, timeLeft, session);
                    }

                    if (now >= session.biteTime()) {
                        session.setBit(true);
                        final long biteDuration = ThreadLocalRandom.current().nextLong(1000L, 2000L);
                        session.setBiteEndTime(now + biteDuration);

                        world.spawnParticle(Particle.POOF, hookLoc, 40, 0.1, 0.1, 0.1, 0.05);
                        world.playSound(hookLoc, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.2F, 0.8F);
                        hook.setVelocity(new Vector(0, -0.15D, 0));
                    }
                } else {
                    if (now >= session.biteEndTime()) {
                        session.setBit(false);
                        final long nextWait = ThreadLocalRandom.current().nextLong(300L, 900L) * 50L;
                        session.setBiteTime(now + nextWait);

                        final ItemStack rod = getFishingRod(player);
                        final int luckLevel = rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
                        session.setPendingLoot(lootService.rollLoot(luckLevel, hookLoc.getBlock().getBiome()));

                        session.setApproachAngle(ThreadLocalRandom.current().nextDouble() * 2.0D * Math.PI);

                        final Location resetLoc = hook.getLocation();
                        resetLoc.setY(session.originalY());
                        hook.teleport(resetLoc);
                        hook.setVelocity(new Vector(0, 0, 0));

                        world.playSound(hookLoc, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8F, 0.5F);
                        world.spawnParticle(Particle.SMOKE, hookLoc, 15, 0.1, 0.1, 0.1, 0.01);
                    } else {
                        if (hookLoc.getY() > session.originalY() - 1.2D) {
                            hook.setVelocity(new Vector(0, -0.15D, 0));
                        } else {
                            if (ThreadLocalRandom.current().nextDouble() <= 0.25D) {
                                hook.setVelocity(new Vector(0, -0.04D, 0));
                            } else {
                                hook.setVelocity(new Vector(0, 0.03D, 0));
                            }
                        }
                    }
                }
            }
        }
    }

    private void initializeSession(final VoidFishingSession session, final Player player, final FishHook hook, final long now, final Biome biome) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        final ItemStack rod = getFishingRod(player);
        int lureLevel = rod.getEnchantmentLevel(Enchantment.LURE);

        final Optional<String> itemId = itemStateRepository.getItemId(rod);
        final boolean isFisherOfVoids = itemId.isPresent() && "fisher_of_voids".equals(itemId.get());

        int minTicks = 300;
        int maxTicks = 900;

        maxTicks -= lureLevel * 100;
        if (maxTicks < minTicks) {
            maxTicks = minTicks;
        }

        if (isFisherOfVoids) {
            minTicks = Math.max(20, minTicks - 80);
            maxTicks = Math.max(minTicks, maxTicks - 160);
        }

        final int finalWaitTicks = (minTicks == maxTicks) ? minTicks : random.nextInt(minTicks, maxTicks + 1);
        final long waitMillis = finalWaitTicks * 50L;

        session.setBiteTime(now + waitMillis);

        final int luckLevel = rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);

        final ItemStack loot = lootService.rollLoot(luckLevel, biome);
        session.setPendingLoot(loot);

        final Location initLoc = hook.getLocation();
        hook.setGravity(false);
        hook.setVelocity(new Vector(0, 0, 0));
        session.setOriginalY(initLoc.getY());

        session.setInitialized(true);

        player.sendActionBar(textService.tr("advancements.stellarity.void_reels"));
    }

    private void spawnApproachingParticles(final World world, final Location hookLoc, final long timeLeft, final VoidFishingSession session) {
        final double factor = Math.max(0.0D, Math.min(1.0D, timeLeft / 4000.0D));
        final double distance = factor * 3.5D;

        final double angle = session.approachAngle();
        final double fishX = hookLoc.getX() + Math.cos(angle) * distance;
        final double fishZ = hookLoc.getZ() + Math.sin(angle) * distance;
        final double fishY = session.originalY();

        final Location fishLoc = new Location(world, fishX, fishY, fishZ);

        final ThreadLocalRandom random = ThreadLocalRandom.current();

        world.spawnParticle(Particle.END_ROD, fishLoc, 1, 0.05, 0.05, 0.05, 0.01);
        world.spawnParticle(Particle.GLOW, fishLoc, 1, 0.05, 0.05, 0.05, 0.01);

        if (random.nextDouble() <= 0.4D) {
            world.spawnParticle(Particle.CRIT, fishLoc, 2, 0.1, 0.0, 0.1, 0.02);
        }

        if (random.nextDouble() <= 0.25D) {
            world.spawnParticle(Particle.CLOUD, fishLoc, 1, 0.05, 0.0, 0.05, 0.005);
        }
    }

    private ItemStack getFishingRod(final Player player) {
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (rod.getType() == Material.FISHING_ROD) {
            return rod;
        }
        rod = player.getInventory().getItemInOffHand();
        if (rod.getType() == Material.FISHING_ROD) {
            return rod;
        }
        return new ItemStack(Material.AIR);
    }

    private boolean isOverVoid(final Location loc) {
        final World world = loc.getWorld();
        if (world == null) {
            return false;
        }
        final int startY = loc.getBlockY();
        final int minHeight = world.getMinHeight();
        for (int y = startY - 1; y >= minHeight; y--) {
            final Material type = world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType();
            if (type.isSolid()) {
                return false;
            }
        }
        return true;
    }
}
