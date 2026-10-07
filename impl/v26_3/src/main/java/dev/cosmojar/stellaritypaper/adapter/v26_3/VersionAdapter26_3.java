package dev.cosmojar.stellaritypaper.adapter.v26_3;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;
import dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationRuleRegistry;
import dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationRuleRegistryAdapter26_3;
import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManagerAdapter26_3;
import dev.cosmojar.stellaritypaper.mechanics.tools.ShulkerSpearAdapter26_3;
import dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter26_3;
import org.bukkit.Material;
import org.bukkit.World;

public final class VersionAdapter26_3 implements VersionAdapter {

    private ConsecrationRuleRegistryAdapter26_3 consecrationAdapter;
    private EndIslandManagerAdapter26_3 endIslandAdapter;
    private ShulkerSpearAdapter26_3 spearAdapter;
    private VoidPendantNmsAdapter26_3 voidPendantAdapter;

    private ConsecrationRuleRegistryAdapter26_3 consecrationAdapter() {
        if (consecrationAdapter == null) consecrationAdapter = new ConsecrationRuleRegistryAdapter26_3();
        return consecrationAdapter;
    }

    private EndIslandManagerAdapter26_3 endIslandAdapter() {
        if (endIslandAdapter == null) endIslandAdapter = new EndIslandManagerAdapter26_3();
        return endIslandAdapter;
    }

    private ShulkerSpearAdapter26_3 spearAdapter() {
        if (spearAdapter == null) spearAdapter = new ShulkerSpearAdapter26_3();
        return spearAdapter;
    }

    private VoidPendantNmsAdapter26_3 voidPendantAdapter() {
        if (voidPendantAdapter == null) voidPendantAdapter = new VoidPendantNmsAdapter26_3();
        return voidPendantAdapter;
    }

    @Override
    public boolean supports(final ServerVersion version) {
        return version == ServerVersion.V26_3;
    }

    @Override
    public String getAdapterName() {
        return "Paper-26.3-Adapter";
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

    @Override
    public void registerVoidPendantEnchantments() {
        voidPendantAdapter().registerVoidPendantEnchantments();
    }

    @Override
    public void bindDragonToFight(World world, org.bukkit.entity.EnderDragon dragon) {
        endIslandAdapter().bindDragonToFight(world, dragon);
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
