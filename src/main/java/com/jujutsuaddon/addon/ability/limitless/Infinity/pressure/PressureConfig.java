package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

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

    // ==================== 范围 ====================

    public static double getLevelRange(int level) {
        if (level <= 0) return getLevel0Range();
        if (level >= 10) return getLevel10Range();

        double t = (level - 1) / 9.0;
        double curved = t * t;
        return getLevel1Range() + curved * (getLevel10Range() - getLevel1Range());
    }

    public static double getLevel0Range() {
        return AbilityConfig.COMMON.level0Range.get();
    }

    public static double getLevel1Range() {
        return AbilityConfig.COMMON.level1Range.get();
    }

    public static double getLevel10Range() {
        return AbilityConfig.COMMON.level10Range.get();
    }

    // ==================== 停止距离 ====================

    public static double getHaltDistance(int level) {
        if (level <= 0) return 0.1;
        if (level >= 10) return getLevel10HaltDistance();

        if (level <= 5) {
            double t5 = (level - 1) / 4.0;
            return getLevel1HaltDistance() + t5 * (getLevel5HaltDistance() - getLevel1HaltDistance());
        } else {
            double t10 = (level - 5) / 5.0;
            return getLevel5HaltDistance() + t10 * (getLevel10HaltDistance() - getLevel5HaltDistance());
        }
    }

    public static double getLevel1HaltDistance() {
        return AbilityConfig.COMMON.level1HaltDistance.get();
    }

    public static double getLevel5HaltDistance() {
        return AbilityConfig.COMMON.level5HaltDistance.get();
    }

    public static double getLevel10HaltDistance() {
        return AbilityConfig.COMMON.level10HaltDistance.get();
    }

    public static double getHaltTransitionZone() {
        return AbilityConfig.COMMON.haltTransitionZone.get();
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

    // ==================== 推力 ====================

    public static double getBasePushForce() {
        return AbilityConfig.COMMON.basePushForce.get();
    }

    public static double getMinLevelMult() {
        return AbilityConfig.COMMON.minLevelMult.get();
    }

    public static double getMaxLevelMult() {
        return AbilityConfig.COMMON.maxLevelMult.get();
    }

    public static double getMaxPushForce() {
        return AbilityConfig.COMMON.maxPushForce.get();
    }

    public static double getPinForce() {
        return AbilityConfig.COMMON.pinForce.get();
    }

    public static double getHaltRepelForce() {
        return AbilityConfig.COMMON.haltRepelForce.get();
    }

    public static double getBreachRepelForce() {
        return AbilityConfig.COMMON.breachRepelForce.get();
    }

    // ==================== 阻力系统 ====================

    public static double getLateralResistance() {
        return AbilityConfig.COMMON.lateralResistance.get();
    }

    public static double getEscapeResistance() {
        return AbilityConfig.COMMON.escapeResistance.get();
    }

    public static double getApproachResistanceBase() {
        return AbilityConfig.COMMON.approachResistanceBase.get();
    }

    public static double getApproachResistanceMax() {
        return AbilityConfig.COMMON.approachResistanceMax.get();
    }

    // ==================== 压力阈值 ====================

    public static double getMinPressureForDamage() {
        return AbilityConfig.COMMON.minPressureForDamage.get();
    }

    public static double getMinPressureForBlockBreak() {
        return AbilityConfig.COMMON.minPressureForBlockBreak.get();
    }

    public static double getMinPressureForPush() {
        return AbilityConfig.COMMON.minPressureForPush.get();
    }

    // ==================== 方块破坏 ====================

    public static float getPressureDecayRate() {
        return AbilityConfig.COMMON.pressureDecayRate.get().floatValue();
    }

    public static float getBreakThresholdMult() {
        return AbilityConfig.COMMON.breakThresholdMult.get().floatValue();
    }

    public static int getBreakStages() {
        return AbilityConfig.COMMON.breakStages.get();
    }

    public static long getPressureTimeoutMs() {
        return AbilityConfig.COMMON.pressureTimeoutMs.get();
    }

    // ==================== 伤害 ====================

    public static double getPressureToDamage() {
        return AbilityConfig.COMMON.pressureToDamage.get();
    }

    public static double getPressureChangeDamageMult() {
        return AbilityConfig.COMMON.pressureChangeDamageMult.get();
    }

    public static int getMaxDamageInterval() {
        return AbilityConfig.COMMON.maxDamageInterval.get();
    }

    public static int getMinDamageInterval() {
        return AbilityConfig.COMMON.minDamageInterval.get();
    }

    public static double getIntervalPressureScale() {
        return AbilityConfig.COMMON.intervalPressureScale.get();
    }

    public static float getMaxDamagePerHit() {
        return AbilityConfig.COMMON.maxDamagePerHit.get().floatValue();
    }

    // ==================== 伤害预兆 ====================

    public static int getDamageWarningTicks() {
        return AbilityConfig.COMMON.damageWarningTicks.get();
    }

    public static double getPressureSurgeThreshold() {
        return AbilityConfig.COMMON.pressureSurgeThreshold.get();
    }

    public static double getSurgeDamageMult() {
        return AbilityConfig.COMMON.surgeDamageMult.get();
    }

    // ==================== 效果开关 ====================

    public static boolean areParticlesEnabled() {
        return AbilityConfig.COMMON.enablePressureParticles.get();
    }

    public static boolean areSoundsEnabled() {
        return AbilityConfig.COMMON.enablePressureSounds.get();
    }
    // ==================== 领域交互 ====================
    public static boolean respectDomainSureHit() {
        return AbilityConfig.COMMON.respectDomainSureHit.get();
    }
    public static boolean respectDomainAmplification() {
        return AbilityConfig.COMMON.respectDomainAmplification.get();
    }

    // ==================== 方块破坏 ==================== （添加这两个方法）
    public static boolean shouldActuallyBreakBlocks() {
        return AbilityConfig.COMMON.actuallyBreakBlocks.get();
    }
    public static boolean shouldDropBlockItems() {
        return AbilityConfig.COMMON.dropBlockItems.get();
    }
    // ==================== 掉落物 ==================== （新增部分）
    public static boolean shouldPushDroppedItems() {
        return AbilityConfig.COMMON.pushDroppedItems.get();
    }
    public static double getItemPushForceMultiplier() {
        return AbilityConfig.COMMON.itemPushForceMultiplier.get();
    }
    public static int getItemPushMinPressure() {
        return AbilityConfig.COMMON.itemPushMinPressure.get();
    }

   // ==================== 投射物 ====================
    public static boolean shouldAffectProjectiles() {
        return AbilityConfig.COMMON.affectProjectiles.get();
    }
    public static int getProjectileMinPressure() {
        return AbilityConfig.COMMON.projectileMinPressure.get();
    }
    public static double getProjectileSlowdownRate() {
        return AbilityConfig.COMMON.projectileSlowdownRate.get();
    }
    public static double getProjectileMinSpeed() {
        return AbilityConfig.COMMON.projectileMinSpeed.get();
    }
    public static double getProjectileRepelForce() {
        return AbilityConfig.COMMON.projectileRepelForce.get();
    }
    // ★ 新增：反弹相关 ★
    public static double getProjectileReflectSpeedMult() {
        return AbilityConfig.COMMON.projectileReflectSpeedMult.get();
    }
    public static double getProjectileReflectMinSpeed() {
        return AbilityConfig.COMMON.projectileReflectMinSpeed.get();
    }
    public static double getProjectileReflectMaxSpeed() {
        return AbilityConfig.COMMON.projectileReflectMaxSpeed.get();
    }
    public static int getProjectileReflectImmuneTicks() {
        return AbilityConfig.COMMON.projectileReflectImmuneTicks.get();
    }

    // ==================== 投射物高级设置 ====================

    // --- 停止区 ---
    public static double getProjectileStopZoneBuffer() {
        return AbilityConfig.COMMON.projectileStopZoneBuffer.get();
    }
    public static double getProjectileStopZoneBufferPerLevel() {
        return AbilityConfig.COMMON.projectileStopZoneBufferPerLevel.get();
    }
    public static double getProjectileStopZoneMinInner() {
        return AbilityConfig.COMMON.projectileStopZoneMinInner.get();
    }

    // --- 出力突变 ---
    public static int getProjectileSurgeCheckInterval() {
        return AbilityConfig.COMMON.projectileSurgeCheckInterval.get();
    }
    public static int getProjectileSurgeLevelThreshold() {
        return AbilityConfig.COMMON.projectileSurgeLevelThreshold.get();
    }
    public static double getProjectileSurgeOutputThreshold() {
        return AbilityConfig.COMMON.projectileSurgeOutputThreshold.get();
    }
    public static double getProjectileSurgeBaseMult() {
        return AbilityConfig.COMMON.projectileSurgeBaseMult.get();
    }
    public static double getProjectileSurgeLevelFactor() {
        return AbilityConfig.COMMON.projectileSurgeLevelFactor.get();
    }
    public static double getProjectileSurgeOutputFactor() {
        return AbilityConfig.COMMON.projectileSurgeOutputFactor.get();
    }
    public static double getProjectileSurgeMaxMult() {
        return AbilityConfig.COMMON.projectileSurgeMaxMult.get();
    }
    public static double getProjectileSurgePushBase() {
        return AbilityConfig.COMMON.projectileSurgePushBase.get();
    }
    public static double getProjectileSurgePushMax() {
        return AbilityConfig.COMMON.projectileSurgePushMax.get();
    }

    // --- 推力区 ---
    public static double getProjectilePushZoneBase() {
        return AbilityConfig.COMMON.projectilePushZoneBase.get();
    }
    public static double getProjectilePushZonePenetrationFactor() {
        return AbilityConfig.COMMON.projectilePushZonePenetrationFactor.get();
    }
    public static double getProjectilePushZoneMax() {
        return AbilityConfig.COMMON.projectilePushZoneMax.get();
    }

    // ==================== 等级因子计算 ====================

    public static double calculateLevelFactor(int level) {
        if (level <= 0) return 0.05;
        if (level >= 10) return getMaxLevelMult();

        return getMinLevelMult() + (level - 1) * (getMaxLevelMult() - getMinLevelMult()) / 9.0;
    }
    // ==================== 咒力消耗 ====================
    public static boolean isPressureCostEnabled() {
        return AbilityConfig.COMMON.enablePressureCost.get();
    }
    public static float getBaseCursedEnergyCost() {
        return AbilityConfig.COMMON.baseCursedEnergyCost.get().floatValue();
    }
    public static float getCostPerPressureLevel() {
        return AbilityConfig.COMMON.costPerPressureLevel.get().floatValue();
    }
    public static float getMaxCostMultiplier() {
        return AbilityConfig.COMMON.maxCostMultiplier.get().floatValue();
    }
    /**
     * 根据压制等级计算咒力消耗
     *
     * 公式: baseCost × (1 + pressureLevel × costPerLevel)
     *
     * 默认配置下的示例:
     * - 等级 0: 0.8 × 1.0 = 0.80 (原版消耗)
     * - 等级 1: 0.8 × 1.2 = 0.96
     * - 等级 3: 0.8 × 1.6 = 1.28
     * - 等级 5: 0.8 × 2.0 = 1.60
     * - 等级 7: 0.8 × 2.4 = 1.92
     * - 等级 10: 0.8 × 3.0 = 2.40
     *
     * @param pressureLevel 当前压制等级 (0-10)
     * @return 计算后的咒力消耗
     */
    public static float calculatePressureCost(int pressureLevel) {
        float baseCost = getBaseCursedEnergyCost();

        if (pressureLevel <= 0) {
            return baseCost;
        }

        float costPerLevel = getCostPerPressureLevel();
        float multiplier = 1.0F + (pressureLevel * costPerLevel);

        // 限制最大倍率
        float maxMult = getMaxCostMultiplier();
        multiplier = Math.min(multiplier, maxMult);

        return baseCost * multiplier;
    }

    private PressureConfig() {}
}
