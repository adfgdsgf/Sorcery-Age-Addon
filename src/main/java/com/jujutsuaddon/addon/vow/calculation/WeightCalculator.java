package com.jujutsuaddon.addon.vow.calculation;

import com.jujutsuaddon.addon.config.VowConfig;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;
import com.jujutsuaddon.addon.vow.condition.ConditionEntry;
import java.util.List;

/**
 * 誓约权重计算器
 * Vow Weight Calculator
 *
 * 核心公式：
 * 1. 条件总权重 = Σ(各条件权重) * 基础缩放 * 誓约类型倍率
 * 2. 收益总消耗 = Σ(各收益消耗)
 * 3. 剩余权重 = 条件总权重 - 收益总消耗
 * 4. 实际收益值 = 基础收益 * (投入权重 ^ 收益缩放指数)
 */
public class WeightCalculator {

    /**
     * 计算条件提供的总权重
     */
    public static float calculateTotalConditionWeight(CustomBindingVow vow) {
        float baseWeight = 0f;

        for (ConditionEntry entry : vow.getConditions()) {
            float conditionWeight = entry.getCondition().calculateWeight(entry.getParams());
            // 应用权重缩放指数
            conditionWeight = (float) Math.pow(conditionWeight, VowConfig.getWeightScalingExponent());
            baseWeight += conditionWeight;
        }

        // 应用基础缩放
        baseWeight *= VowConfig.getBaseWeightScale();

        // 应用誓约类型倍率
        float typeMultiplier = vow.getType() == VowType.PERMANENT
                ? VowConfig.getPermanentVowMultiplier()
                : VowConfig.getDissolvableVowMultiplier();

        return baseWeight * typeMultiplier;
    }

    /**
     * 计算收益需要的总权重消耗
     */
    public static float calculateTotalBenefitCost(CustomBindingVow vow) {
        float totalCost = 0f;

        for (BenefitEntry entry : vow.getBenefits()) {
            totalCost += entry.getBenefit().getRequiredWeight(entry.getParams());
        }

        return totalCost;
    }

    /**
     * 计算剩余可用权重
     */
    public static float calculateRemainingWeight(CustomBindingVow vow) {
        return calculateTotalConditionWeight(vow) - calculateTotalBenefitCost(vow);
    }

    /**
     * 验证誓约权重是否平衡
     */
    public static ValidationResult validateWeightBalance(CustomBindingVow vow) {
        float totalWeight = calculateTotalConditionWeight(vow);
        float totalCost = calculateTotalBenefitCost(vow);

        if (totalWeight <= 0) {
            return ValidationResult.error("vow.error.no_conditions");
        }

        if (totalCost <= 0) {
            return ValidationResult.error("vow.error.no_benefits");
        }

        if (totalCost > totalWeight) {
            return ValidationResult.error("vow.error.insufficient_weight");
        }

        return ValidationResult.success();
    }

    /**
     * 将权重转换为实际收益百分比
     * 应用边际递减
     *
     * @param investedWeight 投入的权重
     * @param costPerPercent 每1%需要的权重（从配置读取）
     * @return 实际百分比值（0.0-1.0+）
     */
    public static float weightToPercentage(float investedWeight, float costPerPercent) {
        if (investedWeight <= 0 || costPerPercent <= 0) return 0f;

        // 先计算理论百分比
        float theoreticalPercent = investedWeight / costPerPercent / 100f;

        // 应用边际递减
        float exponent = VowConfig.getBenefitScalingExponent();
        return (float) Math.pow(theoreticalPercent, exponent);
    }

    /**
     * 计算誓约的总收益价值（用于惩罚计算）
     */
    public static float calculateTotalBenefitValue(CustomBindingVow vow) {
        // 简化实现：直接使用消耗的权重作为价值
        return calculateTotalBenefitCost(vow);
    }
    // ==================== 列表计算辅助方法 ====================
    /**
     * 计算条件列表的总权重（用于GUI预览）
     */
    public static float calculateConditionWeight(List<ConditionEntry> conditions) {
        float total = 0f;
        for (ConditionEntry entry : conditions) {
            total += entry.getCondition().calculateWeight(entry.getParams());
        }
        return total;
    }
    /**
     * 计算收益列表的总消耗（用于GUI预览）
     */
    public static float calculateBenefitCost(List<BenefitEntry> benefits) {
        float total = 0f;
        for (BenefitEntry entry : benefits) {
            total += entry.getBenefit().getRequiredWeight(entry.getParams());
        }
        return total;
    }
}
