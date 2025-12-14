package com.jujutsuaddon.addon.mixin.summon;

import com.jujutsuaddon.addon.util.context.TamedCostContext;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.base.Summon;

@Mixin(Summon.class)
public class MixinSummonCost {

    // 拦截 isTamed 方法
    // remap = false 是因为 JJK 模组通常是非混淆环境，如果报错请尝试去掉
    @Inject(method = "isTamed", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectForceTamed(LivingEntity owner, CallbackInfoReturnable<Boolean> cir) {
        // 只有当我们的上下文开关打开时，才强制返回 true
        if (TamedCostContext.shouldForceTamed()) {
            cir.setReturnValue(true);
        }
    }
}
