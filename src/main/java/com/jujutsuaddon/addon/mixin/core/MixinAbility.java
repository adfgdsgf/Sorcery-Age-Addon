package com.jujutsuaddon.addon.mixin.core;

import com.jujutsuaddon.addon.vow.manager.VowManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.base.Ability;

@Mixin(value = Ability.class, remap = false)
public class MixinAbility {

    // ==================== 1. 限制类誓约 (拦截) ====================
    /**
     * 修复：原先写的 canUse 不存在。
     * 正确的方法名是 "isTriggerable"，它返回 Ability.Status 枚举。
     */
    @Inject(method = "isTriggerable", at = @At("HEAD"), cancellable = true)
    private void checkVowConditions(LivingEntity owner, CallbackInfoReturnable<Ability.Status> cir) {
        // 只在服务端检查玩家
        if (!(owner instanceof Player player) || owner.level().isClientSide) return;

        // 1. 检查是否允许使用
        boolean allowed = VowManager.checkAbilityAttempt(player, (Ability) (Object) this);

        // 2. 如果不允许
        if (!allowed) {
            // 发送消息
            player.displayClientMessage(Component.translatable("vow.message.condition_prevented"), true);
            // ★ 返回 DISABLE 状态，这是原模组中表示“被禁用”的状态，技能将无法释放
            cir.setReturnValue(Ability.Status.DISABLE);
        }
    }

    // ==================== 2. 代价类誓约 (反噬/扣血) ====================
    /**
     * 注入到 charge 方法。
     * 这个方法真实存在，用于扣除咒力和冷却。我们顺便在这里扣血。
     */
    @Inject(method = "charge", at = @At("RETURN"))
    private void applyVowSideEffects(LivingEntity owner, CallbackInfo ci) {
        if (owner instanceof Player player && !owner.level().isClientSide) {
            // 执行扣血逻辑
            VowManager.onAbilityExecuted(player, (Ability) (Object) this);
        }
    }
}
