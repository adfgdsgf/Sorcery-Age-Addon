package com.jujutsuaddon.addon.client;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.client.gui.overlay.VowHudOverlay; // ★ 导入 HUD 类
import com.jujutsuaddon.addon.client.keybind.AddonKeyBindings;
import com.jujutsuaddon.addon.client.skillbar.SkillBarManager;
import com.jujutsuaddon.addon.client.util.FeatureToggleManager;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent; // ★ 导入 HUD 注册事件
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SkillBarManager.init();
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // 功能开关检查
        if (!FeatureToggleManager.isKeybindSystemEnabled()) {
            return;
        }
        AddonKeyBindings.init();

        for (KeyMapping key : AddonKeyBindings.getAllKeys()) {
            event.register(key);
        }
    }

    // ★★★ 新增：注册 HUD Overlay ★★★
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        // 注册誓约惩罚 HUD，显示在最上层
        event.registerAboveAll("vow_penalty_hud", VowHudOverlay.INSTANCE);
    }
}
