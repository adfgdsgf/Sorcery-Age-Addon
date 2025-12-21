package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * 投射物释放辅助类
 *
 * ★★★ 核心原理 ★★★
 *
 * 电影比喻：
 * - 原始速度 = 电影总长度
 * - 捕获时速度 = 进入无限范围时剩余的内容
 * - 控制期间 = 按下暂停/慢放，不消耗内容
 * - 释放时 = 恢复捕获时的速度继续播放
 */
public class ProjectileReleaseHelper {

    /** 速度衰减（考虑被无限"阻挡"消耗的能量）*/
    //private static final double RELEASE_SPEED_FACTOR = 0.85;

    /**
     * 软释放
     */
    public static void softRelease(Projectile projectile) {
        if (!(projectile instanceof IFrozenProjectile fp)) {
            return;
        }

        long currentTick = projectile.level().getGameTime();
        ProjectileReleaseTracker.markSoftRelease(projectile.getUUID(), currentTick);

        fp.jujutsuAddon$setControlled(false);

        // 恢复重力
        projectile.setNoGravity(false);

        projectile.hurtMarked = true;
    }

    /**
     * 硬释放
     */
    public static void hardRelease(Projectile projectile) {
        if (!(projectile instanceof IFrozenProjectile fp)) {
            projectile.setNoGravity(false);
            projectile.hurtMarked = true;
            return;
        }

        long currentTick = projectile.level().getGameTime();
        ProjectileReleaseTracker.markHardRelease(projectile.getUUID(), currentTick);

        // 释放控制
        fp.jujutsuAddon$setControlled(false);

        // 恢复速度
        restoreVelocity(projectile, fp);

        // 清空数据
        clearProjectileData(fp);

        projectile.hurtMarked = true;
    }

    /**
     * 恢复速度
     *
     * ★★★ 使用捕获时的速度，而不是当前速度 ★★★
     */
    private static void restoreVelocity(Projectile projectile, IFrozenProjectile fp) {
        // 火焰弹特殊处理
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            restoreHurtingProjectile(hurting, fp);
            return;
        }
        // ==================== 箭矢类 ====================
        Vec3 currentVelocity = projectile.getDeltaMovement();

        // 只有在完全静止时才给一个小推力
        if (currentVelocity.lengthSqr() < 0.001) {
            Vec3 currentDirection = fp.jujutsuAddon$getCurrentDirection();
            Vec3 captureVelocity = fp.jujutsuAddon$getCaptureVelocity();

            Vec3 direction = null;
            if (currentDirection != null && currentDirection.lengthSqr() > 0.01) {
                direction = currentDirection.normalize();
            } else if (captureVelocity != null && captureVelocity.lengthSqr() > 0.01) {
                direction = captureVelocity.normalize();
            }

            if (direction != null) {
                // 给一个很小的速度，让重力自然接管
                projectile.setDeltaMovement(direction.scale(0.05));
            }
        }
        // 当前有速度的话，保持不变

        // 恢复重力
        projectile.setNoGravity(false);
    }
    /**
     * 清空投射物数据
     */
    private static void clearProjectileData(IFrozenProjectile fp) {
        fp.jujutsuAddon$setOriginalVelocity(null);
        fp.jujutsuAddon$setOriginalPower(null);
        fp.jujutsuAddon$setOriginalCapturePosition(null);
        fp.jujutsuAddon$setCurrentDirection(null);
        fp.jujutsuAddon$setCaptureVelocity(null);
    }

    /**
     * 火焰弹释放处理
     */
    private static void restoreHurtingProjectile(AbstractHurtingProjectile hurting,
                                                 IFrozenProjectile fp) {
        Vec3 captureVelocity = fp.jujutsuAddon$getCaptureVelocity();
        Vec3 originalVelocity = fp.jujutsuAddon$getOriginalVelocity();
        Vec3 currentDirection = fp.jujutsuAddon$getCurrentDirection();

        // 确定方向
        Vec3 direction;
        if (currentDirection != null && currentDirection.lengthSqr() > 0.01) {
            direction = currentDirection.normalize();
        } else if (captureVelocity != null && captureVelocity.lengthSqr() > 0.01) {
            direction = captureVelocity.normalize();
        } else if (originalVelocity != null && originalVelocity.lengthSqr() > 0.01) {
            direction = originalVelocity.normalize();
        } else {
            direction = new Vec3(0, 0, 1);
        }

        // 火焰弹使用原始速度（它们不衰减）
        double speed = 1.0;
        if (captureVelocity != null && captureVelocity.lengthSqr() > 0.001) {
            speed = captureVelocity.length();
        } else if (originalVelocity != null && originalVelocity.lengthSqr() > 0.001) {
            speed = originalVelocity.length();
        }

        // 火焰弹保持无重力
        hurting.setNoGravity(true);

        // 恢复 power
        Vec3 originalPower = fp.jujutsuAddon$getOriginalPower();
        if (originalPower != null && originalPower.lengthSqr() > 0.0001) {
            double powerMagnitude = originalPower.length();
            hurting.xPower = direction.x * powerMagnitude;
            hurting.yPower = direction.y * powerMagnitude;
            hurting.zPower = direction.z * powerMagnitude;
        } else {
            double powerScale = 0.1;
            hurting.xPower = direction.x * powerScale;
            hurting.yPower = direction.y * powerScale;
            hurting.zPower = direction.z * powerScale;
        }

        hurting.setDeltaMovement(direction.scale(speed));
    }

    //==================== 推动投射物 =====================

    /**
     * ★★★ 用指定方向和剩余速度释放 ★★★
     * 用于被推动后方向改变的投射物
     */
    public static void releaseWithDirection(Projectile projectile, Vec3 newDirection) {
        if (!(projectile instanceof IFrozenProjectile fp)) {
            projectile.setNoGravity(false);
            projectile.hurtMarked = true;
            return;
        }

        long currentTick = projectile.level().getGameTime();
        ProjectileReleaseTracker.markHardRelease(projectile.getUUID(), currentTick);

        // 释放控制
        fp.jujutsuAddon$setControlled(false);

        // ★★★ 用新方向和剩余速度设置速度 ★★★
        double remainingSpeed = calculateRemainingSpeed(projectile, fp);
        Vec3 newVelocity = newDirection.normalize().scale(remainingSpeed);

        // 火焰弹特殊处理
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            restoreHurtingProjectileWithDirection(hurting, fp, newDirection, remainingSpeed);
        } else {
            projectile.setDeltaMovement(newVelocity);
            projectile.setNoGravity(false);
        }

        // ★★★ 修复：释放后不需要清空数据，因为 controlled=false 后不会再 tick ★★★
        // clearProjectileData(fp);  // ← 注释掉或删除！

        projectile.hurtMarked = true;
    }
    /**
     * ★★★ 计算剩余速度（使用 ProjectileDragHelper）★★★
     */
    private static double calculateRemainingSpeed(Projectile projectile, IFrozenProjectile fp) {
        Vec3 captureVelocity = fp.jujutsuAddon$getCaptureVelocity();
        Vec3 originalVelocity = fp.jujutsuAddon$getOriginalVelocity();
        // 1. 优先使用捕获速度（进入控制区时的速度）
        if (captureVelocity != null && captureVelocity.lengthSqr() > 0.01) {
            return captureVelocity.length(); //* RELEASE_SPEED_FACTOR;
        }
        // 2. 没有捕获速度，用原始速度
        if (originalVelocity != null && originalVelocity.lengthSqr() > 0.01) {
            return originalVelocity.length(); //* RELEASE_SPEED_FACTOR;
        }
        // 3. 默认值
        return 0.5;
    }
    /**
     * 火焰弹用新方向释放
     */
    private static void restoreHurtingProjectileWithDirection(AbstractHurtingProjectile hurting,
                                                              IFrozenProjectile fp,
                                                              Vec3 newDirection,
                                                              double speed) {
        Vec3 direction = newDirection.normalize();
        // 火焰弹保持无重力
        hurting.setNoGravity(true);
        // 恢复 power（用新方向）
        Vec3 originalPower = fp.jujutsuAddon$getOriginalPower();
        double powerMagnitude = 0.1;
        if (originalPower != null && originalPower.lengthSqr() > 0.0001) {
            powerMagnitude = originalPower.length();
        }
        hurting.xPower = direction.x * powerMagnitude;
        hurting.yPower = direction.y * powerMagnitude;
        hurting.zPower = direction.z * powerMagnitude;
        hurting.setDeltaMovement(direction.scale(speed));
    }

    public static void release(Projectile projectile) {
        hardRelease(projectile);
    }

    public static double getEffectiveRadius(Projectile projectile) {
        double baseRadius = Math.max(projectile.getBbWidth(), projectile.getBbHeight()) / 2.0;
        if (projectile instanceof AbstractHurtingProjectile) {
            return baseRadius + 0.5;
        }
        return baseRadius;
    }
}
