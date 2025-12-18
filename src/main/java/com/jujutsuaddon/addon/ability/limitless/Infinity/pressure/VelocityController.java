package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 通用速度控制器
 * 用于限制实体接近无下限使用者的速度
 */
public class VelocityController {

    /**
     * 计算允许的最大接近速度
     *
     * @param pressureLevel 压制等级
     * @param cursedEnergyOutput 咒力输出
     * @param distance 当前距离
     * @param maxRange 最大范围
     * @param haltDistance 停止距离
     * @return 允许的最大接近速度（正值表示可以接近的速度，0表示不能接近）
     */
    public static double calculateMaxApproachSpeed(int pressureLevel, float cursedEnergyOutput,
                                                   double distance, double maxRange,
                                                   double haltDistance) {
        if (pressureLevel <= 0) return Double.MAX_VALUE;

        double distanceFromHalt = distance - haltDistance;
        double levelFactor = PressureCalculator.calculateLevelFactor(pressureLevel);

        if (distanceFromHalt <= 0) {
            // 已经突破停止边界，完全不允许接近
            return 0;
        }

        // 过渡区：逐渐降低允许的接近速度
        double transitionZone = PressureConfig.getHaltTransitionZone();

        if (distanceFromHalt < transitionZone) {
            // 在过渡区内，线性降低允许速度
            double ratio = distanceFromHalt / transitionZone;
            // 基础允许速度，会被等级和输出降低
            double baseAllowed = 0.5 * ratio;
            return baseAllowed / (levelFactor * cursedEnergyOutput);
        }

        // 远离停止区，计算基于距离的允许速度
        double normalizedDist = (distance - haltDistance) / (maxRange - haltDistance);
        normalizedDist = Math.max(0, Math.min(1, normalizedDist));

        // 越远允许速度越高，但仍受等级影响
        double baseAllowed = 0.3 + normalizedDist * 1.5;
        return baseAllowed / (levelFactor * Math.max(cursedEnergyOutput, 0.5));
    }

    /**
     * 计算速度减速比例（类似投射物的 speedRatio）
     *
     * @param pressureLevel 压制等级
     * @param cursedEnergyOutput 咒力输出
     * @param distance 当前距离
     * @param maxRange 最大范围
     * @param haltDistance 停止距离
     * @return 速度保留比例 (0.0 ~ 1.0)
     */
    public static double calculateSpeedRatio(int pressureLevel, float cursedEnergyOutput,
                                             double distance, double maxRange,
                                             double haltDistance) {
        if (pressureLevel <= 0) return 1.0;

        double distanceFromHalt = distance - haltDistance;

        if (distanceFromHalt <= 0) {
            // 已突破停止边界，速度归零
            return 0;
        }

        double effectiveRange = maxRange - haltDistance;
        if (effectiveRange <= 0) return 0;

        // 基础比例：基于距离的线性衰减
        double baseRatio = distanceFromHalt / effectiveRange;
        baseRatio = Math.max(0, Math.min(1, baseRatio));

        // 应用咒力输出的影响（输出越高，减速越强）
        double outputFactor = 0.5 + cursedEnergyOutput * 0.5;
        double adjustedRatio = Math.pow(baseRatio, outputFactor);

        // 应用等级的影响（等级越高，在相同距离减速越强）
        double levelPenalty = 1.0 - (PressureCalculator.calculateLevelFactor(pressureLevel) * 0.3);
        levelPenalty = Math.max(0.2, levelPenalty);

        return adjustedRatio * levelPenalty;
    }

    /**
     * 计算速度减速比例（可自定义停止距离）
     * 用于投射物等有特殊停止区的情况
     *
     * @param pressureLevel 压制等级
     * @param cursedEnergyOutput 咒力输出
     * @param distance 当前距离
     * @param maxRange 最大范围
     * @param customStopDistance 自定义停止距离（如投射物的 stopZoneOuter）
     * @return 速度保留比例 (0.0 ~ 1.0)
     */
    public static double calculateSpeedRatioWithCustomStop(int pressureLevel, float cursedEnergyOutput,
                                                           double distance, double maxRange,
                                                           double customStopDistance) {
        if (pressureLevel <= 0) return 1.0;
        double distanceFromStop = distance - customStopDistance;
        if (distanceFromStop <= 0) {
            // 已在停止区内
            return 0;
        }
        double effectiveRange = maxRange - customStopDistance;
        if (effectiveRange <= 0) return 0;
        // 基础比例：基于距离的线性衰减
        double baseRatio = distanceFromStop / effectiveRange;
        baseRatio = Math.max(0, Math.min(1, baseRatio));
        // 应用咒力输出的影响（输出越高，减速越强）
        double outputFactor = 0.5 + cursedEnergyOutput * 0.5;
        return Math.pow(baseRatio, outputFactor);
    }

    /**
     * 限制实体的接近速度
     *
     * @param currentVelocity 当前速度
     * @param directionToOwner 指向owner的方向（归一化）
     * @param maxApproachSpeed 最大允许接近速度
     * @param resistanceStrength 阻力强度 (0.0 ~ 1.0)
     * @return 限制后的速度
     */
    public static Vec3 limitApproachVelocity(Vec3 currentVelocity, Vec3 directionToOwner,
                                             double maxApproachSpeed, double resistanceStrength) {
        // 计算接近速度分量（负值表示正在接近owner）
        double approachSpeed = -currentVelocity.dot(directionToOwner);

        if (approachSpeed <= 0) {
            // 正在远离，不需要限制
            return currentVelocity;
        }

        // 超过最大允许接近速度
        if (approachSpeed > maxApproachSpeed) {
            // 计算需要抵消的速度
            double excessSpeed = approachSpeed - maxApproachSpeed;

            // 应用阻力，抵消超出部分
            double resistFactor = Math.min(1.0, resistanceStrength + 0.3);
            double cancelSpeed = excessSpeed * resistFactor;

            // 将抵消量加到速度上（反向）
            return currentVelocity.add(directionToOwner.scale(cancelSpeed));
        }

        return currentVelocity;
    }

    /**
     * 应用百分比减速到速度（保留方向）
     *
     * @param velocity 原始速度
     * @param speedRatio 速度保留比例 (0.0 ~ 1.0)
     * @return 减速后的速度
     */
    public static Vec3 applySpeedRatio(Vec3 velocity, double speedRatio) {
        speedRatio = Math.max(0, Math.min(1, speedRatio));
        return velocity.scale(speedRatio);
    }

    /**
     * 应用接近方向的百分比减速（只减慢接近分量，保留切向分量）
     *
     * @param velocity 原始速度
     * @param directionToOwner 指向owner的方向（归一化）
     * @param approachSpeedRatio 接近方向的速度保留比例
     * @param lateralSpeedRatio 横向的速度保留比例
     * @return 处理后的速度
     */
    public static Vec3 applyDirectionalSpeedRatio(Vec3 velocity, Vec3 directionToOwner,
                                                  double approachSpeedRatio,
                                                  double lateralSpeedRatio) {
        // 分解速度为接近分量和横向分量
        double approachComponent = velocity.dot(directionToOwner);
        Vec3 approachVelocity = directionToOwner.scale(approachComponent);
        Vec3 lateralVelocity = velocity.subtract(approachVelocity);

        // 分别应用减速
        Vec3 newApproachVelocity;
        if (approachComponent < 0) {
            // 正在接近，应用接近减速
            newApproachVelocity = approachVelocity.scale(approachSpeedRatio);
        } else {
            // 正在远离，保持原速或轻微减速
            newApproachVelocity = approachVelocity.scale(Math.max(approachSpeedRatio, 0.9));
        }

        Vec3 newLateralVelocity = lateralVelocity.scale(lateralSpeedRatio);

        return newApproachVelocity.add(newLateralVelocity);
    }

    /**
     * 综合处理实体速度（推荐使用）
     * 结合百分比减速和硬性限制
     *
     * @param entity 目标实体
     * @param ownerCenter owner的中心位置
     * @param pressureLevel 压制等级
     * @param cursedEnergyOutput 咒力输出
     * @param maxRange 最大范围
     * @return 处理结果
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

        // 计算速度比例
        double speedRatio = calculateSpeedRatio(pressureLevel, cursedEnergyOutput,
                distance, maxRange, haltDistance);
        result.speedRatio = speedRatio;

        // 计算横向速度比例（横向阻力较小）
        double lateralRatio = Math.min(1.0, speedRatio + 0.3);

        // 应用方向性减速
        Vec3 limitedVelocity = applyDirectionalSpeedRatio(
                currentVelocity, directionToOwner, speedRatio, lateralRatio);

        // 额外的硬性限制：确保不会突破停止边界
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

        /**
         * 获取接近速度（正值表示正在接近owner）
         */
        public double getApproachSpeed() {
            return -originalVelocity.dot(directionFromOwner.scale(-1));
        }

        /**
         * 获取处理后的接近速度
         */
        public double getProcessedApproachSpeed() {
            return -processedVelocity.dot(directionFromOwner.scale(-1));
        }
    }
}
