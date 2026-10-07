package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class EnchantRuntimeService {

    private final EnchantAttributeBackendService attributeBackendService;
    private final EnchantEventBackendService eventBackendService;
    private final EnchantMobilityBehaviorService mobilityBehaviorService;
    private final EnchantCombatBehaviorService combatBehaviorService;
    private final EnchantTridentBehaviorService tridentBehaviorService;
    private final VoidPendantBehaviorService voidPendantBehaviorService;
    private final CustomStatusEffectService customStatusEffectService;
    private final KaleidoscopeMechanicService kaleidoscopeMechanicService;
    private final DragonbladeMechanicService dragonbladeMechanicService;
    private final HarvesterMechanicService harvesterMechanicService;
    private final HarvesterAbilityUnlockService harvesterAbilityUnlockService;
    private final WeaponMechanicService weaponMechanicService;
    private final ArmorSetBonusService armorSetBonusService;
    private final TrinketBehaviorService trinketBehaviorService;

    public EnchantRuntimeService(
            final EnchantAttributeBackendService attributeBackendService,
            final EnchantEventBackendService eventBackendService,
            final EnchantMobilityBehaviorService mobilityBehaviorService,
            final EnchantCombatBehaviorService combatBehaviorService,
            final EnchantTridentBehaviorService tridentBehaviorService,
            final VoidPendantBehaviorService voidPendantBehaviorService,
            final CustomStatusEffectService customStatusEffectService,
            final KaleidoscopeMechanicService kaleidoscopeMechanicService,
            final DragonbladeMechanicService dragonbladeMechanicService,
            final HarvesterMechanicService harvesterMechanicService,
            final HarvesterAbilityUnlockService harvesterAbilityUnlockService,
            final WeaponMechanicService weaponMechanicService,
            final ArmorSetBonusService armorSetBonusService,
            final TrinketBehaviorService trinketBehaviorService
    ) {
        this.attributeBackendService = attributeBackendService;
        this.eventBackendService = eventBackendService;
        this.mobilityBehaviorService = mobilityBehaviorService;
        this.combatBehaviorService = combatBehaviorService;
        this.tridentBehaviorService = tridentBehaviorService;
        this.voidPendantBehaviorService = voidPendantBehaviorService;
        this.customStatusEffectService = customStatusEffectService;
        this.kaleidoscopeMechanicService = kaleidoscopeMechanicService;
        this.dragonbladeMechanicService = dragonbladeMechanicService;
        this.harvesterMechanicService = harvesterMechanicService;
        this.harvesterAbilityUnlockService = harvesterAbilityUnlockService;
        this.weaponMechanicService = weaponMechanicService;
        this.armorSetBonusService = armorSetBonusService;
        this.trinketBehaviorService = trinketBehaviorService;
    }

    public void recalculate(final Player player) {
        attributeBackendService.recalculate(player);
        mobilityBehaviorService.recalculatePassives(player);
        voidPendantBehaviorService.recalculatePassives(player);
        kaleidoscopeMechanicService.syncPlayer(player);
        harvesterMechanicService.syncPlayerPassives(player);
        weaponMechanicService.syncPlayer(player);
        armorSetBonusService.syncPlayer(player);
        trinketBehaviorService.syncPlayer(player);
    }

    public void cleanup(final Player player) {
        attributeBackendService.clear(player);
        mobilityBehaviorService.clear(player);
        voidPendantBehaviorService.clear(player);
        customStatusEffectService.clear(player);
        kaleidoscopeMechanicService.clear(player);
        harvesterMechanicService.clearPlayer(player);
        weaponMechanicService.clear(player);
        armorSetBonusService.clear(player);
        trinketBehaviorService.clear(player);
    }

    public void onDamage(final EntityDamageByEntityEvent event) {
        eventBackendService.onDamageByEntity(event);
        combatBehaviorService.onDamageByEntity(event);
        harvesterMechanicService.onEntityDamage(event);
        weaponMechanicService.onDamageByEntity(event);
        voidPendantBehaviorService.onDamage(event);
        if (event.getEntity() instanceof Player) {
            voidPendantBehaviorService.onEntityDamage(event);
        }
    }

    public void onEntityDamage(final EntityDamageEvent event) {
        eventBackendService.onEntityDamage(event);
        voidPendantBehaviorService.onEntityDamage(event);
        harvesterMechanicService.onEntityDamage(event);
        weaponMechanicService.onPlayerDamage(event);
        if (event.getEntity() instanceof Player player) {
            armorSetBonusService.onPlayerTakeDamage(event, player);
        }
    }

    public void onPlayerMove(final PlayerMoveEvent event) {
        mobilityBehaviorService.onPlayerMove(event);
        weaponMechanicService.onPlayerMove(event);
    }

    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        tridentBehaviorService.onProjectileLaunch(event);
        weaponMechanicService.onProjectileLaunch(event);
    }

    public void onShootBow(final EntityShootBowEvent event) {
        combatBehaviorService.onShootBow(event);
        weaponMechanicService.onShootBow(event);
    }

    public void onProjectileHit(final ProjectileHitEvent event) {
        combatBehaviorService.onProjectileHit(event);
        weaponMechanicService.onProjectileHit(event);
    }

    public void onInteract(final PlayerInteractEvent event) {
        weaponMechanicService.onInteract(event);
    }

    public void onEntityDeath(final EntityDeathEvent event) {
        harvesterMechanicService.onEntityDeath(event);
        harvesterAbilityUnlockService.onEntityDeath(event);
    }

    public void onEntityResurrect(final EntityResurrectEvent event) {
        harvesterMechanicService.onEntityResurrect(event);
    }

    public void cleanupAll() {
        tridentBehaviorService.clearAll();
        voidPendantBehaviorService.clearAll();
        customStatusEffectService.clearAll();
        kaleidoscopeMechanicService.clearAll();
        dragonbladeMechanicService.clearAll();
        harvesterMechanicService.clearAll();
        weaponMechanicService.clearAll();
    }
}
