package com.jujutsuaddon.addon.mixin.fix.slice;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import radon.jujutsu_kaisen.client.slice.CutModelUtil;

import java.util.Map;

/**
 * 跳过 glGetTexImage 和透明度检查
 */
@Mixin(value = CutModelUtil.class, remap = false)
public class MixinCutModelUtil {

    // 跳过纹理读取
    @Redirect(
            method = "collect",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z",
                    ordinal = 0
            ),
            remap = false
    )
    private static boolean jujutsuAddon$skipTextureRead(Map<?, ?> map, Object key) {
        return true;
    }

    // 跳过透明度检查
    @Redirect(
            method = "collect",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z",
                    ordinal = 1
            ),
            remap = false
    )
    private static boolean jujutsuAddon$skipAlphaCheck(Map<?, ?> map, Object key) {
        return false;
    }
}
