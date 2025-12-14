package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.helper.SoulDamageUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinResistanceCalculation {

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("HEAD"), cancellable = true)
    private void onGetDamageAfterMagicAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity target = (LivingEntity) (Object) this;

        // 检查是否应该应用真伤逻辑 (SSK 或 灵魂伤害)
        // SoulDamageUtil.shouldApplyTrueDamage 内部已经判断了是否为 HR 玩家拿刀
        if (source.getEntity() instanceof LivingEntity attacker) {
            if (SoulDamageUtil.shouldApplyTrueDamage(source, attacker)) {

                // 获取配置的穿透比例 (0.0 ~ 1.0)
                double bypassRatio = AddonConfig.COMMON.soulResistanceBypass.get();

                // 如果配置为 1.0 (100% 穿透)，直接返回原始伤害，跳过抗性计算
                if (bypassRatio >= 0.99) {
                    cir.setReturnValue(amount);
                }
                // 如果是部分穿透 (例如 0.5)，则需要手动计算抗性减免然后补回来
                // 但通常真伤都是 100% 穿透，这里为了性能直接处理全穿透情况
            }
        }
    }
}
