package com.jujutsuaddon.addon.util.helper;

import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

/**
 * 通用术式访问权限工具类
 * 统一处理：原生术式、偷取的术式、复制的术式、嵌套获取的术式
 *
 * 可用于：十影、咒灵操术、或任何其他需要检查术式权限的功能
 */
public class TechniqueAccessHelper {

    /**
     * 检查玩家是否【当前可以使用】指定术式
     * （必须是原生或当前激活的术式之一）
     *
     * @param entity 实体
     * @param technique 要检查的术式
     * @return 是否可以使用
     */
    public static boolean canUseTechnique(LivingEntity entity, CursedTechnique technique) {
        if (entity == null || technique == null) return false;

        ISorcererData sorcererData = entity.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;

        CursedTechnique nativeTech = sorcererData.getTechnique();

        // ★★★ 情况1：原生术式 - 始终可用 ★★★
        if (nativeTech == technique) {
            return true;
        }

        // ★★★ 情况2：偷取的术式 - 必须是当前激活的 ★★★
        CursedTechnique currentStolen = sorcererData.getCurrentStolen();
        if (currentStolen == technique) {
            return true;
        }

        // ★★★ 情况3：复制的术式 - 必须是当前激活的 ★★★
        CursedTechnique currentCopied = sorcererData.getCurrentCopied();
        if (currentCopied == technique) {
            return true;
        }

        // ★★★ 情况4：通过偷取的复制能力获得的术式 ★★★
        // 如果偷取了复制术式，检查复制的当前激活术式
        if (currentStolen != null && TechniqueHelper.isCopyTechnique(currentStolen)) {
            if (currentCopied == technique) {
                return true;
            }
        }

        // ★★★ 情况5：通过复制的偷取能力获得的术式 ★★★
        // 如果复制了偷取术式，检查偷取的当前激活术式
        if (currentCopied != null && TechniqueHelper.isStealTechnique(currentCopied)) {
            if (currentStolen == technique) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查玩家是否【拥有】指定术式（不管是否激活）
     *
     * @param entity 实体
     * @param technique 要检查的术式
     * @return 是否拥有
     */
    public static boolean ownsTechnique(LivingEntity entity, CursedTechnique technique) {
        if (entity == null || technique == null) return false;

        ISorcererData sorcererData = entity.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (sorcererData == null) return false;

        // 原生术式
        if (sorcererData.getTechnique() == technique) return true;

        // 偷取的术式
        if (sorcererData.hasStolen(technique)) return true;

        // 复制的术式
        if (sorcererData.getCopied().contains(technique)) return true;

        return false;
    }

    // ==================== 便捷方法：十影 ====================

    /**
     * 检查是否可以使用十影
     */
    public static boolean canUseTenShadows(LivingEntity entity) {
        return canUseTechnique(entity, CursedTechnique.TEN_SHADOWS);
    }

    /**
     * 检查是否拥有十影
     */
    public static boolean ownsTenShadows(LivingEntity entity) {
        return ownsTechnique(entity, CursedTechnique.TEN_SHADOWS);
    }

    // ==================== 便捷方法：咒灵操术 ====================

    /**
     * 检查是否可以使用咒灵操术
     */
    public static boolean canUseCurseManipulation(LivingEntity entity) {
        return canUseTechnique(entity, CursedTechnique.CURSE_MANIPULATION);
    }

    /**
     * 检查是否拥有咒灵操术
     */
    public static boolean ownsCurseManipulation(LivingEntity entity) {
        return ownsTechnique(entity, CursedTechnique.CURSE_MANIPULATION);
    }

    // ==================== 便捷方法：其他常用术式 ====================

    /**
     * 检查是否可以使用无限
     */
    public static boolean canUseInfinity(LivingEntity entity) {
        return canUseTechnique(entity, CursedTechnique.LIMITLESS);
    }

    /**
     * 检查是否可以使用御厨子
     */
    public static boolean canUseMimicry(LivingEntity entity) {
        return canUseTechnique(entity, CursedTechnique.SHRINE);
    }
}
