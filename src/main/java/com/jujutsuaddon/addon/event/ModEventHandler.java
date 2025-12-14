package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.JujutsuAddon;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 全局事件处理器 (已废弃/仅作存储)
 *
 * 注意：
 * 原本所有的事件监听逻辑都写在这里，导致代码极其臃肿。
 * 现已拆分为以下专业处理器：
 * 1. {@link DamageEventHandler} - 处理战斗、伤害、暴击
 * 2. {@link PlayerEventHandler} - 处理玩家状态、重生、Tick
 *
 * 此类目前仅用于存储跨处理器共享的全局变量。
 */
@Mod.EventBusSubscriber(modid = JujutsuAddon.MODID)
public class ModEventHandler {

    /**
     * 原版暴击缓存
     * 用于在 CriticalHitEvent (DamageEventHandler) 和 LivingHurtEvent (DamageEventHandler) 之间传递暴击倍率。
     */
    public static final Map<UUID, Float> vanillaCritCache = new HashMap<>();

    /**
     * 释魂刀伤害缓存 (ThreadLocal)
     * 用于在 captureFinalHurtDamage 和 onLivingHurt 之间传递原始伤害值，
     * 以便在护甲计算前捕获伤害，实现真伤逻辑。
     */
    public static final ThreadLocal<Float> SSK_PRE_ARMOR_DAMAGE = new ThreadLocal<>();

}
