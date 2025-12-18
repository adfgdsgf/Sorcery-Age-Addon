package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.PressureConfig;
import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.limitless.Infinity;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

@Mixin(value = Infinity.class, remap = false)
public class InfinityCostMixin {

    /**
     * 根据压制等级动态调整无下限的咒力消耗
     */
    @Inject(method = "getCost", at = @At("HEAD"), cancellable = true)
    private void jujutsuAddon$modifyCostByPressure(LivingEntity owner, CallbackInfoReturnable<Float> cir) {
        // 如果压力系统未启用，使用原版消耗
        if (!PressureConfig.isEnabled()) return;

        // 如果动态消耗未启用，使用原版消耗
        if (!PressureConfig.isPressureCostEnabled()) return;

        // 获取能力数据
        ISorcererData data = owner.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return;

        // 检查是否实现了压力访问器接口
        if (!(data instanceof IInfinityPressureAccessor accessor)) return;

        // 获取当前压制等级
        int pressureLevel = accessor.jujutsuAddon$getInfinityPressure();

        // 计算并返回动态消耗
        float finalCost = PressureConfig.calculatePressureCost(pressureLevel);

        cir.setReturnValue(finalCost);
    }
}
