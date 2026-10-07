package dev.cosmojar.stellaritypaper.mobs;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionManager;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.items.CustomItemMatcher;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.mobs.variants.MobVariant;
import dev.cosmojar.stellaritypaper.registry.MinecraftRegistryService;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class EndMobVariantService implements Listener {

    private void applyNativeEndVariant(final Entity entity) {
        if (ServerVersion.getCurrent() != ServerVersion.V1_21_11 && ServerVersion.getCurrent() != ServerVersion.UNKNOWN) {
            VersionManager.getAdapter().applyNativeEndVariant(entity);
            return;
        }
        V1_21_11_Handler.apply(entity);
    }

    private static class V1_21_11_Handler {
        private static final Identifier END_VARIANT_LOC = Identifier.fromNamespaceAndPath("stellarity", "end");

        private static void apply(final Entity entity) {
            try {
                if (!(entity instanceof CraftEntity craftEntity)) {
                    return;
                }
                final net.minecraft.world.entity.Entity nms = craftEntity.getHandle();
                if (!(nms.level() instanceof ServerLevel level)) {
                    return;
                }

                if (nms instanceof net.minecraft.world.entity.animal.cow.Cow cow) {
                    final var registry = level.registryAccess().lookupOrThrow(Registries.COW_VARIANT);
                    final var key = ResourceKey.create(Registries.COW_VARIANT, END_VARIANT_LOC);
                    registry.get(key).ifPresent(cow::setVariant);
                } else if (nms instanceof net.minecraft.world.entity.animal.chicken.Chicken chicken) {
                    final var registry = level.registryAccess().lookupOrThrow(Registries.CHICKEN_VARIANT);
                    final var key = ResourceKey.create(Registries.CHICKEN_VARIANT, END_VARIANT_LOC);
                    registry.get(key).ifPresent(chicken::setVariant);
                } else if (nms instanceof net.minecraft.world.entity.animal.pig.Pig pig) {
                    final var registry = level.registryAccess().lookupOrThrow(Registries.PIG_VARIANT);
                    final var key = ResourceKey.create(Registries.PIG_VARIANT, END_VARIANT_LOC);
                    registry.get(key).ifPresent(pig::setVariant);
                } else if (nms instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf) {
                    final var registry = level.registryAccess().lookupOrThrow(Registries.WOLF_VARIANT);
                    final var key = ResourceKey.create(Registries.WOLF_VARIANT, END_VARIANT_LOC);
                    registry.get(key).ifPresent(wolf::setVariant);
                } else if (nms instanceof net.minecraft.world.entity.animal.feline.Cat cat) {
                    final var registry = level.registryAccess().lookupOrThrow(Registries.CAT_VARIANT);
                    final var key = ResourceKey.create(Registries.CAT_VARIANT, END_VARIANT_LOC);
                    registry.get(key).ifPresent(cat::setVariant);
                }
            } catch (final Throwable ignored) {
            }
        }
    }

    private final StellarityPaperPlugin plugin;
    private final MinecraftRegistryService registryService;
    private final CustomItemMatcher itemMatcher;
    private final CustomItemFactory itemFactory;
    private final ItemDefinitionRegistry itemRegistry;

    public EndMobVariantService(
            final StellarityPaperPlugin plugin,
            final MinecraftRegistryService registryService,
            final CustomItemMatcher itemMatcher,
            final CustomItemFactory itemFactory,
            final ItemDefinitionRegistry itemRegistry
    ) {
        this.plugin = plugin;
        this.registryService = registryService;
        this.itemMatcher = itemMatcher;
        this.itemFactory = itemFactory;
        this.itemRegistry = itemRegistry;
    }

    public void clearVariant(final Entity entity) {
        if (entity == null) {
            return;
        }
        entity.getPersistentDataContainer().remove(MobVariant.MOB_TYPE_KEY);
        for (final MobVariant v : MobVariant.values()) {
            entity.removeScoreboardTag(v.getScoreboardTag());
        }
    }

    public boolean setVariant(final Entity entity, final MobVariant variant) {
        if (entity == null || variant == null) {
            return false;
        }
        if (entity.getType() != variant.getTargetType()) {
            return false;
        }

        clearVariant(entity);

        entity.getPersistentDataContainer().set(MobVariant.MOB_TYPE_KEY, PersistentDataType.STRING, variant.getPdcId());
        entity.addScoreboardTag(variant.getScoreboardTag());

        applyNativeEndVariant(entity);

        applyMobAttributesAndEquipment(entity, variant);
        return true;
    }

    public Optional<MobVariant> getVariant(final Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        final String pdcValue = entity.getPersistentDataContainer().get(MobVariant.MOB_TYPE_KEY, PersistentDataType.STRING);
        return MobVariant.findByPdcId(pdcValue);
    }

    public void syncScoreboardTag(final Entity entity) {
        if (entity == null) {
            return;
        }
        final Optional<MobVariant> variant = getVariant(entity);
        for (final MobVariant v : MobVariant.values()) {
            entity.removeScoreboardTag(v.getScoreboardTag());
        }
        variant.ifPresent(v -> {
            entity.addScoreboardTag(v.getScoreboardTag());
            applyNativeEndVariant(entity);
        });
    }



    private void applyMobAttributesAndEquipment(final Entity entity, final MobVariant variant) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        switch (variant) {
            case VOIDED_ZOMBIE:
                addBonusAttribute(living, Attribute.MAX_HEALTH, 8.0);
                addBonusAttribute(living, Attribute.ATTACK_DAMAGE, 3.0);
                addBonusAttribute(living, Attribute.ARMOR, 3.0);
                addBonusAttribute(living, Attribute.ARMOR_TOUGHNESS, 2.0);
                addBonusAttribute(living, Attribute.KNOCKBACK_RESISTANCE, 0.15);
                living.setHealth(getMaxHealth(living));
                break;

            case VOIDED_SKELETON:
                addBonusAttribute(living, Attribute.MAX_HEALTH, 4.0);
                addBonusAttribute(living, Attribute.ARMOR, 4.0);
                addBonusAttribute(living, Attribute.ARMOR_TOUGHNESS, 1.0);
                addBonusAttribute(living, Attribute.MOVEMENT_SPEED, 0.03);
                living.setHealth(getMaxHealth(living));
                
                itemRegistry.findByCategoryAndName("weapons", "call_of_the_void")
                        .ifPresentOrElse(
                                def -> living.getEquipment().setItemInMainHand(itemFactory.create(def)),
                                () -> living.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD))
                        );
                living.getEquipment().setItemInMainHandDropChance(0.05f);
                break;

            case VOIDED_SLIME:
                if (living instanceof Slime slime) {
                    slime.setSize(1);
                }
                setBaseAttribute(living, Attribute.MAX_HEALTH, 35.0);
                addBonusAttribute(living, Attribute.ARMOR, 10.0);
                addBonusAttribute(living, Attribute.ARMOR_TOUGHNESS, 4.0);
                addBonusAttribute(living, Attribute.KNOCKBACK_RESISTANCE, 0.3);
                living.setHealth(getMaxHealth(living));
                living.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 0, false, false));
                break;

            case FLESH_PIGLIN:
                living.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                addBonusAttribute(living, Attribute.ATTACK_DAMAGE, 3.0);
                addBonusAttribute(living, Attribute.ARMOR, 4.0);
                addBonusAttribute(living, Attribute.KNOCKBACK_RESISTANCE, 0.15);
                addBonusAttribute(living, Attribute.MOVEMENT_SPEED, -0.02);
                if (living instanceof Piglin piglin) {
                    piglin.setBaby(false);
                    piglin.setIsAbleToHunt(true);
                }
                break;

            case VOIDED_SILVERFISH:
                addBonusAttribute(living, Attribute.MAX_HEALTH, 10.0);
                addBonusAttribute(living, Attribute.ATTACK_DAMAGE, 2.0);
                addBonusAttribute(living, Attribute.SCALE, 0.8);
                living.setHealth(getMaxHealth(living));
                break;

            default:
                break;
        }
    }

    private void addBonusAttribute(final LivingEntity entity, final Attribute attribute, final double amount) {
        final AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(instance.getBaseValue() + amount);
        }
    }

    private void setBaseAttribute(final LivingEntity entity, final Attribute attribute, final double value) {
        final AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private double getMaxHealth(final LivingEntity entity) {
        final AttributeInstance instance = entity.getAttribute(Attribute.MAX_HEALTH);
        return instance != null ? instance.getValue() : entity.getHealth();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntitiesLoad(final EntitiesLoadEvent event) {
        for (final Entity entity : event.getEntities()) {
            if (entity.getPersistentDataContainer().has(MobVariant.MOB_TYPE_KEY, PersistentDataType.STRING)) {
                syncScoreboardTag(entity);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handleSpawn(final CreatureSpawnEvent event) {
        final LivingEntity entity = event.getEntity();
        if (event.getLocation().getWorld() == null || !isEndWorld(event.getLocation().getWorld())) {
            return;
        }

        if (entity instanceof Sheep sheep) {
            sheep.setColor(DyeColor.BLACK);
            return;
        }

        final MobVariant endVariant = resolveEndVariantForType(entity.getType());
        if (endVariant != null) {
            setVariant(entity, endVariant);
        }
    }

    private boolean isEndWorld(final World world) {
        if (world == null) {
            return false;
        }
        return world.getEnvironment() == World.Environment.THE_END || world.getKey().asString().equalsIgnoreCase("minecraft:the_end");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handleBreed(final EntityBreedEvent event) {
        final LivingEntity child = event.getEntity();
        final Optional<MobVariant> motherVar = getVariant(event.getMother());
        final Optional<MobVariant> fatherVar = getVariant(event.getFather());

        if (motherVar.isPresent() && fatherVar.isPresent() && motherVar.get() == motherVar.get()) {
            setVariant(child, motherVar.get());
        } else if (motherVar.isPresent() || fatherVar.isPresent()) {
            final MobVariant inherited = motherVar.orElseGet(fatherVar::get);
            if (ThreadLocalRandom.current().nextBoolean()) {
                setVariant(child, inherited);
            }
        } else if (isEndWorld(child.getWorld())) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                final MobVariant endVar = resolveEndVariantForType(child.getType());
                if (endVar != null) {
                    setVariant(child, endVar);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnderEggThrow(final PlayerEggThrowEvent event) {
        final ItemStack thrown = event.getEgg().getItem();
        if (itemMatcher.resolveRawId(thrown).orElse("").equalsIgnoreCase("ender_egg")) {
            event.setHatching(true);
            event.setNumHatches((byte) 1);
            event.setHatchingType(EntityType.CHICKEN);
            
            event.getEgg().getScheduler().run(
                    plugin,
                    task -> {
                        for (final Entity nearby : event.getEgg().getNearbyEntities(2.0, 2.0, 2.0)) {
                            if (nearby instanceof Chicken chicken && !getVariant(chicken).isPresent()) {
                                setVariant(chicken, MobVariant.ENDER_CHICKEN);
                                break;
                            }
                        }
                    },
                    null
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChickenLayEgg(final EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof Chicken chicken)) {
            return;
        }
        final Optional<MobVariant> variant = getVariant(chicken);
        if (variant.isPresent() && variant.get() == MobVariant.ENDER_CHICKEN) {
            final Item itemDrop = event.getItemDrop();
            if (itemDrop.getItemStack().getType() == Material.EGG) {
                itemRegistry.findByCategoryAndName("misc", "ender_egg").ifPresent(def -> {
                    final ItemStack enderEgg = itemFactory.create(def);
                    itemDrop.setItemStack(enderEgg);
                });
            }
        }
    }

    private MobVariant resolveEndVariantForType(final EntityType type) {
        switch (type) {
            case CHICKEN:
                return MobVariant.ENDER_CHICKEN;
            case COW:
                return MobVariant.ENDER_COW;
            case PIG:
                return MobVariant.ENDER_PIG;
            case CAT:
                return MobVariant.ENDER_CAT;
            case WOLF:
                return MobVariant.ENDER_WOLF;
            case ZOMBIE:
                return MobVariant.VOIDED_ZOMBIE;
            case SKELETON:
                return MobVariant.VOIDED_SKELETON;
            case SLIME:
                return MobVariant.VOIDED_SLIME;
            case ZOMBIFIED_PIGLIN:
            case PIGLIN:
                return MobVariant.FLESH_PIGLIN;
            case SILVERFISH:
                return MobVariant.VOIDED_SILVERFISH;
            default:
                return null;
        }
    }
}
