package com.jujutsuaddon.addon.vow.util;

import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

public class VowConditionHelper {

    /**
     * 判断玩家是否拥有指定的【生得术式】
     * 简单的 Capability 检查
     */
    public static boolean hasInnateTechnique(LivingEntity entity, CursedTechnique targetTechnique) {
        if (entity == null) return false;
        return entity.getCapability(SorcererDataHandler.INSTANCE)
                .map(cap -> cap.getTechnique() == targetTechnique)
                .orElse(false);
    }

    /**
     * 判断玩家当前是否是“复制术师”（乙骨流派）
     */
    public static boolean isCopyUser(LivingEntity entity) {
        return hasInnateTechnique(entity, CursedTechnique.MIMICRY);
    }

    /**
     * ★ 新增：判断某个术式是否被玩家“真正拥有”
     * 逻辑：
     * 1. 如果是原生生得术式 -> 是
     * 2. 如果是羂索偷来的肉体术式 -> 是 (视为同等效力)
     * 3. 如果是乙骨复制在库里的 -> 否 (视为借用)
     *
     * @param entity 玩家
     * @param techniqueToCheck 要检查的术式
     */
    public static boolean isTechniqueTrulyOwned(LivingEntity entity, CursedTechnique techniqueToCheck) {
        if (entity == null) return false;

        // 1. 获取玩家当前的主术式
        CursedTechnique currentInnate = entity.getCapability(SorcererDataHandler.INSTANCE)
                .map(cap -> cap.getTechnique())
                .orElse(null);

        if (currentInnate == null) return false;

        // 2. 如果检查的就是当前的主术式，那肯定拥有
        if (currentInnate == techniqueToCheck) return true;

        // 3. 如果玩家是复制术师(乙骨)，且主术式不匹配(说明techniqueToCheck在复制库里)，则不算"真正拥有"
        if (currentInnate == CursedTechnique.MIMICRY) {
            return false;
        }

        // 4. 如果玩家不是乙骨（比如是羂索），但他有额外术式（偷来的），这里通常被视为"拥有"
        // (注：这里需要结合具体上下文，如果你在其他地方用到了，可以直接用这个逻辑)
        return true;
    }
    /**
     * ★ 新增：检查玩家是否可以使用某种术式（无论是生得、复制还是偷窃）
     * 用于 WorldSlashBypassBenefit 的 isAvailable 检查
     */
    public static boolean hasTechniqueAccess(LivingEntity entity, CursedTechnique target) {
        // 1. 先看主术式有没有
        if (hasInnateTechnique(entity, target)) return true;
        // 2. 如果是玩家，再看额外术式（复制/偷窃库）里有没有
        if (entity instanceof Player player) {
            for (CursedTechnique t : TechniqueHelper.getAllExtraTechniques(player)) {
                if (t == target) return true;
            }
        }
        return false;
    }
}
