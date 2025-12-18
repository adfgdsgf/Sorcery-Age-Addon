package com.jujutsuaddon.addon.summon.ai;

import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

/**
 * 每个式神独立的 AI 控制器，存储状态
 */
public class SummonAIController {

    private final SummonEntity summon;

    // 状态字段
    private boolean goalsInitialized = false;
    private int patienceTimer = 0;
    private LivingEntity lastTarget = null;
    private int attackCooldown = 0;
    private int jumpCooldown = 0;
    private int attackProtectionTimer = 0;
    private boolean wasSwinging = false;
    private int autoTargetCooldown = 0;

    public SummonAIController(SummonEntity summon) {
        this.summon = summon;
    }

    public SummonEntity getSummon() {
        return summon;
    }

    // ===== Getters & Setters =====

    public boolean isGoalsInitialized() {
        return goalsInitialized;
    }

    public void setGoalsInitialized(boolean initialized) {
        this.goalsInitialized = initialized;
    }

    public int getPatienceTimer() {
        return patienceTimer;
    }

    public void incrementPatienceTimer() {
        this.patienceTimer++;
    }

    public void resetPatienceTimer() {
        this.patienceTimer = 0;
    }

    public LivingEntity getLastTarget() {
        return lastTarget;
    }

    public void setLastTarget(LivingEntity target) {
        this.lastTarget = target;
    }

    public int getAttackCooldown() {
        return attackCooldown;
    }

    public void setAttackCooldown(int cooldown) {
        this.attackCooldown = cooldown;
    }

    public void decrementAttackCooldown() {
        if (this.attackCooldown > 0) this.attackCooldown--;
    }

    public int getJumpCooldown() {
        return jumpCooldown;
    }

    public void setJumpCooldown(int cooldown) {
        this.jumpCooldown = cooldown;
    }

    public void decrementJumpCooldown() {
        if (this.jumpCooldown > 0) this.jumpCooldown--;
    }

    public int getAttackProtectionTimer() {
        return attackProtectionTimer;
    }

    public void setAttackProtectionTimer(int timer) {
        this.attackProtectionTimer = timer;
    }

    public void decrementAttackProtectionTimer() {
        if (this.attackProtectionTimer > 0) this.attackProtectionTimer--;
    }

    public boolean wasSwinging() {
        return wasSwinging;
    }

    public void setWasSwinging(boolean swinging) {
        this.wasSwinging = swinging;
    }

    public int getAutoTargetCooldown() {
        return autoTargetCooldown;
    }

    public void setAutoTargetCooldown(int cooldown) {
        this.autoTargetCooldown = cooldown;
    }

    public void decrementAutoTargetCooldown() {
        if (this.autoTargetCooldown > 0) this.autoTargetCooldown--;
    }

    /**
     * 重置目标相关状态
     */
    public void resetTargetState(boolean triggerCooldown) {
        this.summon.setTarget(null);
        this.summon.setLastHurtByMob(null);
        this.summon.removeTag("jjk_is_auto_targeting");
        this.lastTarget = null;
        this.patienceTimer = 0;
        if (triggerCooldown) {
            this.attackCooldown = 60;
        }
    }
}
