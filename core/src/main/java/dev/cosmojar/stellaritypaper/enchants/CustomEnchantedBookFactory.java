package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public final class CustomEnchantedBookFactory {

    private final EnchantDefinitionRegistry enchantRegistry;
    private final ItemStateRepository itemStateRepository;
    private final EnchantDataCodec codec;
    private final EnchantItemService enchantItemService;

    public CustomEnchantedBookFactory(
            final EnchantDefinitionRegistry enchantRegistry,
            final ItemStateRepository itemStateRepository,
            final EnchantDataCodec codec,
            final EnchantItemService enchantItemService
    ) {
        this.enchantRegistry = enchantRegistry;
        this.itemStateRepository = itemStateRepository;
        this.codec = codec;
        this.enchantItemService = enchantItemService;
    }

    public Optional<ItemStack> createBook(final String enchantId, final int level) {
        final Optional<EnchantDefinition> definition = enchantRegistry.findById(enchantId);
        if (definition.isEmpty()) {
            return Optional.empty();
        }
        final EnchantDefinition def = definition.get();
        final int validLevel = Math.max(1, Math.min(level, def.tiers().size()));

        final ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);

        final List<EnchantInstance> enchants = List.of(new EnchantInstance(def.id(), validLevel));
        itemStateRepository.setEnchantsData(book, codec.encode(enchants));
        enchantItemService.rebuildNow(book);

        return Optional.of(book);
    }
}
