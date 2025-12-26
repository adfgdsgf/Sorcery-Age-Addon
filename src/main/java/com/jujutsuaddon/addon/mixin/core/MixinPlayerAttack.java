package com.jujutsuaddon.addon.mixin.core;

import com.jujutsuaddon.addon.context.WeaponProxyContext;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Player.class)
public class MixinPlayerAttack {

 /*   // 删除了 modifyAttackDamage 方法：
    // 1. 原代码中它只是 return damage，没有实际修改任何数值。
    // 2. 它的 instanceof 检查导致了服务器崩溃。
    // 删除它既修复了崩溃，又清理了无用代码，不会有任何副作用。

    // 只保留这个修复技能蓄力的逻辑，这是安全的
    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void forceFullChargeForAbility(float adjustTicks, CallbackInfoReturnable<Float> cir) {
        if (WeaponProxyContext.isActive()) {
            cir.setReturnValue(1.0F);
        }
    }*/
}
