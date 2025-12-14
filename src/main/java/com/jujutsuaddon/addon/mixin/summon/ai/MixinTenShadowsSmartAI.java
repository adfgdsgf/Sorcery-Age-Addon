package com.jujutsuaddon.addon.mixin.summon.ai;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import java.util.List;
import java.util.UUID;

@Mixin(TenShadowsSummon.class)
public abstract class MixinTenShadowsSmartAI extends TamableAnimal {

    @Shadow(remap = false) protected List<UUID> participants;

    protected MixinTenShadowsSmartAI(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    // 注意：canAttack 和 isAlliedTo 已移动到 MixinTenShadowsSummonFix 以解决冲突和自瞄问题

    @Inject(method = "tick", at = @At("HEAD"))
    private void forceAttackLogic(CallbackInfo ci) {
        if (this.level().isClientSide) return;

        if (!this.isTame()) {
            // 【优化：仇恨转移逻辑】
            // 只有在【完全没有目标】的时候，才主动去寻找参与者。
            // 这样保证了：如果你 Shift+右键 指定它打别人，或者它正在打铁傀儡，它不会突然转头打你。
            if (this.getTarget() == null || !this.getTarget().isAlive()) {

                // 降低扫描频率 (每秒一次)
                if (this.tickCount % 20 == 0) {
                    List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(20.0));
                    for (Player player : nearbyPlayers) {
                        if (this.participants.contains(player.getUUID())) {
                            this.setTarget(player);
                            this.setAggressive(true);
                            break;
                        }
                    }
                }
            }
        }
    }

    // 拦截坐下：战斗中禁止坐下
    @Override
    public void setOrderedToSit(boolean isSitting) {
        boolean hasTarget = this.getTarget() != null && this.getTarget().isAlive();
        if (hasTarget) {
            super.setOrderedToSit(false);
        } else {
            super.setOrderedToSit(isSitting);
        }
    }

    // 拦截浮水 Goal
    @Redirect(
            method = "createGoals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V"
            )
    )
    private void removeFloatGoal(GoalSelector instance, int priority, Goal goal) {
        if (goal instanceof FloatGoal) {
            return;
        }
        instance.addGoal(priority, goal);
    }
}
