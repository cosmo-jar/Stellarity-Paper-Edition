package dev.cosmojar.stellaritypaper.mechanics.loot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ExplorerMapService {

    private final Plugin plugin;

    public ExplorerMapService(
            final Plugin plugin,
            final dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry itemDefinitionRegistry,
            final dev.cosmojar.stellaritypaper.items.CustomItemFactory customItemFactory
    ) {
        this.plugin = plugin;
    }

    public boolean isMapItemKey(final String itemKey) {
        if (itemKey == null) return false;
        final String k = itemKey.toLowerCase(Locale.ROOT);
        return k.equals("chapel_of_light") || k.equals("chapel_explorer_map")
                || k.equals("end_city")    || k.equals("end_city_explorer_map")
                || k.equals("village")     || k.equals("village_explorer_map")
                || k.equals("floating_treasure") || k.equals("floating_treasure_explorer_map");
    }

    public Optional<ItemStack> createExplorerMap(final String itemKey, final World world, final Location originLoc) {
        if (itemKey == null) return Optional.empty();
        final String k = itemKey.toLowerCase(Locale.ROOT);

        final String translateKey;
        final String structureSearchTag;

        if (k.contains("chapel")) {
            translateKey       = "filled_map.stellarity.chapel_of_light";
            structureSearchTag = "stellarity:chapel_of_light";
        } else if (k.contains("end_city")) {
            translateKey       = "filled_map.stellarity.end_city";
            structureSearchTag = "minecraft:end_city";
        } else if (k.contains("village")) {
            translateKey       = "filled_map.stellarity.end_village";
            structureSearchTag = "stellarity:village";
        } else if (k.contains("floating_treasure")) {
            translateKey       = "filled_map.stellarity.floating_treasure";
            structureSearchTag = "stellarity:floating_treasure";
        } else {
            return Optional.empty();
        }

        final World targetWorld = resolveEndWorld(world, originLoc);
        final Location searchCenter;
        if (targetWorld != null) {
            if (originLoc != null) {
                searchCenter = new Location(targetWorld, originLoc.getX(), originLoc.getY(), originLoc.getZ());
            } else {
                searchCenter = targetWorld.getSpawnLocation();
            }
        } else {
            searchCenter = null;
        }

        final ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        final MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta == null) return Optional.of(mapItem);

        meta.displayName(Component.translatable(translateKey)
                .color(TextColor.color(0xCC26FF))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.translatable("Stellarity")
                        .color(TextColor.color(0xEAA7FF))
                        .font(net.kyori.adventure.key.Key.key("stellarity:tooltip"))
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.setColor(org.bukkit.Color.fromRGB(0xCC, 0x26, 0xFF));

        if (targetWorld != null && searchCenter != null) {
            Location structLoc = null;
            try {
                structLoc = locateStructure(targetWorld, searchCenter, structureSearchTag);
            } catch (final Exception e) {
                plugin.getLogger().warning("[ExplorerMapService] Structure lookup failed for '"
                        + structureSearchTag + "': " + e.getMessage());
            }
            if (structLoc == null) {
                structLoc = searchCenter;
                plugin.getLogger().info("[ExplorerMapService] '" + structureSearchTag
                        + "' not found within radius — map centred on container location.");
            }

            final MapView mapView = Bukkit.createMap(targetWorld);
            mapView.setCenterX(structLoc.getBlockX());
            mapView.setCenterZ(structLoc.getBlockZ());
            mapView.setScale(MapView.Scale.FAR);
            mapView.setTrackingPosition(true);
            meta.setMapView(mapView);
        }

        mapItem.setItemMeta(meta);
        return Optional.of(mapItem);
    }

    private Location locateStructure(final World world, final Location center, final String structureTag) {
        final int searchRadius = "stellarity:chapel_of_light".equals(structureTag) ? 5000 : 1000;

        if ("minecraft:end_city".equals(structureTag)) {
            final org.bukkit.util.StructureSearchResult r =
                    world.locateNearestStructure(center, Structure.END_CITY, searchRadius, false);
            return r != null ? r.getLocation() : null;
        }

        final NamespacedKey key = NamespacedKey.fromString(structureTag);
        if (key != null) {
            final Structure struct = org.bukkit.Registry.STRUCTURE.get(key);
            if (struct != null) {
                final org.bukkit.util.StructureSearchResult r =
                        world.locateNearestStructure(center, struct, searchRadius, false);
                return r != null ? r.getLocation() : null;
            }
            plugin.getLogger().warning("[ExplorerMapService] Structure '" + structureTag
                    + "' not found in Bukkit Registry.STRUCTURE — is the datapack loaded?");
        }
        return null;
    }

    private World resolveEndWorld(final World hint, final Location originLoc) {
        if (hint != null && hint.getEnvironment() == World.Environment.THE_END) {
            return hint;
        }
        if (originLoc != null && originLoc.getWorld() != null
                && originLoc.getWorld().getEnvironment() == World.Environment.THE_END) {
            return originLoc.getWorld();
        }
        for (final World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.THE_END) {
                return w;
            }
        }
        if (hint != null) return hint;
        if (originLoc != null) return originLoc.getWorld();
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }
}
