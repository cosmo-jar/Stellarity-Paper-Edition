package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.items.CustomItemMatcher;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import dev.cosmojar.stellaritypaper.text.MessageService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TrinketBehaviorService {

    private final org.bukkit.plugin.Plugin plugin;
    private final CustomItemMatcher itemMatcher;
    private final StellaritySoundService soundService;
    private final ItemDefinitionRegistry itemRegistry;
    private final CustomItemFactory itemFactory;
    private final ItemsConfigService itemsConfig;
    private final MessageService messageService;

    private final Map<UUID, CrestWrath> crestWraths = new HashMap<>();
    private final Map<UUID, Long> pearlCooldowns = new HashMap<>();
    private final Map<PlayerItemKey, Integer> elektraCharges = new HashMap<>();
    private final Map<PlayerItemKey, Long> elektraLastChargeTime = new HashMap<>();
    private final Map<UUID, LifeCrystalState> lifeCrystalStates = new HashMap<>();
    private final Set<UUID> activeSoaringPlayers = new HashSet<>();
    private final Map<UUID, ActiveAltar> activeAltars = new HashMap<>();
    private final Map<Location, AreaEffectCloud> infectedBushes = new HashMap<>();
    private final Map<UUID, Long> starstruckCooldowns = new HashMap<>();
    private final Map<UUID, Set<LivingEntity>> playerHighlightedEntities = new HashMap<>();

    private final NamespacedKey crestAttackKey;
    private final NamespacedKey soaringGravityKey;
    private final NamespacedKey soaringFallDamageKey;
    private final NamespacedKey prismaticPearlKey;
    private final NamespacedKey itemInstanceUuidKey;
    private final RadiantJewelService radiantJewelService;
    private final CustomStatusEffectService customStatusEffectService;
    private BukkitTask task;

    public TrinketBehaviorService(
            final org.bukkit.plugin.Plugin plugin,
            final CustomItemMatcher itemMatcher,
            final StellaritySoundService soundService,
            final ItemDefinitionRegistry itemRegistry,
            final CustomItemFactory itemFactory,
            final ItemsConfigService itemsConfig,
            final MessageService messageService,
            final RadiantJewelService radiantJewelService,
            final CustomStatusEffectService customStatusEffectService
    ) {
        this.plugin = plugin;
        this.itemMatcher = itemMatcher;
        this.soundService = soundService;
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.itemsConfig = itemsConfig;
        this.messageService = messageService;
        this.radiantJewelService = radiantJewelService;
        this.customStatusEffectService = customStatusEffectService;
        this.crestAttackKey = new NamespacedKey(plugin, "trinket.crest.attack");
        this.soaringGravityKey = new NamespacedKey(plugin, "trinket.soaring.gravity");
        this.soaringFallDamageKey = new NamespacedKey(plugin, "trinket.soaring.falldamage");
        this.prismaticPearlKey = new NamespacedKey(plugin, "trinket.prismatic_pearl");
        this.itemInstanceUuidKey = new NamespacedKey(plugin, "item_instance_uuid");
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        crestWraths.clear();
        pearlCooldowns.clear();
        elektraCharges.clear();
        elektraLastChargeTime.clear();
        lifeCrystalStates.clear();
        activeSoaringPlayers.clear();
        for (final ActiveAltar altar : activeAltars.values()) {
            if (altar.marker() != null && altar.marker().isValid()) altar.marker().remove();
            if (altar.display() != null && altar.display().isValid()) altar.display().remove();
            if (altar.particleTask() != null) altar.particleTask().cancel();
        }
        activeAltars.clear();
        infectedBushes.clear();
        playerHighlightedEntities.clear();
    }

    public boolean isCrestOfTheEndEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("crest_of_the_end.enabled", true); }
    public boolean isDragonsEyeEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("dragons_eye.enabled", true); }
    public boolean isDuskberryEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("duskberry.enabled", true); }
    public boolean isCopperElektraShieldEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("copper_elektra_shield.enabled", true); }
    public boolean isLifeCrystalEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("life_crystal.enabled", true); }
    public boolean isLivingFleshEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("living_flesh.enabled", true); }
    public boolean isPrismaticPearlEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("prismatic_pearl.enabled", true); }
    public boolean isSatchelOfVoidsEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("satchel_of_voids.enabled", true); }
    public boolean isSoaringInsigniaEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("soaring_insignia.enabled", true); }
    public boolean isStarstruckShieldEnabled() { return itemsConfig.getTrinketsConfig().getBoolean("starstruck_shield.enabled", true); }

    public void syncPlayer(final Player player) {
        if (!isHoldingTrinket(player, "crest_of_the_end")) {
            removeModifier(player, Attribute.ATTACK_DAMAGE, crestAttackKey);
            crestWraths.remove(player.getUniqueId());
        }
        if (!isHoldingTrinket(player, "soaring_insignia")) {
            activeSoaringPlayers.remove(player.getUniqueId());
            removeModifier(player, Attribute.GRAVITY, soaringGravityKey);
            removeModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, soaringFallDamageKey);
        }
    }

    public void clear(final Player player) {
        final UUID uuid = player.getUniqueId();
        removeModifier(player, Attribute.ATTACK_DAMAGE, crestAttackKey);
        removeModifier(player, Attribute.GRAVITY, soaringGravityKey);
        removeModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, soaringFallDamageKey);
        crestWraths.remove(uuid);
        pearlCooldowns.remove(uuid);
        elektraCharges.keySet().removeIf(key -> key.playerUuid.equals(uuid));
        elektraLastChargeTime.keySet().removeIf(key -> key.playerUuid.equals(uuid));
        lifeCrystalStates.remove(uuid);
        activeSoaringPlayers.remove(uuid);
        starstruckCooldowns.remove(uuid);
        final ActiveAltar altar = activeAltars.remove(uuid);
        if (altar != null) {
            if (altar.marker() != null && altar.marker().isValid()) altar.marker().remove();
            if (altar.display() != null && altar.display().isValid()) altar.display().remove();
            if (altar.particleTask() != null) altar.particleTask().cancel();
            altar.location().getWorld().playSound(altar.location(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2F, 1.5F);
        }
    }

    private void tick() {
        final long now = System.currentTimeMillis();

        if (isDuskberryEnabled() && !infectedBushes.isEmpty()) {
            final List<Location> toRemove = new ArrayList<>();
            
            for (final Location loc : infectedBushes.keySet()) {
                final org.bukkit.block.Block block = loc.getBlock();
                
                if (block.getType() != Material.SWEET_BERRY_BUSH) {
                    toRemove.add(loc);
                } else {
                    boolean playerNear = false;
                    for (final Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distanceSquared(loc) <= 400.0D) {
                            playerNear = true;
                            break;
                        }
                    }

                    if (playerNear) {
                        final Location particleLoc = loc.clone().add(0.5D, 0.5D, 0.5D);
                        loc.getWorld().spawnParticle(
                                Particle.DRAGON_BREATH,
                                particleLoc,
                                5,
                                0.25D, 0.25D, 0.25D,
                                0.015D,
                                1.0F
                        );
                        
                        for (final Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distanceSquared(loc) <= 4.0D) {
                                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 0));
                                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                            }
                        }
                    }
                }
            }
            
            for (final Location loc : toRemove) {
                infectedBushes.remove(loc);
            }
        }

        activeAltars.values().removeIf(altar -> {
            if (now >= altar.expiresAt()) {
                if (altar.marker() != null && altar.marker().isValid()) altar.marker().remove();
                if (altar.display() != null && altar.display().isValid()) altar.display().remove();
                if (altar.particleTask() != null) altar.particleTask().cancel();
                altar.location().getWorld().playSound(altar.location(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2F, 1.5F);
                return true;
            }
            return false;
        });

        for (final Player player : Bukkit.getOnlinePlayers()) {
            final UUID uuid = player.getUniqueId();

            if (hasInInventory(player, "dragons_eye") && isDragonsEyeEnabled()) {
                if (player.getTicksLived() % 20 == 0) {
                    cleanseDragonsEyeDebuffs(player);
                }
            }

            if (isHoldingTrinket(player, "life_crystal") && isLifeCrystalEnabled()) {
                processLifeCrystal(player);
            } else {
                lifeCrystalStates.remove(uuid);
            }

            final CrestWrath wrath = crestWraths.get(uuid);
            if (wrath != null) {
                final long decay = itemsConfig.getTrinketsConfig().getLong("crest_of_the_end.wrath-decay-time-ms", 4000L);
                if (now - wrath.lastBlockTime >= decay) {
                    removeModifier(player, Attribute.ATTACK_DAMAGE, crestAttackKey);
                    crestWraths.remove(uuid);
                }
            }

            if (isHoldingTrinket(player, "copper_elektra_shield") && isCopperElektraShieldEnabled()) {
                processElektraChargeRecovery(player, now);
            }

            if (isSoaringInsigniaEnabled()) {
                processSoaringInsigniaFlight(player);
            }

            if (isHoldingTrinket(player, "bell_flower")) {
                final Set<LivingEntity> currentHighlighted = new HashSet<>();
                for (final Entity entity : player.getNearbyEntities(15.0D, 15.0D, 15.0D)) {
                    if (entity instanceof LivingEntity living && !entity.equals(player) && !(entity instanceof org.bukkit.entity.ArmorStand) && living.isValid()) {
                        currentHighlighted.add(living);
                    }
                }

                final Set<LivingEntity> oldHighlighted = playerHighlightedEntities.getOrDefault(uuid, java.util.Collections.emptySet());

                for (final LivingEntity oldEntity : oldHighlighted) {
                    if (!currentHighlighted.contains(oldEntity) && oldEntity.isValid()) {
                        oldEntity.removePotionEffect(PotionEffectType.GLOWING);
                    }
                }

                for (final LivingEntity newEntity : currentHighlighted) {
                    newEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, false));
                }

                playerHighlightedEntities.put(uuid, currentHighlighted);
            } else {
                final Set<LivingEntity> oldHighlighted = playerHighlightedEntities.remove(uuid);
                if (oldHighlighted != null) {
                    for (final LivingEntity oldEntity : oldHighlighted) {
                        if (oldEntity.isValid()) {
                            oldEntity.removePotionEffect(PotionEffectType.GLOWING);
                        }
                    }
                }
            }
        }
    }



    private void cleanseDragonsEyeDebuffs(final Player player) {
        final PotionEffectType[] badEffects = {
                PotionEffectType.BLINDNESS, PotionEffectType.DARKNESS, PotionEffectType.WEAKNESS,
                PotionEffectType.SLOWNESS, PotionEffectType.NAUSEA, PotionEffectType.HUNGER,
                PotionEffectType.MINING_FATIGUE
        };
        for (final PotionEffectType effect : badEffects) {
            if (player.hasPotionEffect(effect)) {
                player.removePotionEffect(effect);
            }
        }
    }

    private void processLifeCrystal(final Player player) {
        final LifeCrystalState state = lifeCrystalStates.computeIfAbsent(player.getUniqueId(), k -> new LifeCrystalState(player.getLocation()));
        final double dx = player.getLocation().getX() - state.lastLoc.getX();
        final double dz = player.getLocation().getZ() - state.lastLoc.getZ();
        final double distSq = dx * dx + dz * dz;

        if (distSq > 0.01D) {
            state.charge = 0;
        } else {
            state.charge = Math.min(300, state.charge + 5);

            if (state.charge > 0) {
                final double maxHealingRate = itemsConfig.getTrinketsConfig().getDouble("life_crystal.healing-rate", 3.0D);
                final double healAmount = (state.charge * maxHealingRate) / 600.0D;
                final AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                final double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0D;
                player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));

                int particleCount = 2;
                if (state.charge >= 300) particleCount = 10;
                else if (state.charge >= 225) particleCount = 8;
                else if (state.charge >= 150) particleCount = 6;
                else if (state.charge >= 75) particleCount = 4;

                player.getWorld().spawnParticle(
                        Particle.PORTAL,
                        player.getLocation().add(0.0D, 1.05D, 0.0D),
                        particleCount, 0.0D, 0.0D, 0.0D, 0.87D
                );
            }
            if (player.getTicksLived() % 20 == 0 && state.charge > 0) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4F, 1.2F);
            }
        }
        state.lastLoc = player.getLocation();
    }

    private void processElektraChargeRecovery(final Player player, final long now) {
        final UUID playerUuid = player.getUniqueId();
        for (final EquipmentSlot slot : List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)) {
            final ItemStack item = player.getInventory().getItem(slot);
            if (isCustomItem(item, "copper_elektra_shield")) {
                final UUID itemUuid = getOrCreateItemUuid(item);
                if (itemUuid != null) {
                    final PlayerItemKey key = new PlayerItemKey(playerUuid, itemUuid);
                    final int current = elektraCharges.getOrDefault(key, 3);
                    if (current < 3) {
                        final long lastCharge = elektraLastChargeTime.getOrDefault(key, 0L);
                        final long cd = itemsConfig.getTrinketsConfig().getLong("copper_elektra_shield.charge-cooldown-ms", 3000L);
                        if (now - lastCharge >= cd) {
                            elektraCharges.put(key, current + 1);
                            elektraLastChargeTime.put(key, now);
                            if (slot == EquipmentSlot.HAND) {
                                sendElektraActionBar(player, current + 1);
                            }
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5F, 1.5F);
                        }
                    }
                }
            }
        }
    }

    private void sendElektraActionBar(final Player player, final int charges) {
        final StringBuilder sb = new StringBuilder("⚡ [");
        for (int i = 0; i < 3; i++) {
            if (i < charges) {
                sb.append("||");
            } else {
                sb.append("  ");
            }
            if (i < 2) sb.append("|");
        }
        sb.append("] ⚡");
        player.sendActionBar(net.kyori.adventure.text.Component.text(sb.toString(), net.kyori.adventure.text.format.TextColor.fromHexString("#55FFFF")));
    }

    private void processSoaringInsigniaFlight(final Player player) {
        final UUID uuid = player.getUniqueId();
        
        if (player.isOnGround() || player.getLocation().getBlock().isLiquid()) {
            if (activeSoaringPlayers.remove(uuid)) {
                removeModifier(player, Attribute.GRAVITY, soaringGravityKey);
                removeModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, soaringFallDamageKey);
            }
            return;
        }

        final boolean isFlying = activeSoaringPlayers.contains(uuid);

        if (!isFlying) {
            if (player.isSneaking() && isHoldingTrinket(player, "soaring_insignia")) {
                if (player.getLocation().getPitch() <= -15.0D) {
                    activeSoaringPlayers.add(uuid);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 1, 0, true, false, false));
                    setModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, soaringFallDamageKey, -1.0D, AttributeModifier.Operation.ADD_SCALAR);
                    setModifier(player, Attribute.GRAVITY, soaringGravityKey, -1.1D, AttributeModifier.Operation.ADD_SCALAR);
                }
            }
        } else {
            setModifier(player, Attribute.FALL_DAMAGE_MULTIPLIER, soaringFallDamageKey, -1.0D, AttributeModifier.Operation.ADD_SCALAR);

            if (player.isSneaking()) {
                final double pitch = player.getLocation().getPitch();
                if (pitch <= -15.0D) {
                    setModifier(player, Attribute.GRAVITY, soaringGravityKey, -1.1D, AttributeModifier.Operation.ADD_SCALAR);
                } else if (pitch >= 15.0D) {
                    setModifier(player, Attribute.GRAVITY, soaringGravityKey, -0.9D, AttributeModifier.Operation.ADD_SCALAR);
                } else {
                    setModifier(player, Attribute.GRAVITY, soaringGravityKey, -1.0D, AttributeModifier.Operation.ADD_SCALAR);
                }
            } else {
                setModifier(player, Attribute.GRAVITY, soaringGravityKey, -1.0D, AttributeModifier.Operation.ADD_SCALAR);
            }

            if (player.getTicksLived() % 2 == 0) {
                player.getWorld().spawnParticle(
                        Particle.CLOUD,
                        player.getLocation(),
                        1, 0.1D, 0.1D, 0.1D, 0.01D
                );
            }
        }
    }

    public void onEntityTarget(final EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        if (event.getEntity() instanceof Enderman || event.getEntity() instanceof Phantom) {
            if (hasInInventory(player, "dragons_eye") && isDragonsEyeEnabled()) {
                event.setCancelled(true);
                event.setTarget(null);
            }
        }
    }

    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        if (event.getAction().name().contains("RIGHT_CLICK") && event.getClickedBlock() != null) {
            final org.bukkit.block.Block block = event.getClickedBlock();
            if (block.getType() == Material.SWEET_BERRY_BUSH) {
                final Location loc = block.getLocation();
                if (infectedBushes.containsKey(loc)) {
                    final org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) block.getBlockData();
                    if (ageable.getAge() >= 2) {
                        event.setCancelled(true);

                        ageable.setAge(1);
                        block.setBlockData(ageable);

                        final ItemStack duskberry = getDuskberryItem();
                        loc.getWorld().dropItemNaturally(loc.clone().add(0.5D, 0.5D, 0.5D), duskberry);

                        loc.getWorld().playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
                        return;
                    }
                }
            }
        }

        if (item == null || item.getType().isAir()) {
            return;
        }

        final String customId = itemMatcher.resolveRawId(item).orElse("");
        if (customId.isEmpty()) {
            return;
        }

        if ("prismatic_pearl".equalsIgnoreCase(customId) && isPrismaticPearlEnabled()) {
            if (event.getAction().name().contains("RIGHT_CLICK")) {
                event.setCancelled(true);
                final UUID uuid = player.getUniqueId();
                final long now = System.currentTimeMillis();
                final long cdUntil = pearlCooldowns.getOrDefault(uuid, 0L);

                if (now < cdUntil) {
                    soundService.play(player, "stellarity:item.prismatic_pearl.cooldown", 0.8F, 0.7F, Sound.BLOCK_AMETHYST_BLOCK_BREAK);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.5F, 0.5F);
                    return;
                }

                final ItemStack returnItem = item.clone();
                returnItem.setAmount(1);

                final EnderPearl pearl = player.launchProjectile(EnderPearl.class);
                pearl.setShooter(player);
                pearl.setItem(returnItem);
                final double velMult = itemsConfig.getTrinketsConfig().getDouble("prismatic_pearl.velocity-multiplier", 1.25D);
                pearl.setVelocity(pearl.getVelocity().multiply(velMult));
                pearl.getPersistentDataContainer().set(prismaticPearlKey, PersistentDataType.BOOLEAN, true);

                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }

                final long cooldownMs = itemsConfig.getTrinketsConfig().getLong("prismatic_pearl.cooldown-ms", 5000L);
                pearlCooldowns.put(uuid, now + cooldownMs);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 0.9F, 1.3F);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        final java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(returnItem);
                        for (final ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.6F, 1.2F);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.5F);
                        
                        final Location loc = player.getLocation().add(0.0D, 1.0D, 0.0D);
                        for (int k = 0; k < 20; k++) {
                            int r = ThreadLocalRandom.current().nextInt(256);
                            int g = ThreadLocalRandom.current().nextInt(256);
                            int b = ThreadLocalRandom.current().nextInt(256);
                            player.getWorld().spawnParticle(
                                    Particle.ENTITY_EFFECT,
                                    loc.clone().add(
                                            ThreadLocalRandom.current().nextDouble(-0.5D, 0.5D),
                                            ThreadLocalRandom.current().nextDouble(-0.5D, 0.5D),
                                            ThreadLocalRandom.current().nextDouble(-0.5D, 0.5D)
                                    ),
                                    1,
                                    org.bukkit.Color.fromRGB(r, g, b)
                             );
                        }
                    }
                }, cooldownMs / 50L);
            }
            return;
        }

        if ("copper_elektra_shield".equalsIgnoreCase(customId) && isCopperElektraShieldEnabled()) {
            if (event.getAction().name().contains("RIGHT_CLICK") && player.isSneaking()) {
                event.setCancelled(true);
                final UUID itemUuid = getOrCreateItemUuid(item);
                if (itemUuid != null) {
                    final PlayerItemKey key = new PlayerItemKey(player.getUniqueId(), itemUuid);
                    final int charges = elektraCharges.getOrDefault(key, 3);

                    if (charges > 0) {
                        elektraCharges.put(key, charges - 1);
                        elektraLastChargeTime.put(key, System.currentTimeMillis());
                        sendElektraActionBar(player, charges - 1);
                        triggerElektraDash(player, item);
                    } else {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.6F, 0.8F);
                    }
                }
            }
            return;
        }

        if ("satchel_of_voids".equalsIgnoreCase(customId) && isSatchelOfVoidsEnabled()) {
            if (event.getAction().name().contains("RIGHT_CLICK")) {
                event.setCancelled(true);
                if (player.getCooldown(item.getType()) > 0) {
                    return;
                }

                if (activeAltars.containsKey(player.getUniqueId())) {
                    player.sendMessage(messageService.message("trinkets.satchel_of_voids.already_active"));
                    return;
                }

                for (final ActiveAltar altar : activeAltars.values()) {
                    if (!altar.ownerUuid().equals(player.getUniqueId())) {
                        if (altar.location().getWorld().equals(player.getWorld()) && altar.location().distance(player.getLocation()) <= 10.0D) {
                            player.sendMessage(messageService.message("trinkets.satchel_of_voids.too_close"));
                            return;
                        }
                    }
                }

                final org.bukkit.util.RayTraceResult ray = player.getWorld().rayTraceBlocks(
                        player.getEyeLocation(),
                        player.getLocation().getDirection(),
                        50.0D,
                        org.bukkit.FluidCollisionMode.NEVER,
                        true
                );

                if (ray != null && ray.getHitBlock() != null) {
                    final int altarSeconds = itemsConfig.getTrinketsConfig().getInt("satchel_of_voids.altar-duration-seconds", 30);
                    player.setCooldown(item.getType(), altarSeconds * 20);

                    final Location hitLoc = ray.getHitPosition().toLocation(player.getWorld());
                    final Location loc = hitLoc.clone().add(0.0D, 0.5D, 0.0D);

                    final Marker marker = (Marker) loc.getWorld().spawnEntity(loc, EntityType.MARKER);
                    marker.addScoreboardTag("stellarity.altar_of_the_accursed");
                    marker.addScoreboardTag("stellarity.altar_of_the_accursed_activated");
                    
                    final Location sigilLoc = hitLoc.clone();
                    sigilLoc.setY(hitLoc.getY() + 0.02D);
                    sigilLoc.setPitch(90.0F);
                    sigilLoc.setYaw(0.0F);

                    final org.bukkit.entity.ItemDisplay display = sigilLoc.getWorld().spawn(sigilLoc, org.bukkit.entity.ItemDisplay.class);
                    
                    final ItemStack paperItem = new ItemStack(Material.PAPER);
                    final org.bukkit.inventory.meta.ItemMeta paperMeta = paperItem.getItemMeta();
                    if (paperMeta != null) {
                        paperMeta.setItemModel(NamespacedKey.fromString("stellarity:_particle/sigil_glow"));
                        
                        final org.bukkit.inventory.meta.components.CustomModelDataComponent cmd = paperMeta.getCustomModelDataComponent();
                        cmd.setColors(java.util.List.of(
                            org.bukkit.Color.fromRGB(156, 27, 185),
                            org.bukkit.Color.fromRGB(210, 37, 219)
                        ));
                        paperMeta.setCustomModelDataComponent(cmd);
                        paperItem.setItemMeta(paperMeta);
                    }
                    display.setItemStack(paperItem);
                    display.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIXED);
                    display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
                    display.getPersistentDataContainer().set(new NamespacedKey(plugin, "temp_display"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                    display.setPersistent(false);
                    
                    display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                    
                    display.setTransformation(new org.bukkit.util.Transformation(
                            new org.joml.Vector3f(0f, 0f, 0f),
                            new org.joml.Quaternionf(0, 0, 0, 1),
                            new org.joml.Vector3f(0f, 0f, 0f),
                            new org.joml.Quaternionf(0, 0, 0, 1)
                    ));
                    
                    display.setTeleportDuration(5);
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (display.isValid()) {
                            display.setInterpolationDelay(-1);
                            display.setInterpolationDuration(20);
                            display.setTransformation(new org.bukkit.util.Transformation(
                                    new org.joml.Vector3f(0f, 0f, 0f),
                                    new org.joml.Quaternionf(0, 0, 0, 1),
                                    new org.joml.Vector3f(3.8f, 3.8f, 0.000001f),
                                    new org.joml.Quaternionf(0, 0, 0, 1)
                            ));
                        }
                    }, 2L);

                    loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2F, 1.5F);
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc, 32, 0.3D, 0.3D, 0.3D, 0.2D, null);

                    final BukkitTask[] particleTask = new BukkitTask[1];
                    particleTask[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                        if (!marker.isValid() || !display.isValid()) {
                            if (particleTask[0] != null) particleTask[0].cancel();
                            return;
                        }
                        display.setRotation(display.getLocation().getYaw() + 4.0F, display.getLocation().getPitch());

                        final org.bukkit.Color color1 = org.bukkit.Color.fromRGB(210, 37, 219);
                        final org.bukkit.Color color2 = org.bukkit.Color.fromRGB(82, 14, 97);
                        final Particle.DustTransition transition = new Particle.DustTransition(color1, color2, 1.33F);
                        loc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 4, 0.5D, 0.1D, 0.5D, 0.0D, transition);
                    }, 0L, 2L);

                    final ActiveAltar newAltar = new ActiveAltar(player.getUniqueId(), loc, System.currentTimeMillis() + altarSeconds * 1000L, marker, display, particleTask[0]);
                    activeAltars.put(player.getUniqueId(), newAltar);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (display.isValid()) {
                            display.setInterpolationDelay(-1);
                            display.setInterpolationDuration(30);
                            display.setTransformation(new org.bukkit.util.Transformation(
                                    new org.joml.Vector3f(0f, 0f, 0f),
                                    new org.joml.Quaternionf(0, 0, 0, 1),
                                    new org.joml.Vector3f(0f, 0f, 0f),
                                    new org.joml.Quaternionf(0, 0, 0, 1)
                            ));
                        }
                    }, altarSeconds * 20L - 30L);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        final ActiveAltar altar = activeAltars.remove(player.getUniqueId());
                        if (altar != null) {
                            if (altar.marker() != null && altar.marker().isValid()) altar.marker().remove();
                            if (altar.display() != null && altar.display().isValid()) altar.display().remove();
                            if (altar.particleTask() != null) altar.particleTask().cancel();
                            altar.location().getWorld().playSound(altar.location(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2F, 1.5F);
                        }
                    }, altarSeconds * 20L);
                }
            }
            return;
        }

        if ("soaring_insignia".equalsIgnoreCase(customId) && isSoaringInsigniaEnabled()) {
            if (event.getAction().name().contains("RIGHT_CLICK")) {
                event.setCancelled(true);
            }
            return;
        }

        if ("duskberry".equalsIgnoreCase(customId) && isDuskberryEnabled()) {
            if (event.getAction().name().contains("RIGHT_CLICK") && event.getClickedBlock() != null) {
                final org.bukkit.block.Block clickedBlock = event.getClickedBlock();
                final Material blockType = clickedBlock.getType();
                
                if (blockType == Material.SWEET_BERRY_BUSH) {
                    event.setCancelled(true);
                    final Location loc = clickedBlock.getLocation();
                    if (!infectedBushes.containsKey(loc)) {
                        infectedBushes.put(loc, null);

                        final org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) clickedBlock.getBlockData();
                        ageable.setAge(2);
                        clickedBlock.setBlockData(ageable);

                        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                            item.setAmount(item.getAmount() - 1);
                        }
                        loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 0.6F);
                    }
                    return;
                }
                
                if (isPlantableBlock(blockType) && event.getBlockFace() == org.bukkit.block.BlockFace.UP) {
                    final org.bukkit.block.Block targetBlock = clickedBlock.getRelative(org.bukkit.block.BlockFace.UP);
                    if (targetBlock.getType().isAir()) {
                        event.setCancelled(true);
                        
                        targetBlock.setType(Material.SWEET_BERRY_BUSH);
                        final org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) targetBlock.getBlockData();
                        ageable.setAge(2);
                        targetBlock.setBlockData(ageable);
                        
                        final Location loc = targetBlock.getLocation();
                        infectedBushes.put(loc, null);
                        
                        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                            item.setAmount(item.getAmount() - 1);
                        }
                        loc.getWorld().playSound(loc, Sound.ITEM_CROP_PLANT, 1.0F, 0.8F);
                        loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 0.6F);
                    }
                }
            }
        }
    }

    private void triggerElektraDash(final Player player, final ItemStack item) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5F, 1.8F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 1.0F, 1.5F);

        final double dashDistance = itemsConfig.getTrinketsConfig().getDouble("copper_elektra_shield.dash-distance", 7.0D);
        final Vector dir = player.getLocation().getDirection().multiply(dashDistance / 4.375D).setY(0.1D);
        player.setVelocity(dir);

        final int dashTicks = itemsConfig.getTrinketsConfig().getInt("copper_elektra_shield.dash-duration-ticks", 4);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, dashTicks + 6, 99, true, false, false));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            final Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.CRIT, loc, 24, 0.5D, 0.5D, 0.5D, 0.2D, null);
            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 16, 0.5D, 0.5D, 0.5D, 0.1D, null);

            final List<Entity> entities = player.getNearbyEntities(3.0D, 3.0D, 3.0D);
            boolean hitEnemy = false;
            final double damage = itemsConfig.getTrinketsConfig().getDouble("copper_elektra_shield.damage", 3.0D);
            for (final Entity entity : entities) {
                if (entity instanceof LivingEntity living && living.isValid() && !living.getUniqueId().equals(player.getUniqueId())) {
                    living.damage(damage, player);
                    hitEnemy = true;

                    if (living instanceof Creeper creeper) {
                        creeper.setPowered(true);
                    }
                }
            }

            if (hitEnemy) {
                final UUID itemUuid = getOrCreateItemUuid(item);
                if (itemUuid != null) {
                    final PlayerItemKey key = new PlayerItemKey(player.getUniqueId(), itemUuid);
                    final long lastCharge = elektraLastChargeTime.getOrDefault(key, 0L);
                    final long cd = itemsConfig.getTrinketsConfig().getLong("copper_elektra_shield.charge-cooldown-ms", 3000L);
                    elektraLastChargeTime.put(key, lastCharge - cd / 2);
                }
            }
        }, dashTicks);
    }

    public void onPlayerConsume(final PlayerItemConsumeEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();
        final String customId = itemMatcher.resolveRawId(item).orElse("");

        if ("duskberry".equalsIgnoreCase(customId) && isDuskberryEnabled()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 720, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 720, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 720, 0));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FOX_EAT, 1.0F, 0.9F);
        }

        if ("living_flesh".equalsIgnoreCase(customId) && isLivingFleshEnabled()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0F, 0.8F);
        }
    }

    public void onPlayerDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (isHoldingTrinket(player, "living_flesh") && isLivingFleshEnabled()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_JUMP, 0.9F, 0.8F);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.9F, 0.9F);
            player.getWorld().spawnParticle(Particle.ITEM_SLIME, player.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.3D, 0.3D, 0.3D, 0.05D, null);
        }
    }

    public void onPlayerShieldBlock(final Player defender) {
        if (defender == null || !isHoldingTrinket(defender, "crest_of_the_end") || !isCrestOfTheEndEnabled()) {
            return;
        }

        final UUID uuid = defender.getUniqueId();
        final CrestWrath wrath = crestWraths.computeIfAbsent(uuid, k -> new CrestWrath());
        final long now = System.currentTimeMillis();

        wrath.stacks = Math.min(3, wrath.stacks + 1);
        wrath.lastBlockTime = now;

        final double perStack = itemsConfig.getTrinketsConfig().getDouble("crest_of_the_end.damage-increase-per-stack", 0.15D);
        final double mult = perStack * wrath.stacks;
        setModifier(defender, Attribute.ATTACK_DAMAGE, crestAttackKey, mult, AttributeModifier.Operation.ADD_SCALAR);

        defender.getWorld().playSound(defender.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4F, 1.2F + (0.15F * wrath.stacks));
        defender.getWorld().spawnParticle(Particle.DRAGON_BREATH, defender.getLocation().add(0.0D, 1.0D, 0.0D), 6 * wrath.stacks, 0.2D, 0.2D, 0.2D, 0.02D);
    }

    public void onPlayerMeleeAttack(final EntityDamageByEntityEvent event, final Player attacker, final LivingEntity victim) {
        if (attacker == null || victim == null) {
            return;
        }

        final UUID uuid = attacker.getUniqueId();
        final CrestWrath wrath = crestWraths.remove(uuid);
        if (wrath != null && wrath.stacks > 0 && isCrestOfTheEndEnabled()) {
            removeModifier(attacker, Attribute.ATTACK_DAMAGE, crestAttackKey);

            final double bonus = 6.0D * wrath.stacks;
            event.setDamage(event.getDamage() + bonus);

            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0F, 1.2F);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.8F, 1.0F);
            attacker.getWorld().spawnParticle(Particle.DRAGON_BREATH, victim.getLocation().add(0.0D, 1.0D, 0.0D), 18, 0.3D, 0.3D, 0.3D, 0.05D);
        }
    }

    public void onPearlHit(final ProjectileHitEvent event) {
        final Projectile proj = event.getEntity();
        if (proj instanceof EnderPearl && Boolean.TRUE.equals(proj.getPersistentDataContainer().get(prismaticPearlKey, PersistentDataType.BOOLEAN)) && isPrismaticPearlEnabled()) {
            final Location loc = proj.getLocation();
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2F, 1.4F);
            loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 24, 0.4D, 0.4D, 0.4D, 0.1D, null);
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 16, 0.4D, 0.4D, 0.4D, 0.05D, null);
        }
    }

    public void onStarstruckBlock(final Player defender) {
        if (defender == null || !isHoldingTrinket(defender, "starstruck_shield") || !isStarstruckShieldEnabled()) {
            return;
        }

        final UUID uuid = defender.getUniqueId();
        final long now = System.currentTimeMillis();
        final long cooldownUntil = starstruckCooldowns.getOrDefault(uuid, 0L);
        if (now < cooldownUntil) {
            return;
        }
        final long cd = itemsConfig.getTrinketsConfig().getLong("starstruck_shield.cooldown-ms", 10000L);
        starstruckCooldowns.put(uuid, now + cd);

        final Location targetLoc = defender.getLocation();

        final boolean hasRadiant = radiantJewelService != null && radiantJewelService.hasRadiantJewel(defender);
        final int minStars = itemsConfig.getTrinketsConfig().getInt("starstruck_shield.stars-amount-min", 1);
        final int configuredMaxStars = itemsConfig.getTrinketsConfig().getInt("starstruck_shield.stars-amount-max", 3);
        final int maxStars = hasRadiant ? Math.max(5, configuredMaxStars) : configuredMaxStars;
        final int count = ThreadLocalRandom.current().nextInt(minStars, maxStars + 1);
        for (int i = 0; i < count; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!defender.isOnline()) return;

                final Location starTarget = targetLoc.clone().add(
                        ThreadLocalRandom.current().nextDouble(-1.5D, 1.5D),
                        0.0D,
                        ThreadLocalRandom.current().nextDouble(-1.5D, 1.5D)
                );

                final Location skyLoc = starTarget.clone().add(0.0D, 12.0D, 0.0D);
                skyLoc.getWorld().playSound(skyLoc, "stellarity:item.starstruck_shield.spawn_star", org.bukkit.SoundCategory.PLAYERS, 2.2F, 1.0F);

                for (double y = 0; y < 12.0D; y += 1.0D) {
                    final Location particleLoc = skyLoc.clone().subtract(0.0D, y, 0.0D);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> particleLoc.getWorld().spawnParticle(Particle.GLOW, particleLoc, 3), (long) y / 2);
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    starTarget.getWorld().playSound(starTarget, Sound.ENTITY_FIREWORK_ROCKET_BLAST, org.bukkit.SoundCategory.PLAYERS, 1.0F, 1.5F);
                    starTarget.getWorld().spawnParticle(Particle.EXPLOSION, starTarget, 1);
                    starTarget.getWorld().spawnParticle(Particle.END_ROD, starTarget, 25, 0.0D, 0.0D, 0.0D, 0.06D, null);
                    starTarget.getWorld().spawnParticle(Particle.GLOW, starTarget, 12, 0.3D, 0.3D, 0.3D, 0.05D, null);

                    final double radius = itemsConfig.getTrinketsConfig().getDouble("starstruck_shield.star-explosion-radius", 2.5D);
                    final double starDmg = itemsConfig.getTrinketsConfig().getDouble("starstruck_shield.star-damage", 3.0D);
                    for (final Entity entity : starTarget.getWorld().getNearbyEntities(starTarget, radius, radius, radius)) {
                        if (entity instanceof LivingEntity living && living.isValid() && !living.getUniqueId().equals(defender.getUniqueId())) {
                            living.damage(starDmg, defender);
                            if (customStatusEffectService != null) {
                                if (hasRadiant) {
                                    customStatusEffectService.applyHolyFlames(living, 80, defender);
                                } else {
                                    customStatusEffectService.applyPrismaticInferno(living, 80, defender);
                                }
                            }
                        }
                    }
                }, 6L);

            }, i * (hasRadiant ? 2L : 4L));
        }
    }

    public void onBlockBreak(final org.bukkit.event.block.BlockBreakEvent event) {
        final Location loc = event.getBlock().getLocation();
        if (infectedBushes.containsKey(loc)) {
            final AreaEffectCloud cloud = infectedBushes.remove(loc);
            if (cloud != null && cloud.isValid()) {
                cloud.remove();
            }
            event.setDropItems(false);
            final ItemStack duskberry = getDuskberryItem();
            loc.getWorld().dropItemNaturally(loc, duskberry);
        }
    }

    public void onBlockPlace(final org.bukkit.event.block.BlockPlaceEvent event) {
        if (!isDuskberryEnabled()) return;
        final ItemStack item = event.getItemInHand();
        if (isCustomItem(item, "duskberry")) {
            final org.bukkit.block.Block block = event.getBlock();
            if (block.getType() == Material.SWEET_BERRY_BUSH) {
                final Location loc = block.getLocation();
                infectedBushes.put(loc, null);

                final org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) block.getBlockData();
                ageable.setAge(2);
                block.setBlockData(ageable);

                loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 0.6F);
            }
        }
    }

    private boolean isHoldingTrinket(final Player player, final String itemId) {
        return isCustomItem(player.getInventory().getItemInMainHand(), itemId)
                || isCustomItem(player.getInventory().getItemInOffHand(), itemId);
    }

    private boolean hasInInventory(final Player player, final String itemId) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (isCustomItem(item, itemId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCustomItem(final ItemStack item, final String expectedId) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return itemMatcher.isCustom(item, expectedId);
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

    private org.bukkit.inventory.ItemStack getDuskberryItem() {
        return itemRegistry.findByPdcItemId("duskberry")
                .map(itemFactory::create)
                .orElseGet(() -> new org.bukkit.inventory.ItemStack(Material.SWEET_BERRIES));
    }

    private boolean isPlantableBlock(final Material type) {
        return type == Material.GRASS_BLOCK 
            || type == Material.DIRT 
            || type == Material.COARSE_DIRT 
            || type == Material.ROOTED_DIRT 
            || type == Material.FARMLAND 
            || type == Material.MUD 
            || type == Material.MUDDY_MANGROVE_ROOTS 
            || type == Material.PODZOL 
            || type == Material.MOSS_BLOCK;
    }

    private UUID getOrCreateItemUuid(final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        final org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        final String saved = meta.getPersistentDataContainer().get(itemInstanceUuidKey, org.bukkit.persistence.PersistentDataType.STRING);
        if (saved != null) {
            try {
                return UUID.fromString(saved);
            } catch (final IllegalArgumentException ignored) {
                // Fallback to generating a fresh UUID if saved data is invalid
            }
        }
        final UUID newUuid = UUID.randomUUID();
        meta.getPersistentDataContainer().set(itemInstanceUuidKey, org.bukkit.persistence.PersistentDataType.STRING, newUuid.toString());
        item.setItemMeta(meta);
        return newUuid;
    }

    private record PlayerItemKey(UUID playerUuid, UUID itemUuid) {}

    private static final class CrestWrath {
        private int stacks = 0;
        private long lastBlockTime = 0;
    }

    private static final class LifeCrystalState {
        private int charge = 0;
        private Location lastLoc;

        public LifeCrystalState(final Location lastLoc) {
            this.lastLoc = lastLoc;
        }
    }

    private record ActiveAltar(
            UUID ownerUuid,
            Location location,
            long expiresAt,
            Marker marker,
            org.bukkit.entity.ItemDisplay display,
            BukkitTask particleTask
    ) {}
}
