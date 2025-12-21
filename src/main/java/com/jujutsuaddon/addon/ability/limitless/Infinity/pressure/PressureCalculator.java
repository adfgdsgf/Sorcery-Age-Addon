package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import net.minecraft.world.phys.Vec3;

/**
 * 压力计算器
 *
 * 芝诺模型：平衡点 = 无形的墙
 */
public class PressureCalculator {

    /**
     * 获取最大范围
     */
    public static double calculateRange(int pressureLevel) {
        return PressureConfig.getLevelRange(pressureLevel);
    }

    /**
     * 获取平衡点半径（墙的位置）
     */
    public static double calculateBalanceRadius(int pressureLevel) {
        return PressureConfig.getBalanceRadius(pressureLevel);
    }

    /**
     * 获取等级因子
     */
    public static double calculateLevelFactor(int pressureLevel) {
        return PressureConfig.calculateLevelFactor(pressureLevel);
    }

    /**
     * ★★★ 压力计算 - 两墙夹击原理 ★★★
     *
     * 压力产生条件：
     * 1. 有碰撞（实体墙）
     * 2. 距离 <= 平衡点半径（无限墙）
     *
     * 压力大小 = 距离越近越大
     */
    public static double calculatePressure(int pressureLevel, float cursedEnergyOutput,
                                           double distance3D, double maxRange,
                                           Vec3 ownerMovement, Vec3 direction3D) {
        // 没有碰撞信息时，默认无压力
        return 0;
    }

    public static double calculatePressure(int pressureLevel, float cursedEnergyOutput,
                                           double distance3D, double maxRange,
                                           Vec3 ownerMovement, Vec3 direction3D,
                                           boolean isColliding, float blockHardness) {

        if (pressureLevel <= 0) return 0;

        // ★★★ 条件1：必须有碰撞（实体墙）★★★
        if (!isColliding) {
            return 0;
        }

        // 获取平衡点半径
        double balanceRadius = BalancePointCalculator.getBalanceRadius(pressureLevel, maxRange);

        // ★★★ 条件2：必须在平衡点内（碰到无限墙）★★★
        if (distance3D > balanceRadius) {
            return 0;
        }

        // ==================== 两墙夹住，计算压力 ====================

        double levelFactor = calculateLevelFactor(pressureLevel);

        // ★★★ 核心：压迫因子 = 距离越近越大 ★★★
        // 距离 = balanceRadius → 因子 = 0（刚接触）
        // 距离 = 0 → 因子 = 1（贴脸）
        double compressionFactor = (balanceRadius - distance3D) / balanceRadius;
        compressionFactor = Math.max(0, Math.min(1.0, compressionFactor));

        // 基础压力
        double basePressure = PressureConfig.getBasePressure();

        // 方块硬度加成（被压在更硬的墙上更疼）
        double hardnessMult = PressureConfig.getHardnessPressureMult(blockHardness);

        // ==================== 总压力 ====================
        double pressure = basePressure * levelFactor * cursedEnergyOutput
                * compressionFactor * hardnessMult;

        return pressure;
    }
}
