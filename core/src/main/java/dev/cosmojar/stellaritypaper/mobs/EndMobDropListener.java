package dev.cosmojar.stellaritypaper.mobs;

import dev.cosmojar.stellaritypaper.items.CustomItemDefinition;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Frog;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class EndMobDropListener implements Listener {

    private final ItemDefinitionRegistry itemRegistry;
    private final CustomItemFactory itemFactory;

    public EndMobDropListener(
            final ItemDefinitionRegistry itemRegistry,
            final CustomItemFactory itemFactory
    ) {
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            handlePlayerDeath(event, player);
            return;
        }
        if (event.getEntity() instanceof Enderman enderman) {
            handleEndermanDeath(event, enderman);
        } else if (event.getEntity() instanceof Shulker shulker) {
            handleShulkerDeath(event, shulker);
        } else if (event.getEntity() instanceof MagmaCube magmaCube) {
            handleMagmaCubeDeath(event, magmaCube);
        }
    }

    private void handlePlayerDeath(final EntityDeathEvent event, final Player player) {
        final String name = player.getName();
        if ("BananaKingXO".equalsIgnoreCase(name)) {
            itemRegistry.findByPdcItemId("potassifish").ifPresent(def ->
                    event.getDrops().add(itemFactory.create(def))
            );
        } else if ("kohara_".equalsIgnoreCase(name)) {
            itemRegistry.findByPdcItemId("golden_chorus_fruit").ifPresent(def ->
                    event.getDrops().add(itemFactory.create(def))
            );
        }
    }

    private void handleEndermanDeath(final EntityDeathEvent event, final Enderman enderman) {
        final Player killer = enderman.getKiller();
        int lootingLevel = 0;
        if (killer != null) {
            final ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (weapon.hasItemMeta()) {
                lootingLevel = weapon.getEnchantmentLevel(Enchantment.LOOTING);
            }
        }

        final World world = enderman.getWorld();
        final boolean isTheEnd = world.getEnvironment() == World.Environment.THE_END;

        // 1. Рука эндермена (Enderman's Hand)
        final double handChance;
        if (isTheEnd) {
            // В Энде: 2% базовый + 1% за каждый уровень добычи выше I
            handChance = 0.02 + (Math.max(0, lootingLevel - 1) * 0.01);
        } else {
            // В Обычном мире / Незере: 10% базовый + бонус от добычи
            handChance = lootingLevel == 0 ? 0.10 : (0.10 + (Math.max(0, lootingLevel - 1) * 0.015));
        }

        if (ThreadLocalRandom.current().nextDouble() < handChance) {
            final Optional<CustomItemDefinition> handOpt = itemRegistry.findByCategoryAndName("trinkets", "enderman_hand");
            handOpt.ifPresent(def -> event.getDrops().add(itemFactory.create(def)));
        }

        // 2. Плоть эндермена (Enderman Flesh)
        // 0-2 штуки + до (1 * lootingLevel)
        int fleshCount = ThreadLocalRandom.current().nextInt(0, 3);
        if (lootingLevel > 0) {
            fleshCount += ThreadLocalRandom.current().nextInt(0, lootingLevel + 1);
        }

        if (fleshCount > 0) {
            final Optional<CustomItemDefinition> fleshOpt = itemRegistry.findByCategoryAndName("food", "enderman_flesh");
            if (fleshOpt.isPresent()) {
                final ItemStack flesh = itemFactory.create(fleshOpt.get());
                flesh.setAmount(fleshCount);
                event.getDrops().add(flesh);
            }
        }
    }

    private void handleShulkerDeath(final EntityDeathEvent event, final Shulker shulker) {
        // Игнорируем спец-части боссов (например Shulking)
        if (shulker.getScoreboardTags().contains("stellarity.shulking.body")
            || shulker.getScoreboardTags().contains("stellarity.shulking.rod_shulker")) {
            return;
        }

        final Player killer = shulker.getKiller();
        int lootingLevel = 0;
        if (killer != null) {
            final ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (weapon.hasItemMeta()) {
                lootingLevel = weapon.getEnchantmentLevel(Enchantment.LOOTING);
            }
        }

        // Тело шалкера (Shulker Body): 75% без добычи, 80% + 5%/lvl с добычей
        final double bodyChance = lootingLevel == 0 ? 0.75 : (0.80 + (Math.max(0, lootingLevel - 1) * 0.05));
        if (ThreadLocalRandom.current().nextDouble() < bodyChance) {
            final Optional<CustomItemDefinition> bodyOpt = itemRegistry.findByCategoryAndName("food", "shulker_body");
            bodyOpt.ifPresent(def -> event.getDrops().add(itemFactory.create(def)));
        }
    }

    private void handleMagmaCubeDeath(final EntityDeathEvent event, final MagmaCube magmaCube) {
        if (event.getDamageSource().getCausingEntity() instanceof Frog frog) {
            // Если лягушка съела магма-куб в Энде -> Пепельный квакосвет (Ashen Froglight)
            if (EndIslandManager.isTheEnd(magmaCube.getWorld()) || frog.getScoreboardTags().contains("stellarity.frog.end")) {
                final Optional<CustomItemDefinition> lightOpt = itemRegistry.findByCategoryAndName("blocks", "ashen_froglight");
                lightOpt.ifPresent(def -> event.getDrops().add(itemFactory.create(def)));
            }
        }
    }
}