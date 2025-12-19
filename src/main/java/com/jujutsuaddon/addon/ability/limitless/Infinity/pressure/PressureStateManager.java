package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PressureStateManager {

    // ==================== 碰撞状态变化枚举 ====================
    public enum CollisionStateChange {
        NOT_COLLIDING,      // 未碰撞
        JUST_COLLIDED,      // 刚进入碰撞
        STILL_COLLIDING,    // 持续碰撞中
        JUST_RELEASED       // 刚脱离碰撞（触发突破伤害！）
    }

    // ==================== 实体状态 ====================
    private final Map<UUID, Vec3> previousVelocities = new HashMap<>();
    private final Map<UUID, Boolean> confirmedColliding = new HashMap<>();  // 经过防抖确认的碰撞状态
    private final Map<UUID, Set<BlockPos>> entityCollidingBlocks = new HashMap<>();
    private final Map<UUID, Integer> damageCooldowns = new HashMap<>();
    private final Map<UUID, Integer> pinnedTicks = new HashMap<>();
    private final Map<UUID, Double> previousPressure = new HashMap<>();
    private final Map<UUID, Integer> damageWarningTicks = new HashMap<>();

    // ==================== 状态机新增字段 ====================
    private final Map<UUID, Double> peakPressure = new HashMap<>();           // 碰撞期间的峰值压力
    private final Map<UUID, Integer> collisionFrames = new HashMap<>();       // 连续碰撞帧数
    private final Map<UUID, Integer> noCollisionFrames = new HashMap<>();     // 连续非碰撞帧数

    // 碰撞状态确认所需的帧数（防抖）
    private static final int COLLISION_CONFIRM_FRAMES = 2;
    private static final int NO_COLLISION_CONFIRM_FRAMES = 2;

    // ==================== 玩家状态 ====================
    private final Map<UUID, Vec3> ownerPreviousPos = new HashMap<>();

    // ==================== 方块状态 ====================
    private final Map<BlockPos, Float> blockPressureAccum = new HashMap<>();
    private final Map<BlockPos, Long> blockLastPressureTime = new HashMap<>();
    private final Map<BlockPos, Integer> blockBreakerId = new HashMap<>();
    private int breakerIdCounter = 0;

    // ==================== 投射物状态 ====================
    private final Set<UUID> trackedProjectiles = new HashSet<>();
    private final Map<UUID, Integer> repelledProjectiles = new HashMap<>();

    // ==================== 状态机核心方法 ====================

    /**
     * 更新碰撞状态，返回经过防抖处理后的状态变化
     * @param entityId 实体ID
     * @param rawColliding 原始碰撞检测结果（本帧是否检测到碰撞方块）
     * @return 经过防抖确认的碰撞状态变化
     */
    public CollisionStateChange updateCollisionState(UUID entityId, boolean rawColliding) {
        boolean wasConfirmedColliding = confirmedColliding.getOrDefault(entityId, false);

        if (rawColliding) {
            // 检测到碰撞
            int frames = collisionFrames.getOrDefault(entityId, 0) + 1;
            collisionFrames.put(entityId, frames);
            noCollisionFrames.put(entityId, 0);  // 重置非碰撞计数

            // 连续碰撞足够帧数后确认
            if (frames >= COLLISION_CONFIRM_FRAMES) {
                confirmedColliding.put(entityId, true);
            }
        } else {
            // 未检测到碰撞
            int frames = noCollisionFrames.getOrDefault(entityId, 0) + 1;
            noCollisionFrames.put(entityId, frames);
            collisionFrames.put(entityId, 0);  // 重置碰撞计数

            // 连续不碰撞足够帧数后确认
            if (frames >= NO_COLLISION_CONFIRM_FRAMES) {
                confirmedColliding.put(entityId, false);
            }
        }

        boolean isConfirmedColliding = confirmedColliding.getOrDefault(entityId, false);

        // 检测状态变化
        if (wasConfirmedColliding && !isConfirmedColliding) {
            return CollisionStateChange.JUST_RELEASED;  // 刚脱离碰撞
        } else if (!wasConfirmedColliding && isConfirmedColliding) {
            return CollisionStateChange.JUST_COLLIDED;  // 刚进入碰撞
        } else if (isConfirmedColliding) {
            return CollisionStateChange.STILL_COLLIDING;  // 持续碰撞中
        } else {
            return CollisionStateChange.NOT_COLLIDING;  // 未碰撞
        }
    }

    /**
     * 获取当前确认的碰撞状态
     */
    public boolean isConfirmedColliding(UUID entityId) {
        return confirmedColliding.getOrDefault(entityId, false);
    }

    // ==================== 峰值压力方法 ====================

    /**
     * 更新峰值压力（取最大值）
     */
    public void updatePeakPressure(UUID entityId, double currentPressure) {
        double peak = peakPressure.getOrDefault(entityId, 0.0);
        if (currentPressure > peak) {
            peakPressure.put(entityId, currentPressure);
        }
    }

    /**
     * 获取峰值压力
     */
    public double getPeakPressure(UUID entityId) {
        return peakPressure.getOrDefault(entityId, 0.0);
    }

    /**
     * 重置峰值压力
     */
    public void resetPeakPressure(UUID entityId) {
        peakPressure.remove(entityId);
    }

    // ==================== 实体状态方法 ====================

    public Vec3 getPreviousVelocity(UUID entityId) {
        return previousVelocities.getOrDefault(entityId, Vec3.ZERO);
    }

    public void setPreviousVelocity(UUID entityId, Vec3 velocity) {
        previousVelocities.put(entityId, velocity);
    }

    /**
     * @deprecated 使用 isConfirmedColliding 或 updateCollisionState 代替
     */
    @Deprecated
    public boolean wasColliding(UUID entityId) {
        return confirmedColliding.getOrDefault(entityId, false);
    }

    /**
     * @deprecated 碰撞状态现在由 updateCollisionState 自动管理
     */
    @Deprecated
    public void setColliding(UUID entityId, boolean colliding) {
        // 保留兼容性，但不再直接设置
        // confirmedColliding.put(entityId, colliding);
    }

    public Set<BlockPos> getCollidingBlocks(UUID entityId) {
        return entityCollidingBlocks.getOrDefault(entityId, Collections.emptySet());
    }

    public void setCollidingBlocks(UUID entityId, Set<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            entityCollidingBlocks.remove(entityId);
        } else {
            entityCollidingBlocks.put(entityId, new HashSet<>(blocks));
        }
    }

    public int getDamageCooldown(UUID entityId) {
        return damageCooldowns.getOrDefault(entityId, 0);
    }

    public void setDamageCooldown(UUID entityId, int cooldown) {
        if (cooldown <= 0) {
            damageCooldowns.remove(entityId);
        } else {
            damageCooldowns.put(entityId, cooldown);
        }
    }

    public void decrementDamageCooldown(UUID entityId) {
        int current = damageCooldowns.getOrDefault(entityId, 0);
        if (current > 0) {
            damageCooldowns.put(entityId, current - 1);
        }
    }

    public int getPinnedTicks(UUID entityId) {
        return pinnedTicks.getOrDefault(entityId, 0);
    }

    public void incrementPinnedTicks(UUID entityId) {
        pinnedTicks.put(entityId, getPinnedTicks(entityId) + 1);
    }

    public void resetPinnedTicks(UUID entityId) {
        pinnedTicks.remove(entityId);
    }

    public double getPreviousPressure(UUID entityId) {
        return previousPressure.getOrDefault(entityId, 0.0);
    }

    public void setPreviousPressure(UUID entityId, double pressure) {
        previousPressure.put(entityId, pressure);
    }

    public int getDamageWarningTicks(UUID entityId) {
        return damageWarningTicks.getOrDefault(entityId, 0);
    }

    public void setDamageWarningTicks(UUID entityId, int ticks) {
        if (ticks <= 0) {
            damageWarningTicks.remove(entityId);
        } else {
            damageWarningTicks.put(entityId, ticks);
        }
    }

    public void incrementDamageWarningTicks(UUID entityId) {
        damageWarningTicks.put(entityId, getDamageWarningTicks(entityId) + 1);
    }

    // ==================== 玩家状态方法 ====================

    public Vec3 calculateOwnerMovement(LivingEntity owner) {
        UUID ownerId = owner.getUUID();
        Vec3 prevPos = ownerPreviousPos.getOrDefault(ownerId, owner.position());
        Vec3 currentPos = owner.position();
        Vec3 movement = currentPos.subtract(prevPos);
        ownerPreviousPos.put(ownerId, currentPos);
        return movement;
    }

    // ==================== 方块状态方法 ====================

    public float getBlockPressure(BlockPos pos) {
        return blockPressureAccum.getOrDefault(pos, 0F);
    }

    public void addBlockPressure(BlockPos pos, float amount) {
        float current = blockPressureAccum.getOrDefault(pos, 0F);
        blockPressureAccum.put(pos, current + amount);
        blockLastPressureTime.put(pos, System.currentTimeMillis());
    }

    public void setBlockPressure(BlockPos pos, float pressure) {
        if (pressure <= 0) {
            blockPressureAccum.remove(pos);
            blockLastPressureTime.remove(pos);
        } else {
            blockPressureAccum.put(pos, pressure);
        }
    }

    public Long getBlockLastPressureTime(BlockPos pos) {
        return blockLastPressureTime.get(pos);
    }

    public Set<BlockPos> getAllPressuredBlocks() {
        return new HashSet<>(blockPressureAccum.keySet());
    }

    public int getOrCreateBreakerId(BlockPos pos) {
        return blockBreakerId.computeIfAbsent(pos, p -> breakerIdCounter++);
    }

    public void clearBlockBreakProgress(ServerLevel level, BlockPos pos) {
        Integer breakerId = blockBreakerId.remove(pos);
        if (breakerId != null) {
            level.destroyBlockProgress(breakerId, pos, -1);
        }
        blockLastPressureTime.remove(pos);
    }

    // ==================== 投射物状态方法 ====================

    public void trackProjectile(Projectile projectile) {
        trackedProjectiles.add(projectile.getUUID());
    }

    public Set<UUID> getTrackedProjectileIds() {
        return new HashSet<>(trackedProjectiles);
    }

    public void forceRemoveProjectile(UUID projectileId) {
        trackedProjectiles.remove(projectileId);
    }

    public int getTrackedProjectileCount() {
        return trackedProjectiles.size();
    }

    // ==================== 弹开豁免机制 ====================

    public void markAsRepelled(UUID projectileId, int immuneTicks) {
        repelledProjectiles.put(projectileId, immuneTicks);
    }

    public boolean isRepelled(UUID projectileId) {
        return repelledProjectiles.containsKey(projectileId);
    }

    public void tickRepelledProjectiles() {
        if (repelledProjectiles.isEmpty()) return;

        repelledProjectiles.entrySet().removeIf(entry -> {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                return true;
            }
            entry.setValue(remaining);
            return false;
        });
    }

    // ==================== 释放投射物 ====================

    public void releaseProjectile(Projectile projectile) {
        UUID id = projectile.getUUID();
        trackedProjectiles.remove(id);

        if (projectile instanceof IFrozenProjectile fp) {
            fp.jujutsuAddon$setControlled(false);
        }

        projectile.setDeltaMovement(new Vec3(0, -0.05, 0));
        projectile.setNoGravity(false);

        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = 0;
            hurting.yPower = -0.01;
            hurting.zPower = 0;
        }

        projectile.hurtMarked = true;
    }

    public void releaseAllProjectiles(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        for (UUID id : new HashSet<>(trackedProjectiles)) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Projectile projectile) {
                releaseProjectile(projectile);
            }
        }

        trackedProjectiles.clear();
        repelledProjectiles.clear();
    }

    public void cleanupProjectiles(LivingEntity owner, double currentMaxRange) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        List<Projectile> toRelease = new ArrayList<>();
        Set<UUID> toRemove = new HashSet<>();
        Vec3 ownerPos = owner.position();

        for (UUID id : new HashSet<>(trackedProjectiles)) {
            Entity entity = level.getEntity(id);

            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                toRemove.add(id);
                continue;
            }

            if (!(entity instanceof Projectile projectile)) {
                toRemove.add(id);
                continue;
            }

            double distance = ownerPos.distanceTo(projectile.position());
            if (distance > currentMaxRange * 1.5) {
                toRelease.add(projectile);
            }
        }

        for (Projectile projectile : toRelease) {
            releaseProjectile(projectile);
        }

        for (UUID id : toRemove) {
            trackedProjectiles.remove(id);
        }
    }

    // ==================== 清理单个实体的所有状态 ====================

    public void clearEntityState(UUID entityId) {
        previousVelocities.remove(entityId);
        confirmedColliding.remove(entityId);
        entityCollidingBlocks.remove(entityId);
        damageCooldowns.remove(entityId);
        pinnedTicks.remove(entityId);
        previousPressure.remove(entityId);
        damageWarningTicks.remove(entityId);
        peakPressure.remove(entityId);
        collisionFrames.remove(entityId);
        noCollisionFrames.remove(entityId);
    }

    // ==================== 清理方法 ====================

    public void clearAll(LivingEntity owner) {
        previousVelocities.clear();
        confirmedColliding.clear();
        entityCollidingBlocks.clear();
        damageCooldowns.clear();
        pinnedTicks.clear();
        previousPressure.clear();
        damageWarningTicks.clear();
        peakPressure.clear();
        collisionFrames.clear();
        noCollisionFrames.clear();

        releaseAllProjectiles(owner);
        repelledProjectiles.clear();

        if (owner.level() instanceof ServerLevel level) {
            for (BlockPos pos : new HashSet<>(blockBreakerId.keySet())) {
                clearBlockBreakProgress(level, pos);
            }
        }

        blockPressureAccum.clear();
        blockLastPressureTime.clear();
        blockBreakerId.clear();
    }

    public void cleanup(LivingEntity owner, double range) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        // 清理实体相关状态的通用方法
        java.util.function.Predicate<Map.Entry<UUID, ?>> shouldRemove = entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || owner.distanceTo(entity) > range * 1.5;
        };

        previousVelocities.entrySet().removeIf(shouldRemove);
        confirmedColliding.entrySet().removeIf(shouldRemove);
        entityCollidingBlocks.entrySet().removeIf(shouldRemove);
        damageCooldowns.entrySet().removeIf(shouldRemove);
        pinnedTicks.entrySet().removeIf(shouldRemove);
        previousPressure.entrySet().removeIf(shouldRemove);
        damageWarningTicks.entrySet().removeIf(shouldRemove);
        peakPressure.entrySet().removeIf(shouldRemove);
        collisionFrames.entrySet().removeIf(shouldRemove);
        noCollisionFrames.entrySet().removeIf(shouldRemove);

        // 清理投射物
        cleanupProjectiles(owner, range);

        // 清理方块压力
        long currentTime = System.currentTimeMillis();
        blockPressureAccum.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            Long lastTime = blockLastPressureTime.get(pos);
            if (lastTime == null || currentTime - lastTime > PressureConfig.getPressureTimeoutMs() * 2) {
                clearBlockBreakProgress(level, pos);
                return true;
            }
            return false;
        });
    }
}
