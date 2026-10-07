package dev.cosmojar.stellaritypaper.adapter.v26_2;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;
import dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationRuleRegistry;
import dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationRuleRegistryAdapter26_2;
import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManagerAdapter26_2;
import dev.cosmojar.stellaritypaper.mechanics.tools.ShulkerSpearAdapter26_2;
import org.bukkit.Material;
import org.bukkit.World;

public final class VersionAdapter26_2 implements VersionAdapter {

    private ConsecrationRuleRegistryAdapter26_2 consecrationAdapter;
    private EndIslandManagerAdapter26_2 endIslandAdapter;
    private ShulkerSpearAdapter26_2 spearAdapter;

    private ConsecrationRuleRegistryAdapter26_2 consecrationAdapter() {
        if (consecrationAdapter == null) consecrationAdapter = new ConsecrationRuleRegistryAdapter26_2();
        return consecrationAdapter;
    }

    private EndIslandManagerAdapter26_2 endIslandAdapter() {
        if (endIslandAdapter == null) endIslandAdapter = new EndIslandManagerAdapter26_2();
        return endIslandAdapter;
    }

    private ShulkerSpearAdapter26_2 spearAdapter() {
        if (spearAdapter == null) spearAdapter = new ShulkerSpearAdapter26_2();
        return spearAdapter;
    }

    @Override
    public boolean supports(final ServerVersion version) {
        return version == ServerVersion.V26_2;
    }

    @Override
    public String getAdapterName() {
        return "Paper-26.2-Adapter";
    }

    @Override
    public void registerConsecrationRules(final Object registry) {
        if (registry instanceof ConsecrationRuleRegistry r) {
            consecrationAdapter().applyRules(r);
        }
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

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter26_2 voidPendantAdapter;

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter26_2 voidPendantAdapter() {
        if (voidPendantAdapter == null) voidPendantAdapter = new dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter26_2();
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