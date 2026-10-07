package dev.cosmojar.stellaritypaper.items;

import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.enchants.EnchantItemService;
import dev.cosmojar.stellaritypaper.items.api.ItemComponents;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionEffectSpec;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionSpec;
import dev.cosmojar.stellaritypaper.registry.MinecraftRegistryService;
import dev.cosmojar.stellaritypaper.text.TextService;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

public final class CustomItemFactory {

    private final ItemStateRepository itemStateRepository;
    private final TextService textService;
    private final ItemLoreBuilder loreBuilder;
    private final MinecraftRegistryService registryService;
    private final EnchantItemService enchantItemService;

    public CustomItemFactory(
            final ItemStateRepository itemStateRepository,
            final TextService textService,
            final ItemLoreBuilder loreBuilder,
            final MinecraftRegistryService registryService,
            final EnchantItemService enchantItemService
    ) {
        this.itemStateRepository = itemStateRepository;
        this.textService = textService;
        this.loreBuilder = loreBuilder;
        this.registryService = registryService;
        this.enchantItemService = enchantItemService;
    }

    public ItemStack create(final CustomItemDefinition definition) {
        return create(definition, null);
    }

    public ItemStack createAlwaysLore(final CustomItemDefinition definition) {
        return createAlwaysLore(definition, null);
    }

    public ItemStack createAlwaysLore(final CustomItemDefinition definition, final String customMaterialName) {
        final ItemStack item = create(definition, customMaterialName);
        enchantItemService.rebuildNowAlways(item);
        return item;
    }

    public ItemStack create(final CustomItemDefinition definition, final String customMaterialName) {
        org.bukkit.Material material = definition.itemDefinition().material();
        if (customMaterialName != null && definition.itemDefinition().category().equalsIgnoreCase("armor")) {
            material = getArmorMaterial(material, customMaterialName);
        }
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        applyComponents(item, meta, definition.itemDefinition().components());

        Component translatedName = textService.tr(definition.itemText().nameTranslateKey())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        final String nameColorHex = definition.itemText().nameColor();
        if (nameColorHex != null && !nameColorHex.isBlank()) {
            final net.kyori.adventure.text.format.TextColor color = net.kyori.adventure.text.format.TextColor.fromHexString(nameColorHex);
            if (color != null) {
                translatedName = translatedName.color(color);
            }
        }
        meta.itemName(translatedName);

        meta.lore(loreBuilder.buildBase(definition));

        if (meta instanceof PotionMeta potionMeta) {
            potionMeta.customName(translatedName);
        }

        final String itemModelKey = definition.itemDefinition().itemModelKey();
        if (itemModelKey != null && !itemModelKey.isBlank()) {
            final NamespacedKey modelKey = NamespacedKey.fromString(itemModelKey);
            if (modelKey != null) {
                meta.setItemModel(modelKey);
            }
        }

        final String pdcItemId = definition.itemDefinition().pdcItemId();
        if ("starless_scythe".equalsIgnoreCase(pdcItemId)) {
            applyScytheToolComponent(meta);
        } else if ("music_disc_fires_of_hokkai".equalsIgnoreCase(pdcItemId)) {
            applyJukeboxPlayable(meta, "stellarity:music_disc.fires_of_hokkai");
        } else if ("music_disc_deviants_light_music_box".equalsIgnoreCase(pdcItemId)) {
            applyJukeboxPlayable(meta, "stellarity:music_disc.deviants_light_music_box");
        } else if ("music_disc_precipice_stereo".equalsIgnoreCase(pdcItemId)) {
            applyJukeboxPlayable(meta, "stellarity:music_disc.precipice_stereo");
        } else if ("endonomicon".equalsIgnoreCase(pdcItemId)) {
            meta.setRarity(org.bukkit.inventory.ItemRarity.EPIC);
            meta.setEnchantmentGlintOverride(true);
        } else if ("flavors_of_the_void".equalsIgnoreCase(pdcItemId)) {
            meta.setRarity(org.bukkit.inventory.ItemRarity.RARE);
            meta.setEnchantmentGlintOverride(true);
            if (meta instanceof org.bukkit.inventory.meta.BookMeta bookMeta) {
                dev.cosmojar.stellaritypaper.items.books.FlavorsOfTheVoidBookPages.apply(bookMeta);
            }
        } else if ("radiant_jewel".equalsIgnoreCase(pdcItemId)) {
            meta.setRarity(org.bukkit.inventory.ItemRarity.EPIC);
            meta.setEnchantmentGlintOverride(true);
            meta.setMaxStackSize(1);
        } else if ("loaf_of_plenty".equalsIgnoreCase(pdcItemId)) {
            meta.setRarity(org.bukkit.inventory.ItemRarity.EPIC);
            meta.setMaxStackSize(1);
            meta.getPersistentDataContainer().set(new NamespacedKey("stellarity", "loaf_step"), org.bukkit.persistence.PersistentDataType.INTEGER, 1);
            meta.lore(loreBuilder.buildLoafOfPlentyLore(1));
        } else if ("copper_elektra_shield".equalsIgnoreCase(pdcItemId) || "crest_of_the_end".equalsIgnoreCase(pdcItemId) || "starstruck_shield".equalsIgnoreCase(pdcItemId)) {
            applyShieldPatterns(meta, pdcItemId);
        }

        if (meta instanceof org.bukkit.inventory.meta.SuspiciousStewMeta stewMeta) {
            if ("food_suspicious_stew_absorption".equalsIgnoreCase(pdcItemId)) {
                stewMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 180, 0), true);
            } else if ("food_suspicious_stew_levitation".equalsIgnoreCase(pdcItemId)) {
                stewMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION, 160, 0), true);
            } else if ("food_suspicious_stew_strength".equalsIgnoreCase(pdcItemId)) {
                stewMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 220, 0), true);
            }
        }

        item.setItemMeta(meta);
        meta.itemName(translatedName);
        applyFoodAndConsumable(item, definition.itemDefinition().components());
        itemStateRepository.setItemId(item, pdcItemId);

        enchantItemService.ensureUpToDate(item);
        return item;
    }

    private org.bukkit.Material getArmorMaterial(final org.bukkit.Material baseMaterial, final String materialName) {
        if (baseMaterial == null || materialName == null) {
            return baseMaterial;
        }
        final String name = materialName.toLowerCase(Locale.ROOT);
        final String prefix;
        switch (name) {
            case "chainmail":
            case "chain":
                prefix = "CHAINMAIL";
                break;
            case "iron":
                prefix = "IRON";
                break;
            case "gold":
            case "golden":
                prefix = "GOLDEN";
                break;
            case "diamond":
                prefix = "DIAMOND";
                break;
            case "netherite":
                prefix = "NETHERITE";
                break;
            default:
                return baseMaterial;
        }

        final String baseName = baseMaterial.name();
        if (baseName.endsWith("_BOOTS")) {
            return org.bukkit.Material.valueOf(prefix + "_BOOTS");
        } else if (baseName.endsWith("_LEGGINGS")) {
            return org.bukkit.Material.valueOf(prefix + "_LEGGINGS");
        } else if (baseName.endsWith("_CHESTPLATE")) {
            return org.bukkit.Material.valueOf(prefix + "_CHESTPLATE");
        } else if (baseName.endsWith("_HELMET")) {
            return org.bukkit.Material.valueOf(prefix + "_HELMET");
        }
        return baseMaterial;
    }

    private void applyComponents(final ItemStack item, final ItemMeta meta, final ItemComponents components) {
        applyPotion(components.potion(), meta);
        applyEquippable(components.equippable(), meta);
        applyItemFlags(components, meta);
        applyDyedColor(item, components.dyedColor());
        applyMaxDamage(meta, components.maxDamage());
        applyUnbreakable(meta, components.unbreakable());
        applyFireResistant(meta, components.fireResistant());
        applyUseCooldown(meta, components.useCooldownSeconds());
    }


    private void applyUseCooldown(final ItemMeta meta, final Float useCooldownSeconds) {
        if (useCooldownSeconds == null) {
            return;
        }
        final org.bukkit.inventory.meta.components.UseCooldownComponent cooldown = meta.getUseCooldown();
        cooldown.setCooldownSeconds(useCooldownSeconds);
        meta.setUseCooldown(cooldown);
    }

    private void applyFoodAndConsumable(final ItemStack item, final ItemComponents components) {
        applyFood(item, components.foodNutrition(), components.foodSaturation(), components.foodCanAlwaysEat());
        applyConsumable(item, components.consumableSeconds());
    }

    private void applyFood(final ItemStack item, final Integer nutrition, final Float saturation, final Boolean canAlwaysEat) {
        if (nutrition == null && saturation == null && canAlwaysEat == null) {
            return;
        }
        io.papermc.paper.datacomponent.item.FoodProperties food = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.FOOD);
        final io.papermc.paper.datacomponent.item.FoodProperties.Builder builder = io.papermc.paper.datacomponent.item.FoodProperties.food();
        if (food != null) {
            builder.nutrition(nutrition != null ? nutrition : food.nutrition());
            builder.saturation(saturation != null ? saturation : food.saturation());
            builder.canAlwaysEat(canAlwaysEat != null ? canAlwaysEat : food.canAlwaysEat());
        } else {
            builder.nutrition(nutrition != null ? nutrition : 0);
            builder.saturation(saturation != null ? saturation : 0.0F);
            builder.canAlwaysEat(canAlwaysEat != null ? canAlwaysEat : false);
        }
        item.setData(io.papermc.paper.datacomponent.DataComponentTypes.FOOD, builder.build());
    }

    private void applyConsumable(final ItemStack item, final Float consumeSeconds) {
        if (consumeSeconds == null) {
            return;
        }
        io.papermc.paper.datacomponent.item.Consumable consumable = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.CONSUMABLE);
        final io.papermc.paper.datacomponent.item.Consumable.Builder builder = io.papermc.paper.datacomponent.item.Consumable.consumable();
        if (consumable != null) {
            builder.animation(consumable.animation());
            builder.sound(consumable.sound());
            builder.hasConsumeParticles(consumable.hasConsumeParticles());
        }
        builder.consumeSeconds(consumeSeconds);
        item.setData(io.papermc.paper.datacomponent.DataComponentTypes.CONSUMABLE, builder.build());
    }


    private void applyMaxDamage(final ItemMeta meta, final Integer maxDamage) {
        if (maxDamage == null) {
            return;
        }
        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            damageable.setMaxDamage(maxDamage);
        }
    }

    private void applyUnbreakable(final ItemMeta meta, final Boolean unbreakable) {
        if (unbreakable == null) {
            return;
        }
        meta.setUnbreakable(unbreakable);
    }

    private void applyFireResistant(final ItemMeta meta, final Boolean fireResistant) {
        if (fireResistant == null) {
            return;
        }
        meta.setFireResistant(fireResistant);

    }



    private void applyDyedColor(final ItemStack item, final Integer dyedColor) {
        if (dyedColor == null) {
            return;
        }
        try {
            item.setData(io.papermc.paper.datacomponent.DataComponentTypes.DYED_COLOR,
                    io.papermc.paper.datacomponent.item.DyedItemColor.dyedItemColor(org.bukkit.Color.fromRGB(dyedColor)));
        } catch (final Throwable ignored) {
            final ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(org.bukkit.Color.fromRGB(dyedColor));
                item.setItemMeta(leatherMeta);
            }
        }
    }

    private void applyPotion(final PotionSpec potion, final ItemMeta meta) {
        if (!(meta instanceof PotionMeta potionMeta) || potion == null) {
            return;
        }

        if (potion.basePotionKey() != null && !potion.basePotionKey().isBlank()) {
            final NamespacedKey basePotionKey = NamespacedKey.fromString(potion.basePotionKey());
            if (basePotionKey != null) {
                registryService.find(Registry.POTION, basePotionKey).ifPresent(potionMeta::setBasePotionType);
            }
        }

        if (potion.colorRgb() != null) {
            potionMeta.setColor(Color.fromRGB(potion.colorRgb()));
        }

        for (final PotionEffectSpec effectData : potion.effects()) {
            final NamespacedKey effectKey = NamespacedKey.fromString(effectData.effectKey());
            if (effectKey == null) {
                continue;
            }

            final PotionEffectType effectType = registryService.find(Registry.EFFECT, effectKey).orElse(null);
            if (effectType == null) {
                continue;
            }

            final int duration = effectData.durationTicks() < 0
                    ? PotionEffect.INFINITE_DURATION
                    : effectData.durationTicks();
            final PotionEffect effect = new PotionEffect(
                    effectType,
                    duration,
                    effectData.amplifier(),
                    effectData.ambient(),
                    effectData.showParticles(),
                    effectData.showIcon()
            );
            potionMeta.addCustomEffect(effect, true);
        }
    }

    private void applyEquippable(final EquippableSpec equippableData, final ItemMeta meta) {
        if (equippableData == null) {
            return;
        }

        final EquippableComponent equippable = meta.getEquippable();
        if (equippable == null) {
            return;
        }

        if (equippableData.slot() != null && !equippableData.slot().isBlank()) {
            final EquipmentSlot slot = parseEquipmentSlot(equippableData.slot());
            if (slot != null) {
                equippable.setSlot(slot);
            }
        }

        if (equippableData.modelKey() != null && !equippableData.modelKey().isBlank()) {
            final NamespacedKey modelKey = NamespacedKey.fromString(equippableData.modelKey());
            if (modelKey != null) {
                equippable.setModel(modelKey);
            }
        }

        if (equippableData.equipSoundKey() != null && !equippableData.equipSoundKey().isBlank()) {
            final NamespacedKey soundKey = NamespacedKey.fromString(equippableData.equipSoundKey());
            if (soundKey != null) {
                final Sound equipSound = registryService.find(Registry.SOUNDS, soundKey).orElse(null);
                if (equipSound != null) {
                    equippable.setEquipSound(equipSound);
                }
            }
        }

        if (equippableData.damageOnHurt() != null) {
            equippable.setDamageOnHurt(equippableData.damageOnHurt());
        }
        meta.setEquippable(equippable);
    }

    private void applyItemFlags(final ItemComponents components, final ItemMeta meta) {
        for (final ItemFlag itemFlag : components.itemFlags()) {
            meta.addItemFlags(itemFlag);
        }
    }

    private EquipmentSlot parseEquipmentSlot(final String slotRaw) {
        final String normalized = slotRaw.toUpperCase(Locale.ROOT).replace("-", "_");
        try {
            return EquipmentSlot.valueOf(normalized);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    private void applyScytheToolComponent(final ItemMeta meta) {
        final org.bukkit.inventory.meta.components.ToolComponent tool = meta.getTool();
        tool.addRule(org.bukkit.Material.COBWEB, 15.0F, true);
        tool.addRule(org.bukkit.Tag.LEAVES.getValues(), 6.0F, true);
        tool.addRule(org.bukkit.Tag.WOOL.getValues(), 5.0F, true);
        meta.setTool(tool);
    }

    private void applyJukeboxPlayable(final ItemMeta meta, final String songKey) {
        final org.bukkit.inventory.meta.components.JukeboxPlayableComponent jukebox = meta.getJukeboxPlayable();
        jukebox.setSongKey(org.bukkit.NamespacedKey.fromString(songKey));
        meta.setJukeboxPlayable(jukebox);
    }

    private void applyShieldPatterns(final ItemMeta meta, final String itemId) {
        if (!(meta instanceof org.bukkit.inventory.meta.ShieldMeta shieldMeta)) {
            return;
        }

        final java.util.List<org.bukkit.block.banner.Pattern> patterns = new java.util.ArrayList<>();

        if ("copper_elektra_shield".equalsIgnoreCase(itemId)) {
            shieldMeta.setBaseColor(org.bukkit.DyeColor.ORANGE);
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.YELLOW, org.bukkit.block.banner.PatternType.CROSS));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.ORANGE, org.bukkit.block.banner.PatternType.GRADIENT));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.ORANGE, org.bukkit.block.banner.PatternType.GRADIENT_UP));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.YELLOW, org.bukkit.block.banner.PatternType.BORDER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.ORANGE, org.bukkit.block.banner.PatternType.BORDER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.YELLOW, org.bukkit.block.banner.PatternType.FLOWER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.ORANGE, org.bukkit.block.banner.PatternType.FLOWER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.YELLOW, org.bukkit.block.banner.PatternType.CURLY_BORDER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.ORANGE, org.bukkit.block.banner.PatternType.CURLY_BORDER));
        } else if ("crest_of_the_end".equalsIgnoreCase(itemId)) {
            shieldMeta.setBaseColor(org.bukkit.DyeColor.PURPLE);
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.MAGENTA, org.bukkit.block.banner.PatternType.GRADIENT));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.BLACK, org.bukkit.block.banner.PatternType.TRIANGLE_TOP));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.BLACK, org.bukkit.block.banner.PatternType.TRIANGLE_BOTTOM));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.PURPLE, org.bukkit.block.banner.PatternType.CROSS));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.BLACK, org.bukkit.block.banner.PatternType.CURLY_BORDER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.PURPLE, org.bukkit.block.banner.PatternType.FLOWER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.BLACK, org.bukkit.block.banner.PatternType.FLOWER));
        } else if ("starstruck_shield".equalsIgnoreCase(itemId)) {
            shieldMeta.setBaseColor(org.bukkit.DyeColor.BLACK);
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.ORANGE, org.bukkit.block.banner.PatternType.GRADIENT_UP));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.ORANGE, org.bukkit.block.banner.PatternType.CURLY_BORDER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.YELLOW, org.bukkit.block.banner.PatternType.CURLY_BORDER));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.ORANGE, org.bukkit.block.banner.PatternType.RHOMBUS));
            patterns.add(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.YELLOW, org.bukkit.block.banner.PatternType.RHOMBUS));
        }

        shieldMeta.setPatterns(patterns);
    }
}
