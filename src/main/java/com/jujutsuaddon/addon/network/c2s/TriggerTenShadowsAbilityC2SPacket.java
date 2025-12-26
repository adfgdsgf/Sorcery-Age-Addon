package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.vow.manager.VowManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.ability.AbilityHandler;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsMode;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SetOverlayMessageS2CPacket;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;
import radon.jujutsu_kaisen.network.packet.s2c.SyncTenShadowsDataS2CPacket;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class TriggerTenShadowsAbilityC2SPacket {
    private final ResourceLocation abilityKey;
    @Nullable
    private final TenShadowsMode requiredMode;

    public TriggerTenShadowsAbilityC2SPacket(ResourceLocation abilityKey, @Nullable TenShadowsMode requiredMode) {
        this.abilityKey = abilityKey;
        this.requiredMode = requiredMode;
    }

    public TriggerTenShadowsAbilityC2SPacket(FriendlyByteBuf buf) {
        this.abilityKey = buf.readResourceLocation();
        if (buf.readBoolean()) {
            this.requiredMode = TenShadowsMode.values()[buf.readInt()];
        } else {
            this.requiredMode = null;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.abilityKey);
        buf.writeBoolean(this.requiredMode != null);
        if (this.requiredMode != null) {
            buf.writeInt(this.requiredMode.ordinal());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Ability ability = JJKAbilities.getValue(this.abilityKey);
            if (ability == null) return;

            // ★★★ 誓约系统：检查技能封印 ★★★
            if (VowManager.isAbilityBanned(player, ability)) {
                // 如果被封印，发送提示消息给玩家
                PacketHandler.sendToClient(
                        new SetOverlayMessageS2CPacket(
                                Component.translatable("message.jujutsu_addon.ability_banned_by_vow"),
                                false),
                        player
                );
                return;
            }

            ITenShadowsData tenData = player.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);

            if (AddonConfig.COMMON.enableTenShadowsModeBypass.get() && this.requiredMode != null && tenData != null) {
                if (tenData.getMode() != this.requiredMode) {
                    tenData.setMode(this.requiredMode);
                }
            }

            AbilityHandler.trigger(player, ability);

            if (tenData != null) {
                PacketHandler.sendToClient(new SyncTenShadowsDataS2CPacket(tenData.serializeNBT()), player);
            }

            player.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
