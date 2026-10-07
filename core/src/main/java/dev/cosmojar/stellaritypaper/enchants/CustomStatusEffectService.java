package dev.cosmojar.stellaritypaper.enchants;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * Класс для кастомных статус эффектов enchant системы.
 */
public final class CustomStatusEffectService {

    private final VoidedEffectService voidedEffectService;
    private final PrismaticInfernoEffectService prismaticInfernoEffectService;
    private final HolyFlamesEffectService holyFlamesEffectService;

    public CustomStatusEffectService(
            final VoidedEffectService voidedEffectService,
            final PrismaticInfernoEffectService prismaticInfernoEffectService,
            final HolyFlamesEffectService holyFlamesEffectService
    ) {
        this.voidedEffectService = voidedEffectService;
        this.prismaticInfernoEffectService = prismaticInfernoEffectService;
        this.holyFlamesEffectService = holyFlamesEffectService;
    }

    public void applyVoided(final LivingEntity target, final int durationTicks, final int level) {
        voidedEffectService.apply(target, durationTicks, level);
    }

    public void applyPrismaticInferno(final LivingEntity target, final int durationTicks, final Entity source) {
        prismaticInfernoEffectService.apply(target, durationTicks, source);
    }

    public void applyHolyFlames(final LivingEntity target, final int durationTicks, final Entity source) {
        holyFlamesEffectService.apply(target, durationTicks, source);
    }

    public void clear(final LivingEntity target) {
        voidedEffectService.clear(target);
        prismaticInfernoEffectService.clear(target);
        holyFlamesEffectService.clear(target);
    }

    public void clearAll() {
        voidedEffectService.clearAll();
        prismaticInfernoEffectService.clearAll();
        holyFlamesEffectService.clearAll();
    }
}
