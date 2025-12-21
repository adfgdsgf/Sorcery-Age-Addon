package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 投射物碰撞检测辅助类
 */
public final class ProjectileCollisionHelper {

    private ProjectileCollisionHelper() {}

    /**
     * 检查方块碰撞，如果碰撞则释放控制
     * @return true = 发生碰撞并已处理，调用方应该 return
     */
    public static boolean checkBlockCollision(Projectile projectile, IFrozenProjectile fp) {
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
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 处理实体碰撞检测，返回调整后的速度倍率
     */
    public static float handleEntityCollisions(Projectile projectile, Vec3 deltaMovement, float speedMod) {
        if (speedMod <= 0.01) return speedMod;

        Vec3 position = projectile.position();
        Vec3 reducedDelta = deltaMovement.scale(speedMod);
        Vec3 pos2 = position.add(deltaMovement).add(deltaMovement).add(reducedDelta);
        float inflateDist = (float) Math.max(Math.max(Math.abs(reducedDelta.x),
                Math.abs(reducedDelta.y)), Math.abs(reducedDelta.z)) * 2;

        EntityHitResult mobHit = ProjectileUtil.getEntityHitResult(
                projectile.level(), projectile, position, pos2,
                projectile.getBoundingBox().expandTowards(deltaMovement).inflate(1 + inflateDist),
                entity -> canHitEntity(projectile, entity)
        );

        if (mobHit != null) {
            speedMod *= 0.7F;
        }

        reducedDelta = deltaMovement.scale(speedMod);
        pos2 = position.add(deltaMovement).add(reducedDelta);
        EntityHitResult mobHit2 = ProjectileUtil.getEntityHitResult(
                projectile.level(), projectile, position, pos2,
                projectile.getBoundingBox().expandTowards(deltaMovement).inflate(1),
                entity -> canHitEntity(projectile, entity)
        );

        if (mobHit2 != null) {
            speedMod *= 0.6F;
        }

        if (mobHit2 != null && mobHit != null) {
            speedMod = Math.max(speedMod, 0.11F);
        }

        return speedMod;
    }

    /**
     * 处理命中检测
     * @return true = 命中了某物，投射物应该停止控制
     */
    public static boolean handleHitDetection(Projectile projectile, IFrozenProjectile fp,
                                             Vec3 position, Vec3 deltaMovement) {
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
            return true;
        }

        return false;
    }

    @Nullable
    public static EntityHitResult findHitEntity(Projectile projectile, Vec3 start, Vec3 end, Vec3 delta) {
        return ProjectileUtil.getEntityHitResult(
                projectile.level(), projectile, start, end,
                projectile.getBoundingBox().expandTowards(delta).inflate(1.0),
                entity -> canHitEntity(projectile, entity)
        );
    }

    public static boolean canHitEntity(Projectile projectile, Entity entity) {
        if (!entity.isSpectator() && entity.isAlive() && entity.isPickable()) {
            Entity owner = projectile.getOwner();
            return owner == null || !owner.isPassengerOfSameVehicle(entity);
        }
        return false;
    }

    // ==================== 箭矢反射方法 ====================

    public static boolean isArrowInGround(AbstractArrow arrow) {
        try {
            java.lang.reflect.Field field = AbstractArrow.class.getDeclaredField("inGround");
            field.setAccessible(true);
            return field.getBoolean(arrow);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setArrowInGround(AbstractArrow arrow, boolean value) {
        try {
            java.lang.reflect.Field field = AbstractArrow.class.getDeclaredField("inGround");
            field.setAccessible(true);
            field.setBoolean(arrow, value);
        } catch (Exception e) {
            // ignore
        }
    }
}
