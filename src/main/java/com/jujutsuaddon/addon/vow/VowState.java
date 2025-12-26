package com.jujutsuaddon.addon.vow;

/**
 * 誓约状态枚举
 * Vow State
 *
 * 表示誓约当前的生命周期阶段
 */
public enum VowState {

    /**
     * 未激活状态
     * 誓约已创建但尚未生效
     * 可以编辑、删除或激活
     */
    INACTIVE("vow.state.inactive", false),

    /**
     * 激活状态
     * 誓约正在生效中
     * 条件被监控，收益已应用
     */
    ACTIVE("vow.state.active", true),

    /**
     * 已解除状态
     * 誓约被玩家主动解除
     * 收益已移除，无惩罚
     */
    DISSOLVED("vow.state.dissolved", false),

    /**
     * 已违约状态
     * 誓约因违反条件而终止
     * 收益已移除，惩罚已执行
     */
    VIOLATED("vow.state.violated", false),

    /**
     * 已过期状态
     * 誓约因时间到期而终止（如果有时限）
     */
    EXPIRED("vow.state.expired", false),

    // ★★★ 新增：已耗尽状态 ★★★
    /**
     * 已耗尽状态
     * 永久誓约在一次性模式下使用后进入此状态
     * 收益失效，条件占用已释放，但保留在列表中作为记录
     */
    EXHAUSTED("vow.state.exhausted", false);

    /** 本地化键 */
    private final String translationKey;

    /** 是否为活跃状态（收益生效中） */
    private final boolean active;

    VowState(String translationKey, boolean active) {
        this.translationKey = translationKey;
        this.active = active;
    }

    /**
     * 获取本地化键
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * 判断是否为活跃状态
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 判断誓约是否已终止（无法再激活）
     */
    public boolean isTerminated() {
        // ★★★ 修改：包含 EXHAUSTED ★★★
        return this == DISSOLVED || this == VIOLATED || this == EXPIRED || this == EXHAUSTED;
    }
}
