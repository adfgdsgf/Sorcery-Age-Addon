package com.jujutsuaddon.addon.util.calc;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.context.TamedCostContext;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Ability.ActivationType;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 技能分类解析器
 *
 * 核心职责：判断技能属于哪个 AbilityCategory
 * 不做归一化，只做分类
 */
public class CategoryResolver {

    // 缓存排除列表和RCT关键词，避免每次都读取配置
    private static Set<String> excludedSkillsCache = null;
    private static Set<String> rctKeywordsCache = null;

    /**
     * 解析技能的分类
     *
     * @param ability 技能
     * @param owner 使用者（可为null）
     * @return 技能分类
     */
    public static AbilityCategory resolve(Ability ability, @Nullable LivingEntity owner) {
        if (ability == null) {
            return AbilityCategory.EXCLUDED;
        }

        // ========== 1. 黑名单检查 ==========
        if (isInExcludedList(ability)) {
            return AbilityCategory.EXCLUDED;
        }

        // ========== 2. 非术式排除 ==========
        try {
            if (!ability.isTechnique()) {
                return AbilityCategory.EXCLUDED;
            }
        } catch (Exception e) {
            return AbilityCategory.EXCLUDED;
        }

        // ========== 3. 反转术式排除 ==========
        if (isReverseCursedTechnique(ability)) {
            return AbilityCategory.EXCLUDED;
        }

        // ========== 4. 体术检查（通过 isMelee 判断）==========
        try {
            if (ability.isMelee()) {
                // 体术技能排除（除非在 meleeSkillWhitelist 中）
                String className = ability.getClass().getSimpleName();
                List<? extends String> whitelist = AddonConfig.COMMON.meleeSkillWhitelist.get();
                boolean inWhitelist = false;
                for (String id : whitelist) {
                    if (className.toLowerCase().contains(id.toLowerCase())) {
                        inWhitelist = true;
                        break;
                    }
                }
                if (!inWhitelist) {
                    return AbilityCategory.EXCLUDED;
                }
            }
        } catch (Exception ignored) {}

        // ========== 5. 召唤物分类 ==========
        if (ability instanceof Summon<?> summon) {
            return resolveSummonCategory(summon, owner);
        }

        // ========== 6. 攻击增强类 (IAttack) ==========
        if (ability instanceof Ability.IAttack || ability instanceof Ability.ITenShadowsAttack) {
            return AbilityCategory.ATTACK;
        }

        // ========== 7. 引导技能 (同时是 IChanneled 和 IDurationable) ==========
        if (ability instanceof Ability.IChannelened && ability instanceof Ability.IDurationable) {
            return AbilityCategory.CHANNELED;
        }

        // ========== 8. 切换技能 (IToggled) ==========
        if (ability instanceof Ability.IToggled) {
            return AbilityCategory.TOGGLED;
        }

        // ========== 9. 通过 ActivationType 判断 ==========
        ActivationType type = getActivationType(ability, owner);
        if (type != null) {
            switch (type) {
                case INSTANT:
                    return AbilityCategory.INSTANT;
                case TOGGLED:
                    return AbilityCategory.TOGGLED;
                case CHANNELED:
                    return AbilityCategory.CHANNELED;
            }
        }

        // ========== 10. 默认归为瞬发 ==========
        return AbilityCategory.INSTANT;
    }

    /**
     * 解析召唤物的具体分类
     */
    private static AbilityCategory resolveSummonCategory(Summon<?> summon, @Nullable LivingEntity owner) {
        Class<?> entityClass = summon.getClazz();

        // 检查是否是十影式神
        boolean isTenShadows = TenShadowsSummon.class.isAssignableFrom(entityClass);

        if (isTenShadows) {
            // 判断是否处于调伏状态
            // 方法1：检查 TamedCostContext（如果外部设置了强制调伏状态）
            if (TamedCostContext.shouldForceTamed()) {
                return AbilityCategory.SUMMON_TAMED;
            }

            // 方法2：检查 ActivationType
            try {
                ActivationType type = summon.getActivationType(owner);
                if (type == ActivationType.TOGGLED) {
                    // TOGGLED = 每tick消耗 = 调伏状态
                    return AbilityCategory.SUMMON_TAMED;
                } else if (type == ActivationType.INSTANT) {
                    // INSTANT = 一次性消耗 = 未调伏状态
                    return AbilityCategory.SUMMON_UNTAMED;
                }
            } catch (Exception ignored) {}

            // 无法判断时，默认为未调伏
            return AbilityCategory.SUMMON_UNTAMED;
        }

        // 非十影召唤物（改造人、分身等）
        // 检查是否是每tick消耗类型
        try {
            ActivationType type = summon.getActivationType(owner);
            if (type == ActivationType.TOGGLED) {
                // 类似式神的持续消耗类召唤物
                return AbilityCategory.SUMMON_TAMED;
            }
        } catch (Exception ignored) {}

        // 默认为瞬发召唤物（如改造人）
        return AbilityCategory.SUMMON_INSTANT;
    }

    /**
     * 判断是否在排除列表中
     */
    private static boolean isInExcludedList(Ability ability) {
        if (excludedSkillsCache == null) {
            loadExcludedCache();
        }

        String className = ability.getClass().getSimpleName();
        return excludedSkillsCache.contains(className);
    }

    /**
     * 判断是否是反转术式
     */
    private static boolean isReverseCursedTechnique(Ability ability) {
        if (rctKeywordsCache == null) {
            loadRctCache();
        }

        String className = ability.getClass().getSimpleName().toLowerCase();
        String fullName = ability.getClass().getName().toLowerCase();

        for (String keyword : rctKeywordsCache) {
            if (className.contains(keyword) || fullName.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 安全获取 ActivationType
     */
    @Nullable
    private static ActivationType getActivationType(Ability ability, @Nullable LivingEntity owner) {
        try {
            return ability.getActivationType(owner);
        } catch (Exception e) {
            try {
                return ability.getActivationType(null);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    // =========================================================
    // 缓存管理
    // =========================================================

    private static void loadExcludedCache() {
        excludedSkillsCache = new HashSet<>();
        try {
            List<? extends String> list = AddonConfig.COMMON.balancerExcludedSkills.get();
            excludedSkillsCache.addAll(list);
        } catch (Exception ignored) {}
    }

    private static void loadRctCache() {
        rctKeywordsCache = new HashSet<>();
        try {
            List<? extends String> list = AddonConfig.COMMON.rctKeywords.get();
            for (String keyword : list) {
                rctKeywordsCache.add(keyword.toLowerCase());
            }
        } catch (Exception ignored) {
            // 默认关键词
            rctKeywordsCache.add("rct");
            rctKeywordsCache.add("heal");
            rctKeywordsCache.add("reverse");
        }
    }

    /**
     * 刷新缓存（配置重载时调用）
     */
    public static void reload() {
        excludedSkillsCache = null;
        rctKeywordsCache = null;
    }

    // =========================================================
    // 便捷方法
    // =========================================================

    public static boolean shouldBalance(Ability ability, @Nullable LivingEntity owner) {
        return resolve(ability, owner).shouldBalance();
    }

    public static boolean isSummon(Ability ability, @Nullable LivingEntity owner) {
        return resolve(ability, owner).isSummon();
    }

    public static boolean isTickBased(Ability ability, @Nullable LivingEntity owner) {
        return resolve(ability, owner).isTickBased();
    }

    public static boolean isAttack(Ability ability, @Nullable LivingEntity owner) {
        return resolve(ability, owner) == AbilityCategory.ATTACK;
    }
}
