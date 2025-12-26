package com.jujutsuaddon.addon.vow.benefit;

/**
 * 收益类别枚举
 * Benefit Category
 *
 * 用于对收益进行分类，主要作用：
 * 1. GUI中分类显示
 * 2. 处理同类收益的叠加规则
 * 3. 判断收益是否互斥
 */
public enum BenefitCategory {
    /** 输出类 - 增加咒力输出/伤害倍率 */
    OUTPUT,

    /** 咒力类 - 增加咒力量/恢复速度 */
    ENERGY,

    /** 冷却类 - 减少技能冷却时间 */
    COOLDOWN,

    /** 伤害类 - 直接增加伤害数值 */
    DAMAGE,

    /** 防御类 - 减少受到的伤害 */
    DEFENSE,

    /** 机制类 - 改变游戏机制 */
    MECHANIC,

    /** 技能解锁类 - 解锁新技能或变体 */
    ABILITY_UNLOCK,

    /** 技能强化类 - 增强现有技能效果 */
    ABILITY_ENHANCE,

    /** 感知类 - 增强感知能力 */
    PERCEPTION,

    /** 免疫类 - 免疫特定效果 */
    IMMUNITY,

    /** 术式类 - 术式专属增强 */
    TECHNIQUE,

    /** 条件类 - 满足条件时触发的收益 */
    CONDITIONAL,

    /** 其他类 - 不属于以上分类的收益 */
    OTHER
}
