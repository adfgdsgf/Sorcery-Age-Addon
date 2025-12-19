package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.world.phys.Vec3;

public class PressureCalculator {

    public static double calculateRange(int pressureLevel) {
        return PressureConfig.getLevelRange(pressureLevel);
    }

    public static double calculateHaltDistance(int pressureLevel) {
        return PressureConfig.getPushZoneRadius(pressureLevel);
    }

    public static double calculateStopZoneRadius(int pressureLevel) {
        return PressureConfig.getStopZoneRadius(pressureLevel);
    }

    public static double calculateLevelFactor(int pressureLevel) {
        return PressureConfig.calculateLevelFactor(pressureLevel);
    }

    private static double smoothstep(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * ★★★ 计算压力值（不含方块硬度）★★★
     * 用于一般计算，方块硬度在碰撞时额外乘
     */
    public static double calculatePressure(int pressureLevel, float cursedEnergyOutput,
                                           double distance3D, double maxRange,
                                           Vec3 ownerMovement, Vec3 direction3D) {
        return calculatePressure(pressureLevel, cursedEnergyOutput, distance3D, maxRange,
                ownerMovement, direction3D, false, 1.0f);
    }

    /**
     * ★★★ 计算压力值（含碰撞和方块硬度）★★★
     *
     * @param isColliding 目标是否正在碰撞方块
     * @param blockHardness 碰撞方块的硬度（只在 isColliding=true 时有效）
     */
    public static double calculatePressure(int pressureLevel, float cursedEnergyOutput,
                                           double distance3D, double maxRange,
                                           Vec3 ownerMovement, Vec3 direction3D,
                                           boolean isColliding, float blockHardness) {

        if (pressureLevel <= 0) return 0;

        double levelFactor = calculateLevelFactor(pressureLevel);
        double pushRadius = PressureConfig.getPushZoneRadius(pressureLevel);
        double stopRadius = PressureConfig.getStopZoneRadius(pressureLevel);

        // ★★★ 减速区：没有压力 ★★★
        if (distance3D > stopRadius) {
            return 0;
        }

        double stopZoneWidth = stopRadius - pushRadius;
        double distanceFromPush = distance3D - pushRadius;

        // 计算 owner 的接近速度
        double approachFactor = calculateApproachFactor(ownerMovement, direction3D);

        // ★★★ 碰撞倍率：被挤在墙上时压力更大 ★★★
        double collisionMult = 1.0;
        double hardnessMult = 1.0;
        if (isColliding) {
            collisionMult = 3.0;  // 碰撞时压力×3
            hardnessMult = PressureConfig.getHardnessPressureMult(blockHardness);
        }

        double pressure;

        if (distanceFromPush <= 0) {
            // ==================== 推力区 ====================
            double breachDepth = Math.abs(distanceFromPush);

            // 深度因子：越深入压力越大
            double depthFactor = 1.0 + breachDepth * 0.5;
            double breachMult = 1.0 + breachDepth * PressureConfig.getBreachPressureMult();

            double basePressure = PressureConfig.getBasePressure() * levelFactor
                    * cursedEnergyOutput * depthFactor * breachMult;

            // ★★★ 修复：提高基础压力比例 ★★★
            // 推力自身产生的压力（即使 owner 不动也有）
            double pushPressure = basePressure * 0.5;  // 从 0.15 改成 0.5

            // owner 接近产生的额外压力
            double approachPressure = basePressure * approachFactor * PressureConfig.getApproachMultiplier();

            pressure = pushPressure + approachPressure;

        } else {
            // ==================== 静止区 ====================
            double t = distanceFromPush / stopZoneWidth;
            double distanceFactor = 1.0 - t * 0.85;

            double basePressure = PressureConfig.getBasePressure() * levelFactor
                    * cursedEnergyOutput * distanceFactor;

            // 推力在静止区产生的微小压力
            double pushPressure = 0;
            if (t < 0.3) {
                pushPressure = basePressure * 0.1 * (1.0 - t / 0.3);
            }

            // owner 接近产生的压力
            double approachPressure = 0;
            if (approachFactor > 0.05) {
                approachPressure = basePressure * approachFactor * PressureConfig.getApproachMultiplier();
            }

            pressure = pushPressure + approachPressure;
        }

        // ★★★ 应用碰撞和硬度倍率 ★★★
        return pressure * collisionMult * hardnessMult;
    }

    /**
     * 计算 owner 接近因子
     */
    private static double calculateApproachFactor(Vec3 ownerMovement, Vec3 directionToTarget) {
        double moveSpeed = ownerMovement.length();

        if (moveSpeed < 0.01) {
            return 0.0;
        }

        Vec3 moveDir = ownerMovement.normalize();
        double dot = moveDir.dot(directionToTarget);

        if (dot <= 0.1) {
            return 0.0;
        }

        double speedFactor = Math.min(moveSpeed * 5.0, 2.0);
        return dot * speedFactor;
    }

    /**
     * ★★★ 阻力强度（被动防御，始终生效）★★★
     */
    public static double calculateResistanceStrength(int pressureLevel, double distance3D, double maxRange) {
        double pushRadius = PressureConfig.getPushZoneRadius(pressureLevel);
        double stopRadius = PressureConfig.getStopZoneRadius(pressureLevel);

        double distanceFromPush = distance3D - pushRadius;

        if (distanceFromPush <= 0) {
            return 1.0;
        }

        double stopZoneWidth = stopRadius - pushRadius;
        if (distanceFromPush < stopZoneWidth && stopZoneWidth > 0) {
            double t = 1.0 - (distanceFromPush / stopZoneWidth);
            return PressureConfig.getApproachResistanceBase() +
                    (PressureConfig.getApproachResistanceMax() - PressureConfig.getApproachResistanceBase()) *
                            Math.pow(t, 0.5);
        }

        double slowdownStart = stopRadius;
        double slowdownRange = maxRange - slowdownStart;
        if (slowdownRange <= 0) return PressureConfig.getApproachResistanceBase();

        double normalizedDist = (distance3D - slowdownStart) / slowdownRange;
        normalizedDist = Math.max(0, Math.min(1, normalizedDist));
        return PressureConfig.getApproachResistanceBase() * (1.0 - normalizedDist * 0.8);
    }

    /**
     * ★★★ 推力（被动防御，始终生效）★★★
     */
    public static double calculatePushForce(int pressureLevel, float cursedEnergyOutput,
                                            double distance3D, double maxRange) {

        if (pressureLevel <= 0) return 0;

        double levelFactor = calculateLevelFactor(pressureLevel);
        double pushRadius = PressureConfig.getPushZoneRadius(pressureLevel);
        double stopRadius = PressureConfig.getStopZoneRadius(pressureLevel);

        if (distance3D > stopRadius) {
            return 0;
        }

        double distanceFromPush = distance3D - pushRadius;

        if (distanceFromPush <= 0) {
            double breachDepth = Math.abs(distanceFromPush);
            double breachForce = PressureConfig.getBreachRepelForce() *
                    (1.0 + breachDepth * 2.0) * levelFactor * cursedEnergyOutput;
            return Math.min(breachForce, PressureConfig.getMaxPushForce() * 1.5);
        }

        double stopZoneWidth = stopRadius - pushRadius;
        double t = distanceFromPush / stopZoneWidth;
        double distanceFactor = 1.0 - smoothstep(t) * 0.9;

        double force = PressureConfig.getBasePushForce() * levelFactor * cursedEnergyOutput * distanceFactor;
        return Math.min(force, PressureConfig.getMaxPushForce());
    }
}
