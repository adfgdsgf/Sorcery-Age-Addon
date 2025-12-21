package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core;

/**
 * ★★★ 芝诺悖论计算器 ★★★
 *
 * 核心原理：
 * - 每一步只能走"剩余距离"的一定比例
 * - 结果：无限接近但永远到不了
 */
public final class BalancePointCalculator {

    private static int cachedLevel = -1;
    private static double cachedMaxRange = -1;
    private static double cachedBalanceRadius = -1;

    // ==================== 平衡点半径 ====================

    /**
     * 获取平衡点半径
     */
    public static double getBalanceRadius(int pressureLevel, double maxRange) {
        if (pressureLevel == cachedLevel &&
                Math.abs(maxRange - cachedMaxRange) < 0.001) {
            return cachedBalanceRadius;
        }
        // ★★★ 这一行改成 getBalanceRadius ★★★
        double baseRadius = PressureConfig.getBalanceRadius(pressureLevel);
        double maxRatio = PressureConfig.getBalanceRadiusMaxRatio();
        double minimum = PressureConfig.getBalanceRadiusMinimum();
        baseRadius = Math.min(baseRadius, maxRange * maxRatio);
        baseRadius = Math.max(baseRadius, minimum);
        cachedLevel = pressureLevel;
        cachedMaxRange = maxRange;
        cachedBalanceRadius = baseRadius;
        return baseRadius;
    }

    // ==================== 芝诺倍率（速度保持率）====================

    /**
     * 计算速度保持率
     *
     * @return 0.0（平衡点）~ 1.0（边界）
     */
    public static double calculateZenoMultiplier(double distance, double balanceRadius, double maxRange) {
        if (distance >= maxRange) {
            return 1.0;
        }
        if (distance <= balanceRadius) {
            return 0.0;
        }

        double fadeZone = maxRange - balanceRadius;
        if (fadeZone < 0.1) return 0.0;

        double progress = (distance - balanceRadius) / fadeZone;
        progress = Math.max(0, Math.min(1.0, progress));

        double exponent = PressureConfig.getZenoCurveExponent();
        return Math.pow(progress, exponent);
    }

    // ==================== ★★★ 新增：真正的芝诺移动计算 ★★★ ====================

    /**
     * 计算这一tick能向平衡点移动的最大距离
     *
     * 芝诺原理：每一步最多只能走"到墙剩余距离"的一定比例
     *
     * @param currentDistance 当前距离
     * @param balanceRadius   平衡点半径（墙）
     * @param originalSpeed   原始移动速度
     * @return 这一tick实际能移动的距离
     */
    public static double calculateTrueZenoMove(double currentDistance,
                                               double balanceRadius,
                                               double originalSpeed) {
        double distanceToWall = currentDistance - balanceRadius;

        // 已经在墙内或到达墙
        if (distanceToWall <= 0.001) {
            return 0;
        }

        // ★★★ 芝诺核心 ★★★
        double zenoRatio = PressureConfig.getZenoRatio();
        double maxMoveThisTick = distanceToWall * zenoRatio;

        // 实际移动 = min(想走的速度, 芝诺允许的最大值)
        return Math.min(originalSpeed, maxMoveThisTick);
    }

    // ==================== ★★★ 新增：芝诺推力计算 ★★★ ====================

    /**
     * 计算芝诺推力
     *
     * 原理：
     * - 在平衡点：推力 = 接近速度（刚好抵消，静止）
     * - 平衡点内：推力 > 接近速度（被推出）
     * - 平衡点外：推力 < 接近速度（能接近但减速）
     *
     * @param distance      当前距离
     * @param balanceRadius 平衡点半径
     * @param approachSpeed 接近速度（正值=在靠近）
     * @return 推力大小（正值=向外推）
     */
    public static double calculateZenoPushForce(double distance,
                                                double balanceRadius,
                                                double approachSpeed) {
        if (approachSpeed <= 0) {
            // 没有在接近，不需要推力
            return 0;
        }

        if (distance >= balanceRadius) {
            // 在墙外：推力按比例抵消接近速度
            // 越接近墙，推力越接近完全抵消
            double ratio = balanceRadius / distance;
            return approachSpeed * ratio;
        } else {
            // 在墙内：推力必须大于接近速度才能推出去
            // 使用反比关系：距离越近，推力越大
            double ratio = balanceRadius / Math.max(distance, 0.1);
            return approachSpeed * ratio * ratio;  // 平方增长
        }
    }

    /**
     * 计算实体的有效移动速度
     *
     * @param distance       当前距离
     * @param balanceRadius  平衡点
     * @param maxRange       最大范围
     * @param originalSpeed  原始速度
     * @param isApproaching  是否在接近玩家
     * @return 调整后的速度
     */
    public static double calculateEffectiveSpeed(double distance,
                                                 double balanceRadius,
                                                 double maxRange,
                                                 double originalSpeed,
                                                 boolean isApproaching) {
        // 超出范围：原速
        if (distance >= maxRange) {
            return originalSpeed;
        }

        // 不是在接近：原速（可以自由离开）
        if (!isApproaching) {
            return originalSpeed;
        }

        // 使用芝诺计算
        return calculateTrueZenoMove(distance, balanceRadius, originalSpeed);
    }

    // ==================== 缓存管理 ====================

    public static void invalidateCache() {
        cachedLevel = -1;
        cachedMaxRange = -1;
        cachedBalanceRadius = -1;
    }

    private BalancePointCalculator() {}
}
