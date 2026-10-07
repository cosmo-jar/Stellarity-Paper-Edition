package dev.cosmojar.stellaritypaper.mechanics.tools;

import org.bukkit.Material;

public final class ShulkerSpearAdapter26_1_2 {

    public Material getShulkerSpearMaterial() {
        Material spear = Material.matchMaterial("netherite_spear");
        return spear != null ? spear : Material.NETHERITE_SWORD;
    }
}
