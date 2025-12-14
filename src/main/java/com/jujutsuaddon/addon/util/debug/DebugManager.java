package com.jujutsuaddon.addon.util.debug;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DebugManager {
    // 存储开启了调试模式的玩家 UUID
    private static final Set<UUID> DEBUGGERS = new HashSet<>();

    public static void setDebugging(Player player, boolean enable) {
        if (enable) {
            DEBUGGERS.add(player.getUUID());
        } else {
            DEBUGGERS.remove(player.getUUID());
        }
    }

    public static boolean isDebugging(Player player) {
        if (player == null) return false;
        // 逻辑：如果玩家在列表中，或者全局配置强制开启，则返回 true
        // 这样既保留了单人控制，也保留了管理员在后台强制开启全局调试的能力
        return DEBUGGERS.contains(player.getUUID()) || AddonConfig.COMMON.debugMode.get();
    }
}
