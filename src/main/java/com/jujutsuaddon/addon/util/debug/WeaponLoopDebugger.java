/*
package com.jujutsuaddon.addon.util.debug;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

*/
/**
 * 临时 Debug 类 - 用于追踪伤害循环问题
 * 用完删除
 *//*

@Mod.EventBusSubscriber(modid = "jujutsu_addon")
public class WeaponLoopDebugger {

    private static int damageCounter = 0;
    private static long lastResetTime = 0;

    private static final java.util.Map<UUID, Integer> hitCountThisTick = new ConcurrentHashMap<>();
    private static long currentTickTime = 0;

    private static ServerPlayer lastPlayer = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        Entity directEntity = source.getDirectEntity();
        Entity sourceEntity = source.getEntity();
        long gameTime = target.level().getGameTime();

        // ★ 过滤掉没有攻击者的伤害（debuff、环境伤害等）
        if (directEntity == null && sourceEntity == null) {
            return; // 跳过 debuff/毒/凋零/火焰等
        }

        // ★ 只关注与玩家相关的伤害
        if (sourceEntity instanceof ServerPlayer sp) {
            lastPlayer = sp;
        } else if (directEntity instanceof ServerPlayer sp) {
            lastPlayer = sp;
        }

        if (lastPlayer == null || lastPlayer.isRemoved()) return;

        // 每秒重置计数器
        if (gameTime - lastResetTime > 20) {
            damageCounter = 0;
            lastResetTime = gameTime;
        }

        // 每 tick 重置命中计数
        if (gameTime != currentTickTime) {
            hitCountThisTick.clear();
            currentTickTime = gameTime;
        }

        int hitCount = hitCountThisTick.merge(target.getUUID(), 1, Integer::sum);
        damageCounter++;

        // 限制输出频率
        if (damageCounter > 20) {
            if (damageCounter == 21) {
                sendMsg(lastPlayer, "§c[!] 伤害事件过多，暂停输出...");
            }
            return;
        }

        // 基本信息
        sendMsg(lastPlayer, "§e===== [伤害 #" + damageCounter + "] Tick:" + gameTime + " =====");
        sendMsg(lastPlayer, "§7目标: §f" + target.getClass().getSimpleName() +
                " §7| 本tick第 §c" + hitCount + " §7次");
        sendMsg(lastPlayer, "§7伤害: §f" + String.format("%.2f", event.getAmount()));

        // 伤害源
        String sourceClassName = source.getClass().getSimpleName();
        String sourceFullName = source.getClass().getName();
        sendMsg(lastPlayer, "§7伤害源: §b" + sourceClassName + " §7| msgId: §b" + source.getMsgId());

        // 攻击者
        String directName = directEntity != null ? directEntity.getClass().getSimpleName() : "null";
        String sourceName = sourceEntity != null ? sourceEntity.getClass().getSimpleName() : "null";
        sendMsg(lastPlayer, "§7直接: §a" + directName + " §7| 源: §a" + sourceName);

        // JJK 检测
        if (sourceFullName.contains("jujutsu_kaisen")) {
            try {
                var method = source.getClass().getMethod("getAbility");
                Object ability = method.invoke(source);
                String abilityName = ability != null ? ability.getClass().getSimpleName() : "null";
                sendMsg(lastPlayer, "§d★ JJK技能: " + abilityName);
            } catch (Exception e) {
                sendMsg(lastPlayer, "§d★ JJK伤害源 (无法获取技能)");
            }
        }

        // 关键调用栈
        StringBuilder stackInfo = new StringBuilder("§7栈: ");
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int count = 0;
        for (StackTraceElement element : stack) {
            String className = element.getClassName().toLowerCase();
            String methodName = element.getMethodName();
            if (className.contains("jujutsu") ||
                    className.contains("addon") ||
                    className.contains("weapon") ||
                    className.contains("proxy") ||
                    className.contains("tide") ||
                    className.contains("claw") ||
                    className.contains("tentacle") ||
                    methodName.toLowerCase().contains("hurtenemy") ||
                    methodName.toLowerCase().contains("trigger")) {

                String shortClass = element.getClassName();
                int lastDot = shortClass.lastIndexOf('.');
                if (lastDot > 0) shortClass = shortClass.substring(lastDot + 1);

                stackInfo.append("§e").append(shortClass)
                        .append("§7.§f").append(methodName)
                        .append(" §7→ ");
                count++;
                if (count > 6) break;
            }
        }
        if (count > 0) {
            sendMsg(lastPlayer, stackInfo.toString());
        }

        // 循环警告
        if (hitCount > 3) {
            sendMsg(lastPlayer, "§c§l⚠ 警告: 同一tick内攻击" + hitCount + "次！");
        }
    }

    private static void sendMsg(Player player, String msg) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(msg));
        }
    }
}
*/
