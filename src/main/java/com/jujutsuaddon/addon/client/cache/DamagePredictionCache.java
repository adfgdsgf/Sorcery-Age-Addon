package com.jujutsuaddon.addon.client.cache;

import com.jujutsuaddon.addon.network.s2c.SyncDamagePredictionsS2CPacket.PredictionData;
import radon.jujutsu_kaisen.ability.base.Ability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DamagePredictionCache {

    private static final Map<String, PredictionData> CACHE = new ConcurrentHashMap<>();
    private static long lastUpdateTime = 0;

    // ★★★ 只要有数据就返回，不再检查时间 ★★★

    public static void update(Map<String, PredictionData> predictions) {
        CACHE.clear();
        CACHE.putAll(predictions);
        lastUpdateTime = System.currentTimeMillis();
    }

    public static PredictionData get(Ability ability) {
        if (ability == null) return null;
        return CACHE.get(ability.getClass().getName());
    }

    /**
     * ★★★ 修改：只要有数据就认为有效 ★★★
     */
    public static boolean isValid() {
        return !CACHE.isEmpty();
    }

    /**
     * 是否需要刷新（用于定期请求服务端更新）
     */
    public static boolean needsRefresh() {
        return System.currentTimeMillis() - lastUpdateTime > 2000;
    }

    public static void clear() {
        CACHE.clear();
        lastUpdateTime = 0;
    }
}
