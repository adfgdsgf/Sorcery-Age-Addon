package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.mixin.access.SorcererDataAccess;
import com.jujutsuaddon.addon.mixin.access.TenShadowsDataAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.Adaptation;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;
import radon.jujutsu_kaisen.network.packet.s2c.SyncTenShadowsDataS2CPacket;

import java.util.Iterator;
import java.util.Map;

/**
 * 适应性逻辑的核心工具类
 * 负责处理底层数据修改、Mixin 访问和网络同步
 */
public class AdaptationUtil {

    /**
     * 安全地清除脑部损伤
     */
    public static void clearBrainDamage(LivingEntity entity, ISorcererData sorcererCap) {
        if (sorcererCap instanceof SorcererDataAccess access) {
            if (access.getBrainDamage() > 0) {
                access.setBrainDamage(0);
                sorcererCap.maxOutput(); // 恢复咒力输出

                // 移除负面状态
                if (sorcererCap.hasDisable()) sorcererCap.resetDisable();
                if (sorcererCap.hasSilenced()) sorcererCap.resetSilenced();

                // 同步数据
                if (entity instanceof ServerPlayer serverPlayer) {
                    PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(sorcererCap.serializeNBT()), serverPlayer);
                }
            }
        }
    }

    /**
     * 处理适应度衰减逻辑
     * @return 是否发生了数据变动
     */
    public static boolean performDecay(ITenShadowsData shadowCap) {
        if (!(shadowCap instanceof TenShadowsDataAccess access)) return false;

        Map<Adaptation, Integer> adaptingMap = access.getAdaptingMap();
        if (adaptingMap == null || adaptingMap.isEmpty()) return false;

        boolean changed = false;
        Iterator<Map.Entry<Adaptation, Integer>> iterator = adaptingMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Adaptation, Integer> entry = iterator.next();
            int currentProgress = entry.getValue();

            if (currentProgress > 0) {
                // 每次衰减 1 点，或者您可以自定义衰减速度
                entry.setValue(currentProgress - 1);
                changed = true;
            } else {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 同步十种影法术数据
     */
    public static void syncShadowData(LivingEntity entity, ITenShadowsData shadowCap) {
        if (entity instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToClient(new SyncTenShadowsDataS2CPacket(shadowCap.serializeNBT()), serverPlayer);
        }
    }
}
