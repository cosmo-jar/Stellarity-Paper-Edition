package dev.cosmojar.stellaritypaper.mechanics.pixie;

import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Сервис управления сущностью Пикси в биомах The Hallow.
 */
public final class PixieEntityService implements Listener {

    private static final int MAX_PIXIES_PER_PLAYER_RADIUS = 6;
    private static final double SPAWN_CHECK_RADIUS = 80.0D;
    private static final double DESPAWN_CHECK_RADIUS = 96.0D;

    // Цветовые варианты частиц из датапака Stellarity
    private static final Color[][] PIXIE_COLORS = new Color[][]{
            // 1: Жёлтый
            {Color.fromRGB(236, 187, 81), Color.fromRGB(254, 255, 167)},
            // 2: Magenta
            {Color.fromRGB(251, 167, 255), Color.fromRGB(236, 81, 222)},
            // 3: Голубой
            {Color.fromRGB(167, 255, 250), Color.fromRGB(81, 213, 236)},
            // 4: Лаймовый
            {Color.fromRGB(126, 236, 81), Color.fromRGB(191, 255, 167)}
    };

    private final Plugin plugin;
    private final ItemDefinitionRegistry itemDefinitionRegistry;
    private final CustomItemFactory customItemFactory;
    private final NamespacedKey pixieKey;
    private final NamespacedKey variantKey;
    private final NamespacedKey hallowAllayKey;

    private final Map<UUID, PixieTracker> trackedPixies = new HashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    public PixieEntityService(
            final Plugin plugin,
            final ItemDefinitionRegistry itemDefinitionRegistry,
            final CustomItemFactory customItemFactory
    ) {
        this.plugin = plugin;
        this.itemDefinitionRegistry = itemDefinitionRegistry;
        this.customItemFactory = customItemFactory;
        this.pixieKey = new NamespacedKey(plugin, "pixie");
        this.variantKey = new NamespacedKey(plugin, "pixie_variant");
        this.hallowAllayKey = new NamespacedKey(plugin, "hallow_allay");

        // Задача спавна и деспавна (раз в 4 секунды / 80 тиков)
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::tickSpawnAndDespawn, 80L, 80L));

        // Задача анимации частиц и фоновых звуков (раз в 2 тика)
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::tickParticlesAndSounds, 2L, 2L));
    }

    /**
     * Проверка, находится ли локация в биоме группы The Hallow в Энде.
     */
    public static boolean isHallowBiome(final Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        if (loc.getWorld().getEnvironment() != World.Environment.THE_END) {
            return false;
        }
        try {
            final org.bukkit.block.Biome biome = loc.getWorld().getBiome(loc);
            final NamespacedKey key = biome.getKey();
            final String fullKey = key.asString().toLowerCase(Locale.ROOT);
            final String path = key.getKey().toLowerCase(Locale.ROOT);

            return fullKey.equals("stellarity:the_hallow")
                    || fullKey.equals("stellarity:prismatic_dunes")
                    || fullKey.equals("stellarity:hallowed_tundra")
                    || path.equals("the_hallow")
                    || path.equals("prismatic_dunes")
                    || path.equals("hallowed_tundra");
        } catch (final Throwable ignored) {
            return false;
        }
    }

    /**
     * Периодический спавн и проверка необходимости деспавна Пикси.
     */
    private void tickSpawnAndDespawn() {
        final List<Player> hallowPlayers = new ArrayList<>();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid() || player.getWorld().getEnvironment() != World.Environment.THE_END) {
                continue;
            }
            if (isHallowBiome(player.getLocation())) {
                hallowPlayers.add(player);
            }
        }

        // 1. Деспавн Пикси, если рядом нет игроков в биоме The Hallow
        final List<UUID> toRemove = new ArrayList<>();
        for (final Map.Entry<UUID, PixieTracker> entry : trackedPixies.entrySet()) {
            final Entity entity = entry.getValue().entity();
            if (!entity.isValid() || entity.isDead()) {
                toRemove.add(entry.getKey());
                continue;
            }

            final Location entityLoc = entity.getLocation();
            boolean playerNearbyInHallow = false;
            for (final Player p : hallowPlayers) {
                if (p.getWorld().equals(entityLoc.getWorld())
                        && p.getLocation().distanceSquared(entityLoc) <= (DESPAWN_CHECK_RADIUS * DESPAWN_CHECK_RADIUS)) {
                    playerNearbyInHallow = true;
                    break;
                }
            }

            if (!playerNearbyInHallow) {
                entity.remove();
                toRemove.add(entry.getKey());
            }
        }
        for (final UUID id : toRemove) {
            trackedPixies.remove(id);
        }

        if (hallowPlayers.isEmpty()) {
            return;
        }

        // 2. Спавн новых Пикси вокруг игроков в биоме The Hallow
        final Random random = ThreadLocalRandom.current();
        for (final Player player : hallowPlayers) {
            final Location playerLoc = player.getLocation();
            final World world = player.getWorld();

            // Подсчет существующих Пикси вокруг игрока
            int nearbyPixies = 0;
            for (final PixieTracker tracker : trackedPixies.values()) {
                final Entity entity = tracker.entity();
                if (entity.isValid() && entity.getWorld().equals(world)
                        && entity.getLocation().distanceSquared(playerLoc) <= (SPAWN_CHECK_RADIUS * SPAWN_CHECK_RADIUS)) {
                    nearbyPixies++;
                }
            }

            if (nearbyPixies >= MAX_PIXIES_PER_PLAYER_RADIUS) {
                continue;
            }

            // Попытка найти валидную точку для спавна
            final int offsetX = (random.nextBoolean() ? 1 : -1) * (14 + random.nextInt(14));
            final int offsetZ = (random.nextBoolean() ? 1 : -1) * (14 + random.nextInt(14));
            final int targetX = playerLoc.getBlockX() + offsetX;
            final int targetZ = playerLoc.getBlockZ() + offsetZ;

            final int highestY = world.getHighestBlockYAt(targetX, targetZ);
            if (highestY <= 0 || highestY >= 250) {
                continue;
            }

            final Location spawnLoc = new Location(world, targetX + 0.5D, highestY + 2.5D + random.nextInt(3), targetZ + 0.5D);
            if (!isHallowBiome(spawnLoc) || !spawnLoc.getBlock().getType().isAir()) {
                continue;
            }

            // 95% Пикси, 5% Эллей
            if (random.nextDouble() < 0.05D) {
                spawnHallowAllay(spawnLoc);
            } else {
                spawnPixie(spawnLoc, random.nextInt(4));
            }
        }
    }

    /**
     * Создание сущности Пикси.
     */
    public Vex spawnPixie(final Location location, final int colorVariantIndex) {
        final World world = location.getWorld();
        if (world == null) {
            return null;
        }

        final int variant = Math.max(0, Math.min(3, colorVariantIndex));

        final Vex vex = world.spawn(location, Vex.class, entity -> {
            entity.setSilent(true);
            entity.customName(Component.text("Пикси", TextColor.color(0xFFD655)));
            entity.setCustomNameVisible(false);

            final AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(5.0D);
            }
            entity.setHealth(5.0D);

            final AttributeInstance knockbackRes = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (knockbackRes != null) {
                knockbackRes.setBaseValue(0.9D);
            }

            final AttributeInstance followRange = entity.getAttribute(Attribute.FOLLOW_RANGE);
            if (followRange != null) {
                followRange.setBaseValue(0.0D);
            }

            entity.setTarget(null);
            entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
            entity.setCanPickupItems(false);

            final org.bukkit.inventory.EntityEquipment equipment = entity.getEquipment();
            if (equipment != null) {
                equipment.clear();
                equipment.setItemInMainHand(null);
                equipment.setItemInOffHand(null);
                equipment.setItemInMainHandDropChance(0.0f);
                equipment.setItemInOffHandDropChance(0.0f);
            }

            final PersistentDataContainer pdc = entity.getPersistentDataContainer();
            pdc.set(pixieKey, PersistentDataType.BOOLEAN, true);
            pdc.set(variantKey, PersistentDataType.INTEGER, variant);
        });

        if (vex != null && vex.isValid()) {
            trackedPixies.put(vex.getUniqueId(), new PixieTracker(vex, variant, ThreadLocalRandom.current().nextInt(60)));
        }
        return vex;
    }

    /**
     * Редкий натуральный спавн Эллея в биоме The Hallow (5%).
     */
    private void spawnHallowAllay(final Location location) {
        final World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawn(location, Allay.class, allay -> {
            allay.getPersistentDataContainer().set(hallowAllayKey, PersistentDataType.BOOLEAN, true);
            world.spawnParticle(Particle.DUST, location, 15, 0.2, 0.2, 0.2, 0.0,
                    new Particle.DustOptions(Color.fromRGB(30, 185, 255), 1.0f));
        });
    }

    /**
     * Отрисовка частиц и проигрывание окружающих звуков для активных Пикси.
     */
    private void tickParticlesAndSounds() {
        if (trackedPixies.isEmpty()) {
            return;
        }

        final List<UUID> dead = new ArrayList<>();

        for (final Map.Entry<UUID, PixieTracker> entry : trackedPixies.entrySet()) {
            final PixieTracker tracker = entry.getValue();
            final Entity entity = tracker.entity();

            if (!entity.isValid() || entity.isDead()) {
                dead.add(entry.getKey());
                continue;
            }

            final Location loc = entity.getLocation();
            final World world = loc.getWorld();
            if (world == null) {
                dead.add(entry.getKey());
                continue;
            }

            if (entity instanceof LivingEntity living) {
                final org.bukkit.inventory.EntityEquipment eq = living.getEquipment();
                if (eq != null && (!eq.getItemInMainHand().getType().isAir() || !eq.getItemInOffHand().getType().isAir())) {
                    eq.clear();
                    eq.setItemInMainHand(null);
                    eq.setItemInOffHand(null);
                }
            }

            // Отрисовка 2-х цветных частиц пыли
            final int variant = tracker.variant();
            final Color[] colors = PIXIE_COLORS[variant];
            final Location particleLoc = loc.clone().add(0, 0.35D, 0);

            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.12, 0.12, 0.12, 0.0,
                    new Particle.DustOptions(colors[0], 1.25f));
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.12, 0.12, 0.12, 0.0,
                    new Particle.DustOptions(colors[1], 1.25f));

            // Фоновый мерцающий звук
            tracker.ambientTimer++;
            if (tracker.ambientTimer >= 80) {
                tracker.ambientTimer = 0;
                world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.NEUTRAL, 0.35f, 1.8f);
            }
        }

        for (final UUID id : dead) {
            trackedPixies.remove(id);
        }
    }

    /**
     * Проверка, является ли сущность нашей Пикси через PDC.
     */
    public boolean isPixie(final Entity entity) {
        if (entity == null) {
            return false;
        }
        return entity.getPersistentDataContainer().has(pixieKey, PersistentDataType.BOOLEAN);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(final EntityTargetLivingEntityEvent event) {
        if (isPixie(event.getEntity())) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPixieAttack(final EntityDamageByEntityEvent event) {
        if (isPixie(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPixieInteract(final org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (isPixie(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPixieInteractAt(final org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        if (isPixie(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPixiePickup(final org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (isPixie(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPixieHurt(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        if (!isPixie(entity)) {
            return;
        }
        final Location loc = entity.getLocation();
        final World world = loc.getWorld();
        if (world != null) {
            world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.NEUTRAL, 0.5f, 1.85f);
            world.spawnParticle(Particle.CRIT, loc.clone().add(0, 0.35D, 0), 8, 0.15, 0.15, 0.15, 0.1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPixieDeath(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        if (!isPixie(entity)) {
            return;
        }

        trackedPixies.remove(entity.getUniqueId());
        event.getDrops().clear();
        event.setDroppedExp(1);

        final Location loc = entity.getLocation();
        final World world = loc.getWorld();
        if (world == null) {
            return;
        }

        // Эффект смерти и звук
        world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.NEUTRAL, 0.6f, 1.9f);
        world.spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.35D, 0), 12, 0.2, 0.2, 0.2, 0.05);

        // Расчет дропа Звёздной сажи (starlight_soot)
        final Random random = ThreadLocalRandom.current();
        int amount = random.nextInt(3); // 0..2

        final Player killer = entity.getKiller();
        if (killer != null) {
            final ItemStack mainHand = killer.getInventory().getItemInMainHand();
            final int looting = mainHand.getEnchantmentLevel(Enchantment.LOOTING);
            if (looting > 0) {
                amount += random.nextInt(looting + 1);
            }
        }

        if (amount > 0) {
            final Optional<CustomItemDefinition> sootDef = itemDefinitionRegistry.findByPdcItemId("starlight_soot");
            final ItemStack dropItem;
            if (sootDef.isPresent()) {
                dropItem = customItemFactory.createAlwaysLore(sootDef.get());
            } else {
                dropItem = new ItemStack(org.bukkit.Material.GLOWSTONE_DUST);
            }
            dropItem.setAmount(amount);
            world.dropItemNaturally(loc, dropItem);
        }
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        for (final Entity entity : event.getChunk().getEntities()) {
            if (isPixie(entity)) {
                trackedPixies.remove(entity.getUniqueId());
                entity.remove();
            }
        }
    }

    /**
     * Полная очистка при выключении плагина.
     */
    public void cleanupAll() {
        for (final BukkitTask task : tasks) {
            task.cancel();
        }
        tasks.clear();

        for (final PixieTracker tracker : trackedPixies.values()) {
            final Entity entity = tracker.entity();
            if (entity.isValid()) {
                entity.remove();
            }
        }
        trackedPixies.clear();
    }

    private static final class PixieTracker {
        private final Entity entity;
        private final int variant;
        private int ambientTimer;

        private PixieTracker(final Entity entity, final int variant, final int ambientTimer) {
            this.entity = entity;
            this.variant = variant;
            this.ambientTimer = ambientTimer;
        }

        public Entity entity() {
            return entity;
        }

        public int variant() {
            return variant;
        }
    }
}
