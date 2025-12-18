package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import radon.jujutsu_kaisen.entity.base.DomainExpansionEntity;
import radon.jujutsu_kaisen.entity.base.SummonEntity;
import radon.jujutsu_kaisen.entity.projectile.base.JujutsuProjectile;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProjectilePressureHandler {

    public static void handleProjectiles(LivingEntity owner, int pressureLevel,
                                         float cursedEnergyOutput, double maxRange,
                                         PressureStateManager stateManager) {

        if (!PressureConfig.shouldAffectProjectiles()) {
            stateManager.releaseAllProjectiles(owner);
            return;
        }

        if (pressureLevel < PressureConfig.getProjectileMinPressure()) {
            stateManager.releaseAllProjectiles(owner);
            return;
        }

        stateManager.tickRepelledProjectiles();

        List<Projectile> projectilesInRange = owner.level().getEntitiesOfClass(
                Projectile.class,
                owner.getBoundingBox().inflate(maxRange),
                projectile -> shouldAffect(owner, projectile)
        );

        Set<UUID> currentlyAffected = new HashSet<>();

        for (Projectile projectile : projectilesInRange) {
            if (stateManager.isRepelled(projectile.getUUID())) {
                continue;
            }

            currentlyAffected.add(projectile.getUUID());
            applyPressure(owner, projectile, pressureLevel, cursedEnergyOutput,
                    maxRange, stateManager);
        }

        handleOutOfRangeProjectiles(owner, maxRange, currentlyAffected, stateManager);
    }

    private static void applyPressure(LivingEntity owner, Projectile projectile,
                                      int pressureLevel, float cursedEnergyOutput,
                                      double maxRange,
                                      PressureStateManager stateManager) {
        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Vec3 projectilePos = projectile.position();
        double distance = ownerCenter.distanceTo(projectilePos);
        if (distance < 0.1) distance = 0.1;
        if (distance > maxRange) return;

        PressureStateManager.ProjectileState state = stateManager.trackProjectile(projectile);

        // 首次追踪时保存火焰弹的动力值
        stateManager.saveHurtingProjectilePower(projectile, state);
        projectile.setNoGravity(true);

        if (state.immuneTicks > 0) {
            state.immuneTicks--;
            if (state.positionLocked) {
                stateManager.enforceProjectilePosition(projectile, state);
            }
            return;
        }

        double haltDistance = PressureCalculator.calculateHaltDistance(pressureLevel);
        double levelFactor = PressureCalculator.calculateLevelFactor(pressureLevel);

        double stopZoneBuffer = PressureConfig.getProjectileStopZoneBuffer()
                + (pressureLevel * PressureConfig.getProjectileStopZoneBufferPerLevel());
        double stopZoneOuter = haltDistance + stopZoneBuffer;
        double stopZoneInner = haltDistance - stopZoneBuffer;
        stopZoneInner = Math.max(PressureConfig.getProjectileStopZoneMinInner(), stopZoneInner);

        Vec3 pushDirection = projectilePos.subtract(ownerCenter).normalize();
        Vec3 originalVelocity = state.originalVelocity;

        // ========== 出力突变检测 ==========
        boolean isPowerSurge = false;
        double surgeMult = 1.0;

        if (state.baselinePressureLevel == 0 && state.baselineCursedOutput == 0) {
            state.baselinePressureLevel = pressureLevel;
            state.baselineCursedOutput = cursedEnergyOutput;
        }

        if (state.isFullyStopped || state.stoppedTicks > 0) {
            state.ticksSinceBaseline++;

            if (state.ticksSinceBaseline >= PressureConfig.getProjectileSurgeCheckInterval()) {
                int levelDiff = pressureLevel - state.baselinePressureLevel;
                float outputDiff = cursedEnergyOutput - state.baselineCursedOutput;

                if (levelDiff >= PressureConfig.getProjectileSurgeLevelThreshold() ||
                        (outputDiff >= PressureConfig.getProjectileSurgeOutputThreshold()
                                && state.baselineCursedOutput > 0)) {
                    isPowerSurge = true;
                    surgeMult = PressureConfig.getProjectileSurgeBaseMult()
                            + Math.max(
                            levelDiff * PressureConfig.getProjectileSurgeLevelFactor(),
                            outputDiff * PressureConfig.getProjectileSurgeOutputFactor()
                    );
                    surgeMult = Math.min(surgeMult, PressureConfig.getProjectileSurgeMaxMult());
                }

                state.baselinePressureLevel = pressureLevel;
                state.baselineCursedOutput = cursedEnergyOutput;
                state.ticksSinceBaseline = 0;
            }
        }

        // ========== 出力突变 → 弹开 ==========
        if (isPowerSurge) {
            double pushStrength = PressureConfig.getProjectileSurgePushBase()
                    * surgeMult * levelFactor * cursedEnergyOutput;
            pushStrength = Math.min(pushStrength, PressureConfig.getProjectileSurgePushMax());

            state.unlockPosition();

            Vec3 repelVelocity = pushDirection.scale(pushStrength);
            projectile.setDeltaMovement(repelVelocity);

            // 火焰弹弹开时设置反向动力
            if (projectile instanceof AbstractHurtingProjectile hurting) {
                hurting.xPower = pushDirection.x * pushStrength * 0.1;
                hurting.yPower = pushDirection.y * pushStrength * 0.1;
                hurting.zPower = pushDirection.z * pushStrength * 0.1;
            }

            projectile.setNoGravity(state.originalNoGravity);
            projectile.hurtMarked = true;
            projectile.hasImpulse = true;

            UUID projectileId = projectile.getUUID();
            stateManager.forceRemoveProjectile(projectileId);
            stateManager.markAsRepelled(projectileId, PressureConfig.getProjectileReflectImmuneTicks());

            if (PressureConfig.areSoundsEnabled()) {
                projectile.level().playSound(null,
                        projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL,
                        0.6F, 0.7F);
            }
            if (PressureConfig.areParticlesEnabled()) {
                spawnRepelParticles(projectile);
            }
            return;
        }

        // ========== 三区域判断 ==========
        if (distance > stopZoneOuter) {
            // 【减速区】
            if (state.positionLocked) {
                state.unlockPosition();
            }

            // ★★★ 使用通用方法计算 speedRatio ★★★
            double speedRatio = VelocityController.calculateSpeedRatioWithCustomStop(
                    pressureLevel, cursedEnergyOutput, distance, maxRange, stopZoneOuter);

            Vec3 finalVelocity = originalVelocity.scale(speedRatio);
            projectile.setDeltaMovement(finalVelocity);

            // 火焰弹减速时也要调整动力
            if (projectile instanceof AbstractHurtingProjectile hurting && state.powerSaved) {
                hurting.xPower = state.originalXPower * speedRatio;
                hurting.yPower = state.originalYPower * speedRatio;
                hurting.zPower = state.originalZPower * speedRatio;
            }

            if (PressureConfig.areParticlesEnabled() && speedRatio < 0.95) {
                int interval = 2 + (int) (speedRatio * 6);
                if (projectile.tickCount % interval == 0) {
                    spawnSlowdownParticles(projectile, 1.0 - speedRatio);
                }
            }
            state.isFullyStopped = false;

        } else if (distance >= stopZoneInner) {
            // 【停止区】
            if (!state.isFullyStopped) {
                state.isFullyStopped = true;
                state.stoppedTicks = 0;
                state.baselinePressureLevel = pressureLevel;
                state.baselineCursedOutput = cursedEnergyOutput;
                state.ticksSinceBaseline = 0;

                state.lockPosition(projectile.position());

                if (PressureConfig.areSoundsEnabled()) {
                    projectile.level().playSound(null,
                            projectile.getX(), projectile.getY(), projectile.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL,
                            0.3F, 1.5F);
                }
                if (PressureConfig.areParticlesEnabled()) {
                    spawnHaltParticles(projectile);
                }
            }

            state.stoppedTicks++;

            // 每tick强制保持位置（包括清零火焰弹动力）
            stateManager.enforceProjectilePosition(projectile, state);

            if (PressureConfig.areParticlesEnabled() && projectile.tickCount % 12 == 0) {
                spawnHoverParticles(projectile);
            }

        } else {
            // 【推力区】
            if (state.positionLocked) {
                state.unlockPosition();
            }

            double penetration = stopZoneInner - distance;
            double pushStrength = PressureConfig.getProjectilePushZoneBase()
                    * (1.0 + penetration * PressureConfig.getProjectilePushZonePenetrationFactor())
                    * levelFactor * cursedEnergyOutput;
            pushStrength = Math.min(pushStrength, PressureConfig.getProjectilePushZoneMax());

            Vec3 finalVelocity = pushDirection.scale(pushStrength);
            projectile.setDeltaMovement(finalVelocity);

            // 火焰弹推力区也要设置反向动力
            if (projectile instanceof AbstractHurtingProjectile hurting) {
                hurting.xPower = pushDirection.x * pushStrength * 0.05;
                hurting.yPower = pushDirection.y * pushStrength * 0.05;
                hurting.zPower = pushDirection.z * pushStrength * 0.05;
            }

            state.isFullyStopped = false;

            if (PressureConfig.areParticlesEnabled() && projectile.tickCount % 8 == 0) {
                spawnPushParticles(projectile);
            }
        }

        state.lastDistance = distance;
    }

    private static void handleOutOfRangeProjectiles(LivingEntity owner, double maxRange,
                                                    Set<UUID> currentlyAffected,
                                                    PressureStateManager stateManager) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        Set<UUID> trackedIds = new HashSet<>(stateManager.getTrackedProjectileIds());
        Vec3 ownerPos = owner.position();

        for (UUID trackedId : trackedIds) {
            if (currentlyAffected.contains(trackedId)) {
                continue;
            }

            Entity entity = level.getEntity(trackedId);
            if (entity instanceof Projectile projectile) {
                double distance = ownerPos.distanceTo(projectile.position());
                if (distance > maxRange || !shouldAffect(owner, projectile)) {
                    stateManager.releaseProjectile(projectile);
                }
            } else {
                stateManager.forceRemoveProjectile(trackedId);
            }
        }
    }

    private static boolean shouldAffect(LivingEntity owner, Projectile projectile) {
        Entity projectileOwner = projectile.getOwner();

        if (projectileOwner == owner) return false;

        if (projectileOwner instanceof SummonEntity summon && summon.getOwner() == owner) {
            return false;
        }

        if (projectileOwner instanceof TamableAnimal tamable &&
                tamable.isTame() && tamable.getOwner() == owner) {
            return false;
        }

        if (projectileOwner instanceof LivingEntity livingOwner) {
            if (isInOwnersDomainWithSureHit(owner, livingOwner)) {
                return false;
            }
        }

        try {
            if (!HelperMethods.isBlockable(owner, projectile)) {
                return false;
            }
        } catch (Exception ignored) {}

        if (projectile instanceof JujutsuProjectile jujutsu) {
            try {
                if (jujutsu.isDomain()) {
                    return false;
                }
            } catch (Exception ignored) {}
        }

        return true;
    }

    private static boolean isInOwnersDomainWithSureHit(LivingEntity owner, LivingEntity projectileOwner) {
        List<DomainExpansionEntity> domains = owner.level().getEntitiesOfClass(
                DomainExpansionEntity.class,
                owner.getBoundingBox().inflate(100)
        );

        for (DomainExpansionEntity domain : domains) {
            if (domain.getOwner() != projectileOwner) continue;
            if (!domain.hasSureHitEffect()) continue;
            if (!domain.checkSureHitEffect()) continue;
            if (domain.isAffected(owner)) {
                return true;
            }
        }
        return false;
    }

    // ========== 粒子效果 ==========

    private static void spawnHaltParticles(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.CRIT,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                5, 0.15, 0.15, 0.15, 0.01);
    }

    private static void spawnHoverParticles(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.END_ROD,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                1, 0.08, 0.08, 0.08, 0.001);
    }

    private static void spawnSlowdownParticles(Projectile projectile, double intensity) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        int count = 1 + (int) (intensity * 3);
        level.sendParticles(ParticleTypes.EFFECT,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                count, 0.1, 0.1, 0.1, 0.01);
    }

    private static void spawnPushParticles(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.CLOUD,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                1, 0.05, 0.05, 0.05, 0.005);
    }

    private static void spawnRepelParticles(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.CRIT,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                10, 0.25, 0.25, 0.25, 0.2);
        level.sendParticles(ParticleTypes.CLOUD,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                5, 0.2, 0.2, 0.2, 0.1);
    }
}
