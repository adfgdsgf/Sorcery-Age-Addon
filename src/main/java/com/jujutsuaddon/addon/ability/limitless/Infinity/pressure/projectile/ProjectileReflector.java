package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.InfinityPressureHandler;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.ProjectilePowerSyncS2CPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import radon.jujutsu_kaisen.ability.JJKAbilities;

import java.util.*;

public class ProjectileReflector {

    private static final double MIN_REFLECT_SPEED = 1.0;
    private static final double MAX_REFLECT_SPEED = 4.0;
    private static final int REFLECT_IMMUNE_TICKS = 40;

    /**
     * 反弹所有被控制的静止投射物
     */
    public static void reflectControlledProjectiles(ServerPlayer player, boolean towardsCursor,
                                                    Vec3 clientLookDirection) {
        if (!JJKAbilities.hasToggled(player, JJKAbilities.INFINITY.get())) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) return;

        PressureStateManager stateManager = InfinityPressureHandler.getStateManagerFor(player.getUUID());
        if (stateManager == null) return;

        Set<UUID> trackedIds = stateManager.getTrackedProjectileIds();
        if (trackedIds.isEmpty()) return;

        Vec3 lookDirection = clientLookDirection.normalize();
        Vec3 playerPos = player.getEyePosition();

        List<Projectile> toReflect = new ArrayList<>();

        for (UUID id : new HashSet<>(trackedIds)) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Projectile projectile)) continue;
            if (!(projectile instanceof IFrozenProjectile fp)) continue;

            // ★★★ 修复：用 isControlled() 判断，而不是 speedMultiplier ★★★
            if (!fp.jujutsuAddon$isControlled()) continue;

            UUID freezeOwner = fp.jujutsuAddon$getFreezeOwner();
            if (!player.getUUID().equals(freezeOwner)) continue;

            toReflect.add(projectile);
        }

        if (toReflect.isEmpty()) return;

        int reflectedCount = 0;
        for (Projectile projectile : toReflect) {
            Vec3 reflectDirection;

            if (towardsCursor) {
                reflectDirection = lookDirection;
            } else {
                reflectDirection = calculateReturnDirection(projectile, playerPos);
            }

            reflectProjectile(projectile, player, reflectDirection, stateManager);
            reflectedCount++;
        }

        if (reflectedCount > 0) {
            playReflectEffects(player, reflectedCount);
        }
    }

    private static Vec3 calculateReturnDirection(Projectile projectile, Vec3 playerPos) {
        Entity originalOwner = projectile.getOwner();
        if (originalOwner != null && originalOwner.isAlive()) {
            Vec3 targetPos = originalOwner.position().add(0, originalOwner.getBbHeight() / 2, 0);
            return targetPos.subtract(projectile.position()).normalize();
        }

        if (projectile instanceof IFrozenProjectile fp) {
            Vec3 originalVelocity = fp.jujutsuAddon$getOriginalVelocity();
            if (originalVelocity != null && originalVelocity.lengthSqr() > 0.01) {
                return originalVelocity.normalize().scale(-1);
            }
        }

        Vec3 awayFromPlayer = projectile.position().subtract(playerPos);
        if (awayFromPlayer.lengthSqr() > 0.01) {
            return awayFromPlayer.normalize();
        }

        return new Vec3(0, 1, 0);
    }

    private static void reflectProjectile(Projectile projectile, ServerPlayer newOwner,
                                          Vec3 direction, PressureStateManager stateManager) {
        if (!(projectile instanceof IFrozenProjectile fp)) return;

        Vec3 originalVelocity = fp.jujutsuAddon$getOriginalVelocity();
        double originalSpeed = originalVelocity != null ? originalVelocity.length() : 1.5;
        double speedMultiplier = PressureConfig.getReflectSpeedMultiplier();
        double reflectSpeed = originalSpeed * speedMultiplier;
        reflectSpeed = Math.max(MIN_REFLECT_SPEED, Math.min(MAX_REFLECT_SPEED, reflectSpeed));
        Vec3 newVelocity = direction.normalize().scale(reflectSpeed);

        // 更换所有者
        projectile.setOwner(newOwner);

        // ★★★ 设置新的原始速度（反弹速度作为新的基准） ★★★
        fp.jujutsuAddon$setOriginalVelocity(newVelocity);

        // ★★★ 释放控制（这会同步到客户端） ★★★
        fp.jujutsuAddon$setControlled(false);

        // 设置速度
        projectile.setDeltaMovement(newVelocity);

        // ★★★ 火焰弹特殊处理 ★★★
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            double powerScale = 0.1;
            hurting.xPower = direction.x * powerScale;
            hurting.yPower = direction.y * powerScale;
            hurting.zPower = direction.z * powerScale;

            // 火焰弹保持无重力
            projectile.setNoGravity(true);

            // ★★★ 同步 power 到客户端 ★★★
            AddonNetwork.sendToTrackingEntity(
                    new ProjectilePowerSyncS2CPacket(hurting),
                    projectile
            );
        } else {
            // 箭矢类恢复重力，让原版物理接管
            projectile.setNoGravity(false);
        }

        // 箭矢特殊处理
        if (projectile instanceof AbstractArrow arrow) {
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            setArrowInGround(arrow, false);
            arrow.shakeTime = 0;

            try {
                byte currentPierce = arrow.getPierceLevel();
                arrow.setPierceLevel((byte) Math.min(currentPierce + 1, 5));
            } catch (Exception ignored) {}
        }

        projectile.hurtMarked = true;

        stateManager.forceRemoveProjectile(projectile.getUUID());
        stateManager.markAsRepelled(projectile.getUUID(), REFLECT_IMMUNE_TICKS);

        spawnReflectParticles(projectile, newVelocity);
    }

    private static void playReflectEffects(ServerPlayer player, int count) {
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.8F + count * 0.1F,
                1.2F);

        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS,
                0.5F,
                1.5F);
    }

    private static void spawnReflectParticles(Projectile projectile, Vec3 velocity) {
        if (!(projectile.level() instanceof ServerLevel level)) return;

        Vec3 pos = projectile.position();
        Vec3 vel = velocity.normalize();

        level.sendParticles(ParticleTypes.CRIT,
                pos.x, pos.y, pos.z,
                10, 0.3, 0.3, 0.3, 0.15);

        level.sendParticles(ParticleTypes.ENCHANT,
                pos.x + vel.x * 0.5,
                pos.y + vel.y * 0.5,
                pos.z + vel.z * 0.5,
                6, 0.1, 0.1, 0.1, 0.3);

        level.sendParticles(ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                3, 0.1, 0.1, 0.1, 0.05);
    }

    private static void setArrowInGround(AbstractArrow arrow, boolean value) {
        try {
            java.lang.reflect.Field field = AbstractArrow.class.getDeclaredField("inGround");
            field.setAccessible(true);
            field.setBoolean(arrow, value);
        } catch (Exception ignored) {}
    }
}
