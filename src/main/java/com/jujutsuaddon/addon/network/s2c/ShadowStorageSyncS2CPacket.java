package com.jujutsuaddon.addon.network.s2c;

import com.jujutsuaddon.addon.capability.curse.AddonShadowStorageData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShadowStorageSyncS2CPacket {

    private final CompoundTag data;

    public ShadowStorageSyncS2CPacket(CompoundTag data) {
        this.data = data;
    }

    public ShadowStorageSyncS2CPacket(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 必须在客户端执行
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient());
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleClient() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getCapability(AddonShadowStorageData.CAPABILITY)
                    .ifPresent(storage -> storage.load(data));
        }
    }
}
