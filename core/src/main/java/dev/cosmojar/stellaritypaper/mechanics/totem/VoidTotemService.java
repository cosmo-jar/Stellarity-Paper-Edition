package dev.cosmojar.stellaritypaper.mechanics.totem;

import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import dev.cosmojar.stellaritypaper.text.TextService;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoidTotemService {

    private static final long SAFE_POINT_TTL_MS = 10 * 60_000L; // 10 минут
    private static final long RESCUE_COOLDOWN_MS = 3_000L;
    private static final long NO_DAMAGE_MS = 2_500L;

    private final FeatureFlags featureFlags;
    private final TextService textService;
    private final dev.cosmojar.stellaritypaper.text.MessageService messageService;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;

    private final Map<UUID, SafePoint> lastSafe = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRescueMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> noDamageUntil = new ConcurrentHashMap<>();

    public VoidTotemService(
            final FeatureFlags featureFlags,
            final TextService textService,
            final dev.cosmojar.stellaritypaper.text.MessageService messageService,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.featureFlags = featureFlags;
        this.textService = textService;
        this.messageService = messageService;
        this.advancementService = advancementService;
    }

    public void handleMove(final PlayerMoveEvent event) {
        final Location to = event.getTo();
        if (to == null) {
            return;
        }

        final Location from = event.getFrom();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        if (!featureFlags.isVoidTotemEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        final World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) {
            return;
        }

        final double y = to.getY();
        if (y > -64.0) {
            if (player.isOnGround() && !player.isFlying() && !player.isGliding() && !player.isInsideVehicle()) {
                final Location feet = player.getLocation();
                if (isStandingOnSolid(feet)) {
                    lastSafe.put(player.getUniqueId(), new SafePoint(feet.clone(), System.currentTimeMillis()));
                }
            }
            return;
        }

        if (isHoldingTotem(player)) {
            rescue(player);
        }
    }

    public void handleQuit(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        lastSafe.remove(uuid);
        lastRescueMs.remove(uuid);
        noDamageUntil.remove(uuid);
    }

    public void handleDamage(final org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        final Long until = noDamageUntil.get(player.getUniqueId());
        if (until == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now <= until) {
            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL
                    || event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
                player.setFallDistance(0f);
            }
        } else {
            noDamageUntil.remove(player.getUniqueId());
        }
    }

    public void rescue(final Player p) {
        final long now = System.currentTimeMillis();
        final Long lastRescue = lastRescueMs.get(p.getUniqueId());
        if (lastRescue != null && now - lastRescue < RESCUE_COOLDOWN_MS) {
            return;
        }
        if (!isHoldingTotem(p)) {
            return;
        }

        final SafePoint sp = lastSafe.get(p.getUniqueId());
        if (sp != null && now - sp.timeMs <= SAFE_POINT_TTL_MS) {
            final Location back = findTwoBlockAirSpotNear(sp.location);
            if (back != null && doTeleport(p, back)) {
                lastRescueMs.put(p.getUniqueId(), now);
                return;
            }
        }

        final Location surface = findSurfaceSafeSpot(p.getLocation());
        if (surface != null && doTeleport(p, surface)) {
            lastRescueMs.put(p.getUniqueId(), now);
            return;
        }

        final Location spawn = p.getWorld().getSpawnLocation().clone().add(0, 0.2, 0);
        final Location safeSpawn = findTwoBlockAirSpotNear(spawn);
        if (doTeleport(p, safeSpawn != null ? safeSpawn : spawn)) {
            lastRescueMs.put(p.getUniqueId(), now);
        }
    }

    private boolean doTeleport(final Player p, final Location target) {
        final World w = p.getWorld();

        final Location back = target.clone();
        back.setYaw(p.getLocation().getYaw());
        back.setPitch(p.getLocation().getPitch());
        back.add(0, 0.15, 0);

        final boolean ok = p.teleport(back, PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (!ok) {
            return false;
        }

        consumeTotem(p);

        p.playEffect(EntityEffect.PROTECTED_FROM_DEATH);

        w.playSound(back, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        w.playSound(back, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);

        spawnParticleSafe(w, Particle.PORTAL, back, 35, 0.5, 0.5, 0.5, 0.1);
        spawnParticleSafe(w, Particle.DRAGON_BREATH, back, 35, 0.5, 0.5, 0.5, 0.05);

        p.setFallDistance(0f);
        p.setVelocity(new Vector(0, 0, 0));
        p.setNoDamageTicks(40);
        noDamageUntil.put(p.getUniqueId(), System.currentTimeMillis() + NO_DAMAGE_MS);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 45, 0, false, false, false));

        if (advancementService != null) {
            advancementService.grant(p, "stellarity:minecraft/adventure/postmortal_2");
        }

        if (messageService != null) {
            p.sendActionBar(messageService.message("mechanic.void-totem.actionbar"));
        } else {
            p.sendActionBar(textService.mm("<gradient:#E77CC6:#E7917C>Там живут драконы...</gradient>"));
        }

        return true;
    }

    public boolean isHoldingTotem(final Player player) {
        return isTotem(player.getInventory().getItemInOffHand())
                || isTotem(player.getInventory().getItemInMainHand());
    }

    private boolean isTotem(final ItemStack stack) {
        return stack != null && stack.getType() == Material.TOTEM_OF_UNDYING;
    }

    private void consumeTotem(final Player player) {
        final ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isTotem(offHand)) {
            offHand.subtract(1);
            player.getInventory().setItemInOffHand(offHand);
            return;
        }
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isTotem(mainHand)) {
            mainHand.subtract(1);
            player.getInventory().setItemInMainHand(mainHand);
        }
    }

    private boolean isStandingOnSolid(final Location feet) {
        final Block below = feet.clone().subtract(0, 1, 0).getBlock();
        final Material t = below.getType();
        if (t.isAir() || t == Material.WATER || t == Material.LAVA) {
            return false;
        }
        return !below.getCollisionShape().getBoundingBoxes().isEmpty();
    }

    private Location findTwoBlockAirSpotNear(final Location base) {
        final World w = base.getWorld();
        if (w == null) {
            return null;
        }

        final int x = base.getBlockX();
        final int z = base.getBlockZ();
        final int startY = Math.max(w.getMinHeight(), base.getBlockY());
        final int maxY = Math.min(w.getMaxHeight() - 2, startY + 8);

        for (int y = startY; y <= maxY; y++) {
            if (isTwoBlockAir(w, x, y, z)) {
                return new Location(w, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    private boolean isTwoBlockAir(final World w, final int x, final int y, final int z) {
        final Material a = w.getBlockAt(x, y, z).getType();
        final Material b = w.getBlockAt(x, y + 1, z).getType();
        return a.isAir() && b.isAir();
    }

    private Location findSurfaceSafeSpot(final Location near) {
        final World w = near.getWorld();
        if (w == null) {
            return null;
        }
        final int x = near.getBlockX();
        final int z = near.getBlockZ();

        final int topY = w.getHighestBlockYAt(x, z);
        if (topY <= w.getMinHeight()) {
            return null;
        }
        final int y = Math.min(w.getMaxHeight() - 2, topY + 1);

        for (int i = 0; i < 16; i++) {
            if (isTwoBlockAir(w, x, y + i, z)) {
                return new Location(w, x + 0.5, y + i, z + 0.5);
            }
        }

        final int fallbackY = Math.min(w.getMaxHeight() - 2, y + 10);
        if (isTwoBlockAir(w, x, fallbackY, z)) {
            return new Location(w, x + 0.5, fallbackY, z + 0.5);
        }

        return null;
    }

    private static void spawnParticleSafe(
            final World world,
            final Particle particle,
            final Location loc,
            final int count,
            final double ox,
            final double oy,
            final double oz,
            final double speed
    ) {
        if (world == null || loc == null || particle == null) {
            return;
        }
        final Class<?> dataType = particle.getDataType();
        if (dataType == Void.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed);
        } else if (dataType == Float.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, 1.0F);
        } else if (dataType == Color.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, Color.WHITE);
        } else if (dataType == Particle.DustOptions.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, new Particle.DustOptions(Color.WHITE, 1.0F));
        } else if (dataType == Particle.DustTransition.class) {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed, new Particle.DustTransition(Color.WHITE, Color.WHITE, 1.0F));
        } else {
            world.spawnParticle(particle, loc, count, ox, oy, oz, speed);
        }
    }

    private record SafePoint(Location location, long timeMs) {
    }
}
