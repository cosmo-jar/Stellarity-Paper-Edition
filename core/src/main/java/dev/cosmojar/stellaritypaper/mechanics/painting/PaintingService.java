package dev.cosmojar.stellaritypaper.mechanics.painting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PaintingService {

    private final Plugin plugin;
    private final NamespacedKey variantPdcKey;

    public PaintingService(final Plugin plugin) {
        this.plugin = plugin;
        this.variantPdcKey = new NamespacedKey(plugin, "selected_painting_variant");
    }


    public void openSelector(final Player player, final Painting painting) {
        final org.bukkit.Art currentArt = painting.getArt();
        final int width = currentArt.getBlockWidth();
        final int height = currentArt.getBlockHeight();

        final List<org.bukkit.Art> matchingVariants = new ArrayList<>();
        for (final org.bukkit.Art variant : Registry.ART) {
            if (variant.getBlockWidth() == width && variant.getBlockHeight() == height) {
                matchingVariants.add(variant);
            }
        }

        if (matchingVariants.isEmpty()) {
            player.sendMessage(Component.text("No painting variants found for this size: " 
                    + width + "x" + height, NamedTextColor.RED));
            return;
        }

        int slots = ((matchingVariants.size() - 1) / 9 + 1) * 9;
        slots = Math.min(54, Math.max(9, slots));

        final PaintingInventoryHolder holder = new PaintingInventoryHolder(painting);
        final Component title = Component.text("Paintings (" + width + "x" + height + ")", NamedTextColor.DARK_GRAY);
        final Inventory inventory = Bukkit.createInventory(holder, slots, title);

        for (final org.bukkit.Art variant : matchingVariants) {
            final ItemStack icon = new ItemStack(Material.PAINTING);
            final ItemMeta meta = icon.getItemMeta();
            if (meta == null) continue;

            final NamespacedKey key = org.bukkit.Registry.ART.getKey(variant);
            if (key == null) continue;
            meta.getPersistentDataContainer().set(variantPdcKey, PersistentDataType.STRING, key.toString());

            final Optional<StellarityPainting> optStellarity = getStellarityPainting(key);

            if (optStellarity.isPresent()) {
                final StellarityPainting sp = optStellarity.get();
                meta.displayName(Component.translatable(sp.getTitleKey())
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));

                final List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Author: ", NamedTextColor.GRAY)
                        .append(Component.translatable(sp.getAuthorKey(), NamedTextColor.GOLD))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Size: " + width + "x" + height, NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Id: " + key.toString(), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
            } else {
                final String displayNameStr = formatName(key.getKey());
                meta.displayName(Component.text(displayNameStr, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));

                final List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Vanilla Painting", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Size: " + width + "x" + height, NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Id: " + key.toString(), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
            }

            icon.setItemMeta(meta);
            inventory.addItem(icon);
        }

        player.openInventory(inventory);
    }

    public void applyVariant(final Painting painting, final NamespacedKey key) {
        final org.bukkit.Art variant = Registry.ART.get(key);
        if (variant != null) {
            painting.setArt(variant);
        }
    }

    public NamespacedKey getVariantPdcKey() {
        return variantPdcKey;
    }

    private Optional<StellarityPainting> getStellarityPainting(final NamespacedKey key) {
        if (!"stellarity".equalsIgnoreCase(key.getNamespace())) {
            return Optional.empty();
        }
        for (final StellarityPainting sp : StellarityPainting.values()) {
            if (sp.getId().equalsIgnoreCase(key.getKey())) {
                return Optional.of(sp);
            }
        }
        return Optional.empty();
    }

    private String formatName(final String raw) {
        final String[] parts = raw.split("_");
        final StringBuilder sb = new StringBuilder();
        for (final String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1))
              .append(" ");
        }
        return sb.toString().trim();
    }
}
