package com.jujutsuaddon.addon.mixin.core;

import com.jujutsuaddon.addon.util.calc.AbilityBalancer;
import com.jujutsuaddon.addon.util.context.AbilityContext;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.base.Ability;

@Mixin(value = Ability.class, remap = false)
public class MixinAbility {

    // 注意：这里改成了 RETURN，并且开启了 cancellable = true
    @Inject(method = "getPower(Lradon/jujutsu_kaisen/ability/base/Ability;Lnet/minecraft/world/entity/LivingEntity;)F",
            at = @At("RETURN"), cancellable = true)
    private static void onGetPower(Ability ability, LivingEntity owner, CallbackInfoReturnable<Float> cir) {
        if (!owner.level().isClientSide) {
            // 1. 保持原有的上下文设置
            AbilityContext.set(ability);

            // 2. 获取原始 Power (原版模组算出来的)
            float originalPower = cir.getReturnValue();

            // 3. 获取平衡器倍率 (这里会触发日志打印)
            float balancerMult = AbilityBalancer.getDamageMultiplier(ability, owner);

            // 4. 修改返回值：原始值 * 倍率
            cir.setReturnValue(originalPower * balancerMult);
        }
    }
}
