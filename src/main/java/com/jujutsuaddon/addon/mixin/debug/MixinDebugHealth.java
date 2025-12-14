/*
package com.jujutsuaddon.addon.mixin.debug;

import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

@Mixin(LivingEntity.class)
public class MixinDebugHealth {

    @Inject(method = "setHealth", at = @At("HEAD"))
    private void debugSetHealth(float health, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 只监控式神
        if (entity instanceof SummonEntity) {
            DamageDebugUtil.logHealthChangeConsole(entity, entity.getHealth(), health);
        }
    }
}
*/
package com.jujutsuaddon.addon.mixin.debug;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.LivingEntity;

// 这是一个空壳 Mixin，为了优化性能，已禁用所有调试日志
@Mixin(LivingEntity.class)
public class MixinDebugHealth {
}

