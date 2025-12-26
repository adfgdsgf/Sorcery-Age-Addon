package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.vow.condition.types.ordinary.RecoilCondition;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.ability.limitless.Infinity;

// 目标是 Infinity 类中的静态内部类 InfinityForgeEvents
@Mixin(Infinity.InfinityForgeEvents.class)
public class MixinInfinityEvents {

    /**
     * 拦截 Infinity 的伤害判定。
     * 如果是反噬伤害，直接 return，不让 Infinity 执行 event.setCanceled(true)。
     */
    @Inject(method = "onLivingAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jujutsuAddon$bypassInfinity(LivingAttackEvent event, CallbackInfo ci) {
        // 检查是否处于反噬状态 (由 RecoilCondition 设置)
        if (RecoilCondition.isRecoilActive()) {
            // 直接退出 Infinity 的处理方法
            // 这样 Infinity 就不会把这个伤害事件 cancel 掉
            ci.cancel();
        }
    }
}
