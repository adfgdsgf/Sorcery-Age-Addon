package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.network.FriendlyByteBuf;
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

            // 如果配置启用且需要切换模式，先切换
            if (AddonConfig.COMMON.enableTenShadowsModeBypass.get() && this.requiredMode != null) {
                ITenShadowsData tenData = player.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
                if (tenData != null && tenData.getMode() != this.requiredMode) {
                    tenData.setMode(this.requiredMode);
                    // ★★★ 缺失的代码：同步十影数据到客户端 ★★★
                    PacketHandler.sendToClient(new SyncTenShadowsDataS2CPacket(tenData.serializeNBT()), player);
                }
            }

            // 触发技能
            AbilityHandler.trigger(player, ability);

            // ★★★ 关键修复：同步状态到客户端 ★★★
            player.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
