package com.jujutsuaddon.addon.init;

import com.jujutsuaddon.addon.vow.penalty.PenaltyRegistry;
import com.jujutsuaddon.addon.vow.penalty.types.ordinary.CursedEnergyBacklashPenalty;
import com.jujutsuaddon.addon.vow.penalty.types.ordinary.TechniqueDamagePenalty;
import com.jujutsuaddon.addon.vow.penalty.types.ordinary.TechniqueSealPenalty;

/**
 * 惩罚类型注册
 * Penalty Type Registration
 *
 * 在mod初始化时调用，注册所有可用的惩罚类型。
 * 惩罚会在玩家违反誓约时自动执行。
 */
public class AddonPenalties {

    /**
     * 注册所有惩罚类型
     */
    public static void register() {
        // 咒力反噬 - 即时伤害
        PenaltyRegistry.register(new CursedEnergyBacklashPenalty());

        // 术式封印 - 临时禁用技能
        PenaltyRegistry.register(new TechniqueSealPenalty());

        // 术式损坏 - 永久降低效率（仅永久誓约）
        PenaltyRegistry.register(new TechniqueDamagePenalty());

        // TODO: 添加更多惩罚类型
        // - CursedEnergyDrainPenalty: 咒力清空
        // - DebuffPenalty: 添加负面效果
        // - ExperienceLossPenalty: 损失经验
    }
}
