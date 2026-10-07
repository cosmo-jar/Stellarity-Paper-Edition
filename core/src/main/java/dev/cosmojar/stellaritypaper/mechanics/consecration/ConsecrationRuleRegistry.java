package dev.cosmojar.stellaritypaper.mechanics.consecration;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionManager;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public final class ConsecrationRuleRegistry {

    private final Map<String, String> simpleTransformRules = new HashMap<>();

    public ConsecrationRuleRegistry() {
        addRule(Material.PALE_OAK_WOOD, Material.CHERRY_WOOD);
        addRule(Material.PALE_OAK_LOG, Material.CHERRY_LOG);
        addRule(Material.PALE_OAK_LEAVES, Material.CHERRY_LEAVES);
        addRule(Material.PALE_OAK_SAPLING, Material.CHERRY_SAPLING);
        addRule(Material.STRIPPED_PALE_OAK_LOG, Material.STRIPPED_CHERRY_LOG);
        addRule(Material.STRIPPED_PALE_OAK_WOOD, Material.STRIPPED_CHERRY_WOOD);

        addRule(Material.OAK_WOOD, Material.CHERRY_WOOD);
        addRule(Material.DARK_OAK_WOOD, Material.CHERRY_WOOD);
        addRule(Material.SPRUCE_WOOD, Material.CHERRY_WOOD);
        addRule(Material.BIRCH_WOOD, Material.CHERRY_WOOD);
        addRule(Material.JUNGLE_WOOD, Material.CHERRY_WOOD);
        addRule(Material.ACACIA_WOOD, Material.CHERRY_WOOD);
        addRule(Material.MANGROVE_WOOD, Material.CHERRY_WOOD);

        addRule(Material.OAK_LOG, Material.CHERRY_LOG);
        addRule(Material.DARK_OAK_LOG, Material.CHERRY_LOG);
        addRule(Material.SPRUCE_LOG, Material.CHERRY_LOG);
        addRule(Material.BIRCH_LOG, Material.CHERRY_LOG);
        addRule(Material.JUNGLE_LOG, Material.CHERRY_LOG);
        addRule(Material.ACACIA_LOG, Material.CHERRY_LOG);
        addRule(Material.MANGROVE_LOG, Material.CHERRY_LOG);

        addRule(Material.OAK_LEAVES, Material.CHERRY_LEAVES);
        addRule(Material.DARK_OAK_LEAVES, Material.CHERRY_LEAVES);
        addRule(Material.SPRUCE_LEAVES, Material.CHERRY_LEAVES);
        addRule(Material.BIRCH_LEAVES, Material.CHERRY_LEAVES);
        addRule(Material.JUNGLE_LEAVES, Material.CHERRY_LEAVES);
        addRule(Material.ACACIA_LEAVES, Material.CHERRY_LEAVES);
        addRule(Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES);
        addRule(Material.AZALEA_LEAVES, Material.CHERRY_LEAVES);

        addRule(Material.OAK_SAPLING, Material.CHERRY_SAPLING);
        addRule(Material.DARK_OAK_SAPLING, Material.CHERRY_SAPLING);
        addRule(Material.SPRUCE_SAPLING, Material.CHERRY_SAPLING);
        addRule(Material.BIRCH_SAPLING, Material.CHERRY_SAPLING);
        addRule(Material.JUNGLE_SAPLING, Material.CHERRY_SAPLING);
        addRule(Material.ACACIA_SAPLING, Material.CHERRY_SAPLING);
        addRule(Material.MANGROVE_PROPAGULE, Material.CHERRY_SAPLING);

        if (ServerVersion.getCurrent() == ServerVersion.V1_21_4) {
            VersionManager.getAdapter().registerConsecrationRules(this);
        } else {
            addBiRule(Material.DEAD_BUSH, Material.BUSH);
            addBiRule(Material.PINK_PETALS, Material.WILDFLOWERS);
        }
        addRule(Material.WITHER_ROSE, Material.POPPY);
        if (VersionManager.getAdapter() != null) {
            VersionManager.getAdapter().registerConsecrationRules(this);
        }
        if (!simpleTransformRules.containsKey(Material.POINTED_DRIPSTONE.getKey().toString())) {
            addBiRule(Material.POINTED_DRIPSTONE, "minecraft:sulfur_spike");
        }
        addBiRule(Material.CHORUS_FLOWER, Material.SPORE_BLOSSOM);
        addBiRule(Material.PUMPKIN, Material.MELON);
        addBiRule(Material.PUMPKIN_SEEDS, Material.MELON_SEEDS);
        addBiRule(Material.AZALEA, Material.FLOWERING_AZALEA);
        addBiRule(Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES);

        addBiRule(Material.OBSIDIAN, Material.CRYING_OBSIDIAN);
        addBiRule(Material.INK_SAC, Material.GLOW_INK_SAC);
        addBiRule(Material.BRICK, Material.NETHER_BRICK);
        addBiRule(Material.SLIME_BLOCK, Material.HONEY_BLOCK);
        addBiRule(Material.TORCH, Material.SOUL_TORCH);
        addBiRule(Material.LANTERN, Material.SOUL_LANTERN);
        addBiRule(Material.CAMPFIRE, Material.SOUL_CAMPFIRE);
        addBiRule(Material.GLOWSTONE, Material.SHROOMLIGHT);
        addBiRule(Material.BREEZE_ROD, Material.BLAZE_ROD);
        addBiRule(Material.COMPASS, Material.CLOCK);
        addBiRule(Material.CHARCOAL, Material.COAL);
        addRule(Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_SENSOR);
        addBiRule(Material.TURTLE_SCUTE, Material.ARMADILLO_SCUTE);
        addBiRule(Material.ROTTEN_FLESH, Material.FEATHER);
        addBiRule(Material.FIRE_CHARGE, Material.WIND_CHARGE);
        addBiRule(Material.SCULK_CATALYST, Material.SCULK_SHRIEKER);

        addRule(Material.IRON_INGOT, "stellarity:hallowed_ingot");
        addRule(Material.STICKY_PISTON, Material.PISTON);
        addRule(Material.DISPENSER, Material.DROPPER);
        addRule(Material.OMINOUS_TRIAL_KEY, Material.TRIAL_KEY);

        addRule(Material.OCHRE_FROGLIGHT, Material.VERDANT_FROGLIGHT);
        addRule(Material.VERDANT_FROGLIGHT, Material.PEARLESCENT_FROGLIGHT);
        addRule(Material.PEARLESCENT_FROGLIGHT, Material.OCHRE_FROGLIGHT);

        addBiRule(Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT);
        addRule(Material.MUSIC_DISC_5, Material.MUSIC_DISC_RELIC);
        addBiRule(Material.MUSIC_DISC_CREATOR, Material.MUSIC_DISC_CREATOR_MUSIC_BOX);

        addBiRule(Material.SAND, Material.RED_SAND);
        addBiRule(Material.SANDSTONE, Material.RED_SANDSTONE);
        addBiRule(Material.CUT_SANDSTONE_SLAB, Material.CUT_RED_SANDSTONE_SLAB);
        addBiRule(Material.CHISELED_SANDSTONE, Material.CHISELED_RED_SANDSTONE);
        addBiRule(Material.CUT_SANDSTONE, Material.CUT_RED_SANDSTONE);
        addBiRule(Material.SANDSTONE_SLAB, Material.RED_SANDSTONE_SLAB);
        addBiRule(Material.SMOOTH_SANDSTONE_SLAB, Material.SMOOTH_RED_SANDSTONE_SLAB);
        addBiRule(Material.SMOOTH_SANDSTONE_STAIRS, Material.SMOOTH_RED_SANDSTONE_STAIRS);
        addBiRule(Material.SMOOTH_SANDSTONE, Material.SMOOTH_RED_SANDSTONE);
        addBiRule(Material.SANDSTONE_STAIRS, Material.RED_SANDSTONE_STAIRS);
        addBiRule(Material.SANDSTONE_WALL, Material.RED_SANDSTONE_WALL);

        addRule(Material.IRON_HELMET, Material.CHAINMAIL_HELMET);
        addRule(Material.IRON_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE);
        addRule(Material.IRON_LEGGINGS, Material.CHAINMAIL_LEGGINGS);
        addRule(Material.IRON_BOOTS, Material.CHAINMAIL_BOOTS);

        addBiRule(Material.STONE, Material.DEEPSLATE);
        addBiRule(Material.STONE_SLAB, Material.POLISHED_DEEPSLATE_SLAB);
        addBiRule(Material.STONE_STAIRS, Material.POLISHED_DEEPSLATE_STAIRS);
        addBiRule(Material.STONE_BRICKS, Material.DEEPSLATE_BRICKS);
        addBiRule(Material.STONE_BRICK_SLAB, Material.DEEPSLATE_BRICK_SLAB);
        addBiRule(Material.STONE_BRICK_STAIRS, Material.DEEPSLATE_BRICK_STAIRS);
        addBiRule(Material.STONE_BRICK_WALL, Material.DEEPSLATE_BRICK_WALL);
        addBiRule(Material.COBBLESTONE, Material.COBBLED_DEEPSLATE);
        addBiRule(Material.COBBLESTONE_SLAB, Material.COBBLED_DEEPSLATE_SLAB);
        addBiRule(Material.COBBLESTONE_STAIRS, Material.COBBLED_DEEPSLATE_STAIRS);
        addBiRule(Material.COBBLESTONE_WALL, Material.COBBLED_DEEPSLATE_WALL);
    }

    public void addRule(final Material input, final Material output) {
        if (input != null && output != null) {
            simpleTransformRules.put(input.getKey().toString(), output.getKey().toString());
        }
    }

    public void addRule(final Material input, final String outputKey) {
        if (input != null && outputKey != null) {
            final String keyB = outputKey.contains(":") ? outputKey.toLowerCase(java.util.Locale.ROOT) : "minecraft:" + outputKey.toLowerCase(java.util.Locale.ROOT);
            simpleTransformRules.put(input.getKey().toString(), keyB);
        }
    }

    public void addRule(final String inputKey, final Material output) {
        if (inputKey != null && output != null) {
            final String keyA = inputKey.contains(":") ? inputKey.toLowerCase(java.util.Locale.ROOT) : "minecraft:" + inputKey.toLowerCase(java.util.Locale.ROOT);
            simpleTransformRules.put(keyA, output.getKey().toString());
        }
    }

    public void addRule(final String inputKey, final String outputKey) {
        if (inputKey != null && outputKey != null) {
            final String keyA = inputKey.contains(":") ? inputKey.toLowerCase(java.util.Locale.ROOT) : "minecraft:" + inputKey.toLowerCase(java.util.Locale.ROOT);
            final String keyB = outputKey.contains(":") ? outputKey.toLowerCase(java.util.Locale.ROOT) : "minecraft:" + outputKey.toLowerCase(java.util.Locale.ROOT);
            simpleTransformRules.put(keyA, keyB);
        }
    }

    public void addBiRule(final Material a, final Material b) {
        if (a != null && b != null) {
            final String keyA = a.getKey().toString();
            final String keyB = b.getKey().toString();
            simpleTransformRules.put(keyA, keyB);
            simpleTransformRules.put(keyB, keyA);
        }
    }

    public void addBiRule(final Material a, final String bKey) {
        if (a != null && bKey != null) {
            final String keyA = a.getKey().toString();
            simpleTransformRules.put(keyA, bKey);
            simpleTransformRules.put(bKey, keyA);
        }
    }

    public void addBiRule(final String aKey, final String bKey) {
        if (aKey != null && bKey != null) {
            final String keyA = aKey.contains(":") ? aKey.toLowerCase(java.util.Locale.ROOT) : "minecraft:" + aKey.toLowerCase(java.util.Locale.ROOT);
            final String keyB = bKey.contains(":") ? bKey.toLowerCase(java.util.Locale.ROOT) : "minecraft:" + bKey.toLowerCase(java.util.Locale.ROOT);
            simpleTransformRules.put(keyA, keyB);
            simpleTransformRules.put(keyB, keyA);
        }
    }

    public String resolve(final String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        final String lower = input.toLowerCase(java.util.Locale.ROOT);
        String res = simpleTransformRules.get(lower);
        if (res != null) return res;

        final String key = lower.contains(":") ? lower : "minecraft:" + lower;
        res = simpleTransformRules.get(key);
        if (res != null) return res;

        if (lower.contains(":")) {
            final String raw = lower.split(":")[1];
            return simpleTransformRules.get("minecraft:" + raw);
        }
        return null;
    }
}
