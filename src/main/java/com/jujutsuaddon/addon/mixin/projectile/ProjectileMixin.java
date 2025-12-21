package com.jujutsuaddon.addon.mixin.projectile;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile.ControlledProjectileTick;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@Mixin(Projectile.class)
public abstract class ProjectileMixin extends Entity implements IFrozenProjectile {
// ==================== 释放速度 ====================
    // 添加字段
    @Unique private Vec3 jujutsuAddon$captureVelocity = null;
    // 添加方法实现
    @Override
    @Nullable
    public Vec3 jujutsuAddon$getCaptureVelocity() {
        return jujutsuAddon$captureVelocity;
    }
    @Override
    public void jujutsuAddon$setCaptureVelocity(@Nullable Vec3 velocity) {
        this.jujutsuAddon$captureVelocity = velocity;
    }

    // ==================== 同步数据 ====================
    @Unique
    private static final EntityDataAccessor<Boolean> DATA_CONTROLLED =
            SynchedEntityData.defineId(Projectile.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private static final EntityDataAccessor<Float> DATA_SPEED_MULTIPLIER =
            SynchedEntityData.defineId(Projectile.class, EntityDataSerializers.FLOAT);

    @Unique
    private static final EntityDataAccessor<Float> DATA_STOP_DISTANCE =
            SynchedEntityData.defineId(Projectile.class, EntityDataSerializers.FLOAT);

    @Unique
    private static final EntityDataAccessor<Optional<UUID>> DATA_FREEZE_OWNER =
            SynchedEntityData.defineId(Projectile.class, EntityDataSerializers.OPTIONAL_UUID);

    @Unique
    private static final EntityDataAccessor<Float> DATA_MAX_RANGE =
            SynchedEntityData.defineId(Projectile.class, EntityDataSerializers.FLOAT);

    @Unique
    private static final EntityDataAccessor<Float> DATA_TARGET_VEL_X =
            SynchedEntityData.defineId(Projectile.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> DATA_TARGET_VEL_Y =
            SynchedEntityData.defineId(Projectile.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Float> DATA_TARGET_VEL_Z =
            SynchedEntityData.defineId(Projectile.class, EntityDataSerializers.FLOAT);

    // ==================== 仅服务端/客户端本地使用 ====================
    @Unique private Vec3 jujutsuAddon$originalVelocity = null;
    @Unique private Vec3 jujutsuAddon$originalPower = null;
    @Unique private Vec3 jujutsuAddon$originalCapturePosition = null;
    @Unique private Vec3 jujutsuAddon$currentDirection = null;
    @Unique private Vec3 jujutsuAddon$frozenPosition = null;
    @Unique private float jujutsuAddon$lockedYaw = 0;
    @Unique private float jujutsuAddon$lockedPitch = 0;
    @Unique private boolean jujutsuAddon$rotationLocked = false;

    @Unique private Vec3 jujutsuAddon$clientVelocity = Vec3.ZERO;
    @Unique private Vec3 jujutsuAddon$renderPosition = null;

    public ProjectileMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jujutsuAddon$onInit(CallbackInfo ci) {
        if (!this.entityData.hasItem(DATA_CONTROLLED)) {
            this.entityData.define(DATA_CONTROLLED, false);
            this.entityData.define(DATA_SPEED_MULTIPLIER, 1.0f);
            this.entityData.define(DATA_STOP_DISTANCE, 0.5f);
            this.entityData.define(DATA_FREEZE_OWNER, Optional.empty());
            this.entityData.define(DATA_MAX_RANGE, 10.0f);
            this.entityData.define(DATA_TARGET_VEL_X, 0.0f);
            this.entityData.define(DATA_TARGET_VEL_Y, 0.0f);
            this.entityData.define(DATA_TARGET_VEL_Z, 0.0f);
        }
    }

    // ==================== 基础接口实现 ====================

    @Override
    public boolean jujutsuAddon$isControlled() {
        return this.entityData.get(DATA_CONTROLLED);
    }

    @Override
    public void jujutsuAddon$setControlled(boolean controlled) {
        this.entityData.set(DATA_CONTROLLED, controlled);
        if (!controlled) {
            this.entityData.set(DATA_SPEED_MULTIPLIER, 1.0f);
            this.entityData.set(DATA_STOP_DISTANCE, 0.5f);
            this.entityData.set(DATA_FREEZE_OWNER, Optional.empty());
            this.entityData.set(DATA_MAX_RANGE, 10.0f);
            this.entityData.set(DATA_TARGET_VEL_X, 0.0f);
            this.entityData.set(DATA_TARGET_VEL_Y, 0.0f);
            this.entityData.set(DATA_TARGET_VEL_Z, 0.0f);
            this.jujutsuAddon$frozenPosition = null;
            this.jujutsuAddon$rotationLocked = false;
            this.jujutsuAddon$clientVelocity = Vec3.ZERO;
            this.jujutsuAddon$renderPosition = null;
        }
    }

    @Override
    public float jujutsuAddon$getSpeedMultiplier() {
        return this.entityData.get(DATA_SPEED_MULTIPLIER);
    }

    @Override
    public void jujutsuAddon$setSpeedMultiplier(float multiplier) {
        this.entityData.set(DATA_SPEED_MULTIPLIER, Math.max(0, Math.min(1, multiplier)));
    }

    @Override
    public float jujutsuAddon$getStopDistance() {
        return this.entityData.get(DATA_STOP_DISTANCE);
    }

    @Override
    public void jujutsuAddon$setStopDistance(float distance) {
        this.entityData.set(DATA_STOP_DISTANCE, Math.max(0.3f, distance));
    }

    @Override
    @Nullable
    public UUID jujutsuAddon$getFreezeOwner() {
        return this.entityData.get(DATA_FREEZE_OWNER).orElse(null);
    }

    @Override
    public void jujutsuAddon$setFreezeOwner(@Nullable UUID owner) {
        this.entityData.set(DATA_FREEZE_OWNER, Optional.ofNullable(owner));
    }

    @Override
    public float jujutsuAddon$getMaxRange() {
        return this.entityData.get(DATA_MAX_RANGE);
    }

    @Override
    public void jujutsuAddon$setMaxRange(float range) {
        this.entityData.set(DATA_MAX_RANGE, Math.max(1.0f, range));
    }

    // ==================== 目标速度（同步） ====================

    @Override
    public Vec3 jujutsuAddon$getTargetVelocity() {
        return new Vec3(
                this.entityData.get(DATA_TARGET_VEL_X),
                this.entityData.get(DATA_TARGET_VEL_Y),
                this.entityData.get(DATA_TARGET_VEL_Z)
        );
    }

    @Override
    public void jujutsuAddon$setTargetVelocity(Vec3 velocity) {
        this.entityData.set(DATA_TARGET_VEL_X, (float) velocity.x);
        this.entityData.set(DATA_TARGET_VEL_Y, (float) velocity.y);
        this.entityData.set(DATA_TARGET_VEL_Z, (float) velocity.z);
    }

    // ==================== 客户端插值数据（不同步）====================

    @Override
    public Vec3 jujutsuAddon$getClientVelocity() {
        return jujutsuAddon$clientVelocity;
    }

    @Override
    public void jujutsuAddon$setClientVelocity(Vec3 velocity) {
        this.jujutsuAddon$clientVelocity = velocity;
    }

    @Override
    public Vec3 jujutsuAddon$getRenderPosition() {
        return jujutsuAddon$renderPosition;
    }

    @Override
    public void jujutsuAddon$setRenderPosition(Vec3 position) {
        this.jujutsuAddon$renderPosition = position;
    }

    // ==================== 原始速度 ====================

    @Override
    @Nullable
    public Vec3 jujutsuAddon$getOriginalVelocity() {
        return jujutsuAddon$originalVelocity;
    }

    @Override
    public void jujutsuAddon$setOriginalVelocity(@Nullable Vec3 velocity) {
        this.jujutsuAddon$originalVelocity = velocity;
    }

    @Override
    @Nullable
    public Vec3 jujutsuAddon$getOriginalPower() {
        return jujutsuAddon$originalPower;
    }

    @Override
    public void jujutsuAddon$setOriginalPower(@Nullable Vec3 power) {
        this.jujutsuAddon$originalPower = power;
    }

    // ==================== 原始捕获位置 ====================

    @Override
    @Nullable
    public Vec3 jujutsuAddon$getOriginalCapturePosition() {
        return jujutsuAddon$originalCapturePosition;
    }

    @Override
    public void jujutsuAddon$setOriginalCapturePosition(@Nullable Vec3 position) {
        this.jujutsuAddon$originalCapturePosition = position;
    }

    // ==================== 冻结位置 ====================

    @Override
    @Nullable
    public Vec3 jujutsuAddon$getFrozenPosition() {
        return jujutsuAddon$frozenPosition;
    }

    @Override
    public void jujutsuAddon$setFrozenPosition(@Nullable Vec3 position) {
        this.jujutsuAddon$frozenPosition = position;
    }

    // ==================== 旋转锁定 ====================

    @Override
    public void jujutsuAddon$lockRotation(float yaw, float pitch) {
        this.jujutsuAddon$lockedYaw = yaw;
        this.jujutsuAddon$lockedPitch = pitch;
        this.jujutsuAddon$rotationLocked = true;
    }

    @Override
    public float jujutsuAddon$getLockedYaw() {
        return jujutsuAddon$lockedYaw;
    }

    @Override
    public float jujutsuAddon$getLockedPitch() {
        return jujutsuAddon$lockedPitch;
    }

    @Override
    public boolean jujutsuAddon$isRotationLocked() {
        return jujutsuAddon$rotationLocked;
    }

    // ==================== 当前飞行方向 ====================

    @Override
    @Nullable
    public Vec3 jujutsuAddon$getCurrentDirection() {
        return jujutsuAddon$currentDirection;
    }

    @Override
    public void jujutsuAddon$setCurrentDirection(@Nullable Vec3 direction) {
        this.jujutsuAddon$currentDirection = direction;
    }

    // ==================== Tick 注入 ====================

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void jujutsuAddon$onTick(CallbackInfo ci) {
        if ((Object) this instanceof AbstractArrow) {
            return;
        }
        if ((Object) this instanceof AbstractHurtingProjectile) {
            return;
        }

        if (this.jujutsuAddon$isControlled()) {
            super.tick();
            if (!this.level().isClientSide) {
                ControlledProjectileTick.tick((Projectile) (Object) this);
            }
            this.checkInsideBlocks();
            ci.cancel();
        }
    }
}
