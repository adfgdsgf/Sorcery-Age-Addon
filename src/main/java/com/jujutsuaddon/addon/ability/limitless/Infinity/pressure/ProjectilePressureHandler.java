package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
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

import java.util.*;

public class ProjectilePressureHandler {

    // ★★★ 性能优化：缓存领域检测结果 ★★★
    private static final Map<UUID, Long> domainCheckCache = new HashMap<>();
    private static final Map<UUID, Boolean> domainResultCache = new HashMap<>();
    private static final long DOMAIN_CACHE_DURATION = 20; // 20 tick = 1 秒

    // ★★★ 新增：高速投射物检测的额外范围 ★★★
    private static final double HIGH_SPEED_DETECTION_MULTIPLIER = 2.5;  // 速度的2.5倍
    private static final double MIN_EXTRA_RANGE = 3.0;  // 最小额外范围
    private static final double MAX_EXTRA_RANGE = 20.0; // 最大额外范围

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

        // ★★★ 关键修复：扩大搜索范围以捕获高速投射物 ★★★
        double searchRange = maxRange + MAX_EXTRA_RANGE;

        List<Projectile> projectilesInRange = owner.level().getEntitiesOfClass(
                Projectile.class,
                owner.getBoundingBox().inflate(searchRange),
                projectile -> shouldAffect(owner, projectile)
        );

        Set<UUID> currentlyAffected = new HashSet<>();
        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);

        for (Projectile projectile : projectilesInRange) {
            if (stateManager.isRepelled(projectile.getUUID())) continue;

            double distance = ownerCenter.distanceTo(projectile.position());

            // ★★★ 关键修复：根据速度判断是否会进入范围 ★★★
            if (!willEnterRange(projectile, ownerCenter, distance, maxRange)) {
                continue;
            }

            currentlyAffected.add(projectile.getUUID());

            initializeControlIfNeeded(projectile, owner, ownerCenter, distance, maxRange,
                    pressureLevel, cursedEnergyOutput, stateManager);
        }

        releaseOutOfRange(owner, maxRange, currentlyAffected, stateManager);

        // ★★★ 性能优化：定期清理缓存 ★★★
        if (owner.tickCount % 100 == 0) {
            cleanupDomainCache(owner.level().getGameTime());
        }
    }

    /**
     * ★★★ 新增：判断投射物是否会在下一帧进入范围 ★★★
     */
    private static boolean willEnterRange(Projectile projectile, Vec3 ownerCenter,
                                          double currentDistance, double maxRange) {
        // 已经在范围内
        if (currentDistance <= maxRange) {
            return true;
        }

        // 计算投射物速度
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = velocity.length();

        // 低速投射物，用普通检测
        if (speed < 0.5) {
            return false;  // 不在范围内且速度慢，不处理
        }

        // ★★★ 高速投射物：检查是否正在接近 ★★★
        Vec3 toOwner = ownerCenter.subtract(projectile.position());
        double approachSpeed = velocity.dot(toOwner.normalize());

        // 如果正在远离，不处理
        if (approachSpeed <= 0) {
            return false;
        }

        // ★★★ 计算需要多少帧才能到达范围边界 ★★★
        double distanceToRange = currentDistance - maxRange;
        double framesToReach = distanceToRange / approachSpeed;

        // 如果 2 帧内能到达，提前开始处理
        // 这样即使下一帧跳过了边界，这一帧就已经开始减速了
        return framesToReach <= 2.0;
    }

    private static void initializeControlIfNeeded(Projectile projectile, LivingEntity owner,
                                                  Vec3 ownerCenter, double distance, double maxRange,
                                                  int pressureLevel, float cursedEnergyOutput,
                                                  PressureStateManager stateManager) {
        if (!(projectile instanceof IFrozenProjectile fp)) return;
        boolean wasControlled = fp.jujutsuAddon$isControlled();

        // ★★★ 计算初始速度倍率 ★★★
        float initialSpeedMod;
        if (distance > maxRange) {
            // 在范围外但即将进入，开始预减速
            // 根据距离计算，越近减速越多
            double distanceRatio = (distance - maxRange) / MAX_EXTRA_RANGE;
            distanceRatio = Math.max(0, Math.min(1, distanceRatio));
            // 范围边界处: 0.4, 预检测边界处: 0.8
            initialSpeedMod = (float) (0.4 + distanceRatio * 0.4);
        } else {
            // 在范围内，使用正常入口速度
            initialSpeedMod = (float) PressureConfig.getProjectileEntrySpeed();
        }

        if (!wasControlled) {
            fp.jujutsuAddon$setOriginalVelocity(projectile.getDeltaMovement());
            fp.jujutsuAddon$lockRotation(projectile.getYRot(), projectile.getXRot());

            float stopDistance = (float) PressureConfig.getStopZoneRadius(pressureLevel);
            fp.jujutsuAddon$setStopDistance(stopDistance);
            fp.jujutsuAddon$setMaxRange((float) maxRange);

            fp.jujutsuAddon$setSpeedMultiplier(initialSpeedMod);
            fp.jujutsuAddon$setControlled(true);
            fp.jujutsuAddon$setFreezeOwner(owner.getUUID());
            projectile.setNoGravity(true);

            // ★★★ 立即应用减速，防止下一帧穿透 ★★★
            Vec3 currentVel = projectile.getDeltaMovement();
            projectile.setDeltaMovement(currentVel.scale(initialSpeedMod));

            if (PressureConfig.areSoundsEnabled()) {
                projectile.level().playSound(null,
                        projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 0.3F, 1.5F);
            }
            spawnSlowdownParticles(projectile, 1.0);
        }

        stateManager.trackProjectile(projectile);

        // ★★★ 性能优化：减少粒子频率 ★★★
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
                // ★★★ 修复：用 maxRange + 缓冲区判断，避免边界抖动 ★★★
                if (distance > maxRange + 1.0 || !shouldAffect(owner, projectile)) {
                    releaseProjectile(projectile, stateManager);
                }
            } else {
                stateManager.forceRemoveProjectile(trackedId);
            }
        }
    }

    public static void releaseProjectile(Projectile projectile, PressureStateManager stateManager) {
        if (projectile instanceof IFrozenProjectile fp) {
            fp.jujutsuAddon$setControlled(false);
        }

        // ★★★ 修复：确保火焰弹正确释放 ★★★
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = 0;
            hurting.yPower = -0.03;
            hurting.zPower = 0;
        }

        projectile.setDeltaMovement(new Vec3(0, -0.1, 0));
        projectile.setNoGravity(false);
        projectile.hurtMarked = true;

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

    // ★★★ 性能优化：缓存领域检测 ★★★
    private static boolean isInOwnersDomainWithSureHit(LivingEntity owner, LivingEntity projectileOwner) {
        UUID cacheKey = projectileOwner.getUUID();
        long currentTime = owner.level().getGameTime();

        // 检查缓存
        Long lastCheck = domainCheckCache.get(cacheKey);
        if (lastCheck != null && currentTime - lastCheck < DOMAIN_CACHE_DURATION) {
            Boolean cachedResult = domainResultCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        // 执行检测
        boolean result = doCheckDomainSureHit(owner, projectileOwner);

        // 缓存结果
        domainCheckCache.put(cacheKey, currentTime);
        domainResultCache.put(cacheKey, result);

        return result;
    }

    private static boolean doCheckDomainSureHit(LivingEntity owner, LivingEntity projectileOwner) {
        // ★★★ 性能优化：缩小搜索范围 ★★★
        List<DomainExpansionEntity> domains = owner.level().getEntitiesOfClass(
                DomainExpansionEntity.class,
                owner.getBoundingBox().inflate(50)  // 从 100 减少到 50
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

    // ★★★ 清理过期缓存 ★★★
    private static void cleanupDomainCache(long currentTime) {
        domainCheckCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > DOMAIN_CACHE_DURATION * 5);
        domainResultCache.keySet().retainAll(domainCheckCache.keySet());
    }

    private static void spawnSlowdownParticles(Projectile p, double intensity) {
        if (p.level() instanceof ServerLevel level) {
            int count = 1 + (int) (intensity * 2);  // ★ 减少粒子数量
            level.sendParticles(ParticleTypes.CRIT,
                    p.getX(), p.getY(), p.getZ(),
                    count, 0.1, 0.1, 0.1, 0.01);
        }
    }
}
