package dev.cosmojar.stellaritypaper.adapter.v26_1_1;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;
import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManagerAdapter26_1_1;
import dev.cosmojar.stellaritypaper.mechanics.tools.ShulkerSpearAdapter26_1_1;
import org.bukkit.Material;
import org.bukkit.World;

public final class VersionAdapter26_1_1 implements VersionAdapter {

    private EndIslandManagerAdapter26_1_1 endIslandAdapter;
    private ShulkerSpearAdapter26_1_1 spearAdapter;

    private EndIslandManagerAdapter26_1_1 endIslandAdapter() {
        if (endIslandAdapter == null) endIslandAdapter = new EndIslandManagerAdapter26_1_1();
        return endIslandAdapter;
    }

    private ShulkerSpearAdapter26_1_1 spearAdapter() {
        if (spearAdapter == null) spearAdapter = new ShulkerSpearAdapter26_1_1();
        return spearAdapter;
    }

    @Override
    public boolean supports(final ServerVersion version) {
        return version == ServerVersion.V26_1_1;
    }

    @Override
    public String getAdapterName() {
        return "Paper-26.1.1-Adapter";
    }

    @Override
    public void setExitPortalLocation(World world, int x, int y, int z) {
        endIslandAdapter().setExitPortalLocation(world, x, y, z);
    }

    @Override
    public void resetDragonFight(World world) {
        endIslandAdapter().resetDragonFight(world);
    }

    @Override
    public Boolean getSendCommandFeedback(World world) {
        return endIslandAdapter().getSendCommandFeedback(world);
    }

    @Override
    public void setSendCommandFeedback(World world, Boolean value) {
        endIslandAdapter().setSendCommandFeedback(world, value);
    }

    @Override
    public Material getShulkerSpearMaterial() {
        return spearAdapter().getShulkerSpearMaterial();
    }

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter26_1_1 voidPendantAdapter;

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter26_1_1 voidPendantAdapter() {
        if (voidPendantAdapter == null) voidPendantAdapter = new dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter26_1_1();
        return voidPendantAdapter;
    }

    @Override
    public void registerVoidPendantEnchantments() {
        voidPendantAdapter().registerVoidPendantEnchantments();
    }

    @Override
    public void resetDragonFightForUninstall(World endWorld) {
        endIslandAdapter().resetDragonFightForUninstall(endWorld);
    }

    @Override
    public java.util.List<java.io.File> getExtraFilesToDeleteOnUninstall(World endWorld) {
        return endIslandAdapter().getExtraFilesToDeleteOnUninstall(endWorld);
    }
}