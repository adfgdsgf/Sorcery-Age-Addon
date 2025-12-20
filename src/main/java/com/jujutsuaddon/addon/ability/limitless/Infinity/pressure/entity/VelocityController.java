package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.entity;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 速度控制器（简化版）
 * 主要用于投射物和掉落物
 * 生物推力由 PushForceApplier 统一处理
 */
public class VelocityController {

    public enum Zone {
        PUSH,      // 推力区（在平衡点内）
        SLOWDOWN,  // 减速区（平衡点到范围边界）
        OUTSIDE    // 范围外
    }

    /**
     * 获取实体所在区域信息（不修改速度）
     */
    public static VelocityResult getEntityZoneInfo(Entity entity, Vec3 ownerCenter,
                                                   int pressureLevel, float cursedEnergyOutput,
                                                   double maxRange) {
        VelocityResult result = new VelocityResult();

        Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() / 2, 0);
        Vec3 currentVelocity = entity.getDeltaMovement();

        double distance = ownerCenter.distanceTo(entityCenter);
        if (distance < 0.1) distance = 0.1;

        Vec3 directionFromOwner = entityCenter.subtract(ownerCenter).normalize();

        // 使用 BalancePointCalculator 获取平衡点半径
        double balancePointRadius = BalancePointCalculator.getBalanceRadius(pressureLevel, maxRange);

        result.distance = distance;
        result.balanceRadius = balancePointRadius;
        result.distanceFromBalance = distance - balancePointRadius;
        result.directionFromOwner = directionFromOwner;
        result.originalVelocity = currentVelocity;
        result.processedVelocity = currentVelocity;

        // 判断区域
        if (distance > maxRange) {
            result.zone = Zone.OUTSIDE;
        } else if (distance <= balancePointRadius) {
            result.zone = Zone.PUSH;
        } else {
            result.zone = Zone.SLOWDOWN;
        }

        result.isInPushZone = result.zone == Zone.PUSH;

        // 判断移动方向
        Vec3 directionToOwner = directionFromOwner.scale(-1);
        double movementTowardOwner = currentVelocity.dot(directionToOwner);
        result.isApproaching = movementTowardOwner > 0.01;
        result.isRetreating = movementTowardOwner < -0.01;

        // 计算阻力强度（减速区用）
        if (result.zone == Zone.SLOWDOWN) {
            // 从边界到平衡点，阻力从0增加到1
            double slowdownRange = maxRange - balancePointRadius;
            if (slowdownRange > 0) {
                double distanceFromEdge = maxRange - distance;
                result.resistanceStrength = Math.min(1.0, distanceFromEdge / slowdownRange);
            }
        } else if (result.zone == Zone.PUSH) {
            result.resistanceStrength = 1.0; // 推力区内阻力最大
        }

        return result;
    }

    public static class VelocityResult {
        public Vec3 originalVelocity = Vec3.ZERO;
        public Vec3 processedVelocity = Vec3.ZERO;
        public Vec3 directionFromOwner = Vec3.ZERO;
        public double distance = 0;
        public double balanceRadius = 0;      // 改名：平衡点半径
        public double distanceFromBalance = 0; // 改名：距离平衡点的距离
        public double resistanceStrength = 0;
        public boolean isInPushZone = false;
        public boolean isApproaching = false;
        public boolean isRetreating = false;
        public Zone zone = Zone.OUTSIDE;

        // 兼容旧代码
        @Deprecated
        public double haltDistance = 0;
        @Deprecated
        public double distanceFromHalt = 0;
    }

    // ==================== 投射物方法（保持不变）====================

    public static double calculateSmoothProjectileSpeedRatio(
            int pressureLevel, float cursedEnergyOutput,
            double distance, double maxRange, double stopDistance) {

        if (distance >= maxRange) return 1.0;

        double slowdownRange = maxRange - stopDistance;
        if (slowdownRange <= 0.5) {
            return distance > stopDistance ? 1.0 : 0.0;
        }

        double distanceFromStop = distance - stopDistance;
        if (distanceFromStop <= 0) return 0.0;

        double t = distanceFromStop / slowdownRange;
        t = Math.max(0, Math.min(1, t));

        double entrySpeed = 0.4;
        double midSpeed = 0.12;
        double exitSpeed = 0.0;

        double ratio;
        if (t > 0.5) {
            double localT = (t - 0.5) / 0.5;
            double easeOut = 1.0 - Math.pow(1.0 - localT, 2);
            ratio = midSpeed + (entrySpeed - midSpeed) * easeOut;
        } else {
            double localT = t / 0.5;
            double easeIn = localT * localT;
            ratio = exitSpeed + (midSpeed - exitSpeed) * easeIn;
        }

        double outputInfluence = 0.9 + cursedEnergyOutput * 0.1;
        ratio = Math.pow(ratio, outputInfluence);

        double minSpeed = 0.01 + t * 0.02;
        return Math.max(minSpeed, ratio);
    }

    public static double calculateAggressiveProjectileSpeedRatio(
            int pressureLevel, float cursedEnergyOutput,
            double distance, double maxRange, double stopDistance) {

        if (distance >= maxRange) return 1.0;

        double slowdownRange = maxRange - stopDistance;
        if (slowdownRange <= 0.5) {
            return distance > stopDistance ? 1.0 : 0.0;
        }

        double distanceFromStop = distance - stopDistance;
        if (distanceFromStop <= 0) return 0.0;

        double t = distanceFromStop / slowdownRange;
        t = Math.max(0, Math.min(1, t));

        return Math.max(0.01, 0.3 * Math.pow(t, 1.5));
    }
}
