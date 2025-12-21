package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.conflict.InfinityConflictResolver;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.PressureBypassChecker;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.RaySphereChecker;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.phys.Vec3;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.entity.base.DomainExpansionEntity;
import radon.jujutsu_kaisen.entity.base.SummonEntity;
import radon.jujutsu_kaisen.entity.projectile.base.JujutsuProjectile;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.*;

/**
 * 投射物压力处理器
 */
public class ProjectilePressureHandler {

    private static final Map<UUID, Long> domainCheckCache = new HashMap<>();
    private static final Map<UUID, Boolean> domainResultCache = new HashMap<>();
    private static final long DOMAIN_CACHE_DURATION = 20;

    private static final Map<UUID, Long> amplificationCheckCache = new HashMap<>();
    private static final Map<UUID, Boolean> amplificationResultCache = new HashMap<>();

    private static final double HIGH_SPEED_EXTRA_RANGE = 15.0;

    public static void handleProjectiles(LivingEntity owner, int pressureLevel,
                                         float cursedEnergyOutput, double maxRange,
                                         PressureStateManager stateManager) {
        if (!PressureConfig.shouldAffectProjectiles()) {
            releaseAll(owner, stateManager);
            return;
        }
        if (pressureLevel < PressureConfig.getProjectileMinPressure()) {
            releaseAll(owner, stateManager);
            return;
        }

        stateManager.tickRepelledProjectiles();

        long currentTick = owner.level().getGameTime();
        if (currentTick % 100 == 0) {
            ProjectileReleaseTracker.cleanup(currentTick);
        }

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Set<UUID> currentlyAffected = new HashSet<>();

        List<Projectile> projectilesInRange = owner.level().getEntitiesOfClass(
                Projectile.class,
                owner.getBoundingBox().inflate(maxRange),
                projectile -> shouldAffect(owner, projectile)
        );

        for (Projectile projectile : projectilesInRange) {
            if (stateManager.isRepelled(projectile.getUUID())) continue;

            if (ProjectileReleaseTracker.isInCooldown(projectile.getUUID(), currentTick)) {
                continue;
            }

            double distance = ownerCenter.distanceTo(projectile.position());
            if (distance > maxRange) continue;

            currentlyAffected.add(projectile.getUUID());

            initializeControlIfNeeded(projectile, owner, ownerCenter, distance, maxRange,
                    pressureLevel, cursedEnergyOutput, stateManager);
        }

        preSlowHighSpeedProjectiles(owner, ownerCenter, maxRange, pressureLevel, stateManager, currentTick);
        releaseOutOfRange(owner, maxRange, currentlyAffected, stateManager);

        if (owner.tickCount % 100 == 0) {
            cleanupCaches(owner.level().getGameTime());
        }
    }

    private static void preSlowHighSpeedProjectiles(LivingEntity owner, Vec3 ownerCenter,
                                                    double maxRange, int pressureLevel,
                                                    PressureStateManager stateManager,
                                                    long currentTick) {
        if (owner.tickCount % 2 != 0) return;

        double outerRange = maxRange + HIGH_SPEED_EXTRA_RANGE;

        List<Projectile> outerProjectiles = owner.level().getEntitiesOfClass(
                Projectile.class,
                owner.getBoundingBox().inflate(outerRange),
                projectile -> shouldAffect(owner, projectile)
        );

        for (Projectile projectile : outerProjectiles) {
            if (stateManager.isRepelled(projectile.getUUID())) continue;
            if (!(projectile instanceof IFrozenProjectile fp)) continue;

            if (fp.jujutsuAddon$isControlled()) continue;

            if (ProjectileReleaseTracker.isInCooldown(projectile.getUUID(), currentTick)) {
                continue;
            }

            double distance = ownerCenter.distanceTo(projectile.position());

            if (distance <= maxRange || distance > outerRange) continue;

            Vec3 velocity = projectile.getDeltaMovement();
            double speed = velocity.length();
            if (speed < 1.0) continue;

            Vec3 toOwner = ownerCenter.subtract(projectile.position()).normalize();
            double approachSpeed = velocity.dot(toOwner);
            if (approachSpeed <= 0.5) continue;

            double framesToReach = (distance - maxRange) / approachSpeed;
            if (framesToReach > 3) continue;

            double slowFactor = 0.5 + (framesToReach / 3.0) * 0.3;
            Vec3 newVel = velocity.scale(slowFactor);
            projectile.setDeltaMovement(newVel);
            projectile.hurtMarked = true;

            if (PressureConfig.areParticlesEnabled()) {
                spawnSlowdownParticles(projectile, 0.5);
            }
        }
    }

    private static void initializeControlIfNeeded(Projectile projectile, LivingEntity owner,
                                                  Vec3 ownerCenter, double distance, double maxRange,
                                                  int pressureLevel, float cursedEnergyOutput,
                                                  PressureStateManager stateManager) {
        if (!(projectile instanceof IFrozenProjectile fp)) return;

        boolean wasControlled = fp.jujutsuAddon$isControlled();
        double balanceRadius = BalancePointCalculator.getBalanceRadius(pressureLevel, maxRange);
        float currentStopDistance = (float) balanceRadius;
        float currentMaxRange = (float) maxRange;

        // 如果未被控制，先检查方向是否会穿过平衡点
        if (!wasControlled) {
            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.lengthSqr() > 0.01) {
                boolean willHit = RaySphereChecker.willHitBalancePoint(
                        projectile.position(), velocity, ownerCenter, balanceRadius);
                if (!willHit) {
                    return;
                }
            }
        }

        if (!wasControlled) {
            ProjectileReleaseTracker.clearCooldown(projectile.getUUID());

            if (fp.jujutsuAddon$getOriginalCapturePosition() == null) {
                fp.jujutsuAddon$setOriginalCapturePosition(projectile.position());
            }

            Vec3 currentVel = projectile.getDeltaMovement();
            if (fp.jujutsuAddon$getOriginalVelocity() == null && currentVel.lengthSqr() > 0.001) {
                fp.jujutsuAddon$setOriginalVelocity(currentVel);
                fp.jujutsuAddon$setCurrentDirection(currentVel.normalize());
            }

            // ★★★ 记录捕获时的速度（进入无限范围时的速度）★★★
            if (currentVel.lengthSqr() > 0.001) {
                fp.jujutsuAddon$setCaptureVelocity(currentVel);
            }

            if (projectile instanceof AbstractHurtingProjectile hurting) {
                if (fp.jujutsuAddon$getOriginalPower() == null) {
                    Vec3 power = new Vec3(hurting.xPower, hurting.yPower, hurting.zPower);
                    if (power.lengthSqr() > 0.0001) {
                        fp.jujutsuAddon$setOriginalPower(power);
                    }
                }
                hurting.xPower = 0;
                hurting.yPower = 0;
                hurting.zPower = 0;

                // ★★★ 火焰弹禁用重力 ★★★
                projectile.setNoGravity(true);
            }
            // ★★★ 箭矢/雪球等：不禁用重力！★★★
            // 让 ControlledProjectileTick 中的芝诺物理处理重力

            fp.jujutsuAddon$lockRotation(projectile.getYRot(), projectile.getXRot());
            fp.jujutsuAddon$setSpeedMultiplier((float) PressureConfig.getProjectileEntrySpeed());
            fp.jujutsuAddon$setControlled(true);
            fp.jujutsuAddon$setFreezeOwner(owner.getUUID());

            spawnSlowdownParticles(projectile, 1.0);
        }

        fp.jujutsuAddon$setStopDistance(currentStopDistance);
        fp.jujutsuAddon$setMaxRange(currentMaxRange);
        stateManager.trackProjectile(projectile);

        float currentSpeedMod = fp.jujutsuAddon$getSpeedMultiplier();
        if (PressureConfig.areParticlesEnabled() && currentSpeedMod < 0.3 && projectile.tickCount % 15 == 0) {
            spawnSlowdownParticles(projectile, 1.0 - currentSpeedMod);
        }
    }

    // ==================== 释放逻辑 ====================

    private static void releaseOutOfRange(LivingEntity owner, double maxRange,
                                          Set<UUID> currentlyAffected,
                                          PressureStateManager stateManager) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        for (UUID trackedId : new HashSet<>(stateManager.getTrackedProjectileIds())) {
            if (currentlyAffected.contains(trackedId)) continue;

            Entity entity = level.getEntity(trackedId);
            if (entity instanceof Projectile projectile) {
                double distance = owner.position().distanceTo(projectile.position());
                if (distance > maxRange || !shouldAffect(owner, projectile)) {
                    releaseProjectile(projectile, stateManager);
                }
            } else {
                stateManager.forceRemoveProjectile(trackedId);
            }
        }
    }

    public static void releaseProjectile(Projectile projectile, PressureStateManager stateManager) {
        ProjectileReleaseHelper.release(projectile);
        stateManager.forceRemoveProjectile(projectile.getUUID());
    }

    private static void releaseAll(LivingEntity owner, PressureStateManager stateManager) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        for (UUID id : new HashSet<>(stateManager.getTrackedProjectileIds())) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Projectile projectile) {
                releaseProjectile(projectile, stateManager);
            }
        }
        stateManager.releaseAllProjectiles(owner);
    }

    // ==================== 目标筛选 ====================

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

        InfinityConflictResolver.ConflictResult conflict =
                InfinityConflictResolver.resolveProjectileConflict(owner, projectile);
        if (!conflict.canAffect) {
            return false;
        }

        if (projectileOwner instanceof LivingEntity livingOwner) {
            if (PressureBypassChecker.shouldBypassPressure(livingOwner)) {
                return false;
            }
            if (isInOwnersDomainWithSureHit(owner, livingOwner)) {
                return false;
            }
            if (PressureConfig.respectDomainAmplification()) {
                if (hasDomainAmplification(livingOwner)) {
                    return false;
                }
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

    // ==================== 领域增幅检查 ====================

    private static boolean hasDomainAmplification(LivingEntity entity) {
        UUID cacheKey = entity.getUUID();
        long currentTime = entity.level().getGameTime();

        Long lastCheck = amplificationCheckCache.get(cacheKey);
        if (lastCheck != null && currentTime - lastCheck < DOMAIN_CACHE_DURATION) {
            Boolean cachedResult = amplificationResultCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        boolean result = doCheckDomainAmplification(entity);

        amplificationCheckCache.put(cacheKey, currentTime);
        amplificationResultCache.put(cacheKey, result);

        return result;
    }

    private static boolean doCheckDomainAmplification(LivingEntity entity) {
        try {
            ISorcererData data = entity.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
            if (data != null) {
                return data.hasToggled(JJKAbilities.DOMAIN_AMPLIFICATION.get());
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ==================== 领域必中检查 ====================

    private static boolean isInOwnersDomainWithSureHit(LivingEntity owner, LivingEntity projectileOwner) {
        UUID cacheKey = projectileOwner.getUUID();
        long currentTime = owner.level().getGameTime();

        Long lastCheck = domainCheckCache.get(cacheKey);
        if (lastCheck != null && currentTime - lastCheck < DOMAIN_CACHE_DURATION) {
            Boolean cachedResult = domainResultCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        boolean result = doCheckDomainSureHit(owner, projectileOwner);

        domainCheckCache.put(cacheKey, currentTime);
        domainResultCache.put(cacheKey, result);

        return result;
    }

    private static boolean doCheckDomainSureHit(LivingEntity owner, LivingEntity projectileOwner) {
        List<DomainExpansionEntity> domains = owner.level().getEntitiesOfClass(
                DomainExpansionEntity.class,
                owner.getBoundingBox().inflate(50)
        );

        for (DomainExpansionEntity domain : domains) {
            if (domain.getOwner() != projectileOwner) continue;
            if (!domain.hasSureHitEffect() || !domain.checkSureHitEffect()) continue;
            if (domain.isAffected(owner)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 缓存清理 ====================

    private static void cleanupCaches(long currentTime) {
        domainCheckCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > DOMAIN_CACHE_DURATION * 5);
        domainResultCache.keySet().retainAll(domainCheckCache.keySet());

        amplificationCheckCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > DOMAIN_CACHE_DURATION * 5);
        amplificationResultCache.keySet().retainAll(amplificationCheckCache.keySet());
    }

    private static void spawnSlowdownParticles(Projectile p, double intensity) {
        if (p.level() instanceof ServerLevel level) {
            int count = 1 + (int) (intensity * 2);
            level.sendParticles(ParticleTypes.CRIT,
                    p.getX(), p.getY(), p.getZ(),
                    count, 0.1, 0.1, 0.1, 0.01);
        }
    }
}
