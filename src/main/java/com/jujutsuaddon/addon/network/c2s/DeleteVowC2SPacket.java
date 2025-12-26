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
 * 客户端 → 服务端：请求删除誓约
 */
public class DeleteVowC2SPacket {

    private final UUID vowId;

    public DeleteVowC2SPacket(UUID vowId) {
        this.vowId = vowId;
    }

    public DeleteVowC2SPacket(FriendlyByteBuf buf) {
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

                // 只能删除非激活状态，或者创造模式
                if (vow.getState() != VowState.ACTIVE || player.isCreative()) {

                    // 如果是激活状态（创造模式强制删除），先停用
                    if (vow.getState() == VowState.ACTIVE) {
                        // ★★★ 关键修复：通过 data.deactivateVow() 调用 ★★★
                        data.deactivateVow(vowId, player, DeactivateReason.DISSOLVED);
                    }

                    // 删除誓约（内部会调用 releaseUsedPairs/releaseOccupancy）
                    data.removeVow(vowId);

                    // ★★★ 关键修复：同步时传递 getOccupiedConditionOwners 和 getPenaltyEndTime ★★★
                    AddonNetwork.sendToPlayer(
                            new SyncVowListS2CPacket(
                                    data.getAllVows(),
                                    data.getOccupiedConditionOwners(),
                                    data.getPenaltyEndTime()
                            ),
                            player
                    );

                    player.sendSystemMessage(Component.translatable("vow.message.deleted"));
                } else {
                    player.sendSystemMessage(Component.translatable("vow.message.cannot_delete_active"));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
