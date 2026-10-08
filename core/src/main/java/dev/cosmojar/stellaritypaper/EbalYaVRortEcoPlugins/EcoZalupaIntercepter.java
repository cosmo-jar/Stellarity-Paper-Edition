package dev.cosmojar.stellaritypaper.EbalYaVRortEcoPlugins;

import com.willfp.eco.core.display.DisplayModule;
import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Isolated interceptor of Eco modules.
 * Supports both modern Eco (2026.39+, Display.REGISTRY) and legacy Eco (<=2026.38, Display.REGISTERED_MODULES).
 */
public final class EcoZalupaIntercepter {

    private EcoZalupaIntercepter() {
    }

    public static void vstavitZalupuVEcoDisplay(final StellarityPaperPlugin plugin) {
        logEcoVersion(plugin);
        injectInternal(plugin, true);
    }

    public static void obnovitZashituDisplay() {
        injectInternal(null, false);
    }

    private static void logEcoVersion(final StellarityPaperPlugin plugin) {
        if (plugin == null) {
            return;
        }
        try {
            final Plugin eco = Bukkit.getPluginManager().getPlugin("eco");
            if (eco != null) {
                final String ver = eco.getPluginMeta().getVersion();
                plugin.getLogger().info("Detected Eco core version: " + ver);
            }
        } catch (final Throwable ignored) {
        }
    }

    private static void injectInternal(final StellarityPaperPlugin plugin, final boolean logErrors) {
        try {
            final Class<?> displayClass = Class.forName("com.willfp.eco.core.display.Display");

            // 1. Modern Eco (2026.39+): Display.REGISTRY -> DisplayModuleRegistry.modules
            if (injectModernRegistry(displayClass)) {
                return;
            }

            // 2. Legacy Eco (<=2026.38): Display.REGISTERED_MODULES -> Map<Integer, List<DisplayModule>>
            if (injectLegacyMap(displayClass)) {
                return;
            }

            if (logErrors && plugin != null) {
                plugin.getLogger().warning("Neither Display.REGISTRY nor Display.REGISTERED_MODULES was found in Eco core.");
            }
        } catch (final ClassNotFoundException ignored) {
        } catch (final Throwable t) {
            if (logErrors && plugin != null) {
                plugin.getLogger().warning("Injection error in Eco Display: " + t.getMessage());
            }
        }
    }

    private static boolean injectModernRegistry(final Class<?> displayClass) {
        try {
            final Field registryField = displayClass.getDeclaredField("REGISTRY");
            registryField.setAccessible(true);
            final Object registry = registryField.get(null);
            if (registry == null) {
                return false;
            }

            final Field modulesField = registry.getClass().getDeclaredField("modules");
            modulesField.setAccessible(true);

            synchronized (registry) {
                @SuppressWarnings("unchecked")
                final List<DisplayModule> currentList = (List<DisplayModule>) modulesField.get(registry);
                if (currentList == null) {
                    return true;
                }

                boolean needsUpdate = false;
                for (final DisplayModule mod : currentList) {
                    if (!(mod instanceof ZashchitnikOtEcoEnchants)) {
                        needsUpdate = true;
                        break;
                    }
                }

                if (needsUpdate) {
                    final List<DisplayModule> wrapped = new ArrayList<>(currentList.size());
                    for (final DisplayModule mod : currentList) {
                        if (mod instanceof ZashchitnikOtEcoEnchants) {
                            wrapped.add(mod);
                        } else {
                            wrapped.add(new ZashchitnikOtEcoEnchants(mod));
                        }
                    }
                    modulesField.set(registry, List.copyOf(wrapped));
                }
            }
            return true;
        } catch (final NoSuchFieldException e) {
            return false;
        } catch (final Throwable ignored) {
            return false;
        }
    }

    private static boolean injectLegacyMap(final Class<?> displayClass) {
        try {
            final Field modulesField = displayClass.getDeclaredField("REGISTERED_MODULES");
            modulesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            final Map<Integer, List<DisplayModule>> modulesMap = (Map<Integer, List<DisplayModule>>) modulesField.get(null);
            if (modulesMap == null) {
                return false;
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
            return true;
        } catch (final NoSuchFieldException e) {
            return false;
        } catch (final Throwable ignored) {
            return false;
        }
    }
}
