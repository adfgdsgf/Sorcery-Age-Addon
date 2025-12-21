package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.PlayerMovementTracker;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.ProjectileDeflectionHelper;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.ProjectilePowerSyncS2CPacket;
import com.jujutsuaddon.addon.network.s2c.ProjectileSmoothMoveS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 玩家移动时推动静止投射物
 */
public final class PlayerPushProjectileHandler {

    private PlayerPushProjectileHandler() {}

    private static final int LERP_TICKS = 3;
    private static final double PUSH_FACTOR = 1.2;
    private static final double FACING_THRESHOLD = 0.1;

    /** 推力转冲量的缩放系数 */
    private static final double IMPULSE_SCALE = 2.0;

    public enum PushResult {
        NO_PUSH,
        PUSHED,
        RELEASE
    }

    public static PushResult handlePlayerPush(Projectile projectile,
                                              IFrozenProjectile fp,
                                              LivingEntity owner,
                                              Vec3 ownerCenter,
                                              double balanceRadius) {
        // 1. 获取玩家移动量
        Vec3 ownerMovement = PlayerMovementTracker.getMovement(owner.getUUID());
        if (ownerMovement.lengthSqr() < 1e-6) {
            return PushResult.NO_PUSH;
        }

        Vec3 currentPos = projectile.position();
        double distanceToCenter = currentPos.distanceTo(ownerCenter);

        // 2. 投射物在球面外，不需要推
        if (distanceToCenter >= balanceRadius - 0.01) {
            return PushResult.NO_PUSH;
        }

        // 3. 计算径向推力方向
        Vec3 radialDirection = getRadialDirection(currentPos, ownerCenter);

        // 4. 计算推动距离
        double pushDistance = balanceRadius - distanceToCenter + 0.05;
        pushDistance = Math.min(pushDistance, ownerMovement.length() * PUSH_FACTOR);
        pushDistance = Math.min(pushDistance, 0.5);

        Vec3 pushVector = radialDirection.scale(pushDistance);
        Vec3 targetPos = currentPos.add(pushVector);

        // 5. 检查路径是否被方块阻挡
        Vec3 safePos = checkPathAndGetSafePosition(projectile, currentPos, targetPos);
        if (safePos == null || safePos.distanceToSqr(currentPos) < 1e-6) {
            return PushResult.NO_PUSH;
        }

        // 6. 移动投射物
        projectile.setPos(safePos.x, safePos.y, safePos.z);

        // 7. 发送平滑移动包
        AddonNetwork.sendToTrackingEntity(
                new ProjectileSmoothMoveS2CPacket(projectile.getId(), safePos, LERP_TICKS),
                projectile
        );

        // ★★★ 8. 物理计算新方向（使用工具类）★★★
        Vec3 currentDirection = ProjectileDeflectionHelper.getFlightDirection(fp, projectile);
        ProjectileDeflectionHelper.DeflectionResult deflection =
                ProjectileDeflectionHelper.calculateFromPush(
                        currentDirection, radialDirection, pushDistance, IMPULSE_SCALE);

        Vec3 newDirection = deflection.newDirection;

        // 9. 更新方向
        fp.jujutsuAddon$setCurrentDirection(newDirection);

        // 10. 更新旋转（视觉效果）
        ProjectileMovementUpdater.updateRotation(projectile, newDirection);

        // 11. 火焰弹特殊处理
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            updateHurtingProjectilePower(hurting, fp, newDirection);

            AddonNetwork.sendToTrackingEntity(
                    new ProjectilePowerSyncS2CPacket(hurting),
                    projectile
            );
        }

        // 12. 检查新方向是否还朝向玩家
        Vec3 newOwnerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);

        if (!ProjectileDeflectionHelper.isFacingTarget(
                safePos, newDirection, newOwnerCenter, FACING_THRESHOLD)) {
            // 释放前同步
            if (projectile instanceof AbstractHurtingProjectile hurting) {
                AddonNetwork.sendToTrackingEntity(
                        new ProjectilePowerSyncS2CPacket(hurting),
                        projectile
                );
            }

            ProjectileReleaseHelper.releaseWithDirection(projectile, newDirection);
            return PushResult.RELEASE;
        }

        // 13. 还朝向玩家，继续控制
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.hurtMarked = true;
        return PushResult.PUSHED;
    }

    // ==================== 辅助方法 ====================

    private static void updateHurtingProjectilePower(AbstractHurtingProjectile hurting,
                                                     IFrozenProjectile fp,
                                                     Vec3 newDirection) {
        Vec3 originalPower = fp.jujutsuAddon$getOriginalPower();
        double powerMagnitude = 0.1;
        if (originalPower != null && originalPower.lengthSqr() > 0.0001) {
            powerMagnitude = originalPower.length();
        }
        hurting.xPower = newDirection.x * powerMagnitude;
        hurting.yPower = newDirection.y * powerMagnitude;
        hurting.zPower = newDirection.z * powerMagnitude;
    }

    private static Vec3 getRadialDirection(Vec3 projectilePos, Vec3 ownerCenter) {
        Vec3 fromCenter = projectilePos.subtract(ownerCenter);
        if (fromCenter.lengthSqr() < 0.01) {
            return new Vec3(1, 0, 0);
        }
        return fromCenter.normalize();
    }

    @Nullable
    private static Vec3 checkPathAndGetSafePosition(Projectile projectile, Vec3 from, Vec3 to) {
        BlockHitResult hitResult = projectile.level().clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                projectile
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hitResult.getLocation();
            Vec3 direction = to.subtract(from).normalize();
            return hitPos.subtract(direction.scale(0.1));
        }

        BlockPos blockPos = BlockPos.containing(to);
        if (projectile.level().getBlockState(blockPos).blocksMotion()) {
            return null;
        }

        return to;
    }
}
