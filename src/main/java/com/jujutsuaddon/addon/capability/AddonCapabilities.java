package com.jujutsuaddon.addon.capability;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.network.AddonNetwork;
import com.jujutsuaddon.addon.network.s2c.CurseBaselineSyncS2CPacket;
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
 */
public class AddonCapabilities {

    // ==================== Capability Keys ====================

    public static final ResourceLocation SHADOW_STORAGE_KEY =
            new ResourceLocation(JujutsuAddon.MODID, "shadow_storage");

    public static final ResourceLocation CURSE_BASELINE_KEY =
            new ResourceLocation(JujutsuAddon.MODID, "curse_baseline");

    // ==================== FORGE 总线事件 ====================

    @Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player player) {
                // Shadow Storage
                if (!player.getCapability(AddonShadowStorageData.CAPABILITY).isPresent()) {
                    event.addCapability(SHADOW_STORAGE_KEY, new AddonShadowStorageData.Provider());
                }
                // Curse Baseline
                if (!player.getCapability(AddonCurseBaselineData.CAPABILITY).isPresent()) {
                    event.addCapability(CURSE_BASELINE_KEY, new AddonCurseBaselineData.Provider());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            if (event.isWasDeath()) {
                event.getOriginal().reviveCaps();
            }

            // Shadow Storage
            event.getOriginal().getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(oldData -> {
                event.getEntity().getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(newData -> {
                    newData.load(oldData.save());
                });
            });

            // Curse Baseline
            event.getOriginal().getCapability(AddonCurseBaselineData.CAPABILITY).ifPresent(oldData -> {
                event.getEntity().getCapability(AddonCurseBaselineData.CAPABILITY).ifPresent(newData -> {
                    newData.load(oldData.save());
                });
            });

            if (event.isWasDeath()) {
                event.getOriginal().invalidateCaps();
            }
        }

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                syncAllData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                syncAllData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                syncAllData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            // 清理监听器缓存
            CurseListMonitor.onPlayerLogout(event.getEntity().getUUID());
        }

        /**
         * 同步所有自定义数据到客户端
         */
        private static void syncAllData(ServerPlayer player) {
            // Shadow Storage
            player.getCapability(AddonShadowStorageData.CAPABILITY).ifPresent(data -> {
                AddonNetwork.sendToPlayer(new ShadowStorageSyncS2CPacket(data.save()), player);
            });

            // Curse Baseline
            player.getCapability(AddonCurseBaselineData.CAPABILITY).ifPresent(data -> {
                AddonNetwork.sendToPlayer(new CurseBaselineSyncS2CPacket(data.save()), player);
            });
        }
    }

    // ==================== MOD 总线事件 ====================

    @Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(AddonShadowStorageData.class);
            event.register(AddonCurseBaselineData.class);
        }
    }
}
