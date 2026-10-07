package dev.cosmojar.stellaritypaper.adapter.v1_21_4;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;
import dev.cosmojar.stellaritypaper.items.definitions.tools.ShulkerSpearItemAdapter1_21_4;
import dev.cosmojar.stellaritypaper.mobs.EndMobVariantServiceAdapter1_21_4;
import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManagerAdapter1_21_4;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Версионный адаптер для Paper 1.21.4.
 * Делегирует вызовы
 * Инициализация адаптеров компонентов сделана ленивой.
 */
public class VersionAdapter1_21_4 implements VersionAdapter {

    private ShulkerSpearItemAdapter1_21_4 spearAdapter;
    private EndMobVariantServiceAdapter1_21_4 mobAdapter;
    private EndIslandManagerAdapter1_21_4 endIslandAdapter;

    private ShulkerSpearItemAdapter1_21_4 spearAdapter() {
        if (spearAdapter == null) spearAdapter = new ShulkerSpearItemAdapter1_21_4();
        return spearAdapter;
    }

    private EndMobVariantServiceAdapter1_21_4 mobAdapter() {
        if (mobAdapter == null) mobAdapter = new EndMobVariantServiceAdapter1_21_4();
        return mobAdapter;
    }

    private EndIslandManagerAdapter1_21_4 endIslandAdapter() {
        if (endIslandAdapter == null) endIslandAdapter = new EndIslandManagerAdapter1_21_4();
        return endIslandAdapter;
    }

    @Override
    public boolean supports(ServerVersion version) {
        return version == ServerVersion.V1_21_4;
    }

    @Override
    public String getAdapterName() {
        return "Paper-1.21.4-Adapter";
    }

    @Override
    public Material getShulkerSpearMaterial() {
        return spearAdapter().getFallbackMaterial();
    }

    @Override
    public void applyNativeEndVariant(Entity entity) {
        mobAdapter().applyNativeEndVariant(entity);
    }

    @Override
    public Boolean getSendCommandFeedback(World world) {
        return endIslandAdapter().getSendCommandFeedback(world);
    }

    private dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationRuleRegistryAdapter1_21_4 consecrationAdapter;

    private dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationRuleRegistryAdapter1_21_4 consecrationAdapter() {
        if (consecrationAdapter == null) consecrationAdapter = new dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationRuleRegistryAdapter1_21_4();
        return consecrationAdapter;
    }

    @Override
    public void registerConsecrationRules(Object registry) {
        if (registry != null) {
            consecrationAdapter().applyRules(registry);
        }
    }

    @Override
    public void setSendCommandFeedback(World world, Boolean value) {
        endIslandAdapter().setSendCommandFeedback(world, value);
    }

    private dev.cosmojar.stellaritypaper.mechanics.uninstall.UninstallAdapter1_21_4 uninstallAdapter;

    private dev.cosmojar.stellaritypaper.mechanics.uninstall.UninstallAdapter1_21_4 uninstallAdapter() {
        if (uninstallAdapter == null) uninstallAdapter = new dev.cosmojar.stellaritypaper.mechanics.uninstall.UninstallAdapter1_21_4();
        return uninstallAdapter;
    }

    @Override
    public void fixLevelDat(java.io.File levelDatFile) throws Exception {
        uninstallAdapter().fixLevelDat(levelDatFile);
    }

    @Override
    public boolean cleanChunkTag(Object chunkTag) {
        return uninstallAdapter().cleanChunkTag(chunkTag);
    }

    @Override
    public void bindDragonToFight(World world, org.bukkit.entity.EnderDragon dragon) {
        endIslandAdapter().bindDragonToFight(world, dragon);
    }

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_4 voidPendantAdapter;

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_4 voidPendantAdapter() {
        if (voidPendantAdapter == null) voidPendantAdapter = new dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_4();
        return voidPendantAdapter;
    }

    @Override
    public void registerVoidPendantEnchantments() {
        voidPendantAdapter().registerVoidPendantEnchantments();
    }

    @Override
    public boolean supportsDialogs() {
        return false;
    }

    @Override
    public boolean hasCustomDialogAdapter() {
        return true;
    }
}
