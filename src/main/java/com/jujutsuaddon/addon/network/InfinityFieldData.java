package com.jujutsuaddon.addon.network;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.core.PressureConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 单个无下限场的数据结构
 */
public class InfinityFieldData {

    public final UUID ownerId;
    public final Vec3 ownerPosition;
    public final double ownerHeight;
    public final double balanceRadius;
    public final double maxRange;
    public final float effectiveOutput;

    public InfinityFieldData(UUID ownerId, Vec3 ownerPosition, double ownerHeight,
                             double balanceRadius, double maxRange, float effectiveOutput) {
        this.ownerId = ownerId;
        this.ownerPosition = ownerPosition;
        this.ownerHeight = ownerHeight;
        this.balanceRadius = balanceRadius;
        this.maxRange = maxRange;
        this.effectiveOutput = effectiveOutput;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(ownerId);
        buf.writeDouble(ownerPosition.x);
        buf.writeDouble(ownerPosition.y);
        buf.writeDouble(ownerPosition.z);
        buf.writeDouble(ownerHeight);
        buf.writeDouble(balanceRadius);
        buf.writeDouble(maxRange);
        buf.writeFloat(effectiveOutput);
    }

    public static InfinityFieldData read(FriendlyByteBuf buf) {
        return new InfinityFieldData(
                buf.readUUID(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat()
        );
    }

    public Vec3 getOwnerCenter() {
        return ownerPosition.add(0, ownerHeight / 2, 0);
    }

    public double getDistanceTo(Vec3 position) {
        return getOwnerCenter().distanceTo(position.add(0, 0.9, 0));
    }

    /**
     * 计算芝诺倍率
     * ★★★ 使用 (1-p)^n 曲线，和服务端一致 ★★★
     */
    public double calculateZenoMultiplier(Vec3 position) {
        double distance = getDistanceTo(position);
        if (distance > maxRange) return 0;
        distance = Math.max(distance, 0.1);
        if (distance <= balanceRadius) {
            return 1.0;
        }
        double fadeZone = maxRange - balanceRadius;
        if (fadeZone < 0.1) return 1.0;
        double progress = (distance - balanceRadius) / fadeZone;
        progress = Math.min(1.0, progress);
        // ★★★ 使用 (1-p)^n 曲线 ★★★
        double exponent = PressureConfig.getZenoCurveExponent();
        return Math.pow(1.0 - progress, exponent);
    }

    public Vec3 getHorizontalPushDirection(Vec3 targetPosition) {
        Vec3 ownerCenter = getOwnerCenter();
        Vec3 targetCenter = targetPosition.add(0, 0.9, 0);
        Vec3 direction = targetCenter.subtract(ownerCenter);
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);

        if (horizontal.length() < 0.01) {
            return new Vec3(1, 0, 0);
        }

        return horizontal.normalize();
    }

    /**
     * ★★★ 获取完整的3D推力方向（包含Y轴） ★★★
     */
    public Vec3 getFullPushDirection(Vec3 targetPosition) {
        Vec3 ownerCenter = getOwnerCenter();
        Vec3 targetCenter = targetPosition.add(0, 0.9, 0);
        Vec3 direction = targetCenter.subtract(ownerCenter);
        if (direction.length() < 0.01) {
            return new Vec3(1, 0, 0);
        }
        return direction.normalize();
    }
}
