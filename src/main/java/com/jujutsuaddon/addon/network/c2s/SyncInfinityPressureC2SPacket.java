// src/main/java/com/jujutsuaddon/addon/network/c2s/SyncInfinityPressureC2SPacket.java
package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.api.IInfinityPressureAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.function.Supplier;

public class SyncInfinityPressureC2SPacket {

    private final boolean increase;

    public SyncInfinityPressureC2SPacket(boolean increase) {
        this.increase = increase;
    }

    public SyncInfinityPressureC2SPacket(FriendlyByteBuf buf) {
        this.increase = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.increase);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 检查是否开启了 Infinity
            if (!JJKAbilities.hasToggled(player, JJKAbilities.INFINITY.get())) {
                return;
            }

            player.getCapability(SorcererDataHandler.INSTANCE).ifPresent(data -> {
                if (data instanceof IInfinityPressureAccessor accessor) {
                    if (this.increase) {
                        accessor.jujutsuAddon$increaseInfinityPressure();
                    } else {
                        accessor.jujutsuAddon$decreaseInfinityPressure();
                    }

                    int level = accessor.jujutsuAddon$getInfinityPressure();
                    // 发送 ActionBar 消息
                    player.displayClientMessage(
                            Component.translatable("message.jujutsu_addon.infinity_pressure", level),
                            true
                    );
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
