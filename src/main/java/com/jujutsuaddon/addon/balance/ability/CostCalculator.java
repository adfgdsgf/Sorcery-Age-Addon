// 文件路径: src/main/java/com/jujutsuaddon/addon/balance/ability/CostCalculator.java
package com.jujutsuaddon.addon.balance.ability;

import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Ability.IDurationable;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * 技能消耗计算器
 */
public class CostCalculator {

    private static final float DEFAULT_COOLDOWN_TICKS = 20f;

    public static float getTheoreticalCost(Ability ability) {
        if (ability == null) return 0f;
        try {
            return ability.getCost(null);
        } catch (Exception e) {
            return 0f;
        }
    }

    public static float getTheoreticalCooldownTicks(Ability ability) {
        if (ability == null) return DEFAULT_COOLDOWN_TICKS;

        try {
            Method method = Ability.class.getDeclaredMethod("getCooldown");
            method.setAccessible(true);
            int cd = (int) method.invoke(ability);
            if (cd > 0) return cd;
        } catch (Exception ignored) {}

        try {
            int cd = ability.getRealCooldown(null);
            if (cd > 0) return cd;
        } catch (Exception ignored) {}

        return DEFAULT_COOLDOWN_TICKS;
    }

    public static float getRawCost(Ability ability, @Nullable LivingEntity owner) {
        if (ability == null) return 0f;
        try {
            if (owner != null) {
                return ability.getRealCost(owner);
            }
            return ability.getCost(null);
        } catch (Exception e) {
            try {
                return ability.getCost(null);
            } catch (Exception ignored) {
                return 0f;
            }
        }
    }

    public static float getCooldownTicks(Ability ability, @Nullable LivingEntity owner) {
        if (ability == null) return DEFAULT_COOLDOWN_TICKS;
        try {
            if (owner != null) {
                int cd = ability.getRealCooldown(owner);
                if (cd > 0) return cd;
            }
        } catch (Exception ignored) {}

        try {
            Method method = Ability.class.getDeclaredMethod("getCooldown");
            method.setAccessible(true);
            int cd = (int) method.invoke(ability);
            if (cd > 0) return cd;
        } catch (Exception ignored) {}

        return DEFAULT_COOLDOWN_TICKS;
    }

    public static float getCooldownSeconds(Ability ability, @Nullable LivingEntity owner) {
        return getCooldownTicks(ability, owner) / 20f;
    }

    public static float getTheoreticalCooldownSeconds(Ability ability) {
        return getTheoreticalCooldownTicks(ability) / 20f;
    }

    public static int getDuration(Ability ability) {
        if (ability instanceof IDurationable) {
            return ((IDurationable) ability).getDuration();
        }
        return 0;
    }

    public static float getTotalCostPerUse(Ability ability, @Nullable LivingEntity owner) {
        if (ability == null) return 0f;
        float cost = getRawCost(ability, owner);
        if (cost <= 0) return 0f;

        if (ability instanceof Ability.IChannelened && ability instanceof IDurationable) {
            int duration = getDuration(ability);
            if (duration > 0) {
                return cost * duration;
            }
        }
        return cost;
    }
}
