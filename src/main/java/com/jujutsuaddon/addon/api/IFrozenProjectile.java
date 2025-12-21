package com.jujutsuaddon.addon.api;

import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import java.util.UUID;

public interface IFrozenProjectile {

    // ==================== 控制状态 ====================
    boolean jujutsuAddon$isControlled();
    void jujutsuAddon$setControlled(boolean controlled);

    // ==================== 速度控制 ====================
    float jujutsuAddon$getSpeedMultiplier();
    void jujutsuAddon$setSpeedMultiplier(float multiplier);

    // ==================== 原始速度（发射时） ====================
    @Nullable Vec3 jujutsuAddon$getOriginalVelocity();
    void jujutsuAddon$setOriginalVelocity(@Nullable Vec3 velocity);

    // ==================== ★★★ 捕获时的速度（释放时用）★★★ ====================
    @Nullable Vec3 jujutsuAddon$getCaptureVelocity();
    void jujutsuAddon$setCaptureVelocity(@Nullable Vec3 velocity);

    // ==================== 原始 Power（火焰弹等用）====================
    @Nullable Vec3 jujutsuAddon$getOriginalPower();
    void jujutsuAddon$setOriginalPower(@Nullable Vec3 power);

    // ==================== 原始捕获位置 ====================
    @Nullable Vec3 jujutsuAddon$getOriginalCapturePosition();
    void jujutsuAddon$setOriginalCapturePosition(@Nullable Vec3 position);

    // ==================== 当前飞行方向 ====================
    @Nullable Vec3 jujutsuAddon$getCurrentDirection();
    void jujutsuAddon$setCurrentDirection(@Nullable Vec3 direction);

    // ==================== 冻结位置 ====================
    @Nullable Vec3 jujutsuAddon$getFrozenPosition();
    void jujutsuAddon$setFrozenPosition(@Nullable Vec3 position);

    // ==================== 控制者 ====================
    @Nullable UUID jujutsuAddon$getFreezeOwner();
    void jujutsuAddon$setFreezeOwner(@Nullable UUID owner);

    // ==================== 旋转锁定 ====================
    void jujutsuAddon$lockRotation(float yaw, float pitch);
    float jujutsuAddon$getLockedYaw();
    float jujutsuAddon$getLockedPitch();
    boolean jujutsuAddon$isRotationLocked();

    // ==================== 距离参数 ====================
    float jujutsuAddon$getStopDistance();
    void jujutsuAddon$setStopDistance(float distance);
    float jujutsuAddon$getMaxRange();
    void jujutsuAddon$setMaxRange(float range);

    // ==================== 客户端插值用 ====================
    Vec3 jujutsuAddon$getTargetVelocity();
    void jujutsuAddon$setTargetVelocity(Vec3 velocity);

    Vec3 jujutsuAddon$getClientVelocity();
    void jujutsuAddon$setClientVelocity(Vec3 velocity);

    Vec3 jujutsuAddon$getRenderPosition();
    void jujutsuAddon$setRenderPosition(Vec3 position);
}
