package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.PlayerMovementTracker;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.RaySphereChecker;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.VelocityAnalyzer;
import com.jujutsuaddon.addon.api.ability.IFrozenProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 被控制的投射物每帧处理
 *
 * 使用工具类：
 * - VelocityAnalyzer: 速度分解和接近检测
 * - BalancePointCalculator: 芝诺倍率计算
 * - RaySphereChecker: 射线-球体相交检测（判断路线是否经过墙）
 * - PlayerMovementTracker: 玩家移动追踪
 * - PlayerPushProjectileHandler: 玩家移动推动投射物
 */
public class ControlledProjectileTick {

    // ==================== 主入口 ====================

    public static void tick(Projectile projectile) {
        if (!(projectile instanceof IFrozenProjectile fp)) return;
        if (!fp.jujutsuAddon$isControlled()) return;
        tickProjectile(projectile, fp);
    }

    public static void tickHurtingProjectile(AbstractHurtingProjectile projectile) {
        if (!(projectile instanceof IFrozenProjectile fp)) return;
        if (!fp.jujutsuAddon$isControlled()) return;

        // 火焰弹：清除 power，禁用重力
        //projectile.xPower = 0;
        //projectile.yPower = 0;
        //projectile.zPower = 0;
        projectile.setNoGravity(true);

        tickProjectileFireball(projectile, fp);
    }

    // ==================== 核心逻辑（箭矢/雪球等）====================

    private static void tickProjectile(Projectile projectile, IFrozenProjectile fp) {
        if (!fp.jujutsuAddon$isControlled()) return;

        Vec3 position = projectile.position();
        Vec3 currentVelocity = projectile.getDeltaMovement();

        // 初始化旋转
        if (projectile.xRotO == 0.0F && projectile.yRotO == 0.0F && currentVelocity.lengthSqr() > 0.01) {
            ProjectileMovementUpdater.initRotation(projectile, currentVelocity);
        }

        // 方块碰撞检查
        if (ProjectileCollisionHelper.checkBlockCollision(projectile, fp)) {
            return;
        }

        // 基础状态处理
        handleBasicProjectileState(projectile);

        // 箭矢在地面检查
        if (projectile instanceof AbstractArrow arrow && ProjectileCollisionHelper.isArrowInGround(arrow)) {
            return;
        }

        // 获取控制者
        UUID ownerUUID = fp.jujutsuAddon$getFreezeOwner();
        LivingEntity owner = findOwner(projectile, ownerUUID);

        if (owner == null) {
            releaseProjectile(projectile, fp, "owner为null");
            return;
        }

        // ★ 使用 PlayerMovementTracker 更新玩家移动 ★
        if (owner instanceof Player player) {
            PlayerMovementTracker.update(player);
        }

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        double distance = position.distanceTo(ownerCenter);
        float storedMaxRange = fp.jujutsuAddon$getMaxRange();
        double balanceRadius = fp.jujutsuAddon$getStopDistance();

        // 超出范围检查（保留0.5容差）
        if (distance > storedMaxRange) {
            releaseProjectile(projectile, fp, "超出范围");
            return;
        }

        // ★ 使用 VelocityAnalyzer 分解速度 ★
        VelocityAnalyzer.VelocityComponents velocityComponents =
                VelocityAnalyzer.decompose(position, currentVelocity, ownerCenter);

        // ★ 方向检测（使用 VelocityAnalyzer + RaySphereChecker）★
        if (!checkShouldIntercept(projectile, fp, ownerCenter, balanceRadius, velocityComponents)) {
            releaseProjectile(projectile, fp, "路线不经过墙");
            return;
        }

        // ★ 使用 BalancePointCalculator 计算芝诺倍率 ★
        double zenoMultiplier = BalancePointCalculator.calculateZenoMultiplier(
                distance, balanceRadius, storedMaxRange);

        // ★★★ 新增：如果投射物接近静止，检查玩家推动 ★★★
        if (zenoMultiplier < 1e-4) {
            PlayerPushProjectileHandler.PushResult pushResult =
                    PlayerPushProjectileHandler.handlePlayerPush(
                            projectile, fp, owner, ownerCenter, balanceRadius);

            if (pushResult == PlayerPushProjectileHandler.PushResult.RELEASE) {
                releaseProjectile(projectile, fp, "路线不经过墙");
                return;
            }

            if (pushResult == PlayerPushProjectileHandler.PushResult.PUSHED) {
                fp.jujutsuAddon$setSpeedMultiplier((float) zenoMultiplier);
                projectile.hurtMarked = true;
                return;  // 已处理，跳过后续
            }
            // NO_PUSH: 继续正常逻辑（保持静止）
        }

        // 设置速度倍率（供其他系统读取）
        fp.jujutsuAddon$setSpeedMultiplier((float) zenoMultiplier);

        // 慢放物理
        SlowMotionResult result = applySlowMotionPhysics(projectile, currentVelocity, zenoMultiplier);

        // 实体碰撞检测
        ProjectileCollisionHelper.handleEntityCollisions(projectile, result.movement, (float) zenoMultiplier);

        // 命中检测
        if (ProjectileCollisionHelper.handleHitDetection(projectile, fp, position, result.movement)) {
            return;
        }

        // 移动投射物
        Vec3 newPos = position.add(result.movement);
        projectile.setPos(newPos.x, newPos.y, newPos.z);

        // 更新速度为下一帧的速度
        projectile.setDeltaMovement(result.nextVelocity);

        // 更新旋转
        if (result.movement.lengthSqr() > 0.0001) {
            ProjectileMovementUpdater.updateRotation(projectile, result.movement);
        }

        projectile.hurtMarked = true;
    }

    // ==================== 火焰弹处理 ====================

    private static void tickProjectileFireball(AbstractHurtingProjectile projectile, IFrozenProjectile fp) {

        Vec3 position = projectile.position();
        Vec3 currentVelocity = projectile.getDeltaMovement();
        UUID ownerUUID = fp.jujutsuAddon$getFreezeOwner();
        LivingEntity owner = findOwner(projectile, ownerUUID);
        if (owner == null) {
            releaseProjectile(projectile, fp, "owner为null");
            return;
        }
        if (owner instanceof Player player) {
            PlayerMovementTracker.update(player);
        }
        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        double distance = position.distanceTo(ownerCenter);
        float storedMaxRange = fp.jujutsuAddon$getMaxRange();
        double balanceRadius = fp.jujutsuAddon$getStopDistance();
        if (distance > storedMaxRange + 0.5) {
            releaseProjectile(projectile, fp, "超出范围");
            return;
        }
        VelocityAnalyzer.VelocityComponents velocityComponents =
                VelocityAnalyzer.decompose(position, currentVelocity, ownerCenter);
        if (!checkShouldIntercept(projectile, fp, ownerCenter, balanceRadius, velocityComponents)) {
            releaseProjectile(projectile, fp, "路线不经过墙");
            return;
        }
        double zenoMultiplier = BalancePointCalculator.calculateZenoMultiplier(
                distance, balanceRadius, storedMaxRange);

        // ★★★ 推动检查 ★★★
        if (zenoMultiplier < 1e-4) {
            PlayerPushProjectileHandler.PushResult pushResult =
                    PlayerPushProjectileHandler.handlePlayerPush(
                            projectile, fp, owner, ownerCenter, balanceRadius);
            if (pushResult == PlayerPushProjectileHandler.PushResult.RELEASE) {
                releaseProjectile(projectile, fp, "路线不经过墙");
                return;
            }
            if (pushResult == PlayerPushProjectileHandler.PushResult.PUSHED) {
                fp.jujutsuAddon$setSpeedMultiplier((float) zenoMultiplier);
                projectile.hurtMarked = true;
                return;
            }
        }

        // ★★★ 检查是否被推动过（方向已改变）★★★
        boolean shouldClearPower = true;
        Vec3 currentDir = fp.jujutsuAddon$getCurrentDirection();
        Vec3 originalVel = fp.jujutsuAddon$getOriginalVelocity();

        if (currentDir != null && originalVel != null && originalVel.lengthSqr() > 0.01) {
            double dot = currentDir.normalize().dot(originalVel.normalize());
            if (dot < 0.99) {
                // 方向已改变（被推动过），保留 power
                shouldClearPower = false;
            }
        }

        if (shouldClearPower) {
            projectile.xPower = 0;
            projectile.yPower = 0;
            projectile.zPower = 0;
        }

        // 移动量 = 当前速度 × 时间缩放
        Vec3 movement = currentVelocity.scale(zenoMultiplier);
        // 命中检测
        if (ProjectileCollisionHelper.handleHitDetection(projectile, fp, position, movement)) {
            return;
        }
        // 移动
        Vec3 newPos = position.add(movement);
        projectile.setPos(newPos.x, newPos.y, newPos.z);
        // 更新旋转
        if (movement.lengthSqr() > 0.0001) {
            ProjectileMovementUpdater.updateRotation(projectile, movement);
        }
        fp.jujutsuAddon$setSpeedMultiplier((float) zenoMultiplier);
        projectile.hurtMarked = true;
    }

    // ==================== 慢放物理 ====================

    private static class SlowMotionResult {
        final Vec3 movement;
        final Vec3 nextVelocity;

        SlowMotionResult(Vec3 movement, Vec3 nextVelocity) {
            this.movement = movement;
            this.nextVelocity = nextVelocity;
        }
    }

    private static SlowMotionResult applySlowMotionPhysics(Projectile projectile,
                                                           Vec3 currentVelocity,
                                                           double timeScale) {
        double gravity = ProjectileDragHelper.getGravity(projectile);
        double drag = ProjectileDragHelper.getDrag(projectile);

        // 移动量 = 当前速度 × 时间缩放
        Vec3 movement = currentVelocity.scale(timeScale);

        // 慢放的物理衰减
        double scaledGravity = gravity * timeScale;
        double scaledDrag = Math.pow(drag, timeScale);

        double nextY = (currentVelocity.y - scaledGravity) * scaledDrag;
        double nextX = currentVelocity.x * scaledDrag;
        double nextZ = currentVelocity.z * scaledDrag;

        Vec3 nextVelocity = new Vec3(nextX, nextY, nextZ);

        return new SlowMotionResult(movement, nextVelocity);
    }

    // ==================== 方向检测 ====================

    /**
     * 判断投射物是否应该被拦截
     *
     * 使用两种方法：
     * 1. VelocityAnalyzer：当前速度是否在接近玩家
     * 2. RaySphereChecker：原始飞行路线是否会穿过平衡点
     */
    private static boolean checkShouldIntercept(Projectile projectile,
                                                IFrozenProjectile fp,
                                                Vec3 ownerCenter,
                                                double balanceRadius,
                                                VelocityAnalyzer.VelocityComponents velocityComponents) {

        // ★★★ 首先：获取原始飞行方向 ★★★
        Vec3 originalDirection = fp.jujutsuAddon$getCurrentDirection();
        if (originalDirection == null) {
            Vec3 originalVelocity = fp.jujutsuAddon$getOriginalVelocity();
            if (originalVelocity != null && originalVelocity.lengthSqr() > 0.0001) {
                originalDirection = originalVelocity.normalize();
            }
        }

        // ★★★ 检查原始方向是否朝向玩家 ★★★
        if (originalDirection != null && originalDirection.lengthSqr() > 0.0001) {
            Vec3 toOwner = ownerCenter.subtract(projectile.position());
            if (toOwner.lengthSqr() > 0.01) {
                double dot = toOwner.normalize().dot(originalDirection.normalize());

                // 原始方向不朝向玩家 → 立即释放
                if (dot <= 0) {
                    return false;
                }
            }
        }
        // ★★★ 射线-球体检测：会不会穿过墙 ★★★
        if (originalDirection != null) {
            if (!RaySphereChecker.willHitBalancePoint(
                    projectile.position(), originalDirection, ownerCenter, balanceRadius)) {
                // 不会穿过墙 → 释放
                return false;
            }
        }
        // 当前在接近，或者没有方向信息（保守拦截）
        return true;
    }

    // ==================== 基础状态处理 ====================

    private static void handleBasicProjectileState(Projectile projectile) {
        // 箭矢震动计时器
        if (projectile instanceof AbstractArrow arrow && arrow.shakeTime > 0) {
            arrow.shakeTime--;
        }

        // 水/雪灭火
        BlockState blockState = projectile.level().getBlockState(projectile.blockPosition());
        if (projectile.isInWaterOrRain() || blockState.is(Blocks.POWDER_SNOW)) {
            projectile.clearFire();
        }
    }

    // ==================== 释放逻辑 ====================

    private static void releaseProjectile(Projectile projectile, IFrozenProjectile fp, String reason) {
        if (!fp.jujutsuAddon$isControlled()) return;

        switch (reason) {
            case "路线不经过墙":
            case "超出范围":
            case "owner为null":
                ProjectileReleaseHelper.hardRelease(projectile);
                break;
            default:
                ProjectileReleaseHelper.softRelease(projectile);
                break;
        }
    }

    // ==================== 辅助方法 ====================

    @Nullable
    private static LivingEntity findOwner(Projectile projectile, @Nullable UUID ownerUUID) {
        if (ownerUUID == null) return null;

        // 优先查找玩家
        Player player = projectile.level().getPlayerByUUID(ownerUUID);
        if (player != null) return player;

        // 搜索范围内的生物
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
}
