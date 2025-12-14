/*
package com.jujutsuaddon.addon.mixin.fix.slice;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.entity.projectile.DismantleProjectile;

import java.util.HashSet;
import java.util.Set;

*/
/**
 * 防止同一实体被重复处理
 *//*

@Mixin(value = DismantleProjectile.class, remap = false)
public class MixinDismantleProjectile {

    @Unique
    private final Set<Integer> jujutsuAddon$processedEntities = new HashSet<>();

    */
/**
     * 使用完整方法签名来匹配 onHitEntity
     *//*

    @Inject(
            method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void jujutsuAddon$skipDuplicateHits(EntityHitResult result, CallbackInfo ci) {
        int entityId = result.getEntity().getId();

        if (this.jujutsuAddon$processedEntities.contains(entityId)) {
            ci.cancel();
            return;
        }
        this.jujutsuAddon$processedEntities.add(entityId);
    }
}
*/
