package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public final class CustomFoodListener implements Listener {

    private final StellarityPaperPlugin plugin;
    private final CustomItemMatcher itemMatcher;
    private final MessageService messageService;
    private final ItemLoreBuilder loreBuilder;

    private final NamespacedKey stepKey;
    private final NamespacedKey finalKey;

    public CustomFoodListener(
            final StellarityPaperPlugin plugin,
            final CustomItemMatcher itemMatcher,
            final MessageService messageService,
            final ItemLoreBuilder loreBuilder
    ) {
        this.plugin = plugin;
        this.itemMatcher = itemMatcher;
        this.messageService = messageService;
        this.loreBuilder = loreBuilder;
        this.stepKey = new NamespacedKey("stellarity", "loaf_step");
        this.finalKey = new NamespacedKey("stellarity", "loaf_of_plenty_final");
    }

    public CustomFoodListener(
            final StellarityPaperPlugin plugin,
            final CustomItemMatcher itemMatcher,
            final MessageService messageService
    ) {
        this(plugin, itemMatcher, messageService, null);
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
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                
                final org.bukkit.Color fromColor = org.bukkit.Color.fromRGB(255, 130, 224);
                final org.bukkit.Color toColor = org.bukkit.Color.fromRGB(255, 0, 0);
                final org.bukkit.Particle.DustTransition transition = new org.bukkit.Particle.DustTransition(fromColor, toColor, 1.0F);
                player.getWorld().spawnParticle(org.bukkit.Particle.DUST_COLOR_TRANSITION, player.getLocation().add(0, 1, 0), 30, 0.3D, 0.55D, 0.3D, 0.0D, transition);
                player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, player.getLocation().add(0, 1, 0), 20, 0.3D, 0.55D, 0.3D, 0.0D);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0F, 1.4F);
                break;
        }
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
