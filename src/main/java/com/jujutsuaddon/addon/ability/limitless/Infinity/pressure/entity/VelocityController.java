package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.entity;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.PressureCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 通用速度控制器
 * 用于限制实体接近无下限使用者的速度
 */
public class VelocityController {

    /**
     * 计算允许的最大接近速度
     */
    public static double calculateMaxApproachSpeed(int pressureLevel, float cursedEnergyOutput,
                                                   double distance, double maxRange,
                                                   double haltDistance) {
        if (pressureLevel <= 0) return Double.MAX_VALUE;

        double distanceFromHalt = distance - haltDistance;
        double levelFactor = PressureCalculator.calculateLevelFactor(pressureLevel);

        if (distanceFromHalt <= 0) {
            return 0;
        }

        double transitionZone = PressureConfig.getHaltTransitionZone();

        if (distanceFromHalt < transitionZone) {
            double ratio = distanceFromHalt / transitionZone;
            double baseAllowed = 0.5 * ratio;
            return baseAllowed / (levelFactor * cursedEnergyOutput);
        }

        double normalizedDist = (distance - haltDistance) / (maxRange - haltDistance);
        normalizedDist = Math.max(0, Math.min(1, normalizedDist));

        double baseAllowed = 0.3 + normalizedDist * 1.5;
        return baseAllowed / (levelFactor * Math.max(cursedEnergyOutput, 0.5));
    }

    /**
     * 计算速度减速比例
     */
    public static double calculateSpeedRatio(int pressureLevel, float cursedEnergyOutput,
                                             double distance, double maxRange,
                                             double haltDistance) {
        if (pressureLevel <= 0) return 1.0;

        double distanceFromHalt = distance - haltDistance;

        if (distanceFromHalt <= 0) {
            return 0;
        }

        double effectiveRange = maxRange - haltDistance;
        if (effectiveRange <= 0) return 0;

        double baseRatio = distanceFromHalt / effectiveRange;
        baseRatio = Math.max(0, Math.min(1, baseRatio));

        double outputFactor = 0.5 + cursedEnergyOutput * 0.5;
        double adjustedRatio = Math.pow(baseRatio, outputFactor);

        double levelPenalty = 1.0 - (PressureCalculator.calculateLevelFactor(pressureLevel) * 0.3);
        levelPenalty = Math.max(0.2, levelPenalty);

        return adjustedRatio * levelPenalty;
    }

    /**
     * 计算速度减速比例（可自定义停止距离）
     */
    public static double calculateSpeedRatioWithCustomStop(int pressureLevel, float cursedEnergyOutput,
                                                           double distance, double maxRange,
                                                           double customStopDistance) {
        double effectiveRange = maxRange - customStopDistance;

        if (effectiveRange <= 0) {
            return 1.0;
        }

        double distanceFromStop = distance - customStopDistance;
        double baseRatio = distanceFromStop / effectiveRange;
        baseRatio = Math.max(0, Math.min(1, baseRatio));

        double outputFactor = 0.5 + cursedEnergyOutput * 0.5;
        return Math.pow(baseRatio, outputFactor);
    }

    /**
     * ★★★ 修复：限制实体的接近速度（只限制接近，不限制远离）★★★
     *
     * @param currentVelocity 当前速度
     * @param directionToOwner 指向owner的方向（归一化）
     * @param maxApproachSpeed 最大允许接近速度
     * @param resistanceStrength 阻力强度 (0.0 ~ 1.0)
     * @return 限制后的速度
     */
    public static Vec3 limitApproachVelocity(Vec3 currentVelocity, Vec3 directionToOwner,
                                             double maxApproachSpeed, double resistanceStrength) {
        // 计算接近速度分量
        // dot > 0 = 速度方向与指向owner方向相同 = 正在接近
        // dot < 0 = 速度方向与指向owner方向相反 = 正在远离
        double approachComponent = currentVelocity.dot(directionToOwner);

        if (approachComponent <= 0) {
            // ★★★ 正在远离owner，完全不限制！★★★
            return currentVelocity;
        }

        // 正在接近owner，检查是否超过最大允许速度
        if (approachComponent > maxApproachSpeed) {
            double excessSpeed = approachComponent - maxApproachSpeed;
            double resistFactor = Math.min(1.0, resistanceStrength + 0.3);
            double cancelSpeed = excessSpeed * resistFactor;
            // 抵消超出部分（反向添加）
            return currentVelocity.subtract(directionToOwner.scale(cancelSpeed));
        }

        return currentVelocity;
    }

    /**
     * 应用百分比减速到速度（保留方向）
     */
    public static Vec3 applySpeedRatio(Vec3 velocity, double speedRatio) {
        speedRatio = Math.max(0, Math.min(1, speedRatio));
        return velocity.scale(speedRatio);
    }

    /**
     * ★★★ 修复：应用方向性减速（只减速接近方向，远离方向不减速）★★★
     *
     * @param velocity 原始速度
     * @param directionToOwner 指向owner的方向（归一化）
     * @param approachSpeedRatio 接近方向的速度保留比例 (0.0 ~ 1.0)
     * @param lateralSpeedRatio 横向的速度保留比例 (0.0 ~ 1.0)
     * @return 处理后的速度
     */
    public static Vec3 applyDirectionalSpeedRatio(Vec3 velocity, Vec3 directionToOwner,
                                                  double approachSpeedRatio,
                                                  double lateralSpeedRatio) {
        // 分解速度为接近分量和横向分量
        // approachComponent > 0 = 正在接近 owner
        // approachComponent < 0 = 正在远离 owner
        double approachComponent = velocity.dot(directionToOwner);
        Vec3 approachVelocity = directionToOwner.scale(approachComponent);
        Vec3 lateralVelocity = velocity.subtract(approachVelocity);

        Vec3 newApproachVelocity;
        if (approachComponent > 0) {
            // ★★★ 正在接近 owner，应用减速阻力 ★★★
            newApproachVelocity = approachVelocity.scale(approachSpeedRatio);
        } else {
            // ★★★ 正在远离 owner，完全不减速！★★★
            // 实际上可以保持原速，推力会在别处添加
            newApproachVelocity = approachVelocity;
        }

        // 横向移动：轻微减速（可选，模拟粘滞空间的感觉）
        // 但比接近方向的阻力小很多
        double effectiveLateralRatio = Math.max(lateralSpeedRatio, 0.85);
        Vec3 newLateralVelocity = lateralVelocity.scale(effectiveLateralRatio);

        return newApproachVelocity.add(newLateralVelocity);
    }

    /**
     * 综合处理实体速度（推荐使用）
     */
    public static VelocityResult processEntityVelocity(Entity entity, Vec3 ownerCenter,
                                                       int pressureLevel, float cursedEnergyOutput,
                                                       double maxRange) {
        VelocityResult result = new VelocityResult();

        Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() / 2, 0);
        Vec3 currentVelocity = entity.getDeltaMovement();

        double distance = ownerCenter.distanceTo(entityCenter);
        if (distance < 0.1) distance = 0.1;

        Vec3 directionFromOwner = entityCenter.subtract(ownerCenter).normalize();
        Vec3 directionToOwner = directionFromOwner.scale(-1);

        double haltDistance = PressureCalculator.calculateHaltDistance(pressureLevel);
        double distanceFromHalt = distance - haltDistance;

        result.distance = distance;
        result.haltDistance = haltDistance;
        result.distanceFromHalt = distanceFromHalt;
        result.directionFromOwner = directionFromOwner;
        result.isInStopZone = distanceFromHalt <= 0;
        result.originalVelocity = currentVelocity;

        // ★★★ 判断实体的移动方向 ★★★
        double movementTowardOwner = currentVelocity.dot(directionToOwner);
        boolean isApproaching = movementTowardOwner > 0.01;
        boolean isRetreating = movementTowardOwner < -0.01;

        result.isApproaching = isApproaching;
        result.isRetreating = isRetreating;

        // ★★★ 根据移动方向决定阻力 ★★★
        double speedRatio;
        double lateralRatio;

        if (isRetreating) {
            // 正在远离：完全不减速！
            speedRatio = 1.0;
            lateralRatio = 1.0;
        } else if (isApproaching) {
            // 正在接近：应用减速
            speedRatio = calculateSpeedRatio(pressureLevel, cursedEnergyOutput,
                    distance, maxRange, haltDistance);
            lateralRatio = Math.min(1.0, speedRatio + 0.3);
        } else {
            // 横向移动：轻微减速
            speedRatio = 0.95;
            lateralRatio = 0.9;
        }

        result.speedRatio = speedRatio;

        // 应用方向性减速
        Vec3 limitedVelocity = applyDirectionalSpeedRatio(
                currentVelocity, directionToOwner, speedRatio, lateralRatio);

        // 额外的硬性限制：确保不会突破停止边界（只对接近方向）
        double maxApproach = calculateMaxApproachSpeed(pressureLevel, cursedEnergyOutput,
                distance, maxRange, haltDistance);
        result.maxApproachSpeed = maxApproach;

        double resistanceStrength = PressureCalculator.calculateResistanceStrength(
                pressureLevel, distance, maxRange);
        result.resistanceStrength = resistanceStrength;

        limitedVelocity = limitApproachVelocity(limitedVelocity, directionToOwner,
                maxApproach, resistanceStrength);

        result.processedVelocity = limitedVelocity;

        return result;
    }

    // ... 其他方法保持不变 ...

    /**
     * 速度处理结果
     */
    public static class VelocityResult {
        public Vec3 originalVelocity = Vec3.ZERO;
        public Vec3 processedVelocity = Vec3.ZERO;
        public Vec3 directionFromOwner = Vec3.ZERO;
        public double distance = 0;
        public double haltDistance = 0;
        public double distanceFromHalt = 0;
        public double speedRatio = 1.0;
        public double maxApproachSpeed = Double.MAX_VALUE;
        public double resistanceStrength = 0;
        public boolean isInStopZone = false;
        public boolean isApproaching = false;   // ★ 新增：是否正在接近 ★
        public boolean isRetreating = false;    // ★ 新增：是否正在远离 ★

        /**
         * 获取接近速度（正值表示正在接近owner）
         */
        public double getApproachSpeed() {
            return originalVelocity.dot(directionFromOwner.scale(-1));
        }

        /**
         * 获取处理后的接近速度
         */
        public double getProcessedApproachSpeed() {
            return processedVelocity.dot(directionFromOwner.scale(-1));
        }
    }

    // ==================== 投射物相关方法（保持不变）====================

    public static double calculateSmoothProjectileSpeedRatio(
            int pressureLevel,
            float cursedEnergyOutput,
            double distance,
            double maxRange,
            double stopDistance) {

        if (distance >= maxRange) {
            return 1.0;
        }

        double slowdownRange = maxRange - stopDistance;
        if (slowdownRange <= 0.5) {
            return distance > stopDistance ? 1.0 : 0.0;
        }

        double distanceFromStop = distance - stopDistance;
        if (distanceFromStop <= 0) {
            return 0.0;
        }

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
            int pressureLevel,
            float cursedEnergyOutput,
            double distance,
            double maxRange,
            double stopDistance) {

        if (distance >= maxRange) return 1.0;

        double slowdownRange = maxRange - stopDistance;
        if (slowdownRange <= 0.5) {
            return distance > stopDistance ? 1.0 : 0.0;
        }

        double distanceFromStop = distance - stopDistance;
        if (distanceFromStop <= 0) return 0.0;

        double t = distanceFromStop / slowdownRange;
        t = Math.max(0, Math.min(1, t));

        double ratio = 0.3 * Math.pow(t, 1.5);

        return Math.max(0.01, ratio);
    }
}
