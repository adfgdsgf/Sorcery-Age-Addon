package com.jujutsuaddon.addon.network.s2c;

import com.jujutsuaddon.addon.capability.curse.AddonCurseBaselineData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 咒灵基准数据同步包 (Server -> Client)
 */
public class CurseBaselineSyncS2CPacket {

    private final CompoundTag data;

    public CurseBaselineSyncS2CPacket(CompoundTag data) {
        this.data = data;
    }

    // ==================== 序列化 ====================

    public static void encode(CurseBaselineSyncS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static CurseBaselineSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new CurseBaselineSyncS2CPacket(buf.readNbt());
    }

    // ==================== 处理 ====================

    public static void handle(CurseBaselineSyncS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleClient(CurseBaselineSyncS2CPacket msg) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getCapability(AddonCurseBaselineData.CAPABILITY)
                    .ifPresent(data -> data.load(msg.data));
        }
    }
}
