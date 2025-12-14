package com.jujutsuaddon.addon.mixin.summon.ai;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.helper.SummonAIHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

import java.util.UUID;

@Mixin(Mob.class)
public abstract class MixinSummonAI extends LivingEntity {

    @Shadow @Final protected GoalSelector targetSelector;

    @Unique
    private static final UUID FOLLOW_RANGE_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000003");

    @Unique private int patienceTimer = 0;
    @Unique private LivingEntity lastTarget = null;
    @Unique private int attackCooldown = 0;
    @Unique private int jumpCooldown = 0;
    @Unique private int attackProtectionTimer = 0;
    @Unique private boolean wasSwinging = false;

    protected MixinSummonAI(net.minecraft.world.entity.EntityType<? extends LivingEntity> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onSummonAITick(CallbackInfo ci) {
        if (!((Object) this instanceof SummonEntity summon)) return;
        if (this.level().isClientSide) return;
        if (!summon.isTame()) return;

        // 1. 初始化 AI
        if (!summon.getTags().contains("jjk_addon_ai_initialized")) {
            SummonAIHelper.cleanUpBadGoals(this.targetSelector);
            SummonAIHelper.registerSmartGoals(summon, this.targetSelector);
            summon.addTag("jjk_addon_ai_initialized");
        }

        // 攻击保护期
        if (summon.swinging && !this.wasSwinging) {
            this.attackProtectionTimer = 60;
        }
        this.wasSwinging = summon.swinging;

        if (this.attackProtectionTimer > 0 || summon.swinging) {
            if (this.attackProtectionTimer > 0) {
                this.attackProtectionTimer--;
            }
            this.patienceTimer = 0;
            if (this.jumpCooldown > 0) this.jumpCooldown--;
            if (summon.horizontalCollision && summon.onGround() && summon.getTarget() != null && this.jumpCooldown == 0) {
                summon.getJumpControl().jump();
                this.jumpCooldown = 10;
            }
            return;
        }

        // ★ 修复：只有被玩家骑乘时才跳过攻击 ★
        if (summon.isVehicle()) {
            boolean riddenByPlayer = summon.getPassengers().stream()
                    .anyMatch(e -> e instanceof Player);
            if (riddenByPlayer) {
                if (summon.getTarget() != null) summon.setTarget(null);
                return;
            }
            // 被武器实体等骑乘时继续正常 AI
        }

        LivingEntity currentTarget = summon.getTarget();
        LivingEntity owner = summon.getOwner();

        if (owner != null && currentTarget == owner) return;

        // 攻击冷却
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            if (summon.getTarget() != null) summon.setTarget(null);
            return;
        }

        // 跳跃辅助
        if (this.jumpCooldown > 0) this.jumpCooldown--;
        if (summon.horizontalCollision && summon.onGround() && summon.getTarget() != null && this.jumpCooldown == 0) {
            summon.getJumpControl().jump();
            this.jumpCooldown = 10;
        }

        // 距离回防
        if (owner != null && currentTarget != null) {
            double distToOwnerSqr = summon.distanceToSqr(owner);
            double baseDist = AddonConfig.COMMON.summonRegroupDistance.get();
            boolean isManualTarget = !summon.getTags().contains("jjk_is_auto_targeting");
            if (isManualTarget) {
                baseDist *= AddonConfig.COMMON.manualTargetDistanceMultiplier.get();
            }
            double thresholdSqr = baseDist * baseDist;

            if (distToOwnerSqr > thresholdSqr) {
                if (currentTarget != owner) {
                    boolean isRitualEnemy = currentTarget.getTags().contains("jjk_ritual_enemy");
                    if (!isRitualEnemy || distToOwnerSqr > thresholdSqr * 1.5) {
                        summon.setTarget(null);
                        this.lastTarget = null;
                        summon.removeTag("jjk_is_auto_targeting");
                        return;
                    }
                }
            }
        }

        // 目标状态检查
        if (currentTarget != null) {
            if (!currentTarget.isAlive() || currentTarget.isRemoved()) {
                this.resetTarget(summon, false);
                return;
            }

            // 耐心计时
            if (currentTarget == this.lastTarget) {
                this.patienceTimer++;
                if (summon.distanceToSqr(currentTarget) < 9.0) {
                    this.patienceTimer = 0;
                }
                if (this.patienceTimer > 160) {
                    this.resetTarget(summon, true);
                }
            } else {
                this.lastTarget = currentTarget;
                this.patienceTimer = 0;
            }
        } else {
            this.patienceTimer = 0;
            this.lastTarget = null;
            if (summon.getTags().contains("jjk_is_auto_targeting")) {
                summon.removeTag("jjk_is_auto_targeting");
            }
        }

        // 扩展索敌范围
        if (this.tickCount % 20 == 0) {
            AttributeInstance followRange = summon.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null && followRange.getModifier(FOLLOW_RANGE_UUID) == null) {
                followRange.addTransientModifier(new AttributeModifier(
                        FOLLOW_RANGE_UUID, "JJK Addon Smart AI", 64.0, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    @Unique
    private void resetTarget(net.minecraft.world.entity.TamableAnimal summon, boolean triggerCooldown) {
        summon.setTarget(null);
        summon.setLastHurtByMob(null);
        summon.removeTag("jjk_is_auto_targeting");
        this.lastTarget = null;
        this.patienceTimer = 0;
        if (triggerCooldown) {
            this.attackCooldown = 60;
        }
    }
}
