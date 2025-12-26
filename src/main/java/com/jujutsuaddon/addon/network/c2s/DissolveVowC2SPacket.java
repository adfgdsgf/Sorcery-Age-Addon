package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.capability.vow.VowDataProvider;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.SyncVowListS2CPacket;
import com.jujutsuaddon.addon.vow.VowState;
import com.jujutsuaddon.addon.vow.manager.DeactivateReason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求解除/停用誓约
 * 同时也用于：重置已违约(VIOLATED)或已过期(EXPIRED)的誓约
 */
public class DissolveVowC2SPacket {

    private final UUID vowId;

    public DissolveVowC2SPacket(UUID vowId) {
        this.vowId = vowId;
    }

    public DissolveVowC2SPacket(FriendlyByteBuf buf) {
        this.vowId = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(vowId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(VowDataProvider.VOW_DATA).ifPresent(data -> {
                var vow = data.getVow(vowId);
                if (vow == null) {
                    player.sendSystemMessage(Component.translatable("vow.error.not_found"));
                    return;
                }

                // 1. 状态检查
                if (vow.getState() == VowState.DISSOLVED) {
                    return;
                }

                boolean isCreative = player.isCreative();

                // 2. 永久誓约检查 (创造模式无视)
                if (vow.isPermanent() && !isCreative) {
                    player.sendSystemMessage(Component.translatable("vow.message.cannot_dissolve_permanent"));
                    return;
                }

                // ★★★ 核心修改：删除了施加惩罚的逻辑 ★★★
                // 主动解除誓约(Deactivate)不应该受到惩罚，只有被动违约(Violation)才应该受罚。
                // 这样你在生存模式关闭誓约时，就不会重置惩罚时间了。

                // 3. 强制修改状态
                vow.setState(VowState.DISSOLVED);

                // 4. 执行清理
                data.deactivateVow(vowId, player, DeactivateReason.DISSOLVED);

                // 5. 同步数据
                AddonNetwork.sendToPlayer(
                        new SyncVowListS2CPacket(
                                data.getAllVows(),
                                data.getOccupiedConditionOwners(),
                                data.getPenaltyEndTime()
                        ),
                        player
                );

                // 6. 反馈消息
                if (vow.getState() == VowState.VIOLATED || vow.getState() == VowState.EXPIRED) {
                    player.sendSystemMessage(Component.translatable("vow.message.reset_success"));
                } else {
                    player.sendSystemMessage(Component.translatable("vow.message.dissolved"));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
