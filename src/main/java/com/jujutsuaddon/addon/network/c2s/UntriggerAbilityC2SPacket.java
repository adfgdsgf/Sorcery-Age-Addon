package com.jujutsuaddon.addon.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.ability.AbilityHandler;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;

import java.util.function.Supplier;

public class UntriggerAbilityC2SPacket {
    private final ResourceLocation key;

    public UntriggerAbilityC2SPacket(ResourceLocation key) {
        this.key = key;
    }

    public UntriggerAbilityC2SPacket(FriendlyByteBuf buf) {
        this.key = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.key);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            Ability ability = JJKAbilities.getValue(this.key);
            if (ability == null) return;

            sender.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                // 调用 untrigger
                AbilityHandler.untrigger(sender, ability);

                // ★★★ 关键修复：同步状态到客户端 ★★★
                PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), sender);
            });
        });

        ctx.setPacketHandled(true);
    }
}
