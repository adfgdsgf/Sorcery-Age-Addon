package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.event.vow.VowAbilityHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.base.Ability;

/**
 * Mixin: 拦截技能使用，检查誓约条件
 *
 * 目标：Ability.isTriggerable() 或类似的入口方法
 */
@Mixin(value = Ability.class, remap = false)
public abstract class AbilityVowCheckMixin {

    /**
     * 在技能触发检查时，额外检查誓约条件
     */
    @Inject(method = "isTriggerable", at = @At("HEAD"), cancellable = true)
    private void checkVowConditions(LivingEntity owner, CallbackInfoReturnable<Ability.Status> cir) {
        if (owner.level().isClientSide()) return;

        Ability self = (Ability) (Object) this;

        // ★★★ 修复：调用新方法 checkAndHandle ★★★
        // 逻辑：如果 checkAndHandle 返回 true，说明是【永久束缚】，必须强制拦截。
        // 如果是普通束缚，它会返回 false（放行），但在内部已经处理了惩罚。
        if (VowAbilityHandler.checkAndHandle(owner, self)) {
            // 违反永久誓约，返回失败状态 (物理墙壁)
            cir.setReturnValue(Ability.Status.FAILURE);
        }
    }
}
