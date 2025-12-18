package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.world.phys.Vec3;

public class PressureCalculator {

    /**
     * 计算最大影响范围
     */
    public static double calculateRange(int pressureLevel) {
        return PressureConfig.getLevelRange(pressureLevel);
    }

    /**
     * 计算停止距离
     */
    public static double calculateHaltDistance(int pressureLevel) {
        return PressureConfig.getHaltDistance(pressureLevel);
    }

    /**
     * 计算等级因子
     */
    public static double calculateLevelFactor(int pressureLevel) {
        return PressureConfig.calculateLevelFactor(pressureLevel);
    }

    /**
     * 计算压力值
     */
    public static double calculatePressure(int pressureLevel, float cursedEnergyOutput,
                                           double distance3D, double maxRange,
                                           Vec3 ownerMovement, Vec3 direction3D) {

        if (pressureLevel <= 0) return 0;

        double levelFactor = calculateLevelFactor(pressureLevel);
        double haltDistance = calculateHaltDistance(pressureLevel);

        double distanceFromHalt = distance3D - haltDistance;
        double distanceFactor;
        double breachMultiplier = 1.0;

        if (distanceFromHalt <= 0) {
            distanceFactor = 1.0;
            double breachDepth = Math.abs(distanceFromHalt);
            breachMultiplier = 1.0 + breachDepth * PressureConfig.getBreachPressureMult();
        } else {
            double normalizedDist = Math.min(distanceFromHalt / (maxRange - haltDistance), 1.0);
            distanceFactor = Math.exp(-normalizedDist * PressureConfig.getDistanceDecay());
        }

        double approachBonus = calculateApproachBonus(ownerMovement, direction3D);

        return PressureConfig.getBasePressure() * levelFactor * cursedEnergyOutput *
                distanceFactor * approachBonus * breachMultiplier;
    }

    /**
     * 计算阻力强度
     */
    public static double calculateResistanceStrength(int pressureLevel, double distance3D, double maxRange) {
        double haltDistance = calculateHaltDistance(pressureLevel);
        double transitionZone = PressureConfig.getHaltTransitionZone();

        double distanceFromHalt = distance3D - haltDistance;

        if (distanceFromHalt <= 0) {
            return 1.0;
        } else if (distanceFromHalt < transitionZone) {
            double t = 1.0 - (distanceFromHalt / transitionZone);
            return PressureConfig.getApproachResistanceBase() +
                    (PressureConfig.getApproachResistanceMax() - PressureConfig.getApproachResistanceBase()) *
                            Math.pow(t, 0.5);
        } else {
            double normalizedDist = (distanceFromHalt - transitionZone) / (maxRange - haltDistance - transitionZone);
            normalizedDist = Math.max(0, Math.min(1, normalizedDist));
            return PressureConfig.getApproachResistanceBase() * (1.0 - normalizedDist * 0.8);
        }
    }

    /**
     * 计算推力
     */
    public static double calculatePushForce(int pressureLevel, float cursedEnergyOutput,
                                            double distance3D, double maxRange) {

        if (pressureLevel <= 0) return 0;

        double levelFactor = calculateLevelFactor(pressureLevel);
        double haltDistance = calculateHaltDistance(pressureLevel);

        double distanceFromHalt = distance3D - haltDistance;

        if (distanceFromHalt <= 0) {
            double breachDepth = Math.abs(distanceFromHalt);
            double breachForce = PressureConfig.getBreachRepelForce() *
                    (1.0 + breachDepth * 2.0) * levelFactor * cursedEnergyOutput;
            return Math.min(breachForce, PressureConfig.getMaxPushForce() * 1.5);
        }

        double normalizedDist = Math.min(distanceFromHalt / (maxRange - haltDistance), 1.0);
        double distanceFactor = Math.exp(-normalizedDist * PressureConfig.getDistanceDecay());

        double force = PressureConfig.getBasePushForce() * levelFactor * cursedEnergyOutput * distanceFactor;

        return Math.min(force, PressureConfig.getMaxPushForce());
    }

    private static double calculateApproachBonus(Vec3 ownerMovement, Vec3 direction3D) {
        double moveSpeed = ownerMovement.length();
        if (moveSpeed < 0.01) return 1.0D;

        Vec3 moveDir = ownerMovement.normalize();
        double dot = moveDir.dot(direction3D);

        if (dot > 0.1) {
            double speedBonus = Math.min(moveSpeed * 3.0, 1.5);
            return 1.0D + (dot * (PressureConfig.getApproachMultiplier() - 1.0D) * speedBonus);
        }

        return 1.0D;
    }
}
