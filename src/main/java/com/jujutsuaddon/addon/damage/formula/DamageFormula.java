package com.jujutsuaddon.addon.damage.formula;

import com.jujutsuaddon.addon.damage.data.DamageContext;
import com.jujutsuaddon.addon.damage.result.DamagePrediction;

/**
 * Layer 2: 伤害计算公式
 *
 * ★★★ 核心原则 ★★★
 * 1. 纯函数 - 相同输入永远产生相同输出
 * 2. 无状态 - 不依赖任何外部状态
 * 3. 无副作用 - 不修改任何外部数据
 * 4. 唯一实现 - 服务端和客户端共用这一份代码
 */
public final class DamageFormula {

    private DamageFormula() {} // 工具类，禁止实例化

    // ==================== 主入口 ====================

    /**
     * 计算伤害预测
     *
     * @param ctx 包含所有计算所需数据的上下文
     * @return 预测结果
     */
    public static DamagePrediction calculate(DamageContext ctx) {
        return switch (ctx.abilityType()) {
            case DIRECT_DAMAGE, POWER_BASED -> calculateStandard(ctx);
            case DOMAIN -> calculateDomain(ctx);
            case SUMMON -> DamagePrediction.summon(); // 召唤物需要特殊处理
            case UTILITY -> DamagePrediction.utility();
        };
    }

    // ==================== 标准伤害计算 ====================

    /**
     * 标准伤害计算（直接伤害和 power-based）
     */
    private static DamagePrediction calculateStandard(DamageContext ctx) {
        // ★★★ 修复：分开计算 ★★★
        // vanillaDamage = 原版伤害（用于颜色比较）
        float vanillaDamage = ctx.getVanillaDamage();

        // addonDamage 的基础 = baseDamage * multiplier（不含 power）
        float skillBase = ctx.getEffectiveBaseDamage();

        // power 在 applyAddonFormula 里作为倍率使用
        float addonDamage = applyAddonFormula(ctx, skillBase, false);
        float critDamage = applyAddonFormula(ctx, skillBase, true);
        boolean triggeredByMelee = ctx.isMelee() || ctx.isAttackAbility();
        return new DamagePrediction(
                ctx.abilityType(),
                vanillaDamage,
                addonDamage,
                critDamage,
                triggeredByMelee
        );
    }

    /**
     * 领域伤害计算
     */
    private static DamagePrediction calculateDomain(DamageContext ctx) {
        // ★★★ 领域本身不造成伤害，返回 utility ★★★
        return DamagePrediction.utility();
    }

    // ==================== ★★★ 核心公式 ★★★ ====================

    /**
     * Addon 伤害公式
     *
     * 公式：
     * finalDamage = (basePart + panelPart) × balancer × skillConfig × global × [crit]
     *
     * 其中：
     * - basePart = skillBaseDamage × preservation
     * - panelPart = attackDamage × roleMultiplier × speedModifier × externalMult (如果启用)
     */
    private static float applyAddonFormula(DamageContext ctx, float skillBase, boolean applyCrit) {
        boolean useTechnique = ctx.useTechniqueScaling();
        double roleMultiplier = useTechnique ? ctx.techniqueMultiplier() : ctx.meleeMultiplier();
        double preservation = useTechnique ? ctx.techniquePreservation() : ctx.meleePreservation();
        double speedModifier = 1.0 + (Math.max(4.0, ctx.attackSpeed()) - 4.0) * ctx.attackSpeedScaling();
        // ★★★ 修复：skillBase 已经是 baseDamage * multiplier，这里乘 power ★★★
        double basePart = skillBase * ctx.power() * preservation;
        double panelPart = 0;
        if (ctx.enableAttackDamageScaling()) {
            panelPart = ctx.attackDamage() * roleMultiplier * speedModifier * ctx.externalMultiplier();
        } else {
            basePart *= roleMultiplier;
        }
        double rawTotal = basePart + panelPart;
        float balancer = useTechnique ? ctx.balancerMultiplier() : 1.0f;
        double finalDamage = rawTotal * balancer * ctx.skillConfigMultiplier() * ctx.globalMultiplier();
        if (applyCrit) {
            finalDamage *= ctx.critMultiplier();
        }
        return (float) finalDamage;
    }
}
