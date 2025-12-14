package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.compat.mob.MobConfigManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

public class MobCompatUtils {

    /**
     * 判断实体是否允许享受 JJK Addon 的增强逻辑
     * @param entity 目标实体 (通常是主人或攻击者)
     * @return true 如果是玩家或者在白名单内的生物
     */
    public static boolean isAllowed(LivingEntity entity) {
        if (entity == null) return false;

        // 1. 玩家永远允许
        if (entity instanceof Player) return true;

        // 2. [优化] 直接复用 MobConfigManager 的高速缓存
        // 如果能获取到配置，说明它在白名单里，也就是 Allowed
        return MobConfigManager.getMobConfig(entity) != null;
    }

    /**
     * 辅助判断：如果是驯服生物，检查其主人是否允许
     */
    public static boolean isOwnerAllowed(TamableAnimal tamable) {
        LivingEntity owner = tamable.getOwner();
        return isAllowed(owner);
    }
}
