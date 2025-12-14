package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.inventory.ShadowStorageMenuProvider;
import com.jujutsuaddon.addon.util.helper.TechniqueAccessHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenShadowStorageC2SPacket {

    public OpenShadowStorageC2SPacket() {}

    public OpenShadowStorageC2SPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 检查权限
            if (!TechniqueAccessHelper.canUseTenShadows(player)) {
                return;
            }

            // 打开容器界面
            ShadowStorageMenuProvider.open(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
