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

    // ==================== 原始速度 ====================
    @Nullable Vec3 jujutsuAddon$getOriginalVelocity();
    void jujutsuAddon$setOriginalVelocity(@Nullable Vec3 velocity);

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

    // ★★★ 新增：客户端插值用 ★★★

    /**
     * 服务端设置的目标速度（通过 SynchedEntityData 同步到客户端）
     */
    Vec3 jujutsuAddon$getTargetVelocity();
    void jujutsuAddon$setTargetVelocity(Vec3 velocity);

    /**
     * 客户端当前的插值速度（仅客户端使用）
     */
    Vec3 jujutsuAddon$getClientVelocity();
    void jujutsuAddon$setClientVelocity(Vec3 velocity);

    /**
     * 客户端渲染用的平滑位置（仅客户端使用）
     */
    Vec3 jujutsuAddon$getRenderPosition();
    void jujutsuAddon$setRenderPosition(Vec3 position);
}
