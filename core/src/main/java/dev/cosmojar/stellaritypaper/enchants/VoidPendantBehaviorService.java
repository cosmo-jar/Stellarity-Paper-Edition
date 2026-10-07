package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.registry.KeyFactory;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Поведение технических чар Void Pendant
 * Использует кэшированное O(1) состояние игрока с обновлением по событиям инвентаря.
 */
public final class VoidPendantBehaviorService implements Listener {

    public static final class PendantState {
        public boolean hasVision;
        public boolean hasPower;
        public boolean hasFuror;
        public boolean hasLuck;
        public boolean hasSpelunking;
        public boolean hasRage;
        public boolean hasAgility;
        public int ironLevel;
        public int lapisLevel;
    }

    private final Plugin plugin;
    private final MessageService messageService;

    private final NamespacedKey powerKey;
    private final NamespacedKey luckKey;
    private final NamespacedKey attackSpeedKey;
    private final NamespacedKey spelunkingBreakSpeedKey;
    private final NamespacedKey spelunkingEfficiencyKey;
    private final NamespacedKey fortitudeToughnessKey;
    private final NamespacedKey rageDamageKey;
    private final NamespacedKey rageSweepingKey;
    private final NamespacedKey agilitySpeedKey;
    private final NamespacedKey agilityJumpKey;
    private final NamespacedKey agilityEfficiencyKey;
    private final NamespacedKey enchantsDataKey;

    private final Map<UUID, PendantState> playerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Long> visionCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> visionTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeGlowTasks = new ConcurrentHashMap<>();
    private final Set<UUID> pendingZaps = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public VoidPendantBehaviorService(
            final Plugin plugin,
            final MessageService messageService,
            final KeyFactory keyFactory
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.powerKey = keyFactory.stellarity("enchants.void_pendant.power");
        this.luckKey = keyFactory.stellarity("enchants.void_pendant.luck");
        this.attackSpeedKey = keyFactory.stellarity("enchants.void_pendant.diamond.attack_speed");
        this.spelunkingBreakSpeedKey = keyFactory.stellarity("enchants.void_pendant.spelunking.break_speed");
        this.spelunkingEfficiencyKey = keyFactory.stellarity("enchants.void_pendant.spelunking.efficiency");
        this.fortitudeToughnessKey = keyFactory.stellarity("enchants.void_pendant.fortitude.armor_toughness");
        this.rageDamageKey = keyFactory.stellarity("enchants.void_pendant.rage.damage");
        this.rageSweepingKey = keyFactory.stellarity("enchants.void_pendant.rage.sweeping");
        this.agilitySpeedKey = keyFactory.stellarity("enchants.void_pendant.agility.speed");
        this.agilityJumpKey = keyFactory.stellarity("enchants.void_pendant.agility.jump");
        this.agilityEfficiencyKey = keyFactory.stellarity("enchants.void_pendant.agility.efficiency");
        this.enchantsDataKey = keyFactory.stellarity("enchants_data");
    }

    /**
     * Быстрое получение закэшированного состояния кулона игрока (O(1)).
     */
    public PendantState getPendantState(final Player player) {
        if (player == null) {
            return null;
        }
        return playerStates.get(player.getUniqueId());
    }

    /**
     * Пересчет пассивных эффектов игрока при изменении инвентаря.
     */
    public void recalculatePassives(final Player player) {
        rescan(player);
    }

    /**
     * Мгновенное сканирование инвентаря игрока и кэширование активных эффектов.
     */
    public void rescan(final Player player) {
        if (player == null || !player.isOnline()) {
            if (player != null) {
                clear(player);
            }
            return;
        }

        final PendantState state = new PendantState();
        for (final ItemStack item : player.getInventory().getContents()) {
            inspectPendant(item, state);
        }
        for (final ItemStack item : player.getInventory().getArmorContents()) {
            inspectPendant(item, state);
        }
        inspectPendant(player.getInventory().getItemInOffHand(), state);

        playerStates.put(player.getUniqueId(), state);
        applyModifiers(player, state);
        updateVisionTask(player, state.hasVision);
    }

    /**
     * Планирование сканирования инвентаря.
     */
    public void scheduleRescan(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> rescan(player));
    }

    private void inspectPendant(final ItemStack item, final PendantState state) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        // 1. Проверяем настоящие зарегистрированные зачарования Minecraft / Bukkit (Registry.ENCHANTMENT)
        for (final Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            applyGem(entry.getKey().getKey().toString(), entry.getValue(), state);
        }

        // 2. Дополнительно проверяем сохранённые данные чар в PDC предмета (если есть)
        if (item.hasItemMeta()) {
            final org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            final String pdcData = pdc.get(enchantsDataKey, org.bukkit.persistence.PersistentDataType.STRING);
            if (pdcData != null && pdcData.contains("void_pendant")) {
                applyGem(pdcData, 1, state);
            }
        }
    }

    private void applyGem(final String enchantId, final int level, final PendantState state) {
        if (enchantId == null) {
            return;
        }
        final String id = enchantId.toLowerCase(Locale.ROOT);
        if (!id.contains("void_pendant")) {
            return;
        }
        if (id.contains("amethyst")) {
            state.hasVision = true;
        } else if (id.contains("copper")) {
            state.hasPower = true;
        } else if (id.contains("diamond")) {
            state.hasFuror = true;
        } else if (id.contains("emerald")) {
            state.hasLuck = true;
        } else if (id.contains("gold")) {
            state.hasSpelunking = true;
        } else if (id.contains("iron")) {
            state.ironLevel = Math.max(state.ironLevel, Math.max(1, level));
        } else if (id.contains("lapis")) {
            state.lapisLevel = Math.max(state.lapisLevel, Math.max(1, level));
        } else if (id.contains("netherite")) {
            state.hasRage = true;
        } else if (id.contains("quartz")) {
            state.hasAgility = true;
        }
    }

    private void applyModifiers(final Player player, final PendantState state) {
        // Power (Copper): атаки создают электрические дуги; сохраняем базовый бонус к урону
        setModifier(player, Attribute.ATTACK_DAMAGE, powerKey, state.hasPower ? 0.1D : 0.0D, AttributeModifier.Operation.ADD_SCALAR);
        // Fortune (Emerald): +1 Luck
        setModifier(player, Attribute.LUCK, luckKey, state.hasLuck ? 1.0D : 0.0D, AttributeModifier.Operation.ADD_NUMBER);
        // Furor (Diamond): +6% Attack Speed
        setModifier(player, Attribute.ATTACK_SPEED, attackSpeedKey, state.hasFuror ? 0.06D : 0.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        // Spelunking (Gold): +15% Mining Speed, +5 Mining Efficiency
        setModifier(player, Attribute.BLOCK_BREAK_SPEED, spelunkingBreakSpeedKey, state.hasSpelunking ? 0.15D : 0.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        setModifier(player, Attribute.MINING_EFFICIENCY, spelunkingEfficiencyKey, state.hasSpelunking ? 5.0D : 0.0D, AttributeModifier.Operation.ADD_NUMBER);
        // Fortitude (Iron): +1 Armor Toughness (вместо поглощения урона)
        setModifier(player, Attribute.ARMOR_TOUGHNESS, fortitudeToughnessKey, state.ironLevel > 0 ? (1.0D * state.ironLevel) : 0.0D, AttributeModifier.Operation.ADD_NUMBER);
        // Rage (Netherite): +2 Attack Damage, +35% Sweeping Damage Ratio
        setModifier(player, Attribute.ATTACK_DAMAGE, rageDamageKey, state.hasRage ? 2.0D : 0.0D, AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.SWEEPING_DAMAGE_RATIO, rageSweepingKey, state.hasRage ? 0.35D : 0.0D, AttributeModifier.Operation.ADD_NUMBER);
        // Agility (Quartz): +4% Movement Speed, +4% Jump Strength, +0.04 Movement Efficiency
        setModifier(player, Attribute.MOVEMENT_SPEED, agilitySpeedKey, state.hasAgility ? 0.04D : 0.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        setModifier(player, Attribute.JUMP_STRENGTH, agilityJumpKey, state.hasAgility ? 0.04D : 0.0D, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        setModifier(player, Attribute.MOVEMENT_EFFICIENCY, agilityEfficiencyKey, state.hasAgility ? 0.04D : 0.0D, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Обработка получения урона игроком (Wisdom / Lapis):
     * При получении урона от мобов с вероятностью 5% дропается 5 опыта.
     */
    public void onEntityDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        final PendantState state = getPendantState(player);
        if (state == null || state.lapisLevel <= 0) {
            return;
        }

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity attacker = byEntity.getDamager();
            if (attacker instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
                attacker = shooter;
            }
            if (attacker != null && !attacker.equals(player)) {
                if (ThreadLocalRandom.current().nextDouble() < 0.05D) {
                    player.getWorld().spawn(player.getLocation(), ExperienceOrb.class, orb -> orb.setExperience(5));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);
                }
            }
        }
    }

    /**
     * Обработка нанесения урона игроком (Power / Copper):
     * 15% шанс вызвать электрическую.. чё то там...
     */
    public void onDamage(final EntityDamageByEntityEvent event) {
        final Player attacker = resolvePlayerDamager(event.getDamager());
        if (attacker == null || !(event.getEntity() instanceof LivingEntity victim) || victim.equals(attacker)) {
            return;
        }
        final PendantState state = getPendantState(attacker);
        if (state == null || !state.hasPower) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() < 0.15D) {
            if (pendingZaps.contains(victim.getUniqueId())) {
                return;
            }
            pendingZaps.add(victim.getUniqueId());
            try {
                victim.getWorld().spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        victim.getLocation().add(0, 1.0, 0),
                        10, 0.3, 0.3, 0.3, 0.1
                );
                try {
                    victim.getWorld().playSound(victim.getLocation(), "stellarity:item.void_pendant.zap", SoundCategory.PLAYERS, 1.0f, 2.0f);
                } catch (final Throwable ignored) {
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.4f, 2.0f);
                }
                victim.damage(4.0D, attacker);
            } finally {
                pendingZaps.remove(victim.getUniqueId());
            }
        }
    }

    private Player resolvePlayerDamager(final Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    /**
     * Amethyst (Vision): при приседании подсвечивает сущности в радиусе 15 блоков на 5 секунд (кд 45с).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSneak(final PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        final Player player = event.getPlayer();
        final PendantState state = getPendantState(player);
        if (state == null || !state.hasVision) {
            return;
        }

        final long now = System.currentTimeMillis();
        final long cdEnd = visionCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < cdEnd) {
            return;
        }

        visionCooldowns.put(player.getUniqueId(), now + 45_000L);

        final Set<LivingEntity> glowingEntities = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (final Entity entity : player.getNearbyEntities(15.0D, 15.0D, 15.0D)) {
            if (entity instanceof LivingEntity living && !living.equals(player)) {
                glowingEntities.add(living);
                sendGlowingPacket(player, living, true);
            }
        }

        if (!glowingEntities.isEmpty()) {
            final BukkitTask existing = activeGlowTasks.remove(player.getUniqueId());
            if (existing != null) {
                existing.cancel();
            }

            final BukkitTask glowTask = new BukkitRunnable() {
                private int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        activeGlowTasks.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    ticks += 10;
                    if (ticks >= 100) {
                        for (final LivingEntity living : glowingEntities) {
                            if (living.isValid() && !isNaturallyGlowing(living)) {
                                sendGlowingPacket(player, living, false);
                            }
                        }
                        activeGlowTasks.remove(player.getUniqueId());
                        cancel();
                        return;
                    }

                    for (final LivingEntity living : glowingEntities) {
                        if (living.isValid() && !living.isDead() && living.getWorld().equals(player.getWorld())) {
                            sendGlowingPacket(player, living, true);
                        }
                    }
                }
            }.runTaskTimer(plugin, 10L, 10L);

            activeGlowTasks.put(player.getUniqueId(), glowTask);
        }

        try {
            player.playSound(player.getLocation(), "stellarity:item.void_pendant.vision", SoundCategory.PLAYERS, 1.0f, 1.0f);
        } catch (final Throwable ignored) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            final PendantState current = getPendantState(player);
            if (current != null && current.hasVision) {
                try {
                    player.playSound(player.getLocation(), "stellarity:item.cooldown_end", SoundCategory.PLAYERS, 1.0f, 1.0f);
                } catch (final Throwable ignored) {
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
                player.sendActionBar(messageService.message("trinkets.void_pendant.amethyst_ready"));
            }
        }, 45L * 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRescan(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRescan(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            scheduleRescan(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        scheduleRescan(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        scheduleRescan(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        scheduleRescan(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    public void clear(final Player player) {
        if (player != null) {
            playerStates.remove(player.getUniqueId());
            visionCooldowns.remove(player.getUniqueId());
            removeModifier(player, Attribute.ATTACK_DAMAGE, powerKey);
            removeModifier(player, Attribute.LUCK, luckKey);
            removeModifier(player, Attribute.ATTACK_SPEED, attackSpeedKey);
            removeModifier(player, Attribute.BLOCK_BREAK_SPEED, spelunkingBreakSpeedKey);
            removeModifier(player, Attribute.MINING_EFFICIENCY, spelunkingEfficiencyKey);
            removeModifier(player, Attribute.ARMOR_TOUGHNESS, fortitudeToughnessKey);
            removeModifier(player, Attribute.ATTACK_DAMAGE, rageDamageKey);
            removeModifier(player, Attribute.SWEEPING_DAMAGE_RATIO, rageSweepingKey);
            removeModifier(player, Attribute.MOVEMENT_SPEED, agilitySpeedKey);
            removeModifier(player, Attribute.JUMP_STRENGTH, agilityJumpKey);
            removeModifier(player, Attribute.MOVEMENT_EFFICIENCY, agilityEfficiencyKey);
            final BukkitTask task = visionTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
            }
            final BukkitTask glowTask = activeGlowTasks.remove(player.getUniqueId());
            if (glowTask != null) {
                glowTask.cancel();
            }
        }
    }

    public void clearAll() {
        for (final BukkitTask task : visionTasks.values()) {
            task.cancel();
        }
        visionTasks.clear();
        for (final BukkitTask task : activeGlowTasks.values()) {
            task.cancel();
        }
        activeGlowTasks.clear();
        visionCooldowns.clear();
        playerStates.clear();
    }

    private void updateVisionTask(final Player player, final boolean enabled) {
        final UUID uuid = player.getUniqueId();
        if (!enabled) {
            final BukkitTask task = visionTasks.remove(uuid);
            if (task != null) {
                task.cancel();
            }
            return;
        }
        if (visionTasks.containsKey(uuid)) {
            return;
        }

        final BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                final BukkitTask existing = visionTasks.remove(uuid);
                if (existing != null) {
                    existing.cancel();
                }
                return;
            }
            final PendantState state = getPendantState(player);
            if (state == null || !state.hasVision) {
                final BukkitTask existing = visionTasks.remove(uuid);
                if (existing != null) {
                    existing.cancel();
                }
                return;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0, true, false, true));
        }, 1L, 80L);
        visionTasks.put(uuid, task);
    }

    private void setModifier(
            final Player player,
            final Attribute attribute,
            final NamespacedKey key,
            final double amount,
            final AttributeModifier.Operation operation
    ) {
        final AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        for (final AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                instance.removeModifier(modifier);
            }
        }
        if (Math.abs(amount) < 1.0E-9D) {
            return;
        }
        instance.addModifier(new AttributeModifier(key, amount, operation));
    }

    private void removeModifier(final Player player, final Attribute attribute, final NamespacedKey key) {
        final AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        for (final AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                instance.removeModifier(modifier);
            }
        }
    }

    private boolean isNaturallyGlowing(final Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.hasPotionEffect(PotionEffectType.GLOWING) || living.isGlowing();
        }
        return entity.isGlowing();
    }

    private void sendGlowingPacket(final Player viewer, final Entity target, final boolean glowing) {
        if (!viewer.isOnline() || !target.isValid()) {
            return;
        }
        if (!(viewer instanceof org.bukkit.craftbukkit.entity.CraftPlayer craftPlayer) || !(target instanceof org.bukkit.craftbukkit.entity.CraftEntity craftEntity)) {
            return;
        }
        try {
            final net.minecraft.world.entity.Entity nmsTarget = craftEntity.getHandle();
            final net.minecraft.server.level.ServerPlayer nmsViewer = craftPlayer.getHandle();

            byte flags = 0;
            final List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> all = nmsTarget.getEntityData().packAll();
            if (all != null) {
                for (final net.minecraft.network.syncher.SynchedEntityData.DataValue<?> dv : all) {
                    if (dv.id() == 0 && dv.value() instanceof Byte b) {
                        flags = b;
                        break;
                    }
                }
            }

            final byte newFlags = glowing ? (byte) (flags | 0x40) : (byte) (flags & ~0x40);
            final net.minecraft.network.syncher.SynchedEntityData.DataValue<Byte> dataValue =
                    new net.minecraft.network.syncher.SynchedEntityData.DataValue<>(0, net.minecraft.network.syncher.EntityDataSerializers.BYTE, newFlags);
            final net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket packet =
                    new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(nmsTarget.getId(), List.of(dataValue));

            nmsViewer.connection.send(packet);
        } catch (final Throwable ignored) {
        }
    }
}
