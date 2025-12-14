package com.jujutsuaddon.addon.network.c2s;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsMode;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SwitchTenShadowsModeC2SPacket {
    @Nullable
    private final TenShadowsMode mode;

    public SwitchTenShadowsModeC2SPacket(@Nullable TenShadowsMode mode) {
        this.mode = mode;
    }

    public SwitchTenShadowsModeC2SPacket(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            this.mode = TenShadowsMode.values()[buf.readInt()];
        } else {
            this.mode = null;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.mode != null);
        if (this.mode != null) {
            buf.writeInt(this.mode.ordinal());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || this.mode == null) return;

            // ===== 服务端配置检查 =====
            // 只有当配置启用时，才允许自动切换模式
            if (!AddonConfig.COMMON.enableTenShadowsModeBypass.get()) {
                return; // 配置未启用，忽略此包
            }
            // ==========================

            ITenShadowsData data = player.getCapability(TenShadowsDataHandler.INSTANCE).resolve().orElse(null);
            if (data == null) return;

            data.setMode(this.mode);
        });
        ctx.get().setPacketHandled(true);
    }
}
