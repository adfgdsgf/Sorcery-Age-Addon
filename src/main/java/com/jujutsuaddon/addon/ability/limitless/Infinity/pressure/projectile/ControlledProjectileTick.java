package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
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

        // 初始化旋转
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

        float speedMod = fp.jujutsuAddon$getSpeedMultiplier();
        Vec3 position = projectile.position();

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

        if (distance > maxRange + 0.5) {
            releaseProjectile(projectile, fp);
            return;
        }

        // ==================== 碰撞检测 ====================
        if (speedMod > 0.01) {
            Vec3 pos = position;
            Vec3 pos2 = position.add(deltaMovement).add(deltaMovement).add(reducedDelta);
            float inflateDist = (float) Math.max(Math.max(Math.abs(reducedDelta.x), Math.abs(reducedDelta.y)), Math.abs(reducedDelta.z)) * 2;

            EntityHitResult mobHit = ProjectileUtil.getEntityHitResult(
                    projectile.level(), projectile, pos, pos2,
                    projectile.getBoundingBox().expandTowards(deltaMovement).inflate(1 + inflateDist),
                    entity -> canHitEntity(projectile, entity)
            );
            if (mobHit != null) {
                speedMod *= 0.7F;
            }

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

        Vec3 endPos = position.add(deltaMovement);
        HitResult blockHit = projectile.level().clip(new ClipContext(
                position, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            endPos = blockHit.getLocation();
        }

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

        if (finalHit != null && finalHit.getType() != HitResult.Type.MISS) {
            fp.jujutsuAddon$setSpeedMultiplier(0);
            fp.jujutsuAddon$setControlled(false);
            return;
        }

        // ==================== ★★★ 区域判断和处理 ★★★ ====================
        if (ProjectileZoneHelper.isInStopZone(projectile, distance, stopDistance)) {
            fp.jujutsuAddon$setSpeedMultiplier(0);

            // ★★★ 1. 检查是否在缓冲区 ★★★
            if (ProjectileZoneHelper.isInBufferZone(distance, stopDistance)) {
                // 缓冲区：完全静止，不受任何力，不转向
                projectile.setDeltaMovement(Vec3.ZERO);
                return;
            }

            // ★★★ 2. 检查是否在推力区（太近了）★★★
            if (ProjectileZoneHelper.isInPushZone(distance, stopDistance)) {
                // 推力区：强力推出到缓冲区
                Vec3 toProjectile = position.subtract(ownerCenter);
                if (toProjectile.lengthSqr() < 0.01) {
                    toProjectile = new Vec3(0.1, 0.05, 0.1);
                }
                Vec3 pushDir = toProjectile.normalize();

                // 推到缓冲区中间位置
                float bufferMiddle = stopDistance - ProjectileZoneHelper.BUFFER_ZONE_THICKNESS * 0.5f;
                bufferMiddle = Math.max(bufferMiddle, 0.2f);
                double pushAmount = Math.max(0.1, bufferMiddle - distance);
                pushAmount = Math.min(pushAmount, 0.5); // 限制单次推动距离

                Vec3 newPos = position.add(pushDir.scale(pushAmount));
                BlockPos bp = BlockPos.containing(newPos);
                if (projectile.level().getBlockState(bp).isAir()) {
                    projectile.setPos(newPos.x, newPos.y, newPos.z);
                    // 推力区推出时更新旋转
                    updateRotation(projectile, pushDir);
                }

                projectile.setDeltaMovement(Vec3.ZERO);
                return;
            }

            // ★★★ 3. 静止区（缓冲区外侧）：正常的推动逻辑 ★★★
            PushResult pushResult = calculatePushVelocity(owner, ownerCenter, position, stopDistance, projectile);

            if (ProjectileZoneHelper.isSignificantPush(pushResult.velocity)) {
                Vec3 newPos = position.add(pushResult.velocity);
                BlockPos bp = BlockPos.containing(newPos);
                if (projectile.level().getBlockState(bp).isAir()) {
                    projectile.setPos(newPos.x, newPos.y, newPos.z);

                    // 只有玩家移动推动才转向
                    if (pushResult.isPlayerPush) {
                        updateRotation(projectile, pushResult.velocity);
                    }
                }
            }

            projectile.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // ==================== 减速区处理 ====================
        float entrySpeed = (float) PressureConfig.getProjectileEntrySpeed();
        float stopSpeed = (float) PressureConfig.getProjectileStopSpeed();
        float targetSpeed = ProjectileZoneHelper.calculateSlowdownSpeed(
                (float) distance, stopDistance, maxRange, entrySpeed, stopSpeed);

        speedMod = fp.jujutsuAddon$getSpeedMultiplier();

        if (speedMod > targetSpeed) {
            speedMod = Math.max(targetSpeed, speedMod * 0.87F);
        } else {
            speedMod = targetSpeed;
        }

        fp.jujutsuAddon$setSpeedMultiplier(speedMod);
        reducedDelta = deltaMovement.scale(speedMod);

        if (speedMod > 0.01) {
            projectile.setPos(
                    position.x + reducedDelta.x,
                    position.y + reducedDelta.y,
                    position.z + reducedDelta.z
            );

            if (reducedDelta.lengthSqr() > 0.0001) {
                updateRotation(projectile, reducedDelta);
            }
        } else {
            releaseProjectile(projectile, fp);
        }
    }

    // ==================== 推动结果 ====================

    /**
     * 推动计算结果
     */
    private static class PushResult {
        final Vec3 velocity;
        final boolean isPlayerPush;  // ★★★ 是否是玩家移动产生的推动 ★★★

        PushResult(Vec3 velocity, boolean isPlayerPush) {
            this.velocity = velocity;
            this.isPlayerPush = isPlayerPush;
        }

        static PushResult none() {
            return new PushResult(Vec3.ZERO, false);
        }
    }

    /**
     * 计算推动速度
     * ★★★ 只处理玩家移动产生的推动，边界推动在缓冲区已经处理 ★★★
     */
    private static PushResult calculatePushVelocity(LivingEntity owner,
                                                    Vec3 ownerCenter, Vec3 projectilePos,
                                                    float stopDistance, Projectile projectile) {
        Vec3 toProjectile = projectilePos.subtract(ownerCenter);
        double distToProjectile = toProjectile.length();

        if (distToProjectile < 0.1) {
            return new PushResult(new Vec3(0.1, 0.05, 0.1), false);
        }

        Vec3 pushDir = toProjectile.normalize();

        // ==================== 玩家移动产生的推动 ====================
        Vec3 ownerMovement = getPlayerMovement(owner.getUUID());
        double ownerSpeed = ownerMovement.length();

        // ★★★ 只有玩家明显移动时才产生推动 ★★★
        if (ownerSpeed < ProjectileZoneHelper.PLAYER_MOVEMENT_THRESHOLD) {
            return PushResult.none();
        }

        double approachSpeed = ownerMovement.dot(pushDir);

        // ★★★ 只有玩家明显接近投射物时才推动 ★★★
        if (approachSpeed < ProjectileZoneHelper.PLAYER_APPROACH_THRESHOLD) {
            return PushResult.none();
        }

        double maxPushSpeed = PressureConfig.getMaxPushForce();
        double basePushSpeed = PressureConfig.getBasePushForce();

        double ownerPush = approachSpeed * 1.5;
        ownerPush = Math.min(ownerPush, maxPushSpeed);
        ownerPush = Math.max(ownerPush, basePushSpeed * 0.5);

        return new PushResult(pushDir.scale(ownerPush), true);
    }

    // ==================== 辅助方法 ====================

    private static void updateRotation(Projectile projectile, Vec3 velocity) {
        Vec3 direction = velocity.normalize();
        double horizDist = direction.horizontalDistance();

        if (horizDist < 0.001) return;

        float targetYaw = (float) (Mth.atan2(direction.x, direction.z) * (180.0 / Math.PI));
        float targetPitch = (float) (Mth.atan2(direction.y, horizDist) * (180.0 / Math.PI));

        projectile.setYRot(lerpRotation(projectile.getYRot(), targetYaw));
        projectile.setXRot(Mth.clamp(lerpRotation(projectile.getXRot(), targetPitch), -90.0F, 90.0F));
    }

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
