package dev.cosmojar.stellaritypaper.mobs;

import dev.cosmojar.stellaritypaper.api.ServerVersion;
import dev.cosmojar.stellaritypaper.api.VersionAdapter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class EndMobVariantServiceAdapter1_21_9 implements VersionAdapter {

    private static final ResourceLocation END_VARIANT_LOC = ResourceLocation.fromNamespaceAndPath("stellarity", "end");

    @Override
    public boolean supports(ServerVersion version) {
        return version == ServerVersion.V1_21_9;
    }

    @Override
    public String getAdapterName() {
        return "EndMobVariantService-Adapter (MC 1.21.9)";
    }

    @Override
    public void applyNativeEndVariant(Entity entity) {
        try {
            if (!(entity instanceof CraftEntity craftEntity)) {
                return;
            }
            final net.minecraft.world.entity.Entity nms = craftEntity.getHandle();
            if (!(nms.level() instanceof ServerLevel level)) {
                return;
            }

            if (nms instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf) {
                final var registry = level.registryAccess().lookupOrThrow(Registries.WOLF_VARIANT);
                final var key = ResourceKey.create(Registries.WOLF_VARIANT, END_VARIANT_LOC);
                registry.get(key).ifPresent(wolf::setVariant);
            } else if (nms instanceof net.minecraft.world.entity.animal.Cat cat) {
                final var registry = level.registryAccess().lookupOrThrow(Registries.CAT_VARIANT);
                final var key = ResourceKey.create(Registries.CAT_VARIANT, END_VARIANT_LOC);
                registry.get(key).ifPresent(cat::setVariant);
            }
        } catch (final Throwable ignored) {
        }
    }
}
