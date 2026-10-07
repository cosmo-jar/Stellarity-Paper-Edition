package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * логика зачарования daybroken для Kaleidoscope:
 * локальное накопление заряда и разряд по веткам 0/1/2/3.
 */
public final class KaleidoscopeMechanicService {

    private static final String KALEIDOSCOPE_ITEM_ID = "kaleidoscope";
    private static final int MAX_PROGRESS = 120;
    private static final long CHARGE_TICK_PERIOD = 1L;
    private static final TextColor ACTION_BAR_MAIN_COLOR = TextColor.fromHexString("#EEEEEE");
    private static final TextColor ACTION_BAR_STAR_COLOR = TextColor.fromHexString("#FFCF37");

    private final org.bukkit.plugin.Plugin plugin;
    private final EnchantActiveService activeService;
    private final ItemStateRepository itemStateRepository;
    private final StellaritySoundService soundService;
    private final RadiantJewelService radiantJewelService;
    private final CustomStatusEffectService customStatusEffectService;
    private final NamespacedKey itemInstanceUuidKey;
    private final Map<PlayerItemKey, ChargeState> states = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public KaleidoscopeMechanicService(
            final org.bukkit.plugin.Plugin plugin,
            final EnchantActiveService activeService,
            final ItemStateRepository itemStateRepository,
            final StellaritySoundService soundService,
            final RadiantJewelService radiantJewelService,
            final CustomStatusEffectService customStatusEffectService
    ) {
        this.plugin = plugin;
        this.activeService = activeService;
        this.itemStateRepository = itemStateRepository;
        this.soundService = soundService;
        this.radiantJewelService = radiantJewelService;
        this.customStatusEffectService = customStatusEffectService;
        this.itemInstanceUuidKey = new NamespacedKey(plugin, "item_instance_uuid");
    }

    public void syncPlayer(final Player player) {
        if (player == null) {
            return;
        }
        if (radiantJewelService != null) {
            radiantJewelService.rescan(player);
        }
        if (canCharge(player)) {
            ensureSession(player);
            return;
        }
        stopSession(player.getUniqueId(), false);
    }

    public void clear(final Player player) {
        if (player == null) {
            return;
        }
        stopSession(player.getUniqueId(), true);
    }

    public void clearAll() {
        for (final BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        states.clear();
    }

    public void onDaybrokenMeleeHit(
            final EntityDamageByEntityEvent event,
            final Player attacker,
            final LivingEntity victim
    ) {
        if (attacker == null || victim == null) {
            return;
        }
        if (!isKaleidoscopeInMainHand(attacker)) {
            return;
        }

        final int charge = consumeCharge(attacker);
        sendChargeActionBar(attacker, 0);
        if (charge <= 32) {
            applyFireIfTargetFireAtMost(victim, 40, 40);
            victim.getWorld().spawnParticle(Particle.END_ROD, victim.getLocation().add(0.0D, 1.0D, 0.0D), 8, 0.2D, 0.2D, 0.2D, 0.02D);
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0.0D, 1.0D, 0.0D), 13, 0.2D, 0.2D, 0.2D, 0.02D);
            return;
        }

        final boolean hasRadiant = radiantJewelService != null && radiantJewelService.hasRadiantJewel(attacker);
        if (charge <= 65) {
            runChargeOne(event, attacker, victim, charge, hasRadiant);
            return;
        }
        if (charge <= 99) {
            runChargeTwo(event, attacker, victim, charge, hasRadiant);
            return;
        }
        runChargeThree(event, attacker, victim, hasRadiant);
    }

    private void runChargeOne(
            final EntityDamageByEntityEvent event,
            final Player attacker,
            final LivingEntity victim,
            final int charge,
            final boolean hasRadiant
    ) {
        if (customStatusEffectService != null) {
            customStatusEffectService.applyHolyFlames(victim, hasRadiant ? 80 : 60, attacker);
        }
        event.setDamage(event.getDamage() + 30.0D + charge);
        soundService.play(victim, "stellarity:item.kaleidoscope.level_1", 1.0F, 1.0F, null);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.55F, 1.0F);
        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0F, 1.0F);
        victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0.0D, 1.0D, 0.0D), 32, 0.35D, 0.35D, 0.35D, 0.08D);
        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0.0D, 1.0D, 0.0D), 24, 0.6D, 0.6D, 0.6D, 0.0D);
    }

    private void runChargeTwo(
            final EntityDamageByEntityEvent event,
            final Player attacker,
            final LivingEntity victim,
            final int charge,
            final boolean hasRadiant
    ) {
        final int fire = charge * 2;
        final double baseDamage = 80.0D + charge;
        if (customStatusEffectService != null) {
            customStatusEffectService.applyHolyFlames(victim, hasRadiant ? 90 : 70, attacker);
        }
        event.setDamage(event.getDamage() + baseDamage);

        final int holyFlamesTicks = hasRadiant ? 90 : 70;
        applyRingSplash(attacker, victim, 0.1D, 0.9D, baseDamage * 0.70D, fire, 0.80D, 200, holyFlamesTicks);
        applyRingSplash(attacker, victim, 0.91D, 1.8D, baseDamage * 0.50D, fire, 0.65D, 200, holyFlamesTicks);
        applyRingSplash(attacker, victim, 1.81D, 2.7D, baseDamage * 0.30D, fire, 0.50D, 200, holyFlamesTicks);

        soundService.play(victim, "stellarity:item.kaleidoscope.level_2", 1.0F, 1.0F, null);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.9F);
        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0F, 0.8F);
        victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0.0D, 1.0D, 0.0D), 64, 0.5D, 0.5D, 0.5D, 0.147D);
        victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0.0D, 1.0D, 0.0D), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void runChargeThree(
            final EntityDamageByEntityEvent event,
            final Player attacker,
            final LivingEntity victim,
            final boolean hasRadiant
    ) {
        event.setDamage(event.getDamage() + 300.0D);
        if (customStatusEffectService != null) {
            customStatusEffectService.applyHolyFlames(victim, hasRadiant ? 100 : 80, attacker);
        }

        final int holyFlamesTicks = hasRadiant ? 100 : 80;
        applyRingSplash(attacker, victim, 0.1D, 1.4D, 231.0D, 260, 0.80D, Integer.MAX_VALUE, holyFlamesTicks);
        applyRingSplash(attacker, victim, 1.41D, 2.8D, 150.0D, 260, 0.65D, Integer.MAX_VALUE, holyFlamesTicks);
        applyRingSplash(attacker, victim, 2.81D, 4.2D, 90.0D, 260, 0.50D, Integer.MAX_VALUE, holyFlamesTicks);

        soundService.play(victim, "stellarity:item.kaleidoscope.level_3", 1.0F, 1.0F, null);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0F, 0.6F);
        victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0.0D, 1.0D, 0.0D), 128, 0.8D, 0.8D, 0.8D, 0.3D, null);
        victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.5D, 0.5D, 0.5D, 0.0D, null);
    }

    private void applyRingSplash(
            final Player attacker,
            final LivingEntity center,
            final double minDistance,
            final double maxDistance,
            final double damage,
            final int baseFireTicks,
            final double fireMultiplier,
            final int setFireIfAtMost,
            final int holyFlamesTicks
    ) {
        final List<Entity> nearby = center.getNearbyEntities(maxDistance + 0.25D, maxDistance + 0.25D, maxDistance + 0.25D);
        for (final Entity entity : nearby) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }
            if (target.getUniqueId().equals(center.getUniqueId())) {
                continue;
            }
            if (target.getUniqueId().equals(attacker.getUniqueId())) {
                continue;
            }
            final double distance = target.getLocation().distance(center.getLocation());
            if (distance < minDistance || distance > maxDistance) {
                continue;
            }
            target.damage(damage);
            final int fireTicks = (int) Math.floor(baseFireTicks * fireMultiplier);
            applyFireIfTargetFireAtMost(target, setFireIfAtMost, fireTicks);
            if (holyFlamesTicks > 0 && customStatusEffectService != null) {
                customStatusEffectService.applyHolyFlames(target, holyFlamesTicks, attacker);
            }
        }
    }

    private void applyFireIfTargetFireAtMost(
            final LivingEntity target,
            final int threshold,
            final int fireTicks
    ) {
        if (target.getFireTicks() <= threshold) {
            target.setFireTicks(Math.max(0, fireTicks));
        }
    }

    private UUID getOrCreateItemUuid(final Player player) {
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType().isAir()) {
            return null;
        }
        final ItemMeta meta = mainHand.getItemMeta();
        if (meta == null) {
            return null;
        }
        final String saved = meta.getPersistentDataContainer().get(itemInstanceUuidKey, PersistentDataType.STRING);
        if (saved != null) {
            try {
                return UUID.fromString(saved);
            } catch (final IllegalArgumentException ignored) {
                // Fallback to generating a fresh UUID if saved data is invalid
            }
        }
        final UUID newUuid = UUID.randomUUID();
        meta.getPersistentDataContainer().set(itemInstanceUuidKey, PersistentDataType.STRING, newUuid.toString());
        mainHand.setItemMeta(meta);
        return newUuid;
    }

    private int consumeCharge(final Player player) {
        final UUID itemUuid = getOrCreateItemUuid(player);
        if (itemUuid == null) {
            return 0;
        }
        final PlayerItemKey key = new PlayerItemKey(player.getUniqueId(), itemUuid);
        final ChargeState state = states.computeIfAbsent(key, ignored -> new ChargeState());
        final int charge = chargeFromProgress(state.chargeProgress);
        state.chargeProgress = 0;
        return charge;
    }

    private void ensureSession(final Player player) {
        final UUID playerUuid = player.getUniqueId();
        final UUID itemUuid = getOrCreateItemUuid(player);
        if (itemUuid == null) {
            return;
        }
        final PlayerItemKey key = new PlayerItemKey(playerUuid, itemUuid);
        final ChargeState state = states.computeIfAbsent(key, ignored -> new ChargeState());
        sendChargeActionBar(player, chargeFromProgress(state.chargeProgress));
        if (tasks.containsKey(playerUuid)) {
            return;
        }
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(playerUuid, itemUuid), CHARGE_TICK_PERIOD, CHARGE_TICK_PERIOD);
        tasks.put(playerUuid, task);
    }

    private void tick(final UUID playerUuid, final UUID itemUuid) {
        final Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline() || !canCharge(player)) {
            stopSession(playerUuid, false);
            return;
        }
        final UUID currentItemUuid = getOrCreateItemUuid(player);
        if (currentItemUuid == null || !currentItemUuid.equals(itemUuid)) {
            stopSession(playerUuid, false);
            return;
        }

        final PlayerItemKey key = new PlayerItemKey(playerUuid, itemUuid);
        final ChargeState state = states.computeIfAbsent(key, ignored -> new ChargeState());
        final int oldProgress = state.chargeProgress;
        if (state.chargeProgress < MAX_PROGRESS) {
            state.chargeProgress++;
            if (radiantJewelService != null && radiantJewelService.hasRadiantJewel(player)
                    && ThreadLocalRandom.current().nextBoolean() && state.chargeProgress < MAX_PROGRESS) {
                state.chargeProgress++;
            }
        }
        if (oldProgress < 119 && state.chargeProgress >= 119) {
            soundService.play(player, "stellarity:item.kaleidoscope.ready", 1.0F, 1.0F, null);
            player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0F, 2.0F);
        }
        final int charge = chargeFromProgress(state.chargeProgress);
        sendChargeActionBar(player, charge);
        spawnHoldingParticles(player, charge);
    }

    private void sendChargeActionBar(final Player player, final int charge) {
        final Component actionBar = Component.text("•", ACTION_BAR_MAIN_COLOR)
                .append(Component.text(" ⭐ ", ACTION_BAR_STAR_COLOR))
                .append(Component.text(Integer.toString(charge), ACTION_BAR_MAIN_COLOR))
                .append(Component.text(" ⭐ ", ACTION_BAR_STAR_COLOR))
                .append(Component.text("•", ACTION_BAR_MAIN_COLOR));
        player.sendActionBar(actionBar);
    }

    private int chargeFromProgress(final int progress) {
        return (progress * 10) / 12;
    }

    private void spawnHoldingParticles(final Player player, final int charge) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        if (charge >= 33 && charge <= 65) {
            if (random.nextInt(100) < 20) {
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0.0D, 1.1D, 0.0D), 1, 0.1D, 0.1D, 0.1D, 0.05D, null);
            }
            return;
        }
        if (charge >= 66 && charge <= 99) {
            if (random.nextInt(100) < 40) {
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0.0D, 1.1D, 0.0D), 1, 0.1D, 0.1D, 0.1D, 0.05D, null);
            }
            if (random.nextInt(100) < 11) {
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0.0D, 1.1D, 0.0D), 1, 0.1D, 0.1D, 0.1D, 0.05D, null);
            }
            return;
        }
        if (charge == 100) {
            if (random.nextInt(100) < 60) {
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0.0D, 1.1D, 0.0D), 1, 0.1D, 0.1D, 0.1D, 0.05D, null);
            }
            if (random.nextInt(100) < 22) {
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0.0D, 1.1D, 0.0D), 1, 0.1D, 0.1D, 0.1D, 0.05D, null);
            }
        }
    }

    private boolean canCharge(final Player player) {
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType().isAir()) {
            return false;
        }
        final String itemId = itemStateRepository.getItemId(mainHand).orElse("");
        if (!KALEIDOSCOPE_ITEM_ID.equalsIgnoreCase(itemId)) {
            return false;
        }
        final int level = activeService.highestLevel(activeService.fromItem(mainHand, "mainhand"), EnchantIds.TECHNICAL_DAYBROKEN);
        return level > 0;
    }

    private boolean isKaleidoscopeInMainHand(final Player player) {
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        final String itemId = itemStateRepository.getItemId(mainHand).orElse("");
        return KALEIDOSCOPE_ITEM_ID.equalsIgnoreCase(itemId);
    }

    private void stopSession(final UUID uuid, final boolean clearState) {
        final BukkitTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        final Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendActionBar(Component.empty());
        }
        if (clearState) {
            states.keySet().removeIf(key -> key.playerUuid.equals(uuid));
        }
    }

    private record PlayerItemKey(UUID playerUuid, UUID itemUuid) {}

    private static final class ChargeState {
        private int chargeProgress = 0;
    }
}
