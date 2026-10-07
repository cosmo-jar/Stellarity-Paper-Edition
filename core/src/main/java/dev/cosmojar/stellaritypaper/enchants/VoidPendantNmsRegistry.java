package dev.cosmojar.stellaritypaper.enchants;

import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.CraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * NMS регистрация технических зачарований для Void Pendant.
 * Используется регистрация при запуске сервера до заморозки реестра чар (unfreeze -> intrusive holder -> register -> freeze).
 */
public final class VoidPendantNmsRegistry {

    private static final String[] GEMS = {
            "amethyst", "copper", "diamond", "emerald", "gold", "iron", "lapis", "netherite", "quartz"
    };

    public void registerAll() {
        try {
            final var nmsRegistry = ((CraftServer) Bukkit.getServer()).getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            if (!(nmsRegistry instanceof MappedRegistry<Enchantment> mapped)) {
                return;
            }

            final Field frozenField = getFrozenField();
            final Field allTagsField = getAllTagsField();
            final Field intrusiveHoldersField = getUnregisteredIntrusiveHoldersField();

            // 1. Unfreeze
            if (frozenField != null) {
                frozenField.set(mapped, false);
            }

            // 2. Set unregistered intrusive holders map
            if (intrusiveHoldersField != null) {
                intrusiveHoldersField.set(mapped, new IdentityHashMap<Enchantment, net.minecraft.core.Holder.Reference<Enchantment>>());
            }

            // 3. Reset allTags to unbound TagSet
            if (allTagsField != null) {
                final Class<?> tagSetClass = allTagsField.getType();
                for (final Method m : tagSetClass.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0 && m.getReturnType().equals(tagSetClass)) {
                        m.setAccessible(true);
                        final Object unboundTagSet = m.invoke(null);
                        allTagsField.set(mapped, unboundTagSet);
                        break;
                    }
                }
            }

            final HolderSet<Item> emptyItems = HolderSet.direct();

            for (final String gem : GEMS) {
                final net.minecraft.resources.Identifier location = net.minecraft.resources.Identifier.fromNamespaceAndPath("stellarity", "_technical/void_pendant/" + gem);

                if (mapped.containsKey(location)) {
                    continue;
                }

                final Enchantment enchantment = Enchantment.enchantment(
                        Enchantment.definition(
                                emptyItems,
                                1,
                                1,
                                Enchantment.constantCost(1),
                                Enchantment.constantCost(1),
                                0
                        )
                ).build(location);

                mapped.createIntrusiveHolder(enchantment);
                Registry.register(mapped, location, enchantment);
            }

            // 4. Freeze registry
            mapped.freeze();

            // 5. Clear CraftRegistry cache
            try {
                final org.bukkit.Registry<org.bukkit.enchantments.Enchantment> bukkitRegistry = org.bukkit.Registry.ENCHANTMENT;
                final Field cacheField = CraftRegistry.class.getDeclaredField("cache");
                cacheField.setAccessible(true);
                cacheField.set(bukkitRegistry, new java.util.HashMap<NamespacedKey, org.bukkit.enchantments.Enchantment>());
            } catch (final Throwable ignored) {
            }
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    private static Field getFrozenField() {
        try {
            final Field f = MappedRegistry.class.getDeclaredField("frozen");
            f.setAccessible(true);
            return f;
        } catch (final NoSuchFieldException e) {
            for (final Field f : MappedRegistry.class.getDeclaredFields()) {
                if (f.getType().isPrimitive() && f.getType().equals(boolean.class)) {
                    f.setAccessible(true);
                    return f;
                }
            }
            return null;
        }
    }

    private static Field getAllTagsField() {
        try {
            final Field f = MappedRegistry.class.getDeclaredField("allTags");
            f.setAccessible(true);
            return f;
        } catch (final NoSuchFieldException e) {
            for (final Field f : MappedRegistry.class.getDeclaredFields()) {
                if (f.getType().getName().contains("TagSet")) {
                    f.setAccessible(true);
                    return f;
                }
            }
            return null;
        }
    }

    private static Field getUnregisteredIntrusiveHoldersField() {
        try {
            final Field f = MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            f.setAccessible(true);
            return f;
        } catch (final NoSuchFieldException e) {
            Field candidate = null;
            for (final Field f : MappedRegistry.class.getDeclaredFields()) {
                if (f.getType().equals(Map.class)) {
                    candidate = f;
                }
            }
            if (candidate != null) {
                candidate.setAccessible(true);
                return candidate;
            }
            return null;
        }
    }
}