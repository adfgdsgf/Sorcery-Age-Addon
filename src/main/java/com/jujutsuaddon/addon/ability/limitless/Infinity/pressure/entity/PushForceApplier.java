package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.entity;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.PressureCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureCurve;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PushForceApplier {

    public static CollisionInfo apply(LivingEntity owner, LivingEntity target,
                                      int pressureLevel, float cursedEnergyOutput,
                                      double maxRange,
                                      PressureStateManager stateManager) {
        CollisionInfo info = new CollisionInfo();

        // ==================== 基础计算 ====================
        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2, 0);
        double distance = ownerCenter.distanceTo(targetCenter);

        final double MIN_DISTANCE = 0.3;
        distance = Math.max(distance, MIN_DISTANCE);

        // 推力方向
        Vec3 pushDirection;
        if (ownerCenter.distanceTo(targetCenter) < 0.1) {
            Vec3 look = owner.getLookAngle();
            pushDirection = new Vec3(look.x, look.y * 0.3, look.z).normalize();
        } else {
            pushDirection = targetCenter.subtract(ownerCenter).normalize();
        }

        // 超出范围
        if (distance > maxRange) {
            stateManager.clearPositionHistory(target.getUUID());
            info.isColliding = false;
            info.forceDirection = pushDirection;
            info.distance = distance;
            info.pressureValue = 0;
            return info;
        }

        // ==================== 使用统一计算器 ====================
        double balanceRadius = BalancePointCalculator.getBalanceRadius(pressureLevel, maxRange);
        double zenoMultiplier = BalancePointCalculator.calculateZenoMultiplier(distance, balanceRadius, maxRange);

        // 填充信息
        info.haltDistance = balanceRadius;
        info.distanceFromHalt = distance - balanceRadius;
        info.isBreaching = distance < balanceRadius;
        info.forceDirection = pushDirection;
        info.distance = distance;
        info.zenoMultiplier = zenoMultiplier;

        // ==================== 碰撞检测 ====================
        info.isColliding = CollisionHandler.isCollidingInForceDirection(target, pushDirection);

        float blockHardness = 1.0f;
        if (info.isColliding) {
            blockHardness = getCollidingBlockHardness(target, pushDirection);
            info.collidingBlockHardness = blockHardness;
        }

// ==================== 压力计算 ====================
        Vec3 ownerMovement = stateManager.calculateOwnerMovement(owner);
        double basePressureValue = PressureCalculator.calculatePressure(
                pressureLevel, cursedEnergyOutput, distance, maxRange,
                ownerMovement, pushDirection, info.isColliding, blockHardness);
// ★★★ 如果被撞墙（在平衡点内），使用压力曲线 ★★★
        if (info.isColliding && distance < balanceRadius) {
            info.pressureValue = PressureCurve.calculateCollisionPressure(
                    distance, balanceRadius, basePressureValue);

        } else {
            info.pressureValue = basePressureValue;
        }

        // ==================== 判断区域 ====================
        boolean inBalanceZone = distance < balanceRadius;
        boolean inSlowdownZone = distance >= balanceRadius && distance <= maxRange;

        // ==================== 水平推力方向 ====================
        Vec3 horizontalPush = new Vec3(pushDirection.x, 0, pushDirection.z);
        if (horizontalPush.length() < 0.01) {
            Vec3 look = owner.getLookAngle();
            horizontalPush = new Vec3(look.x, 0, look.z);
        }
        if (horizontalPush.length() > 0.01) {
            horizontalPush = horizontalPush.normalize();
        } else {
            horizontalPush = new Vec3(1, 0, 0);
        }

        // ==================== 减速区处理 ====================
        if (inSlowdownZone) {
            stateManager.resetPinnedTicks(target.getUUID());
            return info;
        }

        // ==================== 需要干预的情况 ====================
        Vec3 vel = target.getDeltaMovement();
        double vx = vel.x;
        double vy = vel.y;
        double vz = vel.z;

        // 受伤弹跳抑制
        if (target.hurtTime > 0 && vy > 0.1) {
            vy *= 0.3;
        }

        // ==================== 红圈内 - 推出去 ====================
        if (inBalanceZone) {
            double depth = balanceRadius - distance;
            double pushForce = depth * 0.15 * cursedEnergyOutput;
            pushForce = Math.min(pushForce, 0.25);

            vx += horizontalPush.x * pushForce;
            vz += horizontalPush.z * pushForce;
        }

        // ==================== 撞墙处理 ====================
        if (info.isColliding) {
            stateManager.incrementPinnedTicks(target.getUUID());
            Vec3 newVel = applyPinningForce(vx, vy, vz, horizontalPush,
                    pushDirection, target, stateManager);
            vx = newVel.x;
            vy = newVel.y;
            vz = newVel.z;
        } else {
            stateManager.resetPinnedTicks(target.getUUID());
        }

        // ==================== 速度限制 ====================
        boolean isOnGround = target.onGround();
        if (isOnGround && vy > 0.1) vy = 0.1;
        if (target.horizontalCollision) vy = clamp(vy, -0.5, 0.15);
        if (isOnGround && target.horizontalCollision && vy > 0) vy = 0;

        double maxHorizontal = 2.0;
        vx = clamp(vx, -maxHorizontal, maxHorizontal);
        vz = clamp(vz, -maxHorizontal, maxHorizontal);
        vy = clamp(vy, -1.5, 0.3);

        // ==================== 应用 ====================
        target.setDeltaMovement(vx, vy, vz);
        target.hurtMarked = true;

        stateManager.setColliding(target.getUUID(), info.isColliding);
        stateManager.setPreviousVelocity(target.getUUID(), target.getDeltaMovement());

        return info;
    }

    private static Vec3 applyPinningForce(double vx, double vy, double vz,
                                          Vec3 horizontalPush, Vec3 pushDirection,
                                          LivingEntity target,
                                          PressureStateManager stateManager) {
        int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());

        double pinForce = PressureConfig.getPinForce() * (1.0 + pinnedTicks * 0.1);
        pinForce = Math.min(pinForce, 0.3);

        vx += horizontalPush.x * pinForce;
        vz += horizontalPush.z * pinForce;

        Vec3 velocity = new Vec3(vx, vy, vz);
        Vec3 lateralVel = velocity.subtract(pushDirection.scale(velocity.dot(pushDirection)));
        Vec3 horizontalLateral = new Vec3(lateralVel.x, 0, lateralVel.z);

        if (horizontalLateral.length() > 0.01) {
            double resist = 0.5 + Math.min(pinnedTicks * 0.02, 0.2);
            vx -= horizontalLateral.x * resist;
            vz -= horizontalLateral.z * resist;
        }

        return new Vec3(vx, vy, vz);
    }

    private static float getCollidingBlockHardness(LivingEntity target, Vec3 forceDirection) {
        Level level = target.level();
        double checkDistance = 0.3;
        Vec3 checkOffset = forceDirection.scale(checkDistance);

        Vec3[] checkPoints = {
                target.position().add(checkOffset),
                target.position().add(0, target.getBbHeight() * 0.5, 0).add(checkOffset),
                target.position().add(0, target.getBbHeight() * 0.9, 0).add(checkOffset)
        };

        float maxHardness = 0.0f;
        for (Vec3 point : checkPoints) {
            BlockPos blockPos = BlockPos.containing(point);
            BlockState state = level.getBlockState(blockPos);
            if (!state.isAir()) {
                float hardness = state.getDestroySpeed(level, blockPos);
                if (hardness < 0) return -1.0f;
                maxHardness = Math.max(maxHardness, hardness);
            }
        }

        return maxHardness > 0 ? maxHardness : 1.5f;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class CollisionInfo {
        public boolean isColliding = false;
        public boolean isBreaching = false;
        public Vec3 forceDirection = Vec3.ZERO;
        public double distance = 0;
        public double pressureValue = 0;
        public double haltDistance = 0;
        public double distanceFromHalt = 0;
        public double resistanceStrength = 0;
        public float collidingBlockHardness = 1.0f;
        public double zenoMultiplier = 0;
    }
}
