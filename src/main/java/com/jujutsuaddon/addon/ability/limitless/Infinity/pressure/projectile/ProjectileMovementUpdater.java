package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * 投射物移动更新器
 * 处理位置、速度、旋转的更新
 */
public final class ProjectileMovementUpdater {

    private ProjectileMovementUpdater() {}

    /**
     * 更新投射物旋转（朝向速度方向）
     */
    public static void updateRotation(Projectile projectile, Vec3 velocity) {
        if (velocity.lengthSqr() < 0.0001) return;

        Vec3 direction = velocity.normalize();
        double horizDist = direction.horizontalDistance();

        if (horizDist < 0.001) return;

        float targetYaw = (float) (Mth.atan2(direction.x, direction.z) * (180.0 / Math.PI));
        float targetPitch = (float) (Mth.atan2(direction.y, horizDist) * (180.0 / Math.PI));

        projectile.setYRot(lerpRotation(projectile.getYRot(), targetYaw));
        projectile.setXRot(Mth.clamp(lerpRotation(projectile.getXRot(), targetPitch), -90.0F, 90.0F));
    }

    /**
     * 初始化旋转（首次设置）
     */
    public static void initRotation(Projectile projectile, Vec3 velocity) {
        if (velocity.lengthSqr() < 0.0001) return;

        double horizontalDist = velocity.horizontalDistance();
        float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * 180.0F / (float) Math.PI);
        float pitch = (float) (Mth.atan2(velocity.y, horizontalDist) * 180.0F / (float) Math.PI);

        projectile.setYRot(yaw);
        projectile.setXRot(pitch);
        projectile.yRotO = yaw;
        projectile.xRotO = pitch;
    }

    /**
     * 按速度倍率移动投射物
     */
    public static void moveWithSpeedMultiplier(Projectile projectile, float speedMult) {
        Vec3 delta = projectile.getDeltaMovement();
        Vec3 reducedDelta = delta.scale(speedMult);
        Vec3 pos = projectile.position();

        projectile.setPos(
                pos.x + reducedDelta.x,
                pos.y + reducedDelta.y,
                pos.z + reducedDelta.z
        );

        if (reducedDelta.lengthSqr() > 0.0001) {
            updateRotation(projectile, reducedDelta);
        }
    }

    /**
     * 直接移动到指定位置
     */
    public static void moveTo(Projectile projectile, Vec3 newPos, Vec3 moveDirection) {
        projectile.setPos(newPos.x, newPos.y, newPos.z);
        if (moveDirection.lengthSqr() > 0.0001) {
            updateRotation(projectile, moveDirection);
        }
    }

    /**
     * 平滑旋转插值
     */
    private static float lerpRotation(float current, float target) {
        while (target - current < -180.0F) {
            current -= 360.0F;
        }
        while (target - current >= 180.0F) {
            current += 360.0F;
        }
        return Mth.lerp(0.2F, current, target);
    }
}
