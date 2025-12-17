// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/core/DamageResult.java
package com.jujutsuaddon.addon.damage.core;

/**
 * 伤害计算结果
 *
 * ★ 注意：rawDamage 不含全局倍率 ★
 */
public record DamageResult(
        double rawDamage,       // 原始伤害（不含全局倍率、不含暴击）
        double expectedDamage,  // 期望伤害（含暴击期望，不含全局倍率）
        double critDamage,      // 暴击伤害（不含全局倍率）
        double basePart,        // 基础部分
        double panelPart,       // 面板部分
        double speedModifier,   // 攻速修正
        double skillMultiplier  // 技能倍率（用于调试）
) {

    public float toFloat() {
        return (float) rawDamage;
    }

    public float expectedToFloat() {
        return (float) expectedDamage;
    }

    public float critToFloat() {
        return (float) critDamage;
    }

    /**
     * 应用全局倍率
     */
    public double withGlobalMultiplier(double globalMult) {
        return rawDamage * globalMult;
    }

    public double expectedWithGlobal(double globalMult) {
        return expectedDamage * globalMult;
    }

    public double critWithGlobal(double globalMult) {
        return critDamage * globalMult;
    }

    public static DamageResult zero() {
        return new DamageResult(0, 0, 0, 0, 0, 1.0, 1.0);
    }

    public static DamageResult passthrough(float originalDamage) {
        return new DamageResult(originalDamage, originalDamage, originalDamage,
                originalDamage, 0, 1.0, 1.0);
    }
}
