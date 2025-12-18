package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
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

    /**
     * 处理范围内所有投射物
     */
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

        List<Projectile> projectilesInRange = owner.level().getEntitiesOfClass(
                Projectile.class,
                owner.getBoundingBox().inflate(maxRange),
                projectile -> shouldAffect(owner, projectile)
        );

        Set<UUID> currentlyAffected = new HashSet<>();

        for (Projectile projectile : projectilesInRange) {
            currentlyAffected.add(projectile.getUUID());
            applyPressure(owner, projectile, pressureLevel, cursedEnergyOutput,
                    maxRange, stateManager);
        }

        handleOutOfRangeProjectiles(owner, maxRange, currentlyAffected, stateManager);
    }

    /**
     * 处理超出范围的投射物
     */
    private static void handleOutOfRangeProjectiles(LivingEntity owner, double maxRange,
                                                    Set<UUID> currentlyAffected,
                                                    PressureStateManager stateManager) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        Set<UUID> trackedIds = stateManager.getTrackedProjectileIds();
        Vec3 ownerPos = owner.position();

        for (UUID trackedId : trackedIds) {
            if (currentlyAffected.contains(trackedId)) {
                continue;
            }

            Entity entity = level.getEntity(trackedId);
            if (entity instanceof Projectile projectile) {
                double distance = ownerPos.distanceTo(projectile.position());

                if (distance > maxRange) {
                    stateManager.releaseProjectile(projectile);
                } else if (!shouldAffect(owner, projectile)) {
                    stateManager.releaseProjectile(projectile);
                }
            }
        }
    }

    /**
     * 检查投射物是否应该受影响
     */
    private static boolean shouldAffect(LivingEntity owner, Projectile projectile) {
        Entity projectileOwner = projectile.getOwner();
        if (projectileOwner == owner) {
            return false;
        }

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

    /**
     * 对投射物施加压力效果
     */
    private static void applyPressure(LivingEntity owner, Projectile projectile,
                                      int pressureLevel, float cursedEnergyOutput,
                                      double maxRange,
                                      PressureStateManager stateManager) {

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Vec3 projectilePos = projectile.position();

        double distance = ownerCenter.distanceTo(projectilePos);
        if (distance < 0.1) distance = 0.1;
        if (distance > maxRange) return;

        // 跟踪投射物
        PressureStateManager.ProjectileState state = stateManager.trackProjectile(projectile);

        // ★ 检查豁免时间 ★
        if (state.immuneTicks > 0) {
            state.immuneTicks--;
            // 豁免期间仍然更新状态，但不施加任何力
            stateManager.updateProjectileState(projectile.getUUID(), distance, false);
            return;
        }

        double haltDistance = PressureCalculator.calculateHaltDistance(pressureLevel);
        double distanceFromHalt = distance - haltDistance;

        Vec3 currentVelocity = projectile.getDeltaMovement();
        double currentSpeed = currentVelocity.length();

        Vec3 toOwner = ownerCenter.subtract(projectilePos).normalize();
        Vec3 flyDirection = currentSpeed > 0.001 ? currentVelocity.normalize() : Vec3.ZERO;
        double approachDot = flyDirection.dot(toOwner);
        boolean isApproaching = approachDot > 0.1;

        // 记录接近速度（用于反弹计算）
        double approachSpeed = 0;
        if (isApproaching) {
            approachSpeed = currentVelocity.dot(toOwner);
            state.lastApproachSpeed = Math.max(state.lastApproachSpeed, approachSpeed);
        }

        Vec3 pushDirection = projectilePos.subtract(ownerCenter).normalize();
        double levelFactor = PressureCalculator.calculateLevelFactor(pressureLevel);

        Vec3 newVelocity;
        boolean isHovering = false;

        if (distanceFromHalt < 0) {
            // ==================== 突破停止边界：反弹！ ====================
            double breachDepth = Math.abs(distanceFromHalt);

            // 计算反弹速度
            // 基于：压力等级 + 突破深度 + 之前积累的接近速度
            double reflectSpeed = calculateReflectSpeed(
                    pressureLevel, cursedEnergyOutput, breachDepth,
                    state.lastApproachSpeed, state.hoverTicks);

            if (reflectSpeed >= PressureConfig.getProjectileReflectMinSpeed()) {
                // ★ 有效反弹：获得反向速度！ ★
                newVelocity = pushDirection.scale(reflectSpeed);

                // 恢复重力（正常飞行）
                projectile.setNoGravity(state.originalNoGravity);

                // 设置豁免时间
                state.immuneTicks = PressureConfig.getProjectileReflectImmuneTicks();

                // 重置累积数据
                state.lastApproachSpeed = 0;
                state.hoverTicks = 0;
                state.isHovering = false;

                // 反弹音效和粒子
                if (PressureConfig.areSoundsEnabled()) {
                    float volume = Math.min(0.3F + (float)(reflectSpeed * 0.2F), 0.8F);
                    projectile.level().playSound(null,
                            projectile.getX(), projectile.getY(), projectile.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL,
                            volume, 0.8F + (float)(Math.random() * 0.4));
                }

                if (PressureConfig.areParticlesEnabled()) {
                    spawnReflectParticles(projectile, pushDirection, reflectSpeed);
                }

            } else {
                // 推力不足以形成有效反弹，只是慢慢推回
                double slowdownMult = Math.max(0.1, 1.0 - breachDepth * 0.5);
                Vec3 slowedVelocity = currentVelocity.scale(slowdownMult);

                double repelForce = PressureConfig.getProjectileRepelForce() *
                        (1.0 + breachDepth * 2.0) * levelFactor * cursedEnergyOutput;
                repelForce = Math.min(repelForce, 0.3);

                newVelocity = slowedVelocity.add(pushDirection.scale(repelForce));
                projectile.setNoGravity(true);

                if (PressureConfig.areParticlesEnabled() && projectile.tickCount % 2 == 0) {
                    spawnRepelParticles(projectile, pushDirection);
                }
            }

        } else if (distanceFromHalt < PressureConfig.getHaltTransitionZone()) {
            // ==================== 过渡区：急剧减速 ====================
            double transitionProgress = 1.0 - (distanceFromHalt / PressureConfig.getHaltTransitionZone());

            double slowdownRate = PressureConfig.getProjectileSlowdownRate() *
                    (1.0 + transitionProgress * 3.0) * levelFactor * cursedEnergyOutput;
            slowdownRate = Math.min(slowdownRate, 0.8);

            if (isApproaching) {
                Vec3 approachVelocity = toOwner.scale(approachSpeed);
                Vec3 lateralVelocity = currentVelocity.subtract(approachVelocity);

                double newApproachSpeed = approachSpeed * (1.0 - slowdownRate);
                lateralVelocity = lateralVelocity.scale(1.0 - slowdownRate * 0.3);

                newVelocity = lateralVelocity.add(toOwner.scale(newApproachSpeed));
            } else {
                newVelocity = currentVelocity.scale(1.0 - slowdownRate * 0.2);
            }

            if (transitionProgress > 0.5) {
                projectile.setNoGravity(true);
            }

            if (PressureConfig.areParticlesEnabled() && projectile.tickCount % 4 == 0) {
                spawnSlowdownParticles(projectile, transitionProgress);
            }

        } else {
            // ==================== 外围区域：渐进减速 ====================
            double normalizedDist = (distanceFromHalt - PressureConfig.getHaltTransitionZone()) /
                    (maxRange - haltDistance - PressureConfig.getHaltTransitionZone());
            normalizedDist = Math.max(0, Math.min(1, normalizedDist));

            double slowdownRate = PressureConfig.getProjectileSlowdownRate() *
                    (1.0 - normalizedDist * 0.7) * levelFactor * cursedEnergyOutput;

            if (isApproaching) {
                Vec3 approachVelocity = toOwner.scale(approachSpeed);
                Vec3 lateralVelocity = currentVelocity.subtract(approachVelocity);

                double newApproachSpeed = approachSpeed * (1.0 - slowdownRate);
                newVelocity = lateralVelocity.add(toOwner.scale(newApproachSpeed));
            } else {
                newVelocity = currentVelocity;
            }

            if (!state.isHovering) {
                projectile.setNoGravity(state.originalNoGravity);
            }

            if (PressureConfig.areParticlesEnabled() && projectile.tickCount % 10 == 0 && normalizedDist < 0.5) {
                spawnDistortionParticles(projectile);
            }
        }

        // 检查是否悬浮
        double newSpeed = newVelocity.length();
        if (newSpeed < PressureConfig.getProjectileMinSpeed()) {
            isHovering = true;

            if (isApproaching && currentSpeed > 0.001) {
                newVelocity = pushDirection.scale(PressureConfig.getProjectileMinSpeed() * 0.5);
            } else {
                newVelocity = currentVelocity.scale(0.1);
            }

            projectile.setNoGravity(true);

            if (PressureConfig.areParticlesEnabled() && projectile.tickCount % 8 == 0) {
                spawnHoverParticles(projectile);
            }

            if (PressureConfig.areSoundsEnabled() && projectile.tickCount % 40 == 0) {
                projectile.level().playSound(null,
                        projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.BEACON_AMBIENT, SoundSource.NEUTRAL,
                        0.1F, 2.0F);
            }
        }

        projectile.setDeltaMovement(newVelocity);
        stateManager.updateProjectileState(projectile.getUUID(), distance, isHovering);
    }

    /**
     * 计算反弹速度
     */
    private static double calculateReflectSpeed(int pressureLevel, float cursedEnergyOutput,
                                                double breachDepth, double accumulatedApproachSpeed,
                                                int hoverTicks) {

        double levelFactor = PressureCalculator.calculateLevelFactor(pressureLevel);

        // 基础反弹力 = 压力等级因子 × 咒力输出 × 突破深度
        double baseReflect = levelFactor * cursedEnergyOutput * (1.0 + breachDepth * 2.0);

        // 加上积累的接近速度（投射物原本的动能）
        double speedBonus = accumulatedApproachSpeed * PressureConfig.getProjectileReflectSpeedMult();

        // 悬浮时间越长，积累的"压力"越大
        double hoverBonus = Math.min(hoverTicks * 0.02, 0.5);

        double totalSpeed = (baseReflect * 0.5) + speedBonus + hoverBonus;

        // 限制最大速度
        return Math.min(totalSpeed, PressureConfig.getProjectileReflectMaxSpeed());
    }

    // ==================== 粒子效果 ====================

    private static void spawnReflectParticles(Projectile projectile, Vec3 pushDir, double speed) {
        if (!(projectile.level() instanceof ServerLevel level)) return;

        int intensity = (int) Math.min(speed * 5, 15);

        // 爆发粒子
        level.sendParticles(ParticleTypes.CRIT,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                intensity, 0.2, 0.2, 0.2, speed * 0.1);

        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                2, 0.1, 0.1, 0.1, 0);

        // 方向指示粒子
        for (int i = 0; i < 3; i++) {
            double offset = (i + 1) * 0.3;
            level.sendParticles(ParticleTypes.END_ROD,
                    projectile.getX() + pushDir.x * offset,
                    projectile.getY() + pushDir.y * offset,
                    projectile.getZ() + pushDir.z * offset,
                    1, 0, 0, 0, 0.01);
        }
    }

    private static void spawnRepelParticles(Projectile projectile, Vec3 pushDir) {
        if (!(projectile.level() instanceof ServerLevel level)) return;

        level.sendParticles(ParticleTypes.CRIT,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                5, 0.1, 0.1, 0.1, 0.05);

        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                projectile.getX() - pushDir.x * 0.3,
                projectile.getY() - pushDir.y * 0.3,
                projectile.getZ() - pushDir.z * 0.3,
                1, 0, 0, 0, 0);
    }

    private static void spawnSlowdownParticles(Projectile projectile, double intensity) {
        if (!(projectile.level() instanceof ServerLevel level)) return;

        int count = 1 + (int)(intensity * 3);
        level.sendParticles(ParticleTypes.EFFECT,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                count, 0.1, 0.1, 0.1, 0.01);
    }

    private static void spawnHoverParticles(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;

        level.sendParticles(ParticleTypes.END_ROD,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                2, 0.15, 0.15, 0.15, 0.005);
    }

    private static void spawnDistortionParticles(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return;

        level.sendParticles(ParticleTypes.EFFECT,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                1, 0.05, 0.05, 0.05, 0.01);
    }
}
