package com.jujutsuaddon.addon.mixin;

import com.jujutsuaddon.addon.client.input.ClientMovementHandler;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 键盘输入Mixin
 * 在输入被处理后、移动计算前修改
 */
@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void jujutsuAddon$modifyInputAfterTick(boolean isSneaking, float sneakMultiplier, CallbackInfo ci) {
        // 调用客户端移动处理器
        ClientMovementHandler.modifyInput(this);
    }
}
