package com.jujutsuaddon.addon.mixin.fix.slice;

import org.joml.Matrix3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.client.slice.LegacyMath;

/**
 * 修复 JJK 切片渲染的矩阵求逆崩溃
 * 当惯性张量矩阵不可逆时，使用单位矩阵作为 fallback
 */
@Mixin(value = LegacyMath.class, remap = false)
public class MixinLegacyMath {

    /**
     * 在 invert 方法开头检查矩阵是否可逆
     * 如果行列式接近0，直接设置为单位矩阵并跳过原方法
     */
    @Inject(method = "invert", at = @At("HEAD"), cancellable = true)
    private static void jujutsuAddon$safeInvert(Matrix3f mat, CallbackInfo ci) {
        float det = mat.determinant();

        // 行列式接近0，矩阵不可逆
        if (Math.abs(det) < 1.0E-6f) {
            // 设置为单位矩阵作为安全的 fallback
            mat.identity();
            ci.cancel();  // 跳过原方法，避免抛出异常
        }
    }
}
