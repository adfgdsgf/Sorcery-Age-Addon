package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.config.AddonConfig;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectileHitTracker {

    // 投射物UUID -> 已命中的实体UUID集合
    private static final Map<UUID, Set<UUID>> HIT_CACHE = new ConcurrentHashMap<>();

    // 类级别缓存：Entity类 -> 是否是单次命中投射物
    private static final Map<Class<?>, Boolean> CLASS_CACHE = new ConcurrentHashMap<>();

    // 配置缓存
    private static List<String> singleHitPatterns = null;
    private static long lastConfigLoad = 0;
    private static final long CONFIG_REFRESH_INTERVAL = 5000; // 5秒

    /**
     * 获取单次命中投射物的匹配模式
     */
    private static List<String> getSingleHitPatterns() {
        long now = System.currentTimeMillis();
        // 每5秒重新加载配置，支持热重载
        if (singleHitPatterns == null || now - lastConfigLoad > CONFIG_REFRESH_INTERVAL) {
            List<String> newPatterns = new ArrayList<>(AddonConfig.COMMON.singleHitProjectiles.get());

            // 如果配置发生变化，清除类缓存
            if (singleHitPatterns != null && !singleHitPatterns.equals(newPatterns)) {
                CLASS_CACHE.clear();
            }

            singleHitPatterns = newPatterns;
            lastConfigLoad = now;
        }
        return singleHitPatterns;
    }

    /**
     * 检查实体是否是单次命中投射物
     * 使用类级别缓存优化性能
     */
    public static boolean isSingleHitProjectile(Entity entity) {
        if (entity == null) return false;

        // 类级别缓存，每个类只计算一次
        return CLASS_CACHE.computeIfAbsent(entity.getClass(), clazz -> {
            String className = clazz.getSimpleName();
            String fullClassName = clazz.getName();

            for (String pattern : getSingleHitPatterns()) {
                if (className.contains(pattern) || fullClassName.contains(pattern)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * 检查是否应该阻止这次命中（已经命中过）
     * @return true = 阻止伤害, false = 允许伤害
     */
    public static boolean shouldBlockHit(Entity projectile, Entity target) {
        if (projectile == null || target == null) return false;

        UUID projectileId = projectile.getUUID();
        UUID targetId = target.getUUID();

        Set<UUID> hitSet = HIT_CACHE.computeIfAbsent(projectileId, k -> ConcurrentHashMap.newKeySet());

        if (hitSet.contains(targetId)) {
            return true; // 已经命中过，阻止这次伤害
        }

        hitSet.add(targetId);
        return false; // 第一次命中，允许伤害
    }

    /**
     * 清理投射物的命中记录
     */
    public static void clearProjectile(UUID projectileId) {
        if (projectileId != null) {
            HIT_CACHE.remove(projectileId);
        }
    }

    /**
     * 清理所有缓存（可用于世界卸载时）
     */
    public static void clearAll() {
        HIT_CACHE.clear();
    }

    /**
     * 强制刷新配置缓存
     * 同时清除类缓存，确保新配置生效
     */
    public static void reloadConfig() {
        singleHitPatterns = null;
        CLASS_CACHE.clear();
    }

    /**
     * 获取当前缓存状态（调试用）
     */
    public static String getDebugInfo() {
        return String.format("HIT_CACHE: %d projectiles, CLASS_CACHE: %d classes",
                HIT_CACHE.size(), CLASS_CACHE.size());
    }
}
