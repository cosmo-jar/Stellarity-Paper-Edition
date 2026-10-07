package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CustomEnchantAnvilListener implements Listener {

    private final EnchantDefinitionRegistry enchantRegistry;
    private final EnchantBehaviorRegistry behaviorRegistry;
    private final ItemStateRepository itemStateRepository;
    private final EnchantDataCodec codec;
    private final EnchantItemService enchantItemService;

    public CustomEnchantAnvilListener(
            final EnchantDefinitionRegistry enchantRegistry,
            final EnchantBehaviorRegistry behaviorRegistry,
            final ItemStateRepository itemStateRepository,
            final EnchantDataCodec codec,
            final EnchantItemService enchantItemService
    ) {
        this.enchantRegistry = enchantRegistry;
        this.behaviorRegistry = behaviorRegistry;
        this.itemStateRepository = itemStateRepository;
        this.codec = codec;
        this.enchantItemService = enchantItemService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(final PrepareAnvilEvent event) {
        final AnvilInventory inventory = event.getInventory();
        final ItemStack left = inventory.getItem(0);
        final ItemStack right = inventory.getItem(1);

        if (left == null || left.getType().isAir() || right == null || right.getType().isAir()) {
            return;
        }

        if (isHarvester(left) && hasForbiddenHarvesterEnchant(right)) {
            event.setResult(null);
            return;
        }

        final List<EnchantInstance> rightEnchants = decodeEnchants(right);
        if (rightEnchants.isEmpty()) {
            return;
        }

        for (final EnchantInstance rightInst : rightEnchants) {
            if (!behaviorRegistry.canApplyToItem(rightInst.id(), left)) {
                event.setResult(null);
                return;
            }
        }

        final List<EnchantInstance> leftEnchants = decodeEnchants(left);
        final Map<String, Integer> mergedMap = new LinkedHashMap<>();
        for (final EnchantInstance leftInst : leftEnchants) {
            mergedMap.put(leftInst.id(), leftInst.level());
        }

        boolean changed = false;
        int levelCost = 2;

        for (final EnchantInstance rightInst : rightEnchants) {
            final String enchantId = rightInst.id();
            final int rightLevel = rightInst.level();
            final Optional<EnchantDefinition> defOpt = enchantRegistry.findById(enchantId);
            final int maxTier = defOpt.map(d -> d.tiers().size()).orElse(1);

            if (mergedMap.containsKey(enchantId)) {
                final int leftLevel = mergedMap.get(enchantId);
                final int newLevel;
                if (leftLevel == rightLevel) {
                    newLevel = Math.min(maxTier, leftLevel + 1);
                } else {
                    newLevel = Math.min(maxTier, Math.max(leftLevel, rightLevel));
                }
                if (newLevel != leftLevel) {
                    mergedMap.put(enchantId, newLevel);
                    changed = true;
                }
                levelCost += newLevel * 2;
            } else {
                mergedMap.put(enchantId, Math.min(maxTier, rightLevel));
                changed = true;
                levelCost += rightLevel * 2;
            }
        }

        if (!changed && !leftEnchants.isEmpty()) {
            return;
        }

        final ItemStack result = left.clone();
        final List<EnchantInstance> finalEnchants = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : mergedMap.entrySet()) {
            finalEnchants.add(new EnchantInstance(entry.getKey(), entry.getValue()));
        }

        itemStateRepository.setEnchantsData(result, codec.encode(finalEnchants));
        enchantItemService.rebuildNow(result);

        event.getView().setRepairCost(levelCost);
        event.setResult(result);
    }

    private List<EnchantInstance> decodeEnchants(final ItemStack item) {
        final Optional<String> data = itemStateRepository.getEnchantsData(item);
        if (data.isEmpty() || data.get().isBlank()) {
            return List.of();
        }
        return codec.decode(data.get());
    }

    private boolean isHarvester(final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return "harvester".equalsIgnoreCase(itemStateRepository.getItemId(item).orElse(""));
    }

    private boolean hasForbiddenHarvesterEnchant(final ItemStack right) {
        if (right == null || right.getType().isAir()) {
            return false;
        }
        final List<EnchantInstance> customList = decodeEnchants(right);
        for (final EnchantInstance ci : customList) {
            if ("critical_strike".equalsIgnoreCase(ci.id())) {
                return true;
            }
        }
        Map<org.bukkit.enchantments.Enchantment, Integer> enchants = right.getEnchantments();
        if (right.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta) {
            enchants = storageMeta.getStoredEnchants();
        }
        for (final org.bukkit.enchantments.Enchantment ench : enchants.keySet()) {
            final String key = ench.getKey().getKey().toLowerCase(java.util.Locale.ROOT);
            if (key.equals("sharpness") || key.equals("smite") || key.equals("bane_of_arthropods")
                    || key.equals("fire_aspect") || key.equals("flame")) {
                return true;
            }
        }
        return false;
    }
}
