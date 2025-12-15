package com.jujutsuaddon.addon.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.AbsorbedCurse;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 召唤已吸收咒灵的网络包
 * 与原版保持一致，消耗咒力
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

            boolean success;
            if (curseIndex == -1) {
                // 召唤全部
                success = summonAllCurses(player, data, curses);
            } else if (curseIndex >= 0 && curseIndex < curses.size()) {
                // 召唤指定索引
                success = summonSingleCurse(player, data, curses.get(curseIndex));
            } else {
                success = false;
            }

            // 同步数据到客户端
            if (success) {
                PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(data.serializeNBT()), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 召唤单个咒灵（消耗咒力）
     */
    private boolean summonSingleCurse(ServerPlayer player, ISorcererData data, AbsorbedCurse curse) {
        float cost = JJKAbilities.getCurseCost(curse);

        // 检查咒力是否足够（创造模式跳过）
        if (!player.isCreative() && data.getEnergy() < cost) {
            player.displayClientMessage(
                    Component.translatable("message.jujutsu_addon.not_enough_energy"), true);
            return false;
        }

        // 调用原版方法召唤咒灵
        if (JJKAbilities.summonCurse(player, curse, false) != null) {
            // 召唤成功，消耗咒力
            if (!player.isCreative()) {
                data.useEnergy(cost);
            }
            return true;
        }
        return false;
    }

    /**
     * 召唤全部咒灵（消耗咒力）
     */
    private boolean summonAllCurses(ServerPlayer player, ISorcererData data, List<AbsorbedCurse> curses) {
        // 计算总消耗
        float totalCost = 0.0F;
        for (AbsorbedCurse curse : curses) {
            totalCost += JJKAbilities.getCurseCost(curse);
        }

        // 检查咒力是否足够（创造模式跳过）
        if (!player.isCreative() && data.getEnergy() < totalCost) {
            player.displayClientMessage(
                    Component.translatable("message.jujutsu_addon.not_enough_energy"), true);
            return false;
        }

        // 复制列表避免并发修改
        List<AbsorbedCurse> cursesToSummon = new ArrayList<>(curses);
        float actualCost = 0.0F;

        for (AbsorbedCurse curse : cursesToSummon) {
            float cost = JJKAbilities.getCurseCost(curse);
            if (JJKAbilities.summonCurse(player, curse, false) != null) {
                actualCost += cost;
            }
        }

        // 消耗实际使用的咒力
        if (!player.isCreative() && actualCost > 0) {
            data.useEnergy(actualCost);
        }

        return actualCost > 0;
    }
}
