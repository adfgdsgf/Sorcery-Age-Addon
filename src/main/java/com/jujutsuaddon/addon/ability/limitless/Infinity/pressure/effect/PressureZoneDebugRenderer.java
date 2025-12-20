/*
package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.effect;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

*/
/**
 * 调试用 - 可视化压力区域边界
 *//*

public class PressureZoneDebugRenderer {

    public static void renderZoneBoundaries(LivingEntity owner, int pressureLevel,
                                            float cursedEnergyOutput, double maxRange) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        if (owner.tickCount % 2 != 0) return;

        double ownerX = owner.getX();
        double ownerY = owner.getY() + 0.1;
        double ownerZ = owner.getZ();

        double pushZoneRadius = PressureConfig.getStopZoneRadius(pressureLevel);

        // 红色 = 平衡点（红圈）
        renderCircle(level, ownerX, ownerY, ownerZ, pushZoneRadius, ParticleTypes.FLAME, 16);

        // 绿色 = 最大范围（绿圈）
        renderCircle(level, ownerX, ownerY, ownerZ, maxRange, ParticleTypes.HAPPY_VILLAGER, 24);

        // ★★★ 调试：每2秒在聊天栏显示圈的半径 ★★★
        if (owner instanceof ServerPlayer player && owner.tickCount % 40 == 0) {
        }
    }

    public static void renderTargetStatus(LivingEntity owner, LivingEntity target,
                                          int pressureLevel, float cursedEnergyOutput,
                                          double maxRange) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        if (target.tickCount % 3 != 0) return;

        // ★★★ 使用 3D 中心距离，和 PushForceApplier 一致 ★★★
        double distance = owner.position().add(0, owner.getBbHeight() / 2, 0)
                .distanceTo(target.position().add(0, target.getBbHeight() / 2, 0));

        double pushZoneRadius = PressureConfig.getStopZoneRadius(pressureLevel);

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() + 0.3;
        double z = target.getZ();

        if (distance <= pushZoneRadius) {
            // 红圈内 = 红火焰
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 3, 0.1, 0.1, 0.1, 0);
        } else if (distance <= maxRange) {
            // 减速区 = 蓝火焰
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.1, 0.1, 0.1, 0);
        }
    }

    private static void renderCircle(ServerLevel level, double cx, double cy, double cz,
                                     double radius, net.minecraft.core.particles.ParticleOptions particle,
                                     int points) {
        if (radius <= 0) return;
        for (int i = 0; i < points; i++) {
            double angle = (i / (double) points) * Math.PI * 2;
            level.sendParticles(particle,
                    cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }
    }
}
*/
