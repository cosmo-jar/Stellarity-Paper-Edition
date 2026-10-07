package dev.cosmojar.stellaritypaper.bootstrap;

import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionManager;
import dev.cosmojar.stellaritypaper.api.exception.UnsupportedServerVersionException;
import dev.cosmojar.stellaritypaper.command.GiveCommand;
import dev.cosmojar.stellaritypaper.command.EnchantAdminCommand;
import dev.cosmojar.stellaritypaper.command.ReloadCommand;
import dev.cosmojar.stellaritypaper.command.StellarityCommand;
import dev.cosmojar.stellaritypaper.config.ConfigService;
import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.core.FeatureFlags;
import dev.cosmojar.stellaritypaper.core.ServiceContainer;
import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.data.PdcKeys;
import dev.cosmojar.stellaritypaper.data.PlayerStateRepository;
import dev.cosmojar.stellaritypaper.data.SessionTaskService;
import dev.cosmojar.stellaritypaper.enchants.EnchantAttributeBackendService;
import dev.cosmojar.stellaritypaper.enchants.EnchantBehaviorRegistry;
import dev.cosmojar.stellaritypaper.enchants.EnchantCombatBehaviorService;
import dev.cosmojar.stellaritypaper.enchants.EnchantDataCodec;
import dev.cosmojar.stellaritypaper.enchants.EnchantDefinitionRegistry;
import dev.cosmojar.stellaritypaper.enchants.EnchantEventBackendService;
import dev.cosmojar.stellaritypaper.enchants.EnchantActiveService;
import dev.cosmojar.stellaritypaper.enchants.GeneratedEnchantDefinitions;
import dev.cosmojar.stellaritypaper.enchants.CustomEnchantedBookFactory;
import dev.cosmojar.stellaritypaper.enchants.CustomStatusEffectService;
import dev.cosmojar.stellaritypaper.enchants.EnchantHorseBackendService;
import dev.cosmojar.stellaritypaper.enchants.EnchantHorseRuntimeListener;
import dev.cosmojar.stellaritypaper.enchants.EnchantItemService;
import dev.cosmojar.stellaritypaper.enchants.EnchantLocalizationValidator;
import dev.cosmojar.stellaritypaper.enchants.EnchantLoreService;
import dev.cosmojar.stellaritypaper.enchants.EnchantMobilityBehaviorService;
import dev.cosmojar.stellaritypaper.enchants.EnchantRuntimeListener;
import dev.cosmojar.stellaritypaper.enchants.EnchantRuntimeService;
import dev.cosmojar.stellaritypaper.enchants.EnchantTridentBehaviorService;
import dev.cosmojar.stellaritypaper.enchants.KaleidoscopeMechanicService;
import dev.cosmojar.stellaritypaper.enchants.DragonbladeMechanicService;
import dev.cosmojar.stellaritypaper.enchants.HarvesterMechanicService;
import dev.cosmojar.stellaritypaper.enchants.HarvesterAbilityCodec;
import dev.cosmojar.stellaritypaper.enchants.HarvesterLoreService;
import dev.cosmojar.stellaritypaper.enchants.HarvesterAbilityUnlockService;
import dev.cosmojar.stellaritypaper.enchants.PrismaticInfernoEffectService;
import dev.cosmojar.stellaritypaper.enchants.HolyFlamesEffectService;
import dev.cosmojar.stellaritypaper.enchants.RadiantJewelService;
import dev.cosmojar.stellaritypaper.enchants.StellaritySoundService;
import dev.cosmojar.stellaritypaper.mechanics.painting.PaintingService;
import dev.cosmojar.stellaritypaper.mechanics.painting.PaintingListener;
import dev.cosmojar.stellaritypaper.mechanics.villager.EndVillagerTradeRegistry;
import dev.cosmojar.stellaritypaper.mechanics.villager.EndVillagerTradeService;
import dev.cosmojar.stellaritypaper.mechanics.villager.MerchantRecipeFactory;
import dev.cosmojar.stellaritypaper.enchants.VoidedEffectService;
import dev.cosmojar.stellaritypaper.enchants.WeaponMechanicService;
import dev.cosmojar.stellaritypaper.enchants.VoidPendantBehaviorService;
import dev.cosmojar.stellaritypaper.enchants.ArmorSetBonusService;
import dev.cosmojar.stellaritypaper.enchants.ArmorSetBonusListener;
import dev.cosmojar.stellaritypaper.enchants.TrinketBehaviorService;
import dev.cosmojar.stellaritypaper.enchants.TrinketBehaviorListener;
import dev.cosmojar.stellaritypaper.enchants.ElytraBehaviorService;
import dev.cosmojar.stellaritypaper.enchants.ElytraBehaviorListener;
import dev.cosmojar.stellaritypaper.items.CustomItemFactory;
import dev.cosmojar.stellaritypaper.mechanics.portal.EndPortalAmbientService;
import dev.cosmojar.stellaritypaper.mechanics.portal.EndPortalCinematicService;
import dev.cosmojar.stellaritypaper.mechanics.portal.EndPortalListener;
import dev.cosmojar.stellaritypaper.items.CustomItemMatcher;
import dev.cosmojar.stellaritypaper.items.CustomFoodListener;
import dev.cosmojar.stellaritypaper.items.CustomSpawnEggListener;
import dev.cosmojar.stellaritypaper.items.CustomBlockService;
import dev.cosmojar.stellaritypaper.items.CustomBlockListener;
import dev.cosmojar.stellaritypaper.items.ItemDefinitionRegistry;
import dev.cosmojar.stellaritypaper.items.ItemLoreBuilder;
import dev.cosmojar.stellaritypaper.items.api.StellarityItemDefinition;
import dev.cosmojar.stellaritypaper.items.catalog.ItemTextCatalog;
import dev.cosmojar.stellaritypaper.items.definitions.GeneratedItemRegistry;
import dev.cosmojar.stellaritypaper.mechanics.altar.AccursedAltarListener;
import dev.cosmojar.stellaritypaper.mechanics.altar.AccursedAltarService;
import dev.cosmojar.stellaritypaper.mechanics.altar.SacredAltarService;
import dev.cosmojar.stellaritypaper.mechanics.cauldron.CauldronCraftListener;
import dev.cosmojar.stellaritypaper.mechanics.cauldron.CauldronCraftService;
import dev.cosmojar.stellaritypaper.mechanics.cauldron.CauldronRecipeRegistry;
import dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationListener;
import dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationRuleRegistry;
import dev.cosmojar.stellaritypaper.mechanics.consecration.ConsecrationService;
import dev.cosmojar.stellaritypaper.mechanics.fishing.VoidFishingListener;
import dev.cosmojar.stellaritypaper.mechanics.fishing.VoidFishingLootService;
import dev.cosmojar.stellaritypaper.mechanics.fishing.VoidFishingService;
import dev.cosmojar.stellaritypaper.mechanics.totem.VoidTotemListener;
import dev.cosmojar.stellaritypaper.mechanics.totem.VoidTotemService;
import dev.cosmojar.stellaritypaper.mechanics.trident.TridentReturnListener;
import dev.cosmojar.stellaritypaper.mechanics.trident.TridentReturnService;
import dev.cosmojar.stellaritypaper.mobs.EndMobVariantListener;
import dev.cosmojar.stellaritypaper.mobs.PhantomDropListener;
import dev.cosmojar.stellaritypaper.mobs.boss.BossManager;
import dev.cosmojar.stellaritypaper.mobs.EndMobVariantService;
import dev.cosmojar.stellaritypaper.registry.KeyFactory;
import dev.cosmojar.stellaritypaper.registry.MinecraftRegistryService;
import dev.cosmojar.stellaritypaper.text.MessageService;
import dev.cosmojar.stellaritypaper.text.MiniMessageService;
import dev.cosmojar.stellaritypaper.text.TextService;
import dev.cosmojar.stellaritypaper.ui.ItemInfoGuiService;
import dev.cosmojar.stellaritypaper.update.UpdateCheckerService;
import dev.cosmojar.stellaritypaper.update.UpdateNotificationListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;

import java.util.List;

public final class PluginBootstrap {

    private final StellarityPaperPlugin plugin;
    private final ServiceContainer services;
    private final ListenerRegistrar listenerRegistrar;

    public PluginBootstrap(final StellarityPaperPlugin plugin) {
        this.plugin = plugin;
        this.services = new ServiceContainer();
        this.listenerRegistrar = new ListenerRegistrar(plugin);
    }

    public void load() {
        registerCore();
        

        final PaintingService paintingService = new PaintingService(plugin);
        services.register(PaintingService.class, paintingService);
    }

    public void enable() {
        try {
            final ServerVersion currentVersion = ServerVersion.getCurrent();
            VersionManager.initialize(currentVersion);
            plugin.getLogger().info("Adapter " + VersionManager.getAdapter().getAdapterName() + " has been successfully loaded." + " (server version: " + Bukkit.getMinecraftVersion() + ")");
        } catch (final UnsupportedServerVersionException e) {
            plugin.getLogger().severe("=================================================");
            plugin.getLogger().severe("[Error] Server version '" + Bukkit.getMinecraftVersion() + "' not supported!");
            plugin.getLogger().severe("Enabling Stellarity has been cancelled.");
            plugin.getLogger().severe("=================================================");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        dev.cosmojar.stellaritypaper.integration.WorldGuardHook.init();
        if ("Paper-1.21.11-Adapter".equals(VersionManager.getAdapter().getAdapterName())) {
            new dev.cosmojar.stellaritypaper.enchants.VoidPendantNmsRegistry().registerAll();
        } else {
            VersionManager.getAdapter().registerVoidPendantEnchantments();
        }
        registerItems();
        registerMobs();
        registerMechanics();
        registerListeners();
        registerCommands();

        services.require(dev.cosmojar.stellaritypaper.mechanics.structures.EndStructuresService.class).enable();

        services.require(ArmorSetBonusService.class).start();
        services.require(TrinketBehaviorService.class).start();
        services.require(ElytraBehaviorService.class).start();
        services.require(EndPortalAmbientService.class).start();
        services.require(UpdateCheckerService.class).start();

        dev.cosmojar.stellaritypaper.EbalYaVRortEcoPlugins.ecohook.vkluchitZashituBlyat(
                plugin,
                services.require(ItemDefinitionRegistry.class),
                services.require(ItemLoreBuilder.class),
                services.require(ItemStateRepository.class)
        );

        cleanupTemporaryEntities();

        plugin.getLogger().info(services.require(MessageService.class).raw("log.plugin.enabled"));
    }

    private void cleanupTemporaryEntities() {
        try {
            final org.bukkit.NamespacedKey tempKey = new org.bukkit.NamespacedKey(plugin, "temp_display");
            final org.bukkit.NamespacedKey breathKey = new org.bukkit.NamespacedKey(plugin, "dragon_breath");
            final org.bukkit.NamespacedKey encounterKey = new org.bukkit.NamespacedKey(plugin, "encounter_id");

            for (final org.bukkit.World world : Bukkit.getWorlds()) {
                for (final org.bukkit.entity.Entity entity : world.getEntities()) {
                    final org.bukkit.persistence.PersistentDataContainer pdc = entity.getPersistentDataContainer();
                    if (pdc.has(tempKey, org.bukkit.persistence.PersistentDataType.BYTE)
                            || pdc.has(breathKey, org.bukkit.persistence.PersistentDataType.BYTE)
                            || pdc.has(encounterKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        if (!(entity instanceof org.bukkit.entity.EnderDragon)) {
                            entity.remove();
                            continue;
                        }
                    }
                    for (final String tag : entity.getScoreboardTags()) {
                        if (tag.startsWith("stellarity.empress_of_light")
                                || tag.startsWith("stellarity.shulking")
                                || tag.equals("stellarity.dragon_breath")) {
                            entity.remove();
                            break;
                        }
                    }
                    if (entity instanceof org.bukkit.entity.AreaEffectCloud cloud && dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager.isTheEnd(world)) {
                        if (cloud.hasCustomEffect(org.bukkit.potion.PotionEffectType.WEAKNESS)) {
                            cloud.remove();
                        }
                    }
                }
            }
        } catch (final Exception ignored) {
        }
    }

    public void disable() {
        cleanupTemporaryEntities();
        dev.cosmojar.stellaritypaper.EbalYaVRortEcoPlugins.ecohook.svalitVZakhat();
        try {
            services.require(dev.cosmojar.stellaritypaper.mechanics.pixie.PixieEntityService.class).cleanupAll();
        } catch (final IllegalStateException ignored) {
        }
        try {
            services.require(VoidFishingService.class).cancelAll();
        } catch (final IllegalStateException ignored) {
        }
        services.require(SessionTaskService.class).cancelAll();
        try {
            services.require(dev.cosmojar.stellaritypaper.mechanics.structures.EndStructuresService.class).disable();
        } catch (final IllegalStateException ignored) {
        }
        try {
            services.require(dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager.class).cleanup();
        } catch (final IllegalStateException ignored) {
        }

        try {
            services.require(ArmorSetBonusService.class).stop();
            services.require(TrinketBehaviorService.class).stop();
            services.require(ElytraBehaviorService.class).stop();
        } catch (final IllegalStateException ignored) {
        }
        try {
            final EnchantRuntimeService runtimeService = services.require(EnchantRuntimeService.class);
            final ArmorSetBonusService armorSetBonusService = services.require(ArmorSetBonusService.class);
            final TrinketBehaviorService trinketBehaviorService = services.require(TrinketBehaviorService.class);
            for (final var player : Bukkit.getOnlinePlayers()) {
                runtimeService.cleanup(player);
                armorSetBonusService.clear(player);
                trinketBehaviorService.clear(player);
            }
            runtimeService.cleanupAll();
            final EnchantHorseBackendService horseBackendService = services.require(EnchantHorseBackendService.class);
            for (final var world : Bukkit.getWorlds()) {
                for (final AbstractHorse horse : world.getEntitiesByClass(AbstractHorse.class)) {
                    horseBackendService.clear(horse);
                }
            }
        } catch (final IllegalStateException ignored) {
        }
        try {
            final BossManager bossManager = services.require(BossManager.class);
            bossManager.cleanupAll();
        } catch (final IllegalStateException ignored) {
        }
        try {
            services.require(EndPortalAmbientService.class).cleanupAll();
            services.require(EndPortalCinematicService.class).cleanupAll();
        } catch (final IllegalStateException ignored) {
        }
        plugin.getLogger().info(services.require(MessageService.class).raw("log.plugin.disabled"));
    }

    private void registerCore() {
        final ConfigService configService = new ConfigService(plugin);
        final ItemsConfigService itemsConfigService = new ItemsConfigService(plugin);
        itemsConfigService.load();
        final FeatureFlags featureFlags = new FeatureFlags(configService);
        final MiniMessageService miniMessageService = new MiniMessageService();
        final TextService textService = new TextService(miniMessageService);
        final MessageService messageService = new MessageService(plugin, configService, textService);
        final KeyFactory keyFactory = new KeyFactory(plugin);
        final MinecraftRegistryService registryService = new MinecraftRegistryService();
        final PdcKeys pdcKeys = new PdcKeys(keyFactory);
        final ItemStateRepository itemStateRepository = new ItemStateRepository(pdcKeys);
        final PlayerStateRepository playerStateRepository = new PlayerStateRepository(pdcKeys);
        final SessionTaskService sessionTaskService = new SessionTaskService(plugin);
        final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService =
                new dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService();
        final UpdateCheckerService updateCheckerService = new UpdateCheckerService(plugin);

        services.register(ConfigService.class, configService);
        services.register(ItemsConfigService.class, itemsConfigService);
        services.register(FeatureFlags.class, featureFlags);
        services.register(MiniMessageService.class, miniMessageService);
        services.register(TextService.class, textService);
        services.register(MessageService.class, messageService);
        services.register(KeyFactory.class, keyFactory);
        services.register(MinecraftRegistryService.class, registryService);
        services.register(PdcKeys.class, pdcKeys);
        services.register(ItemStateRepository.class, itemStateRepository);
        services.register(PlayerStateRepository.class, playerStateRepository);
        services.register(SessionTaskService.class, sessionTaskService);
        services.register(dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService.class, advancementService);
        services.register(UpdateCheckerService.class, updateCheckerService);
    }

    private void registerItems() {
        final ItemLoreBuilder loreBuilder = new ItemLoreBuilder(
                services.require(TextService.class),
                services.require(ConfigService.class),
                services.require(ItemsConfigService.class),
                services.require(MessageService.class)
        );
        services.register(ItemLoreBuilder.class, loreBuilder);
        final List<StellarityItemDefinition> generatedDefinitions = GeneratedItemRegistry.create();
        final ItemTextCatalog itemTextCatalog = new ItemTextCatalog(plugin);
        final ItemDefinitionRegistry itemDefinitionRegistry = new ItemDefinitionRegistry(
                itemTextCatalog,
                generatedDefinitions
        );
        final CustomItemMatcher customItemMatcher = new CustomItemMatcher(services.require(ItemStateRepository.class));

        final EnchantDefinitionRegistry enchantRegistry = new EnchantDefinitionRegistry(
                GeneratedEnchantDefinitions.create()
        );
        enchantRegistry.validateItemBindings(generatedDefinitions);
        final EnchantLocalizationValidator enchantLocalizationValidator = new EnchantLocalizationValidator(
                plugin,
                services.require(ConfigService.class)
        );
        enchantLocalizationValidator.validate(enchantRegistry);
        final EnchantDataCodec enchantDataCodec = new EnchantDataCodec();
        final HarvesterAbilityCodec harvesterAbilityCodec = new HarvesterAbilityCodec();
        final EnchantBehaviorRegistry enchantBehaviorRegistry = new EnchantBehaviorRegistry();
        final EnchantLoreService enchantLoreService = new EnchantLoreService(
                services.require(TextService.class),
                services.require(MessageService.class),
                enchantRegistry
        );
        final HarvesterLoreService harvesterLoreService = new HarvesterLoreService(
                services.require(ItemStateRepository.class),
                harvesterAbilityCodec
        );
        final EnchantActiveService enchantActiveService = new EnchantActiveService(
                enchantRegistry,
                enchantDataCodec,
                services.require(ItemStateRepository.class)
        );
        final StellaritySoundService stellaritySoundService = new StellaritySoundService();
        final VoidedEffectService voidedEffectService = new VoidedEffectService(
                plugin,
                services.require(KeyFactory.class)
        );
        final PrismaticInfernoEffectService prismaticInfernoEffectService = new PrismaticInfernoEffectService(plugin);
        final HolyFlamesEffectService holyFlamesEffectService = new HolyFlamesEffectService(plugin);
        final CustomStatusEffectService customStatusEffectService = new CustomStatusEffectService(
                voidedEffectService,
                prismaticInfernoEffectService,
                holyFlamesEffectService
        );
        final RadiantJewelService radiantJewelService = new RadiantJewelService(
                plugin,
                services.require(ItemStateRepository.class)
        );
        final EnchantMobilityBehaviorService enchantMobilityBehaviorService = new EnchantMobilityBehaviorService(
                enchantActiveService,
                services.require(KeyFactory.class)
        );
        final VoidPendantBehaviorService voidPendantBehaviorService = new VoidPendantBehaviorService(
                plugin,
                services.require(MessageService.class),
                services.require(KeyFactory.class)
        );
        final KaleidoscopeMechanicService kaleidoscopeMechanicService = new KaleidoscopeMechanicService(
                plugin,
                enchantActiveService,
                services.require(ItemStateRepository.class),
                stellaritySoundService,
                radiantJewelService,
                customStatusEffectService
        );
        final DragonbladeMechanicService dragonbladeMechanicService = new DragonbladeMechanicService(
                plugin,
                enchantActiveService,
                services.require(ItemStateRepository.class),
                customStatusEffectService,
                stellaritySoundService,
                services.require(ItemsConfigService.class)
        );
        final HarvesterMechanicService harvesterMechanicService = new HarvesterMechanicService(
                plugin,
                enchantActiveService,
                services.require(ItemStateRepository.class),
                harvesterAbilityCodec,
                stellaritySoundService
        );
        final EnchantCombatBehaviorService enchantCombatBehaviorService = new EnchantCombatBehaviorService(
                plugin,
                enchantActiveService,
                enchantDataCodec,
                services.require(ItemStateRepository.class),
                customStatusEffectService,
                kaleidoscopeMechanicService,
                dragonbladeMechanicService,
                harvesterMechanicService,
                stellaritySoundService
        );
        final EnchantTridentBehaviorService enchantTridentBehaviorService = new EnchantTridentBehaviorService(
                plugin,
                enchantActiveService
        );
        final EnchantItemService enchantItemService = new EnchantItemService(
                services.require(ConfigService.class),
                services.require(ItemStateRepository.class),
                itemDefinitionRegistry,
                enchantDataCodec,
                enchantLoreService,
                loreBuilder,
                harvesterLoreService
        );
        final CustomItemFactory customItemFactory = new CustomItemFactory(
                services.require(ItemStateRepository.class),
                services.require(TextService.class),
                loreBuilder,
                services.require(MinecraftRegistryService.class),
                enchantItemService
        );
        final CustomEnchantedBookFactory customEnchantedBookFactory = new CustomEnchantedBookFactory(
                enchantRegistry,
                services.require(ItemStateRepository.class),
                enchantDataCodec,
                enchantItemService
        );
        final CustomBlockService customBlockService = new CustomBlockService(
                plugin,
                services.require(ItemStateRepository.class),
                customItemFactory,
                itemDefinitionRegistry
        );
        final HarvesterAbilityUnlockService harvesterAbilityUnlockService = new HarvesterAbilityUnlockService(
                enchantActiveService,
                services.require(ItemStateRepository.class),
                services.require(PlayerStateRepository.class),
                harvesterAbilityCodec,
                enchantItemService,
                stellaritySoundService,
                services.require(dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService.class)
        );
        final WeaponMechanicService weaponMechanicService = new WeaponMechanicService(
                plugin,
                services.require(ItemStateRepository.class),
                customStatusEffectService,
                harvesterMechanicService,
                stellaritySoundService,
                services.require(ItemsConfigService.class),
                services.require(MessageService.class),
                radiantJewelService
        );
        final ArmorSetBonusService armorSetBonusService = new ArmorSetBonusService(
                plugin,
                customItemMatcher,
                services.require(ItemsConfigService.class)
        );
        final TrinketBehaviorService trinketBehaviorService = new TrinketBehaviorService(
                plugin,
                customItemMatcher,
                stellaritySoundService,
                itemDefinitionRegistry,
                customItemFactory,
                services.require(ItemsConfigService.class),
                services.require(MessageService.class),
                radiantJewelService,
                customStatusEffectService
        );
        final ElytraBehaviorService elytraBehaviorService = new ElytraBehaviorService(
                plugin,
                customItemMatcher,
                services.require(ItemsConfigService.class)
        );
        final EnchantAttributeBackendService enchantAttributeBackendService = new EnchantAttributeBackendService(
                enchantRegistry,
                enchantDataCodec,
                services.require(ItemStateRepository.class),
                services.require(MinecraftRegistryService.class),
                services.require(KeyFactory.class)
        );
        final EnchantEventBackendService enchantEventBackendService = new EnchantEventBackendService(
                enchantRegistry,
                enchantDataCodec,
                services.require(ItemStateRepository.class)
        );
        final EnchantRuntimeService enchantRuntimeService = new EnchantRuntimeService(
                enchantAttributeBackendService,
                enchantEventBackendService,
                enchantMobilityBehaviorService,
                enchantCombatBehaviorService,
                enchantTridentBehaviorService,
                voidPendantBehaviorService,
                customStatusEffectService,
                kaleidoscopeMechanicService,
                dragonbladeMechanicService,
                harvesterMechanicService,
                harvesterAbilityUnlockService,
                weaponMechanicService,
                armorSetBonusService,
                trinketBehaviorService
        );
        final EnchantHorseBackendService enchantHorseBackendService = new EnchantHorseBackendService(
                enchantRegistry,
                enchantDataCodec,
                services.require(ItemStateRepository.class),
                services.require(MinecraftRegistryService.class),
                services.require(KeyFactory.class)
        );


        services.register(ItemLoreBuilder.class, loreBuilder);
        services.register(ItemTextCatalog.class, itemTextCatalog);
        services.register(ItemDefinitionRegistry.class, itemDefinitionRegistry);
        services.register(EnchantDefinitionRegistry.class, enchantRegistry);
        services.register(EnchantLocalizationValidator.class, enchantLocalizationValidator);
        services.register(EnchantDataCodec.class, enchantDataCodec);
        services.register(HarvesterAbilityCodec.class, harvesterAbilityCodec);
        services.register(EnchantBehaviorRegistry.class, enchantBehaviorRegistry);
        services.register(EnchantLoreService.class, enchantLoreService);
        services.register(HarvesterLoreService.class, harvesterLoreService);
        services.register(EnchantActiveService.class, enchantActiveService);
        services.register(StellaritySoundService.class, stellaritySoundService);
        services.register(VoidedEffectService.class, voidedEffectService);
        services.register(PrismaticInfernoEffectService.class, prismaticInfernoEffectService);
        services.register(HolyFlamesEffectService.class, holyFlamesEffectService);
        services.register(CustomStatusEffectService.class, customStatusEffectService);
        services.register(RadiantJewelService.class, radiantJewelService);
        services.register(EnchantMobilityBehaviorService.class, enchantMobilityBehaviorService);
        services.register(VoidPendantBehaviorService.class, voidPendantBehaviorService);
        services.register(KaleidoscopeMechanicService.class, kaleidoscopeMechanicService);
        services.register(DragonbladeMechanicService.class, dragonbladeMechanicService);
        services.register(HarvesterMechanicService.class, harvesterMechanicService);
        services.register(HarvesterAbilityUnlockService.class, harvesterAbilityUnlockService);
        services.register(WeaponMechanicService.class, weaponMechanicService);
        services.register(ArmorSetBonusService.class, armorSetBonusService);
        services.register(TrinketBehaviorService.class, trinketBehaviorService);
        services.register(ElytraBehaviorService.class, elytraBehaviorService);
        services.register(EnchantCombatBehaviorService.class, enchantCombatBehaviorService);
        services.register(EnchantTridentBehaviorService.class, enchantTridentBehaviorService);
        services.register(EnchantItemService.class, enchantItemService);
        services.register(EnchantAttributeBackendService.class, enchantAttributeBackendService);
        services.register(EnchantEventBackendService.class, enchantEventBackendService);
        services.register(EnchantRuntimeService.class, enchantRuntimeService);
        services.register(EnchantHorseBackendService.class, enchantHorseBackendService);
        final dev.cosmojar.stellaritypaper.mechanics.loot.ExplorerMapService explorerMapService = new dev.cosmojar.stellaritypaper.mechanics.loot.ExplorerMapService(
                plugin,
                services.require(ItemDefinitionRegistry.class),
                customItemFactory
        );
        services.register(dev.cosmojar.stellaritypaper.mechanics.loot.ExplorerMapService.class, explorerMapService);

        final dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService customLootService = new dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService(
                plugin,
                services.require(ItemDefinitionRegistry.class),
                customItemFactory,
                customEnchantedBookFactory,
                explorerMapService
        );
        services.register(dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService.class, customLootService);

        final ItemInfoGuiService itemInfoGuiService = new ItemInfoGuiService(
                plugin,
                services.require(MessageService.class),
                services.require(TextService.class),
                services.require(ItemLoreBuilder.class),
                services.require(ItemStateRepository.class),
                services.require(EnchantDataCodec.class)
        );

        services.register(CustomItemFactory.class, customItemFactory);
        services.register(CustomEnchantedBookFactory.class, customEnchantedBookFactory);
        services.register(CustomItemMatcher.class, customItemMatcher);
        services.register(ItemInfoGuiService.class, itemInfoGuiService);
        services.register(CustomBlockService.class, customBlockService);
    }

    private void registerMechanics() {
        final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService =
                services.require(dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService.class);

        final VoidTotemService voidTotemService = new VoidTotemService(
                services.require(FeatureFlags.class),
                services.require(TextService.class),
                services.require(dev.cosmojar.stellaritypaper.text.MessageService.class),
                advancementService
        );

        final TridentReturnService tridentReturnService = new TridentReturnService(
                services.require(FeatureFlags.class),
                services.require(SessionTaskService.class)
        );

        final VoidFishingLootService voidFishingLootService = new VoidFishingLootService(
                services.require(FeatureFlags.class),
                services.require(ItemDefinitionRegistry.class),
                services.require(CustomItemFactory.class),
                services.require(dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService.class)
        );
        final VoidFishingService voidFishingService = new VoidFishingService(
                plugin,
                services.require(FeatureFlags.class),
                voidFishingLootService,
                services.require(ItemStateRepository.class),
                services.require(TextService.class),
                advancementService
        );

        final ConsecrationRuleRegistry consecrationRuleRegistry = new ConsecrationRuleRegistry();
        final ConsecrationService consecrationService = new ConsecrationService(
                plugin,
                services.require(FeatureFlags.class),
                consecrationRuleRegistry,
                services.require(TextService.class),
                advancementService
        );

        final CauldronRecipeRegistry cauldronRecipeRegistry = new CauldronRecipeRegistry();
        final CauldronCraftService cauldronCraftService = new CauldronCraftService(
                plugin,
                services.require(FeatureFlags.class),
                cauldronRecipeRegistry,
                services.require(ItemDefinitionRegistry.class),
                services.require(CustomItemFactory.class),
                services.require(ItemStateRepository.class),
                services.require(TextService.class),
                consecrationService,
                advancementService
        );

        final AccursedAltarService altarService = new AccursedAltarService(
                plugin,
                services.require(FeatureFlags.class),
                services.require(CustomBlockService.class),
                services.require(ItemStateRepository.class),
                services.require(CustomItemFactory.class),
                services.require(ItemDefinitionRegistry.class),
                services.require(MessageService.class),
                services.require(EnchantDataCodec.class),
                services.require(EnchantItemService.class),
                advancementService
        );
        final SacredAltarService sacredAltarService = new SacredAltarService(
                plugin,
                services.require(CustomBlockService.class),
                services.require(ItemStateRepository.class),
                services.require(ItemsConfigService.class),
                services.require(MessageService.class),
                services.require(BossManager.class),
                advancementService
        );

        final dev.cosmojar.stellaritypaper.mechanics.pixie.PixieInAJarService pixieInAJarService = new dev.cosmojar.stellaritypaper.mechanics.pixie.PixieInAJarService(
                plugin,
                services.require(CustomBlockService.class)
        );

        final dev.cosmojar.stellaritypaper.mechanics.pixie.PixieEntityService pixieEntityService = new dev.cosmojar.stellaritypaper.mechanics.pixie.PixieEntityService(
                plugin,
                services.require(ItemDefinitionRegistry.class),
                services.require(CustomItemFactory.class)
        );

        final dev.cosmojar.stellaritypaper.mechanics.structures.StructuresCleaner structuresCleaner =
                new dev.cosmojar.stellaritypaper.mechanics.structures.StructuresCleaner(plugin);

        final dev.cosmojar.stellaritypaper.mechanics.structures.EndCityCrystalService endCityCrystalService =
                new dev.cosmojar.stellaritypaper.mechanics.structures.EndCityCrystalService(
                        plugin,
                        advancementService,
                        services.require(MessageService.class)
                );

        final dev.cosmojar.stellaritypaper.mechanics.structures.EndStructuresService structuresService =
                new dev.cosmojar.stellaritypaper.mechanics.structures.EndStructuresService(
                        plugin,
                        services.require(FeatureFlags.class),
                        services.require(CustomBlockService.class),
                        structuresCleaner,
                        endCityCrystalService
                );

        services.register(dev.cosmojar.stellaritypaper.mechanics.structures.EndCityCrystalService.class, endCityCrystalService);

        services.register(VoidTotemService.class, voidTotemService);
        services.register(TridentReturnService.class, tridentReturnService);
        services.register(VoidFishingLootService.class, voidFishingLootService);
        services.register(VoidFishingService.class, voidFishingService);
        services.register(CauldronRecipeRegistry.class, cauldronRecipeRegistry);
        services.register(CauldronCraftService.class, cauldronCraftService);
        services.register(ConsecrationRuleRegistry.class, consecrationRuleRegistry);
        services.register(ConsecrationService.class, consecrationService);
        services.register(AccursedAltarService.class, altarService);
        services.register(SacredAltarService.class, sacredAltarService);
        services.register(dev.cosmojar.stellaritypaper.mechanics.pixie.PixieInAJarService.class, pixieInAJarService);
        services.register(dev.cosmojar.stellaritypaper.mechanics.pixie.PixieEntityService.class, pixieEntityService);
        services.register(dev.cosmojar.stellaritypaper.mechanics.structures.EndStructuresService.class, structuresService);
        services.register(dev.cosmojar.stellaritypaper.command.UninstallService.class, new dev.cosmojar.stellaritypaper.command.UninstallService(plugin, services.require(MessageService.class)));

        final EndVillagerTradeRegistry endVillagerTradeRegistry = new EndVillagerTradeRegistry();
        final MerchantRecipeFactory merchantRecipeFactory = new MerchantRecipeFactory(
                services.require(ItemDefinitionRegistry.class),
                services.require(CustomItemFactory.class),
                services.require(dev.cosmojar.stellaritypaper.mechanics.loot.ExplorerMapService.class),
                plugin.getLogger()
        );
        final EndVillagerTradeService endVillagerTradeService = new EndVillagerTradeService(
                plugin,
                endVillagerTradeRegistry,
                merchantRecipeFactory
        );
        services.register(EndVillagerTradeRegistry.class, endVillagerTradeRegistry);
        services.register(MerchantRecipeFactory.class, merchantRecipeFactory);
        services.register(EndVillagerTradeService.class, endVillagerTradeService);

        final dev.cosmojar.stellaritypaper.items.endonomicon.EndonomiconService endonomiconService =
                new dev.cosmojar.stellaritypaper.items.endonomicon.EndonomiconService(
                        plugin,
                        altarService,
                        services.require(ItemDefinitionRegistry.class),
                        services.require(MessageService.class),
                        services.require(CustomItemFactory.class),
                        services.require(ItemStateRepository.class)
                );
        services.register(dev.cosmojar.stellaritypaper.items.endonomicon.EndonomiconService.class, endonomiconService);

        final dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager endIslandManager = new dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager(
            plugin, 
            services.require(CustomBlockService.class),
            services.require(CustomItemFactory.class),
            services.require(ItemDefinitionRegistry.class),
            services.require(MessageService.class)
        );
        services.register(dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager.class, endIslandManager);

        final EndPortalAmbientService endPortalAmbientService = new EndPortalAmbientService(plugin);
        services.register(EndPortalAmbientService.class, endPortalAmbientService);

        final EndPortalCinematicService endPortalCinematicService = new EndPortalCinematicService(
                plugin,
                services.require(StellaritySoundService.class),
                endPortalAmbientService,
                services.require(dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService.class)
        );
        services.register(EndPortalCinematicService.class, endPortalCinematicService);

        final EndPortalListener endPortalListener = new EndPortalListener(
                plugin,
                services.require(StellaritySoundService.class),
                endPortalCinematicService,
                endPortalAmbientService
        );
        services.register(EndPortalListener.class, endPortalListener);
    }

    private void registerMobs() {
        final EndMobVariantService endMobVariantService = new EndMobVariantService(
                plugin,
                services.require(MinecraftRegistryService.class),
                services.require(CustomItemMatcher.class),
                services.require(CustomItemFactory.class),
                services.require(ItemDefinitionRegistry.class)
        );
        final BossManager bossManager = new BossManager(
                plugin,
                services.require(MessageService.class),
                services.require(ItemDefinitionRegistry.class),
                services.require(CustomItemFactory.class),
                services.require(dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService.class),
                services.require(CustomStatusEffectService.class)
        );

        services.register(EndMobVariantService.class, endMobVariantService);
        services.register(BossManager.class, bossManager);
    }

    private void registerListeners() {
        listenerRegistrar.register(
                new PaintingListener(services.require(PaintingService.class)),
                new VoidTotemListener(services.require(VoidTotemService.class)),
                new TridentReturnListener(services.require(TridentReturnService.class)),
                new VoidFishingListener(services.require(VoidFishingService.class)),
                new CauldronCraftListener(services.require(CauldronCraftService.class)),
                services.require(CauldronCraftService.class),
                new ConsecrationListener(services.require(ConsecrationService.class)),
                new AccursedAltarListener(
                        plugin,
                        services.require(AccursedAltarService.class),
                        services.require(MessageService.class),
                        services.require(ItemDefinitionRegistry.class),
                        services.require(CustomItemFactory.class),
                        services.require(dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService.class)
                ),
                services.require(AccursedAltarService.class),
                services.require(SacredAltarService.class),
                new dev.cosmojar.stellaritypaper.items.endonomicon.EndonomiconListener(services.require(dev.cosmojar.stellaritypaper.items.endonomicon.EndonomiconService.class)),
                services.require(dev.cosmojar.stellaritypaper.mechanics.pixie.PixieInAJarService.class),
                services.require(dev.cosmojar.stellaritypaper.mechanics.pixie.PixieEntityService.class),
                services.require(dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager.class),
                new dev.cosmojar.stellaritypaper.mechanics.end.EndCrystalListener(plugin, services.require(dev.cosmojar.stellaritypaper.mechanics.end.EndIslandManager.class)),
                new dev.cosmojar.stellaritypaper.mobs.boss.dragon.DragonListener(
                        plugin,
                        services.require(BossManager.class),
                        services.require(MessageService.class),
                        services.require(dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService.class)
                ),
                new dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementListener(
                        plugin,
                        services.require(dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService.class),
                        services.require(ItemStateRepository.class),
                        services.require(dev.cosmojar.stellaritypaper.mechanics.pixie.PixieEntityService.class),
                        services.require(ItemDefinitionRegistry.class),
                        services.require(CustomItemFactory.class)
                ),
                new EndMobVariantListener(services.require(EndMobVariantService.class)),
                services.require(EndMobVariantService.class),
                services.require(HolyFlamesEffectService.class),
                services.require(RadiantJewelService.class),
                services.require(VoidPendantBehaviorService.class),
                new ArmorSetBonusListener(services.require(ArmorSetBonusService.class)),
                new TrinketBehaviorListener(services.require(TrinketBehaviorService.class)),
                new ElytraBehaviorListener(services.require(ElytraBehaviorService.class)),
                services.require(DragonbladeMechanicService.class),
                new EnchantRuntimeListener(
                        plugin,
                        services.require(EnchantRuntimeService.class),
                        services.require(EnchantItemService.class),
                        services.require(ItemStateRepository.class),
                        services.require(ItemsConfigService.class)
                ),
                new EnchantHorseRuntimeListener(
                        plugin,
                        services.require(EnchantHorseBackendService.class),
                        services.require(EnchantItemService.class)
                ),
                new dev.cosmojar.stellaritypaper.enchants.CustomEnchantAnvilListener(
                        services.require(EnchantDefinitionRegistry.class),
                        services.require(EnchantBehaviorRegistry.class),
                        services.require(ItemStateRepository.class),
                        services.require(EnchantDataCodec.class),
                        services.require(EnchantItemService.class)
                ),
                new dev.cosmojar.stellaritypaper.enchants.CustomEnchantGrindstoneListener(
                        services.require(ItemStateRepository.class),
                        services.require(EnchantItemService.class),
                        services.require(EnchantDataCodec.class)
                ),
                new dev.cosmojar.stellaritypaper.mechanics.smithing.CustomTrimSmithingListener(
                        services.require(ItemStateRepository.class),
                        services.require(EnchantItemService.class)
                ),
                new dev.cosmojar.stellaritypaper.mechanics.structures.StructureLootListener(
                        plugin,
                        services.require(dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService.class),
                        services.require(ItemStateRepository.class)
                ),
                new dev.cosmojar.stellaritypaper.mechanics.structures.EndCityVaultListener(
                        plugin,
                        services.require(dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService.class),
                        services.require(ItemStateRepository.class),
                        services.require(MessageService.class),
                        services.require(dev.cosmojar.stellaritypaper.mechanics.structures.EndCityCrystalService.class)
                ),
                new dev.cosmojar.stellaritypaper.mechanics.structures.EndCityTrialSpawnerListener(
                        plugin,
                        services.require(dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService.class)
                ),
                new dev.cosmojar.stellaritypaper.mechanics.structures.EndCityCrystalListener(
                        services.require(dev.cosmojar.stellaritypaper.mechanics.structures.EndCityCrystalService.class),
                        services.require(dev.cosmojar.stellaritypaper.mechanics.structures.EndStructuresService.class)
                ),
                new CustomFoodListener(
                        plugin,
                        services.require(CustomItemMatcher.class),
                        services.require(MessageService.class),
                        services.require(ItemLoreBuilder.class)
                ),
                new CustomSpawnEggListener(
                        services.require(CustomItemMatcher.class),
                        services.require(EndMobVariantService.class)
                ),
                new CustomBlockListener(
                        plugin,
                        services.require(CustomBlockService.class),
                        services.require(ItemStateRepository.class),
                        services.require(ItemDefinitionRegistry.class),
                        services.require(CustomItemFactory.class)
                ),
                services.require(ItemInfoGuiService.class),
                new PhantomDropListener(
                        services.require(ItemDefinitionRegistry.class),
                        services.require(CustomItemFactory.class)
                ),
                new dev.cosmojar.stellaritypaper.mobs.EndMobDropListener(
                        services.require(ItemDefinitionRegistry.class),
                        services.require(CustomItemFactory.class)
                ),
                new dev.cosmojar.stellaritypaper.items.CustomWingsCraftListener(
                        services.require(ItemStateRepository.class),
                        services.require(ItemDefinitionRegistry.class),
                        services.require(CustomItemFactory.class)
                ),
                new dev.cosmojar.stellaritypaper.items.CustomVanillaCraftProtectionListener(
                        services.require(ItemStateRepository.class)
                ),
                services.require(EndVillagerTradeService.class),
                services.require(EndPortalListener.class),
                new UpdateNotificationListener(
                        plugin,
                        services.require(UpdateCheckerService.class)
                )
        );
    }

    private void registerCommands() {
        final ReloadCommand reloadCommand = new ReloadCommand(
                services.require(ConfigService.class),
                services.require(ItemsConfigService.class),
                services.require(MessageService.class),
                services.require(dev.cosmojar.stellaritypaper.mechanics.loot.CustomLootService.class)
        );
        final GiveCommand giveCommand = new GiveCommand(
                services.require(ItemDefinitionRegistry.class),
                services.require(CustomItemFactory.class),
                services.require(CustomEnchantedBookFactory.class),
                services.require(MessageService.class),
                services.require(dev.cosmojar.stellaritypaper.mechanics.loot.ExplorerMapService.class)
        );
        final EnchantAdminCommand enchantAdminCommand = new EnchantAdminCommand(
                services.require(EnchantDefinitionRegistry.class),
                services.require(EnchantBehaviorRegistry.class),
                services.require(EnchantDataCodec.class),
                services.require(ItemStateRepository.class),
                services.require(EnchantItemService.class),
                services.require(MessageService.class)
        );
        final StellarityCommand stellarityCommand = new StellarityCommand(
                reloadCommand,
                giveCommand,
                enchantAdminCommand,
                services.require(dev.cosmojar.stellaritypaper.command.UninstallService.class),
                services.require(ItemDefinitionRegistry.class),
                services.require(EnchantDefinitionRegistry.class),
                services.require(MessageService.class),
                services.require(ItemStateRepository.class),
                services.require(ItemInfoGuiService.class)
        );

        final org.bukkit.command.CommandMap commandMap = Bukkit.getCommandMap();
        final DynamicCommand dynamicCommand = new DynamicCommand(
                "stellarity",
                "The main command for Stellarity Paper",
                "/stellarity <reload|give|enchant|item|uninstall>",
                List.of(),
                stellarityCommand,
                stellarityCommand
        );
        dynamicCommand.setPermission("stellarity.command");
        commandMap.register("stellarity", "stellaritypaper", dynamicCommand);
    }

    private static class DynamicCommand extends org.bukkit.command.Command {
        private final org.bukkit.command.CommandExecutor executor;
        private final org.bukkit.command.TabCompleter completer;

        protected DynamicCommand(String name, String description, String usageMessage, List<String> aliases, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
            super(name, description, usageMessage, aliases);
            this.executor = executor;
            this.completer = completer;
        }

        @Override
        public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
            return executor.onCommand(sender, this, commandLabel, args);
        }

        @Override
        public List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
            if (completer != null) {
                return completer.onTabComplete(sender, this, alias, args);
            }
            return super.tabComplete(sender, alias, args);
        }
    }
}
