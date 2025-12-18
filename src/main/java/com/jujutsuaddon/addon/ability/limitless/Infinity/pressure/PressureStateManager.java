package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

    // ==================== 投射物状态 ==================== ★ 新增
    private final Map<UUID, ProjectileState> affectedProjectiles = new HashMap<>();

    /**
     * 投射物状态记录
     */
    public static class ProjectileState {
        public final boolean originalNoGravity;  // 原始重力状态
        public final long firstAffectedTime;     // 首次受影响时间
        public double lastDistance;              // 上次距离
        public boolean isHovering;               // 是否悬浮中
        public int hoverTicks;                   // 悬浮时长
        public int immuneTicks;                  // ★ 新增：豁免时间（刚被弹开后不受影响）
        public double lastApproachSpeed;         // ★ 新增：上次接近速度（用于计算反弹）
        public ProjectileState(boolean originalNoGravity) {
            this.originalNoGravity = originalNoGravity;
            this.firstAffectedTime = System.currentTimeMillis();
            this.lastDistance = 0;
            this.isHovering = false;
            this.hoverTicks = 0;
            this.immuneTicks = 0;
            this.lastApproachSpeed = 0;
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

    // ==================== 投射物状态方法 ==================== ★ 新增

    /**
     * 开始跟踪投射物，记录其原始状态
     * @return 投射物状态（新建或已存在）
     */
    public ProjectileState trackProjectile(Projectile projectile) {
        UUID id = projectile.getUUID();
        if (!affectedProjectiles.containsKey(id)) {
            affectedProjectiles.put(id, new ProjectileState(projectile.isNoGravity()));
        }
        return affectedProjectiles.get(id);
    }

    /**
     * 获取投射物状态
     */
    public ProjectileState getProjectileState(UUID projectileId) {
        return affectedProjectiles.get(projectileId);
    }

    /**
     * 更新投射物状态
     */
    public void updateProjectileState(UUID projectileId, double distance, boolean hovering) {
        ProjectileState state = affectedProjectiles.get(projectileId);
        if (state != null) {
            state.lastDistance = distance;
            state.isHovering = hovering;
            if (hovering) {
                state.hoverTicks++;
            } else {
                state.hoverTicks = 0;
            }
        }
    }

    /**
     * 释放投射物，恢复其原始状态
     */
    public void releaseProjectile(Projectile projectile) {
        UUID id = projectile.getUUID();
        ProjectileState state = affectedProjectiles.remove(id);
        if (state != null) {
            // 恢复原始重力状态
            projectile.setNoGravity(state.originalNoGravity);
        }
    }

    /**
     * 检查投射物是否被跟踪
     */
    public boolean isProjectileTracked(UUID projectileId) {
        return affectedProjectiles.containsKey(projectileId);
    }

    /**
     * 获取所有被跟踪的投射物ID
     */
    public Set<UUID> getTrackedProjectileIds() {
        return new HashSet<>(affectedProjectiles.keySet());
    }

    /**
     * 清理超出范围的投射物
     */
    public void cleanupProjectiles(LivingEntity owner, double currentMaxRange) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        Set<UUID> toRemove = new HashSet<>();
        Vec3 ownerPos = owner.position();

        for (UUID id : affectedProjectiles.keySet()) {
            Entity entity = level.getEntity(id);

            // 投射物不存在、已死亡或已移除
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                toRemove.add(id);
                continue;
            }

            if (!(entity instanceof Projectile projectile)) {
                toRemove.add(id);
                continue;
            }

            // 投射物超出当前范围的1.5倍
            double distance = ownerPos.distanceTo(projectile.position());
            if (distance > currentMaxRange * 1.5) {
                // 超出范围，释放并恢复重力
                releaseProjectile(projectile);
                // 已经在 releaseProjectile 中移除了，这里确保移除
                toRemove.add(id);
            }
        }

        // 移除已清理的条目
        for (UUID id : toRemove) {
            affectedProjectiles.remove(id);
        }
    }

    /**
     * 释放所有被跟踪的投射物（压力关闭或等级归零时调用）
     */
    public void releaseAllProjectiles(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        for (UUID id : new HashSet<>(affectedProjectiles.keySet())) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Projectile projectile) {
                releaseProjectile(projectile);
            }
        }
        affectedProjectiles.clear();
    }

    /**
     * 获取被跟踪的投射物数量
     */
    public int getTrackedProjectileCount() {
        return affectedProjectiles.size();
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

        // ★ 释放所有投射物 ★
        releaseAllProjectiles(owner);

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

        // ★ 清理投射物 ★
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
