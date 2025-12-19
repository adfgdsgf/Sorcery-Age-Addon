package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.entity;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.PressureCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PushForceApplier {

    public static CollisionInfo apply(LivingEntity owner, LivingEntity target,
                                      int pressureLevel, float cursedEnergyOutput,
                                      double maxRange,
                                      PressureStateManager stateManager) {
        CollisionInfo info = new CollisionInfo();

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2, 0);
        double distance3D = ownerCenter.distanceTo(targetCenter);
// ★★★ 最小距离保护：距离太近时使用玩家视线方向 ★★★
        Vec3 forceDirection;
        final double MIN_EFFECTIVE_DISTANCE = 0.5;  // 最小有效距离
        if (distance3D < MIN_EFFECTIVE_DISTANCE) {
            // 距离太近，使用玩家视线方向作为推力方向
            Vec3 lookDirection = owner.getLookAngle();
            // 主要使用水平方向，避免把目标推到天上或地下
            forceDirection = new Vec3(lookDirection.x, lookDirection.y * 0.3, lookDirection.z).normalize();

            // 将距离设为最小值用于后续计算，避免压力计算异常
            distance3D = MIN_EFFECTIVE_DISTANCE;
        } else {
            forceDirection = targetCenter.subtract(ownerCenter).normalize();
        }

        double pushRadius = PressureConfig.getPushZoneRadius(pressureLevel);
        double stopRadius = PressureConfig.getStopZoneRadius(pressureLevel);
        double distanceFromPush = distance3D - pushRadius;

        info.haltDistance = pushRadius;
        info.distanceFromHalt = distanceFromPush;
        info.isBreaching = distanceFromPush < 0;

        // ★★★ 先检测碰撞，再计算压力 ★★★
        if (distance3D > maxRange) {
            info.isColliding = false;
            info.forceDirection = forceDirection;
            info.distance = distance3D;
            info.pressureValue = 0;
            return info;
        }

        info.isColliding = CollisionHandler.isCollidingInForceDirection(target, forceDirection);

        // ★★★ 获取碰撞方块硬度 ★★★
        float blockHardness = 1.0f;  // 默认值
        if (info.isColliding) {
            blockHardness = getCollidingBlockHardness(target, forceDirection);
            info.collidingBlockHardness = blockHardness;
        }

        Vec3 ownerMovement = stateManager.calculateOwnerMovement(owner);

        // ★★★ 压力计算（含碰撞和方块硬度）★★★
        info.pressureValue = PressureCalculator.calculatePressure(
                pressureLevel, cursedEnergyOutput, distance3D, maxRange,
                ownerMovement, forceDirection,
                info.isColliding, blockHardness);

        VelocityController.VelocityResult velocityResult = VelocityController.processEntityVelocity(
                target, ownerCenter, pressureLevel, cursedEnergyOutput, maxRange);

        Vec3 currentVelocity = velocityResult.processedVelocity;
        double resistanceStrength = velocityResult.resistanceStrength;
        info.resistanceStrength = resistanceStrength;

        Vec3 horizontalForce = new Vec3(forceDirection.x, 0, forceDirection.z);
        double hForceLen = horizontalForce.length();
        if (hForceLen > 0.01) {
            horizontalForce = horizontalForce.normalize();
        }

        double newVelX = currentVelocity.x;
        double newVelY = currentVelocity.y;
        double newVelZ = currentVelocity.z;

        // 受伤时抑制向上弹
        if (target.hurtTime > 0 && newVelY > 0.1) {
            newVelY *= 0.3;
        }

        if (distanceFromPush < 0) {
            // ==================== 突破推力区 ====================
            double breachDepth = Math.abs(distanceFromPush);
            double breachForce = PressureConfig.getBreachRepelForce() *
                    (1.0 + breachDepth * 2.0) *
                    PressureCalculator.calculateLevelFactor(pressureLevel) *
                    cursedEnergyOutput;
            breachForce = Math.min(breachForce, PressureConfig.getMaxPushForce() * 2.0);

            // 突破时主要是水平推力
            newVelX += horizontalForce.x * breachForce;
            newVelZ += horizontalForce.z * breachForce;

            // 大幅限制 Y 轴力
            if (forceDirection.y < -0.3 && !target.onGround()) {
                newVelY += forceDirection.y * breachForce * 0.2;
            }

            double approachVel = -(currentVelocity.x * horizontalForce.x +
                    currentVelocity.z * horizontalForce.z);
            if (approachVel > 0) {
                newVelX += horizontalForce.x * approachVel;
                newVelZ += horizontalForce.z * approachVel;
            }

            if (info.isColliding) {
                stateManager.incrementPinnedTicks(target.getUUID());
                int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());

                double pinForce = PressureConfig.getPinForce() * (1.0 + pinnedTicks * 0.1);
                newVelX += horizontalForce.x * pinForce;
                newVelZ += horizontalForce.z * pinForce;

                Vec3 velocity = new Vec3(newVelX, newVelY, newVelZ);
                Vec3 lateralVel = velocity.subtract(forceDirection.scale(velocity.dot(forceDirection)));
                Vec3 horizontalLateral = new Vec3(lateralVel.x, 0, lateralVel.z);
                if (horizontalLateral.length() > 0.01) {
                    double lateralResist = 0.8 + Math.min(pinnedTicks * 0.02, 0.15);
                    newVelX -= horizontalLateral.x * lateralResist;
                    newVelZ -= horizontalLateral.z * lateralResist;
                }
            } else {
                stateManager.resetPinnedTicks(target.getUUID());
            }

        } else if (info.isColliding) {
            // ==================== 碰撞但未突破 ====================
            stateManager.incrementPinnedTicks(target.getUUID());
            int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());

            double pushForce = PressureCalculator.calculatePushForce(
                    pressureLevel, cursedEnergyOutput, distance3D, maxRange);

            double collisionMult = 1.3 + Math.min(pinnedTicks * 0.05, 0.7);
            double crushForce = pushForce * collisionMult;

            newVelX += horizontalForce.x * crushForce;
            newVelZ += horizontalForce.z * crushForce;

            if (forceDirection.y < -0.5 && !target.onGround()) {
                newVelY += forceDirection.y * crushForce * 0.3;
            }

            Vec3 velocity = new Vec3(newVelX, newVelY, newVelZ);
            double escapeSpeed = -velocity.dot(forceDirection);
            if (escapeSpeed > 0) {
                double resistForce = escapeSpeed * PressureConfig.getEscapeResistance();
                newVelX += forceDirection.x * resistForce;
                newVelZ += forceDirection.z * resistForce;
            }

            Vec3 lateralVel = velocity.subtract(forceDirection.scale(velocity.dot(forceDirection)));
            Vec3 horizontalLateral = new Vec3(lateralVel.x, 0, lateralVel.z);
            if (horizontalLateral.length() > 0.01) {
                double lateralResist = PressureConfig.getLateralResistance();
                if (pinnedTicks > 3) {
                    lateralResist = Math.min(0.9, lateralResist + pinnedTicks * 0.02);
                }
                newVelX -= horizontalLateral.x * lateralResist;
                newVelZ -= horizontalLateral.z * lateralResist;
            }

            if (newVelY > 0.05) {
                newVelY *= (1.0 - resistanceStrength * 0.5);
            }

        } else {
            // ==================== 未碰撞、未突破 ====================
            stateManager.resetPinnedTicks(target.getUUID());

            double pushForce = PressureCalculator.calculatePushForce(
                    pressureLevel, cursedEnergyOutput, distance3D, maxRange);

            double stopZoneWidth = stopRadius - pushRadius;
            if (distanceFromPush < stopZoneWidth && stopZoneWidth > 0) {
                double proximityMult = 1.0 + (1.0 - distanceFromPush / stopZoneWidth) * 0.5;
                pushForce *= proximityMult;
            }

            newVelX += horizontalForce.x * pushForce;
            newVelZ += horizontalForce.z * pushForce;

            if (forceDirection.y < -0.5 && !target.onGround()) {
                newVelY += forceDirection.y * pushForce * 0.2;
            }
        }

        // ★★★ Y 轴速度限制 ★★★
        if (target.onGround()) {
            if (newVelY > 0.1) {
                newVelY = 0.1;
            }
        }

        if (target.horizontalCollision) {
            newVelY = clamp(newVelY, -0.5, 0.15);
        }

        if (target.onGround() && target.horizontalCollision) {
            if (newVelY > 0) {
                newVelY = 0;
            }
        }

        newVelX = clamp(newVelX, -3.5, 3.5);
        newVelZ = clamp(newVelZ, -3.5, 3.5);

        if (info.isBreaching) {
            newVelY = clamp(newVelY, -2.0, 0.5);
        } else {
            newVelY = clamp(newVelY, -1.5, 0.3);
        }

        target.setDeltaMovement(newVelX, newVelY, newVelZ);
        target.hurtMarked = true;

        stateManager.setColliding(target.getUUID(), info.isColliding);
        stateManager.setPreviousVelocity(target.getUUID(), target.getDeltaMovement());

        info.forceDirection = forceDirection;
        info.distance = distance3D;

        return info;
    }

    /**
     * ★★★ 获取目标碰撞方向的方块硬度 ★★★
     */
    private static float getCollidingBlockHardness(LivingEntity target, Vec3 forceDirection) {
        Level level = target.level();
        AABB box = target.getBoundingBox();

        // 在推力方向上稍微偏移，检测前方的方块
        double checkDistance = 0.3;
        Vec3 checkOffset = forceDirection.scale(checkDistance);

        // 检测多个点（上中下）
        Vec3[] checkPoints = {
                target.position().add(checkOffset),                                    // 脚部
                target.position().add(0, target.getBbHeight() * 0.5, 0).add(checkOffset), // 中部
                target.position().add(0, target.getBbHeight() * 0.9, 0).add(checkOffset)  // 头部
        };

        float maxHardness = 0.0f;

        for (Vec3 point : checkPoints) {
            BlockPos blockPos = BlockPos.containing(point);
            BlockState state = level.getBlockState(blockPos);

            if (!state.isAir()) {
                float hardness = state.getDestroySpeed(level, blockPos);
                if (hardness < 0) {
                    // 不可破坏的方块（基岩等），返回 -1
                    return -1.0f;
                }
                maxHardness = Math.max(maxHardness, hardness);
            }
        }

        // 如果检测到水平碰撞但没找到方块，检查更宽的范围
        if (maxHardness == 0.0f && target.horizontalCollision) {
            // 扩大检测范围
            double expandX = forceDirection.x > 0.1 ? 0.5 : (forceDirection.x < -0.1 ? -0.5 : 0);
            double expandZ = forceDirection.z > 0.1 ? 0.5 : (forceDirection.z < -0.1 ? -0.5 : 0);

            for (double y = 0; y < target.getBbHeight(); y += 0.5) {
                BlockPos checkPos = BlockPos.containing(
                        target.getX() + expandX,
                        target.getY() + y,
                        target.getZ() + expandZ
                );
                BlockState state = level.getBlockState(checkPos);

                if (!state.isAir()) {
                    float hardness = state.getDestroySpeed(level, checkPos);
                    if (hardness < 0) {
                        return -1.0f;
                    }
                    maxHardness = Math.max(maxHardness, hardness);
                }
            }
        }

        // 默认返回石头的硬度
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
        public float collidingBlockHardness = 1.0f;  // ★ 新增：碰撞方块硬度 ★
    }
}
