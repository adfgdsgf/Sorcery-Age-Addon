package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core;

/**
 * ★★★ 平衡点统一计算器 ★★★
 * 所有需要计算芝诺倍率的地方都必须调用这里！
 */
public final class BalancePointCalculator {

    // ★★★ 简单缓存（同一tick内避免重复计算）★★★
    private static int cachedLevel = -1;
    private static double cachedMaxRange = -1;
    private static double cachedBalanceRadius = -1;
    private static long cachedTick = -1;

    /**
     * 获取平衡点半径
     */
    public static double getBalanceRadius(int pressureLevel, double maxRange) {
        // ★★★ 缓存检查（参数相同时直接返回）★★★
        if (pressureLevel == cachedLevel &&
                Math.abs(maxRange - cachedMaxRange) < 0.001) {
            return cachedBalanceRadius;
        }

        double baseRadius = PressureConfig.getStopZoneRadius(pressureLevel);

        // 从配置读取边界值
        double maxRatio = PressureConfig.getBalanceRadiusMaxRatio();
        double minimum = PressureConfig.getBalanceRadiusMinimum();

        baseRadius = Math.min(baseRadius, maxRange * maxRatio);
        baseRadius = Math.max(baseRadius, minimum);

        // ★★★ 更新缓存 ★★★
        cachedLevel = pressureLevel;
        cachedMaxRange = maxRange;
        cachedBalanceRadius = baseRadius;

        return baseRadius;
    }

    /**
     * 计算芝诺倍率
     * 使用 (1-p)^n 曲线，让大部分区域都有明显阻力
     */
    public static double calculateZenoMultiplier(double distance, double balanceRadius, double maxRange) {
        if (distance > maxRange) return 0;
        distance = Math.max(distance, 0.1);
        if (distance <= balanceRadius) {
            return 1.0;
        }
        double fadeZone = maxRange - balanceRadius;
        if (fadeZone < 0.1) return 1.0;
        double progress = (distance - balanceRadius) / fadeZone;
        progress = Math.min(1.0, progress);
        // 使用 (1-p)^n 曲线，n 越大越陡峭
        double exponent = PressureConfig.getZenoCurveExponent();
        return Math.pow(1.0 - progress, exponent);
    }

    /**
     * 清除缓存（配置热重载时调用）
     */
    public static void invalidateCache() {
        cachedLevel = -1;
        cachedMaxRange = -1;
        cachedBalanceRadius = -1;
    }

    private BalancePointCalculator() {}
}
