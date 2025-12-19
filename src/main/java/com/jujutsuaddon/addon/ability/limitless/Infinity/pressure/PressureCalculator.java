package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
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
     */
    public static double calculatePressure(int pressureLevel, float cursedEnergyOutput,
                                           double distance3D, double maxRange,
                                           Vec3 ownerMovement, Vec3 direction3D) {
        return calculatePressure(pressureLevel, cursedEnergyOutput, distance3D, maxRange,
                ownerMovement, direction3D, false, 1.0f);
    }

    /**
     * ★★★ 计算压力值（含碰撞和方块硬度）★★★
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
            collisionMult = 3.0;
            hardnessMult = PressureConfig.getHardnessPressureMult(blockHardness);
        }

        double pressure;

        if (distanceFromPush <= 0) {
            // ==================== ★★★ 推力区（核心修改）★★★ ====================

            // ★★★ 1. 深度归一化：0（推力区边界）→ 1（玩家位置/脸贴脸）★★★
            double breachDepth = (pushRadius - distance3D) / pushRadius;
            breachDepth = Math.max(0, Math.min(1, breachDepth));

            // ★★★ 2. 深度曲线：pow(0.6) 让边界处压力上升更快，深处增长放缓 ★★★
            // breachDepth=0.3 → depthCurve≈0.47
            // breachDepth=0.5 → depthCurve≈0.66
            // breachDepth=1.0 → depthCurve=1.0
            double depthCurve = Math.pow(breachDepth, 0.6);

            // ★★★ 3. 等级平方：让低等级和高等级差距更大 ★★★
            // 等级1: levelFactor=0.1 → levelSquared=0.01 (极弱)
            // 等级5: levelFactor=0.5 → levelSquared=0.25 (中等)
            // 等级10: levelFactor=1.0 → levelSquared=1.0 (最强)
            double levelSquared = levelFactor * levelFactor;

            // ★★★ 4. 使用 breachPressureMult 作为推力区基础倍率 ★★★
            double breachBaseMult = PressureConfig.getBreachPressureMult();  // 默认 3.0

            // ★★★ 5. 计算基础压力 ★★★
            // 公式: basePressure × breachMult × 深度曲线 × 等级² × 咒力输出
            double basePressure = PressureConfig.getBasePressure()
                    * breachBaseMult
                    * depthCurve
                    * levelSquared
                    * cursedEnergyOutput;

            // ★★★ 6. 推力区自身压力（玩家不动也有）★★★
            // 这是被动压力，只要在推力区内就有
            double pushPressure = basePressure * 0.6;

            // ★★★ 7. 玩家移动额外压力 ★★★
            double approachPressure = basePressure * approachFactor
                    * PressureConfig.getApproachMultiplier();

            pressure = pushPressure + approachPressure;

        } else {
            // ==================== 停止区 ====================
            double t = distanceFromPush / stopZoneWidth;
            double distanceFactor = 1.0 - t * 0.85;

            double basePressure = PressureConfig.getBasePressure() * levelFactor
                    * cursedEnergyOutput * distanceFactor;

            // 停止区边缘有微小压力
            double pushPressure = 0;
            if (t < 0.3) {
                pushPressure = basePressure * 0.1 * (1.0 - t / 0.3);
            }

            // 玩家移动产生的压力
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
