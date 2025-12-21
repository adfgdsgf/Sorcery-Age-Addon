package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.VelocityAnalyzer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 压力场状态分析器
 *
 * 分析实体相对于无下限压力场的完整状态
 * 包括：区域判断、芝诺倍率、接近速度
 *
 * 内部使用 VelocityAnalyzer 做速度分解
 */
public class VelocityController {

    public enum Zone {
        PUSH,      // 推力区（在平衡点内）
        SLOWDOWN,  // 减速区（平衡点到范围边界）
        OUTSIDE    // 范围外
    }

    /**
     * 获取实体相对于压力场的完整状态
     */
    public static VelocityResult getEntityZoneInfo(Entity entity, Vec3 ownerCenter,
                                                   int pressureLevel, float cursedEnergyOutput,
                                                   double maxRange) {
        Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() / 2, 0);
        Vec3 currentVelocity = entity.getDeltaMovement();

        return analyze(entityCenter, currentVelocity, ownerCenter, pressureLevel, maxRange);
    }

    /**
     * 纯位置/速度分析（不需要实体引用）
     * 适用于客户端预测或其他场景
     */
    public static VelocityResult analyze(Vec3 position, Vec3 velocity, Vec3 ownerCenter,
                                         int pressureLevel, double maxRange) {
        VelocityResult result = new VelocityResult();

        // === 基础距离计算 ===
        double distance = ownerCenter.distanceTo(position);
        if (distance < 0.1) distance = 0.1;

        double balanceRadius = BalancePointCalculator.getBalanceRadius(pressureLevel, maxRange);

        result.distance = distance;
        result.balanceRadius = balanceRadius;
        result.distanceFromBalance = distance - balanceRadius;

        // === 使用 VelocityAnalyzer 做速度分解 ===
        VelocityAnalyzer.VelocityComponents components =
                VelocityAnalyzer.decompose(position, velocity, ownerCenter);

        result.originalVelocity = velocity;
        result.processedVelocity = velocity;
        result.directionFromOwner = VelocityAnalyzer.getPushDirection(position, ownerCenter);
        result.approachSpeed = Math.max(0, components.radial);
        result.isApproaching = components.isApproaching();
        result.isRetreating = components.isRetreating();

        // === 区域判断 ===
        if (distance > maxRange) {
            result.zone = Zone.OUTSIDE;
        } else if (distance <= balanceRadius) {
            result.zone = Zone.PUSH;
        } else {
            result.zone = Zone.SLOWDOWN;
        }

        result.isInPushZone = result.zone == Zone.PUSH;

        // === 芝诺倍率 ===
        result.zenoMultiplier = BalancePointCalculator.calculateZenoMultiplier(
                distance, balanceRadius, maxRange);

        // === 阻力强度 ===
        if (result.zone == Zone.SLOWDOWN) {
            double slowdownRange = maxRange - balanceRadius;
            if (slowdownRange > 0) {
                double distanceFromEdge = maxRange - distance;
                result.resistanceStrength = Math.min(1.0, distanceFromEdge / slowdownRange);
            }
        } else if (result.zone == Zone.PUSH) {
            result.resistanceStrength = 1.0;
        }

        return result;
    }

    // ==================== 快捷方法 ====================

    /**
     * 快速判断是否需要干预
     */
    public static boolean shouldIntervene(Vec3 position, Vec3 velocity, Vec3 ownerCenter,
                                          int pressureLevel, double maxRange) {
        double distance = ownerCenter.distanceTo(position);
        if (distance > maxRange) return false;

        double balanceRadius = BalancePointCalculator.getBalanceRadius(pressureLevel, maxRange);

        // 在平衡点内总是需要干预（推出）
        if (distance <= balanceRadius) return true;

        // 减速区：只有接近时才干预
        return VelocityAnalyzer.isApproaching(position, velocity, ownerCenter);
    }

    public static class VelocityResult {
        public Vec3 originalVelocity = Vec3.ZERO;
        public Vec3 processedVelocity = Vec3.ZERO;
        public Vec3 directionFromOwner = Vec3.ZERO;
        public double distance = 0;
        public double balanceRadius = 0;
        public double distanceFromBalance = 0;
        public double resistanceStrength = 0;
        public double approachSpeed = 0;
        public double zenoMultiplier = 0;
        public boolean isInPushZone = false;
        public boolean isApproaching = false;
        public boolean isRetreating = false;
        public Zone zone = Zone.OUTSIDE;
    }
}
