package com.jujutsuaddon.addon.init;

import com.jujutsuaddon.addon.vow.benefit.BenefitRegistry;
import com.jujutsuaddon.addon.vow.benefit.types.ordinary.CooldownReductionBenefit;
import com.jujutsuaddon.addon.vow.benefit.types.ordinary.EnergyBoostBenefit;
import com.jujutsuaddon.addon.vow.benefit.types.ordinary.OutputBoostBenefit;
import com.jujutsuaddon.addon.vow.benefit.types.permanent.WorldSlashBypassBenefit;

/**
 * 收益类型注册
 * Benefit Type Registration
 *
 * 在mod初始化时调用，注册所有可用的收益类型。
 * 玩家创建誓约时可以从这些收益中选择。
 */
public class AddonBenefits {

    /**
     * 注册所有收益类型
     */
    public static void register() {
        // 输出提升 - 增加咒力输出百分比
        BenefitRegistry.register(new OutputBoostBenefit());

        // 咒力量提升 - 增加最大咒力量
        BenefitRegistry.register(new EnergyBoostBenefit());

        // 冷却缩减 - 减少技能冷却时间
        BenefitRegistry.register(new CooldownReductionBenefit());
        // 无视条件施法世界斩
        BenefitRegistry.register(new WorldSlashBypassBenefit());

        // TODO: 添加更多收益类型
        // - DamageBoostBenefit: 直接伤害加成
        // - DefenseBoostBenefit: 伤害减免
        // - AbilityUnlockBenefit: 解锁特殊技能
    }
}
