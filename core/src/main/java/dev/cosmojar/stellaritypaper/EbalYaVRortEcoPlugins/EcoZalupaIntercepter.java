package dev.cosmojar.stellaritypaper.EbalYaVRortEcoPlugins;

import com.willfp.eco.core.display.DisplayModule;
import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Isolated interceptor of Eco modules.
 */
public final class EcoZalupaIntercepter {

    private EcoZalupaIntercepter() {
    }

    public static void vstavitZalupuVEcoDisplay(final StellarityPaperPlugin plugin) {
        try {
            final Class<?> displayClass = Class.forName("com.willfp.eco.core.display.Display");
            final Field modulesField = displayClass.getDeclaredField("REGISTERED_MODULES");
            modulesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            final Map<Integer, List<DisplayModule>> modulesMap = (Map<Integer, List<DisplayModule>>) modulesField.get(null);
            if (modulesMap == null) {
                return;
            }

            synchronized (modulesMap) {
                for (final Map.Entry<Integer, List<DisplayModule>> entry : modulesMap.entrySet()) {
                    final List<DisplayModule> list = entry.getValue();
                    if (list == null) continue;
                    for (int i = 0; i < list.size(); i++) {
                        final DisplayModule mod = list.get(i);
                        if (!(mod instanceof ZashchitnikOtEcoEnchants)) {
                            list.set(i, new ZashchitnikOtEcoEnchants(mod));
                        }
                    }
                }
            }

        } catch (final ClassNotFoundException ignored) {
        } catch (final Throwable t) {
            plugin.getLogger().warning("Injection error in Display.REGISTERED_MODULES: " + t.getMessage());
        }
    }

    public static void obnovitZashituDisplay() {
        try {
            final Class<?> displayClass = Class.forName("com.willfp.eco.core.display.Display");
            final Field modulesField = displayClass.getDeclaredField("REGISTERED_MODULES");
            modulesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            final Map<Integer, List<DisplayModule>> modulesMap = (Map<Integer, List<DisplayModule>>) modulesField.get(null);
            if (modulesMap == null) {
                return;
            }

            synchronized (modulesMap) {
                for (final Map.Entry<Integer, List<DisplayModule>> entry : modulesMap.entrySet()) {
                    final List<DisplayModule> list = entry.getValue();
                    if (list == null) continue;
                    for (int i = 0; i < list.size(); i++) {
                        final DisplayModule mod = list.get(i);
                        if (!(mod instanceof ZashchitnikOtEcoEnchants)) {
                            list.set(i, new ZashchitnikOtEcoEnchants(mod));
                        }
                    }
                }
            }
        } catch (final Throwable ignored) {
        }
    }
}
