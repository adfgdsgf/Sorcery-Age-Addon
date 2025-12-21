package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util;

import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

/**
 * 投射物方向检测器
 *
 * ★★★ 核心原理：射线-球体相交检测 ★★★
 *
 * 判断投射物的原始飞行路线（一条射线）是否会穿过平衡点（一个球体/墙）
 *
 * 射线：从投射物当前位置出发，沿原始飞行方向延伸
 * 球体：以玩家为圆心，平衡点半径为半径
 *
 * 如果射线穿过球体 → 投射物会撞到"墙" → 应该拦截
 * 如果射线不穿过球体 → 投射物会擦边飞过 → 应该释放
 */
public final class RaySphereChecker {

    private RaySphereChecker() {}

    /**
     * 检查投射物的原始飞行路线是否会穿过平衡点球体
     *
     * @param projectilePos   投射物当前位置
     * @param originalVelocity 原始飞行速度（捕获时保存的）
     * @param ownerCenter     玩家中心位置（球心）
     * @param balanceRadius   平衡点半径（墙的位置）
     * @return true = 会穿过墙（应拦截），false = 不会穿过（应释放）
     */
    public static boolean willHitBalancePoint(Vec3 projectilePos,
                                              @Nullable Vec3 originalVelocity,
                                              Vec3 ownerCenter,
                                              double balanceRadius) {
        // 没有原始速度数据，无法判断方向，保守处理：拦截
        if (originalVelocity == null || originalVelocity.lengthSqr() < 0.0001) {
            return true;
        }
        Vec3 direction = originalVelocity.normalize();

        // ★★★ 新增：检查玩家是不是在投射物的飞行方向前面 ★★★
        Vec3 toOwner = ownerCenter.subtract(projectilePos);
        double dot = toOwner.normalize().dot(direction);

        // dot <= 0 表示玩家在投射物后面或侧面，不可能被打到
        if (dot <= 0) {
            return false;  // 释放
        }
        // --- 下面是原来的逻辑 ---

        double distanceToOwner = projectilePos.distanceTo(ownerCenter);
        if (distanceToOwner <= balanceRadius) {
            return true;
        }
        // 射线-球体相交检测
        Vec3 V = projectilePos.subtract(ownerCenter);
        double vDotD = V.dot(direction);
        double vLenSq = V.lengthSqr();
        double rSq = balanceRadius * balanceRadius;
        double discriminantDiv4 = vDotD * vDotD - vLenSq + rSq;
        if (discriminantDiv4 < 0) {
            return false;
        }
        double sqrtDiscriminant = Math.sqrt(discriminantDiv4);
        double t2 = -vDotD + sqrtDiscriminant;
        if (t2 < 0) {
            return false;
        }
        return true;
    }

    /**
     * 计算射线到球心的最近距离
     * 可用于更精细的判断或调试
     *
     * @return 射线到球心的最近距离
     */
    public static double rayToPointDistance(Vec3 rayOrigin, Vec3 rayDirection, Vec3 point) {
        if (rayDirection.lengthSqr() < 0.0001) {
            return rayOrigin.distanceTo(point);
        }

        Vec3 dir = rayDirection.normalize();
        Vec3 toPoint = point.subtract(rayOrigin);

        // 投影长度（点在射线方向上的投影）
        double projection = toPoint.dot(dir);

        if (projection < 0) {
            // 点在射线起点后面，最近距离就是起点到点的距离
            return toPoint.length();
        }

        // 射线上离点最近的位置
        Vec3 closestOnRay = rayOrigin.add(dir.scale(projection));
        return closestOnRay.distanceTo(point);
    }

    /**
     * 带容差的检测（考虑投射物碰撞箱大小）
     *
     * @param projectileRadius 投射物的等效半径
     */
    public static boolean willHitBalancePointWithTolerance(Vec3 projectilePos,
                                                           @Nullable Vec3 originalVelocity,
                                                           Vec3 ownerCenter,
                                                           double balanceRadius,
                                                           double projectileRadius) {
        // 扩大球体半径来容纳投射物大小
        return willHitBalancePoint(projectilePos, originalVelocity, ownerCenter,
                balanceRadius + projectileRadius);
    }

    /**
     * 获取详细的检测结果（用于调试）
     */
    public static DetectionResult getDetailedResult(Vec3 projectilePos,
                                                    @Nullable Vec3 originalVelocity,
                                                    Vec3 ownerCenter,
                                                    double balanceRadius) {
        DetectionResult result = new DetectionResult();

        if (originalVelocity == null || originalVelocity.lengthSqr() < 0.0001) {
            result.willHit = true;
            result.reason = "无原始速度，保守拦截";
            return result;
        }

        result.distanceToOwner = projectilePos.distanceTo(ownerCenter);

        if (result.distanceToOwner <= balanceRadius) {
            result.willHit = true;
            result.reason = "已在平衡点内";
            return result;
        }

        Vec3 direction = originalVelocity.normalize();
        Vec3 V = projectilePos.subtract(ownerCenter);

        double vDotD = V.dot(direction);
        double vLenSq = V.lengthSqr();
        double rSq = balanceRadius * balanceRadius;
        double discriminantDiv4 = vDotD * vDotD - vLenSq + rSq;

        result.closestDistance = rayToPointDistance(projectilePos, originalVelocity, ownerCenter);

        if (discriminantDiv4 < 0) {
            result.willHit = false;
            result.reason = "射线不相交（最近距离: " + String.format("%.2f", result.closestDistance) + "）";
            return result;
        }

        double sqrtDiscriminant = Math.sqrt(discriminantDiv4);
        result.t1 = -vDotD - sqrtDiscriminant;
        result.t2 = -vDotD + sqrtDiscriminant;

        if (result.t2 < 0) {
            result.willHit = false;
            result.reason = "交点都在后方（已飞过）";
            return result;
        }

        result.willHit = true;
        result.reason = "会穿过墙（t1=" + String.format("%.2f", result.t1) +
                ", t2=" + String.format("%.2f", result.t2) + "）";
        return result;
    }

    /**
     * 检测结果详情（用于调试）
     */
    public static class DetectionResult {
        public boolean willHit = false;
        public String reason = "";
        public double distanceToOwner = 0;
        public double closestDistance = 0;
        public double t1 = 0;  // 近交点参数
        public double t2 = 0;  // 远交点参数
    }
}
