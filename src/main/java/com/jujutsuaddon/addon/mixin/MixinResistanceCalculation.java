package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.util.helper.SoulDamageUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinResistanceCalculation {

    // ★★★ 新增：拦截物理护甲计算 (getDamageAfterArmorAbsorb) ★★★
    // 这就是你缺失的部分，加上这个，钻石甲/下界合金甲就防不住了
    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void onGetDamageAfterArmorAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity target = (LivingEntity) (Object) this;

        if (source.getEntity() instanceof LivingEntity attacker) {
            // 使用 SoulDamageUtil 判断是否为真伤 (包含释魂刀逻辑)
            if (SoulDamageUtil.shouldApplyTrueDamage(source, attacker)) {
                double bypassRatio = AddonConfig.COMMON.soulResistanceBypass.get();

                // 如果配置是全穿透，直接返回原始伤害，跳过护甲计算
                if (bypassRatio >= 0.99) {
                    cir.setReturnValue(amount);
                }
            }
        }
    }

    // ★★★ 原有：拦截魔法抗性计算 (getDamageAfterMagicAbsorb) ★★★
    // 这个保留，用于穿透抗性提升药水和保护附魔
    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("HEAD"), cancellable = true)
    private void onGetDamageAfterMagicAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity target = (LivingEntity) (Object) this;

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (SoulDamageUtil.shouldApplyTrueDamage(source, attacker)) {
                double bypassRatio = AddonConfig.COMMON.soulResistanceBypass.get();
                if (bypassRatio >= 0.99) {
                    cir.setReturnValue(amount);
                }
            }
        }
    }
}
