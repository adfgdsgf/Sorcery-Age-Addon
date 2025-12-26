package com.jujutsuaddon.addon.vow.calculation;

import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.benefit.BenefitCategory;
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 誓约权重和收益计算器
 * Vow Weight and Benefit Calculator
 *
 * 核心公式：
 * - 条件权重 = 条件严苛程度（非线性增长）
 * - 可用收益 = f(总权重) * 誓约类型倍率
 * - 实际收益 ≤ 可用收益（且不超过类别上限）
 */
public class VowCalculator {

    // ==================== 配置常量 ====================

    /** 输出类收益的最大叠加值 (20%) - 配合加法逻辑 */
    public static final float MAX_OUTPUT_BONUS = 0.2f;

    /** 咒力量类收益的最大叠加值 (50%) */
    public static final float MAX_ENERGY_BONUS = 0.5f;

    /** 冷却缩减的最大值 (50%) */
    public static final float MAX_COOLDOWN_REDUCTION = 0.5f;

    /** 永久誓约的收益倍率 */
    public static final float PERMANENT_VOW_MULTIPLIER = 1.3f;

    /** 可解除誓约的收益倍率 */
    public static final float DISSOLVABLE_VOW_MULTIPLIER = 1.0f;

    // ==================== 权重计算 ====================

    /**
     * 计算时间限制的权重（非线性）
     * @param hours 限制的小时数 (1-23)
     * @return 权重值
     */
    public static float calculateTimeWeight(int hours) {
        if (hours <= 0) return 0;
        if (hours >= 24) hours = 23;
        // 公式: f(x) = x * (1 + 0.05 * x)
        return hours * (1 + 0.05f * hours);
    }

    /**
     * 计算血量阈值条件的权重
     * @param threshold 血量百分比阈值 (0.0 - 1.0)
     */
    public static float calculateHealthThresholdWeight(float threshold) {
        // 公式: f(x) = 10 * x^2
        return 10f * threshold * threshold;
    }

    /**
     * 计算禁用技能条件的权重
     */
    public static float calculateAbilityBanWeight(int abilityCount, boolean includesCoreTechnique) {
        float base = abilityCount * 2.0f;
        if (includesCoreTechnique) {
            base *= 1.5f;
        }
        return base;
    }

    /**
     * 计算必须咏唱条件的权重
     */
    public static float calculateChantWeight(int syllableCount) {
        return syllableCount * 1.5f;
    }

    // ==================== 收益计算 ====================

    /**
     * 计算可用收益点数
     */
    public static float calculateAvailableBenefitPoints(float totalWeight, VowType type) {
        float multiplier = type == VowType.PERMANENT
                ? PERMANENT_VOW_MULTIPLIER
                : DISSOLVABLE_VOW_MULTIPLIER;
        return (float) Math.pow(totalWeight, 1.15) * multiplier;
    }

    /**
     * 计算输出提升百分比
     * 使用幂函数以获得更平滑的增长曲线
     * 0.5点 -> ~1.3%
     * 50点  -> ~20.9% (截断为20%)
     */
    public static float calculateOutputBonus(float investedPoints) {
        float bonus = 0.02f * (float) Math.pow(investedPoints, 0.6);
        return Math.min(bonus, MAX_OUTPUT_BONUS);
    }

    /**
     * 计算获得指定输出加成需要多少点数
     */
    public static float pointsRequiredForOutputBonus(float desiredBonus) {
        desiredBonus = Math.min(desiredBonus, MAX_OUTPUT_BONUS);
        // 反向公式: (y / 0.02)^(1/0.6)
        return (float) Math.pow(desiredBonus / 0.02f, 1.0 / 0.6);
    }

    /**
     * 计算咒力量提升百分比
     */
    public static float calculateEnergyBonus(float investedPoints) {
        float bonus = 0.06f * (float) Math.pow(investedPoints, 0.6);
        return Math.min(bonus, MAX_ENERGY_BONUS);
    }

    /**
     * 计算冷却缩减百分比
     */
    public static float calculateCooldownReduction(float investedPoints) {
        float reduction = 0.05f * (float) Math.pow(investedPoints, 0.6);
        return Math.min(reduction, MAX_COOLDOWN_REDUCTION);
    }

    // ==================== 叠加计算 ====================

    /**
     * 计算多个誓约叠加后的总收益
     */
    public static Map<BenefitCategory, Float> calculateStackedBenefits(List<CustomBindingVow> activeVows) {
        Map<BenefitCategory, Float> totals = new EnumMap<>(BenefitCategory.class);

        for (BenefitCategory category : BenefitCategory.values()) {
            float total = 0f;

            for (CustomBindingVow vow : activeVows) {
                for (BenefitEntry entry : vow.getBenefits()) {
                    if (entry.getBenefit().getCategory() == category) {
                        total += entry.getBenefit().getCurrentBonus(null, entry.getParams());
                    }
                }
            }

            total = Math.min(total, getCategoryMax(category));
            totals.put(category, total);
        }

        return totals;
    }

    /**
     * 获取类别的叠加上限
     */
    public static float getCategoryMax(BenefitCategory category) {
        return switch (category) {
            case OUTPUT -> MAX_OUTPUT_BONUS;
            case ENERGY -> MAX_ENERGY_BONUS;
            case COOLDOWN -> MAX_COOLDOWN_REDUCTION;
            default -> Float.MAX_VALUE;
        };
    }

    // ==================== 验证 ====================

    /**
     * 验证誓约配置是否平衡
     */
    public static ValidationResult validateVowBalance(CustomBindingVow vow) {
        // ★ 新增逻辑：永久誓约不需要计算权重平衡
        if (vow.getType() == VowType.PERMANENT) {
            // 只要有一条条件和一条收益，就视为合法
            if (vow.getConditions().isEmpty()) {
                return ValidationResult.error("vow.error.no_condition");
            }
            if (vow.getBenefits().isEmpty()) {
                return ValidationResult.error("vow.error.no_benefit");
            }
            // 永久誓约是 1换1，直接通过
            return ValidationResult.success();
        }
        // --- 以下是原有的普通誓约逻辑 ---
        float totalWeight = vow.calculateTotalWeight();
        float totalCost = vow.calculateTotalCost();
        float available = calculateAvailableBenefitPoints(totalWeight, vow.getType());
        if (totalCost > available) {
            return ValidationResult.error("vow.error.insufficient_weight");
        }
        // 检查单类别是否超过上限
        Map<BenefitCategory, Float> categoryTotals = new EnumMap<>(BenefitCategory.class);
        for (BenefitEntry entry : vow.getBenefits()) {
            BenefitCategory cat = entry.getBenefit().getCategory();
            float current = categoryTotals.getOrDefault(cat, 0f);
            float bonus = entry.getBenefit().getCurrentBonus(null, entry.getParams());
            categoryTotals.put(cat, current + bonus);
        }
        for (Map.Entry<BenefitCategory, Float> entry : categoryTotals.entrySet()) {
            float max = getCategoryMax(entry.getKey());
            if (entry.getValue() > max) {
                return ValidationResult.error("vow.error.category_exceeded");
            }
        }
        return ValidationResult.success();
    }

    // ==================== 惩罚计算 (新增) ====================

    /**
     * 计算违约/撤销后的全局冷却惩罚时长（毫秒）
     * 规则：
     * 1. 基础惩罚时间 5分钟
     * 2. 权重越高，惩罚越长 (每1点权重 +30秒)
     * 3. 永久誓约违约，惩罚翻倍
     */
    public static long calculatePenaltyDuration(CustomBindingVow vow) {
        long baseTime = 5 * 60 * 1000; // 5分钟基础
        float totalWeight = vow.calculateTotalWeight();

        // 每1点权重增加 30秒 惩罚
        long weightPenalty = (long) (totalWeight * 30 * 1000);

        long total = baseTime + weightPenalty;

        // 如果是永久誓约（通常不可撤销，但如果通过特殊手段违约），惩罚更重
        if (vow.isPermanent()) {
            total *= 2;
        }

        return total;
    }
}
