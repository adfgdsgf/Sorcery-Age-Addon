package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * 投射物区域判断辅助类
 *
 * 芝诺模型区域：
 * - 平衡点内（Push/Buffer）：速度 = 0
 * - 平衡点外（Slowdown）：速度按芝诺公式衰减
 */
public class ProjectileZoneHelper {

    // ==================== 区域参数（类内部常量）====================
    // 这些是实现细节，不需要用户配置

    /** 迟滞缓冲（防止在边界抖动）*/
    public static final float HYSTERESIS_BUFFER = 0.4f;

    /** 缓冲区厚度（平衡点内的安全区）*/
    public static final float BUFFER_ZONE_THICKNESS = 0.3f;

    /** 最小推力阈值 */
    public static final double MIN_PUSH_THRESHOLD = 0.005;

    /** 玩家移动检测阈值 */
    public static final double PLAYER_MOVEMENT_THRESHOLD = 0.03;

    /** 玩家接近检测阈值 */
    public static final double PLAYER_APPROACH_THRESHOLD = 0.02;

    /** 最大推动距离 */
    public static final double MAX_PUSH_AMOUNT = 0.5;

    /** 最小有效移动速度 */
    public static final double MIN_OWNER_SPEED = 0.01;

    /** 减速区最低速度 */
    public static final float SLOWDOWN_MIN_SPEED = 0.05f;

    /** 过渡缓冲区大小 */
    public static final double TRANSITION_BUFFER = 0.3;

    // ==================== 区域枚举 ====================

    public enum Zone {
        PUSH,       // 推力区（太靠近玩家）
        BUFFER,     // 缓冲区（平衡点附近）
        STOP,       // 静止区（在平衡点）
        SLOWDOWN,   // 减速区（平衡点外）
        OUTSIDE     // 范围外
    }

    // ==================== 区域判断 ====================

    /**
     * 获取投射物所在区域
     */
    public static Zone getZone(double distance, double balanceRadius, double maxRange) {
        double pushZoneBoundary = Math.max(0.1, balanceRadius - BUFFER_ZONE_THICKNESS);
        double stopZoneBoundary = balanceRadius + HYSTERESIS_BUFFER;

        if (distance <= pushZoneBoundary) {
            return Zone.PUSH;
        } else if (distance <= balanceRadius) {
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
     * 是否在停止区（包含迟滞）
     */
    public static boolean isInStopZone(Projectile projectile, double distance, double balanceRadius) {
        boolean wasStationary = isStationary(projectile);
        double effectiveBoundary = wasStationary
                ? balanceRadius + HYSTERESIS_BUFFER
                : balanceRadius;
        return distance <= effectiveBoundary;
    }

    /**
     * 是否在缓冲区
     */
    public static boolean isInBufferZone(double distance, double balanceRadius) {
        double pushZoneBoundary = Math.max(0.1, balanceRadius - BUFFER_ZONE_THICKNESS);
        return distance > pushZoneBoundary && distance <= balanceRadius;
    }

    /**
     * 是否在推力区
     */
    public static boolean isInPushZone(double distance, double balanceRadius) {
        double pushZoneBoundary = Math.max(0.1, balanceRadius - BUFFER_ZONE_THICKNESS);
        return distance <= pushZoneBoundary;
    }

    /**
     * 投射物是否静止
     */
    public static boolean isStationary(Projectile projectile) {
        if (projectile instanceof IFrozenProjectile fp) {
            return fp.jujutsuAddon$getSpeedMultiplier() < 0.05f;
        }
        return projectile.getDeltaMovement().lengthSqr() < 0.001;
    }

    /**
     * 获取目标距离（停止位置）
     */
    public static double getTargetDistance(double balanceRadius) {
        return balanceRadius + HYSTERESIS_BUFFER * 0.6;
    }

    /**
     * 推力是否显著
     */
    public static boolean isSignificantPush(Vec3 pushVelocity) {
        return pushVelocity.lengthSqr() > MIN_PUSH_THRESHOLD * MIN_PUSH_THRESHOLD;
    }

    // ==================== ★★★ 芝诺减速计算（使用已有工具类）★★★ ====================

    /**
     * 使用芝诺公式计算投射物速度倍率
     *
     * @param distance       当前距离
     * @param balanceRadius  平衡点半径
     * @param maxRange       最大范围
     * @return 速度倍率 (0.0 ~ 1.0)
     */
    public static float calculateSlowdownSpeedZeno(double distance, double balanceRadius, double maxRange) {
        double effectiveStop = balanceRadius + HYSTERESIS_BUFFER;

        // 平衡点内：停止
        if (distance <= effectiveStop) {
            return 0f;
        }

        // 过渡缓冲区
        double bufferEnd = effectiveStop + TRANSITION_BUFFER;

        if (distance <= bufferEnd) {
            float bufferRatio = (float) ((distance - effectiveStop) / TRANSITION_BUFFER);
            return 0.02f + bufferRatio * 0.03f;
        }

        // ★★★ 使用 BalancePointCalculator 计算芝诺倍率 ★★★
        double zenoMultiplier = BalancePointCalculator.calculateZenoMultiplier(
                distance, balanceRadius, maxRange);

        float speedRatio = (float) zenoMultiplier;

        // ★★★ 使用 PressureConfig 获取入口速度 ★★★
        float maxSpeed = (float) PressureConfig.getProjectileEntrySpeed();

        return SLOWDOWN_MIN_SPEED + speedRatio * (maxSpeed - SLOWDOWN_MIN_SPEED);
    }

    /**
     * 计算芝诺移动距离
     *
     * @param distance       当前距离
     * @param balanceRadius  平衡点
     * @param originalSpeed  原始速度
     * @return 这一tick能移动的距离
     */
    public static double calculateZenoMove(double distance, double balanceRadius, double originalSpeed) {
        // ★★★ 使用 BalancePointCalculator ★★★
        return BalancePointCalculator.calculateTrueZenoMove(distance, balanceRadius, originalSpeed);
    }
}
