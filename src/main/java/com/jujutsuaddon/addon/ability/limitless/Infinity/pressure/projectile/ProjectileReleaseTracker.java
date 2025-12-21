package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 投射物释放追踪器
 *
 * 管理释放冷却，区分"软释放"和"硬释放"
 *
 * - 软释放：投射物从停止区退到减速区，短CD（5 tick）
 * - 硬释放：投射物完全离开范围或路线不经过墙，中CD（15 tick）
 */
public final class ProjectileReleaseTracker {

    private static final Map<UUID, Long> RELEASE_COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, ReleaseType> RELEASE_TYPE = new ConcurrentHashMap<>();

    /** 软释放冷却（从停止区退到减速区）*/
    public static final int SOFT_COOLDOWN_TICKS = 5;

    /** 硬释放冷却（完全离开或路线不经过）*/
    public static final int HARD_COOLDOWN_TICKS = 15;

    public enum ReleaseType {
        SOFT,  // 退到减速区
        HARD   // 完全离开
    }

    private ProjectileReleaseTracker() {}

    /**
     * 标记软释放（还在范围内）
     */
    public static void markSoftRelease(UUID projectileId, long currentTick) {
        RELEASE_COOLDOWN.put(projectileId, currentTick);
        RELEASE_TYPE.put(projectileId, ReleaseType.SOFT);
    }

    /**
     * 标记硬释放（完全离开）
     */
    public static void markHardRelease(UUID projectileId, long currentTick) {
        RELEASE_COOLDOWN.put(projectileId, currentTick);
        RELEASE_TYPE.put(projectileId, ReleaseType.HARD);
    }

    /**
     * 检查是否在冷却中
     */
    public static boolean isInCooldown(UUID projectileId, long currentTick) {
        Long releaseTime = RELEASE_COOLDOWN.get(projectileId);
        if (releaseTime == null) return false;

        ReleaseType type = RELEASE_TYPE.getOrDefault(projectileId, ReleaseType.HARD);
        int cooldownTicks = (type == ReleaseType.SOFT) ? SOFT_COOLDOWN_TICKS : HARD_COOLDOWN_TICKS;

        return currentTick - releaseTime < cooldownTicks;
    }

    /**
     * 清除冷却
     */
    public static void clearCooldown(UUID projectileId) {
        RELEASE_COOLDOWN.remove(projectileId);
        RELEASE_TYPE.remove(projectileId);
    }

    /**
     * 定期清理过期数据
     */
    public static void cleanup(long currentTick) {
        RELEASE_COOLDOWN.entrySet().removeIf(entry ->
                currentTick - entry.getValue() > HARD_COOLDOWN_TICKS * 2);
        RELEASE_TYPE.keySet().retainAll(RELEASE_COOLDOWN.keySet());
    }
}
