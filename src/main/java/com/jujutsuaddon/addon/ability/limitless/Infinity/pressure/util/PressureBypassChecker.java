// src/main/java/com/jujutsuaddon/addon/ability/limitless/Infinity/pressure/util/PressureBypassChecker.java
package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import radon.jujutsu_kaisen.item.JJKItems;

/**
 * 压力系统豁免检测器
 */
public final class PressureBypassChecker {

    private PressureBypassChecker() {}

    /**
     * 检查实体是否持有天逆鉾（主手或副手）
     */
    public static boolean isHoldingInvertedSpear(LivingEntity entity) {
        if (entity == null) return false;

        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offHand = entity.getOffhandItem();

        return isInvertedSpear(mainHand) || isInvertedSpear(offHand);
    }

    private static boolean isInvertedSpear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try {
            return stack.is(JJKItems.INVERTED_SPEAR_OF_HEAVEN.get());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查实体是否应该绕过压力系统
     * 可在此扩展其他豁免条件
     */
    public static boolean shouldBypassPressure(LivingEntity entity) {
        if (entity == null) return false;

        // 1. 天逆鉾
        if (isHoldingInvertedSpear(entity)) {
            return true;
        }

        // 2. 未来可扩展其他豁免条件
        // if (hasOtherBypassCondition(entity)) return true;

        return false;
    }
}
