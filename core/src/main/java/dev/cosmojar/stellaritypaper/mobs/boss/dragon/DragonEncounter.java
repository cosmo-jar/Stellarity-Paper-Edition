package dev.cosmojar.stellaritypaper.mobs.boss.dragon;

import dev.cosmojar.stellaritypaper.text.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


public class DragonEncounter {

    public static final double[][] TOWER_COORDS = new double[][] {
        {63.5, 101.0, 0.5},
        {50.5, 106.0, 36.5},
        {18.5, 95.0, 59.5},
        {-18.5, 107.0, 59.5},
        {-50.5, 106.0, 36.5},
        {-62.5, 94.0, 0.5},
        {-50.5, 101.0, -38.5},
        {-18.5, 97.0, -59.5},
        {18.5, 88.0, -59.5},
        {50.5, 96.0, -38.5}
    };

    private static final Map<UUID, DragonEncounter> ENCOUNTERS = new HashMap<>();

    public static DragonEncounter getEncounter(UUID dragonId) {
        return ENCOUNTERS.get(dragonId);
    }

    public static void removeEncounter(UUID dragonId) {
        DragonEncounter enc = ENCOUNTERS.remove(dragonId);
        if (enc != null) {
            enc.cleanup();
        }
    }

    public static Map<UUID, DragonEncounter> getEncounters() {
        return ENCOUNTERS;
    }

    private final Plugin plugin;
    private final MessageService messageService;
    private final UUID encounterId;
    private final EnderDragon dragon;
    private final NamespacedKey encounterKey;
    private final NamespacedKey crystalKey;
    private final NamespacedKey shulkerKey;
    private final NamespacedKey dragonBreathKey;

    private net.kyori.adventure.bossbar.BossBar fallbackDragonBossBar;
    private net.kyori.adventure.bossbar.BossBar crystalBossBar;

    private final boolean[] towerCrystalsAlive = new boolean[10];

    private boolean reviveCrystalsUsed = false;
    private boolean isInvulnerable = true;
    private int aliveCrystalsCount = 10;
    private int perchCooldown = 20;
    private int chainfiringRemaining = 0;
    private int chainfireCooldown = 30;
    private int shulkerHellCooldown = 15;
    private int roarBreathCooldown = 20;

    private BukkitTask loopTask;

    public DragonEncounter(Plugin plugin, EnderDragon dragon, double maxHealth, MessageService messageService) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.dragon = dragon;
        this.encounterId = UUID.randomUUID();

        this.encounterKey = new NamespacedKey(plugin, "encounter_id");
        this.crystalKey = new NamespacedKey(plugin, "dragon_crystal");
        this.shulkerKey = new NamespacedKey(plugin, "dragon_shulker");
        this.dragonBreathKey = new NamespacedKey(plugin, "dragon_breath");

        Arrays.fill(towerCrystalsAlive, true);

        removeEncounter(dragon.getUniqueId());

        PersistentDataContainer pdc = dragon.getPersistentDataContainer();
        pdc.set(encounterKey, PersistentDataType.STRING, encounterId.toString());

        applyDragonAttributes(maxHealth);

        setupBossBars();

        scanTowerCrystals();

        ENCOUNTERS.put(dragon.getUniqueId(), this);

        startEncounterTask();
    }

    public UUID getEncounterId() {
        return encounterId;
    }

    private void applyDragonAttributes(double maxHealth) {
        AttributeInstance hp = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(maxHealth);
        dragon.setHealth(maxHealth);

        AttributeInstance armor = dragon.getAttribute(Attribute.ARMOR);
        if (armor != null) armor.setBaseValue(8.0);

        AttributeInstance armorTough = dragon.getAttribute(Attribute.ARMOR_TOUGHNESS);
        if (armorTough != null) armorTough.setBaseValue(4.0);

        AttributeInstance knockback = dragon.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) knockback.setBaseValue(1.0);

        AttributeInstance range = dragon.getAttribute(Attribute.FOLLOW_RANGE);
        if (range != null) range.setBaseValue(80.0);

        dragon.getEquipment().setItemInMainHand(new ItemStack(Material.TOTEM_OF_UNDYING));
    }

    private void setupBossBars() {
        Component dragonTitle = dragon.customName() != null ? dragon.customName() : Component.translatable("entity.minecraft.ender_dragon");
        fallbackDragonBossBar = net.kyori.adventure.bossbar.BossBar.bossBar(
                dragonTitle,
                1.0f,
                net.kyori.adventure.bossbar.BossBar.Color.WHITE,
                net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS
        );

        Component crystalTitle = messageService.message("dragon.crystals_left", Map.of("count", String.valueOf(aliveCrystalsCount)));
        crystalBossBar = net.kyori.adventure.bossbar.BossBar.bossBar(
                crystalTitle,
                1.0f,
                net.kyori.adventure.bossbar.BossBar.Color.PURPLE,
                net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS
        );

        setInvulnerableState(true);
    }

    public void scanTowerCrystals() {
        World world = dragon.getWorld();

        for (int i = 0; i < TOWER_COORDS.length; i++) {
            double[] coord = TOWER_COORDS[i];
            Location loc = new Location(world, coord[0], coord[1], coord[2]);
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;

            if (!world.isChunkLoaded(cx, cz)) {
                world.loadChunk(cx, cz);
            }

            boolean found = false;
            for (org.bukkit.entity.Entity entity : world.getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
                if (entity instanceof EnderCrystal crystal && crystal.isValid() && !crystal.isDead()) {
                    crystal.getPersistentDataContainer().set(crystalKey, PersistentDataType.BYTE, (byte) 1);
                    crystal.getPersistentDataContainer().set(encounterKey, PersistentDataType.STRING, encounterId.toString());
                    found = true;
                    break;
                }
            }
            towerCrystalsAlive[i] = found;
        }
        recalculateCrystalCount();
    }

    private void checkTowerCrystalsStatus() {
        World world = dragon.getWorld();

        for (int i = 0; i < TOWER_COORDS.length; i++) {
            if (!towerCrystalsAlive[i]) continue;

            double[] coord = TOWER_COORDS[i];
            Location loc = new Location(world, coord[0], coord[1], coord[2]);
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;

            if (world.isChunkLoaded(cx, cz)) {
                boolean found = false;
                for (org.bukkit.entity.Entity entity : world.getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
                    if (entity instanceof EnderCrystal crystal && crystal.isValid() && !crystal.isDead()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    towerCrystalsAlive[i] = false;
                }
            }
        }
        recalculateCrystalCount();
    }

    private void recalculateCrystalCount() {
        int count = 0;
        for (boolean alive : towerCrystalsAlive) {
            if (alive) count++;
        }
        updateCrystalCount(count);
    }

    public void updateCrystalCount(int count) {
        this.aliveCrystalsCount = Math.max(0, count);
        float progress = Math.min(1.0f, Math.max(0.0f, (float) aliveCrystalsCount / 10.0f));
        crystalBossBar.progress(progress);

        Component crystalTitle = messageService.message("dragon.crystals_left", Map.of("count", String.valueOf(aliveCrystalsCount)));
        crystalBossBar.name(crystalTitle);

        if (aliveCrystalsCount > 0 && !isInvulnerable) {
            setInvulnerableState(true);
        } else if (aliveCrystalsCount == 0 && isInvulnerable) {
            setInvulnerableState(false);
        }
    }

    public void onCrystalDestroyed(EnderCrystal crystal) {
        Location loc = crystal.getLocation();
        double minDistance = Double.MAX_VALUE;
        int closestIndex = -1;

        for (int i = 0; i < TOWER_COORDS.length; i++) {
            double[] coord = TOWER_COORDS[i];
            double distSq = loc.distanceSquared(new Location(crystal.getWorld(), coord[0], coord[1], coord[2]));
            if (distSq < minDistance) {
                minDistance = distSq;
                closestIndex = i;
            }
        }

        if (closestIndex != -1 && minDistance <= 36.0) {
            towerCrystalsAlive[closestIndex] = false;
        }
        recalculateCrystalCount();
    }

    public void setInvulnerableState(boolean invulnerable) {
        this.isInvulnerable = invulnerable;
        dragon.setInvulnerable(invulnerable);
        dragon.setGlowing(invulnerable);

        World world = dragon.getWorld();

        BarColor targetColor = invulnerable ? BarColor.WHITE : BarColor.PINK;
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle != null && battle.getBossBar() != null) {
            battle.getBossBar().setColor(targetColor);
        }
        if (fallbackDragonBossBar != null) {
            fallbackDragonBossBar.color(invulnerable ? net.kyori.adventure.bossbar.BossBar.Color.WHITE : net.kyori.adventure.bossbar.BossBar.Color.PINK);
        }

        Sound s1 = invulnerable ? Sound.BLOCK_CONDUIT_ACTIVATE : Sound.BLOCK_CONDUIT_DEACTIVATE;
        Sound s2 = invulnerable ? Sound.BLOCK_BEACON_ACTIVATE : Sound.BLOCK_BEACON_DEACTIVATE;
        Location center = new Location(world, 0, 64, 0);

        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= 90000) {
                p.playSound(p.getLocation(), s1, SoundCategory.HOSTILE, 1.0f, 1.0f);
                p.playSound(p.getLocation(), s2, SoundCategory.HOSTILE, 1.0f, 1.0f);
            }
        }
    }

    private void startEncounterTask() {
        loopTask = new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!dragon.isValid() || dragon.isDead()) {
                    cleanup();
                    this.cancel();
                    return;
                }

                tick++;

                if (tick % 10 == 0) {
                    checkTowerCrystalsStatus();
                }

                updateBossBarViewers();

                if (isInvulnerable && tick % 5 == 0) {
                    spawnCrystalParticles();
                }

                if (tick % 20 == 0) {
                    tickCooldowns();
                    executeAI();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void updateBossBarViewers() {
        World world = dragon.getWorld();
        Location islandCenter = new Location(world, 0.5, 64.0, 0.5);

        AttributeInstance hpAttr = dragon.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = hpAttr != null ? hpAttr.getValue() : 300.0;
        double hpProgress = Math.min(1.0, Math.max(0.0, dragon.getHealth() / maxHp));

        BarColor targetColor = isInvulnerable ? BarColor.WHITE : BarColor.PINK;
        net.kyori.adventure.bossbar.BossBar.Color advTargetColor = isInvulnerable ? net.kyori.adventure.bossbar.BossBar.Color.WHITE : net.kyori.adventure.bossbar.BossBar.Color.PINK;
        
        DragonBattle battle = world.getEnderDragonBattle();

        boolean vanillaIsManaging = false;
        if (battle != null && battle.getBossBar() != null) {
            EnderDragon battleDragon = battle.getEnderDragon();
            if (battleDragon != null && battleDragon.getUniqueId().equals(dragon.getUniqueId())) {
                vanillaIsManaging = true;
            }
        }

        if (vanillaIsManaging) {
            if (battle.getBossBar() != null) {
                battle.getBossBar().setProgress(hpProgress);
                battle.getBossBar().setColor(targetColor);
                battle.getBossBar().setVisible(true);
            }
        } else if (fallbackDragonBossBar != null) {
            fallbackDragonBossBar.progress((float) hpProgress);
            fallbackDragonBossBar.color(advTargetColor);
        }

        double maxDistSq = 90000.0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean inSameWorld = p.getWorld().equals(world);
            boolean inRange = inSameWorld && p.getLocation().distanceSquared(islandCenter) <= maxDistSq;

            if (inRange) {
                if (vanillaIsManaging) {
                    p.hideBossBar(fallbackDragonBossBar);
                } else {
                    p.showBossBar(fallbackDragonBossBar);
                }

                if (aliveCrystalsCount > 0) {
                    p.showBossBar(crystalBossBar);
                } else {
                    p.hideBossBar(crystalBossBar);
                }
            } else {
                hideFromPlayer(p);
            }
        }
    }

    public void hideFromPlayer(Player p) {
        World world = dragon.getWorld();
        if (world != null) {
            DragonBattle battle = world.getEnderDragonBattle();
            if (battle != null && battle.getBossBar() != null) {
                battle.getBossBar().removePlayer(p);
            }
        }
        if (fallbackDragonBossBar != null) {
            p.hideBossBar(fallbackDragonBossBar);
        }
        p.hideBossBar(crystalBossBar);
    }

    private void spawnCrystalParticles() {
        World world = dragon.getWorld();
        for (int i = 0; i < TOWER_COORDS.length; i++) {
            if (towerCrystalsAlive[i]) {
                double[] coord = TOWER_COORDS[i];
                Location loc = new Location(world, coord[0], coord[1] + 1.0, coord[2]);
                if (world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    DragonRespawnManager.spawnParticleSafe(world, Particle.PORTAL, loc, 3, 0.6, 0.6, 0.6, 0.02);
                    DragonRespawnManager.spawnParticleSafe(world, Particle.DRAGON_BREATH, loc, 2, 0.6, 0.6, 0.6, 0.02);
                }
            }
        }
    }

    private void tickCooldowns() {
        if (perchCooldown > 0) perchCooldown--;
        if (shulkerHellCooldown > 0) shulkerHellCooldown--;
        if (roarBreathCooldown > 0) roarBreathCooldown--;
        if (chainfireCooldown > 0) chainfireCooldown--;

        if (chainfiringRemaining > 0) {
            shootChainfireBall();
            chainfiringRemaining--;
        }
    }

    private void executeAI() {
        AttributeInstance hpAttr = dragon.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = hpAttr != null ? hpAttr.getValue() : 300.0;
        double healthPct = dragon.getHealth() / maxHp;

        if (healthPct < 0.50 && !reviveCrystalsUsed) {
            triggerReviveCrystals();
            return;
        }

        Player target = findTarget();
        Location loc = dragon.getLocation();
        boolean isPerching = Math.abs(loc.getX()) <= 12 && Math.abs(loc.getZ()) <= 12;

        if (isPerching) {
            if (perchCooldown <= 0) {
                triggerShulkerSummon(healthPct);
                perchCooldown = 25;
            }
            if (roarBreathCooldown <= 0) {
                triggerRoarBreath();
            }
        } else {
            if (target != null) {
                if (shulkerHellCooldown <= 0) {
                    triggerShulkerHell(target);
                } else if (chainfireCooldown <= 0 && chainfiringRemaining <= 0) {
                    triggerChainfiring(healthPct);
                    chainfireCooldown = healthPct < 0.33 ? 20 : 35;
                }
            }
        }
    }

    public void triggerReviveCrystals() {
        this.reviveCrystalsUsed = true;
        AttributeInstance hpAttr = dragon.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = hpAttr != null ? hpAttr.getValue() : 300.0;
        dragon.setHealth(Math.max(dragon.getHealth(), maxHp * 0.50));
        if (dragon.getPhase() == EnderDragon.Phase.DYING) {
            dragon.setPhase(EnderDragon.Phase.CIRCLING);
        }
        World world = dragon.getWorld();
        world.playSound(dragon.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 6.0f, 1.0f);

        List<Integer> towerIndices = new ArrayList<>();
        for (int i = 0; i < TOWER_COORDS.length; i++) towerIndices.add(i);
        Collections.shuffle(towerIndices);

        int reviveCount = Math.min(6, Math.max(3, 3 + world.getDifficulty().getValue()));

        for (int i = 0; i < reviveCount && i < towerIndices.size(); i++) {
            int towerIdx = towerIndices.get(i);
            double[] coord = TOWER_COORDS[towerIdx];
            Location spawnLoc = new Location(world, coord[0], coord[1], coord[2]);

            EnderCrystal crystal = (EnderCrystal) world.spawnEntity(spawnLoc, EntityType.END_CRYSTAL);
            crystal.setShowingBottom(true);
            crystal.getPersistentDataContainer().set(crystalKey, PersistentDataType.BYTE, (byte) 1);
            crystal.getPersistentDataContainer().set(encounterKey, PersistentDataType.STRING, encounterId.toString());

            towerCrystalsAlive[towerIdx] = true;

            DragonRespawnManager.spawnParticleSafe(world, Particle.EXPLOSION_EMITTER, spawnLoc, 1, 0, 0, 0, 0);
            DragonRespawnManager.spawnParticleSafe(world, Particle.DRAGON_BREATH, spawnLoc, 50, 1.0, 1.0, 1.0, 0.05);

            spawnEndRodWave(spawnLoc);
        }

        recalculateCrystalCount();
        setInvulnerableState(true);
    }

    private void spawnEndRodWave(Location center) {
        World world = center.getWorld();
        new BukkitRunnable() {
            private int step = 0;

            @Override
            public void run() {
                step++;
                if (step > 24) {
                    this.cancel();
                    return;
                }

                double angleStep = Math.PI / 12.0;
                for (int i = 0; i < 24; i++) {
                    double angle = i * angleStep;
                    double x = Math.cos(angle) * (step * 8.0);
                    double z = Math.sin(angle) * (step * 8.0);
                    Location pLoc = center.clone().add(x, 0, z);
                    DragonRespawnManager.spawnParticleSafe(world, Particle.END_ROD, pLoc, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void triggerShulkerSummon(double healthPct) {
        World world = dragon.getWorld();

        Location centerPortal = new Location(world, 0.5, 63.0, 0.5);
        int existingShulkers = 0;
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(centerPortal, 12.0, 10.0, 12.0)) {
            if (entity instanceof Shulker shulker && shulker.getPersistentDataContainer().has(shulkerKey, PersistentDataType.BYTE)) {
                existingShulkers++;
            }
        }
        if (existingShulkers >= 3) return;

        Location[] shulkerSpots = new Location[] {
            new Location(world, 3.5, 63.0, 0.5),
            new Location(world, -2.5, 63.0, 0.5),
            new Location(world, 0.5, 63.0, 3.5),
            new Location(world, 0.5, 63.0, -2.5),
            new Location(world, 2.5, 63.0, 2.5)
        };

        int count = healthPct < 0.33 ? 5 : (healthPct < 0.66 ? 4 : 3);
        count = Math.max(0, count - existingShulkers);

        Player target = findTarget();

        for (int i = 0; i < count && i < shulkerSpots.length; i++) {
            Location spawnLoc = shulkerSpots[i];

            Shulker shulker = (Shulker) world.spawnEntity(spawnLoc, EntityType.SHULKER);
            shulker.getPersistentDataContainer().set(shulkerKey, PersistentDataType.BYTE, (byte) 1);
            shulker.getPersistentDataContainer().set(encounterKey, PersistentDataType.STRING, encounterId.toString());

            shulker.setAI(true);
            shulker.setInvulnerable(false);
            if (target != null) shulker.setTarget(target);

            AttributeInstance hp = shulker.getAttribute(Attribute.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(20.0);
            shulker.setHealth(20.0);

            AttributeInstance armor = shulker.getAttribute(Attribute.ARMOR_TOUGHNESS);
            if (armor != null) armor.setBaseValue(4.0);

            DragonRespawnManager.spawnParticleSafe(world, Particle.END_ROD, spawnLoc, 20, 0.5, 0.5, 0.5, 0.05);
            world.playSound(spawnLoc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }

    public void triggerChainfiring(double healthPct) {
        if (chainfiringRemaining > 0) return;
        this.chainfiringRemaining = healthPct <= 0.33 ? 24 : 12;
    }

    private void shootChainfireBall() {
        Player target = findTarget();
        if (target == null) return;

        World world = dragon.getWorld();
        Location head = dragon.getLocation().add(0, -2, 0);
        Vector dir = target.getLocation().toVector().subtract(head.toVector()).normalize();

        DragonFireball fireball = (DragonFireball) world.spawnEntity(head.add(dir.clone().multiply(3)), EntityType.DRAGON_FIREBALL);
        fireball.setDirection(dir);
        fireball.getPersistentDataContainer().set(encounterKey, PersistentDataType.STRING, encounterId.toString());
        world.playSound(head, Sound.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.HOSTILE, 3.0f, 1.2f);
    }

    public void triggerShulkerHell(Player target) {
        if (shulkerHellCooldown > 0) return;
        shulkerHellCooldown = 15;

        World world = dragon.getWorld();
        int count = 5 + (world.getDifficulty().getValue() * 2);

        for (int i = 0; i < count; i++) {
            Location loc = dragon.getLocation().add(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 6,
                    -2,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 6
            );
            ShulkerBullet bullet = (ShulkerBullet) world.spawnEntity(loc, EntityType.SHULKER_BULLET);
            bullet.setTarget(target);
            bullet.getPersistentDataContainer().set(encounterKey, PersistentDataType.STRING, encounterId.toString());
            DragonRespawnManager.spawnParticleSafe(world, Particle.DRAGON_BREATH, loc, 5, 0.2, 0.2, 0.2, 0.01);
        }
        world.playSound(dragon.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.HOSTILE, 3.0f, 1.5f);
    }

    public void triggerRoarBreath() {
        if (roarBreathCooldown > 0) return;
        roarBreathCooldown = 20;

        World world = dragon.getWorld();
        world.playSound(dragon.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 5.0f, 0.5f);

        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(dragon.getLocation(), 24.0, 24.0, 24.0)) {
            if (entity instanceof AreaEffectCloud cloud) {
                if (cloud.getParticle() == Particle.DRAGON_BREATH) {
                    cloud.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0, false, false), true);
                    cloud.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 240, 0, false, false), true);
                }
            }
        }
    }

    private Player findTarget() {
        if (dragon.getTarget() instanceof Player p) return p;
        List<Player> players = dragon.getWorld().getPlayers();
        if (players.isEmpty()) return null;
        return players.get(ThreadLocalRandom.current().nextInt(players.size()));
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    public boolean isReviveCrystalsUsed() {
        return reviveCrystalsUsed;
    }

    public EnderDragon getDragon() {
        return dragon;
    }

    public void cleanup() {
        if (loopTask != null) {
            loopTask.cancel();
            loopTask = null;
        }
        World world = dragon.getWorld();
        if (world != null) {
            DragonBattle battle = world.getEnderDragonBattle();
            if (battle != null && battle.getBossBar() != null) {
                battle.getBossBar().removeAll();
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (fallbackDragonBossBar != null) {
                p.hideBossBar(fallbackDragonBossBar);
            }
            p.hideBossBar(crystalBossBar);
        }
        ENCOUNTERS.remove(dragon.getUniqueId());
    }
}
