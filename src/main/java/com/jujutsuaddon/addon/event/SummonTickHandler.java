package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.summon.ai.SummonAIManager;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

@Mod.EventBusSubscriber(modid = "jujutsu_addon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SummonTickHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof SummonEntity summon) {
            SummonAIManager.getInstance().tickSummonAI(summon);
        }
    }
}
