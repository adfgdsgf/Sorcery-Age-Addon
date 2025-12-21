package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 投射物控制辅助类
 */
public final class ProjectileFreezeHelper {

    private ProjectileFreezeHelper() {}

    /**
     * ★★★ 完全冻结投射物 ★★★
     */
    public static boolean freeze(Projectile projectile, Vec3 position, @Nullable UUID ownerUUID,
                                 float stopDistance, float maxRange) {
        if (projectile == null || position == null) return false;

        if (projectile instanceof IFrozenProjectile fp) {
            fp.jujutsuAddon$setControlled(true);
            fp.jujutsuAddon$setSpeedMultiplier(0);
            fp.jujutsuAddon$setFrozenPosition(position);
            fp.jujutsuAddon$setFreezeOwner(ownerUUID);
            fp.jujutsuAddon$setStopDistance(stopDistance);
            fp.jujutsuAddon$setMaxRange(maxRange);

            if (fp.jujutsuAddon$getOriginalVelocity() == null) {
                fp.jujutsuAddon$setOriginalVelocity(projectile.getDeltaMovement());
            }

            fp.jujutsuAddon$lockRotation(projectile.getYRot(), projectile.getXRot());
        }

        projectile.setPos(position);
        projectile.setNoGravity(true);

        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = 0;
            hurting.yPower = 0;
            hurting.zPower = 0;
        }

        projectile.hurtMarked = true;
        return true;
    }

    /**
     * ★★★ 简化版冻结（使用默认值）★★★
     */
    public static boolean freeze(Projectile projectile, Vec3 position, @Nullable UUID ownerUUID) {
        return freeze(projectile, position, ownerUUID, 0.5f, 10.0f);
    }

    /**
     * ★★★ 释放投射物 ★★★
     */
    public static void unfreeze(Projectile projectile) {
        if (projectile == null) return;

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

    /**
     * ★★★ 设置减速 ★★★
     */
    public static void setSlowdown(Projectile projectile, float speedMultiplier,
                                   @Nullable UUID ownerUUID, float stopDistance, float maxRange) {
        if (projectile == null) return;

        if (projectile instanceof IFrozenProjectile fp) {
            boolean wasControlled = fp.jujutsuAddon$isControlled();

            fp.jujutsuAddon$setControlled(true);
            fp.jujutsuAddon$setSpeedMultiplier(speedMultiplier);
            fp.jujutsuAddon$setFreezeOwner(ownerUUID);
            fp.jujutsuAddon$setFrozenPosition(null);
            fp.jujutsuAddon$setStopDistance(stopDistance);
            fp.jujutsuAddon$setMaxRange(maxRange);

            if (!wasControlled) {
                fp.jujutsuAddon$setOriginalVelocity(projectile.getDeltaMovement());
                fp.jujutsuAddon$lockRotation(projectile.getYRot(), projectile.getXRot());
            }
        }

        projectile.setNoGravity(true);

        if (speedMultiplier < 0.15f && projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = 0;
            hurting.yPower = 0;
            hurting.zPower = 0;
        }
    }

    // ==================== 状态查询 ====================

    public static boolean isControlled(Projectile projectile) {
        if (projectile instanceof IFrozenProjectile fp) {
            return fp.jujutsuAddon$isControlled();
        }
        return false;
    }

    public static boolean isFrozen(Projectile projectile) {
        if (projectile instanceof IFrozenProjectile fp) {
            return fp.jujutsuAddon$isControlled() && fp.jujutsuAddon$getSpeedMultiplier() <= 0.01f;
        }
        return false;
    }

    public static float getSpeedMultiplier(Projectile projectile) {
        if (projectile instanceof IFrozenProjectile fp) {
            return fp.jujutsuAddon$getSpeedMultiplier();
        }
        return 1.0f;
    }

    @Nullable
    public static Vec3 getFrozenPosition(Projectile projectile) {
        if (projectile instanceof IFrozenProjectile fp) {
            return fp.jujutsuAddon$getFrozenPosition();
        }
        return null;
    }

    public static boolean isControlledBy(Projectile projectile, UUID ownerUUID) {
        if (ownerUUID == null) return false;
        if (projectile instanceof IFrozenProjectile fp) {
            return fp.jujutsuAddon$isControlled() && ownerUUID.equals(fp.jujutsuAddon$getFreezeOwner());
        }
        return false;
    }
}