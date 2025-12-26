package com.jujutsuaddon.addon.vow.penalty;

/**
 * 惩罚严重程度枚举
 * Penalty Severity
 *
 * 用于UI警告显示和惩罚强度判断
 */
public enum PenaltySeverity {
    /** 轻微 - 小幅度debuff，短时间效果 */
    MINOR,

    /** 中等 - 中等debuff，可能影响战斗能力 */
    MODERATE,

    /** 严重 - 严重debuff，大幅削弱能力 */
    SEVERE,

    /** 致命 - 可能导致死亡或永久性损失 */
    FATAL
}
