// src/main/java/com/jujutsuaddon/addon/network/s2c/SyncInfinityPressureS2CPacket.java
package com.jujutsuaddon.addon.network.s2c;

import com.jujutsuaddon.addon.api.ability.IInfinityPressureAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.function.Supplier;

public class SyncInfinityPressureS2CPacket {

    private final int pressureLevel;

    public SyncInfinityPressureS2CPacket(int pressureLevel) {
        this.pressureLevel = pressureLevel;
    }

    public SyncInfinityPressureS2CPacket(FriendlyByteBuf buf) {
        this.pressureLevel = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.pressureLevel);
    }

    public static SyncInfinityPressureS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncInfinityPressureS2CPacket(buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient);
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleClient() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        player.getCapability(SorcererDataHandler.INSTANCE).ifPresent(data -> {
            if (data instanceof IInfinityPressureAccessor accessor) {
                accessor.jujutsuAddon$setInfinityPressure(this.pressureLevel);
            }
        });
    }
}
