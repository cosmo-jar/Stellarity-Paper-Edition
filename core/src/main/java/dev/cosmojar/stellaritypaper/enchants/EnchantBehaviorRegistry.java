package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Реестр поведенческих зачарований с проверкой совместимости предмета.
 */
public final class EnchantBehaviorRegistry {

    private static final Map<String, Set<Material>> ALLOWED_MATERIALS = Map.ofEntries(
            Map.entry(EnchantIds.CRITICAL_STRIKE, Set.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE)),
            Map.entry(EnchantIds.VOID_STRIKE, Set.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE)),
            Map.entry(EnchantIds.AMBUSH, Set.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE)),
            Map.entry(EnchantIds.LEVITATION_SHOT, Set.of(Material.BOW, Material.CROSSBOW)),
            Map.entry(EnchantIds.VOID_SHOT, Set.of(Material.BOW, Material.CROSSBOW)),
            Map.entry(EnchantIds.TECHNICAL_INFERNAL_INFUSION, Set.of(Material.NETHERITE_SWORD, Material.CROSSBOW)),
            Map.entry(EnchantIds.TECHNICAL_MIGHTY_WIND, Set.of(Material.TRIDENT)),
            Map.entry(EnchantIds.TECHNICAL_DAYBROKEN, Set.of(Material.NETHERITE_AXE)),
            Map.entry(EnchantIds.TECHNICAL_DRACONIC, Set.of(Material.NETHERITE_SWORD)),
            Map.entry(EnchantIds.TECHNICAL_SOUL_HARVEST, Set.of(Material.IRON_SWORD)),
            Map.entry(EnchantIds.DUNE_SPEED, Set.of(Material.NETHERITE_BOOTS, Material.DIAMOND_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.CHAINMAIL_BOOTS, Material.LEATHER_BOOTS)),
            Map.entry(EnchantIds.PLATED, Set.of(Material.ELYTRA)),
            Map.entry(EnchantIds.SOARING, Set.of(Material.ELYTRA)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_AMETHYST, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_COPPER, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_DIAMOND, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_EMERALD, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_GOLD, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_IRON, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_LAPIS, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_NETHERITE, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES)),
            Map.entry(EnchantIds.TECHNICAL_VOID_PENDANT_QUARTZ, Set.of(Material.IRON_NUGGET, Material.SWEET_BERRIES))
    );

    public Optional<Set<Material>> allowedMaterials(final String enchantId) {
        if (enchantId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ALLOWED_MATERIALS.get(normalize(enchantId)));
    }

    public boolean canApplyToItem(final String enchantId, final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        final String normId = normalize(enchantId);
        final String matName = item.getType().name().toUpperCase(Locale.ROOT);

        if (normId.equals(EnchantIds.DUNE_SPEED)) {
            return matName.endsWith("_BOOTS") || matName.contains("BOOTS");
        }

        final Optional<Set<Material>> allowed = allowedMaterials(normId);
        if (allowed.isPresent()) {
            if (allowed.get().contains(item.getType())) {
                return true;
            }
            if (normId.equals(EnchantIds.PLATED) || normId.equals(EnchantIds.SOARING)) {
                return item.getType() == Material.ELYTRA || matName.endsWith("_CHESTPLATE") || matName.contains("CHESTPLATE");
            }
            return false;
        }

        if (normId.endsWith("_feet") || normId.endsWith("_boots")) {
            return matName.endsWith("_BOOTS") || matName.contains("BOOTS");
        }
        if (normId.endsWith("_legs") || normId.endsWith("_leggings")) {
            return matName.endsWith("_LEGGINGS") || matName.contains("LEGGINGS");
        }
        if (normId.endsWith("_chest") || normId.endsWith("_chestplate") || normId.endsWith("_body")) {
            return matName.endsWith("_CHESTPLATE") || matName.contains("CHESTPLATE") || item.getType() == Material.ELYTRA;
        }
        if (normId.endsWith("_head") || normId.endsWith("_helmet")) {
            return matName.endsWith("_HELMET") || matName.contains("HELMET") || item.getType() == Material.TURTLE_HELMET;
        }

        return true;
    }

    private String normalize(final String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
