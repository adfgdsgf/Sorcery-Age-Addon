package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家移动追踪器
 * 追踪玩家每帧的移动量
 */
public final class PlayerMovementTracker {

    private static final Map<UUID, MovementData> PLAYER_MOVEMENT = new ConcurrentHashMap<>();

    private PlayerMovementTracker() {}

    private static class MovementData {
        Vec3 lastPosition;
        Vec3 movement = Vec3.ZERO;
        long lastTick = -1;

        MovementData(Vec3 pos) {
            this.lastPosition = pos;
        }
    }

    /**
     * 每帧更新玩家移动（在服务端调用）
     */
    public static void update(Player player) {
        if (player.level().isClientSide) return;
        updateEntity(player);
    }

    /**
     * 通用版本：支持任意 LivingEntity
     */
    public static void updateEntity(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        UUID uuid = entity.getUUID();
        Vec3 currentPos = entity.position();
        long currentTick = entity.level().getGameTime();

        MovementData data = PLAYER_MOVEMENT.get(uuid);
        if (data == null) {
            PLAYER_MOVEMENT.put(uuid, new MovementData(currentPos));
            return;
        }

        if (data.lastTick != currentTick) {
            data.movement = currentPos.subtract(data.lastPosition);
            data.lastPosition = currentPos;
            data.lastTick = currentTick;
        }
    }

    /**
     * 获取实体这一帧的移动量
     */
    public static Vec3 getMovement(UUID entityUUID) {
        MovementData data = PLAYER_MOVEMENT.get(entityUUID);
        return data != null ? data.movement : Vec3.ZERO;
    }

    /**
     * 清理不活跃的数据
     */
    public static void cleanup(UUID entityUUID) {
        PLAYER_MOVEMENT.remove(entityUUID);
    }

    /**
     * 清理所有过期数据（定期调用）
     */
    public static void cleanupAll(long currentTick) {
        PLAYER_MOVEMENT.entrySet().removeIf(entry ->
                currentTick - entry.getValue().lastTick > 100);
    }
}
