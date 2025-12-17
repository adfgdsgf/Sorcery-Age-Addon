// 文件路径: src/main/java/com/jujutsuaddon/addon/balance/ability/AbilityBalancer.java
package com.jujutsuaddon.addon.balance.ability;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

public class AbilityBalancer {

    // ==================== 公开 API ====================

    public static float getDamageMultiplier(Ability currentAbility, LivingEntity owner) {
        return getDamageMultiplierInternal(currentAbility, owner, false);
    }

    public static float getSummonMultiplier(Ability summonAbility, LivingEntity owner) {
        return getSummonMultiplierInternal(summonAbility, owner, false);
    }

    public static float getDamageMultiplierSilent(Ability currentAbility, LivingEntity owner) {
        return getDamageMultiplierInternal(currentAbility, owner, true);
    }

    public static float getSummonMultiplierSilent(Ability summonAbility, LivingEntity owner) {
        return getSummonMultiplierInternal(summonAbility, owner, true);
    }

    // ==================== 内部实现 ====================

    private static float getDamageMultiplierInternal(Ability currentAbility, LivingEntity owner, boolean silent) {
        if (!AddonConfig.COMMON.enableSkillBalancer.get()) return 1.0f;
        if (currentAbility == null || owner == null) return 1.0f;

        AbilityCategory category = CategoryResolver.resolve(currentAbility, owner);
        if (!category.shouldBalance()) {
            if (!silent) logExcluded(owner, currentAbility, category, "Category excluded");
            return 1.0f;
        }

        if (category.isSummon()) {
            if (!silent) logExcluded(owner, currentAbility, category, "Summon handled separately");
            return 1.0f;
        }

        CursedTechnique technique = null;
        ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap != null) technique = cap.getTechnique();

        float multiplier = CategoryBenchmark.getMultiplier(currentAbility, owner);
        float maxMultiplier = AddonConfig.COMMON.balancerMaxMultiplier.get().floatValue();
        float finalMult = Math.min(multiplier, maxMultiplier);

        if (!silent) logBalancerResult(owner, currentAbility, category, technique, finalMult);

        return finalMult;
    }

    private static float getSummonMultiplierInternal(Ability summonAbility, LivingEntity owner, boolean silent) {
        if (!AddonConfig.COMMON.enableSkillBalancer.get()) return 1.0f;
        if (summonAbility == null) return 1.0f;

        AbilityCategory category = CategoryResolver.resolve(summonAbility, owner);
        if (!category.isSummon()) return 1.0f;

        float multiplier = CategoryBenchmark.getMultiplier(summonAbility, owner);
        float maxMultiplier = AddonConfig.COMMON.balancerMaxMultiplier.get().floatValue();
        return Math.min(multiplier, maxMultiplier);
    }

    // ==================== 调试日志 ====================

    private static void logExcluded(LivingEntity owner, Ability ability, AbilityCategory category, String reason) {
        if (!(owner instanceof Player player)) return;
        if (!DebugManager.isDebugging(player)) return;
        String skillName = ability.getClass().getSimpleName();
        if (DamageDebugUtil.shouldLogBalancerForSkill(player, "excluded_" + skillName)) {
            DamageDebugUtil.logBalancerExcluded(player, skillName, category.name(), reason);
        }
    }

    private static void logBalancerResult(LivingEntity owner, Ability ability, AbilityCategory category,
                                          CursedTechnique technique, float finalMult) {
        if (!(owner instanceof Player player)) return;
        if (!DebugManager.isDebugging(player)) return;
        String skillName = ability.getClass().getSimpleName();

        if (DamageDebugUtil.shouldLogBalancerForSkill(player, skillName)) {
            float currentCost = CategoryBenchmark.calculateCostForCategory(ability, category, owner);
            float benchmarkCost = CategoryBenchmark.getBenchmarkCost(category, technique);
            String benchmarkName = CategoryBenchmark.getBenchmarkName(category, technique);
            float rawRatio = benchmarkCost > 0 ? currentCost / benchmarkCost : 0;
            String techName = technique != null ? technique.name() : "NONE";
            String benchmarkInfo = benchmarkName != null ? benchmarkName + " (auto)" : "(default)";

            DamageDebugUtil.logBalancerDetails(player, skillName, category.name(), techName,
                    category.getCostFormula(), currentCost, benchmarkCost, benchmarkInfo, rawRatio, finalMult);
        }
    }

    // ==================== 缓存管理 ====================

    public static void reload() {
        CategoryResolver.reload();
        CategoryBenchmark.reload();
    }
}
