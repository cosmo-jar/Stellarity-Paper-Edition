package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CustomWingsCraftListener implements Listener {

    private static final Set<String> CUSTOM_WINGS = Set.of(
            "phantom_wings",
            "dragon_wings",
            "empress_wings"
    );

    private final ItemStateRepository itemStateRepository;
    private final ItemDefinitionRegistry itemRegistry;
    private final CustomItemFactory customItemFactory;

    public CustomWingsCraftListener(
            final ItemStateRepository itemStateRepository,
            final ItemDefinitionRegistry itemRegistry,
            final CustomItemFactory customItemFactory
    ) {
        this.itemStateRepository = itemStateRepository;
        this.itemRegistry = itemRegistry;
        this.customItemFactory = customItemFactory;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(final PrepareItemCraftEvent event) {
        final InventoryType invType = event.getInventory().getType();
        if (invType != InventoryType.WORKBENCH && invType != InventoryType.CRAFTING) {
            return;
        }

        final ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType() != Material.ELYTRA) {
            return;
        }

        final ItemStack[] matrix = event.getInventory().getMatrix();
        ItemStack wing1 = null;
        ItemStack wing2 = null;
        int count = 0;

        for (final ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                count++;
                if (count == 1) {
                    wing1 = item;
                } else if (count == 2) {
                    wing2 = item;
                } else {
                    break;
                }
            }
        }

        if (count != 2) {
            return;
        }

        if (wing1.getType() != Material.ELYTRA || wing2.getType() != Material.ELYTRA) {
            return;
        }

        final String id1 = itemStateRepository.getItemId(wing1).orElse(null);
        final String id2 = itemStateRepository.getItemId(wing2).orElse(null);

        final boolean isCustom1 = id1 != null && CUSTOM_WINGS.contains(id1);
        final boolean isCustom2 = id2 != null && CUSTOM_WINGS.contains(id2);

        // оба обычные ванильные элитры
        if (!isCustom1 && !isCustom2) {
            return;
        }

        // Одна кастомная, а вторая ванильная. Запрет
        if (!Objects.equals(id1, id2)) {
            event.getInventory().setResult(null);
            return;
        }

        // одинаковые кастомные
        final Optional<CustomItemDefinition> defOpt = itemRegistry.findByPdcItemId(id1);
        if (defOpt.isEmpty()) {
            event.getInventory().setResult(null);
            return;
        }

        final ItemMeta m1 = wing1.getItemMeta();
        final ItemMeta m2 = wing2.getItemMeta();
        if (!(m1 instanceof Damageable d1) || !(m2 instanceof Damageable d2)) {
            event.getInventory().setResult(null);
            return;
        }

        final int damage1 = d1.getDamage();
        final int damage2 = d2.getDamage();

        // если оба предмета полностью целые, починка не выполняется
        if (damage1 == 0 && damage2 == 0) {
            event.getInventory().setResult(null);
            return;
        }

        final int maxDamage = d1.hasMaxDamage() ? d1.getMaxDamage() : wing1.getType().getMaxDurability();
        final int remaining1 = Math.max(0, maxDamage - damage1);
        final int remaining2 = Math.max(0, maxDamage - damage2);
        final int bonus = (int) Math.floor(maxDamage * 0.05); // +5% прочности
        final int newRemaining = Math.min(maxDamage, remaining1 + remaining2 + bonus);
        final int newDamage = Math.max(0, maxDamage - newRemaining);

        final ItemStack repaired = customItemFactory.create(defOpt.get());
        final ItemMeta repMeta = repaired.getItemMeta();
        if (repMeta instanceof Damageable repDam) {
            repDam.setDamage(newDamage);
            repaired.setItemMeta(repDam);
        }

        final Map<Enchantment, Integer> enchants = new HashMap<>(wing1.getEnchantments());
        wing2.getEnchantments().forEach((ench, lvl) -> enchants.merge(ench, lvl, Math::max));
        for (final Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            repaired.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        event.getInventory().setResult(repaired);
    }
}