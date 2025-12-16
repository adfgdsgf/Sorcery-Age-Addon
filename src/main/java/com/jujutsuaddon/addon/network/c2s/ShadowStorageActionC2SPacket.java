package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.capability.AddonShadowStorageData;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.ShadowStorageSyncS2CPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShadowStorageActionC2SPacket {

    public enum Action {
        STORE_ONE,
        STORE_HALF,
        STORE_ALL,
        RETRIEVE_ONE,
        RETRIEVE_HALF,
        RETRIEVE_ALL,
        RETRIEVE_STACK,
        SORT
    }

    private final Action action;
    private final int shadowIndex;
    private final int playerSlot;

    public ShadowStorageActionC2SPacket(Action action, int shadowIndex, int playerSlot) {
        this.action = action;
        this.shadowIndex = shadowIndex;
        this.playerSlot = playerSlot;
    }

    public ShadowStorageActionC2SPacket(FriendlyByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
        this.shadowIndex = buf.readInt();
        this.playerSlot = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(action.ordinal());
        buf.writeInt(shadowIndex);
        buf.writeInt(playerSlot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(storage -> {
                // 业务逻辑下沉到 Capability 中
                storage.handleAction(player, action.ordinal(), shadowIndex, playerSlot);

                // 操作完成后同步到客户端
                AddonNetwork.sendToPlayer(new ShadowStorageSyncS2CPacket(storage.save()), player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
