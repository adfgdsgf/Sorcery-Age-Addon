// src/main/java/com/jujutsuaddon/addon/ability/limitless/Infinity/pressure/util/VelocityAnalyzer.java
package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util;

import net.minecraft.world.phys.Vec3;

/**
 * 速度分析工具
 *
 * 计算接近速度、分解径向/切向分量
 * 用于投射物、掉落物、生物、客户端输入
 */
public final class VelocityAnalyzer {

    private VelocityAnalyzer() {}

    // ==================== 速度分解结果 ====================

    public static class VelocityComponents {
        /** 径向分量（朝向目标方向）正=接近，负=远离 */
        public final double radial;
        /** 切向分量（垂直于径向） */
        public final Vec3 tangent;
        /** 径向方向向量（从位置指向目标） */
        public final Vec3 radialDirection;

        public VelocityComponents(double radial, Vec3 tangent, Vec3 radialDirection) {
            this.radial = radial;
            this.tangent = tangent;
            this.radialDirection = radialDirection;
        }

        public boolean isApproaching() {
            return radial > 0.01;
        }

        public boolean isRetreating() {
            return radial < -0.01;
        }

        public boolean isStationary() {
            return Math.abs(radial) <= 0.01 && tangent.lengthSqr() < 0.0001;
        }
    }

    // ==================== 核心方法 ====================

    /**
     * 分解速度为径向和切向分量
     *
     * @param position     当前位置
     * @param velocity     当前速度
     * @param targetCenter 目标中心（施术者位置）
     * @return 分解后的速度分量
     */
    public static VelocityComponents decompose(Vec3 position, Vec3 velocity, Vec3 targetCenter) {
        Vec3 toTarget = targetCenter.subtract(position);

        if (toTarget.lengthSqr() < 0.01) {
            // 太近，无法确定方向
            return new VelocityComponents(0, velocity, Vec3.ZERO);
        }

        Vec3 radialDirection = toTarget.normalize();

        // 径向分量 = 速度在径向方向的投影
        // 正值 = 向目标移动（接近）
        // 负值 = 远离目标
        double radialComponent = velocity.dot(radialDirection);

        // 切向分量 = 速度 - 径向部分
        Vec3 radialVector = radialDirection.scale(radialComponent);
        Vec3 tangentVector = velocity.subtract(radialVector);

        return new VelocityComponents(radialComponent, tangentVector, radialDirection);
    }

    /**
     * 快速检查：是否在接近目标
     */
    public static boolean isApproaching(Vec3 position, Vec3 velocity, Vec3 targetCenter) {
        return calculateApproachSpeed(position, velocity, targetCenter) > 0.01;
    }

    /**
     * 计算接近速度（正值=接近，负值=远离）
     */
    public static double calculateApproachSpeed(Vec3 position, Vec3 velocity, Vec3 targetCenter) {
        Vec3 toTarget = targetCenter.subtract(position);
        if (toTarget.lengthSqr() < 0.01) return 0;
        return velocity.dot(toTarget.normalize());
    }

    /**
     * 获取推力方向（从目标指向位置，即"推开"方向）
     */
    public static Vec3 getPushDirection(Vec3 position, Vec3 targetCenter) {
        Vec3 fromTarget = position.subtract(targetCenter);
        if (fromTarget.lengthSqr() < 0.01) {
            return new Vec3(0.1, 0, 0.1);  // 默认方向
        }
        return fromTarget.normalize();
    }

    /**
     * 获取水平推力方向（忽略Y轴）
     */
    public static Vec3 getHorizontalPushDirection(Vec3 position, Vec3 targetCenter) {
        Vec3 fromTarget = position.subtract(targetCenter);
        Vec3 horizontal = new Vec3(fromTarget.x, 0, fromTarget.z);
        if (horizontal.lengthSqr() < 0.01) {
            return new Vec3(1, 0, 0);
        }
        return horizontal.normalize();
    }
}
