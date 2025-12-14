/*
package com.jujutsuaddon.addon.mixin.debug;

import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AttributeInstance.class)
public abstract class MixinDebugAttributes {

    @Shadow public abstract net.minecraft.world.entity.ai.attributes.Attribute getAttribute();

    @Inject(method = "addTransientModifier", at = @At("HEAD"))
    private void debugAddModifier(AttributeModifier modifier, CallbackInfo ci) {
        // 移除了 MAX_HEALTH 的限制，现在监控所有属性
        DamageDebugUtil.logAttributeChangeConsole(
                (AttributeInstance) (Object) this,
                modifier,
                "debug.jujutsu_addon.console.action.add",
                true
        );
    }

    @Inject(method = "removeModifier(Ljava/util/UUID;)V", at = @At("HEAD"))
    private void debugRemoveModifier(UUID uuid, CallbackInfo ci) {
        DamageDebugUtil.logAttributeRemoveConsole(
                (AttributeInstance) (Object) this,
                uuid
        );
    }

    @Inject(method = "removeModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V", at = @At("HEAD"))
    private void debugRemoveModifierObj(AttributeModifier modifier, CallbackInfo ci) {
        DamageDebugUtil.logAttributeChangeConsole(
                (AttributeInstance) (Object) this,
                modifier,
                "debug.jujutsu_addon.console.action.remove",
                false
        );
    }
}
*/
package com.jujutsuaddon.addon.mixin.debug;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

// 这是一个空壳 Mixin，为了优化性能，已禁用所有调试日志
@Mixin(AttributeInstance.class)
public abstract class MixinDebugAttributes {
}
