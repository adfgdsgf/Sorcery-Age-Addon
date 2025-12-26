package com.jujutsuaddon.addon.vow.penalty;

import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.ViolationRecord;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.base.Ability;

/**
 * 违约上下文
 * Violation Context
 *
 * 封装违约发生时的完整上下文信息，
 * 用于惩罚执行时获取相关信息。
 *
 * 包含：
 * - 违约的誓约信息
 * - 违反的具体条件
 * - 违约记录
 * - 触发违约的技能（如果有）
 */
public class ViolationContext {

    /** 被违反的誓约 */
    private final CustomBindingVow vow;

    /** 违约记录 */
    private final ViolationRecord record;

    /** 被违反的条件 */
    private final ICondition violatedCondition;

    /** 违约发生的游戏时间 */
    private final long gameTime;

    /** 触发违约的技能（可能为null） */
    @Nullable
    private Ability attemptedAbility;

    public ViolationContext(CustomBindingVow vow, ViolationRecord record,
                            ICondition violatedCondition, long gameTime) {
        this.vow = vow;
        this.record = record;
        this.violatedCondition = violatedCondition;
        this.gameTime = gameTime;
    }

    // ==================== Getters ====================

    public CustomBindingVow getVow() {
        return vow;
    }

    public ViolationRecord getRecord() {
        return record;
    }

    public ICondition getViolatedCondition() {
        return violatedCondition;
    }

    public long getGameTime() {
        return gameTime;
    }

    /**
     * 判断违反的是否是永久誓约
     * 永久誓约的违约惩罚通常更严重
     */
    public boolean isPermanentVow() {
        return vow.isPermanent();
    }

    @Nullable
    public Ability getAttemptedAbility() {
        return attemptedAbility;
    }

    /**
     * 设置触发违约的技能
     * @param ability 尝试使用的技能
     * @return 当前上下文（支持链式调用）
     */
    public ViolationContext withAbility(Ability ability) {
        this.attemptedAbility = ability;
        return this;
    }
}
