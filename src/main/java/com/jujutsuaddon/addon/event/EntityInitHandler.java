package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.JujutsuAddon;
import com.jujutsuaddon.addon.util.context.AbilityContext;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ability.base.Ability;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class EntityInitHandler {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        Ability ability = AbilityContext.get();

        if (ability != null) {
            // 【修改】使用 getName() 获取全类名 (例如 radon.jujutsu_kaisen.ability.sukuna.Dismantle)
            // 【修改】使用新的键名 "jjk_addon_source_class"
            entity.getPersistentData().putString("jjk_addon_source_class", ability.getClass().getName());
        }
    }
}
