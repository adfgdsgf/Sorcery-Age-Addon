package com.jujutsuaddon.addon.client;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.client.skillbar.SkillBarManager;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // ★★★ 不需要在这里调用 init()，RegisterKeyMappingsEvent 会处理 ★★★
            SkillBarManager.init();
        });
    }
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // ★★★ init() 内部有防重复检查，可以安全调用 ★★★
        AddonKeyBindings.init();

        for (KeyMapping key : AddonKeyBindings.getAllKeys()) {
            event.register(key);
        }
    }
}