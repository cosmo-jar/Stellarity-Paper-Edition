package dev.cosmojar.stellaritypaper.mobs.boss.dragon;

import dev.cosmojar.stellaritypaper.mobs.boss.BossManager;
import dev.cosmojar.stellaritypaper.mobs.boss.StellarityBoss;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class StellarityDragon implements StellarityBoss {

    private final Plugin plugin;
    private final EnderDragon dragon;
    private final BossManager bossManager;
    public StellarityDragon(Plugin plugin, EnderDragon dragon, BossManager bossManager, dev.cosmojar.stellaritypaper.text.MessageService messageService) {
        this.plugin = plugin;
        this.dragon = dragon;
        this.bossManager = bossManager;

        double health = plugin.getConfig().getDouble("boss.ender_dragon.max-health", 300.0);
        DragonEncounter existing = DragonEncounter.getEncounter(dragon.getUniqueId());
        if (existing == null) {
            new DragonEncounter(plugin, dragon, health, messageService);
        }

        bossManager.registerBoss(this);
    }

    public DragonEncounter getEncounter() {
        return dragon != null ? DragonEncounter.getEncounter(dragon.getUniqueId()) : null;
    }

    @Override
    public LivingEntity getBaseEntity() {
        return dragon;
    }

    @Override
    public ItemDisplay getDisplayEntity() {
        return null;
    }

    @Override
    public boolean isAlive() {
        return dragon != null && dragon.isValid() && !dragon.isDead();
    }

    @Override
    public void tick() {
    }

    @Override
    public void cleanup() {
        if (dragon != null) {
            DragonEncounter.removeEncounter(dragon.getUniqueId());
        }
    }

    @Override
    public boolean isInvulnerable() {
        DragonEncounter enc = getEncounter();
        return enc != null && enc.isInvulnerable();
    }

    @Override
    public void damageShield(double damage) {
    }
}
