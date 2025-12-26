package com.jujutsuaddon.addon.vow.validation;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.base.Ability;

/**
 * 违约检测上下文
 * Check Context
 *
 * 封装触发违约检测时的完整上下文信息。
 * 条件检测时可以根据上下文获取相关数据。
 */
public class CheckContext {

    /** 触发类型 */
    private final CheckTrigger trigger;

    /** 触发时的游戏时间 */
    private final long gameTime;

    /** 相关技能（技能类触发器） */
    @Nullable
    private final Ability ability;

    /** 相关实体（攻击/击杀等） */
    @Nullable
    private final Entity targetEntity;

    /** 伤害来源（受伤/攻击） */
    @Nullable
    private final DamageSource damageSource;

    /** 伤害数值 */
    private final float damageAmount;

    private CheckContext(Builder builder) {
        this.trigger = builder.trigger;
        this.gameTime = builder.gameTime;
        this.ability = builder.ability;
        this.targetEntity = builder.targetEntity;
        this.damageSource = builder.damageSource;
        this.damageAmount = builder.damageAmount;
    }

    /**
     * 创建构建器
     */
    public static Builder builder(CheckTrigger trigger, long gameTime) {
        return new Builder(trigger, gameTime);
    }

    // ==================== Getters ====================

    public CheckTrigger getTrigger() {
        return trigger;
    }

    public long getGameTime() {
        return gameTime;
    }

    @Nullable
    public Ability getAbility() {
        return ability;
    }

    @Nullable
    public Entity getTargetEntity() {
        return targetEntity;
    }

    @Nullable
    public DamageSource getDamageSource() {
        return damageSource;
    }

    public float getDamageAmount() {
        return damageAmount;
    }

    // ==================== Builder ====================

    public static class Builder {
        private final CheckTrigger trigger;
        private final long gameTime;
        private Ability ability;
        private Entity targetEntity;
        private DamageSource damageSource;
        private float damageAmount;

        private Builder(CheckTrigger trigger, long gameTime) {
            this.trigger = trigger;
            this.gameTime = gameTime;
        }

        public Builder ability(Ability ability) {
            this.ability = ability;
            return this;
        }

        public Builder target(Entity entity) {
            this.targetEntity = entity;
            return this;
        }

        public Builder damage(DamageSource source, float amount) {
            this.damageSource = source;
            this.damageAmount = amount;
            return this;
        }

        public CheckContext build() {
            return new CheckContext(this);
        }
    }
}
