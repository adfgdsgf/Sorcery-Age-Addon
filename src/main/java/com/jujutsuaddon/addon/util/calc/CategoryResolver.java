package com.jujutsuaddon.addon.util.calc;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.context.TamedCostContext;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Ability.ActivationType;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;  // ★★★ 新增导入 ★★★
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 技能分类解析器
 */
public class CategoryResolver {

    private static Set<String> excludedSkillsCache = null;
    private static Set<String> rctKeywordsCache = null;

    /**
     * 解析技能的分类
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

        // ★★★ 4. 领域展开排除 ★★★
        // 领域展开有自己独特的机制，不应该参与消耗平衡
        if (ability instanceof DomainExpansion) {
            return AbilityCategory.EXCLUDED;
        }

        // ========== 5. 体术检查（通过 isMelee 判断）==========
        try {
            if (ability.isMelee()) {
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

        // ========== 6. 召唤物分类 ==========
        if (ability instanceof Summon<?> summon) {
            return resolveSummonCategory(summon, owner);
        }

        // ========== 7. 攻击增强类 (IAttack) ==========
        if (ability instanceof Ability.IAttack || ability instanceof Ability.ITenShadowsAttack) {
            return AbilityCategory.ATTACK;
        }

        // ========== 8. 引导技能 (同时是 IChanneled 和 IDurationable) ==========
        if (ability instanceof Ability.IChannelened && ability instanceof Ability.IDurationable) {
            return AbilityCategory.CHANNELED;
        }

        // ========== 9. 切换技能 (IToggled) ==========
        if (ability instanceof Ability.IToggled) {
            return AbilityCategory.TOGGLED;
        }

        // ========== 10. 通过 ActivationType 判断 ==========
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

        // ========== 11. 默认归为瞬发 ==========
        return AbilityCategory.INSTANT;
    }

    /**
     * 解析召唤物的具体分类
     */
    private static AbilityCategory resolveSummonCategory(Summon<?> summon, @Nullable LivingEntity owner) {
        Class<?> entityClass = summon.getClazz();

        boolean isTenShadows = TenShadowsSummon.class.isAssignableFrom(entityClass);

        if (isTenShadows) {
            if (TamedCostContext.shouldForceTamed()) {
                return AbilityCategory.SUMMON_TAMED;
            }

            try {
                ActivationType type = summon.getActivationType(owner);
                if (type == ActivationType.TOGGLED) {
                    return AbilityCategory.SUMMON_TAMED;
                } else if (type == ActivationType.INSTANT) {
                    return AbilityCategory.SUMMON_UNTAMED;
                }
            } catch (Exception ignored) {}

            return AbilityCategory.SUMMON_UNTAMED;
        }

        try {
            ActivationType type = summon.getActivationType(owner);
            if (type == ActivationType.TOGGLED) {
                return AbilityCategory.SUMMON_TAMED;
            }
        } catch (Exception ignored) {}

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
            rctKeywordsCache.add("rct");
            rctKeywordsCache.add("heal");
            rctKeywordsCache.add("reverse");
        }
    }

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
