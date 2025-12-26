// 文件路径: src/main/java/com/jujutsuaddon/addon/ability/limitless/Infinity/pressure/damage/PressureDamageConfig.java
package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.damage;

import com.jujutsuaddon.addon.config.AbilityConfig;

/**
 * 压力伤害配置读取
 *
 * 从 AbilityConfig 读取压力伤害相关配置
 */
public final class PressureDamageConfig {

    private PressureDamageConfig() {}

    // ==================== 伤害阈值 ====================

    public static double getMinPressureForDamage() {
        return AbilityConfig.COMMON.minPressureForDamage.get();
    }

    // ==================== 伤害上限 ====================

    public static double getMaxDamagePerHit() {
        return AbilityConfig.COMMON.maxDamagePerHit.get();
    }

    // ==================== 伤害间隔 ====================

    public static int getMinDamageInterval() {
        return AbilityConfig.COMMON.minDamageInterval.get();
    }

    public static int getMaxDamageInterval() {
        return AbilityConfig.COMMON.maxDamageInterval.get();
    }

    public static double getMaxPressureForInterval() {
        return AbilityConfig.COMMON.maxPressureForInterval.get();
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
}
