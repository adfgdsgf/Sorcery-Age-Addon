package com.jujutsuaddon.addon.util.debug;

import java.util.*;

public class DebugState {
    // 记录每 tick 的伤害累积
    private static final Map<UUID, Float> tickDamageAccumulator = new HashMap<>();
    // 记录已经报告过 Tier 信息的实体 ID
    private static final Set<Integer> tierDebugCache = new HashSet<>();
    // --- 伤害累积逻辑 ---
    public static void accumulate(UUID uuid, float amount) {
        tickDamageAccumulator.merge(uuid, amount, Float::sum);
    }
    public static float getAndClearDamage(UUID uuid) {
        if (tickDamageAccumulator.containsKey(uuid)) {
            return tickDamageAccumulator.remove(uuid);
        }
        return 0f;
    }
    // ★★★ 新增：获取当前累积值（不清除）★★★
    public static float getCurrentDamage(UUID uuid) {
        return tickDamageAccumulator.getOrDefault(uuid, 0f);
    }
    // --- Tier 缓存逻辑 ---
    public static boolean hasLoggedTier(int entityId) {
        return tierDebugCache.contains(entityId);
    }
    public static void markTierLogged(int entityId) {
        tierDebugCache.add(entityId);
    }
    // 如果需要重置缓存（比如玩家重进存档时），可以加个 clear 方法
    public static void clearAll() {
        tickDamageAccumulator.clear();
        tierDebugCache.clear();
    }
}
