package dev.cosmojar.stellaritypaper.mechanics.fishing;

import org.bukkit.entity.FishHook;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class VoidFishingSession {

    private final UUID playerUuid;
    private final FishHook hook;
    private final long startedAt;
    
    private boolean initialized;
    private double originalY;
    private double approachAngle;
    private long biteTime;
    private long biteEndTime;
    private ItemStack pendingLoot;
    private boolean bit;
    private boolean active;

    public VoidFishingSession(
            final UUID playerUuid,
            final FishHook hook,
            final long startedAt
    ) {
        this.playerUuid = playerUuid;
        this.hook = hook;
        this.startedAt = startedAt;
        this.initialized = false;
        this.originalY = 0.0D;
        this.approachAngle = java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 2.0D * Math.PI;
        this.bit = false;
        this.active = true;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public FishHook hook() {
        return hook;
    }

    public long startedAt() {
        return startedAt;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    public long biteTime() {
        return biteTime;
    }

    public void setBiteTime(final long biteTime) {
        this.biteTime = biteTime;
    }

    public long biteEndTime() {
        return biteEndTime;
    }

    public void setBiteEndTime(final long biteEndTime) {
        this.biteEndTime = biteEndTime;
    }

    public ItemStack pendingLoot() {
        return pendingLoot;
    }

    public void setPendingLoot(final ItemStack pendingLoot) {
        this.pendingLoot = pendingLoot;
    }

    public boolean isBit() {
        return bit;
    }

    public void setBit(final boolean bit) {
        this.bit = bit;
    }

    public double originalY() {
        return originalY;
    }

    public void setOriginalY(final double originalY) {
        this.originalY = originalY;
    }

    public double approachAngle() {
        return approachAngle;
    }

    public void setApproachAngle(final double approachAngle) {
        this.approachAngle = approachAngle;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }
}
