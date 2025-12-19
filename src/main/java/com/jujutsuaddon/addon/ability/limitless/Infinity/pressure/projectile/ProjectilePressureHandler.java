package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.conflict.InfinityConflictResolver;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class ProjectilePressureHandler {

    private static final Map<UUID, Long> domainCheckCache = new HashMap<>();
    private static final Map<UUID, Boolean> domainResultCache = new HashMap<>();
    private static final long DOMAIN_CACHE_DURATION = 20;

    // ★★★ 领域增幅缓存 ★★★
    private static final Map<UUID, Long> amplificationCheckCache = new HashMap<>();
    private static final Map<UUID, Boolean> amplificationResultCache = new HashMap<>();

    // ★★★ 高速投射物预检测范围 ★★★
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

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Set<UUID> currentlyAffected = new HashSet<>();

        // ==================== 1. 正常范围内的投射物 ====================
        List<Projectile> projectilesInRange = owner.level().getEntitiesOfClass(
                Projectile.class,
                owner.getBoundingBox().inflate(maxRange),
                projectile -> shouldAffect(owner, projectile)
        );

        for (Projectile projectile : projectilesInRange) {
            if (stateManager.isRepelled(projectile.getUUID())) continue;

            double distance = ownerCenter.distanceTo(projectile.position());
            if (distance > maxRange) continue;

            currentlyAffected.add(projectile.getUUID());

            initializeControlIfNeeded(projectile, owner, ownerCenter, distance, maxRange,
                    pressureLevel, cursedEnergyOutput, stateManager);
        }

        // ==================== 2. 高速接近的投射物（额外检测）====================
        preSlowHighSpeedProjectiles(owner, ownerCenter, maxRange, pressureLevel, stateManager);

        // ==================== 3. 释放超出范围的 ====================
        releaseOutOfRange(owner, maxRange, currentlyAffected, stateManager);

        // ★★★ 定期清理缓存 ★★★
        if (owner.tickCount % 100 == 0) {
            cleanupCaches(owner.level().getGameTime());
        }
    }

    /**
     * ★★★ 预处理高速接近的投射物 ★★★
     */
    private static void preSlowHighSpeedProjectiles(LivingEntity owner, Vec3 ownerCenter,
                                                    double maxRange, int pressureLevel,
                                                    PressureStateManager stateManager) {
        double outerRange = maxRange + HIGH_SPEED_EXTRA_RANGE;

        List<Projectile> outerProjectiles = owner.level().getEntitiesOfClass(
                Projectile.class,
                owner.getBoundingBox().inflate(outerRange),
                projectile -> shouldAffect(owner, projectile)
        );

        for (Projectile projectile : outerProjectiles) {
            if (stateManager.isRepelled(projectile.getUUID())) continue;
            if (!(projectile instanceof IFrozenProjectile fp)) continue;

            // 已经被控制的跳过
            if (fp.jujutsuAddon$isControlled()) continue;

            double distance = ownerCenter.distanceTo(projectile.position());

            // 只处理在 maxRange 外但在 outerRange 内的
            if (distance <= maxRange || distance > outerRange) continue;

            // 检查是否高速接近
            Vec3 velocity = projectile.getDeltaMovement();
            double speed = velocity.length();
            if (speed < 1.0) continue;

            Vec3 toOwner = ownerCenter.subtract(projectile.position()).normalize();
            double approachSpeed = velocity.dot(toOwner);
            if (approachSpeed <= 0.5) continue;

            // 计算到达边界的帧数
            double framesToReach = (distance - maxRange) / approachSpeed;
            if (framesToReach > 3) continue;

            // ★★★ 提前减速！★★★
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
        // ★★★ 计算当前等级对应的边界参数 ★★★
        float currentStopDistance = (float) PressureConfig.getStopZoneRadius(pressureLevel);
        float currentMaxRange = (float) maxRange;
        if (!wasControlled) {
            // 第一次被控制，初始化
            fp.jujutsuAddon$setOriginalVelocity(projectile.getDeltaMovement());
            fp.jujutsuAddon$lockRotation(projectile.getYRot(), projectile.getXRot());
            fp.jujutsuAddon$setSpeedMultiplier((float) PressureConfig.getProjectileEntrySpeed());
            fp.jujutsuAddon$setControlled(true);
            fp.jujutsuAddon$setFreezeOwner(owner.getUUID());
            projectile.setNoGravity(true);
            if (PressureConfig.areSoundsEnabled()) {
                projectile.level().playSound(null,
                        projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 0.3F, 1.5F);
            }
            spawnSlowdownParticles(projectile, 1.0);
        }
        // ★★★ 关键：每帧都更新边界参数（即使已被控制）★★★
        // 这样当等级变化时，投射物会知道新的边界位置
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
        if (projectile instanceof IFrozenProjectile fp) {
            fp.jujutsuAddon$setControlled(false);
        }

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
        // 自己的投射物不拦截
        if (projectileOwner == owner) return false;
        // 自己召唤物的投射物不拦截
        if (projectileOwner instanceof SummonEntity summon && summon.getOwner() == owner) {
            return false;
        }
        // 自己驯服动物的投射物不拦截
        if (projectileOwner instanceof TamableAnimal tamable &&
                tamable.isTame() && tamable.getOwner() == owner) {
            return false;
        }
        // ★★★ 新增：无下限冲突检测 ★★★
        InfinityConflictResolver.ConflictResult conflict =
                InfinityConflictResolver.resolveProjectileConflict(owner, projectile);
        if (!conflict.canAffect) {
            // 投射物发射者的无下限更强或相等，无法拦截
            return false;
        }
        // 投射物发射者相关检查
        if (projectileOwner instanceof LivingEntity livingOwner) {
            // 领域必中检查
            if (isInOwnersDomainWithSureHit(owner, livingOwner)) {
                return false;
            }
            // 领域增幅检查
            if (PressureConfig.respectDomainAmplification()) {
                if (hasDomainAmplification(livingOwner)) {
                    return false;
                }
            }
        }
        // JJK 的 isBlockable 检查
        try {
            if (!HelperMethods.isBlockable(owner, projectile)) {
                return false;
            }
        } catch (Exception ignored) {}
        // JJK 领域技能检查
        if (projectile instanceof JujutsuProjectile jujutsu) {
            try {
                if (jujutsu.isDomain()) {
                    return false;
                }
            } catch (Exception ignored) {}
        }
        return true;
    }

    // ==================== 领域增幅检查（带缓存）====================

    private static boolean hasDomainAmplification(LivingEntity entity) {
        UUID cacheKey = entity.getUUID();
        long currentTime = entity.level().getGameTime();

        // 检查缓存
        Long lastCheck = amplificationCheckCache.get(cacheKey);
        if (lastCheck != null && currentTime - lastCheck < DOMAIN_CACHE_DURATION) {
            Boolean cachedResult = amplificationResultCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        // 执行检查
        boolean result = doCheckDomainAmplification(entity);

        // 缓存结果
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

    // ==================== 领域必中检查（带缓存）====================

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
        // 清理领域检查缓存
        domainCheckCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > DOMAIN_CACHE_DURATION * 5);
        domainResultCache.keySet().retainAll(domainCheckCache.keySet());

        // 清理领域增幅缓存
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
