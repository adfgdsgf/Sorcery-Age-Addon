package com.jujutsuaddon.addon.damage.result;

import com.jujutsuaddon.addon.damage.data.DamageContext.AbilityType;

/**
 * 伤害预测结果
 *
 * 不可变类，包含所有预测信息
 */
public record DamagePrediction(
        AbilityType type,
        float vanillaDamage,    // 原版伤害（技能基础值）
        float addonDamage,      // Addon 计算后的伤害
        float critDamage,       // 暴击伤害
        boolean isMelee         // 是否由近战触发
) {

    // ==================== 工厂方法 ====================

    public static DamagePrediction unknown() {
        return new DamagePrediction(AbilityType.UTILITY, -1, -1, -1, false);
    }

    public static DamagePrediction utility() {
        return new DamagePrediction(AbilityType.UTILITY, 0, 0, 0, false);
    }

    public static DamagePrediction summon() {
        return new DamagePrediction(AbilityType.SUMMON, -1, -1, -1, false);
    }

    // ==================== 查询方法 ====================

    public boolean canPredict() {
        return vanillaDamage >= 0;
    }

    public boolean hasAddonModification() {
        if (!canPredict() || vanillaDamage <= 0) return false;
        return Math.abs(addonDamage - vanillaDamage) > vanillaDamage * 0.01f;
    }

    public float getDisplayDamage() {
        return canPredict() ? addonDamage : vanillaDamage;
    }

    public DamageChange getDamageChange() {
        if (!canPredict() || vanillaDamage <= 0) return DamageChange.NONE;
        if (addonDamage > vanillaDamage * 1.01f) return DamageChange.INCREASED;
        if (addonDamage < vanillaDamage * 0.99f) return DamageChange.DECREASED;
        return DamageChange.NONE;
    }

    // ==================== 格式化 ====================

    public String formatDamage(float damage) {
        if (damage < 0) return "?";
        if (damage == 0) return "0";

        String baseStr;
        if (damage >= 10000) baseStr = String.format("%.1fW", damage / 10000);
        else if (damage >= 1000) baseStr = String.format("%.1fK", damage / 1000);
        else if (damage >= 100) baseStr = String.format("%.0f", damage);
        else baseStr = String.format("%.1f", damage);

        if (canPredict() && critDamage > damage * 1.1f) {
            String critStr;
            if (critDamage >= 10000) critStr = String.format("%.1fW", critDamage / 10000);
            else if (critDamage >= 1000) critStr = String.format("%.1fK", critDamage / 1000);
            else critStr = String.format("%.0f", critDamage);
            return baseStr + " §7(§c" + critStr + "§7)";
        }
        return baseStr;
    }

    public String formatWithChange() {
        if (!canPredict()) return "?";

        String damageStr = formatDamage(addonDamage);
        return switch (getDamageChange()) {
            case INCREASED -> "§a↑§r " + damageStr;
            case DECREASED -> "§c↓§r " + damageStr;
            case NONE -> damageStr;
        };
    }

    // ==================== 枚举 ====================

    public enum DamageChange {
        INCREASED, DECREASED, NONE
    }
}
