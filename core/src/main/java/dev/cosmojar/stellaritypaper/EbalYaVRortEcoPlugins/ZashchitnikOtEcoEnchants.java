package dev.cosmojar.stellaritypaper.EbalYaVRortEcoPlugins;

import com.willfp.eco.core.display.DisplayContext;
import com.willfp.eco.core.display.DisplayModule;
import com.willfp.eco.core.display.DisplayProperties;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A protective class for Eco modules (EnchantDisplay, ItemFlagDisplay и др.).
 * Intercepts display/revert calls and blocks them for all Stellarity items,
 * so that ECO couldn’t even lay a finger on the lore and break the TranslatableComponent 😡😡😡.
 */
public final class ZashchitnikOtEcoEnchants extends DisplayModule {

    private final DisplayModule delegat;

    public ZashchitnikOtEcoEnchants(final DisplayModule delegat) {
        super(delegat.getPlugin(), delegat.getWeight());
        this.delegat = delegat;
    }

    /**
     * Modern Eco display method (2026.39+).
     */
    @Override
    public void display(final DisplayContext context) {
        if (context != null && ecohook.etoPredmetStellarity(context.getItemStack())) {
            return;
        }
        delegat.display(context);
    }

    /**
     * Legacy Eco display method without player context (scheduled for removal in 2027.39).
     */
    @Override
    @SuppressWarnings({"deprecation", "removal"})
    public void display(final ItemStack item, final Object... args) {
        if (ecohook.etoPredmetStellarity(item)) {
            return;
        }
        delegat.display(item, args);
    }

    /**
     * Legacy Eco display method with player context (scheduled for removal in 2027.39).
     */
    @Override
    @SuppressWarnings({"deprecation", "removal"})
    public void display(final ItemStack item, final Player player, final Object... args) {
        if (ecohook.etoPredmetStellarity(item)) {
            return;
        }
        delegat.display(item, player, args);
    }

    /**
     * Legacy Eco display method with player context and properties (scheduled for removal in 2027.39).
     */
    @Override
    @SuppressWarnings({"deprecation", "removal"})
    public void display(final ItemStack item, final Player player, final DisplayProperties props, final Object... args) {
        if (ecohook.etoPredmetStellarity(item)) {
            return;
        }
        delegat.display(item, player, props, args);
    }

    /**
     * Legacy Eco revert method (scheduled for removal in 2027.39).
     */
    @Override
    @SuppressWarnings({"deprecation", "removal"})
    public void revert(final ItemStack item) {
        if (ecohook.etoPredmetStellarity(item)) {
            return;
        }
        delegat.revert(item);
    }

    @Override
    public Object[] generateVarArgs(final ItemStack item) {
        if (ecohook.etoPredmetStellarity(item)) {
            return new Object[0];
        }
        return delegat.generateVarArgs(item);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == delegat) return true;
        if (obj instanceof ZashchitnikOtEcoEnchants other) {
            return delegat.equals(other.delegat);
        }
        return delegat.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegat.hashCode();
    }
}
