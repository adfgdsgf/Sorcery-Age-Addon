package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsMode;
import radon.jujutsu_kaisen.entity.JJKEntities;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 十影术式辅助类
 * 提供忽略模式限制的技能获取方法
 */
public class TenShadowsHelper {

    // 需要 ABILITY 模式的技能
    private static final Set<Ability> ABILITY_MODE_SKILLS = new HashSet<>();

    static {
        ABILITY_MODE_SKILLS.add(JJKAbilities.NUE_LIGHTNING.get());
        ABILITY_MODE_SKILLS.add(JJKAbilities.PIERCING_WATER.get());
        ABILITY_MODE_SKILLS.add(JJKAbilities.GREAT_SERPENT_GRAB.get());
    }

    /**
     * 检查功能是否启用
     */
    public static boolean isEnabled() {
        return AddonConfig.COMMON.enableTenShadowsModeBypass.get();
    }

    /**
     * 检查是否允许召唤和技能同时使用
     */
    public static boolean allowSimultaneous() {
        return isEnabled() && AddonConfig.COMMON.allowSimultaneousSummonAndAbility.get();
    }

    /**
     * 检查玩家是否拥有十影术式（原生或复制）
     */
    public static boolean hasTenShadows(LivingEntity owner) {
        if (!owner.getCapability(SorcererDataHandler.INSTANCE).isPresent()) return false;
        ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return false;

        if (cap.getTechnique() == CursedTechnique.TEN_SHADOWS) return true;
        if (cap.getCurrentCopied() == CursedTechnique.TEN_SHADOWS) return true;

        Set<CursedTechnique> copied = cap.getCopied();
        return copied != null && copied.contains(CursedTechnique.TEN_SHADOWS);
    }

    /**
     * 获取所有可用的十影技能（忽略模式限制）
     * 只有在配置启用时才使用此方法
     */
    public static List<Ability> getAllAvailableTenShadowsAbilities(LivingEntity owner) {
        List<Ability> result = new ArrayList<>();

        if (!isEnabled()) return result; // 配置未启用，返回空
        if (!hasTenShadows(owner)) return result;
        if (!owner.getCapability(TenShadowsDataHandler.INSTANCE).isPresent()) return result;

        ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
        ISorcererData sorcererData = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);

        if (tenData == null || sorcererData == null) return result;

        TenShadowsMode originalMode = tenData.getMode();
        var registry = owner.level().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);

        for (Ability ability : CursedTechnique.TEN_SHADOWS.getAbilities()) {
            if (ability == null) continue;

            // 检查解锁状态
            if (ability.isUnlockable() && !ability.isUnlocked(owner)) continue;

            // 特殊处理技能模式技能
            if (ABILITY_MODE_SKILLS.contains(ability)) {
                if (isAbilityModeSkillAvailable(owner, ability, tenData, sorcererData, registry)) {
                    result.add(ability);
                }
                continue;
            }

            // 特殊处理召唤类技能
            if (ability instanceof Summon<?> summon && summon.isTenShadows()) {
                if (isSummonAvailable(owner, summon, tenData, registry)) {
                    result.add(ability);
                }
                continue;
            }

            // 通用技能（SwitchMode, ShadowStorage, ShadowTravel等）
            result.add(ability);
        }

        // 添加领域
        Ability domain = CursedTechnique.TEN_SHADOWS.getDomain();
        if (domain != null) {
            if (!domain.isUnlockable() || domain.isUnlocked(owner)) {
                result.add(domain);
            }
        }

        tenData.setMode(originalMode);
        return result;
    }

    /**
     * 检查技能模式技能是否可用
     */
    private static boolean isAbilityModeSkillAvailable(LivingEntity owner, Ability ability,
                                                       ITenShadowsData tenData, ISorcererData sorcererData,
                                                       net.minecraft.core.Registry<EntityType<?>> registry) {
        // 检查驯服状态
        if (ability == JJKAbilities.NUE_LIGHTNING.get()) {
            if (!tenData.hasTamed(registry, JJKEntities.NUE.get())) return false;
            // 如果不允许同时使用，检查是否已召唤
            if (!allowSimultaneous() && sorcererData.hasToggled(JJKAbilities.NUE.get())) return false;
            return true;
        }
        if (ability == JJKAbilities.PIERCING_WATER.get()) {
            if (!tenData.hasTamed(registry, JJKEntities.MAX_ELEPHANT.get())) return false;
            if (!allowSimultaneous() && sorcererData.hasToggled(JJKAbilities.MAX_ELEPHANT.get())) return false;
            return true;
        }
        if (ability == JJKAbilities.GREAT_SERPENT_GRAB.get()) {
            if (!tenData.hasTamed(registry, JJKEntities.GREAT_SERPENT.get())) return false;
            if (tenData.isDead(registry, JJKEntities.GREAT_SERPENT.get())) return false;
            if (!allowSimultaneous() && sorcererData.hasToggled(JJKAbilities.GREAT_SERPENT.get())) return false;
            return true;
        }
        return true;
    }

    /**
     * 检查召唤类技能是否可用
     */
    private static boolean isSummonAvailable(LivingEntity owner, Summon<?> summon,
                                             ITenShadowsData tenData,
                                             net.minecraft.core.Registry<EntityType<?>> registry) {
        // 检查死亡状态
        if (summon.canDie()) {
            if (summon == JJKAbilities.DIVINE_DOGS.get()) {
                boolean whiteDead = tenData.isDead(registry, JJKEntities.DIVINE_DOG_WHITE.get());
                boolean blackDead = tenData.isDead(registry, JJKEntities.DIVINE_DOG_BLACK.get());
                if (whiteDead && blackDead) return false;
            } else {
                for (EntityType<?> type : summon.getTypes()) {
                    if (tenData.isDead(registry, type)) return false;
                }
            }
        }

        // 检查融合所需的式神
        List<EntityType<?>> fusions = summon.getFusions();
        if (!fusions.isEmpty()) {
            for (EntityType<?> fusion : fusions) {
                if (!tenData.hasTamed(registry, fusion)) return false;
            }
        }

        return true;
    }

    /**
     * 检查技能是否因召唤冲突而不可用
     * （式神已被召唤，对应的技能模式能力不可用）
     */
    public static boolean hasSummonConflict(LivingEntity owner, Ability ability) {
        if (!ABILITY_MODE_SKILLS.contains(ability)) return false;
        if (allowSimultaneous()) return false; // 允许同时使用则没有冲突

        ISorcererData sorcererData = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (sorcererData == null) return false;

        // ★★★ 修复：通过对应的 Summon ability 获取实体类 ★★★
        if (ability == JJKAbilities.NUE_LIGHTNING.get()) {
            Summon<?> nue = JJKAbilities.NUE.get();
            return sorcererData.hasToggled(nue) || sorcererData.hasSummonOfClass(nue.getClazz());
        }
        if (ability == JJKAbilities.PIERCING_WATER.get()) {
            Summon<?> maxElephant = JJKAbilities.MAX_ELEPHANT.get();
            return sorcererData.hasToggled(maxElephant) || sorcererData.hasSummonOfClass(maxElephant.getClazz());
        }
        if (ability == JJKAbilities.GREAT_SERPENT_GRAB.get()) {
            Summon<?> greatSerpent = JJKAbilities.GREAT_SERPENT.get();
            return sorcererData.hasToggled(greatSerpent) || sorcererData.hasSummonOfClass(greatSerpent.getClazz());
        }
        return false;
    }
    /**
     * 检查技能是否是 ABILITY 模式技能
     */
    public static boolean isAbilityModeSkill(Ability ability) {
        return ABILITY_MODE_SKILLS.contains(ability);
    }

    /**
     * 获取技能所需的模式
     */
    @Nullable
    public static TenShadowsMode getRequiredMode(Ability ability) {
        if (ABILITY_MODE_SKILLS.contains(ability)) {
            return TenShadowsMode.ABILITY;
        }
        if (ability instanceof Summon<?> summon && summon.isTenShadows()) {
            return TenShadowsMode.SUMMON;
        }
        return null;
    }

    /**
     * 自动切换到技能所需的模式
     * @return 是否切换了模式
     */
    public static boolean autoSwitchMode(LivingEntity owner, Ability ability) {
        if (!isEnabled()) return false; // 配置未启用

        TenShadowsMode required = getRequiredMode(ability);
        if (required == null) return false;

        if (!owner.getCapability(TenShadowsDataHandler.INSTANCE).isPresent()) return false;
        ITenShadowsData tenData = owner.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
        if (tenData == null) return false;

        if (tenData.getMode() != required) {
            tenData.setMode(required);
            return true;
        }
        return false;
    }

    /**
     * 判断技能是否是十影术式的技能
     */
    public static boolean isTenShadowsAbility(Ability ability) {
        if (ability == null) return false;

        for (Ability a : CursedTechnique.TEN_SHADOWS.getAbilities()) {
            if (a == ability) return true;
        }
        return ability == CursedTechnique.TEN_SHADOWS.getDomain();
    }
}
