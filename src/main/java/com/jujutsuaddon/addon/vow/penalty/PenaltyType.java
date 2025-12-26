package com.jujutsuaddon.addon.vow.penalty;

/**
 * 惩罚类型枚举
 * Penalty Type
 *
 * 定义惩罚的执行方式和持续性
 */
public enum PenaltyType {
    /** 即时惩罚 - 立即执行一次，如受到伤害 */
    INSTANT,

    /** 临时惩罚 - 持续一段时间，如debuff效果 */
    TEMPORARY,

    /** 永久惩罚 - 无法恢复，如永久降低属性 */
    PERMANENT,

    /** 条件惩罚 - 满足特定条件时触发 */
    CONDITIONAL
}
