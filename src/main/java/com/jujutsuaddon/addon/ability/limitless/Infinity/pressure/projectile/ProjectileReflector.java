package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.InfinityPressureHandler;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
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
     *
     * @param player 玩家
     * @param towardsCursor true=朝准星方向, false=朝原发射者（或反向飞回）
     * @param clientLookDirection 客户端传来的准星方向
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

            // 只反弹速度接近0的（静止的）
            float speedMod = fp.jujutsuAddon$getSpeedMultiplier();
            if (speedMod > 0.15f) continue;

            // 确认是被这个玩家控制的
            UUID freezeOwner = fp.jujutsuAddon$getFreezeOwner();
            if (!player.getUUID().equals(freezeOwner)) continue;

            toReflect.add(projectile);
        }

        if (toReflect.isEmpty()) return;

        int reflectedCount = 0;
        for (Projectile projectile : toReflect) {
            Vec3 reflectDirection;

            if (towardsCursor) {
                // 朝准星方向
                reflectDirection = lookDirection;
            } else {
                // ★★★ 修改：优先朝原发射者，没有则反向飞回 ★★★
                reflectDirection = calculateReturnDirection(projectile, playerPos);
            }

            reflectProjectile(projectile, player, reflectDirection, stateManager);
            reflectedCount++;
        }

        if (reflectedCount > 0) {
            playReflectEffects(player, reflectedCount);
        }
    }

    /**
     * ★★★ 新增：计算返回方向 ★★★
     * 优先级：
     * 1. 原发射者存在 → 朝原发射者
     * 2. 有原始速度记录 → 反向飞回
     * 3. 都没有 → 朝投射物来的方向（远离玩家）
     */
    private static Vec3 calculateReturnDirection(Projectile projectile, Vec3 playerPos) {
        // 1. 尝试获取原发射者
        Entity originalOwner = projectile.getOwner();
        if (originalOwner != null && originalOwner.isAlive()) {
            Vec3 targetPos = originalOwner.position().add(0, originalOwner.getBbHeight() / 2, 0);
            return targetPos.subtract(projectile.position()).normalize();
        }

        // 2. 尝试获取原始速度（反向）
        if (projectile instanceof IFrozenProjectile fp) {
            Vec3 originalVelocity = fp.jujutsuAddon$getOriginalVelocity();
            if (originalVelocity != null && originalVelocity.lengthSqr() > 0.01) {
                // 反向：投射物原本朝哪飞来，就朝反方向弹回去
                return originalVelocity.normalize().scale(-1);
            }
        }

        // 3. 最后手段：从玩家位置向外推（远离玩家）
        Vec3 awayFromPlayer = projectile.position().subtract(playerPos);
        if (awayFromPlayer.lengthSqr() > 0.01) {
            return awayFromPlayer.normalize();
        }

        // 4. 实在不行就向上弹
        return new Vec3(0, 1, 0);
    }

    /**
     * 反弹单个投射物
     */
    private static void reflectProjectile(Projectile projectile, ServerPlayer newOwner,
                                          Vec3 direction, PressureStateManager stateManager) {
        if (!(projectile instanceof IFrozenProjectile fp)) return;

        // 计算反弹速度
        Vec3 originalVelocity = fp.jujutsuAddon$getOriginalVelocity();
        double originalSpeed = originalVelocity != null ? originalVelocity.length() : 1.5;

        double speedMultiplier = PressureConfig.getReflectSpeedMultiplier();
        double reflectSpeed = originalSpeed * speedMultiplier;
        reflectSpeed = Math.max(MIN_REFLECT_SPEED, Math.min(MAX_REFLECT_SPEED, reflectSpeed));

        Vec3 newVelocity = direction.normalize().scale(reflectSpeed);

        // ★★★ 重要：更换所有者 ★★★
        projectile.setOwner(newOwner);

        // 释放控制状态
        fp.jujutsuAddon$setControlled(false);
        fp.jujutsuAddon$setSpeedMultiplier(1.0f);

        // 设置新速度
        projectile.setDeltaMovement(newVelocity);
        projectile.setNoGravity(false);

        // 处理特殊投射物类型
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.xPower = newVelocity.x * 0.1;
            hurting.yPower = newVelocity.y * 0.1;
            hurting.zPower = newVelocity.z * 0.1;
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

        // 从追踪列表移除
        stateManager.forceRemoveProjectile(projectile.getUUID());

        // 标记为已弹开（短时间内不再拦截）
        stateManager.markAsRepelled(projectile.getUUID(), REFLECT_IMMUNE_TICKS);

        // 生成反弹粒子
        spawnReflectParticles(projectile, newVelocity);
    }

    // ==================== 特效 ====================

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
