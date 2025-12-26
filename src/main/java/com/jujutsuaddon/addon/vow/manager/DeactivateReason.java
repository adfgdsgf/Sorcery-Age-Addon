package com.jujutsuaddon.addon.vow.manager;

/**
 * 誓约停用原因枚举
 * Vow Deactivation Reason
 *
 * 记录誓约被停用的具体原因，用于后续处理和日志记录
 */
public enum DeactivateReason {
    /** 违反誓约条件导致停用 */
    VIOLATION,

    /** 玩家主动解除誓约 */
    DISSOLVED,

    /** 誓约时间到期自动停用 */
    EXPIRED,

    /** 誓约持有者死亡导致停用 */
    DEATH,

    /** 管理员/命令强制停用 */
    ADMIN,
    /** 永久誓约已耗尽 */
    EXHAUSTED
}
