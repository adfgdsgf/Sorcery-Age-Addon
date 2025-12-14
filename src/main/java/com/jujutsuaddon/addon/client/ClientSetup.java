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
            AddonKeyBindings.init();
            SkillBarManager.init();

            // ★★★ 不再需要 MenuScreens.register ★★★
            // 新的 ShadowStorageScreen 是普通 Screen，直接在客户端打开
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        if (AddonKeyBindings.SKILL_SLOT_KEYS.isEmpty()) {
            AddonKeyBindings.init();
        }

        for (KeyMapping key : AddonKeyBindings.getAllKeys()) {
            event.register(key);
        }
    }
}
