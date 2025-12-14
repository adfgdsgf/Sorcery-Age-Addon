package com.jujutsuaddon.addon.util.calc;

import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Ability.IDurationable;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * 技能消耗计算器 - 修复版 v2
 */
public class CostCalculator {

    private static final float DEFAULT_COOLDOWN_TICKS = 20f;

    // =========================================================
    // 理论消耗（用于平衡器计算）
    // =========================================================

    /**
     * 获取技能的理论消耗（不受六眼、熟练度等因素影响）
     *
     * ★ 平衡器应该使用这个方法，确保公平比较 ★
     */
    public static float getTheoreticalCost(Ability ability) {
        if (ability == null) return 0f;
        try {
            // 始终使用 getCost(null) 获取基础理论消耗
            return ability.getCost(null);
        } catch (Exception e) {
            return 0f;
        }
    }

    /**
     * 获取技能的理论冷却时间（tick）
     */
    public static float getTheoreticalCooldownTicks(Ability ability) {
        if (ability == null) return DEFAULT_COOLDOWN_TICKS;

        // 优先使用反射获取基础冷却（不受玩家影响）
        try {
            Method method = Ability.class.getDeclaredMethod("getCooldown");
            method.setAccessible(true);
            int cd = (int) method.invoke(ability);
            if (cd > 0) return cd;
        } catch (Exception ignored) {}

        // 如果反射失败，尝试用 null
        try {
            int cd = ability.getRealCooldown(null);
            if (cd > 0) return cd;
        } catch (Exception ignored) {}

        return DEFAULT_COOLDOWN_TICKS;
    }

    // =========================================================
    // 实际消耗（用于其他场景）
    // =========================================================

    /**
     * 获取技能的实际消耗（受六眼、熟练度等因素影响）
     */
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

    /**
     * 获取技能的冷却时间（tick）
     */
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

    /**
     * 获取技能的冷却时间（秒）
     */
    public static float getCooldownSeconds(Ability ability, @Nullable LivingEntity owner) {
        return getCooldownTicks(ability, owner) / 20f;
    }

    /**
     * 获取技能的理论冷却时间（秒）
     */
    public static float getTheoreticalCooldownSeconds(Ability ability) {
        return getTheoreticalCooldownTicks(ability) / 20f;
    }

    /**
     * 获取技能的持续时间（用于引导技能）
     */
    public static int getDuration(Ability ability) {
        if (ability instanceof IDurationable) {
            return ((IDurationable) ability).getDuration();
        }
        return 0;
    }

    /**
     * 获取单次使用的总消耗（引导技能 = cost × duration）
     */
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
