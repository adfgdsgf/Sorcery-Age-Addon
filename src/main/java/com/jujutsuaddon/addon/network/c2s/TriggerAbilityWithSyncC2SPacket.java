package com.jujutsuaddon.addon.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.ability.AbilityHandler;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SetOverlayMessageS2CPacket;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;
import radon.jujutsu_kaisen.network.packet.s2c.TriggerAbilityS2CPacket;

import java.util.function.Supplier;

/**
 * 触发技能并同步数据
 * 基于原版 TriggerAbilityC2SPacket，添加了数据同步
 */
public class TriggerAbilityWithSyncC2SPacket {

    private final ResourceLocation key;

    public TriggerAbilityWithSyncC2SPacket(ResourceLocation key) {
        this.key = key;
    }

    public TriggerAbilityWithSyncC2SPacket(FriendlyByteBuf buf) {
        this.key = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.key);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            Ability ability = JJKAbilities.getValue(this.key);
            if (ability == null) return;

            // ★★★ 使用原版逻辑触发技能 ★★★
            Ability.Status status = AbilityHandler.trigger(sender, ability);

            ISorcererData cap = sender.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
            if (cap == null) return;

            if (status == Ability.Status.SUCCESS) {
                // ★★★ 关键：发送原版的客户端回调包！★★★
                PacketHandler.sendToClient(new TriggerAbilityS2CPacket(this.key), sender);
            } else {
                // ★★★ 处理失败状态消息（与原版一致）★★★
                switch (status) {
                    case ENERGY -> PacketHandler.sendToClient(
                            new SetOverlayMessageS2CPacket(
                                    Component.translatable(String.format("ability.%s.fail.energy", JujutsuKaisen.MOD_ID)),
                                    false), sender);
                    case COOLDOWN -> PacketHandler.sendToClient(
                            new SetOverlayMessageS2CPacket(
                                    Component.translatable(String.format("ability.%s.fail.cooldown", JujutsuKaisen.MOD_ID),
                                            Math.max(1, cap.getRemainingCooldown(ability) / 20)),
                                    false), sender);
                    case BURNOUT -> PacketHandler.sendToClient(
                            new SetOverlayMessageS2CPacket(
                                    Component.translatable(String.format("ability.%s.fail.burnout", JujutsuKaisen.MOD_ID),
                                            Math.max(1, cap.getBurnout() / 20)),
                                    false), sender);
                    case DISABLE -> PacketHandler.sendToClient(
                            new SetOverlayMessageS2CPacket(
                                    Component.translatable(String.format("ability.%s.fail.disable", JujutsuKaisen.MOD_ID)),
                                    false), sender);
                    case FAILURE -> PacketHandler.sendToClient(
                            new SetOverlayMessageS2CPacket(
                                    Component.translatable(String.format("ability.%s.fail.failure", JujutsuKaisen.MOD_ID)),
                                    false), sender);
                    case CHANT -> PacketHandler.sendToClient(
                            new SetOverlayMessageS2CPacket(
                                    Component.translatable(String.format("ability.%s.fail.chant", JujutsuKaisen.MOD_ID)),
                                    false), sender);
                    case THROAT -> PacketHandler.sendToClient(
                            new SetOverlayMessageS2CPacket(
                                    Component.translatable(String.format("ability.%s.fail.throat", JujutsuKaisen.MOD_ID),
                                            Math.max(1, cap.getThroatDamage() / 20)),
                                    false), sender);
                    case EMPTYINV -> PacketHandler.sendToClient(
                            new SetOverlayMessageS2CPacket(
                                    Component.translatable(String.format("ability.%s.fail.emptyinv", JujutsuKaisen.MOD_ID)),
                                    false), sender);
                    // SILENCED, DISARMED, UNUSABLE, SUCCESS 不显示消息
                    default -> {}
                }
            }

            // ★★★ 附加：同步数据到客户端（这是我们添加的部分）★★★
            PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), sender);
        });
        ctx.get().setPacketHandled(true);
    }
}
