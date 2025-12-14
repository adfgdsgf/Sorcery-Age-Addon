package com.jujutsuaddon.addon.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.function.Supplier;

public class StopChannelingC2SPacket {

    private final ResourceLocation key;

    public StopChannelingC2SPacket(ResourceLocation key) {
        this.key = key;
    }

    public StopChannelingC2SPacket(FriendlyByteBuf buf) {
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

            sender.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                Ability channeled = cap.getChanneled();
                if (channeled == null) return;

                ResourceLocation channeledKey = JJKAbilities.getKey(channeled);
                if (channeledKey != null && channeledKey.equals(this.key)) {
                    // 用相同的 ability 调用 channel() 会切换状态（设为 null）
                    cap.channel(channeled);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
