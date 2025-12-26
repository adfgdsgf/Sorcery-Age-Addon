package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.JujutsuAddon;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory; // 【新增导入】
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class SummonEffectSyncHandler {

    @SubscribeEvent
    public static void onSummonTick(LivingEvent.LivingTickEvent event) {
        // 1. 基础检查：服务端、每秒同步一次 (20 ticks)
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity().tickCount % 20 != 0) return;

        // 2. 检查是否为开启了增强的召唤物
        if (event.getEntity() instanceof TamableAnimal summon && summon.getOwner() instanceof Player owner) {
            if (!AddonConfig.COMMON.enableSummonScaling.get()) return;

            // 3. 获取主人的所有药水效果/BUFF
            Collection<MobEffectInstance> ownerEffects = owner.getActiveEffects();

            for (MobEffectInstance ownerEffect : ownerEffects) {
                MobEffect type = ownerEffect.getEffect();

                // 【核心修改】过滤负面效果
                // 如果这个效果是“有害的”(HARMFUL)，比如中毒、凋零、缓慢，直接跳过，不传给召唤物
                if (type.getCategory() == MobEffectCategory.HARMFUL) continue;

                // 4. 同步给召唤物 (只同步增益和中性效果)
                // 只有当召唤物身上没有这个效果，或者等级比主人低时才更新
                if (!summon.hasEffect(type) || summon.getEffect(type).getAmplifier() < ownerEffect.getAmplifier()) {
                    summon.addEffect(new MobEffectInstance(
                            type,
                            60, // 持续3秒，留有余量防止闪烁
                            ownerEffect.getAmplifier(),
                            ownerEffect.isAmbient(),
                            ownerEffect.isVisible(),
                            ownerEffect.showIcon()
                    ));
                }
            }
        }
    }
}
