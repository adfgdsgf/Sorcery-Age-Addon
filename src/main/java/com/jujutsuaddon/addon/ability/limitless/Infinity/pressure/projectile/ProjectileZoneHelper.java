package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * 投射物区域判断辅助类
 *
 * ★★★ 现在使用芝诺曲线计算减速 ★★★
 *
 * 区域划分（从玩家向外）：
 * 玩家 → 推力区 → 缓冲区 → 静止区(平衡点) → 减速区(芝诺曲线) → 正常
 *
 * - 推力区：投射物被强力推出
 * - 缓冲区：投射物保持静止，不受任何力影响
 * - 静止区：投射物悬浮在平衡点附近
 * - 减速区：投射物按芝诺曲线减速
 */
public class ProjectileZoneHelper {

    // ==================== 区域参数 ====================

    /** 滞后缓冲：静止投射物需要超过这个额外距离才会重新移动 */
    public static final float HYSTERESIS_BUFFER = 0.4f;

    /** 目标位置偏移比例（投射物稳定在静止区边界外侧）*/
    public static final float TARGET_OFFSET_RATIO = 0.6f;

    /** 缓冲区厚度：推力区和静止区之间的保护层 */
    public static final float BUFFER_ZONE_THICKNESS = 0.3f;

    // ==================== 推动阈值 ====================

    /** 最小推动阈值：低于这个值不移动位置 */
    public static final double MIN_PUSH_THRESHOLD = 0.005;

    /** 边界推动触发死区 */
    public static final double BOUNDARY_PUSH_DEADZONE = 0.15;

    /** 玩家移动检测阈值 */
    public static final double PLAYER_MOVEMENT_THRESHOLD = 0.03;

    /** 玩家接近检测阈值 */
    public static final double PLAYER_APPROACH_THRESHOLD = 0.02;

    // ==================== 区域枚举 ====================

    public enum Zone {
        PUSH,       // 推力区：被强力推出
        BUFFER,     // 缓冲区：完全静止，不受力
        STOP,       // 静止区：悬浮在平衡点
        SLOWDOWN,   // 减速区：芝诺曲线减速
        OUTSIDE     // 范围外
    }

    // ==================== 区域判断 ====================

    /**
     * 获取投射物当前所在区域
     *
     * @param distance 投射物到玩家的距离
     * @param balanceRadius 平衡点半径（从 BalancePointCalculator 获取）
     * @param maxRange 最大范围
     * @return 所在区域
     */
    public static Zone getZone(double distance, double balanceRadius, double maxRange) {
        // 推力区边界 = balanceRadius - 缓冲区厚度
        double pushZoneBoundary = Math.max(0.1, balanceRadius - BUFFER_ZONE_THICKNESS);

        // 缓冲区边界 = balanceRadius
        double bufferZoneBoundary = balanceRadius;

        // 静止区边界 = balanceRadius + 滞后缓冲
        double stopZoneBoundary = balanceRadius + HYSTERESIS_BUFFER;

        if (distance <= pushZoneBoundary) {
            return Zone.PUSH;
        } else if (distance <= bufferZoneBoundary) {
            return Zone.BUFFER;
        } else if (distance <= stopZoneBoundary) {
            return Zone.STOP;
        } else if (distance <= maxRange) {
            return Zone.SLOWDOWN;
        } else {
            return Zone.OUTSIDE;
        }
    }

    /**
     * 判断投射物是否在停止区（包括缓冲区）
     */
    public static boolean isInStopZone(Projectile projectile, double distance, double balanceRadius) {
        boolean wasStationary = isStationary(projectile);
        double effectiveBoundary = wasStationary
                ? balanceRadius + HYSTERESIS_BUFFER
                : balanceRadius;
        return distance <= effectiveBoundary;
    }

    /**
     * 判断投射物是否在缓冲区
     */
    public static boolean isInBufferZone(double distance, double balanceRadius) {
        double pushZoneBoundary = Math.max(0.1, balanceRadius - BUFFER_ZONE_THICKNESS);
        return distance > pushZoneBoundary && distance <= balanceRadius;
    }

    /**
     * 判断投射物是否在推力区
     */
    public static boolean isInPushZone(double distance, double balanceRadius) {
        double pushZoneBoundary = Math.max(0.1, balanceRadius - BUFFER_ZONE_THICKNESS);
        return distance <= pushZoneBoundary;
    }

    /**
     * 投射物是否处于静止状态
     */
    public static boolean isStationary(Projectile projectile) {
        if (projectile instanceof IFrozenProjectile fp) {
            return fp.jujutsuAddon$getSpeedMultiplier() < 0.05f;
        }
        return projectile.getDeltaMovement().lengthSqr() < 0.001;
    }

    /**
     * 计算目标稳定位置（平衡点外侧）
     */
    public static double getTargetDistance(double balanceRadius) {
        return balanceRadius + HYSTERESIS_BUFFER * TARGET_OFFSET_RATIO;
    }

    /**
     * 判断是否需要边界推动
     */
    public static boolean needsBoundaryPush(double distance, double balanceRadius) {
        double targetDist = getTargetDistance(balanceRadius);
        return distance < targetDist - BOUNDARY_PUSH_DEADZONE;
    }

    /**
     * 计算边界推动距离
     */
    public static double calculateBoundaryPushDistance(double distance, double balanceRadius, boolean wasStationary) {
        if (!needsBoundaryPush(distance, balanceRadius)) {
            return 0;
        }

        double targetDist = getTargetDistance(balanceRadius);
        double needMove = targetDist - distance;

        double pushRate = wasStationary ? 0.25 : 0.4;
        return Math.min(needMove * pushRate, 0.4);
    }

    /**
     * 判断推动是否足够大
     */
    public static boolean isSignificantPush(Vec3 pushVelocity) {
        return pushVelocity.lengthSqr() > MIN_PUSH_THRESHOLD * MIN_PUSH_THRESHOLD;
    }

    // ==================== ★★★ 芝诺曲线减速计算 ★★★ ====================

    /**
     * 使用芝诺曲线计算减速区的速度倍率
     *
     * @param distance 当前距离
     * @param balanceRadius 平衡点半径
     * @param maxRange 最大范围
     * @return 速度倍率 (0.0 ~ 1.0)
     */
    public static float calculateSlowdownSpeedZeno(double distance, double balanceRadius, double maxRange) {
        // 平衡点内：完全静止
        double effectiveStop = balanceRadius + HYSTERESIS_BUFFER;
        if (distance <= effectiveStop) {
            return 0f;
        }

        // 过渡缓冲区（刚离开平衡点时）
        double bufferZone = 0.3;
        double bufferEnd = effectiveStop + bufferZone;

        if (distance <= bufferEnd) {
            // 缓冲区内：极低速度
            float bufferRatio = (float) ((distance - effectiveStop) / bufferZone);
            return 0.02f + bufferRatio * 0.03f;  // 0.02 ~ 0.05
        }

        // ★★★ 使用芝诺曲线计算减速 ★★★
        double zenoMultiplier = BalancePointCalculator.calculateZenoMultiplier(distance, balanceRadius, maxRange);

        // zenoMultiplier: 1.0（平衡点）→ 0.0（边界）
        // 我们需要反过来：0.0（平衡点）→ 1.0（边界）
        float speedRatio = 1.0f - (float) zenoMultiplier;

        // 限制最小速度（防止在边界处完全静止）
        float minSpeed = 0.05f;
        float maxSpeed = (float) PressureConfig.getProjectileEntrySpeed();

        return minSpeed + speedRatio * (maxSpeed - minSpeed);
    }

    /**
     * 旧版线性减速（保留兼容）
     */
    public static float calculateSlowdownSpeedLinear(float distance, float stopDistance, float maxRange,
                                                     float entrySpeed, float stopSpeed) {
        float effectiveStop = stopDistance + HYSTERESIS_BUFFER;

        if (distance <= effectiveStop) return 0f;

        float bufferZone = 0.3f;
        float bufferEnd = effectiveStop + bufferZone;

        if (distance <= bufferEnd) {
            float bufferRatio = (distance - effectiveStop) / bufferZone;
            return stopSpeed * 0.5f + bufferRatio * (stopSpeed * 0.5f);
        }

        float slowdownZoneSize = maxRange - bufferEnd;
        if (slowdownZoneSize < 0.5f) slowdownZoneSize = 0.5f;

        float distanceFromBuffer = distance - bufferEnd;
        float t = Math.min(1.0f, distanceFromBuffer / slowdownZoneSize);
        float easeOut = 1.0f - (1.0f - t) * (1.0f - t);
        return stopSpeed + (entrySpeed - stopSpeed) * easeOut;
    }

    /**
     * 主要的减速计算方法（使用芝诺曲线）
     */
    public static float calculateSlowdownSpeed(float distance, float stopDistance, float maxRange,
                                               float entrySpeed, float stopSpeed) {
        // ★★★ 使用芝诺曲线 ★★★
        return calculateSlowdownSpeedZeno(distance, stopDistance, maxRange);
    }
}
