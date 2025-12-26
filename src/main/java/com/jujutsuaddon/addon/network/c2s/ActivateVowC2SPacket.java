package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.capability.vow.VowDataProvider;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.SyncVowListS2CPacket;
import com.jujutsuaddon.addon.vow.VowState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求激活誓约
 */
public class ActivateVowC2SPacket {

    private final UUID vowId;

    public ActivateVowC2SPacket(UUID vowId) {
        this.vowId = vowId;
    }

    public ActivateVowC2SPacket(FriendlyByteBuf buf) {
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

                // 状态检查
                if (vow.getState() == VowState.ACTIVE) {
                    player.sendSystemMessage(Component.translatable("vow.message.already_active"));
                    return;
                }
                // 违约状态必须先重置(Dissolve)才能激活
                if (vow.getState() == VowState.VIOLATED) {
                    player.sendSystemMessage(Component.translatable("vow.message.violated_cannot_activate"));
                    return;
                }

                // ★★★ 核心修复：创造模式逻辑 ★★★
                boolean isCreative = player.isCreative();

                // 1. 检查惩罚时间
                // 如果是创造模式，直接跳过这个检查
                if (!isCreative && data.isUnderPenalty()) {
                    long timeLeft = (data.getPenaltyEndTime() - System.currentTimeMillis()) / 1000;
                    player.sendSystemMessage(Component.translatable("vow.error.under_penalty", timeLeft)
                            .withStyle(ChatFormatting.RED));
                    return;
                }

                // 2. 检查组合占用 (Anti-Stacking)
                // 这个通常创造模式也要遵守，或者你可以决定创造模式是否也无视这个
                // 这里保持原样：创造模式也要遵守组合互斥
                if (!data.canActivateVow(vow)) {
                    player.sendSystemMessage(Component.translatable("vow.message.contains_used_pair"));
                    return;
                }

                // 执行激活
                boolean success = data.activateVow(vowId, player);

                if (success) {
                    // 同步数据
                    AddonNetwork.sendToPlayer(
                            new SyncVowListS2CPacket(
                                    data.getAllVows(),
                                    data.getOccupiedConditionOwners(),
                                    data.getPenaltyEndTime()
                            ),
                            player
                    );
                    player.sendSystemMessage(Component.translatable("vow.message.activated"));
                } else {
                    player.sendSystemMessage(Component.translatable("vow.message.activate_failed"));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
