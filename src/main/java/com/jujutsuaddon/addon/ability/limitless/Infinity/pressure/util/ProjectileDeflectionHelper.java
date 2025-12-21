package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util;

import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * 投射物偏转计算工具
 *
 * ★★★ 物理偏转模型 ★★★
 *
 * 基于动量守恒：
 * - 投射物有"动量"（方向 × 惯性质量）
 * - 外力产生"冲量"（力方向 × 力大小）
 * - 新动量 = 旧动量 + 冲量
 * - 新方向 = normalize(新动量)
 *
 * 自然结果：
 * - 正面对冲：动量和冲量方向相反，需要多次累积才能反向
 * - 侧面来袭：动量和冲量垂直叠加，很快就偏离
 */
public final class ProjectileDeflectionHelper {

    private ProjectileDeflectionHelper() {}

    // ==================== 偏转结果 ====================

    public static class DeflectionResult {
        /** 偏转后的新方向（归一化） */
        public final Vec3 newDirection;

        /** 偏转角度（弧度） */
        public final double deflectionAngle;

        /** 偏转角度（度） */
        public final double deflectionDegrees;

        /** 新动量向量（未归一化） */
        public final Vec3 newMomentum;

        public DeflectionResult(Vec3 newDirection, double deflectionAngle, Vec3 newMomentum) {
            this.newDirection = newDirection;
            this.deflectionAngle = deflectionAngle;
            this.deflectionDegrees = Math.toDegrees(deflectionAngle);
            this.newMomentum = newMomentum;
        }

        /**
         * 是否发生了显著偏转（超过5度）
         */
        public boolean isSignificant() {
            return deflectionDegrees > 5.0;
        }

        /**
         * 是否几乎反向了（超过90度）
         */
        public boolean isReversed() {
            return deflectionDegrees > 90.0;
        }
    }

    // ==================== 核心计算 ====================

    /**
     * 计算物理偏转
     *
     * @param currentDirection 当前飞行方向（应已归一化）
     * @param impulseDirection 冲量/推力方向（应已归一化）
     * @param impulseMagnitude 冲量大小
     * @param inertiaMass      惯性质量（越大越难偏转）
     * @return 偏转结果
     */
    public static DeflectionResult calculate(Vec3 currentDirection,
                                             Vec3 impulseDirection,
                                             double impulseMagnitude,
                                             double inertiaMass) {
        // 确保方向归一化
        Vec3 dir = currentDirection.normalize();
        Vec3 impulseDir = impulseDirection.normalize();

        // 原始动量 = 方向 × 惯性质量
        Vec3 originalMomentum = dir.scale(inertiaMass);

        // 冲量 = 冲量方向 × 冲量大小
        Vec3 impulse = impulseDir.scale(impulseMagnitude);

        // 新动量 = 原始动量 + 冲量
        Vec3 newMomentum = originalMomentum.add(impulse);

        // 归一化得到新方向
        Vec3 newDirection;
        if (newMomentum.lengthSqr() < 0.0001) {
            // 动量抵消（极少数情况），使用冲量方向
            newDirection = impulseDir;
            newMomentum = impulseDir;
        } else {
            newDirection = newMomentum.normalize();
        }

        // 计算偏转角度
        double dot = dir.dot(newDirection);
        dot = clamp(dot, -1.0, 1.0);  // 防止浮点误差
        double deflectionAngle = Math.acos(dot);

        return new DeflectionResult(newDirection, deflectionAngle, newMomentum);
    }

    /**
     * 简化版：使用默认惯性质量
     */
    public static DeflectionResult calculate(Vec3 currentDirection,
                                             Vec3 impulseDirection,
                                             double impulseMagnitude) {
        return calculate(currentDirection, impulseDirection, impulseMagnitude, 1.0);
    }

    /**
     * 推动场景专用：
     * 将 pushDistance 转换为冲量
     *
     * @param currentDirection 当前飞行方向
     * @param radialDirection  径向方向（从中心指向投射物）
     * @param pushDistance     推动距离
     * @param impulseScale     冲量缩放系数
     * @return 偏转结果
     */
    public static DeflectionResult calculateFromPush(Vec3 currentDirection,
                                                     Vec3 radialDirection,
                                                     double pushDistance,
                                                     double impulseScale) {
        double impulseMagnitude = pushDistance * impulseScale;
        return calculate(currentDirection, radialDirection, impulseMagnitude, 1.0);
    }

    /**
     * 推动场景：使用默认缩放系数
     */
    public static DeflectionResult calculateFromPush(Vec3 currentDirection,
                                                     Vec3 radialDirection,
                                                     double pushDistance) {
        return calculateFromPush(currentDirection, radialDirection, pushDistance, 2.0);
    }

    // ==================== 方向提取 ====================

    /**
     * 获取投射物当前飞行方向
     * 按优先级尝试多个来源
     */
    public static Vec3 getFlightDirection(Projectile projectile) {
        if (projectile instanceof IFrozenProjectile fp) {
            return getFlightDirection(fp, projectile);
        }

        // 非冻结投射物，直接用速度
        Vec3 vel = projectile.getDeltaMovement();
        if (vel.lengthSqr() > 0.01) {
            return vel.normalize();
        }

        // 用朝向
        return getDirectionFromRotation(projectile);
    }

    /**
     * 获取被控制投射物的飞行方向
     */
    public static Vec3 getFlightDirection(IFrozenProjectile fp, Projectile projectile) {
        // 1. 优先使用存储的当前方向
        Vec3 stored = fp.jujutsuAddon$getCurrentDirection();
        if (stored != null && stored.lengthSqr() > 0.01) {
            return stored.normalize();
        }

        // 2. 其次使用原始速度方向
        Vec3 originalVel = fp.jujutsuAddon$getOriginalVelocity();
        if (originalVel != null && originalVel.lengthSqr() > 0.01) {
            return originalVel.normalize();
        }

        // 3. 使用当前速度
        Vec3 currentVel = projectile.getDeltaMovement();
        if (currentVel.lengthSqr() > 0.01) {
            return currentVel.normalize();
        }

        // 4. 最后用朝向
        return getDirectionFromRotation(projectile);
    }

    /**
     * 从投射物旋转角度计算方向
     */
    public static Vec3 getDirectionFromRotation(Projectile projectile) {
        float yaw = projectile.getYRot();
        float pitch = projectile.getXRot();
        return getDirectionFromRotation(yaw, pitch);
    }

    /**
     * 从旋转角度计算方向向量
     */
    public static Vec3 getDirectionFromRotation(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        Vec3 dir = new Vec3(x, y, z);
        return dir.lengthSqr() > 0.01 ? dir.normalize() : new Vec3(0, 0, 1);
    }

    // ==================== 方向判断 ====================

    /**
     * 检查方向是否朝向目标点
     *
     * @param position  当前位置
     * @param direction 方向向量
     * @param target    目标点
     * @param threshold 阈值（dot > threshold 认为朝向）
     * @return true = 朝向目标
     */
    public static boolean isFacingTarget(Vec3 position, Vec3 direction, Vec3 target, double threshold) {
        Vec3 toTarget = target.subtract(position);
        if (toTarget.lengthSqr() < 0.01) {
            return true;  // 太近，保守处理
        }
        double dot = direction.normalize().dot(toTarget.normalize());
        return dot > threshold;
    }

    /**
     * 使用默认阈值检查
     */
    public static boolean isFacingTarget(Vec3 position, Vec3 direction, Vec3 target) {
        return isFacingTarget(position, direction, target, 0.1);
    }

    /**
     * 计算两个方向之间的夹角（弧度）
     */
    public static double angleBetween(Vec3 dir1, Vec3 dir2) {
        double dot = dir1.normalize().dot(dir2.normalize());
        dot = clamp(dot, -1.0, 1.0);
        return Math.acos(dot);
    }

    /**
     * 计算两个方向之间的夹角（度）
     */
    public static double angleBetweenDegrees(Vec3 dir1, Vec3 dir2) {
        return Math.toDegrees(angleBetween(dir1, dir2));
    }

    // ==================== 辅助方法 ====================

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
