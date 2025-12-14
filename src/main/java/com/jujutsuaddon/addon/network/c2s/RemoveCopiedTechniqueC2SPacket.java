package com.jujutsuaddon.addon.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;

import java.util.function.Supplier;

public class RemoveCopiedTechniqueC2SPacket {

    private final String techniqueName;

    public RemoveCopiedTechniqueC2SPacket(CursedTechnique technique) {
        this.techniqueName = technique.name();
    }

    public RemoveCopiedTechniqueC2SPacket(FriendlyByteBuf buf) {
        this.techniqueName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.techniqueName);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            sender.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                // 找到对应的术式
                CursedTechnique technique = null;
                try {
                    technique = CursedTechnique.valueOf(this.techniqueName);
                } catch (IllegalArgumentException ignored) {
                    return;
                }

                // 检查玩家是否确实拥有这个复制的术式
                if (cap.getCopied().contains(technique)) {
                    // 调用 uncopy 方法删除
                    cap.uncopy(technique);

                    // 同步到客户端
                    PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), sender);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
