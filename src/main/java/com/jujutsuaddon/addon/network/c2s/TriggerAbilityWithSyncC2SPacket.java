// 新建 TriggerAbilityWithSyncC2SPacket.java
package com.jujutsuaddon.addon.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.ability.AbilityHandler;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;

import java.util.function.Supplier;

public class TriggerAbilityWithSyncC2SPacket {

    private final ResourceLocation key;

    public TriggerAbilityWithSyncC2SPacket(ResourceLocation key) {
        this.key = key;
    }

    public TriggerAbilityWithSyncC2SPacket(FriendlyByteBuf buf) {
        this.key = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.key);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Ability ability = JJKAbilities.getValue(this.key);
            if (ability == null) return;

            // 触发技能
            AbilityHandler.trigger(player, ability);

            // ★★★ 关键：强制同步状态到客户端 ★★★
            player.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
