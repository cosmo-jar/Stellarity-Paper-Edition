package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.data.PlayerStateRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Event-driven unlock-система способностей Harvester.
 */
public final class HarvesterAbilityUnlockService {

    private static final String HARVESTER_ITEM_ID = "harvester";
    private static final int MAX_ABILITIES = 3;
    private static final double MAX_KILL_DISTANCE = 6.5D;

    private final EnchantActiveService activeService;
    private final ItemStateRepository itemStateRepository;
    private final PlayerStateRepository playerStateRepository;
    private final HarvesterAbilityCodec abilityCodec;
    private final EnchantItemService enchantItemService;
    private final StellaritySoundService soundService;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;

    public HarvesterAbilityUnlockService(
            final EnchantActiveService activeService,
            final ItemStateRepository itemStateRepository,
            final PlayerStateRepository playerStateRepository,
            final HarvesterAbilityCodec abilityCodec,
            final EnchantItemService enchantItemService,
            final StellaritySoundService soundService,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.activeService = activeService;
        this.itemStateRepository = itemStateRepository;
        this.playerStateRepository = playerStateRepository;
        this.abilityCodec = abilityCodec;
        this.enchantItemService = enchantItemService;
        this.soundService = soundService;
        this.advancementService = advancementService;
    }

    public void onEntityDeath(final EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.isOnline()) {
            return;
        }
        if (event.getEntity().getLocation().distance(killer.getLocation()) > MAX_KILL_DISTANCE) {
            return;
        }

        final ItemStack item = killer.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (!HARVESTER_ITEM_ID.equalsIgnoreCase(itemStateRepository.getItemId(item).orElse(""))) {
            return;
        }
        if (activeService.highestLevel(activeService.fromItem(item, "mainhand"), EnchantIds.TECHNICAL_SOUL_HARVEST) <= 0) {
            return;
        }

        final Optional<HarvesterAbility> matchedAbility = HarvesterAbility.fromEntityType(event.getEntityType());
        if (matchedAbility.isEmpty()) {
            return;
        }
        final HarvesterAbility ability = matchedAbility.get();

        final List<String> unlocked = new ArrayList<>(abilityCodec.decodeAbilities(itemStateRepository.getHarvesterAbilities(item).orElse("")));
        if (unlocked.size() >= MAX_ABILITIES || unlocked.contains(ability.id())) {
            return;
        }

        final Map<String, Integer> progress = new java.util.HashMap<>(
                abilityCodec.decodeProgress(playerStateRepository.getHarvesterAbilityProgress(killer).orElse(""))
        );
        final int nextProgress = progress.getOrDefault(ability.id(), 0) + 1;
        progress.put(ability.id(), nextProgress);

        killer.sendActionBar(Component.translatable("item.stellarity.harvester.ability.hint"));

        if (nextProgress < ability.requiredProgress()) {
            playerStateRepository.setHarvesterAbilityProgress(killer, abilityCodec.encodeProgress(progress));
            return;
        }

        unlocked.add(ability.id());
        itemStateRepository.setHarvesterAbilities(item, abilityCodec.encodeAbilities(unlocked));
        progress.remove(ability.id());
        playerStateRepository.setHarvesterAbilityProgress(killer, abilityCodec.encodeProgress(progress));

        enchantItemService.rebuildNow(item);
        soundService.play(killer, "stellarity:item.harvester.unlock_ability", 1.0F, 1.0F, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME);

        if (advancementService != null) {
            advancementService.grant(killer, "stellarity:exploration/harvester/abilities/" + ability.id());
            advancementService.grantCriteria(killer, "stellarity:exploration/harvester/unlock_all_abilities", ability.id());
            if (unlocked.size() >= MAX_ABILITIES) {
                advancementService.grant(killer, "stellarity:exploration/harvester/max_out");
            }
        }
    }
}
