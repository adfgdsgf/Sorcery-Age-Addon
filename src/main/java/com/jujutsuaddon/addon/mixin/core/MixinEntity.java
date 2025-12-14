package com.jujutsuaddon.addon.mixin.core;

import com.jujutsuaddon.addon.util.helper.ProjectileHitTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

    /**
     * 实体被移除时，如果是投射物则清理命中记录
     * 防止内存泄漏
     */
    @Inject(method = "remove", at = @At("HEAD"))
    private void jujutsuAddon$cleanupProjectileHits(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 只对投射物进行处理
        if (self instanceof Projectile) {
            ProjectileHitTracker.clearProjectile(self.getUUID());
        }
    }
}
