package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 投射物物理参数辅助类
 *
 * 动态获取：
 * - drag/inertia（速度衰减）
 * - gravity（重力）
 * - 最大下落速度
 */
public class ProjectileDragHelper {

    /** 模组可以注册特定投射物的 drag 值 */
    private static final Map<Class<? extends Projectile>, Double> CUSTOM_DRAG = new HashMap<>();

    /** 模组可以注册特定投射物的 gravity 值 */
    private static final Map<Class<? extends Projectile>, Double> CUSTOM_GRAVITY = new HashMap<>();

    /** 缓存反射获取的方法 */
    private static Method cachedInertiaMethod = null;
    private static Method cachedGravityMethod = null;
    private static boolean inertiaMethodSearched = false;
    private static boolean gravityMethodSearched = false;

    // ==================== 注册接口 ====================

    public static void registerDrag(Class<? extends Projectile> clazz, double drag) {
        CUSTOM_DRAG.put(clazz, drag);
    }

    public static void registerGravity(Class<? extends Projectile> clazz, double gravity) {
        CUSTOM_GRAVITY.put(clazz, gravity);
    }

    // ==================== Drag（速度衰减）====================

    /**
     * 获取投射物的速度衰减率
     */
    public static double getDrag(Projectile projectile) {
        // 1. 检查自定义注册
        for (Map.Entry<Class<? extends Projectile>, Double> entry : CUSTOM_DRAG.entrySet()) {
            if (entry.getKey().isInstance(projectile)) {
                return entry.getValue();
            }
        }

        // 2. AbstractArrow：调用原版的 getInertia() 方法
        if (projectile instanceof AbstractArrow arrow) {
            return getArrowInertia(arrow);
        }

        // 3. ThrowableProjectile（雪球、鸡蛋等）
        if (projectile instanceof ThrowableProjectile throwable) {
            return throwable.isInWater() ? 0.8 : 0.99;
        }

        // 4. AbstractHurtingProjectile（火焰弹等）- 没有衰减
        if (projectile instanceof AbstractHurtingProjectile) {
            return 1.0;
        }

        // 5. 默认值
        return projectile.isInWater() ? 0.8 : 0.99;
    }

    private static float getArrowInertia(AbstractArrow arrow) {
        if (!inertiaMethodSearched) {
            try {
                cachedInertiaMethod = AbstractArrow.class.getDeclaredMethod("getInertia");
                cachedInertiaMethod.setAccessible(true);
            } catch (Exception e) {
                cachedInertiaMethod = null;
            }
            inertiaMethodSearched = true;
        }

        if (cachedInertiaMethod != null) {
            try {
                return (float) cachedInertiaMethod.invoke(arrow);
            } catch (Exception ignored) {}
        }

        // 回退
        if (arrow.isInWater()) return 0.6F;
        if (arrow.isNoPhysics()) return 1.0F;
        return 0.99F;
    }

    // ==================== Gravity（重力）====================

    /**
     * 获取投射物的重力值（每tick下落速度增量）
     *
     * 原版值：
     * - AbstractArrow: 0.05
     * - ThrowableProjectile: 0.03
     * - AbstractHurtingProjectile: 0（无重力）
     */
    public static double getGravity(Projectile projectile) {
        // 1. 检查自定义注册
        for (Map.Entry<Class<? extends Projectile>, Double> entry : CUSTOM_GRAVITY.entrySet()) {
            if (entry.getKey().isInstance(projectile)) {
                return entry.getValue();
            }
        }

        // 2. AbstractArrow：尝试调用 getGravity()
        if (projectile instanceof AbstractArrow arrow) {
            return getArrowGravity(arrow);
        }

        // 3. ThrowableProjectile
        if (projectile instanceof ThrowableProjectile throwable) {
            return getThrowableGravity(throwable);
        }

        // 4. AbstractHurtingProjectile - 无重力
        if (projectile instanceof AbstractHurtingProjectile) {
            return 0;
        }

        // 5. 默认
        return 0.05;
    }

    private static double getArrowGravity(AbstractArrow arrow) {
        // 原版 AbstractArrow 没有 getGravity() 方法，重力是硬编码的 0.05
        // 但有些模组可能会覆盖
        if (!gravityMethodSearched) {
            try {
                // 尝试查找 getGravity 方法（如果模组添加了的话）
                cachedGravityMethod = arrow.getClass().getMethod("getGravity");
                cachedGravityMethod.setAccessible(true);
            } catch (Exception e) {
                cachedGravityMethod = null;
            }
            gravityMethodSearched = true;
        }

        if (cachedGravityMethod != null) {
            try {
                Object result = cachedGravityMethod.invoke(arrow);
                if (result instanceof Number) {
                    return Math.abs(((Number) result).doubleValue());
                }
            } catch (Exception ignored) {}
        }

        // 水中重力更大（下沉更快？不对，原版水中 drag 更大，不是重力更大）
        // 原版箭矢在水中重力不变，只是 drag 变小
        return 0.05;
    }

    private static double getThrowableGravity(ThrowableProjectile throwable) {
        // ThrowableProjectile.getGravity() 是 protected 的，默认返回 0.03
        try {
            Method method = ThrowableProjectile.class.getDeclaredMethod("getGravity");
            method.setAccessible(true);
            return Math.abs((float) method.invoke(throwable));
        } catch (Exception e) {
            return 0.03;
        }
    }

    // ==================== 最大下落速度 ====================

    /**
     * 获取投射物的最大下落速度
     *
     * Minecraft 物理：终端速度约为 78.4 格/秒 = 3.92 格/tick
     * 但投射物通常在达到这个速度之前就落地了
     */
    public static double getMaxFallSpeed(Projectile projectile) {
        // 使用物理公式：终端速度 = 重力 / (1 - drag)
        double gravity = getGravity(projectile);
        double drag = getDrag(projectile);

        if (drag >= 1.0) {
            // 无衰减，理论上无限加速，使用默认值
            return 3.92;
        }

        // 终端速度公式
        double terminalVelocity = gravity / (1.0 - drag);

        // 限制在合理范围内
        return Math.min(terminalVelocity, 3.92);
    }

    // ==================== 理论最大飞行距离 ====================

    /**
     * 计算理论最大飞行距离（水平）
     */
    public static double calculateTheoreticalMaxDistance(double initialSpeed, double drag) {
        if (drag >= 1.0) {
            return Double.MAX_VALUE;
        }
        if (drag <= 0) {
            return initialSpeed;
        }
        return initialSpeed / (1.0 - drag);
    }

    /**
     * 根据剩余距离计算当前应有的速度
     */
    public static double calculateSpeedFromRemainingDistance(double remainingDistance, double drag) {
        if (drag >= 1.0) {
            return remainingDistance > 0 ? 1.0 : 0;
        }
        if (remainingDistance <= 0) {
            return 0;
        }
        return remainingDistance * (1.0 - drag);
    }

    /**
     * 计算剩余可飞行距离
     */
    public static double calculateRemainingDistance(double maxDistance, double consumedDistance) {
        return Math.max(0, maxDistance - consumedDistance);
    }
}
