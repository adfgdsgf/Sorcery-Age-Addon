package com.jujutsuaddon.addon.network.s2c;

import com.jujutsuaddon.addon.client.InfinityFieldClientCache;
import com.jujutsuaddon.addon.network.InfinityFieldData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步无下限场数据
 */
public class InfinityFieldSyncS2CPacket {

    private final List<InfinityFieldData> fields;
    private final long serverTick;

    public InfinityFieldSyncS2CPacket(List<InfinityFieldData> fields, long serverTick) {
        this.fields = fields;
        this.serverTick = serverTick;
    }

    public InfinityFieldSyncS2CPacket(FriendlyByteBuf buf) {
        this.serverTick = buf.readLong();
        int count = buf.readVarInt();
        this.fields = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            fields.add(InfinityFieldData.read(buf));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(serverTick);
        buf.writeVarInt(fields.size());
        for (InfinityFieldData field : fields) {
            field.write(buf);
        }
    }

    public static InfinityFieldSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new InfinityFieldSyncS2CPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                InfinityFieldClientCache.updateFields(fields, serverTick);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
