package dev.cosmojar.stellaritypaper.mobs.boss;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;

/**
 * Общий интерфейс для всех боссов.
 */
public interface StellarityBoss {


    LivingEntity getBaseEntity();


    ItemDisplay getDisplayEntity();


    boolean isAlive();

    /**
     * Выполняет периодический тик логики босса (ИИ, атаки, эффекты).
     * Вызывается раз в тик менеджером BossManager.
     */
    void tick();

    /**
     * Метод очистки сущностей босса при его смерти, деспавне или выгрузке.
     */
    void cleanup();


    boolean isInvulnerable();


    void damageShield(double damage);

    default org.bukkit.Location getAltarLocation() {
        return null;
    }
}
