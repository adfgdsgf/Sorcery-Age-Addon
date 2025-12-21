package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.BalancePointCalculator;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.effect.PressureEffectRenderer;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.PlayerMovementTracker;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util.VelocityAnalyzer;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * 投射物区域处理器
 *
 * 处理各个区域（停止区、缓冲区、推力区、减速区）的具体逻辑
 */
public final class ProjectileZoneProcessor {

    private ProjectileZoneProcessor() {}

    // ==================== 推力结果 ====================

    public static class PushResult {
        public final Vec3 velocity;
        public final boolean isPlayerPush;

        public PushResult(Vec3 velocity, boolean isPlayerPush) {
            this.velocity = velocity;
            this.isPlayerPush = isPlayerPush;
        }

        public static PushResult none() {
            return new PushResult(Vec3.ZERO, false);
        }
    }

    // ==================== 区域判断与处理入口 ====================

    /**
     * 处理投射物在各区域的行为
     * @return true = 已处理完毕（投射物静止或被推动），false = 需要继续处理减速
     */
    public static boolean processZones(Projectile projectile, IFrozenProjectile fp,
                                       LivingEntity owner, Vec3 ownerCenter, Vec3 position,
                                       double distance, double balanceRadius, float maxRange) {

        boolean isInStopZone = ProjectileZoneHelper.isInStopZone(projectile, distance, balanceRadius);

        if (isInStopZone) {
            boolean isInBufferZone = ProjectileZoneHelper.isInBufferZone(distance, balanceRadius);
            boolean isInPushZone = ProjectileZoneHelper.isInPushZone(distance, balanceRadius);

            processStopZone(projectile, fp, owner, ownerCenter, position, distance,
                    balanceRadius, isInBufferZone, isInPushZone);
            return true;
        }

        return false;
    }

    // ==================== 停止区处理 ====================

    private static void processStopZone(Projectile projectile, IFrozenProjectile fp,
                                        LivingEntity owner, Vec3 ownerCenter, Vec3 position,
                                        double distance, double balanceRadius,
                                        boolean isInBufferZone, boolean isInPushZone) {
        fp.jujutsuAddon$setSpeedMultiplier(0);

        if (projectile.level() instanceof ServerLevel serverLevel) {
            PressureEffectRenderer.renderProjectileFrozenRing(serverLevel, projectile, 0f);
        }

        // 推力区：太靠近玩家，强力推出
        if (isInPushZone) {
            processPushZone(projectile, owner, ownerCenter, position, distance, balanceRadius);
            return;
        }

        // 缓冲区：完全静止，只跟随玩家径向移动
        if (isInBufferZone) {
            followOwnerRadialMovement(projectile, owner, ownerCenter, position);
            projectile.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // 静止区：检查玩家移动推动
        processStaticZone(projectile, owner, ownerCenter, position, balanceRadius);
    }

    // ==================== 推力区处理 ====================

    private static void processPushZone(Projectile projectile, LivingEntity owner,
                                        Vec3 ownerCenter, Vec3 position,
                                        double distance, double balanceRadius) {
        // 先跟随玩家径向移动
        followOwnerRadialMovement(projectile, owner, ownerCenter, position);

        // 更新位置引用
        position = projectile.position();

        Vec3 toProjectile = position.subtract(ownerCenter);
        if (toProjectile.lengthSqr() < 0.01) {
            toProjectile = new Vec3(0.1, 0.05, 0.1);
        }
        Vec3 pushDir = toProjectile.normalize();

        // 更新当前飞行方向
        if (projectile instanceof IFrozenProjectile fp) {
            fp.jujutsuAddon$setCurrentDirection(pushDir);
        }

        // 计算需要推到缓冲区中间的距离
        double bufferMiddle = balanceRadius - ProjectileZoneHelper.BUFFER_ZONE_THICKNESS * 0.5;
        bufferMiddle = Math.max(bufferMiddle, 0.2);
        double currentDist = toProjectile.length();
        double pushAmount = Math.max(0.1, bufferMiddle - currentDist);
        pushAmount = Math.min(pushAmount, ProjectileZoneHelper.MAX_PUSH_AMOUNT);

        Vec3 newPos = position.add(pushDir.scale(pushAmount));
        BlockPos bp = BlockPos.containing(newPos);
        if (projectile.level().getBlockState(bp).isAir()) {
            ProjectileMovementUpdater.moveTo(projectile, newPos, pushDir);
        }

        projectile.setDeltaMovement(Vec3.ZERO);
    }

    // ==================== 静止区处理 ====================

    private static void processStaticZone(Projectile projectile, LivingEntity owner,
                                          Vec3 ownerCenter, Vec3 position,
                                          double balanceRadius) {
        // 跟随玩家径向移动（保持相对距离）
        followOwnerRadialMovement(projectile, owner, ownerCenter, position);

        // 更新位置引用
        position = projectile.position();
        ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);

        // 额外推力逻辑（玩家主动接近时）
        PushResult pushResult = calculatePushVelocity(owner, ownerCenter, position, balanceRadius);

        if (ProjectileZoneHelper.isSignificantPush(pushResult.velocity) && pushResult.isPlayerPush) {
            Vec3 extraPos = projectile.position().add(pushResult.velocity);
            BlockPos bp = BlockPos.containing(extraPos);
            if (projectile.level().getBlockState(bp).isAir()) {
                ProjectileMovementUpdater.moveTo(projectile, extraPos, pushResult.velocity);

                // 更新当前飞行方向
                if (projectile instanceof IFrozenProjectile fp) {
                    Vec3 pushDir = pushResult.velocity.normalize();
                    fp.jujutsuAddon$setCurrentDirection(pushDir);
                }
            }
        }

        projectile.setDeltaMovement(Vec3.ZERO);
    }

    // ==================== 径向跟随 ====================

    /**
     * 让投射物只跟随玩家的"径向"移动分量
     */
    private static void followOwnerRadialMovement(Projectile projectile, LivingEntity owner,
                                                  Vec3 ownerCenter, Vec3 projectilePos) {
        Vec3 ownerMovement = PlayerMovementTracker.getMovement(owner.getUUID());
        double ownerSpeed = ownerMovement.length();

        if (ownerSpeed < ProjectileZoneHelper.MIN_OWNER_SPEED) return;

        // 计算投射物到玩家的方向（径向）
        Vec3 toOwner = ownerCenter.subtract(projectilePos);
        if (toOwner.lengthSqr() < 0.01) return;
        Vec3 radialDir = toOwner.normalize();

        // 只提取玩家移动在径向上的分量
        double radialComponent = ownerMovement.dot(radialDir);

        // 玩家接近或侧移，不需要跟随
        if (radialComponent >= 0) {
            return;
        }

        // 玩家远离，投射物需要跟随以保持距离
        Vec3 followMovement = radialDir.scale(radialComponent);

        Vec3 newPos = projectilePos.add(followMovement);
        BlockPos bp = BlockPos.containing(newPos);

        if (projectile.level().getBlockState(bp).isAir()) {
            projectile.setPos(newPos.x, newPos.y, newPos.z);
        }
    }

    // ==================== 减速区处理 ====================

    /**
     * 处理减速区逻辑
     * @return true = 正常处理, false = 速度过低需要释放
     */
    public static boolean processSlowdownZone(Projectile projectile, IFrozenProjectile fp,
                                              double distance, double balanceRadius, float maxRange,
                                              float currentSpeed) {
        float targetSpeed = ProjectileZoneHelper.calculateSlowdownSpeedZeno(
                distance, balanceRadius, maxRange);

        float speedMod;
        if (currentSpeed > targetSpeed) {
            speedMod = Math.max(targetSpeed, currentSpeed * 0.85F);
        } else {
            speedMod = targetSpeed;
        }
        fp.jujutsuAddon$setSpeedMultiplier(speedMod);

        if (speedMod > 0.01) {
            ProjectileMovementUpdater.moveWithSpeedMultiplier(projectile, speedMod);
            return true;
        } else {
            return false;
        }
    }

    // ==================== ★★★ 推力计算（使用已有工具类）★★★ ====================

    private static PushResult calculatePushVelocity(LivingEntity owner,
                                                    Vec3 ownerCenter, Vec3 projectilePos,
                                                    double balanceRadius) {
        Vec3 toProjectile = projectilePos.subtract(ownerCenter);
        double distToProjectile = toProjectile.length();

        if (distToProjectile < 0.1) {
            return new PushResult(new Vec3(0.1, 0.05, 0.1), false);
        }

        Vec3 pushDir = toProjectile.normalize();
        Vec3 ownerMovement = PlayerMovementTracker.getMovement(owner.getUUID());

        if (ownerMovement.lengthSqr() < 0.001) {
            return PushResult.none();
        }

        // ★★★ 使用 VelocityAnalyzer 检查玩家是否在接近投射物 ★★★
        double approachSpeed = VelocityAnalyzer.calculateApproachSpeed(
                ownerCenter, ownerMovement, projectilePos);

        if (approachSpeed < ProjectileZoneHelper.PLAYER_APPROACH_THRESHOLD) {
            return PushResult.none();
        }

        // ★★★ 使用 BalancePointCalculator 计算芝诺推力 ★★★
        double zenoPush = BalancePointCalculator.calculateZenoPushForce(
                distToProjectile, balanceRadius, approachSpeed);

        // ★★★ 使用 PressureConfig 获取推力限制 ★★★
        zenoPush = Math.max(zenoPush, PressureConfig.getBasePushForce() * 0.5);
        zenoPush = Math.min(zenoPush, PressureConfig.getMaxPushForce());

        return new PushResult(pushDir.scale(zenoPush), true);
    }
}
