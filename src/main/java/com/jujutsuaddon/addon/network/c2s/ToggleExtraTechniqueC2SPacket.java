package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;

import java.util.function.Supplier;

/**
 * 统一处理复制/偷取术式的激活切换
 */
public class ToggleExtraTechniqueC2SPacket {

    private final String techniqueName;
    private final int sourceType;  // 0=COPIED, 1=STOLEN
    private final boolean deactivate;

    public ToggleExtraTechniqueC2SPacket(CursedTechnique technique,
                                         TechniqueHelper.TechniqueSource source,
                                         boolean deactivate) {
        this.techniqueName = technique.name();
        this.sourceType = source == TechniqueHelper.TechniqueSource.STOLEN ? 1 : 0;
        this.deactivate = deactivate;
    }

    public ToggleExtraTechniqueC2SPacket(FriendlyByteBuf buf) {
        this.techniqueName = buf.readUtf();
        this.sourceType = buf.readInt();
        this.deactivate = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.techniqueName);
        buf.writeInt(this.sourceType);
        buf.writeBoolean(this.deactivate);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            sender.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                CursedTechnique technique;
                try {
                    technique = CursedTechnique.valueOf(this.techniqueName);
                } catch (IllegalArgumentException e) {
                    return;
                }

                if (sourceType == 1) {
                    // ★★★ 偷取的术式 ★★★
                    if (cap.getStolen() == null || !cap.getStolen().contains(technique)) {
                        return;
                    }
                    if (deactivate) {
                        cap.setCurrentStolen(null);
                    } else {
                        cap.setCurrentStolen(technique);
                    }
                } else {
                    // ★★★ 复制的术式 ★★★
                    if (cap.getCopied() == null || !cap.getCopied().contains(technique)) {
                        return;
                    }
                    if (deactivate) {
                        cap.setCurrentCopied(null);
                    } else {
                        cap.setCurrentCopied(technique);
                    }
                }

                // 同步到客户端
                PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), sender);
            });
        });

        ctx.setPacketHandled(true);
    }
}
