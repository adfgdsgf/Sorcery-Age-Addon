package com.jujutsuaddon.addon.network;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.network.c2s.*;
import com.jujutsuaddon.addon.network.c2s.ActivateVowC2SPacket;
import com.jujutsuaddon.addon.network.c2s.DissolveVowC2SPacket;
import com.jujutsuaddon.addon.network.s2c.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络通信管理器
 * Network Communication Manager
 *
 * 负责注册和发送所有网络包
 */
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
        // ==================== 原有网络包 ====================

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

        // 影子库存操作包 (C2S)
        CHANNEL.registerMessage(packetId++,
                ShadowStorageActionC2SPacket.class,
                ShadowStorageActionC2SPacket::encode,
                ShadowStorageActionC2SPacket::new,
                ShadowStorageActionC2SPacket::handle
        );

        // 影子库存同步包 (S2C)
        CHANNEL.registerMessage(packetId++,
                ShadowStorageSyncS2CPacket.class,
                ShadowStorageSyncS2CPacket::encode,
                ShadowStorageSyncS2CPacket::new,
                ShadowStorageSyncS2CPacket::handle
        );

        CHANNEL.registerMessage(packetId++,
                ReflectProjectilesC2SPacket.class,
                ReflectProjectilesC2SPacket::encode,
                ReflectProjectilesC2SPacket::new,
                ReflectProjectilesC2SPacket::handle
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

        // 伤害预测同步包 (S2C)
        CHANNEL.messageBuilder(SyncDamagePredictionsS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDamagePredictionsS2CPacket::encode)
                .decoder(SyncDamagePredictionsS2CPacket::decode)
                .consumerMainThread(SyncDamagePredictionsS2CPacket::handle)
                .add();

        // 无下限压制等级同步包 (C2S)
        CHANNEL.registerMessage(packetId++,
                SyncInfinityPressureC2SPacket.class,
                SyncInfinityPressureC2SPacket::encode,
                SyncInfinityPressureC2SPacket::new,
                SyncInfinityPressureC2SPacket::handle
        );

        // 无下限场同步包 (S2C)
        CHANNEL.messageBuilder(InfinityFieldSyncS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(InfinityFieldSyncS2CPacket::encode)
                .decoder(InfinityFieldSyncS2CPacket::decode)
                .consumerMainThread(InfinityFieldSyncS2CPacket::handle)
                .add();

        // 无下限压力等级同步包 (S2C)
        CHANNEL.messageBuilder(SyncInfinityPressureS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncInfinityPressureS2CPacket::encode)
                .decoder(SyncInfinityPressureS2CPacket::decode)
                .consumerMainThread(SyncInfinityPressureS2CPacket::handle)
                .add();

        // 投射物平滑移动包 (S2C)
        CHANNEL.messageBuilder(ProjectileSmoothMoveS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProjectileSmoothMoveS2CPacket::encode)
                .decoder(ProjectileSmoothMoveS2CPacket::decode)
                .consumerMainThread(ProjectileSmoothMoveS2CPacket::handle)
                .add();

        // 火焰弹 Power 同步包 (S2C)
        CHANNEL.messageBuilder(ProjectilePowerSyncS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProjectilePowerSyncS2CPacket::encode)
                .decoder(ProjectilePowerSyncS2CPacket::decode)
                .consumerMainThread(ProjectilePowerSyncS2CPacket::handle)
                .add();

        // ==================== 誓约系统网络包 ====================
// 创建誓约 (C2S) - 使用 CreateVowC2SPacket
        CHANNEL.messageBuilder(CreateVowC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CreateVowC2SPacket::encode)
                .decoder(CreateVowC2SPacket::new)
                .consumerMainThread(CreateVowC2SPacket::handle)
                .add();
// 请求誓约列表 (C2S)
        CHANNEL.messageBuilder(RequestVowListC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestVowListC2SPacket::encode)
                .decoder(RequestVowListC2SPacket::new)
                .consumerMainThread(RequestVowListC2SPacket::handle)
                .add();
// 激活誓约 (C2S)
        CHANNEL.messageBuilder(ActivateVowC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateVowC2SPacket::toBytes)
                .decoder(ActivateVowC2SPacket::new)
                .consumerMainThread(ActivateVowC2SPacket::handle)
                .add();
// 解除誓约 (C2S)
        CHANNEL.messageBuilder(DissolveVowC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DissolveVowC2SPacket::toBytes)
                .decoder(DissolveVowC2SPacket::new)
                .consumerMainThread(DissolveVowC2SPacket::handle)
                .add();

        // 删除誓约 (C2S)
        CHANNEL.messageBuilder(DeleteVowC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteVowC2SPacket::toBytes)
                .decoder(DeleteVowC2SPacket::new)
                .consumerMainThread(DeleteVowC2SPacket::handle)
                .add();
// 同步誓约列表 (S2C)
        CHANNEL.messageBuilder(SyncVowListS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncVowListS2CPacket::encode)
                .decoder(SyncVowListS2CPacket::new)
                .consumerMainThread(SyncVowListS2CPacket::handle)
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
     * 发送到追踪指定实体的所有玩家
     */
    public static void sendToTrackingEntity(Object packet, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    /**
     * 服务端 -> 所有玩家
     */
    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
