package dev.cosmojar.stellaritypaper.items.definitions.tools;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;
import org.bukkit.Material;

/**
 * Версионный адаптер предмета ShulkerSpear для версии Minecraft 1.21.8.
 */
public class ShulkerSpearItemAdapter1_21_8 implements VersionAdapter {

    @Override
    public boolean supports(ServerVersion version) {
        return version == ServerVersion.V1_21_8;
    }

    @Override
    public String getAdapterName() {
        return "ShulkerSpear-Adapter (MC 1.21.8)";
    }

    public Material getFallbackMaterial() {
        return Material.NETHERITE_SWORD;
    }
}
