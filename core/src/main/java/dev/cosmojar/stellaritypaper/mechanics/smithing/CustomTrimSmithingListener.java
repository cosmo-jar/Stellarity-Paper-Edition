package dev.cosmojar.stellaritypaper.mechanics.smithing;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.enchants.EnchantItemService;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Map;
import java.util.Optional;
public final class CustomTrimSmithingListener implements Listener {

    private static final NamespacedKey CHORUS_KEY = new NamespacedKey("stellarity", "chorus");
    private static final NamespacedKey ENDERITE_KEY = new NamespacedKey("stellarity", "enderite");

    private static final Map<Material, TrimPattern> TEMPLATE_TO_PATTERN = Map.ofEntries(
            Map.entry(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.SENTRY),
            Map.entry(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.DUNE),
            Map.entry(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.COAST),
            Map.entry(Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.WILD),
            Map.entry(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.WARD),
            Map.entry(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.EYE),
            Map.entry(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.VEX),
            Map.entry(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.TIDE),
            Map.entry(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.SNOUT),
            Map.entry(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.RIB),
            Map.entry(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.SPIRE),
            Map.entry(Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.WAYFINDER),
            Map.entry(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.SHAPER),
            Map.entry(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.SILENCE),
            Map.entry(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.RAISER),
            Map.entry(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.HOST),
            Map.entry(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.FLOW),
            Map.entry(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPattern.BOLT)
    );

    private final ItemStateRepository itemStateRepository;
    private final EnchantItemService enchantItemService;

    public CustomTrimSmithingListener(
            final ItemStateRepository itemStateRepository,
            final EnchantItemService enchantItemService
    ) {
        this.itemStateRepository = itemStateRepository;
        this.enchantItemService = enchantItemService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(final PrepareSmithingEvent event) {
        final SmithingInventory inventory = event.getInventory();
        final ItemStack template = inventory.getItem(0);
        final ItemStack base = inventory.getItem(1);
        final ItemStack addition = inventory.getItem(2);

        if (addition == null || addition.getType().isAir()
                || base == null || base.getType().isAir()
                || template == null || template.getType().isAir()) {
            return;
        }

        if (itemStateRepository.getItemId(template).isPresent()) {
            event.setResult(null);
            return;
        }

        final Optional<String> customAdditionIdOpt = itemStateRepository.getItemId(addition);

        // В слоте добавки обычный ванильный предмет без PDC
        if (customAdditionIdOpt.isEmpty()) {
            final ItemStack currentResult = event.getResult();
            if (currentResult != null && !currentResult.getType().isAir()) {
                enchantItemService.ensureUpToDate(currentResult);
            }
            return;
        }

        final String customAdditionId = customAdditionIdOpt.get();

        // В слоте добавки кастомный предмет
        final NamespacedKey targetTrimKey;
        if ("chorus_plating".equals(customAdditionId)) {
            targetTrimKey = CHORUS_KEY;
        } else if ("enderite_shard".equals(customAdditionId)) {
            targetTrimKey = ENDERITE_KEY;
        } else {
            // Любой другой кастомный предмет плагина
            // блок чтоб стол случайно не съел кастомный предмет как материал
            event.setResult(null);
            return;
        }

        if (!(base.getItemMeta() instanceof ArmorMeta)) {
            event.setResult(null);
            return;
        }

        final TrimMaterial trimMaterial = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.TRIM_MATERIAL)
                .get(targetTrimKey);
        if (trimMaterial == null) {
            return;
        }

        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) {
            result = base.clone();
            result.setAmount(1);
        } else {
            result = result.clone();
        }

        final ItemMeta meta = result.getItemMeta();
        if (!(meta instanceof ArmorMeta armorMeta)) {
            return;
        }

        TrimPattern pattern = null;
        final ArmorTrim existingTrim = armorMeta.getTrim();
        if (existingTrim != null) {
            pattern = existingTrim.getPattern();
        }
        if (pattern == null) {
            pattern = TEMPLATE_TO_PATTERN.get(template.getType());
        }
        if (pattern == null) {
            event.setResult(null);
            return;
        }

        armorMeta.setTrim(new ArmorTrim(trimMaterial, pattern));
        result.setItemMeta(armorMeta);
        enchantItemService.ensureUpToDate(result);

        event.setResult(result);
    }
}
