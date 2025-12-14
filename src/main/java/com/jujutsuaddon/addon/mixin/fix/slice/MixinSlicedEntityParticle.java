package com.jujutsuaddon.addon.mixin.fix.slice;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import radon.jujutsu_kaisen.client.particle.SlicedEntityParticle;
import radon.jujutsu_kaisen.client.slice.Collider;
import radon.jujutsu_kaisen.client.slice.GJK;
import radon.jujutsu_kaisen.client.slice.RigidBody;

/**
 * 优化 generateChunks 中的 GJK 碰撞检测
 *
 * 原代码对每对模型数据执行 GJK 碰撞检测来判断是否属于同一 chunk
 * 这是 O(n²) 复杂度且 GJK 本身也很慢
 *
 * 优化：跳过 GJK 检测，只用 AABB 检测
 */
@Mixin(value = SlicedEntityParticle.class, remap = false)
public class MixinSlicedEntityParticle {

    /**
     * 跳过 GJK.collidesAny，只依赖 AABB 检测
     * 这会让 chunk 分组稍微不精确，但大幅提升性能
     */
    @Redirect(
            method = "generateChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lradon/jujutsu_kaisen/client/slice/GJK;collidesAny(Lradon/jujutsu_kaisen/client/slice/RigidBody;Lradon/jujutsu_kaisen/client/slice/RigidBody;Lradon/jujutsu_kaisen/client/slice/Collider;Lradon/jujutsu_kaisen/client/slice/Collider;)Z"
            ),
            remap = false
    )
    private static boolean jujutsuAddon$skipGJK(RigidBody bodyA, RigidBody bodyB, Collider a, Collider b) {
        // 直接返回 true，让 AABB 检测决定是否合并
        // 原代码：if (AABB.intersects() && GJK.collidesAny())
        // 现在：if (AABB.intersects() && true) = if (AABB.intersects())
        return true;
    }
}
