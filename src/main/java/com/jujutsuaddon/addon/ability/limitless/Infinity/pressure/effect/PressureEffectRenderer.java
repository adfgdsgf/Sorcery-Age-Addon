package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.effect;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureStateManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PressureEffectRenderer {

    // ==================== 波纹效果冷却追踪 ====================
    private static final Map<UUID, Long> lastRippleTime = new HashMap<>();
    private static final long RIPPLE_COOLDOWN_TICKS = 30; // 1.5秒冷却

    // ==================== 投射物静止环 ====================

    /**
     * ★★★ 渲染投射物被停住时的环形粒子 ★★★
     * 环垂直于投射物飞行方向
     */
    public static void renderProjectileFrozenRing(ServerLevel level, Projectile projectile,
                                                  float speedMultiplier) {
        if (!PressureConfig.areParticlesEnabled()) return;

        // 只在完全静止或接近静止时显示
        if (speedMultiplier > 0.1f) return;

        // ★★★ 频率大幅降低：每50tick渲染一次（约2.5秒）★★★
        if (projectile.tickCount % 50 != 0) return;

        Vec3 pos = projectile.position();
        double x = pos.x;
        double y = pos.y + projectile.getBbHeight() / 2;
        double z = pos.z;

        double radius = 0.15;

        // 获取投射物朝向
        Vec3 direction = projectile.getDeltaMovement();
        if (direction.lengthSqr() < 0.0001) {
            float yaw = projectile.getYRot() * ((float) Math.PI / 180F);
            float pitch = projectile.getXRot() * ((float) Math.PI / 180F);
            direction = new Vec3(
                    -Math.sin(yaw) * Math.cos(pitch),
                    -Math.sin(pitch),
                    Math.cos(yaw) * Math.cos(pitch)
            );
        }
        direction = direction.normalize();

        Vec3 perpendicular1 = getPerpendicular(direction);
        Vec3 perpendicular2 = direction.cross(perpendicular1).normalize();

        // ★★★ 粒子数量大幅减少：只有2个 ★★★
        int particleCount = 2;

        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * Math.PI * 2;

            double px = x + (perpendicular1.x * Math.cos(angle) + perpendicular2.x * Math.sin(angle)) * radius;
            double py = y + (perpendicular1.y * Math.cos(angle) + perpendicular2.y * Math.sin(angle)) * radius;
            double pz = z + (perpendicular1.z * Math.cos(angle) + perpendicular2.z * Math.sin(angle)) * radius;

            level.sendParticles(ParticleTypes.END_ROD,
                    px, py, pz,
                    1, 0, 0, 0, 0);
        }
    }

    /**
     * 渲染收缩的环效果
     */
    private static void renderShrinkingRing(ServerLevel level, double x, double y, double z,
                                            double startRadius, double endRadius) {
        int steps = 3;
        for (int step = 0; step < steps; step++) {
            double t = step / (double) steps;
            double radius = startRadius + (endRadius - startRadius) * t;
            int particleCount = 6;

            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double) particleCount) * Math.PI * 2;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;

                // 向内的速度
                double vx = (x - px) * 0.05;
                double vz = (z - pz) * 0.05;

                level.sendParticles(ParticleTypes.ENCHANT,
                        px, y, pz,
                        1, vx, 0, vz, 0.02);
            }
        }
    }

    /**
     * ★★★ 投射物刚进入停止区时的冲击效果 ★★★
     */
    public static void renderProjectileStopImpact(ServerLevel level, Projectile projectile) {
        if (!PressureConfig.areParticlesEnabled()) return;

        Vec3 pos = projectile.position();
        double x = pos.x;
        double y = pos.y + projectile.getBbHeight() / 2;
        double z = pos.z;

        // 小型扩散效果
        double radius = 0.3;
        int particleCount = 8;

        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * Math.PI * 2;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            double vx = Math.cos(angle) * 0.05;
            double vz = Math.sin(angle) * 0.05;

        }

        // ★★★ 删除了声音 ★★★
    }

    // ==================== 玩家接近波纹效果 ====================

    /**
     * ★★★ 渲染玩家接近静止区时的波纹效果 ★★★
     * 波纹从玩家位置向外扩散，表示空间被压缩
     *
     * @param owner 无下限使用者
     * @param target 进入区域的目标
     * @param balanceRadius 平衡点半径
     * @param distance 目标当前距离
     */
    public static void renderApproachRipple(ServerLevel level, LivingEntity owner,
                                            LivingEntity target, double balanceRadius,
                                            double distance) {
        if (!PressureConfig.areParticlesEnabled()) return;

        UUID targetId = target.getUUID();
        long currentTick = level.getGameTime();

        // 检查冷却
        Long lastTime = lastRippleTime.get(targetId);
        if (lastTime != null && currentTick - lastTime < RIPPLE_COOLDOWN_TICKS) {
            return;
        }

        // 只在接近平衡点边缘时触发（距离在 balanceRadius 到 balanceRadius + 1.0 之间）
        double triggerZoneOuter = balanceRadius + 1.0;
        double triggerZoneInner = balanceRadius;

        if (distance > triggerZoneOuter || distance < triggerZoneInner) {
            return;
        }

        // 更新冷却
        lastRippleTime.put(targetId, currentTick);

        // 计算波纹中心（在目标和owner之间，靠近平衡点边界）
        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 direction = targetCenter.subtract(ownerCenter).normalize();

        // 波纹中心在平衡点边界上
        Vec3 rippleCenter = ownerCenter.add(direction.scale(balanceRadius));

        renderExpandingRipple(level, rippleCenter, direction);

        // 音效
        if (PressureConfig.areSoundsEnabled()) {
            level.playSound(null,
                    rippleCenter.x, rippleCenter.y, rippleCenter.z,
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                    0.3F, 1.5F);
        }
    }

    /**
     * 渲染向外扩散的波纹
     */
    private static void renderExpandingRipple(ServerLevel level, Vec3 center, Vec3 outwardDirection) {
        double x = center.x;
        double y = center.y;
        double z = center.z;

        // 多层波纹
        int ringCount = 3;
        for (int ring = 0; ring < ringCount; ring++) {
            double radius = 0.3 + ring * 0.4;
            int particleCount = 8 + ring * 4;

            double speed = 0.08 + ring * 0.03;

            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double) particleCount) * Math.PI * 2;

                Vec3 perpendicular = getPerpendicular(outwardDirection);
                Vec3 perpendicular2 = outwardDirection.cross(perpendicular).normalize();

                double px = x + (perpendicular.x * Math.cos(angle) + perpendicular2.x * Math.sin(angle)) * radius;
                double py = y + (perpendicular.y * Math.cos(angle) + perpendicular2.y * Math.sin(angle)) * radius;
                double pz = z + (perpendicular.z * Math.cos(angle) + perpendicular2.z * Math.sin(angle)) * radius;

                double vx = outwardDirection.x * speed + (px - x) * 0.05;
                double vy = outwardDirection.y * speed + (py - y) * 0.05;
                double vz = outwardDirection.z * speed + (pz - z) * 0.05;

                level.sendParticles(ParticleTypes.ENCHANT,
                        px, py, pz,
                        1, vx, vy, vz, 0.01);
            }
        }

        // 外圈扩散粒子
        for (int i = 0; i < 6; i++) {
            double angle = (i / 6.0) * Math.PI * 2;
            Vec3 perpendicular = getPerpendicular(outwardDirection);
            Vec3 perpendicular2 = outwardDirection.cross(perpendicular).normalize();

            double vx = (perpendicular.x * Math.cos(angle) + perpendicular2.x * Math.sin(angle)) * 0.15;
            double vy = (perpendicular.y * Math.cos(angle) + perpendicular2.y * Math.sin(angle)) * 0.15;
            double vz = (perpendicular.z * Math.cos(angle) + perpendicular2.z * Math.sin(angle)) * 0.15;

            level.sendParticles(ParticleTypes.END_ROD,
                    x + vx, y + vy, z + vz,
                    1, vx * 2, vy * 2, vz * 2, 0.02);
        }
    }

    /**
     * 获取一个与给定向量垂直的向量
     */
    private static Vec3 getPerpendicular(Vec3 v) {
        if (Math.abs(v.y) < 0.99) {
            return v.cross(new Vec3(0, 1, 0)).normalize();
        } else {
            return v.cross(new Vec3(1, 0, 0)).normalize();
        }
    }

    /**
     * ★★★ 渲染持续的边界波动效果 ★★★
     * 当有实体在静止区边缘时，边界会有轻微的波动
     */
    public static void renderBoundaryFluctuation(ServerLevel level, LivingEntity owner,
                                                 double balanceRadius, int tick) {
        if (!PressureConfig.areParticlesEnabled()) return;

        // 每10tick渲染一次
        if (tick % 50 != 0) return;

        Vec3 ownerCenter = owner.position().add(0, owner.getBbHeight() / 2, 0);

        // 在边界上随机位置生成少量粒子
        int particleCount = 2;
        for (int i = 0; i < particleCount; i++) {
            double theta = Math.random() * Math.PI * 2;  // 水平角度
            double phi = Math.random() * Math.PI - Math.PI / 2;  // 垂直角度（-90 到 90度）

            double x = ownerCenter.x + Math.cos(theta) * Math.cos(phi) * balanceRadius;
            double y = ownerCenter.y + Math.sin(phi) * balanceRadius * 0.5;  // 压扁成椭圆
            double z = ownerCenter.z + Math.sin(theta) * Math.cos(phi) * balanceRadius;

            // 轻微的脉动
            double pulse = Math.sin(tick * 0.2 + theta) * 0.02;

            level.sendParticles(ParticleTypes.ENCHANT,
                    x, y, z,
                    1, pulse, pulse * 0.5, pulse, 0.005);
        }
    }

    // ==================== 清理 ====================

    /**
     * 清理冷却缓存（定期调用）
     */
    public static void cleanupCooldowns(long currentTick) {
        lastRippleTime.entrySet().removeIf(entry ->
                currentTick - entry.getValue() > RIPPLE_COOLDOWN_TICKS * 10);
    }

    // ==================== 原有方法保持不变 ====================

    public static void renderPressureEffect(LivingEntity owner, LivingEntity target,
                                            double pressure, double distanceFromHalt,
                                            boolean isBreaching,
                                            PressureStateManager stateManager) {

        if (!(owner.level() instanceof ServerLevel level)) return;

        boolean particlesEnabled = PressureConfig.areParticlesEnabled();
        boolean soundsEnabled = PressureConfig.areSoundsEnabled();

        if (!particlesEnabled && !soundsEnabled) return;

        int tick = target.tickCount;
        PressureStage stage = getPressureStage(pressure);

        switch (stage) {
            case NONE:
                break;
            case LIGHT:
                renderLightPressure(level, target, pressure, tick, particlesEnabled, soundsEnabled);
                break;
            case MEDIUM:
                renderMediumPressure(level, target, pressure, tick, particlesEnabled, soundsEnabled);
                break;
            case HEAVY:
                renderHeavyPressure(level, target, pressure, tick, particlesEnabled, soundsEnabled);
                break;
            case CRITICAL:
                renderCriticalPressure(level, target, pressure, tick, particlesEnabled, soundsEnabled);
                break;
        }

        if (isBreaching) {
            renderBreachEffect(level, target, Math.abs(distanceFromHalt), tick, particlesEnabled, soundsEnabled);
        }
    }

    public static void renderDamageWarning(ServerLevel level, LivingEntity target,
                                           double pressure, int ticksUntilDamage) {

        boolean particlesEnabled = PressureConfig.areParticlesEnabled();
        boolean soundsEnabled = PressureConfig.areSoundsEnabled();

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (ticksUntilDamage <= 3) {
            if (particlesEnabled) {
                double shrinkFactor = ticksUntilDamage / 3.0;
                double radius = 0.5 + shrinkFactor * 0.5;

                int particleCount = 8 - ticksUntilDamage * 2;
                for (int i = 0; i < particleCount; i++) {
                    double angle = (i / (double) particleCount) * Math.PI * 2;
                    double px = x + Math.cos(angle) * radius;
                    double pz = z + Math.sin(angle) * radius;

                    double vx = (x - px) * 0.1;
                    double vz = (z - pz) * 0.1;

                }
            }

            if (soundsEnabled) {
                if (ticksUntilDamage == 3) {
                    level.playSound(null, x, y, z,
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                            0.3F, 0.5F);
                }

                if (ticksUntilDamage == 1) {
                    level.playSound(null, x, y, z,
                            SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS,
                            0.2F, 0.3F);
                }
            }
        }
    }

    public static void renderPressureSurge(ServerLevel level, LivingEntity target,
                                           double pressureChange) {
        boolean particlesEnabled = PressureConfig.areParticlesEnabled();
        boolean soundsEnabled = PressureConfig.areSoundsEnabled();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();
        if (particlesEnabled) {
            int intensity = (int) Math.min(pressureChange * 2, 20);
            level.sendParticles(ParticleTypes.EXPLOSION,
                    x, y, z, 1, 0, 0, 0, 0);
            // ★★★ 删除了 SWEEP_ATTACK，改用 ENCHANT ★★★
            level.sendParticles(ParticleTypes.ENCHANT,
                    x, y, z, intensity, 0.5, 0.5, 0.5, 0.1);
        }
        if (soundsEnabled) {
            level.playSound(null, x, y, z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                    0.6F, 0.8F);
            level.playSound(null, x, y, z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                    0.8F, 0.4F);
        }
    }

    // ==================== 各阶段效果 ====================

    private static void renderLightPressure(ServerLevel level, LivingEntity target,
                                            double pressure, int tick,
                                            boolean particles, boolean sounds) {
        if (tick % 60 != 0) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particles) {
            level.sendParticles(ParticleTypes.EFFECT,
                    x, y, z, 2, 0.3, 0.3, 0.3, 0.01);
        }

        if (sounds && tick % 130 == 0) {
            level.playSound(null, x, y, z,
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                    0.1F, 0.3F);
        }
    }

    private static void renderMediumPressure(ServerLevel level, LivingEntity target,
                                             double pressure, int tick,
                                             boolean particles, boolean sounds) {
        if (tick % 20 != 0) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particles) {
            level.sendParticles(ParticleTypes.EFFECT,
                    x, y, z, 4, 0.4, 0.4, 0.4, 0.02);

            level.sendParticles(ParticleTypes.SMOKE,
                    x, y + 0.8, z, 2, 0.3, 0.1, 0.3, 0.01);
        }

        if (sounds && tick % 15 == 0) {
            level.playSound(null, x, y, z,
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                    0.2F, 0.4F);
        }
    }

    private static void renderHeavyPressure(ServerLevel level, LivingEntity target,
                                            double pressure, int tick,
                                            boolean particles, boolean sounds) {
        if (tick % 15 != 0) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particles) {

            level.sendParticles(ParticleTypes.SMOKE,
                    x, y + 0.8, z, 3, 0.2, 0.1, 0.2, 0.02);

            level.sendParticles(ParticleTypes.POOF,
                    x, target.getY() + 0.1, z, 2, 0.3, 0, 0.3, 0.01);
        }

        if (sounds && tick % 10 == 0) {
            level.playSound(null, x, y, z,
                    SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.PLAYERS,
                    0.3F, 0.5F);
        }
    }

    private static void renderCriticalPressure(ServerLevel level, LivingEntity target,
                                               double pressure, int tick,
                                               boolean particles, boolean sounds) {
        if (tick % 8 != 0) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();

        if (particles) {
            double radius = 0.6 + Math.sin(tick * 0.3) * 0.2;
            int particleCount = 6;
            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double) particleCount) * Math.PI * 2 + tick * 0.1;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
            }
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    x, y, z, 2, 0.1, 0.1, 0.1, 0.01);
        }
        if (sounds && tick % 8 == 0) {
            level.playSound(null, x, y, z,
                    SoundEvents.CHAIN_STEP, SoundSource.PLAYERS,
                    0.4F, 0.6F);
        }
    }

    private static void renderBreachEffect(ServerLevel level, LivingEntity target,
                                           double breachDepth, int tick,
                                           boolean particles, boolean sounds) {
        if (tick % 2 != 0) return;
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();
        if (particles) {
            int intensity = (int) Math.min(breachDepth * 2, 8);

            level.sendParticles(ParticleTypes.ENCHANT,
                    x, target.getY() + 0.1, z, intensity, 0.5, 0, 0.5, 0.02);
        }
        if (sounds && tick % 5 == 0) {
            float volume = Math.min(0.3F + (float)(breachDepth * 0.1), 0.6F);
            level.playSound(null, x, y, z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                    volume, 0.4F);
        }
    }

    // ==================== 辅助方法 ====================

    private enum PressureStage {
        NONE, LIGHT, MEDIUM, HEAVY, CRITICAL
    }

    private static PressureStage getPressureStage(double pressure) {
        if (pressure < 1.0) return PressureStage.NONE;
        if (pressure < 2.0) return PressureStage.LIGHT;
        if (pressure < 4.0) return PressureStage.MEDIUM;
        if (pressure < 6.0) return PressureStage.HEAVY;
        return PressureStage.CRITICAL;
    }
}
