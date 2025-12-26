// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/core/DamageCore.java
package com.jujutsuaddon.addon.damage.core;

import com.jujutsuaddon.addon.config.AddonConfig;

/**
 * 伤害计算核心
 *
 * ★★★ 正确公式 ★★★
 * {[(基础 × 保留) × (面板 × 乘法 × 独立 × 属性 × 攻速) + (加法 × 职业)] × 平衡} × 输出
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
        double balancer = ctx.balancerMultiplier() * ctx.skillConfigMultiplier();

        // 1. 基础部分 = 基础 × 保留
        double basePart = ctx.originalBaseDamage() * ctx.preservation();

        double scaledDamage = basePart;
        double additionPart = 0;

        if (ctx.enablePanelScaling()) {
            // 2. 面板乘区 = 面板 × 乘法 × 独立 × 属性 × 攻速
            double panelMultiplier = ctx.totalPanel()
                    * (1.0 + ctx.externalMultBase())   // 乘法乘区
                    * ctx.externalMultTotal()          // 独立乘区
                    * ctx.independentAttrMult()        // 属性乘区
                    * ctx.speedModifier();             // 攻速

            // ★★★ 核心变更：基础 × 面板乘区（相乘而非相加）★★★
            scaledDamage = basePart * panelMultiplier;

            // 3. 加法部分 = 加法 × 职业（独立加上）
            additionPart = ctx.externalAddition() * ctx.roleMultiplier();
        }

        // 4. 合计 × 平衡
        double beforeOutput = (scaledDamage + additionPart) * balancer;

        // 5. 最后 × 输出
        double rawTotal = beforeOutput * ctx.cursedEnergyOutput();

        // ★ NaN 保护 ★
        if (Double.isNaN(rawTotal) || Double.isInfinite(rawTotal)) {
            rawTotal = ctx.originalBaseDamage();
        }

        // 6. 计算暴击伤害
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
                scaledDamage - basePart + additionPart,  // 面板贡献 = 缩放后 - 基础 + 加法
                ctx.speedModifier(),
                balancer
        );
    }

    // ==================== 天与咒缚分支 ====================

    private static DamageResult calculateHeavenlyRestriction(DamageContext ctx) {
        double balancer = ctx.balancerMultiplier() * ctx.skillConfigMultiplier();

        // ★ 天与咒缚：面板乘区直接作为伤害基础（没有技能基础伤害）★
        double panelMultiplier = ctx.totalPanel()
                * (1.0 + ctx.externalMultBase())
                * ctx.externalMultTotal()
                * ctx.independentAttrMult()
                * ctx.speedModifier();

        // 加法部分
        double additionPart = ctx.externalAddition() * ctx.roleMultiplier();

        // 合计 × 平衡
        double beforeOutput = (panelMultiplier + additionPart) * balancer;

        // × 输出
        double rawDamage = beforeOutput * ctx.cursedEnergyOutput();

        // 保留公式检查（有技能时，保底不低于原始伤害的一定比例）
        if (ctx.ability() != null) {
            double preservationRatio = AddonConfig.COMMON.hrMeleePreservation.get();
            double preservedOriginal = ctx.originalBaseDamage() * preservationRatio;

            if (preservedOriginal > rawDamage) {
                rawDamage = preservedOriginal;
            }
        }

        // ★ NaN 保护 ★
        if (Double.isNaN(rawDamage) || Double.isInfinite(rawDamage)) {
            rawDamage = ctx.originalBaseDamage();
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
                balancer
        );
    }

    // ==================== 便捷方法 ====================

    public static float calculateFinal(DamageContext ctx, boolean useCritExpectation) {
        DamageResult result = calculate(ctx);
        double damage = useCritExpectation ? result.expectedDamage() : result.rawDamage();
        return (float) (damage * ctx.globalMultiplier());
    }
}
