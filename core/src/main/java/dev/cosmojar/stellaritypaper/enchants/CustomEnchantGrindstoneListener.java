package dev.cosmojar.stellaritypaper.enchants;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CustomEnchantGrindstoneListener implements Listener {

    private final ItemStateRepository itemStateRepository;
    private final EnchantItemService enchantItemService;
    private final EnchantDataCodec enchantDataCodec;

    public CustomEnchantGrindstoneListener(
            final ItemStateRepository itemStateRepository,
            final EnchantItemService enchantItemService,
            final EnchantDataCodec enchantDataCodec
    ) {
        this.itemStateRepository = itemStateRepository;
        this.enchantItemService = enchantItemService;
        this.enchantDataCodec = enchantDataCodec;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareGrindstone(final PrepareGrindstoneEvent event) {
        final GrindstoneInventory inventory = event.getInventory();
        final ItemStack top = inventory.getItem(0);
        final ItemStack bottom = inventory.getItem(1);

        final boolean topCustom = top != null && itemStateRepository.getEnchantsData(top).isPresent();
        final boolean bottomCustom = bottom != null && itemStateRepository.getEnchantsData(bottom).isPresent();

        if (!topCustom && !bottomCustom) {
            return;
        }

        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) {
            final ItemStack source = topCustom ? top : bottom;
            if (!source.getType().isAir()) {
                if (source.getType() == Material.ENCHANTED_BOOK) {
                    result = new ItemStack(Material.BOOK);
                } else {
                    result = source.clone();
                }
            }
        } else {
            result = result.clone();
        }

        if (result != null && !result.getType().isAir()) {
            if (result.getType() == Material.ENCHANTED_BOOK) {
                result = new ItemStack(Material.BOOK);
            }
            itemStateRepository.setEnchantsData(result, null);
            enchantItemService.rebuildNow(result);
            event.setResult(result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.GRINDSTONE) {
            return;
        }
        if (event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null || currentItem.getType().isAir()) {
            return;
        }

        if (itemStateRepository.getEnchantsData(currentItem).isPresent()) {
            itemStateRepository.setEnchantsData(currentItem, null);
            enchantItemService.rebuildNow(currentItem);
        }

        final GrindstoneInventory inventory = (GrindstoneInventory) event.getInventory();
        final ItemStack top = inventory.getItem(0);
        final ItemStack bottom = inventory.getItem(1);

        int totalCustomXp = 0;

        if (top != null && !top.getType().isAir()) {
            totalCustomXp += calculateCustomXp(top);
        }
        if (bottom != null && !bottom.getType().isAir()) {
            totalCustomXp += calculateCustomXp(bottom);
        }

        if (totalCustomXp > 0) {
            spawnExperienceOrbs(player, totalCustomXp);
        }
    }

    private int calculateCustomXp(final ItemStack item) {
        final String data = itemStateRepository.getEnchantsData(item).orElse(null);
        if (data == null || data.isEmpty()) {
            return 0;
        }
        final List<EnchantInstance> instances = enchantDataCodec.decode(data);
        int totalXp = 0;
        for (final EnchantInstance instance : instances) {
            int level = Math.max(1, instance.level());
            totalXp += ThreadLocalRandom.current().nextInt(level * 2, (level * 6) + 1);
        }
        return totalXp;
    }

    private void spawnExperienceOrbs(final Player player, final int totalXp) {
        final Location loc = player.getLocation();
        final int orbCount = ThreadLocalRandom.current().nextInt(2, 5);
        final int xpPerOrb = Math.max(1, totalXp / orbCount);
        final int remainder = totalXp % orbCount;

        for (int i = 0; i < orbCount; i++) {
            final int xp = xpPerOrb + (i == 0 ? remainder : 0);
            if (xp > 0 && loc.getWorld() != null) {
                loc.getWorld().spawn(loc, ExperienceOrb.class, orb -> orb.setExperience(xp));
            }
        }
    }
}
