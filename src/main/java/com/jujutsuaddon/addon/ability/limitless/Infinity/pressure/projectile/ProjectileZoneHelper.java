package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * 投射物区域判断辅助类
 *
 * 区域划分（从玩家向外）：
 * 玩家 → 推力区 → 缓冲区 → 静止区 → 减速区 → 正常
 *
 * - 推力区：投射物被强力推出
 * - 缓冲区：投射物保持静止，不受任何力影响，不转向
 * - 静止区：投射物悬浮，可能被玩家移动推动
 * - 减速区：投射物减速飞行
 */
public class ProjectileZoneHelper {

    // ==================== 区域参数 ====================

    // 滞后缓冲：静止投射物需要超过这个额外距离才会重新移动
    public static final float HYSTERESIS_BUFFER = 0.4f;

    // 目标位置偏移比例（投射物稳定在静止区边界外侧）
    public static final float TARGET_OFFSET_RATIO = 0.6f;

    // ★★★ 缓冲区厚度：推力区和静止区之间的保护层 ★★★
    // 在这个区域内，投射物完全静止，不受任何推力，不转向
    public static final float BUFFER_ZONE_THICKNESS = 0.3f;

    // ==================== 推动阈值 ====================

    // 最小推动阈值：低于这个值不移动位置
    public static final double MIN_PUSH_THRESHOLD = 0.005;

    // 边界推动触发死区
    public static final double BOUNDARY_PUSH_DEADZONE = 0.15;

    // 玩家移动检测阈值
    public static final double PLAYER_MOVEMENT_THRESHOLD = 0.03;

    // 玩家接近检测阈值
    public static final double PLAYER_APPROACH_THRESHOLD = 0.02;

    // ==================== 区域判断 ====================

    /**
     * 判断投射物所在区域
     */
    public enum Zone {
        PUSH,       // 推力区：被强力推出
        BUFFER,     // 缓冲区：完全静止，不受力，不转向
        STOP,       // 静止区：悬浮，可被玩家移动推动
        SLOWDOWN,   // 减速区：减速飞行
        OUTSIDE     // 范围外
    }

    /**
     * 获取投射物当前所在区域
     *
     * @param distance 投射物到玩家的距离
     * @param stopDistance 静止区边界（配置的停止距离）
     * @param maxRange 最大范围
     * @return 所在区域
     */
    public static Zone getZone(double distance, float stopDistance, float maxRange) {
        // 计算各区域边界
        // 推力区边界 = stopDistance - 缓冲区厚度
        float pushZoneBoundary = Math.max(0.1f, stopDistance - BUFFER_ZONE_THICKNESS);

        // 缓冲区边界 = stopDistance（静止区开始的地方）
        float bufferZoneBoundary = stopDistance;

        // 静止区边界 = stopDistance + 滞后缓冲
        float stopZoneBoundary = stopDistance + HYSTERESIS_BUFFER;

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
     * 用于决定是否应用减速逻辑
     */
    public static boolean isInStopZone(Projectile projectile, double distance, float stopDistance) {
        boolean wasStationary = isStationary(projectile);
        float effectiveBoundary = wasStationary
                ? stopDistance + HYSTERESIS_BUFFER
                : stopDistance;
        return distance <= effectiveBoundary;
    }

    /**
     * ★★★ 判断投射物是否在缓冲区 ★★★
     * 缓冲区内：完全静止，不受推力，不转向
     */
    public static boolean isInBufferZone(double distance, float stopDistance) {
        float pushZoneBoundary = Math.max(0.1f, stopDistance - BUFFER_ZONE_THICKNESS);
        return distance > pushZoneBoundary && distance <= stopDistance;
    }

    /**
     * ★★★ 判断投射物是否在推力区 ★★★
     * 推力区内：被强力推出
     */
    public static boolean isInPushZone(double distance, float stopDistance) {
        float pushZoneBoundary = Math.max(0.1f, stopDistance - BUFFER_ZONE_THICKNESS);
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
     * 计算目标稳定位置（静止区边界外侧）
     */
    public static double getTargetDistance(float stopDistance) {
        return stopDistance + HYSTERESIS_BUFFER * TARGET_OFFSET_RATIO;
    }

    /**
     * 判断是否需要边界推动
     */
    public static boolean needsBoundaryPush(double distance, float stopDistance) {
        double targetDist = getTargetDistance(stopDistance);
        return distance < targetDist - BOUNDARY_PUSH_DEADZONE;
    }

    /**
     * 计算边界推动距离
     */
    public static double calculateBoundaryPushDistance(double distance, float stopDistance, boolean wasStationary) {
        if (!needsBoundaryPush(distance, stopDistance)) {
            return 0;
        }

        double targetDist = getTargetDistance(stopDistance);
        double needMove = targetDist - distance;

        double pushRate = wasStationary ? 0.25 : 0.4;
        return Math.min(needMove * pushRate, 0.4);
    }

    /**
     * 判断推动是否足够大，值得更新位置
     */
    public static boolean isSignificantPush(Vec3 pushVelocity) {
        return pushVelocity.lengthSqr() > MIN_PUSH_THRESHOLD * MIN_PUSH_THRESHOLD;
    }

    /**
     * 计算减速区的速度
     */
    public static float calculateSlowdownSpeed(float distance, float stopDistance, float maxRange,
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
}
