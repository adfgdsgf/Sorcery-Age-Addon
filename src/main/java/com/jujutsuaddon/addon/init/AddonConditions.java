package com.jujutsuaddon.addon.init;

import com.jujutsuaddon.addon.vow.condition.ConditionRegistry;
import com.jujutsuaddon.addon.vow.condition.types.ordinary.*;
import com.jujutsuaddon.addon.vow.condition.types.permanent.AbilityBanCondition;

/**
 * 条件类型注册
 * Condition Type Registration
 *
 * 在mod初始化时调用，注册所有可用的条件类型。
 * 玩家创建誓约时可以从这些条件中选择。
 */
public class AddonConditions {

    /**
     * 注册所有条件类型
     */
    public static void register() {
        // 时间限制条件 - 只能在指定时间段使用
        ConditionRegistry.register(new TimeRangeCondition());

        // 血量阈值条件 - 只能在血量高于/低于某值时使用
        ConditionRegistry.register(new HealthThresholdCondition());

        // 技能禁用条件 - 禁止使用指定技能
        ConditionRegistry.register(new AbilityBanCondition());

        // 咏唱必须条件 - 必须咏唱才能使用技能
        ConditionRegistry.register(new ChantRequiredCondition());

        // 后坐力条件 - 使用技能后受到自伤
        ConditionRegistry.register(new RecoilCondition());

        // TODO: 添加更多条件类型
        // - TargetRestrictionCondition: 只能攻击特定类型目标
        // - LocationCondition: 只能在特定区域使用
        // - WeatherCondition: 只能在特定天气使用
    }
}
