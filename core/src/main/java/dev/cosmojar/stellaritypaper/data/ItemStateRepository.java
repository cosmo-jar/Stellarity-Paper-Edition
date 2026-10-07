package dev.cosmojar.stellaritypaper.data;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class ItemStateRepository {

    private final PdcKeys keys;
    private final NamespacedKey itemKey = new NamespacedKey("stellarity", "item");

    public ItemStateRepository(final PdcKeys keys) {
        this.keys = keys;
    }

    public boolean isCustomItem(final ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(keys.itemId(), PersistentDataType.STRING)
                || pdc.has(itemKey, PersistentDataType.STRING);
    }

    public void setItemId(final ItemStack item, final String itemId) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(keys.itemId(), PersistentDataType.STRING, itemId);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, itemId);
        item.setItemMeta(meta);
    }

    public Optional<String> getItemId(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        String id = meta.getPersistentDataContainer().get(keys.itemId(), PersistentDataType.STRING);
        if (id == null) {
            id = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        }
        return Optional.ofNullable(id);
    }

    public void setEnchantsData(final ItemStack item, final String data) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        if (data == null || data.isEmpty()) {
            meta.getPersistentDataContainer().remove(keys.enchantsData());
        } else {
            meta.getPersistentDataContainer().set(keys.enchantsData(), PersistentDataType.STRING, data);
        }
        item.setItemMeta(meta);
    }

    public Optional<String> getEnchantsData(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(meta.getPersistentDataContainer().get(keys.enchantsData(), PersistentDataType.STRING));
    }

    public void setEnchantsSchema(final ItemStack item, final int schema) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(keys.enchantsSchema(), PersistentDataType.INTEGER, schema);
        item.setItemMeta(meta);
    }

    public Optional<Integer> getEnchantsSchema(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(meta.getPersistentDataContainer().get(keys.enchantsSchema(), PersistentDataType.INTEGER));
    }

    public void setEnchantsLocale(final ItemStack item, final String locale) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(keys.enchantsLocale(), PersistentDataType.STRING, locale);
        item.setItemMeta(meta);
    }

    public Optional<String> getEnchantsLocale(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(meta.getPersistentDataContainer().get(keys.enchantsLocale(), PersistentDataType.STRING));
    }

    public void setHarvesterAbilities(final ItemStack item, final String data) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(keys.harvesterAbilities(), PersistentDataType.STRING, data);
        item.setItemMeta(meta);
    }

    public Optional<String> getHarvesterAbilities(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(meta.getPersistentDataContainer().get(keys.harvesterAbilities(), PersistentDataType.STRING));
    }
}
