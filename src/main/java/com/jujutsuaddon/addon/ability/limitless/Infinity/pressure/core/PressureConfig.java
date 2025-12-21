package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core;

import com.jujutsuaddon.addon.AbilityConfig;

/**
 * 无下限压力系统配置
 *
 * ★★★ 芝诺悖论模型 ★★★
 *
 * 区域结构：
 *   玩家 ──── 平衡点(墙) ──── 边界
 *              ↑
 *         balanceRadius
 *
 * - 平衡点内：倍率 > 1 → 被推开
 * - 平衡点处：倍率 = 1 → 静止
 * - 平衡点外：倍率 < 1 → 减速但能移动
 */
public class PressureConfig {

    // ==================== 总开关 ====================

    public static boolean isEnabled() {
        return AbilityConfig.COMMON.enableInfinityPressure.get();
    }

    // ==================== 范围计算 ====================

    public static double getLevel1Range() {
        return AbilityConfig.COMMON.level1Range.get();
    }

    public static double getLevel10Range() {
        return AbilityConfig.COMMON.level10Range.get();
    }

    public static double getRangeCurveExponent() {
        return AbilityConfig.COMMON.rangeCurveExponent.get();
    }

    /**
     * 获取指定等级的最大范围
     */
    public static double getLevelRange(int level) {
        if (level <= 0) return getLevel1Range() * 0.5;
        if (level >= 10) return getLevel10Range();

        double minRange = getLevel1Range();
        double maxRange = getLevel10Range();
        double exponent = getRangeCurveExponent();

        double t = level / 10.0;
        return minRange + (maxRange - minRange) * Math.pow(t, exponent);
    }

    // ==================== 区域占比（芝诺模型）====================

    /**
     * 减速区份数（平衡点到边界）
     */
    public static double getSlowdownZoneParts() {
        return AbilityConfig.COMMON.slowdownZoneParts.get();
    }

    /**
     * 平衡点份数（墙的位置）
     */
    public static double getBalancePointParts() {
        return AbilityConfig.COMMON.balancePointParts.get();
    }

    /**
     * 总份数
     */
    public static double getTotalParts() {
        return getSlowdownZoneParts() + getBalancePointParts();
    }

    // ==================== 芝诺参数 ====================

    public static double getBalanceRadiusMinimum() {
        return AbilityConfig.COMMON.balanceRadiusMinimum.get();
    }

    public static double getBalanceRadiusMaxRatio() {
        return AbilityConfig.COMMON.balanceRadiusMaxRatio.get();
    }

    public static double getZenoCurveExponent() {
        return AbilityConfig.COMMON.zenoCurveExponent.get();
    }

    public static double getZenoRatio() {
        return AbilityConfig.COMMON.zenoRatio.get();
    }

    /**
     * ★★★ 核心：获取平衡点半径（墙的位置）★★★
     *
     * 这是芝诺模型的核心参数
     */
    public static double getBalanceRadius(int level) {
        double totalRange = getLevelRange(level);
        return totalRange * (getBalancePointParts() / getTotalParts());
    }

    // ==================== 推力 ====================

    public static double getMaxPushForce() {
        return AbilityConfig.COMMON.maxPushForce.get();
    }

    public static double getPinForce() {
        return AbilityConfig.COMMON.pinForce.get();
    }

    // ==================== 压力值 ====================

    public static double getBasePressure() {
        return AbilityConfig.COMMON.basePressure.get();
    }

    public static double getDistanceDecay() {
        return AbilityConfig.COMMON.distanceDecay.get();
    }

    public static double getApproachMultiplier() {
        return AbilityConfig.COMMON.approachMultiplier.get();
    }

    public static double getBreachPressureMult() {
        return AbilityConfig.COMMON.breachPressureMult.get();
    }

    // ==================== 等级因子 ====================

    public static double getMinLevelMult() {
        return AbilityConfig.COMMON.minLevelMult.get();
    }

    public static double getMaxLevelMult() {
        return AbilityConfig.COMMON.maxLevelMult.get();
    }

    public static double calculateLevelFactor(int level) {
        if (level <= 0) return 0.05;
        if (level >= 10) return getMaxLevelMult();
        return getMinLevelMult() + (level - 1) * (getMaxLevelMult() - getMinLevelMult()) / 9.0;
    }

    // ==================== 伤害 ====================

    public static double getMinPressureForDamage() {
        return AbilityConfig.COMMON.minPressureForDamage.get();
    }

    public static float getMaxDamagePerHit() {
        return AbilityConfig.COMMON.maxDamagePerHit.get().floatValue();
    }

    public static int getMinDamageInterval() {
        return AbilityConfig.COMMON.minDamageInterval.get();
    }

    public static int getMaxDamageInterval() {
        return AbilityConfig.COMMON.maxDamageInterval.get();
    }

    // ==================== 投射物 ====================

    public static boolean shouldAffectProjectiles() {
        return AbilityConfig.COMMON.affectProjectiles.get();
    }

    public static int getProjectileMinPressure() {
        return AbilityConfig.COMMON.projectileMinLevel.get();
    }

    public static double getProjectileEntrySpeed() {
        return AbilityConfig.COMMON.projectileEntrySpeed.get();
    }

    public static double getReflectSpeedMultiplier() {
        return AbilityConfig.COMMON.reflectSpeedMultiplier.get();
    }

    // ==================== 方块 ====================

    public static boolean shouldActuallyBreakBlocks() {
        return AbilityConfig.COMMON.actuallyBreakBlocks.get();
    }

    public static boolean shouldDropBlockItems() {
        return AbilityConfig.COMMON.dropBlockItems.get();
    }

    public static double getMinPressureForBlockBreak() {
        return AbilityConfig.COMMON.minPressureForBlockBreak.get();
    }

    public static float getBreakThresholdMult() {
        return AbilityConfig.COMMON.breakThresholdMult.get().floatValue();
    }

    // ==================== 方块硬度 ====================

    public static double getSoftHardnessThreshold() {
        return AbilityConfig.COMMON.softHardnessThreshold.get();
    }

    public static double getHardHardnessThreshold() {
        return AbilityConfig.COMMON.hardHardnessThreshold.get();
    }

    public static double getSoftBlockPressureMult() {
        return AbilityConfig.COMMON.softBlockPressureMult.get();
    }

    public static double getNormalBlockPressureMult() {
        return AbilityConfig.COMMON.normalBlockPressureMult.get();
    }

    public static double getHardBlockPressureMult() {
        return AbilityConfig.COMMON.hardBlockPressureMult.get();
    }

    public static double getBedrockPressureMult() {
        return AbilityConfig.COMMON.bedrockPressureMult.get();
    }

    public static double getHardnessPressureMult(float blockHardness) {
        if (blockHardness < 0) {
            return getBedrockPressureMult();
        }
        double softThreshold = getSoftHardnessThreshold();
        double hardThreshold = getHardHardnessThreshold();
        if (blockHardness <= softThreshold) {
            return getSoftBlockPressureMult();
        } else if (blockHardness >= hardThreshold) {
            return getHardBlockPressureMult();
        } else {
            double t = (blockHardness - softThreshold) / (hardThreshold - softThreshold);
            return getNormalBlockPressureMult() + t * (getHardBlockPressureMult() - getNormalBlockPressureMult());
        }
    }

    // ==================== 掉落物 ====================

    public static boolean shouldPushDroppedItems() {
        return AbilityConfig.COMMON.pushDroppedItems.get();
    }

    public static double getItemPushForceMultiplier() {
        return AbilityConfig.COMMON.itemPushForceMultiplier.get();
    }

    // ==================== 效果 ====================

    public static boolean areParticlesEnabled() {
        return AbilityConfig.COMMON.enableParticles.get();
    }

    public static boolean areSoundsEnabled() {
        return AbilityConfig.COMMON.enableSounds.get();
    }

    // ==================== 交互 ====================

    public static boolean respectDomainSureHit() {
        return AbilityConfig.COMMON.respectDomainSureHit.get();
    }

    public static boolean respectDomainAmplification() {
        return AbilityConfig.COMMON.respectDomainAmplification.get();
    }

    // ==================== 咒力消耗 ====================

    public static boolean isPressureCostEnabled() {
        return AbilityConfig.COMMON.enablePressureCost.get();
    }

    public static float getBaseCursedEnergyCost() {
        return AbilityConfig.COMMON.baseCursedEnergyCost.get().floatValue();
    }

    public static float getCostPerPressureLevel() {
        return AbilityConfig.COMMON.costPerLevel.get().floatValue();
    }

    public static float getMaxCostMultiplier() {
        return AbilityConfig.COMMON.maxCostMultiplier.get().floatValue();
    }

    public static float calculatePressureCost(int pressureLevel) {
        float baseCost = getBaseCursedEnergyCost();
        if (pressureLevel <= 0) return baseCost;

        float multiplier = 1.0F + (pressureLevel * getCostPerPressureLevel());
        multiplier = Math.min(multiplier, getMaxCostMultiplier());
        return baseCost * multiplier;
    }

    // ==================== 压力曲线 ====================

    public static double getMaxPressureMultiplier() {
        return AbilityConfig.COMMON.maxPressureMultiplier.get();
    }

    public static double getCurveSteepness() {
        return AbilityConfig.COMMON.curveSteepness.get();
    }

    public static double getCollisionMinDistance() {
        return AbilityConfig.COMMON.collisionMinDistance.get();
    }

    public static float getBlockPressureRate() {
        return AbilityConfig.COMMON.blockPressureRate.get().floatValue();
    }

    // ==================== 内部常量（固定值）====================

    /** 方块压力超时（毫秒）*/
    public static long getPressureTimeoutMs() {
        return 2000L;
    }

    /** Minecraft 方块破坏阶段数（固定10）*/
    public static int getBreakStages() {
        return 10;
    }

    /** 方块压力衰减速率（每tick）*/
    public static float getPressureDecayRate() {
        return 0.2f;
    }

    /** 掉落物推力基础值 */
    public static double getBasePushForce() {
        return 0.015;
    }

    /** 掉落物推力最低等级 */
    public static int getItemPushMinPressure() {
        return 1;
    }

    /** 基础伤害间隔 */
    public static int getBaseDamageInterval() {
        return getMaxDamageInterval();
    }

    /** 压力激增阈值 */
    public static double getPressureSurgeThreshold() {
        return 5.0;
    }

    /** 伤害警告tick数 */
    public static int getDamageWarningTicks() {
        return 5;
    }

    private PressureConfig() {}
}
