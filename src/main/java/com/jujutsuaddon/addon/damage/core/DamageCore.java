// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/core/DamageCore.java
package com.jujutsuaddon.addon.damage.core;

import com.jujutsuaddon.addon.AddonConfig;

/**
 * 伤害计算核心
 *
 * ★★★ 唯一的伤害公式实现 ★★★
 *
 * 核心公式（方案B）：
 * - basePart = originalDamage × preservation（基础只吃保留）
 * - panelPart = totalPanel × roleMultiplier × speedModifier × panelMultiplier
 * - rawDamage = (basePart + panelPart) × skillMult
 */
public final class DamageCore {

    private DamageCore() {}

    // ==================== 主入口 ====================

    public static DamageResult calculate(DamageContext ctx) {
        if (ctx.isHeavenlyRestriction()) {
            return calculateHeavenlyRestriction(ctx);
        }
        return calculateSorcerer(ctx);
    }

    // ==================== 咒术师分支 ====================

    private static DamageResult calculateSorcerer(DamageContext ctx) {

        // 技能总倍率（平衡器 × 配置）
        double totalSkillMult = ctx.balancerMultiplier() * ctx.skillConfigMultiplier();

        // 1. 基础部分：只吃保留率
        //    ★★★ 方案B：不吃职业、不吃外部 ★★★
        double basePart = ctx.originalBaseDamage()
                * ctx.preservation();

        // 2. 面板部分：吃职业、攻速、外部
        double panelPart = 0;
        if (ctx.enablePanelScaling()) {
            panelPart = ctx.totalPanel()
                    * ctx.roleMultiplier()      // 职业
                    * ctx.speedModifier()       // 攻速
                    * ctx.panelMultiplier();    // 外部
        }

        // 3. 合计后统一乘平衡器
        double rawTotal = (basePart + panelPart) * totalSkillMult;

        // 4. 计算暴击伤害
        double critDamage = rawTotal * ctx.critDamage();
        double expectedDamage = rawTotal;

        if (ctx.critChance() > 0 && ctx.critDamage() > 1.0) {
            expectedDamage = rawTotal * (1.0 + ctx.critChance() * (ctx.critDamage() - 1.0));
        }

        return new DamageResult(
                rawTotal,
                expectedDamage,
                critDamage,
                basePart,
                panelPart,
                ctx.speedModifier(),
                totalSkillMult
        );
    }

    // ==================== 天与咒缚分支 ====================

    private static DamageResult calculateHeavenlyRestriction(DamageContext ctx) {

        double classMultiplier = ctx.roleMultiplier();

        double rawDamage = ctx.totalPanel()
                * classMultiplier
                * ctx.speedModifier()
                * ctx.panelMultiplier();

        // 保留公式检查（方案B：保留部分不吃外部）
        if (ctx.ability() != null) {
            double preservationRatio = AddonConfig.COMMON.hrMeleePreservation.get();
            double preservedOriginal = ctx.originalBaseDamage() * preservationRatio;
            // ★★★ 删除了 baseMultiplier ★★★

            if (preservedOriginal > rawDamage) {
                rawDamage = preservedOriginal;
            }
        }

        double critDamage = rawDamage * ctx.critDamage();
        double expectedDamage = rawDamage;

        if (ctx.critChance() > 0 && ctx.critDamage() > 1.0) {
            expectedDamage = rawDamage * (1.0 + ctx.critChance() * (ctx.critDamage() - 1.0));
        }

        return new DamageResult(
                rawDamage,
                expectedDamage,
                critDamage,
                rawDamage,
                0,
                ctx.speedModifier(),
                1.0
        );
    }

    // ==================== 便捷方法 ====================

    public static float calculateFinal(DamageContext ctx, boolean useCritExpectation) {
        DamageResult result = calculate(ctx);
        double damage = useCritExpectation ? result.expectedDamage() : result.rawDamage();
        return (float) (damage * ctx.globalMultiplier());
    }
}
