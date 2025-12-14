package com.jujutsuaddon.addon.mixin.core;

import com.jujutsuaddon.addon.util.context.WeaponProxyContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.item.cursed_tool.SplitSoulKatanaItem;

@Mixin(Player.class)
public class MixinPlayerAttack {

    @ModifyVariable(method = "attack", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private float modifyAttackDamage(float damage) {
        Player player = (Player) (Object) this;
        ItemStack stack = player.getMainHandItem();

        if (!stack.isEmpty() && stack.getItem() instanceof SplitSoulKatanaItem) {
            return damage;
        }

        return damage;
    }

    // 新加：技能伤害时强制返回满蓄力
    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void forceFullChargeForAbility(float adjustTicks, CallbackInfoReturnable<Float> cir) {
        if (WeaponProxyContext.isActive()) {
            cir.setReturnValue(1.0F);
        }
    }
}
