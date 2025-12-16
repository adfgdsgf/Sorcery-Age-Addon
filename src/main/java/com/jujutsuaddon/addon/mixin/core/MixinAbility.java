package com.jujutsuaddon.addon.mixin.core;

import com.jujutsuaddon.addon.util.context.AbilityContext;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.base.Ability;

@Mixin(value = Ability.class, remap = false)
public class MixinAbility {

    /**
     * 只设置技能上下文，不再修改 power 返回值
     * balancerMult 统一由 AbilityDamageCalculator 处理
     */
    @Inject(method = "getPower(Lradon/jujutsu_kaisen/ability/base/Ability;Lnet/minecraft/world/entity/LivingEntity;)F",
            at = @At("HEAD"))
    private static void onGetPower(Ability ability, LivingEntity owner, CallbackInfoReturnable<Float> cir) {
        if (!owner.level().isClientSide) {
            AbilityContext.set(ability);
        }
    }
}
