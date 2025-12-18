package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PressureStateManager {

    // ==================== 实体状态 ====================
    private final Map<UUID, Vec3> previousVelocities = new HashMap<>();
    private final Map<UUID, Boolean> wasColliding = new HashMap<>();
    private final Map<UUID, Set<BlockPos>> entityCollidingBlocks = new HashMap<>();
    private final Map<UUID, Integer> damageCooldowns = new HashMap<>();
    private final Map<UUID, Integer> pinnedTicks = new HashMap<>();
    private final Map<UUID, Double> previousPressure = new HashMap<>();
    private final Map<UUID, Integer> damageWarningTicks = new HashMap<>();

    // ==================== 玩家状态 ====================
    private final Map<UUID, Vec3> ownerPreviousPos = new HashMap<>();

    // ==================== 方块状态 ====================
    private final Map<BlockPos, Float> blockPressureAccum = new HashMap<>();
    private final Map<BlockPos, Long> blockLastPressureTime = new HashMap<>();
    private final Map<BlockPos, Integer> blockBreakerId = new HashMap<>();
    private int breakerIdCounter = 0;

    // ==================== 投射物状态 ====================
    private final Map<UUID, ProjectileState> projectileStates = new HashMap<>();
    private final Map<UUID, Integer> repelledProjectiles = new HashMap<>();

    public static class ProjectileState {
        public final boolean originalNoGravity;
        public final long firstAffectedTime;
        public final Vec3 originalVelocity;
        public double lastDistance;
        public boolean isFullyStopped;
        public int stoppedTicks;
        public int immuneTicks;
        public int baselinePressureLevel;
        public float baselineCursedOutput;
        public int ticksSinceBaseline;

        // 位置锁定
        public Vec3 haltPosition;
        public Vec3 lastAppliedPosition;
        public boolean positionLocked;
        public int positionResetCount;

        // ★★★ 火焰弹等特殊投射物的动力值 ★★★
        public boolean isHurtingProjectile;
        public double originalXPower;
        public double originalYPower;
        public double originalZPower;
        public boolean powerSaved;

        public ProjectileState(boolean originalNoGravity, Vec3 originalVelocity) {
            this.originalNoGravity = originalNoGravity;
            this.originalVelocity = originalVelocity;
            this.firstAffectedTime = System.currentTimeMillis();
            this.lastDistance = 0;
            this.isFullyStopped = false;
            this.stoppedTicks = 0;
            this.immuneTicks = 0;
            this.baselinePressureLevel = 0;
            this.baselineCursedOutput = 0;
            this.ticksSinceBaseline = 0;

            this.haltPosition = null;
            this.lastAppliedPosition = null;
            this.positionLocked = false;
            this.positionResetCount = 0;

            // 火焰弹相关
            this.isHurtingProjectile = false;
            this.originalXPower = 0;
            this.originalYPower = 0;
            this.originalZPower = 0;
            this.powerSaved = false;
        }

        public void lockPosition(Vec3 position) {
            this.haltPosition = position;
            this.lastAppliedPosition = position;
            this.positionLocked = true;
        }

        public void unlockPosition() {
            this.positionLocked = false;
            this.haltPosition = null;
            this.lastAppliedPosition = null;
            this.positionResetCount = 0;
        }

        public boolean updateAndCheckPosition(Vec3 currentPosition) {
            if (!positionLocked || lastAppliedPosition == null) {
                return false;
            }
            double drift = currentPosition.distanceTo(lastAppliedPosition);
            if (drift > 0.01) {
                positionResetCount++;
                return true;
            }
            return false;
        }
    }

    // ==================== 实体状态方法 ====================

    public Vec3 getPreviousVelocity(UUID entityId) {
        return previousVelocities.getOrDefault(entityId, Vec3.ZERO);
    }

    public void setPreviousVelocity(UUID entityId, Vec3 velocity) {
        previousVelocities.put(entityId, velocity);
    }

    public boolean wasColliding(UUID entityId) {
        return wasColliding.getOrDefault(entityId, false);
    }

    public void setColliding(UUID entityId, boolean colliding) {
        wasColliding.put(entityId, colliding);
    }

    public Set<BlockPos> getCollidingBlocks(UUID entityId) {
        return entityCollidingBlocks.getOrDefault(entityId, Collections.emptySet());
    }

    public void setCollidingBlocks(UUID entityId, Set<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            entityCollidingBlocks.remove(entityId);
        } else {
            entityCollidingBlocks.put(entityId, blocks);
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

    public ProjectileState trackProjectile(Projectile projectile) {
        UUID id = projectile.getUUID();
        if (!projectileStates.containsKey(id)) {
            ProjectileState state = new ProjectileState(
                    projectile.isNoGravity(),
                    projectile.getDeltaMovement()
            );
            projectileStates.put(id, state);
            return state;
        }
        return projectileStates.get(id);
    }

    public ProjectileState getProjectileState(UUID projectileId) {
        return projectileStates.get(projectileId);
    }

    public void forceRemoveProjectile(UUID projectileId) {
        projectileStates.remove(projectileId);
    }

    // ==================== 火焰弹专用方法 ====================

    /**
     * 保存火焰弹的动力值
     */
    public void saveHurtingProjectilePower(Projectile projectile, ProjectileState state) {
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            if (!state.powerSaved) {
                state.isHurtingProjectile = true;
                state.originalXPower = hurting.xPower;
                state.originalYPower = hurting.yPower;
                state.originalZPower = hurting.zPower;
                state.powerSaved = true;
            }
        }
    }

    /**
     * 清零火焰弹的动力值（停止时调用）
     */
    public void clearHurtingProjectilePower(Projectile projectile) {
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = 0;
            hurting.yPower = 0;
            hurting.zPower = 0;
        }
    }

    /**
     * 恢复火焰弹的动力值（释放时调用）
     */
    public void restoreHurtingProjectilePower(Projectile projectile, ProjectileState state) {
        if (projectile instanceof AbstractHurtingProjectile hurting && state.powerSaved) {
            hurting.xPower = state.originalXPower;
            hurting.yPower = state.originalYPower;
            hurting.zPower = state.originalZPower;
        }
    }

    /**
     * 设置火焰弹的动力值（减速/推力时调用）
     */
    public void setHurtingProjectilePower(Projectile projectile, double xPower, double yPower, double zPower) {
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = xPower;
            hurting.yPower = yPower;
            hurting.zPower = zPower;
        }
    }

    // ==================== 投射物控制方法 ====================

    /**
     * 强制保持投射物位置（包括火焰弹动力清零）
     */
    public void enforceProjectilePosition(Projectile projectile, ProjectileState state) {
        if (state == null || !state.positionLocked || state.haltPosition == null) {
            return;
        }

        // 强制设置位置
        projectile.setPos(state.haltPosition);
        projectile.setOldPosAndRot();  // 防止插值导致的抖动

        // 清零速度
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.setNoGravity(true);

        // ★★★ 清零火焰弹的动力 ★★★
        clearHurtingProjectilePower(projectile);

        // 记录应用的位置
        state.lastAppliedPosition = state.haltPosition;

        // 标记同步
        projectile.hurtMarked = true;
        projectile.hasImpulse = false;  // 防止物理引擎干扰
    }

    /**
     * 释放投射物（恢复原始状态）
     */
    public void releaseProjectile(Projectile projectile) {
        UUID id = projectile.getUUID();
        ProjectileState state = projectileStates.remove(id);
        if (state != null) {
            Vec3 currentPos = projectile.position();

            // 恢复原始重力设置
            projectile.setNoGravity(state.originalNoGravity);

            // ★★★ 恢复火焰弹的动力值 ★★★
            if (state.isHurtingProjectile && state.powerSaved) {
                restoreHurtingProjectilePower(projectile, state);
                // 火焰弹会自己根据 power 飞行，给一个小的初始速度帮助启动
                Vec3 powerDir = new Vec3(state.originalXPower, state.originalYPower, state.originalZPower);
                if (powerDir.lengthSqr() > 0.0001) {
                    projectile.setDeltaMovement(powerDir.normalize().scale(0.1));
                }
            } else {
                // 普通投射物
                if (!state.originalNoGravity) {
                    projectile.setDeltaMovement(0, -0.05, 0);
                } else {
                    projectile.setDeltaMovement(Vec3.ZERO);
                }
            }

            projectile.setPos(currentPos);
            projectile.hurtMarked = true;
            projectile.hasImpulse = true;
        }
    }

    public boolean isProjectileTracked(UUID projectileId) {
        return projectileStates.containsKey(projectileId);
    }

    public Set<UUID> getTrackedProjectileIds() {
        return new HashSet<>(projectileStates.keySet());
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

    public void cleanupProjectiles(LivingEntity owner, double currentMaxRange) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        Set<UUID> toRemove = new HashSet<>();
        Vec3 ownerPos = owner.position();

        for (UUID id : projectileStates.keySet()) {
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
                releaseProjectile(projectile);
                toRemove.add(id);
            }
        }

        for (UUID id : toRemove) {
            projectileStates.remove(id);
        }
    }

    public void releaseAllProjectiles(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        for (UUID id : new HashSet<>(projectileStates.keySet())) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Projectile projectile) {
                releaseProjectile(projectile);
            }
        }
        projectileStates.clear();
        repelledProjectiles.clear();
    }

    public int getTrackedProjectileCount() {
        return projectileStates.size();
    }

    // ==================== 清理方法 ====================

    public void clearAll(LivingEntity owner) {
        previousVelocities.clear();
        wasColliding.clear();
        entityCollidingBlocks.clear();
        damageCooldowns.clear();
        pinnedTicks.clear();
        previousPressure.clear();
        damageWarningTicks.clear();

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

        previousVelocities.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || owner.distanceTo(entity) > range * 1.5;
        });

        wasColliding.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || owner.distanceTo(entity) > range * 1.5;
        });

        entityCollidingBlocks.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || owner.distanceTo(entity) > range * 1.5;
        });

        damageCooldowns.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || owner.distanceTo(entity) > range * 1.5;
        });

        pinnedTicks.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || owner.distanceTo(entity) > range * 1.5;
        });

        previousPressure.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || owner.distanceTo(entity) > range * 1.5;
        });

        damageWarningTicks.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || owner.distanceTo(entity) > range * 1.5;
        });

        cleanupProjectiles(owner, range);

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
