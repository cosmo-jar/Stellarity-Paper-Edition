package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class CustomItemMatcher {

    private final ItemStateRepository itemStateRepository;

    public CustomItemMatcher(final ItemStateRepository itemStateRepository) {
        this.itemStateRepository = itemStateRepository;
    }

    public Optional<String> resolveRawId(final ItemStack item) {
        return itemStateRepository.getItemId(item);
    }

    public boolean isCustom(final ItemStack item, final String expectedItemId) {
        return resolveRawId(item).map(value -> value.equalsIgnoreCase(expectedItemId)).orElse(false);
    }
}
