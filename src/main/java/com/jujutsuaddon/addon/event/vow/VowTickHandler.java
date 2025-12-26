package com.jujutsuaddon.addon.event.vow;

import com.jujutsuaddon.addon.vow.manager.VowManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 誓约Tick检测处理器
 * 用于检测时间类条件等需要持续监控的条件
 */
@Mod.EventBusSubscriber(modid = "jujutsu_addon")
public class VowTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;

        Player player = event.player;

        // 每秒检测一次（20 ticks = 1秒）
        if (player.tickCount % 20 == 0) {
            VowManager.checkOnTick(player);
        }
    }
}
