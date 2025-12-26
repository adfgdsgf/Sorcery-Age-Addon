package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.ViolationRecord;
import com.jujutsuaddon.addon.vow.manager.VowManager;
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

            // ============================================================
            // ★★★ 誓约判定逻辑 ★★★
            // ============================================================

            boolean shouldSkipBenefits = false; // 标记：本次释放是否应该跳过收益

            ViolationRecord violation = VowManager.getBanViolation(sender, ability);

            if (violation != null) {
                CustomBindingVow vow = VowManager.getVow(sender, violation.getVowId());
                if (vow != null) {

                    // 情况 A: 永久束缚 -> 拦截
                    if (vow.isPermanent()) {
                        PacketHandler.sendToClient(
                                new SetOverlayMessageS2CPacket(
                                        Component.translatable("message.jujutsu_addon.ability_banned_by_vow"),
                                        false),
                                sender
                        );
                        return;
                    }

                    // 情况 B: 普通束缚 -> 惩罚 + 标记跳过收益
                    else {
                        if (!sender.isCreative()) {
                            VowManager.handleViolation(sender, vow);
                            // ★ 关键：标记为跳过收益
                            shouldSkipBenefits = true;
                        }
                    }
                }
            }

            // ============================================================
            // ★★★ 技能触发逻辑 ★★★
            // ============================================================

            // 1. 如果需要跳过收益，设置全局状态
            if (shouldSkipBenefits) {
                VowManager.setSkippingBenefits(true);
            }

            try {
                // 2. 触发技能 (此时 VowManager.isSkippingBenefits() 为 true)
                // 技能内部计算冷却/消耗时，应该读取这个状态并返回原始值
                Ability.Status status = AbilityHandler.trigger(sender, ability);

                ISorcererData cap = sender.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
                if (cap == null) return;

                if (status == Ability.Status.SUCCESS) {
                    PacketHandler.sendToClient(new TriggerAbilityS2CPacket(this.key), sender);
                } else {
                    // 错误处理 (保持原样)
                    handleFailure(sender, status, cap);
                }

                PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), sender);

            } finally {
                // 3. ★★★ 无论技能释放成功还是报错，必须把状态还原！★★★
                // 否则玩家以后放技能都没收益了
                if (shouldSkipBenefits) {
                    VowManager.setSkippingBenefits(false);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // 提取出来的错误处理方法，让主逻辑更清晰
    private void handleFailure(ServerPlayer sender, Ability.Status status, ISorcererData cap) {
        switch (status) {
            case ENERGY -> PacketHandler.sendToClient(
                    new SetOverlayMessageS2CPacket(
                            Component.translatable(String.format("ability.%s.fail.energy", JujutsuKaisen.MOD_ID)),
                            false), sender);
            case COOLDOWN -> PacketHandler.sendToClient(
                    new SetOverlayMessageS2CPacket(
                            Component.translatable(String.format("ability.%s.fail.cooldown", JujutsuKaisen.MOD_ID),
                                    Math.max(1, cap.getRemainingCooldown(JJKAbilities.getValue(this.key)) / 20)),
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
            default -> {}
        }
    }
}
