package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.data.PlayerStateRepository;
import dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class CustomFoodListener implements Listener {

    public static final int MAX_HEARTFISH_CONSUMED = 5;
    public static final double HEALTH_PER_HEARTFISH = 2.0D;
    public static final NamespacedKey HEARTFISH_MODIFIER_KEY = new NamespacedKey("stellarity", "crystal_heartfish.max_hp");
    public static final NamespacedKey HEARTFISH_MODIFIER_FALLBACK = new NamespacedKey("stellarity", "crystal_heartfish_max_hp");

    private final StellarityPaperPlugin plugin;
    private final CustomItemMatcher itemMatcher;
    private final MessageService messageService;
    private final ItemLoreBuilder loreBuilder;
    private final PlayerStateRepository playerStateRepository;
    private final AdvancementService advancementService;
    private final ItemsConfigService itemsConfigService;

    private final NamespacedKey stepKey;
    private final NamespacedKey finalKey;
    private final NamespacedKey heartfishPdcKey;

    public CustomFoodListener(
            final StellarityPaperPlugin plugin,
            final CustomItemMatcher itemMatcher,
            final MessageService messageService,
            final ItemLoreBuilder loreBuilder,
            final PlayerStateRepository playerStateRepository,
            final AdvancementService advancementService,
            final ItemsConfigService itemsConfigService
    ) {
        this.plugin = plugin;
        this.itemMatcher = itemMatcher;
        this.messageService = messageService;
        this.loreBuilder = loreBuilder;
        this.playerStateRepository = playerStateRepository;
        this.advancementService = advancementService;
        this.itemsConfigService = itemsConfigService;
        this.stepKey = new NamespacedKey("stellarity", "loaf_step");
        this.finalKey = new NamespacedKey("stellarity", "loaf_of_plenty_final");
        this.heartfishPdcKey = new NamespacedKey("stellarity", "crystal_heartfish_consumed");
        if (this.itemsConfigService != null) {
            this.itemsConfigService.addReloadListener(this::processResetList);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(final PlayerItemConsumeEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();
        final String customId = itemMatcher.resolveRawId(item).orElse("");

        if (customId.isEmpty()) {
            return;
        }

        if ("loaf_of_plenty".equalsIgnoreCase(customId)) {
            handleLoafOfPlenty(event, player, item);
            return;
        }

        applyFoodEffects(player, customId);
    }

    private void handleLoafOfPlenty(final PlayerItemConsumeEvent event, final Player player, final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        final boolean isFinal = meta.getPersistentDataContainer().has(finalKey, PersistentDataType.BOOLEAN);

        if (isFinal) {
            return;
        }

        final int currentStep = meta.getPersistentDataContainer().getOrDefault(stepKey, PersistentDataType.INTEGER, 1);
        final int nextStep = currentStep + 1;

        final ItemStack replacement = item.clone();
        final ItemMeta repMeta = replacement.getItemMeta();
        if (repMeta == null) {
            return;
        }

        if (nextStep >= 15) {
            repMeta.getPersistentDataContainer().set(finalKey, PersistentDataType.BOOLEAN, true);
            if (loreBuilder != null) {
                repMeta.lore(loreBuilder.buildLoafOfPlentyLore(15));
            }

            final org.bukkit.inventory.meta.components.FoodComponent food = repMeta.getFood();
            food.setNutrition(1);
            food.setSaturation(1.0f);
            food.setCanAlwaysEat(false);
            repMeta.setFood(food);
        } else {
            repMeta.getPersistentDataContainer().set(stepKey, PersistentDataType.INTEGER, nextStep);
            if (loreBuilder != null) {
                repMeta.lore(loreBuilder.buildLoafOfPlentyLore(nextStep));
            }
        }

        replacement.setItemMeta(repMeta);
        event.setReplacement(replacement);

        // safety for Creative mode or client inventory desync
        final org.bukkit.inventory.EquipmentSlot hand = event.getHand();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            final ItemStack held = player.getInventory().getItem(hand);
            if (held != null && !held.getType().isAir()
                    && itemMatcher.resolveRawId(held).map("loaf_of_plenty"::equalsIgnoreCase).orElse(false)) {
                final ItemMeta heldMeta = held.getItemMeta();
                if (heldMeta != null) {
                    final boolean heldIsFinal = heldMeta.getPersistentDataContainer().has(finalKey, PersistentDataType.BOOLEAN);
                    if (nextStep >= 15 && !heldIsFinal) {
                        player.getInventory().setItem(hand, replacement);
                    } else if (nextStep < 15) {
                        final int heldStep = heldMeta.getPersistentDataContainer().getOrDefault(stepKey, PersistentDataType.INTEGER, 1);
                        if (heldStep < nextStep) {
                            player.getInventory().setItem(hand, replacement);
                        }
                    }
                }
            }
        });
    }

    private void applyFoodEffects(final Player player, final String customId) {
        switch (customId.toLowerCase()) {
            case "bubblefish":
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.removePotionEffect(PotionEffectType.POISON);
                    player.removePotionEffect(PotionEffectType.NAUSEA);
                    player.removePotionEffect(PotionEffectType.HUNGER);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 7600, 0));
                });
                break;
            case "candied_chorus_fruit":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5600, 0));
                teleportRandomly(player, 16.0);
                break;
            case "crimson_tigerfish":
            case "fleshy_piranha":
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 12000, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 8000, 0));
                break;
            case "enderman_flesh":
                if (ThreadLocalRandom.current().nextDouble() < 0.8) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 16800, 0));
                }
                teleportRandomly(player, 8.0);
                break;
            case "flarefin_koi":
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6400, 0));
                break;
            case "fried_chorus_fruit":
                teleportRandomly(player, 16.0);
                break;
            case "frost_minnow":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 8000, 0));
                break;
            case "golden_chorus_fruit":
                teleportRandomly(player, 300.0);
                break;
            case "overgrown_cod":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1600, 3));
                break;
            case "pho":
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 12800, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60000, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60000, 0));
                break;
            case "prismatic_sushi":
            case "sushi":
                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 16000, 0));
                break;
            case "prismite":
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 2000, 1));
                break;
            case "shepherds_pie":
                player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 25600, 1));
                break;
            case "shulker_body":
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 16000, 0));
                }
                teleportRandomly(player, 16.0);
                break;
            case "royal_jelly":
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1200, 0));
                break;
            case "royal_jelly_ii":
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 1));
                break;
            case "amethyst_budfish":
                player.damage(1.0);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0F, 1.0F);
                break;
            case "crystal_heartfish":
                handleCrystalHeartfish(player);
                break;
        }
    }

    private boolean isHeartfishEnabled() {
        if (itemsConfigService == null || itemsConfigService.getFoodConfig() == null) {
            return true;
        }
        return itemsConfigService.getFoodConfig().getBoolean("crystal_heartfish.enabled", true);
    }

    private int getMaxHeartfishConsumed() {
        if (itemsConfigService == null || itemsConfigService.getFoodConfig() == null) {
            return MAX_HEARTFISH_CONSUMED;
        }
        return itemsConfigService.getFoodConfig().getInt("crystal_heartfish.max-consumed", MAX_HEARTFISH_CONSUMED);
    }

    private double getHealthPerHeartfish() {
        if (itemsConfigService == null || itemsConfigService.getFoodConfig() == null) {
            return HEALTH_PER_HEARTFISH;
        }
        return itemsConfigService.getFoodConfig().getDouble("crystal_heartfish.health-per-fish", HEALTH_PER_HEARTFISH);
    }

    public void syncHeartfishHealth(final Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        final int count = (playerStateRepository != null)
                ? playerStateRepository.getCrystalHeartfishConsumed(player)
                : player.getPersistentDataContainer().getOrDefault(heartfishPdcKey, PersistentDataType.INTEGER, 0);

        final boolean enabled = isHeartfishEnabled();

        // O(1)
        if (!enabled || count <= 0) {
            removeHeartfishModifierIfPresent(player);
            return;
        }

        applyHeartfishHealthModifier(player, count);
    }

    private void removeHeartfishModifierIfPresent(final Player player) {
        final AttributeInstance maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr == null) {
            return;
        }
        for (final AttributeModifier modifier : maxHpAttr.getModifiers()) {
            if (HEARTFISH_MODIFIER_KEY.equals(modifier.getKey()) || HEARTFISH_MODIFIER_FALLBACK.equals(modifier.getKey())) {
                maxHpAttr.removeModifier(modifier);
                if (player.getHealth() > maxHpAttr.getValue()) {
                    player.setHealth(Math.max(1.0D, maxHpAttr.getValue()));
                }
                break;
            }
        }
    }

    public void applyHeartfishHealthModifier(final Player player, final int count) {
        if (player == null) {
            return;
        }
        final AttributeInstance maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHpAttr == null) {
            return;
        }

        for (final AttributeModifier modifier : List.copyOf(maxHpAttr.getModifiers())) {
            if (HEARTFISH_MODIFIER_KEY.equals(modifier.getKey()) || HEARTFISH_MODIFIER_FALLBACK.equals(modifier.getKey())) {
                maxHpAttr.removeModifier(modifier);
            }
        }

        final int maxConsumed = getMaxHeartfishConsumed();
        final double healthPerFish = getHealthPerHeartfish();
        final int clamped = Math.min(maxConsumed, Math.max(0, count));
        if (clamped > 0) {
            final double bonusHp = clamped * healthPerFish;
            final AttributeModifier modifier = new AttributeModifier(
                    HEARTFISH_MODIFIER_KEY,
                    bonusHp,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            maxHpAttr.addModifier(modifier);
        }

        if (player.getHealth() > maxHpAttr.getValue()) {
            player.setHealth(Math.max(1.0D, maxHpAttr.getValue()));
        }
    }

    private void handleCrystalHeartfish(final Player player) {
        if (!isHeartfishEnabled()) {
            return;
        }

        final int maxConsumed = getMaxHeartfishConsumed();
        final double healthPerFish = getHealthPerHeartfish();

        final int currentCount = (playerStateRepository != null)
                ? playerStateRepository.getCrystalHeartfishConsumed(player)
                : player.getPersistentDataContainer().getOrDefault(heartfishPdcKey, PersistentDataType.INTEGER, 0);

        if (currentCount < maxConsumed) {
            final int newCount = currentCount + 1;
            if (playerStateRepository != null) {
                playerStateRepository.setCrystalHeartfishConsumed(player, newCount);
            } else {
                player.getPersistentDataContainer().set(heartfishPdcKey, PersistentDataType.INTEGER, newCount);
            }

            applyHeartfishHealthModifier(player, newCount);

            final AttributeInstance maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
            final double maxHp = (maxHpAttr != null) ? maxHpAttr.getValue() : 20.0D;
            player.setHealth(Math.min(maxHp, player.getHealth() + healthPerFish));

            spawnHeartfishEffects(player);

            if (newCount >= maxConsumed && advancementService != null) {
                advancementService.grant(player, "stellarity:void_fishing/topped_off");
            }
        }
    }

    private void spawnHeartfishEffects(final Player player) {
        final org.bukkit.Color fromColor = org.bukkit.Color.fromRGB(255, 130, 224);
        final org.bukkit.Color toColor = org.bukkit.Color.fromRGB(255, 0, 0);
        final org.bukkit.Particle.DustTransition transition = new org.bukkit.Particle.DustTransition(fromColor, toColor, 1.0F);
        player.getWorld().spawnParticle(org.bukkit.Particle.DUST_COLOR_TRANSITION, player.getLocation().add(0, 1, 0), 30, 0.3D, 0.55D, 0.3D, 0.0D, transition);
        player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, player.getLocation().add(0, 1, 0), 20, 0.3D, 0.55D, 0.3D, 0.0D);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0F, 1.4F);
    }

    public Set<String> getCleanResetPlayerNames() {
        if (itemsConfigService == null || itemsConfigService.getFoodConfig() == null) {
            return Collections.emptySet();
        }
        final YamlConfiguration config = itemsConfigService.getFoodConfig();
        final List<String> raw = config.getStringList("crystal_heartfish.reset-players");
        if (raw.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<String> names = new HashSet<>();
        for (final String s : raw) {
            if (s != null) {
                final String trimmed = s.trim().toLowerCase(Locale.ROOT);
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
        }
        return names;
    }

    public int processResetList() {
        final Set<String> names = getCleanResetPlayerNames();
        if (names.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (final Player online : plugin.getServer().getOnlinePlayers()) {
            if (names.contains(online.getName().toLowerCase(Locale.ROOT))) {
                resetPlayerHeartfish(online);
                count++;
            }
        }
        return count;
    }

    public void resetPlayerHeartfish(final Player player) {
        if (player == null) {
            return;
        }
        if (playerStateRepository != null) {
            playerStateRepository.setCrystalHeartfishConsumed(player, 0);
        } else {
            player.getPersistentDataContainer().set(heartfishPdcKey, PersistentDataType.INTEGER, 0);
        }
        removeHeartfishModifierIfPresent(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final Set<String> resetList = getCleanResetPlayerNames();
        if (!resetList.isEmpty() && resetList.contains(player.getName().toLowerCase(Locale.ROOT))) {
            resetPlayerHeartfish(player);
            return;
        }

        final int count = (playerStateRepository != null)
                ? playerStateRepository.getCrystalHeartfishConsumed(player)
                : player.getPersistentDataContainer().getOrDefault(heartfishPdcKey, PersistentDataType.INTEGER, 0);
        // O(1)
        if (count <= 0 && isHeartfishEnabled()) {
            return;
        }
        syncHeartfishHealth(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final int count = (playerStateRepository != null)
                ? playerStateRepository.getCrystalHeartfishConsumed(player)
                : player.getPersistentDataContainer().getOrDefault(heartfishPdcKey, PersistentDataType.INTEGER, 0);
        // O(1)
        if (count <= 0 || !isHeartfishEnabled()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                syncHeartfishHealth(player);
            }
        });
    }

    private void teleportRandomly(final Player player, final double diameter) {
        final org.bukkit.World world = player.getWorld();
        final org.bukkit.Location loc = player.getLocation();
        final double radius = diameter / 2.0;
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 16; i++) {
            final double dx = (random.nextDouble() * 2.0 - 1.0) * radius;
            final double dz = (random.nextDouble() * 2.0 - 1.0) * radius;
            final org.bukkit.Location target = loc.clone().add(dx, 0, dz);
            target.setY(world.getHighestBlockYAt(target) + 1);
            if (target.getBlock().getType().isAir() && target.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                player.teleport(target);
                world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                break;
            }
        }
    }
}
