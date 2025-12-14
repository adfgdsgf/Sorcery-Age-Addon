package com.jujutsuaddon.addon.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.entity.ai.goal.WaterWalkingFloatGoal;

@Mixin(WaterWalkingFloatGoal.class)
public abstract class MixinWaterWalkingFloatGoal extends Goal {

    @Shadow(remap = false) @Final private PathfinderMob mob;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        LivingEntity target = this.mob.getTarget();

        // 智能潜水逻辑
        if (target != null && target.isAlive() && target.isInWater()) {
            // 如果目标在水里，且高度比我低 (说明在水下)
            if (target.getY() < this.mob.getY() - 0.5) {
                // 1. 允许下潜 (关闭浮水)
                this.mob.getNavigation().setCanFloat(false);

                // 2. 阻止原版逻辑执行 (原版逻辑会强行跳跃)
                ci.cancel();
                return;
            }
        }

        // 正常情况：恢复浮水能力
        // 必须加这个，否则上岸后可能还会沉底
        this.mob.getNavigation().setCanFloat(true);
    }
}
