package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ControlledProjectileTick {

    private static final float BUFFER_ZONE = 0.3f;

    // 玩家位置追踪
    private static final Map<UUID, PlayerMovementData> PLAYER_MOVEMENT = new ConcurrentHashMap<>();

    private static class PlayerMovementData {
        Vec3 lastPosition;
        Vec3 movement = Vec3.ZERO;
        long lastTick = -1;

        PlayerMovementData(Vec3 pos) {
            this.lastPosition = pos;
        }
    }

    public static void updatePlayerMovement(Player player) {
        if (player.level().isClientSide) return;

        UUID uuid = player.getUUID();
        Vec3 currentPos = player.position();
        long currentTick = player.level().getGameTime();

        PlayerMovementData data = PLAYER_MOVEMENT.get(uuid);
        if (data == null) {
            PLAYER_MOVEMENT.put(uuid, new PlayerMovementData(currentPos));
            return;
        }

        if (data.lastTick != currentTick) {
            data.movement = currentPos.subtract(data.lastPosition);
            data.lastPosition = currentPos;
            data.lastTick = currentTick;
        }
    }

    private static Vec3 getPlayerMovement(UUID uuid) {
        PlayerMovementData data = PLAYER_MOVEMENT.get(uuid);
        return data != null ? data.movement : Vec3.ZERO;
    }

    // ==================== 主入口 ====================

    public static void tick(Projectile projectile) {
        if (!(projectile instanceof IFrozenProjectile fp)) return;

        // ★★★ 和参考代码一样，不区分客户端/服务端 ★★★
        // Mixin已经在服务端调用，客户端会通过MC默认同步收到位置
        tickProjectile(projectile, fp);
    }

    public static void tickHurtingProjectile(AbstractHurtingProjectile projectile) {
        if (!(projectile instanceof IFrozenProjectile fp)) return;

        projectile.xPower = 0;
        projectile.yPower = 0;
        projectile.zPower = 0;
        projectile.setNoGravity(true);

        tickProjectile(projectile, fp);
    }

    // ==================== 核心逻辑 ====================

    private static void tickProjectile(Projectile projectile, IFrozenProjectile fp) {
        Vec3 deltaMovement = projectile.getDeltaMovement();

        // 初始化旋转（和参考代码一样）
        if (projectile.xRotO == 0.0F && projectile.yRotO == 0.0F) {
            double horizontalDist = deltaMovement.horizontalDistance();
            projectile.setYRot((float) (Mth.atan2(deltaMovement.x, deltaMovement.z) * 180.0F / (float) Math.PI));
            projectile.setXRot((float) (Mth.atan2(deltaMovement.y, horizontalDist) * 180.0F / (float) Math.PI));
            projectile.yRotO = projectile.getYRot();
            projectile.xRotO = projectile.getXRot();
        }

        // 方块碰撞检测
        BlockPos blockPos = projectile.blockPosition();
        BlockState blockState = projectile.level().getBlockState(blockPos);
        if (!blockState.isAir()) {
            VoxelShape shape = blockState.getCollisionShape(projectile.level(), blockPos);
            if (!shape.isEmpty()) {
                Vec3 pos = projectile.position();
                for (AABB aabb : shape.toAabbs()) {
                    if (aabb.move(blockPos).contains(pos)) {
                        if (projectile instanceof AbstractArrow arrow) {
                            setArrowInGround(arrow, true);
                        }
                        fp.jujutsuAddon$setControlled(false);
                        return;
                    }
                }
            }
        }

        if (projectile instanceof AbstractArrow arrow && arrow.shakeTime > 0) {
            arrow.shakeTime--;
        }

        if (projectile.isInWaterOrRain() || blockState.is(Blocks.POWDER_SNOW)) {
            projectile.clearFire();
        }

        if (projectile instanceof AbstractArrow arrow && isArrowInGround(arrow)) {
            return;
        }

        // 获取速度倍率
        float speedMod = fp.jujutsuAddon$getSpeedMultiplier();
        Vec3 position = projectile.position();

        // 钓鱼钩重力
        if (projectile instanceof FishingHook) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().add(0.0, -0.03 * speedMod, 0.0));
        }

        Vec3 reducedDelta = deltaMovement.scale(speedMod);

        // ==================== 距离检测和处理 ====================
        UUID ownerUUID = fp.jujutsuAddon$getFreezeOwner();
        LivingEntity owner = findOwner(projectile, ownerUUID);

        if (owner == null) {
            releaseProjectile(projectile, fp);
            return;
        }

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        double distance = position.distanceTo(ownerCenter);

        float stopDistance = fp.jujutsuAddon$getStopDistance();
        float maxRange = fp.jujutsuAddon$getMaxRange();

        // 超出范围释放
        if (distance > maxRange + 0.5) {
            releaseProjectile(projectile, fp);
            return;
        }

        // ==================== 碰撞检测（和参考代码一样）====================
        if (speedMod > 0.01) {
            Vec3 pos = position;
            Vec3 pos2 = position.add(deltaMovement).add(deltaMovement).add(reducedDelta);
            float inflateDist = (float) Math.max(Math.max(Math.abs(reducedDelta.x), Math.abs(reducedDelta.y)), Math.abs(reducedDelta.z)) * 2;

            // 第一次检测
            EntityHitResult mobHit = ProjectileUtil.getEntityHitResult(
                    projectile.level(), projectile, pos, pos2,
                    projectile.getBoundingBox().expandTowards(deltaMovement).inflate(1 + inflateDist),
                    entity -> canHitEntity(projectile, entity)
            );
            if (mobHit != null) {
                speedMod *= 0.7F;
            }

            // 第二次检测
            reducedDelta = deltaMovement.scale(speedMod);
            pos2 = position.add(deltaMovement).add(reducedDelta);
            EntityHitResult mobHit2 = ProjectileUtil.getEntityHitResult(
                    projectile.level(), projectile, pos, pos2,
                    projectile.getBoundingBox().expandTowards(deltaMovement).inflate(1),
                    entity -> canHitEntity(projectile, entity)
            );
            if (mobHit2 != null) {
                speedMod *= 0.6F;
            }

            if (mobHit2 != null && mobHit != null) {
                if (speedMod <= 0.1) {
                    speedMod = 0.11F;
                }
                fp.jujutsuAddon$setSpeedMultiplier(speedMod);
            }
        }

        // 方块碰撞检测
        Vec3 endPos = position.add(deltaMovement);
        HitResult blockHit = projectile.level().clip(new ClipContext(
                position, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            endPos = blockHit.getLocation();
        }

        // 实体碰撞
        EntityHitResult entityHit = findHitEntity(projectile, position, endPos, deltaMovement);
        HitResult finalHit = entityHit != null ? entityHit : blockHit;

        if (finalHit != null && finalHit.getType() == HitResult.Type.ENTITY) {
            Entity hitEntity = ((EntityHitResult) finalHit).getEntity();
            Entity projectileOwner = projectile.getOwner();
            if (hitEntity instanceof Player targetPlayer && projectileOwner instanceof Player ownerPlayer) {
                if (!ownerPlayer.canHarmPlayer(targetPlayer)) {
                    finalHit = null;
                }
            }
        }

        // 碰撞后释放
        if (finalHit != null && finalHit.getType() != HitResult.Type.MISS) {
            fp.jujutsuAddon$setSpeedMultiplier(0);
            fp.jujutsuAddon$setControlled(false);
            return;
        }

        // ==================== 停止区处理 ====================
        if (distance <= stopDistance) {
            fp.jujutsuAddon$setSpeedMultiplier(0);

            // 计算推动
            Vec3 pushVelocity = calculatePushVelocity(owner, ownerCenter, position, stopDistance);

            if (pushVelocity.lengthSqr() > 0.001) {
                Vec3 newPos = position.add(pushVelocity);
                BlockPos bp = BlockPos.containing(newPos);
                if (projectile.level().getBlockState(bp).isAir()) {
                    // ★★★ 直接setPos，不设置hurtMarked ★★★
                    projectile.setPos(newPos.x, newPos.y, newPos.z);
                    updateRotation(projectile, pushVelocity);
                }
            }

            projectile.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // ==================== 减速区处理 ====================
        float targetSpeed = calculateSpeedFromDistance((float) distance, stopDistance, maxRange);
        speedMod = fp.jujutsuAddon$getSpeedMultiplier();

        if (speedMod > targetSpeed) {
            speedMod = Math.max(targetSpeed, speedMod * 0.87F);  // ★★★ 和参考代码一样用0.87 ★★★
        } else {
            speedMod = targetSpeed;
        }

        fp.jujutsuAddon$setSpeedMultiplier(speedMod);
        reducedDelta = deltaMovement.scale(speedMod);

        // ★★★ 关键：和参考代码一样，只有速度>0.01才移动 ★★★
        if (speedMod > 0.01) {
            // ★★★ 直接setPos，不设置hurtMarked，不设置xOld ★★★
            projectile.setPos(
                    position.x + reducedDelta.x,
                    position.y + reducedDelta.y,
                    position.z + reducedDelta.z
            );

            // 更新旋转
            if (reducedDelta.lengthSqr() > 0.0001) {
                updateRotation(projectile, reducedDelta);
            }
        } else {
            // 速度太小，释放
            releaseProjectile(projectile, fp);
        }
    }

    // ==================== 辅助方法 ====================

    private static Vec3 calculatePushVelocity(LivingEntity owner,
                                              Vec3 ownerCenter, Vec3 projectilePos,
                                              float stopDistance) {
        Vec3 ownerMovement = getPlayerMovement(owner.getUUID());
        double ownerSpeed = ownerMovement.length();

        Vec3 toProjectile = projectilePos.subtract(ownerCenter);
        double distToProjectile = toProjectile.length();

        if (distToProjectile < 0.1 || ownerSpeed < 0.005) {
            return Vec3.ZERO;
        }

        Vec3 pushDir = toProjectile.normalize();
        double approachSpeed = ownerMovement.dot(pushDir);

        if (approachSpeed <= 0) {
            return Vec3.ZERO;
        }

        double distance = projectilePos.distanceTo(ownerCenter);
        double targetDist = stopDistance + 0.3;

        if (distance >= targetDist) {
            return Vec3.ZERO;
        }

        double maxPushSpeed = PressureConfig.getMaxPushForce();
        double basePushSpeed = PressureConfig.getBasePushForce();

        double pushSpeed = approachSpeed * 1.5;
        pushSpeed = Math.min(pushSpeed, maxPushSpeed);
        pushSpeed = Math.max(pushSpeed, basePushSpeed);

        return pushDir.scale(pushSpeed);
    }

    private static float calculateSpeedFromDistance(float distance, float stopDistance, float maxRange) {
        if (distance <= stopDistance) return 0f;

        float entrySpeed = (float) PressureConfig.getProjectileEntrySpeed();
        float stopSpeed = (float) PressureConfig.getProjectileStopSpeed();
        float bufferEnd = stopDistance + BUFFER_ZONE;

        if (distance <= bufferEnd) {
            float bufferRatio = (distance - stopDistance) / BUFFER_ZONE;
            return stopSpeed + bufferRatio * (0.10f - stopSpeed);
        }

        float slowdownZoneSize = maxRange - bufferEnd;
        if (slowdownZoneSize < 0.5f) slowdownZoneSize = 0.5f;

        float distanceFromBuffer = distance - bufferEnd;
        float t = Math.min(1.0f, distanceFromBuffer / slowdownZoneSize);
        float easeOut = 1.0f - (1.0f - t) * (1.0f - t);
        return 0.10f + (entrySpeed - 0.10f) * easeOut;
    }

    private static void updateRotation(Projectile projectile, Vec3 velocity) {
        Vec3 direction = velocity.normalize();
        double horizDist = direction.horizontalDistance();

        if (horizDist < 0.001) return;

        float targetYaw = (float) (Mth.atan2(direction.x, direction.z) * (180.0 / Math.PI));
        float targetPitch = (float) (Mth.atan2(direction.y, horizDist) * (180.0 / Math.PI));

        // ★★★ 平滑插值，和参考代码的lerpRotation类似 ★★★
        projectile.setYRot(lerpRotation(projectile.getYRot(), targetYaw));
        projectile.setXRot(Mth.clamp(lerpRotation(projectile.getXRot(), targetPitch), -90.0F, 90.0F));
    }

    // ★★★ 从参考代码复制的旋转插值 ★★★
    private static float lerpRotation(float current, float target) {
        while (target - current < -180.0F) {
            current -= 360.0F;
        }
        while (target - current >= 180.0F) {
            current += 360.0F;
        }
        return Mth.lerp(0.2F, current, target);
    }

    private static void releaseProjectile(Projectile projectile, IFrozenProjectile fp) {
        fp.jujutsuAddon$setControlled(false);
        projectile.setNoGravity(false);

        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = 0;
            hurting.yPower = -0.03;
            hurting.zPower = 0;
            hurting.setDeltaMovement(0, -0.1, 0);
        } else {
            Vec3 originalVel = fp.jujutsuAddon$getOriginalVelocity();
            if (originalVel != null && originalVel.lengthSqr() > 0.01) {
                projectile.setDeltaMovement(
                        originalVel.x * 0.3,
                        Math.min(originalVel.y * 0.3, -0.05),
                        originalVel.z * 0.3
                );
            } else {
                projectile.setDeltaMovement(0, -0.1, 0);
            }
        }
    }

    @Nullable
    private static LivingEntity findOwner(Projectile projectile, @Nullable UUID ownerUUID) {
        if (ownerUUID == null) return null;

        Player player = projectile.level().getPlayerByUUID(ownerUUID);
        if (player != null) return player;

        float maxRange = 20f;
        if (projectile instanceof IFrozenProjectile fp) {
            maxRange = fp.jujutsuAddon$getMaxRange() * 2;
        }

        for (Entity entity : projectile.level().getEntities(
                projectile,
                projectile.getBoundingBox().inflate(maxRange),
                e -> e instanceof LivingEntity && e.getUUID().equals(ownerUUID))) {
            return (LivingEntity) entity;
        }
        return null;
    }

    @Nullable
    private static EntityHitResult findHitEntity(Projectile projectile, Vec3 start, Vec3 end, Vec3 delta) {
        return ProjectileUtil.getEntityHitResult(
                projectile.level(), projectile, start, end,
                projectile.getBoundingBox().expandTowards(delta).inflate(1.0),
                entity -> canHitEntity(projectile, entity)
        );
    }

    private static boolean canHitEntity(Projectile projectile, Entity entity) {
        if (!entity.isSpectator() && entity.isAlive() && entity.isPickable()) {
            Entity owner = projectile.getOwner();
            return owner == null || !owner.isPassengerOfSameVehicle(entity);
        }
        return false;
    }

    private static boolean isArrowInGround(AbstractArrow arrow) {
        try {
            java.lang.reflect.Field field = AbstractArrow.class.getDeclaredField("inGround");
            field.setAccessible(true);
            return field.getBoolean(arrow);
        } catch (Exception e) {
            return false;
        }
    }

    private static void setArrowInGround(AbstractArrow arrow, boolean value) {
        try {
            java.lang.reflect.Field field = AbstractArrow.class.getDeclaredField("inGround");
            field.setAccessible(true);
            field.setBoolean(arrow, value);
        } catch (Exception e) {
            // ignore
        }
    }
}
