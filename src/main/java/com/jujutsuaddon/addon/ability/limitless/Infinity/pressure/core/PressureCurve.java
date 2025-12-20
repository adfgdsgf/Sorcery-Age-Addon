package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.damage.PressureDamageConfig;

public class PressureCurve {

    // ==================== 常量 ====================

    /** 伤害间隔计算用的压力上限 */
    private static final double MAX_PRESSURE_FOR_INTERVAL = 20000.0;

    /** 伤害间隔计算用的压力下限 */
    private static final double MIN_PRESSURE_FOR_INTERVAL = 3.0;

    // ==================== 压力计算 ====================

    public static double calculateCollisionPressure(double distance, double balanceRadius, double basePressure) {
        double minDist = PressureDamageConfig.getCollisionMinDistance();
        distance = Math.max(distance, minDist);
        if (distance >= balanceRadius) {
            return basePressure;
        }
        double multiplier = getPressureMultiplier(distance, balanceRadius);
        return basePressure * multiplier;
    }

    public static double getPressureMultiplier(double distance, double balanceRadius) {
        double minDist = PressureDamageConfig.getCollisionMinDistance();
        distance = Math.max(distance, minDist);

        // 超出平衡半径，返回基础倍率
        if (distance >= balanceRadius) {
            return 1.0;
        }
        double maxMult = PressureDamageConfig.getMaxPressureMultiplier();

        // 反比例 + 归一化到 [0, 1] 范围
        // ratio: 距离越近越大
        double ratio = minDist / distance;
        double maxRatio = minDist / balanceRadius;

        // 归一化：distance=minDist时为1，distance=balanceRadius时为0
        double normalized = (ratio - maxRatio) / (1.0 - maxRatio);
        normalized = Math.max(0, Math.min(1, normalized));

        // 映射到 [1, maxMult]
        return 1.0 + (maxMult - 1.0) * normalized;
    }

    // ==================== 伤害间隔计算（统一入口）====================

    /**
     * 根据压力计算伤害间隔（tick）
     * 压力越高，间隔越短
     *
     * @param pressure 当前压力值
     * @return 伤害间隔（tick）
     */
    public static int calculateDamageInterval(double pressure) {
        int minInterval = PressureDamageConfig.getMinDamageInterval();  // 2
        int maxInterval = PressureDamageConfig.getMaxDamageInterval();  // 15
        if (pressure <= 0.1) {
            return maxInterval;
        }
        // ★★★ 反比例衰减：压力越高，间隔越短 ★★★
        // 公式：interval = maxInterval / (1 + pressure / scale)
        double scale = 20.0;  // 衰减速度控制
        double rawInterval = maxInterval / (1.0 + pressure / scale);

        int interval = (int) Math.round(rawInterval);
        return Math.max(minInterval, Math.min(maxInterval, interval));
    }

    // ==================== 伤害倍率计算 ====================

    /**
     * 根据压力计算伤害倍率
     */
    public static double calculateDamageMultiplier(double pressure, double basePressure) {
        double baseMult = 1.0;
        double maxMult = 3.0;
        if (pressure <= basePressure) {
            return baseMult;
        }
        double pressureRatio = pressure / basePressure;
        double multiplier = baseMult + Math.log(pressureRatio) * 1.5;
        return Math.min(maxMult, multiplier);
    }

    // ==================== 调试信息 ====================

    public static String describePressureState(double distance, double balanceRadius, double pressure) {
        double multiplier = getPressureMultiplier(distance, balanceRadius);
        String intensity;
        if (multiplier < 2.0) {
            intensity = "Light";
        } else if (multiplier < 5.0) {
            intensity = "Medium";
        } else if (multiplier < 10.0) {
            intensity = "Heavy";
        } else {
            intensity = "Extreme";
        }
        return String.format("%s (x%.1f)", intensity, multiplier);
    }
}
