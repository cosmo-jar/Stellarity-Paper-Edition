package dev.cosmojar.stellaritypaper.mechanics.uninstall;

import net.minecraft.nbt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class UninstallAdapter1_21_4 {

    private static final Set<String> CUSTOM_NAMESPACES = Set.of(
        "stellarity",
        "awesomedungeonend",
        "far_end",
        "kohara",
        "fwaystones",
        "waystones",
        "fokastudio"
    );

    public void fixLevelDat(File levelDatFile) throws Exception {
        Path path = levelDatFile.toPath();
        CompoundTag rootTag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        if (rootTag == null || !rootTag.contains("Data")) {
            return;
        }

        CompoundTag dataTag = rootTag.getCompound("Data");
        if (dataTag == null) return;

        if (dataTag.contains("DragonFight")) {
            CompoundTag dfTag = new CompoundTag();
            dfTag.putBoolean("DragonKilled", false);
            dfTag.putBoolean("PreviouslyKilled", false);
            dfTag.putBoolean("NeedsStateFix", false);
            dataTag.put("DragonFight", dfTag);
        }

        if (dataTag.contains("WorldGenSettings")) {
            CompoundTag worldGenTag = dataTag.getCompound("WorldGenSettings");
            if (worldGenTag != null && worldGenTag.contains("dimensions")) {
                CompoundTag dimensionsTag = worldGenTag.getCompound("dimensions");
                if (dimensionsTag != null && dimensionsTag.contains("minecraft:the_end")) {
                    CompoundTag endTag = dimensionsTag.getCompound("minecraft:the_end");
                    if (endTag != null && endTag.contains("generator")) {
                        CompoundTag generatorTag = endTag.getCompound("generator");
                        if (generatorTag != null && generatorTag.contains("biome_source")) {
                            CompoundTag biomeSourceTag = generatorTag.getCompound("biome_source");
                            if (biomeSourceTag != null) {
                                biomeSourceTag.putString("type", "minecraft:the_end");
                                biomeSourceTag.remove("biomes");
                            }
                        }
                    }
                }
            }
        }

        if (dataTag.contains("DataPacks")) {
            CompoundTag datapacksTag = dataTag.getCompound("DataPacks");

            if (datapacksTag != null && datapacksTag.contains("Enabled")) {
                ListTag enabledList = datapacksTag.getList("Enabled", 8);
                if (enabledList != null) {
                    for (int i = enabledList.size() - 1; i >= 0; i--) {
                        String packName = enabledList.getString(i);
                        if (isCustomNamespace(packName)) {
                            enabledList.remove(i);
                        }
                    }
                }
            }

            if (datapacksTag != null && datapacksTag.contains("Disabled")) {
                ListTag disabledList = datapacksTag.getList("Disabled", 8);
                if (disabledList != null) {
                    for (int i = disabledList.size() - 1; i >= 0; i--) {
                        String packName = disabledList.getString(i);
                        if (isCustomNamespace(packName)) {
                            disabledList.remove(i);
                        }
                    }
                }
            }
        }

        NbtIo.writeCompressed(rootTag, path);
    }

    public boolean cleanChunkTag(Object rawChunkTag) {
        if (!(rawChunkTag instanceof CompoundTag chunkTag)) return false;
        boolean modified = false;

        if (chunkTag.contains("sections")) {
            ListTag sections = chunkTag.getList("sections", 10);
            if (sections != null) {
                for (int i = 0; i < sections.size(); i++) {
                    CompoundTag sectionCompound = sections.getCompound(i);
                    if (sectionCompound == null) continue;

                    if (sectionCompound.contains("biomes")) {
                        CompoundTag biomesTag = sectionCompound.getCompound("biomes");
                        if (biomesTag != null && biomesTag.contains("palette")) {
                            ListTag paletteList = biomesTag.getList("palette", 8);
                            if (paletteList != null) {
                                for (int p = 0; p < paletteList.size(); p++) {
                                    String val = paletteList.getString(p);
                                    if (isCustomNamespace(val)) {
                                        paletteList.set(p, StringTag.valueOf("minecraft:the_end"));
                                        modified = true;
                                    }
                                }
                            }
                        }
                    }

                    if (sectionCompound.contains("block_states")) {
                        CompoundTag blockStatesTag = sectionCompound.getCompound("block_states");
                        if (blockStatesTag != null && blockStatesTag.contains("palette")) {
                            ListTag paletteList = blockStatesTag.getList("palette", 10);
                            if (paletteList != null) {
                                for (int p = 0; p < paletteList.size(); p++) {
                                    CompoundTag blockCompound = paletteList.getCompound(p);
                                    if (blockCompound != null && blockCompound.contains("Name")) {
                                        String nameVal = blockCompound.getString("Name");
                                        if ("minecraft:structure_block".equals(nameVal) || "structure_block".equals(nameVal) || isCustomNamespace(nameVal)) {
                                            blockCompound.putString("Name", "minecraft:air");
                                            blockCompound.remove("Properties");
                                            modified = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (chunkTag.contains("structures")) {
            CompoundTag structuresTag = chunkTag.getCompound("structures");
            if (structuresTag != null) {
                if (structuresTag.contains("starts")) {
                    CompoundTag startsTag = structuresTag.getCompound("starts");
                    if (startsTag != null) {
                        List<String> keysToRemove = new ArrayList<>();
                        for (String key : startsTag.getAllKeys()) {
                            if (isCustomNamespace(key)) {
                                keysToRemove.add(key);
                            } else {
                                CompoundTag startCompound = startsTag.getCompound(key);
                                if (startCompound != null && cleanCompoundRecursive(startCompound)) {
                                    modified = true;
                                }
                            }
                        }
                        for (String key : keysToRemove) {
                            startsTag.remove(key);
                            modified = true;
                        }
                    }
                }

                if (structuresTag.contains("References")) {
                    CompoundTag referencesTag = structuresTag.getCompound("References");
                    if (referencesTag != null) {
                        List<String> keysToRemove = new ArrayList<>();
                        for (String key : referencesTag.getAllKeys()) {
                            if (isCustomNamespace(key)) {
                                keysToRemove.add(key);
                            }
                        }
                        for (String key : keysToRemove) {
                            referencesTag.remove(key);
                            modified = true;
                        }
                    }
                }
            }
        }

        if (chunkTag.contains("block_entities")) {
            ListTag blockEntitiesList = chunkTag.getList("block_entities", 10);
            if (blockEntitiesList != null) {
                for (int i = blockEntitiesList.size() - 1; i >= 0; i--) {
                    CompoundTag beCompound = blockEntitiesList.getCompound(i);
                    if (beCompound != null) {
                        if (beCompound.contains("id")) {
                            String idVal = beCompound.getString("id");
                            if ("minecraft:structure_block".equals(idVal) || "structure_block".equals(idVal) || isCustomNamespace(idVal)) {
                                blockEntitiesList.remove(i);
                                modified = true;
                                continue;
                            }
                        }
                        if (beCompound.contains("Items")) {
                            ListTag itemsList = beCompound.getList("Items", 10);
                            if (itemsList != null) {
                                for (int it = itemsList.size() - 1; it >= 0; it--) {
                                    CompoundTag itemCompound = itemsList.getCompound(it);
                                    if (itemCompound != null) {
                                        String itemId = itemCompound.getString("id");
                                        if (isCustomNamespace(itemId)) {
                                            itemsList.remove(it);
                                            modified = true;
                                            continue;
                                        }
                                        if (itemCompound.contains("components")) {
                                            CompoundTag comp = itemCompound.getCompound("components");
                                            if (comp != null && cleanItemComponents(comp)) {
                                                modified = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (chunkTag.contains("entities")) {
            ListTag entitiesList = chunkTag.getList("entities", 10);
            if (entitiesList != null) {
                for (int i = entitiesList.size() - 1; i >= 0; i--) {
                    CompoundTag entCompound = entitiesList.getCompound(i);
                    if (entCompound != null && entCompound.contains("id")) {
                        String idVal = entCompound.getString("id");
                        if (isCustomNamespace(idVal)) {
                            entitiesList.remove(i);
                            modified = true;
                        }
                    }
                }
            }
        }

        return modified;
    }

    private static boolean cleanCompoundRecursive(CompoundTag compound) {
        boolean modified = false;
        List<String> keys = new ArrayList<>(compound.getAllKeys());
        for (String key : keys) {
            Tag tag = compound.get(key);
            if (tag instanceof StringTag stringTag) {
                String val = stringTag.getAsString();
                if (isCustomNamespace(val)) {
                    if ("processors".equalsIgnoreCase(key) || "location".equalsIgnoreCase(key) || "pool".equalsIgnoreCase(key)) {
                        compound.putString(key, "minecraft:empty");
                    } else if ("element_type".equalsIgnoreCase(key)) {
                        compound.putString(key, "minecraft:single_pool_element");
                    } else {
                        compound.putString(key, "minecraft:empty");
                    }
                    modified = true;
                }
            } else if (tag instanceof CompoundTag childCompound) {
                if (cleanCompoundRecursive(childCompound)) {
                    modified = true;
                }
            } else if (tag instanceof ListTag listTag) {
                for (int i = 0; i < listTag.size(); i++) {
                    Tag elem = listTag.get(i);
                    if (elem instanceof CompoundTag elemCompound) {
                        if (cleanCompoundRecursive(elemCompound)) {
                            modified = true;
                        }
                    } else if (elem instanceof StringTag strElem) {
                        String val = strElem.getAsString();
                        if (isCustomNamespace(val)) {
                            listTag.set(i, StringTag.valueOf("minecraft:empty"));
                            modified = true;
                        }
                    }
                }
            }
        }
        return modified;
    }

    private static boolean cleanItemComponents(CompoundTag componentsTag) {
        boolean modified = false;
        List<String> keys = new ArrayList<>(componentsTag.getAllKeys());
        for (String key : keys) {
            if (isCustomNamespace(key)) {
                componentsTag.remove(key);
                modified = true;
            } else if ("minecraft:stored_enchantments".equals(key) || "minecraft:enchantments".equals(key)) {
                CompoundTag enchants = componentsTag.getCompound(key);
                if (enchants != null) {
                    List<String> enchKeys = new ArrayList<>(enchants.getAllKeys());
                    for (String enchKey : enchKeys) {
                        if (isCustomNamespace(enchKey)) {
                            enchants.remove(enchKey);
                            modified = true;
                        }
                    }
                }
            }
        }
        return modified;
    }

    private static boolean isCustomNamespace(String id) {
        if (id == null || id.isEmpty()) return false;
        int colonIdx = id.indexOf(':');
        if (colonIdx != -1) {
            String ns = id.substring(0, colonIdx);
            if (CUSTOM_NAMESPACES.contains(ns)) return true;
        }
        for (String ns : CUSTOM_NAMESPACES) {
            if (id.contains(ns)) return true;
        }
        return false;
    }
}
