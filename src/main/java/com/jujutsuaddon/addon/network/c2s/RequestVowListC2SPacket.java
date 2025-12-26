package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.capability.vow.IVowData;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.SyncVowListS2CPacket;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.manager.VowManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 请求誓约列表包 (C2S)
 * 客户端 -> 服务端
 */
public class RequestVowListC2SPacket {

    public RequestVowListC2SPacket() {}
    public RequestVowListC2SPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 1. 获取玩家誓约
            Collection<CustomBindingVow> vows = VowManager.getPlayerVows(player);

            // 2. ★ 修改：获取条件占用信息 (原 getUsedPairOwners 已废弃)
            // 这里调用的是我们在 VowManager 里新加的方法
            Map<String, UUID> occupiedConditions = VowManager.getOccupiedConditionOwners(player);

            // 3. 获取惩罚时间
            long penaltyEndTime = 0L;
            IVowData data = VowManager.getVowData(player);
            if (data != null) {
                penaltyEndTime = data.getPenaltyEndTime();
            }

            // 4. 发送完整数据回客户端
            // SyncVowListS2CPacket 的构造函数参数顺序是：列表, 占用Map, 惩罚时间
            AddonNetwork.sendToPlayer(
                    new SyncVowListS2CPacket(vows, occupiedConditions, penaltyEndTime),
                    player
            );
        });

        ctx.get().setPacketHandled(true);
    }
}
