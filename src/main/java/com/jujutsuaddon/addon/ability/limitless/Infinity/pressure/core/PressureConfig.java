package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core;

import com.jujutsuaddon.addon.AbilityConfig;

/**
 * 无下限压力系统配置
 * 所有值从 AbilityConfig 读取，支持热重载
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
     * 获取指定等级的总范围（平方曲线）
     * 公式: level1Range + (level10Range - level1Range) × (level/10)^exponent
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

    // ==================== 区域占比 ====================

    public static double getSlowdownZoneParts() {
        return AbilityConfig.COMMON.slowdownZoneParts.get();
    }

    public static double getStopZoneParts() {
        return AbilityConfig.COMMON.stopZoneParts.get();
    }

    public static double getPushZoneParts() {
        return AbilityConfig.COMMON.pushZoneParts.get();
    }

    public static double getTotalParts() {
        return getSlowdownZoneParts() + getStopZoneParts() + getPushZoneParts();
    }

    // ==================== 区域边界计算 ====================

    /**
     * 推力区半径（最内层，0 ~ pushRadius）
     * 进入这里 = 突破，会被强推+伤害
     */
    public static double getPushZoneRadius(int level) {
        double totalRange = getLevelRange(level);
        return totalRange * (getPushZoneParts() / getTotalParts());
    }

    /**
     * 停止区外边界（pushRadius ~ stopRadius）
     * 投射物在这里悬浮，生物被强推
     */
    public static double getStopZoneRadius(int level) {
        double totalRange = getLevelRange(level);
        double pushRatio = getPushZoneParts() / getTotalParts();
        double stopRatio = getStopZoneParts() / getTotalParts();
        return totalRange * (pushRatio + stopRatio);
    }

    /**
     * 减速区起始 = 停止区外边界
     */
    public static double getSlowdownZoneStart(int level) {
        return getStopZoneRadius(level);
    }

    /**
     * 兼容旧代码：haltDistance 现在等于 pushZoneRadius
     */
    public static double getHaltDistance(int level) {
        return getPushZoneRadius(level);
    }

    /**
     * 兼容旧代码：过渡区
     */
    public static double getHaltTransitionZone() {
        return 1.5;
    }

    // ==================== 推力 ====================

    public static double getBasePushForce() {
        return AbilityConfig.COMMON.basePushForce.get();
    }

    public static double getMaxPushForce() {
        return AbilityConfig.COMMON.maxPushForce.get();
    }

    public static double getBreachRepelForce() {
        return AbilityConfig.COMMON.breachRepelForce.get();
    }

    public static double getPinForce() {
        return AbilityConfig.COMMON.pinForce.get();
    }

    // ==================== 阻力 ====================

    public static double getLateralResistance() {
        return AbilityConfig.COMMON.lateralResistance.get();
    }

    public static double getEscapeResistance() {
        return AbilityConfig.COMMON.escapeResistance.get();
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

    public static double getPressureToDamage() {
        return AbilityConfig.COMMON.pressureToDamage.get();
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

    public static double getSurgeDamageMult() {
        return AbilityConfig.COMMON.surgeDamageMult.get();
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

    public static double getProjectileStopSpeed() {
        return AbilityConfig.COMMON.projectileStopSpeed.get();
    }

    // ★★★ 新增：投射物反弹速度倍率 ★★★
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

    // ==================== 方块硬度影响 ====================
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
    /**
     * 根据方块硬度计算压力倍率
     */
    public static double getHardnessPressureMult(float blockHardness) {
        // 基岩或不可破坏
        if (blockHardness < 0) {
            return getBedrockPressureMult();
        }
        double softThreshold = getSoftHardnessThreshold();
        double hardThreshold = getHardHardnessThreshold();
        if (blockHardness <= softThreshold) {
            // 软方块
            return getSoftBlockPressureMult();
        } else if (blockHardness >= hardThreshold) {
            // 硬方块
            return getHardBlockPressureMult();
        } else {
            // 普通方块：线性插值
            double t = (blockHardness - softThreshold) / (hardThreshold - softThreshold);
            double normalMult = getNormalBlockPressureMult();
            double hardMult = getHardBlockPressureMult();
            return normalMult + t * (hardMult - normalMult);
        }
    }

    // ==================== 兼容旧代码的固定值 ====================

    public static double getMinPressureForPush() { return 0.5; }
    public static float getPressureDecayRate() { return 0.2f; }
    public static int getBreakStages() { return 10; }
    public static long getPressureTimeoutMs() { return 2000L; }
    public static double getPressureChangeDamageMult() { return 0.5; }
    public static double getIntervalPressureScale() { return 1.5; }
    public static double getPressureSurgeThreshold() { return 5.0; }
    public static int getDamageWarningTicks() { return 5; }
    public static int getItemPushMinPressure() { return 1; }
    public static double getApproachResistanceBase() { return 0.3; }
    public static double getApproachResistanceMax() { return 0.95; }

    private PressureConfig() {}
}
