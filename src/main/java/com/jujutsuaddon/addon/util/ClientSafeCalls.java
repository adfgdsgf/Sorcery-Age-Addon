package com.jujutsuaddon.addon.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * 绝对安全的客户端调用工具类。
 * 外部类没有任何客户端引用，服务器可以安全加载。
 */
public class ClientSafeCalls {

    public static float getOutputBonus(LivingEntity owner) {
        // 如果是服务端，DistExecutor 直接返回 null，不会去加载 Inner 类
        // 如果是客户端，才会加载 Inner 类并执行逻辑
        Float result = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Inner.getOutputBonus(owner));
        return result != null ? result : 0f;
    }

    public static float getEnergyBonus(LivingEntity owner) {
        Float result = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Inner.getEnergyBonus(owner));
        return result != null ? result : 0f;
    }

    // ★★★ 隔离区 ★★★
    // 这个内部类只有在客户端运行时才会被 JVM 加载。
    // 服务器永远不会加载它，所以这里面的 import 和引用不会导致服务器崩溃。
    private static class Inner {
        public static float getOutputBonus(LivingEntity owner) {
            // 使用全限定名，避免在外部类 import
            if (owner == net.minecraft.client.Minecraft.getInstance().player) {
                return com.jujutsuaddon.addon.client.cache.ClientVowDataCache.calculateTotalOutputBonus();
            }
            return 0f;
        }

        public static float getEnergyBonus(LivingEntity owner) {
            if (owner == net.minecraft.client.Minecraft.getInstance().player) {
                return com.jujutsuaddon.addon.client.cache.ClientVowDataCache.calculateTotalEnergyBonus();
            }
            return 0f;
        }
    }
}
