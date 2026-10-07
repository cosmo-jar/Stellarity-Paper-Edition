package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Формирует динамический лор-блок способностей Harvester.
 */
public final class HarvesterLoreService {

    private static final String HARVESTER_ITEM_ID = "harvester";

    private final ItemStateRepository itemStateRepository;
    private final HarvesterAbilityCodec abilityCodec;

    public HarvesterLoreService(
            final ItemStateRepository itemStateRepository,
            final HarvesterAbilityCodec abilityCodec
    ) {
        this.itemStateRepository = itemStateRepository;
        this.abilityCodec = abilityCodec;
    }

    public List<Component> buildLore(final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return List.of();
        }
        final String itemId = itemStateRepository.getItemId(item).orElse("");
        if (!HARVESTER_ITEM_ID.equalsIgnoreCase(itemId)) {
            return List.of();
        }

        final List<String> unlocked = abilityCodec.decodeAbilities(itemStateRepository.getHarvesterAbilities(item).orElse(""));
        if (unlocked.isEmpty()) {
            return List.of();
        }
        final Set<String> unlockedSet = Set.copyOf(unlocked);

        final List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (final HarvesterAbility ability : HarvesterAbility.LORE_DISPLAY_ORDER) {
            if (!unlockedSet.contains(ability.id())) {
                continue;
            }
            lore.add(
                    Component.translatable(
                            "item.stellarity.harvester.ability.tooltip",
                            Component.translatable(ability.translateKey())
                                    .color(TextColor.fromHexString("#4BC6FF"))
                                    .decoration(TextDecoration.ITALIC, false),
                            Component.translatable("item.stellarity.harvester.ability.known")
                                    .color(TextColor.fromHexString("#F466CC"))
                                    .decorate(TextDecoration.BOLD)
                                    .decoration(TextDecoration.ITALIC, false)
                    ).color(TextColor.fromHexString("#EEEEEE"))
                            .decoration(TextDecoration.ITALIC, false)
            );
        }
        lore.add(Component.empty());
        return lore;
    }
}

