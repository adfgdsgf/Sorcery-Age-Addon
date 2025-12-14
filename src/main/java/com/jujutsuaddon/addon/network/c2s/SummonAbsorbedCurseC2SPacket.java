package com.jujutsuaddon.addon.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.AbsorbedCurse;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.List;
import java.util.function.Supplier;

/**
 * 召唤已吸收咒灵的网络包
 */
public class SummonAbsorbedCurseC2SPacket {

    private final int curseIndex; // -1 = 全部召唤

    public SummonAbsorbedCurseC2SPacket(int curseIndex) {
        this.curseIndex = curseIndex;
    }

    public SummonAbsorbedCurseC2SPacket(FriendlyByteBuf buf) {
        this.curseIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(curseIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
            if (data == null) return;

            List<AbsorbedCurse> curses = data.getCurses();
            if (curses.isEmpty()) return;

            if (curseIndex == -1) {
                // 召唤全部
                for (AbsorbedCurse curse : curses) {
                    JJKAbilities.summonCurse(player, curse, false);
                }
            } else if (curseIndex >= 0 && curseIndex < curses.size()) {
                // 召唤指定索引
                AbsorbedCurse curse = curses.get(curseIndex);
                JJKAbilities.summonCurse(player, curse, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
