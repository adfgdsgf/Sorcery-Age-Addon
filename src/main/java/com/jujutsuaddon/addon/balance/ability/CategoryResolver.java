// 文件路径: src/main/java/com/jujutsuaddon/addon/balance/ability/CategoryResolver.java
package com.jujutsuaddon.addon.balance.ability;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.context.TamedCostContext;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Ability.ActivationType;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
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

    public static AbilityCategory resolve(Ability ability, @Nullable LivingEntity owner) {
        if (ability == null) {
            return AbilityCategory.EXCLUDED;
        }

        if (isInExcludedList(ability)) {
            return AbilityCategory.EXCLUDED;
        }

        try {
            if (!ability.isTechnique()) {
                return AbilityCategory.EXCLUDED;
            }
        } catch (Exception e) {
            return AbilityCategory.EXCLUDED;
        }

        if (isReverseCursedTechnique(ability)) {
            return AbilityCategory.EXCLUDED;
        }

        if (ability instanceof DomainExpansion) {
            return AbilityCategory.EXCLUDED;
        }

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

        if (ability instanceof Summon<?> summon) {
            return resolveSummonCategory(summon, owner);
        }

        if (ability instanceof Ability.IAttack || ability instanceof Ability.ITenShadowsAttack) {
            return AbilityCategory.ATTACK;
        }

        if (ability instanceof Ability.IChannelened && ability instanceof Ability.IDurationable) {
            return AbilityCategory.CHANNELED;
        }

        if (ability instanceof Ability.IToggled) {
            return AbilityCategory.TOGGLED;
        }

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

        return AbilityCategory.INSTANT;
    }

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

    private static boolean isInExcludedList(Ability ability) {
        if (excludedSkillsCache == null) {
            loadExcludedCache();
        }
        String className = ability.getClass().getSimpleName();
        return excludedSkillsCache.contains(className);
    }

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
