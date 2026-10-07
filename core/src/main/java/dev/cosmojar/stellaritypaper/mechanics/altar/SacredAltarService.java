package dev.cosmojar.stellaritypaper.mechanics.altar;

import dev.cosmojar.stellaritypaper.config.ItemsConfigService;
import dev.cosmojar.stellaritypaper.data.ItemStateRepository;
import dev.cosmojar.stellaritypaper.integration.WorldGuardHook;
import dev.cosmojar.stellaritypaper.items.CustomBlockService;
import dev.cosmojar.stellaritypaper.mobs.boss.BossManager;
import dev.cosmojar.stellaritypaper.mobs.boss.EmpressOfLight;
import dev.cosmojar.stellaritypaper.mobs.boss.Shulking;
import dev.cosmojar.stellaritypaper.text.MessageService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Color;
import org.bukkit.Bukkit;

public final class SacredAltarService implements org.bukkit.event.Listener {

    private final Plugin plugin;
    private final CustomBlockService customBlockService;
    private final ItemStateRepository itemStateRepository;
    private final ItemsConfigService itemsConfigService;
    private final MessageService messageService;
    private final BossManager bossManager;
    private final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService;
    private final java.util.Set<Location> staticAltars = new java.util.HashSet<>();
    private final java.util.Set<Location> activeSpawningAltars = new java.util.HashSet<>();
    private final java.util.Map<Location, Long> lastBusyWarnTime = new java.util.HashMap<>();

    public SacredAltarService(
            final Plugin plugin,
            final CustomBlockService customBlockService,
            final ItemStateRepository itemStateRepository,
            final ItemsConfigService itemsConfigService,
            final MessageService messageService,
            final BossManager bossManager,
            final dev.cosmojar.stellaritypaper.mechanics.advancements.AdvancementService advancementService
    ) {
        this.plugin = plugin;
        this.customBlockService = customBlockService;
        this.itemStateRepository = itemStateRepository;
        this.itemsConfigService = itemsConfigService;
        this.messageService = messageService;
        this.bossManager = bossManager;
        this.advancementService = advancementService;

        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::tickAltarLogic, 20L, 20L);
        loadLoadedAltars();
    }

    private void tickAltarLogic() {
        final Material baseMat = customBlockService != null ? 
                customBlockService.getBaseMaterial("altar_of_the_sacred") : Material.CRYING_OBSIDIAN;
        final Material activeMat = baseMat != null ? baseMat : Material.CRYING_OBSIDIAN;

        for (final Location loc : new java.util.ArrayList<>(staticAltars)) {
            if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                if (loc.getBlock().getType() != activeMat) {
                    staticAltars.remove(loc);
                    continue;
                }
                processAltarAt(loc);
            }
        }
    }

    private void processAltarAt(final Location blockLoc) {
        final Location searchLoc = blockLoc.clone().add(0.5D, 1.1D, 0.5D);

        for (final Entity entity : searchLoc.getWorld().getNearbyEntities(searchLoc, 0.8D, 0.5D, 0.8D)) {
            if (entity instanceof Item itemEntity) {
                final ItemStack item = itemEntity.getItemStack();
                if (item.getType() == Material.AIR) {
                    continue;
                }

                final Material mat = item.getType();
                final PotionEffectType effectType = getBlessEffectType(mat);
                if (effectType != null) {
                    final int mul = getBlessMultiplier(mat);
                    applyBlessing(searchLoc, itemEntity, effectType, mul);
                    break;
                }

                final String customId = itemStateRepository.getItemId(item).orElse(null);
                if (customId != null) {
                    if ("starlight_soot".equalsIgnoreCase(customId)) {
                        tryTriggerBossSpawn(blockLoc, searchLoc, itemEntity, "Empress of Light");
                        break;
                    } else if ("shulker_body".equalsIgnoreCase(customId)) {
                        tryTriggerBossSpawn(blockLoc, searchLoc, itemEntity, "Shulking");
                        break;
                    }
                }
            }
        }
    }

    private void applyBlessing(final Location loc, final Item itemEntity, final PotionEffectType effectType, final int mul) {
        final ItemStack item = itemEntity.getItemStack();
        final int count = item.getAmount();


        final double x = count * 10.0;
        final double baseSeconds = ((2.0 * x * x) / 100.0) + 300.0;
        final double durationSeconds = (baseSeconds * mul) / 100.0;
        final double durationTicks = durationSeconds * 20.0;

        Player target = null;
        double nearest = Double.MAX_VALUE;
        for (final Player player : loc.getWorld().getPlayers()) {
            final double dist = player.getLocation().distance(loc);
            if (dist <= 10.0 && dist < nearest) {
                nearest = dist;
                target = player;
            }
        }

        if (target != null) {
            target.addPotionEffect(new PotionEffect(effectType, (int) durationTicks, 0, true, true, true));

            playBlessEffects(loc);

            itemEntity.remove();
        }
    }

    private boolean isAltarBusy(final Location blockLoc) {
        if (blockLoc == null) return false;
        if (activeSpawningAltars.contains(blockLoc)) {
            return true;
        }
        return bossManager.isAltarActive(blockLoc);
    }

    private void tryTriggerBossSpawn(final Location blockLoc, final Location loc, final Item itemEntity, final String bossName) {
        Player target = null;
        double nearest = Double.MAX_VALUE;
        for (final Player player : loc.getWorld().getPlayers()) {
            final double dist = player.getLocation().distance(loc);
            if (dist <= 10.0 && dist < nearest) {
                nearest = dist;
                target = player;
            }
        }

        if (target == null) {
            return;
        }

        if (isAltarBusy(blockLoc)) {
            final long now = System.currentTimeMillis();
            final long lastWarn = lastBusyWarnTime.getOrDefault(blockLoc, 0L);
            if (now - lastWarn >= 4000L) {
                lastBusyWarnTime.put(blockLoc, now);
                target.sendMessage(messageService.message("altar_of_the_sacred.already_summoned"));
            }
            return;
        }

        if (!isBossSpawnAllowed(loc.getWorld())) {
            target.sendMessage(messageService.message("altar_of_the_sacred.invalid_world"));
            return;
        }

        if (!hasBuildRightsInRadius(target, loc, 10)) {
            target.sendMessage(messageService.message("altar_of_the_sacred.too_close_to_private"));
            return;
        }

        if (!isMobSpawningAllowedInRadius(loc, 10)) {
            target.sendMessage(messageService.message("altar_of_the_sacred.mob_spawning_denied"));
            return;
        }

        final ItemStack item = itemEntity.getItemStack();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            itemEntity.remove();
        }

        final boolean isEmpress = "Empress of Light".equalsIgnoreCase(bossName);
        boolean isRadiantRitual = false;
        if (isEmpress) {
            final ItemStack offHand = target.getInventory().getItemInOffHand();
            if (offHand != null && !offHand.getType().isAir()) {
                final String offHandId = itemStateRepository.getItemId(offHand).orElse("");
                if ("kaleidoscope".equalsIgnoreCase(offHandId)) {
                    isRadiantRitual = true;
                }
            }
        }

        if (isRadiantRitual) {
            final World world = loc.getWorld();
            EmpressOfLight.spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, loc, 2, 0.0, 0.0, 0.0, 0.0);
            EmpressOfLight.spawnParticleSafe(world, Particle.FLASH, loc.clone().add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0.0, Color.WHITE);
            EmpressOfLight.spawnParticleSafe(world, Particle.END_ROD, loc, 50, 0.3, 0.8, 0.3, 0.15);

            final Color[] rainbow = new Color[] {
                    Color.fromRGB(255, 0, 0),
                    Color.fromRGB(255, 127, 0),
                    Color.fromRGB(255, 255, 0),
                    Color.fromRGB(0, 255, 0),
                    Color.fromRGB(0, 200, 255),
                    Color.fromRGB(150, 0, 255),
                    Color.fromRGB(255, 100, 200)
            };
            for (final Color c : rainbow) {
                EmpressOfLight.spawnParticleSafe(world, Particle.DUST, loc.clone().add(0, 1, 0), 12, 0.8, 0.8, 0.8, 0.0, new Particle.DustOptions(c, 1.6F));
            }

            world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.BLOCKS, 1.5F, 0.7F);
            world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS, 1.5F, 1.2F);
            world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.BLOCKS, 1.2F, 1.5F);
            world.playSound(loc, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.BLOCKS, 1.0F, 1.2F);

            final String radiantName = messageService.raw("boss.radiant_empress_of_light.name");
            final java.util.Map<String, String> placeholders = java.util.Map.of("boss", radiantName != null ? radiantName : "✦ Radiant Empress of Light");
            final net.kyori.adventure.text.Component startMsg = messageService.message("altar_of_the_sacred.spawn_started", placeholders);
            Bukkit.broadcast(startMsg);
        } else {
            EmpressOfLight.spawnParticleSafe(loc.getWorld(), Particle.EXPLOSION, loc, 1, 0.0, 0.0, 0.0, 0.0);
            EmpressOfLight.spawnParticleSafe(loc.getWorld(), Particle.SMOKE, loc, 14, 0.0, 0.0, 0.0, 0.12);
            EmpressOfLight.spawnParticleSafe(loc.getWorld(), Particle.END_ROD, loc, 28, 0.0, 0.0, 0.0, 0.12);

            loc.getWorld().playSound(loc, Sound.ENTITY_ALLAY_ITEM_TAKEN, 1.0F, 1.0F);
            loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, 1.0F, 1.0F);
            loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, 1.0F, 0.0F);

            final java.util.Map<String, String> placeholders = java.util.Map.of("boss", bossName);
            final net.kyori.adventure.text.Component startMsg = messageService.message("altar_of_the_sacred.spawn_started", placeholders);
            Bukkit.broadcast(startMsg);
        }

        final Location spawnLoc = loc.clone().add(0.0D, 1.0D, 0.0D);
        if (isEmpress) {
            if (advancementService != null) {
                advancementService.grant(target, "stellarity:empress_of_light/summon_empress");
            }
            activeSpawningAltars.add(blockLoc);
            final EmpressOfLight.Variant variant = isRadiantRitual ? EmpressOfLight.Variant.RADIANT : null;
            final EmpressSpawnAnimationTask task = new EmpressSpawnAnimationTask(
                    plugin, spawnLoc, bossManager, blockLoc, activeSpawningAltars, variant
            );
            final int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                try {
                    task.run();
                } catch (final Throwable t) {
                    activeSpawningAltars.remove(blockLoc);
                    if (!(t instanceof RuntimeException && "Stop task".equals(t.getMessage()))) {
                        plugin.getLogger().severe("Animation execution exception summoning the Empress of Light:");
                        t.printStackTrace();
                    }
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                }
            }, 0L, 1L);
        } else if ("Shulking".equalsIgnoreCase(bossName)) {
            new Shulking(plugin, spawnLoc, bossManager, blockLoc);
        }
    }

    private boolean isBossSpawnAllowed(final World world) {
        final org.bukkit.configuration.file.YamlConfiguration config = itemsConfigService.getBlocksConfig();
        if (config == null) {
            return false;
        }

        if (config.getBoolean("altar_of_the_sacred.allow-boss-spawn-in-any-world", false)) {
            return true;
        }

        final java.util.List<String> allowedWorlds = config.getStringList("altar_of_the_sacred.allowed-boss-spawn-worlds");
        return allowedWorlds.contains(world.getName());
    }

    private boolean hasBuildRightsInRadius(final Player player, final Location center, final int radius) {
        for (int x = -radius; x <= radius; x += 5) {
            for (int z = -radius; z <= radius; z += 5) {
                final Location checkLoc = center.clone().add(x, 0, z);
                if (!WorldGuardHook.canBuild(player, checkLoc)) {
                    return false;
                }
            }
        }
        final int[][] offsets = {
            {-radius, -radius}, {-radius, radius}, {radius, -radius}, {radius, radius}
        };
        for (final int[] offset : offsets) {
            final Location checkLoc = center.clone().add(offset[0], 0, offset[1]);
            if (!WorldGuardHook.canBuild(player, checkLoc)) {
                return false;
            }
        }
        return true;
    }

    private boolean isMobSpawningAllowedInRadius(final Location center, final int radius) {
        if (!WorldGuardHook.isMobSpawningAllowed(center)) {
            return false;
        }
        for (int x = -radius; x <= radius; x += 5) {
            for (int z = -radius; z <= radius; z += 5) {
                final Location checkLoc = center.clone().add(x, 0, z);
                if (!WorldGuardHook.isMobSpawningAllowed(checkLoc)) {
                    return false;
                }
            }
        }
        final int[][] offsets = {
            {-radius, -radius}, {-radius, radius}, {radius, -radius}, {radius, radius}
        };
        for (final int[] offset : offsets) {
            final Location checkLoc = center.clone().add(offset[0], 0, offset[1]);
            if (!WorldGuardHook.isMobSpawningAllowed(checkLoc)) {
                return false;
            }
        }
        return true;
    }


    private void playBlessEffects(final Location loc) {
        EmpressOfLight.spawnParticleSafe(loc.getWorld(), Particle.EXPLOSION, loc, 1, 0.0, 0.0, 0.0, 0.0);
        EmpressOfLight.spawnParticleSafe(loc.getWorld(), Particle.HAPPY_VILLAGER, loc, 14, 0.3, 0.3, 0.3, 0.0);

        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, 1.0F, 0.0F);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, 1.0F, 1.0F);
        loc.getWorld().playSound(loc, Sound.BLOCK_BELL_RESONATE, 1.0F, 1.5F);

        loc.getWorld().playSound(loc, "block.altar_of_the_sacred.bless", SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    private PotionEffectType getBlessEffectType(final Material mat) {
        switch (mat) {
            case IRON_INGOT:
                return PotionEffectType.RESISTANCE;
            case GOLD_INGOT:
                return PotionEffectType.HASTE;
            case EMERALD:
                return PotionEffectType.HERO_OF_THE_VILLAGE;
            case DIAMOND:
                return PotionEffectType.SPEED;
            case NETHERITE_INGOT:
                return PotionEffectType.FIRE_RESISTANCE;
            default:
                return null;
        }
    }

    private int getBlessMultiplier(final Material mat) {
        switch (mat) {
            case IRON_INGOT:
                return 60;
            case GOLD_INGOT:
                return 70;
            case EMERALD:
                return 85;
            case DIAMOND:
                return 110;
            case NETHERITE_INGOT:
                return 380;
            default:
                return 0;
        }
    }

    @org.bukkit.event.EventHandler
    public void onChunkLoad(final org.bukkit.event.world.ChunkLoadEvent event) {
        for (final Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemDisplay display) {
                final String blockId = customBlockService.getBlockId(display);
                if ("altar_of_the_sacred".equals(blockId)) {
                    staticAltars.add(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation());
                }
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onChunkUnload(final org.bukkit.event.world.ChunkUnloadEvent event) {
        staticAltars.removeIf(loc -> loc.getWorld().equals(event.getWorld()) && 
            (loc.getBlockX() >> 4) == event.getChunk().getX() && 
            (loc.getBlockZ() >> 4) == event.getChunk().getZ());
        lastBusyWarnTime.keySet().removeIf(loc -> loc.getWorld().equals(event.getWorld()) && 
            (loc.getBlockX() >> 4) == event.getChunk().getX() && 
            (loc.getBlockZ() >> 4) == event.getChunk().getZ());
        activeSpawningAltars.removeIf(loc -> loc.getWorld().equals(event.getWorld()) && 
            (loc.getBlockX() >> 4) == event.getChunk().getX() && 
            (loc.getBlockZ() >> 4) == event.getChunk().getZ());
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onBlockBreak(final org.bukkit.event.block.BlockBreakEvent event) {
        if (event.isCancelled() && event.getBlock().getType() != org.bukkit.Material.AIR) {
            return;
        }
        final Location loc = event.getBlock().getLocation();
        staticAltars.remove(loc);
        lastBusyWarnTime.remove(loc);
        activeSpawningAltars.remove(loc);
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(final org.bukkit.event.entity.EntitySpawnEvent event) {
        if (event.getEntity() instanceof ItemDisplay display) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (display.isValid()) {
                    final String blockId = customBlockService.getBlockId(display);
                    if ("altar_of_the_sacred".equals(blockId)) {
                        staticAltars.add(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation());
                    }
                }
            });
        }
    }

    private void loadLoadedAltars() {
        for (final World world : Bukkit.getWorlds()) {
            for (final org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (final Entity entity : chunk.getEntities()) {
                    if (entity instanceof ItemDisplay display) {
                        final String blockId = customBlockService.getBlockId(display);
                        if ("altar_of_the_sacred".equals(blockId)) {
                            staticAltars.add(display.getLocation().subtract(0.0D, 0.52D, 0.0D).getBlock().getLocation());
                        }
                    }
                }
            }
        }
    }

    private static final class EmpressSpawnAnimationTask implements Runnable {
        private final Plugin plugin;
        private final Location startLoc;
        private final BossManager bossManager;
        private final Location altarLoc;
        private final java.util.Set<Location> activeSpawningAltars;

        private int tick = 0;
        private final Location animLoc;
        private LivingEntity empressEntity = null;
        private EmpressOfLight empressBoss = null;
        private boolean isDaytime = false;
        private final EmpressOfLight.Variant variant;


        EmpressSpawnAnimationTask(
                final Plugin plugin,
                final Location startLoc,
                final BossManager bossManager,
                final Location altarLoc,
                final java.util.Set<Location> activeSpawningAltars,
                final EmpressOfLight.Variant variant
        ) {
            this.plugin = plugin;
            this.startLoc = startLoc;
            this.bossManager = bossManager;
            this.altarLoc = altarLoc;
            this.activeSpawningAltars = activeSpawningAltars;
            this.variant = variant;
            this.animLoc = startLoc.clone().add(0.0D, 1.0D, 0.0D);
            this.animLoc.setYaw(0.0F);
        }

        @Override
        public void run() {
            tick++;
            final World world = animLoc.getWorld();

            if (tick <= 100) {
                animLoc.add(0.0D, 0.04D, 0.0D);
            } else {
                animLoc.add(0.0D, 0.02D, 0.0D);
                animLoc.setYaw(animLoc.getYaw() + 2.0F);
            }

            if (tick == 1) {
                isDaytime = world.isDayTime();
                world.playSound(animLoc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 0.5F, 0.0F);
            }

            if (tick <= 60) {
                EmpressOfLight.spawnParticleSafe(world, Particle.PORTAL, animLoc, 12, 0.0, 0.0, 0.0, 3.0);
            }

            if (tick == 60 || tick == 80 || tick == 90 || tick == 95) {
                final Location flashLoc = animLoc.clone().subtract(0.0D, 1.5D, 0.0D);
                EmpressOfLight.spawnParticleSafe(world, Particle.EXPLOSION, flashLoc, 1, 0.0, 0.0, 0.0, 0.0);
                EmpressOfLight.spawnParticleSafe(world, Particle.END_ROD, flashLoc, 20, 0.0, 0.0, 0.0, 0.1);
                world.playSound(flashLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1.0F, 1.3F);
                world.playSound(flashLoc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, 1.0F, 0.0F);

                if (variant == EmpressOfLight.Variant.RADIANT) {
                    final Color[] rainbow = new Color[] {
                            Color.fromRGB(255, 0, 0),
                            Color.fromRGB(255, 127, 0),
                            Color.fromRGB(255, 255, 0),
                            Color.fromRGB(0, 255, 0),
                            Color.fromRGB(0, 200, 255),
                            Color.fromRGB(150, 0, 255)
                    };
                    for (final Color c : rainbow) {
                        EmpressOfLight.spawnParticleSafe(world, Particle.DUST, flashLoc, 4, 0.5, 0.5, 0.5, 0.0, new Particle.DustOptions(c, 1.5F));
                    }
                    world.playSound(flashLoc, Sound.BLOCK_BELL_USE, SoundCategory.HOSTILE, 1.2F, 1.0F);
                }
            }

            if (tick == 96) {
                final Location expLoc = animLoc.clone().subtract(0.0D, 1.0D, 0.0D);
                EmpressOfLight.spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, expLoc, 1, 0.0, 0.0, 0.0, 0.0);
            }

            if (tick == 101) {
                try {
                    empressBoss = new EmpressOfLight(plugin, animLoc, bossManager, altarLoc, variant);
                    empressBoss.setSpawning(true);
                    empressEntity = empressBoss.getBaseEntity();
                    if (empressEntity != null) {
                        empressEntity.setInvulnerable(true);
                        empressEntity.setSilent(true);
                    }
                } catch (final Throwable t) {
                    activeSpawningAltars.remove(altarLoc);
                    plugin.getLogger().severe("Error when summoning the Empress of Light boss.:");
                    t.printStackTrace();
                    throw new RuntimeException("Spawn failed", t);
                }

                final Location expLoc = animLoc.clone().subtract(0.0D, 1.0D, 0.0D);
                EmpressOfLight.spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, expLoc, 1, 0.0, 0.0, 0.0, 0.0);
                EmpressOfLight.spawnParticleSafe(world, Particle.END_ROD, animLoc.clone().add(0.0D, 1.0D, 0.0D), 150, 0.05, 0.05, 0.05, 0.45);

                world.playSound(animLoc.clone().add(0.0D, 1.0D, 0.0D), Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 1.0F, 0.8F);
                world.playSound(animLoc.clone().add(0.0D, 1.0D, 0.0D), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 1.5F, 0.8F);
                world.playSound(animLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.HOSTILE, 0.33F, 0.55F);
                world.playSound(animLoc.clone().add(0.0D, 1.0D, 0.0D), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 0.5F, 1.0F);
            }

            if (tick == 105) {
                final Location expLoc = animLoc.clone().subtract(0.0D, 1.0D, 0.0D);
                EmpressOfLight.spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, expLoc, 1, 0.0, 0.0, 0.0, 0.0);
            }

            if (tick == 125) {
                if (empressEntity != null && empressEntity.isValid()) {
                    world.playSound(empressEntity.getLocation().add(0.0D, 1.0D, 0.0D), Sound.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 1.5F, 0.8F);
                }
            }


            if (tick >= 101 && tick <= 170 && empressEntity != null && empressEntity.isValid()) {
                final Location bossLoc = empressEntity.getLocation();
                spawnAuroraParticles(bossLoc, isDaytime);
            }

            if (tick == 165) {
                if (empressBoss != null) {
                    empressBoss.setSpawning(false);
                }
                if (empressEntity != null && empressEntity.isValid()) {
                    empressEntity.setInvulnerable(false);
                }
            }

            if (tick >= 170) {
                activeSpawningAltars.remove(altarLoc);
                throw new RuntimeException("Stop task");
            }
        }

        private void spawnAuroraParticles(final Location center, final boolean daytime) {
            final World world = center.getWorld();
            final double yawRad = Math.toRadians(animLoc.getYaw());

            for (int angle = 0; angle < 360; angle += 30) {
                final double rad = Math.toRadians(angle) + yawRad;

                final double cos = Math.cos(rad);
                final double sin = Math.sin(rad);

                final Color color;
                if (daytime) {
                    color = Color.fromRGB(255, 208, 0);
                } else {
                    color = getAuroraColor(angle);
                }

                final Particle.DustOptions dust = new Particle.DustOptions(color, 1.45F);

                final Location pLoc4 = center.clone().add(cos * 4.0D, 0.3D, sin * 4.0D);
                EmpressOfLight.spawnParticleSafe(world, Particle.DUST, pLoc4, 4, 0.9D, 0.75D, 0.9D, 0.0D, dust);

                final Location pLoc3 = center.clone().add(cos * 3.0D, 0.3D, sin * 3.0D);
                EmpressOfLight.spawnParticleSafe(world, Particle.DUST, pLoc3, 4, 0.6D, 0.6D, 0.6D, 0.0D, dust);
            }
        }

        private Color getAuroraColor(final int angle) {
            switch (angle) {
                case 0: return Color.fromRGB(255, 0, 0);
                case 30: return Color.fromRGB(255, 72, 0);
                case 60: return Color.fromRGB(255, 136, 0);
                case 90: return Color.fromRGB(255, 208, 0);
                case 120: return Color.fromRGB(255, 255, 0);
                case 150: return Color.fromRGB(187, 255, 0);
                case 180: return Color.fromRGB(0, 255, 42);
                case 210: return Color.fromRGB(0, 255, 136);
                case 240: return Color.fromRGB(0, 255, 234);
                case 270: return Color.fromRGB(38, 132, 255);
                case 300: return Color.fromRGB(157, 51, 255);
                case 330: return Color.fromRGB(255, 0, 128);
                default: return Color.WHITE;
            }
        }
    }
}
