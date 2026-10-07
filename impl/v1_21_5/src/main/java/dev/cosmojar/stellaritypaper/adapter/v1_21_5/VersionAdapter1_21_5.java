package dev.cosmojar.stellaritypaper.adapter.v1_21_5;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;
import dev.cosmojar.stellaritypaper.items.definitions.tools.ShulkerSpearItemAdapter1_21_5;
import dev.cosmojar.stellaritypaper.mobs.EndMobVariantServiceAdapter1_21_5;
import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManagerAdapter1_21_5;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class VersionAdapter1_21_5 implements VersionAdapter {

    private ShulkerSpearItemAdapter1_21_5 spearAdapter;
    private EndMobVariantServiceAdapter1_21_5 mobAdapter;
    private EndIslandManagerAdapter1_21_5 endIslandAdapter;

    private ShulkerSpearItemAdapter1_21_5 spearAdapter() {
        if (spearAdapter == null) spearAdapter = new ShulkerSpearItemAdapter1_21_5();
        return spearAdapter;
    }

    private EndMobVariantServiceAdapter1_21_5 mobAdapter() {
        if (mobAdapter == null) mobAdapter = new EndMobVariantServiceAdapter1_21_5();
        return mobAdapter;
    }

    private EndIslandManagerAdapter1_21_5 endIslandAdapter() {
        if (endIslandAdapter == null) endIslandAdapter = new EndIslandManagerAdapter1_21_5();
        return endIslandAdapter;
    }

    @Override
    public boolean supports(ServerVersion version) {
        return version == ServerVersion.V1_21_5;
    }

    @Override
    public String getAdapterName() {
        return "Paper-1.21.5-Adapter";
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

    @Override
    public void setSendCommandFeedback(World world, Boolean value) {
        endIslandAdapter().setSendCommandFeedback(world, value);
    }

    @Override
    public void bindDragonToFight(World world, org.bukkit.entity.EnderDragon dragon) {
        endIslandAdapter().bindDragonToFight(world, dragon);
    }

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_5 voidPendantAdapter;

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_5 voidPendantAdapter() {
        if (voidPendantAdapter == null) voidPendantAdapter = new dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_5();
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