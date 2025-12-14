package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.mixin.access.SorcererDataAccess;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 术式辅助工具类
 * 统一处理复制术式(Copied)和偷取术式(Stolen)
 */
public class TechniqueHelper {

    /**
     * 获取所有额外术式（复制 + 偷取）
     */
    public static Set<CursedTechnique> getAllExtraTechniques(LivingEntity owner) {
        Set<CursedTechnique> result = new LinkedHashSet<>();
        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return result;

        // 添加复制的术式（使用 getRealCopied 绕过限制）
        result.addAll(getRealCopied(owner));

        // 添加当前激活的复制术式（确保不遗漏）
        CursedTechnique currentCopied = data.getCurrentCopied();
        if (currentCopied != null) {
            result.add(currentCopied);
        }

        // 添加偷取的术式（使用 getRealStolen 绕过限制）
        result.addAll(getRealStolen(owner));

        // 添加当前激活的偷取术式（确保不遗漏）
        CursedTechnique currentStolen = data.getCurrentStolen();
        if (currentStolen != null) {
            result.add(currentStolen);
        }

        return result;
    }

    /**
     * ★★★ 获取真实的复制术式集合（通过 Mixin Accessor 绕过条件限制）★★★
     */
    public static Set<CursedTechnique> getRealCopied(LivingEntity owner) {
        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return Set.of();

        // 1. 优先使用 Mixin Accessor 直接访问私有字段
        if (data instanceof SorcererDataAccess accessor) {
            Set<CursedTechnique> realCopied = accessor.getCopiedSet();
            if (realCopied != null && !realCopied.isEmpty()) {
                return new LinkedHashSet<>(realCopied);  // 返回副本防止外部修改
            }
        }

        // 2. 回退：尝试正常 API
        Set<CursedTechnique> copied = data.getCopied();
        if (copied != null && !copied.isEmpty()) {
            return new LinkedHashSet<>(copied);
        }

        // 3. 最后回退：至少返回 currentCopied
        CursedTechnique current = data.getCurrentCopied();
        if (current != null) {
            return new LinkedHashSet<>(Set.of(current));
        }

        return Set.of();
    }

    /**
     * ★★★ 获取真实的偷取术式集合（通过 Mixin Accessor 绕过条件限制）★★★
     */
    public static Set<CursedTechnique> getRealStolen(LivingEntity owner) {
        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return Set.of();

        // 1. 优先使用 Mixin Accessor 直接访问私有字段
        if (data instanceof SorcererDataAccess accessor) {
            Set<CursedTechnique> realStolen = accessor.getStolenSet();
            if (realStolen != null && !realStolen.isEmpty()) {
                return new LinkedHashSet<>(realStolen);
            }
        }

        // 2. 回退：尝试正常 API
        Set<CursedTechnique> stolen = data.getStolen();
        if (stolen != null && !stolen.isEmpty()) {
            return new LinkedHashSet<>(stolen);
        }

        // 3. 最后回退：至少返回 currentStolen
        CursedTechnique current = data.getCurrentStolen();
        if (current != null) {
            return new LinkedHashSet<>(Set.of(current));
        }

        return Set.of();
    }

    /**
     * 获取所有额外术式的技能
     */
    public static Set<Ability> getAllExtraAbilities(LivingEntity owner) {
        Set<Ability> result = new HashSet<>();

        for (CursedTechnique technique : getAllExtraTechniques(owner)) {
            if (technique != null) {
                result.addAll(Arrays.asList(technique.getAbilities()));
            }
        }

        return result;
    }

    /**
     * 检查玩家是否拥有某个额外术式（复制或偷取）
     */
    public static boolean hasExtraTechnique(LivingEntity owner, CursedTechnique technique) {
        if (technique == null) return false;
        return getAllExtraTechniques(owner).contains(technique);
    }

    /**
     * 获取术式的来源类型
     */
    public static TechniqueSource getTechniqueSource(LivingEntity owner, CursedTechnique technique) {
        if (technique == null) return TechniqueSource.NONE;

        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return TechniqueSource.NONE;

        // 原生术式
        if (data.getTechnique() == technique) {
            return TechniqueSource.NATIVE;
        }

        // 复制的术式（使用 getRealCopied）
        Set<CursedTechnique> copied = getRealCopied(owner);
        if (copied.contains(technique)) {
            return TechniqueSource.COPIED;
        }

        // 偷取的术式（使用 getRealStolen）
        Set<CursedTechnique> stolen = getRealStolen(owner);
        if (stolen.contains(technique)) {
            return TechniqueSource.STOLEN;
        }

        return TechniqueSource.NONE;
    }

    /**
     * ★★★ 检查术式是否当前激活 ★★★
     * 复制术式检查 getCurrentCopied()
     * 偷取术式检查 getCurrentStolen()
     */
    public static boolean isTechniqueActive(LivingEntity owner, CursedTechnique technique) {
        if (technique == null) return false;

        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return false;

        TechniqueSource source = getTechniqueSource(owner, technique);

        switch (source) {
            case COPIED:
                return data.getCurrentCopied() == technique;
            case STOLEN:
                return data.getCurrentStolen() == technique;
            default:
                return false;
        }
    }

    /**
     * ★★★ 获取当前激活的所有额外术式 ★★★
     */
    public static Set<CursedTechnique> getActiveTechniques(LivingEntity owner) {
        Set<CursedTechnique> result = new HashSet<>();

        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return result;

        CursedTechnique currentCopied = data.getCurrentCopied();
        if (currentCopied != null) {
            result.add(currentCopied);
        }

        CursedTechnique currentStolen = data.getCurrentStolen();
        if (currentStolen != null) {
            result.add(currentStolen);
        }

        return result;
    }

    /**
     * ★★★ 检查技能是否可用（所属术式是否激活）★★★
     */
    public static boolean isAbilityAvailable(LivingEntity owner, Ability ability) {
        if (ability == null) return false;

        // 获取技能所属的术式和来源
        for (CursedTechnique technique : getAllExtraTechniques(owner)) {
            if (technique == null) continue;
            for (Ability techAbility : technique.getAbilities()) {
                if (techAbility == ability) {
                    // 找到了，检查这个术式是否激活
                    return isTechniqueActive(owner, technique);
                }
            }
        }

        // 不是额外术式的技能，默认可用
        return true;
    }

    /**
     * 获取技能所属的术式
     */
    @Nullable
    public static CursedTechnique getAbilityTechnique(LivingEntity owner, Ability ability) {
        if (ability == null) return null;

        for (CursedTechnique technique : getAllExtraTechniques(owner)) {
            if (technique != null) {
                for (Ability techAbility : technique.getAbilities()) {
                    if (techAbility == ability) {
                        return technique;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 获取技能所属术式的来源
     */
    public static TechniqueSource getAbilitySource(LivingEntity owner, Ability ability) {
        CursedTechnique technique = getAbilityTechnique(owner, ability);
        if (technique == null) return TechniqueSource.NONE;
        return getTechniqueSource(owner, technique);
    }

    // ==================== 冲突检查 ====================

    /**
     * ★★★ 检查技能是否因所属术式未激活而有冲突 ★★★
     * 用于技能栏显示判断
     *
     * @param owner   实体
     * @param ability 要检查的技能
     * @return true = 有冲突（术式未激活），false = 无冲突
     */
    public static boolean hasAbilityTechniqueConflict(LivingEntity owner, Ability ability) {
        if (owner == null || ability == null) return false;

        // 获取技能所属的额外术式
        CursedTechnique technique = getAbilityTechnique(owner, ability);

        // 不是额外术式的技能，没有冲突
        if (technique == null) {
            return false;
        }

        // 如果术式未激活，则有冲突
        return !isTechniqueActive(owner, technique);
    }

    /**
     * 判断术式是否是复制类型（模仿）
     */
    public static boolean isCopyTechnique(@Nullable CursedTechnique technique) {
        if (technique == null) return false;
        return technique == CursedTechnique.MIMICRY;
    }

    /**
     * 判断术式是否是偷取类型（大脑移植）
     */
    public static boolean isStealTechnique(@Nullable CursedTechnique technique) {
        if (technique == null) return false;
        return technique == CursedTechnique.BRAIN_TRANSPLANT;
    }

    /**
     * 术式来源枚举
     */
    public enum TechniqueSource {
        NONE,       // 未知/不存在
        NATIVE,     // 原生术式
        COPIED,     // 复制（咒骂模倣）
        STOLEN      // 偷取（大脑操作）
    }
}
