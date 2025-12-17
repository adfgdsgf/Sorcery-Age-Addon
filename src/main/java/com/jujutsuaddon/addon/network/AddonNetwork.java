package com.jujutsuaddon.addon.network;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.network.c2s.*;
import com.jujutsuaddon.addon.network.s2c.CurseBaselineSyncS2CPacket;
import com.jujutsuaddon.addon.network.s2c.ShadowStorageSyncS2CPacket;
import com.jujutsuaddon.addon.network.s2c.SyncDamagePredictionsS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class AddonNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JujutsuAddon.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );



    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                StopChannelingC2SPacket.class,
                StopChannelingC2SPacket::encode,
                StopChannelingC2SPacket::new,
                StopChannelingC2SPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                RemoveCopiedTechniqueC2SPacket.class,
                RemoveCopiedTechniqueC2SPacket::encode,
                RemoveCopiedTechniqueC2SPacket::new,
                RemoveCopiedTechniqueC2SPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                UntriggerAbilityC2SPacket.class,
                UntriggerAbilityC2SPacket::encode,
                UntriggerAbilityC2SPacket::new,
                UntriggerAbilityC2SPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                SwitchTenShadowsModeC2SPacket.class,
                SwitchTenShadowsModeC2SPacket::encode,
                SwitchTenShadowsModeC2SPacket::new,
                SwitchTenShadowsModeC2SPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                TriggerTenShadowsAbilityC2SPacket.class,
                TriggerTenShadowsAbilityC2SPacket::encode,
                TriggerTenShadowsAbilityC2SPacket::new,
                TriggerTenShadowsAbilityC2SPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                ToggleExtraTechniqueC2SPacket.class,
                ToggleExtraTechniqueC2SPacket::encode,
                ToggleExtraTechniqueC2SPacket::new,
                ToggleExtraTechniqueC2SPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                TriggerAbilityWithSyncC2SPacket.class,
                TriggerAbilityWithSyncC2SPacket::encode,
                TriggerAbilityWithSyncC2SPacket::new,
                TriggerAbilityWithSyncC2SPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                SummonAbsorbedCurseC2SPacket.class,
                SummonAbsorbedCurseC2SPacket::encode,
                SummonAbsorbedCurseC2SPacket::new,
                SummonAbsorbedCurseC2SPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                StoreShadowItemC2SPacket.class,
                StoreShadowItemC2SPacket::encode,
                StoreShadowItemC2SPacket::new,
                StoreShadowItemC2SPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                RetrieveShadowItemC2SPacket.class,
                RetrieveShadowItemC2SPacket::encode,
                RetrieveShadowItemC2SPacket::new,
                RetrieveShadowItemC2SPacket::handle
        );

        // ★★★ 影子库存操作包 (C2S) ★★★
        CHANNEL.registerMessage(packetId++,
                ShadowStorageActionC2SPacket.class,
                ShadowStorageActionC2SPacket::encode,
                ShadowStorageActionC2SPacket::new,
                ShadowStorageActionC2SPacket::handle
        );

        // ★★★ 影子库存同步包 (S2C) ★★★
        CHANNEL.registerMessage(packetId++,
                ShadowStorageSyncS2CPacket.class,
                ShadowStorageSyncS2CPacket::encode,
                ShadowStorageSyncS2CPacket::new,
                ShadowStorageSyncS2CPacket::handle
        );

        CHANNEL.messageBuilder(OpenShadowStorageC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenShadowStorageC2SPacket::encode)
                .decoder(OpenShadowStorageC2SPacket::new)
                .consumerMainThread(OpenShadowStorageC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(CurseBaselineSyncS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CurseBaselineSyncS2CPacket::encode)
                .decoder(CurseBaselineSyncS2CPacket::decode)
                .consumerMainThread(CurseBaselineSyncS2CPacket::handle)
                .add();

        // ★★★ 伤害预测同步包 (S2C) ★★★
        CHANNEL.messageBuilder(SyncDamagePredictionsS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDamagePredictionsS2CPacket::encode)
                .decoder(SyncDamagePredictionsS2CPacket::decode)
                .consumerMainThread(SyncDamagePredictionsS2CPacket::handle)
                .add();
    }

    // ==================== 发送方法 ====================

    /**
     * 客户端 -> 服务端
     */
    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    /**
     * 服务端 -> 指定玩家
     */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * 服务端 -> 所有玩家
     */
    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
