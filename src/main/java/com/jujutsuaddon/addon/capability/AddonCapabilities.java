package com.jujutsuaddon.addon.capability;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.ShadowStorageSyncS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Capability 注册和事件处理
 * 使用 @EventBusSubscriber 自动注册，不需要在主类手动添加
 */
public class AddonCapabilities {

    public static final ResourceLocation SHADOW_STORAGE_KEY =
            new ResourceLocation(JujutsuAddon.MODID, "shadow_storage");

    // ==================== FORGE 总线事件 ====================
    @Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

        /**
         * 附加 Capability 到玩家实体
         */
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player player) {
                if (!player.getCapability(AddonShadowStorageData.CAPABILITY).isPresent()) {
                    event.addCapability(SHADOW_STORAGE_KEY, new AddonShadowStorageData.Provider());
                }
            }
        }

        /**
         * 玩家死亡/跨维度时保留数据
         */
        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            if (event.isWasDeath()) {
                event.getOriginal().reviveCaps();
            }

            event.getOriginal().getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(oldData -> {
                event.getEntity().getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(newData -> {
                    newData.load(oldData.save());
                });
            });

            if (event.isWasDeath()) {
                event.getOriginal().invalidateCaps();
            }
        }

        /**
         * 玩家登录时同步数据到客户端
         */
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(data -> {
                    AddonNetwork.sendToPlayer(new ShadowStorageSyncS2CPacket(data.save()), serverPlayer);
                });
            }
        }

        /**
         * 玩家重生时同步数据
         */
        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(data -> {
                    AddonNetwork.sendToPlayer(new ShadowStorageSyncS2CPacket(data.save()), serverPlayer);
                });
            }
        }

        /**
         * 玩家切换维度时同步数据
         */
        @SubscribeEvent
        public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(data -> {
                    AddonNetwork.sendToPlayer(new ShadowStorageSyncS2CPacket(data.save()), serverPlayer);
                });
            }
        }
    }

    // ==================== MOD 总线事件 ====================
    @Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        /**
         * 注册 Capability 类型
         */
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(AddonShadowStorageData.class);
        }
    }
}
