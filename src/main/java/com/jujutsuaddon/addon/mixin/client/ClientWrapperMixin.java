package com.jujutsuaddon.addon.mixin.client;

import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.c2s.OpenShadowStorageC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.client.ClientWrapper;

/**
 * 拦截原版影子库存界面，替换为我们的容器式界面
 */
@Mixin(value = ClientWrapper.class, remap = false)
public class ClientWrapperMixin {

    /**
     * 拦截原版的影子库存界面，发送网络包请求服务端打开容器
     */
    @Inject(method = "openShadowInventory", at = @At("HEAD"), cancellable = true)
    private static void jujutsuaddon$openShadowInventory(CallbackInfo ci) {
        // 发送网络包请求服务端打开容器界面
        AddonNetwork.sendToServer(new OpenShadowStorageC2SPacket());
        ci.cancel();
    }
}
