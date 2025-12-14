package com.jujutsuaddon.addon.util.calc;

import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

import javax.annotation.Nullable;

/**
 * 技能消耗统一分析工具 - 门面类（精简版）
 */
public class AbilityCostHelper {

    // =========================================================
    // 分类判断
    // =========================================================

    public static AbilityCategory getCategory(Ability ability, @Nullable LivingEntity owner) {
        return CategoryResolver.resolve(ability, owner);
    }

    public static boolean isTickBasedCost(Ability ability, @Nullable LivingEntity owner) {
        return CategoryResolver.isTickBased(ability, owner);
    }

    public static boolean isSummonAbility(Ability ability) {
        return CategoryResolver.resolve(ability, null).isSummon();
    }

    // =========================================================
    // 消耗计算
    // =========================================================

    public static float getRawCost(Ability ability, @Nullable LivingEntity owner) {
        return CostCalculator.getRawCost(ability, owner);
    }

    public static float getCooldownTicks(Ability ability, @Nullable LivingEntity owner) {
        return CostCalculator.getCooldownTicks(ability, owner);
    }

    public static float getCooldownSeconds(Ability ability, @Nullable LivingEntity owner) {
        return CostCalculator.getCooldownSeconds(ability, owner);
    }

    /**
     * 获取技能在其分类下的标准化消耗
     */
    public static float getCategoryCost(Ability ability, @Nullable LivingEntity owner) {
        AbilityCategory category = CategoryResolver.resolve(ability, owner);
        return CategoryBenchmark.calculateCostForCategory(ability, category, owner);
    }

    // =========================================================
    // 基准与倍率
    // =========================================================

    public static float getBenchmarkCost(AbilityCategory category, @Nullable CursedTechnique technique) {
        return CategoryBenchmark.getBenchmarkCost(category, technique);
    }

    public static float getMultiplier(Ability ability, @Nullable LivingEntity owner) {
        return CategoryBenchmark.getMultiplier(ability, owner);
    }

    // =========================================================
    // 缓存管理
    // =========================================================

    public static void reload() {
        CategoryResolver.reload();
        CategoryBenchmark.reload();
    }

    public static boolean isCacheInitialized() {
        return CategoryBenchmark.isInitialized();
    }

    // =========================================================
    // 调试
    // =========================================================

    public static String getDebugInfo(Ability ability, @Nullable LivingEntity owner) {
        return CategoryBenchmark.getDebugInfo(ability, owner);
    }
}
