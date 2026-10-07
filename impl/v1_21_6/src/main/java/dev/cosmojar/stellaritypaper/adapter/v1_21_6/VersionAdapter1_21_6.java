package dev.cosmojar.stellaritypaper.adapter.v1_21_6;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;
import dev.cosmojar.stellaritypaper.items.definitions.tools.ShulkerSpearItemAdapter1_21_6;
import dev.cosmojar.stellaritypaper.mobs.EndMobVariantServiceAdapter1_21_6;
import dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManagerAdapter1_21_6;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class VersionAdapter1_21_6 implements VersionAdapter {

    private ShulkerSpearItemAdapter1_21_6 spearAdapter;
    private EndMobVariantServiceAdapter1_21_6 mobAdapter;
    private EndIslandManagerAdapter1_21_6 endIslandAdapter;

    private ShulkerSpearItemAdapter1_21_6 spearAdapter() {
        if (spearAdapter == null) spearAdapter = new ShulkerSpearItemAdapter1_21_6();
        return spearAdapter;
    }

    private EndMobVariantServiceAdapter1_21_6 mobAdapter() {
        if (mobAdapter == null) mobAdapter = new EndMobVariantServiceAdapter1_21_6();
        return mobAdapter;
    }

    private EndIslandManagerAdapter1_21_6 endIslandAdapter() {
        if (endIslandAdapter == null) endIslandAdapter = new EndIslandManagerAdapter1_21_6();
        return endIslandAdapter;
    }

    @Override
    public boolean supports(ServerVersion version) {
        return version == ServerVersion.V1_21_6;
    }

    @Override
    public String getAdapterName() {
        return "Paper-1.21.6-Adapter";
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

    private dev.cosmojar.stellaritypaper.mechanics.dialog.EndonomiconDialogAdapter1_21_6 dialogAdapter;

    private dev.cosmojar.stellaritypaper.mechanics.dialog.EndonomiconDialogAdapter1_21_6 dialogAdapter() {
        if (dialogAdapter == null) dialogAdapter = new dev.cosmojar.stellaritypaper.mechanics.dialog.EndonomiconDialogAdapter1_21_6();
        return dialogAdapter;
    }

    @Override
    public boolean hasCustomDialogAdapter() {
        return true;
    }

    @Override
    public boolean openEndonomiconMainMenu(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, int favCount, int recCount) {
        dialogAdapter().openMainMenu(player, service, plugin, favCount, recCount);
        return true;
    }

    @Override
    public boolean openEndonomiconRecipeDetail(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, Object vm, boolean isFavorite) {
        dialogAdapter().openRecipeDetail(player, service, plugin, vm, isFavorite);
        return true;
    }

    @Override
    public boolean openEndonomiconCategoryView(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, String category, Object matchesList, int page, int totalPages) {
        dialogAdapter().openCategoryView(player, service, plugin, category, matchesList, page, totalPages);
        return true;
    }

    @Override
    public boolean openEndonomiconFavoritesView(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, Object matchesList, int page, int totalPages) {
        dialogAdapter().openFavoritesView(player, service, plugin, matchesList, page, totalPages);
        return true;
    }

    @Override
    public boolean openEndonomiconRecentView(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, Object matchesList, int page, int totalPages) {
        dialogAdapter().openRecentView(player, service, plugin, matchesList, page, totalPages);
        return true;
    }

    @Override
    public boolean openEndonomiconSearchResults(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin, Object searchResult, int page, int totalPages) {
        dialogAdapter().openSearchResults(player, service, plugin, searchResult, page, totalPages);
        return true;
    }

    @Override
    public boolean openEndonomiconSearchPrompt(org.bukkit.entity.Player player, Object service, org.bukkit.plugin.java.JavaPlugin plugin) {
        dialogAdapter().openSearchPrompt(player, service, plugin);
        return true;
    }

    @Override
    public void bindDragonToFight(World world, org.bukkit.entity.EnderDragon dragon) {
        endIslandAdapter().bindDragonToFight(world, dragon);
    }

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_6 voidPendantAdapter;

    private dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_6 voidPendantAdapter() {
        if (voidPendantAdapter == null) voidPendantAdapter = new dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsAdapter1_21_6();
        return voidPendantAdapter;
    }

    @Override
    public void registerVoidPendantEnchantments() {
        voidPendantAdapter().registerVoidPendantEnchantments();
    }
}