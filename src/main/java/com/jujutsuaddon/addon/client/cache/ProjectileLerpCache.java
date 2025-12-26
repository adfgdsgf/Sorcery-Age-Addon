package com.jujutsuaddon.addon.client.cache;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端投射物插值缓存
 * 用于平滑显示被推动的投射物
 */
public class ProjectileLerpCache {

    private static final Map<Integer, LerpData> LERP_MAP = new ConcurrentHashMap<>();

    public static class LerpData {
        public final Vec3 startPos;
        public final Vec3 targetPos;
        public final int totalTicks;
        public int remainingTicks;

        public LerpData(Vec3 startPos, Vec3 targetPos, int totalTicks) {
            this.startPos = startPos;
            this.targetPos = targetPos;
            this.totalTicks = totalTicks;
            this.remainingTicks = totalTicks;
        }

        /**
         * 获取当前插值进度 (0.0 ~ 1.0)
         */
        public double getProgress() {
            if (totalTicks <= 0) return 1.0;
            return 1.0 - ((double) remainingTicks / totalTicks);
        }

        /**
         * 获取当前插值位置
         */
        public Vec3 getCurrentPos() {
            double t = getProgress();
            // 使用平滑插值 (ease-out)
            t = 1.0 - Math.pow(1.0 - t, 2);
            return startPos.lerp(targetPos, t);
        }
    }

    /**
     * 添加插值任务
     */
    public static void addLerp(int entityId, Vec3 targetPos, int lerpTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) return;

        Vec3 startPos = entity.position();

        // 如果已经有插值任务，从当前插值位置开始
        LerpData existing = LERP_MAP.get(entityId);
        if (existing != null && existing.remainingTicks > 0) {
            startPos = existing.getCurrentPos();
        }

        LERP_MAP.put(entityId, new LerpData(startPos, targetPos, lerpTicks));
    }

    /**
     * 每帧调用，更新插值并应用到实体
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            LERP_MAP.clear();
            return;
        }

        Iterator<Map.Entry<Integer, LerpData>> it = LERP_MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, LerpData> entry = it.next();
            int entityId = entry.getKey();
            LerpData data = entry.getValue();

            Entity entity = mc.level.getEntity(entityId);
            if (entity == null) {
                it.remove();
                continue;
            }

            // 更新位置
            Vec3 currentPos = data.getCurrentPos();
            entity.setPos(currentPos.x, currentPos.y, currentPos.z);

            // 减少剩余帧数
            data.remainingTicks--;

            // 完成后移除
            if (data.remainingTicks <= 0) {
                // 确保最终位置精确
                entity.setPos(data.targetPos.x, data.targetPos.y, data.targetPos.z);
                it.remove();
            }
        }
    }

    /**
     * 检查实体是否正在插值中
     */
    public static boolean isLerping(int entityId) {
        LerpData data = LERP_MAP.get(entityId);
        return data != null && data.remainingTicks > 0;
    }

    /**
     * 清除指定实体的插值
     */
    public static void remove(int entityId) {
        LERP_MAP.remove(entityId);
    }

    /**
     * 清除所有插值
     */
    public static void clear() {
        LERP_MAP.clear();
    }
}
