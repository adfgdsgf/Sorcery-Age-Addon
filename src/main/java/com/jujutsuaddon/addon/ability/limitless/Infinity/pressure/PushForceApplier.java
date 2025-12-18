package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class PushForceApplier {

    public static CollisionInfo apply(LivingEntity owner, LivingEntity target,
                                      int pressureLevel, float cursedEnergyOutput,
                                      double maxRange,
                                      PressureStateManager stateManager) {

        CollisionInfo info = new CollisionInfo();

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2, 0);

        double distance3D = ownerCenter.distanceTo(targetCenter);
        if (distance3D < 0.1) distance3D = 0.1;

        Vec3 forceDirection = targetCenter.subtract(ownerCenter).normalize();

        double haltDistance = PressureCalculator.calculateHaltDistance(pressureLevel);
        double distanceFromHalt = distance3D - haltDistance;

        info.haltDistance = haltDistance;
        info.distanceFromHalt = distanceFromHalt;
        info.isBreaching = distanceFromHalt < 0;

        Vec3 ownerMovement = stateManager.calculateOwnerMovement(owner);
        info.pressureValue = PressureCalculator.calculatePressure(
                pressureLevel, cursedEnergyOutput, distance3D, maxRange,
                ownerMovement, forceDirection);

        if (info.pressureValue < PressureConfig.getMinPressureForPush()) {
            info.isColliding = false;
            info.forceDirection = forceDirection;
            info.distance = distance3D;
            return info;
        }

        info.isColliding = CollisionHandler.isCollidingInForceDirection(target, forceDirection);

        // ★★★ 使用通用速度控制器进行预处理 ★★★
        VelocityController.VelocityResult velocityResult = VelocityController.processEntityVelocity(
                target, ownerCenter, pressureLevel, cursedEnergyOutput, maxRange);

        Vec3 currentVelocity = velocityResult.processedVelocity;  // 使用限制后的速度
        double resistanceStrength = velocityResult.resistanceStrength;
        info.resistanceStrength = resistanceStrength;

        Vec3 horizontalForce = new Vec3(forceDirection.x, 0, forceDirection.z);
        double hForceLen = horizontalForce.length();
        if (hForceLen > 0.01) {
            horizontalForce = horizontalForce.normalize();
        }

        double newVelX = currentVelocity.x;
        double newVelY = currentVelocity.y;
        double newVelZ = currentVelocity.z;

        if (target.hurtTime > 0 && newVelY > 0.1) {
            newVelY *= 0.3;
        }

        if (distanceFromHalt < 0) {
            // ==================== 突破停止边界 ====================
            double breachDepth = Math.abs(distanceFromHalt);

            double breachForce = PressureConfig.getBreachRepelForce() *
                    (1.0 + breachDepth * 2.0) *
                    PressureCalculator.calculateLevelFactor(pressureLevel) *
                    cursedEnergyOutput;
            breachForce = Math.min(breachForce, PressureConfig.getMaxPushForce() * 2.0);

            newVelX += horizontalForce.x * breachForce;
            newVelZ += horizontalForce.z * breachForce;

            if (Math.abs(forceDirection.y) > 0.2) {
                newVelY += forceDirection.y * breachForce * 0.6;
            } else {
                newVelY += 0.02 * breachForce;
            }

            // ★★★ 强制抵消所有接近速度 ★★★
            double approachVel = -(currentVelocity.x * horizontalForce.x +
                    currentVelocity.z * horizontalForce.z);
            if (approachVel > 0) {
                newVelX += horizontalForce.x * approachVel;  // 完全抵消
                newVelZ += horizontalForce.z * approachVel;
            }

            if (info.isColliding) {
                stateManager.incrementPinnedTicks(target.getUUID());
                int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());

                double pinForce = PressureConfig.getPinForce() * (1.0 + pinnedTicks * 0.1);
                newVelX += horizontalForce.x * pinForce;
                newVelZ += horizontalForce.z * pinForce;

                Vec3 velocity = new Vec3(newVelX, newVelY, newVelZ);
                Vec3 lateralVel = velocity.subtract(forceDirection.scale(velocity.dot(forceDirection)));
                Vec3 horizontalLateral = new Vec3(lateralVel.x, 0, lateralVel.z);

                if (horizontalLateral.length() > 0.01) {
                    double lateralResist = 0.8 + Math.min(pinnedTicks * 0.02, 0.15);
                    newVelX -= horizontalLateral.x * lateralResist;
                    newVelZ -= horizontalLateral.z * lateralResist;
                }
            } else {
                stateManager.resetPinnedTicks(target.getUUID());
            }

        } else if (info.isColliding) {
            // ==================== 碰撞但未突破边界 ====================
            stateManager.incrementPinnedTicks(target.getUUID());
            int pinnedTicks = stateManager.getPinnedTicks(target.getUUID());

            double pushForce = PressureCalculator.calculatePushForce(
                    pressureLevel, cursedEnergyOutput, distance3D, maxRange);

            double collisionMult = 1.3 + Math.min(pinnedTicks * 0.05, 0.7);
            double crushForce = pushForce * collisionMult;

            newVelX += horizontalForce.x * crushForce;
            newVelZ += horizontalForce.z * crushForce;

            if (forceDirection.y < -0.3) {
                newVelY += forceDirection.y * crushForce * 0.8;
            } else if (forceDirection.y > 0.3) {
                newVelY += forceDirection.y * crushForce * 0.3;
            }

            Vec3 velocity = new Vec3(newVelX, newVelY, newVelZ);
            double escapeSpeed = -velocity.dot(forceDirection);

            if (escapeSpeed > 0) {
                double resistForce = escapeSpeed * PressureConfig.getEscapeResistance();
                newVelX += forceDirection.x * resistForce;
                newVelZ += forceDirection.z * resistForce;
            }

            Vec3 lateralVel = velocity.subtract(forceDirection.scale(velocity.dot(forceDirection)));
            Vec3 horizontalLateral = new Vec3(lateralVel.x, 0, lateralVel.z);

            if (horizontalLateral.length() > 0.01) {
                double lateralResist = PressureConfig.getLateralResistance();
                if (pinnedTicks > 3) {
                    lateralResist = Math.min(0.9, lateralResist + pinnedTicks * 0.02);
                }
                newVelX -= horizontalLateral.x * lateralResist;
                newVelZ -= horizontalLateral.z * lateralResist;
            }

            if (newVelY > 0.05) {
                newVelY *= (1.0 - resistanceStrength * 0.5);
            }

        } else {
            // ==================== 未碰撞、未突破 ====================
            stateManager.resetPinnedTicks(target.getUUID());

            double pushForce = PressureCalculator.calculatePushForce(
                    pressureLevel, cursedEnergyOutput, distance3D, maxRange);

            if (distanceFromHalt < PressureConfig.getHaltTransitionZone()) {
                // 接近停止边界，增强推力
                double proximityMult = 1.0 + (1.0 - distanceFromHalt / PressureConfig.getHaltTransitionZone()) * 0.5;
                pushForce *= proximityMult;
            }

            newVelX += horizontalForce.x * pushForce;
            newVelZ += horizontalForce.z * pushForce;

            if (forceDirection.y < -0.3) {
                newVelY += forceDirection.y * pushForce * 0.5;
            }
        }

        newVelX = clamp(newVelX, -3.5, 3.5);
        newVelY = clamp(newVelY, -3.0, 2.5);
        newVelZ = clamp(newVelZ, -3.5, 3.5);

        target.setDeltaMovement(newVelX, newVelY, newVelZ);
        target.hurtMarked = true;

        stateManager.setColliding(target.getUUID(), info.isColliding);
        stateManager.setPreviousVelocity(target.getUUID(), target.getDeltaMovement());

        info.forceDirection = forceDirection;
        info.distance = distance3D;

        return info;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class CollisionInfo {
        public boolean isColliding = false;
        public boolean isBreaching = false;
        public Vec3 forceDirection = Vec3.ZERO;
        public double distance = 0;
        public double pressureValue = 0;
        public double haltDistance = 0;
        public double distanceFromHalt = 0;
        public double resistanceStrength = 0;
    }
}
