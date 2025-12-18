package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.util.helper.SummonAIHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

import java.util.List;

@Mod.EventBusSubscriber(modid = "jujutsu_addon", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SummonEventHandler {

    // ================================================================
    // 护主：主人被攻击
    // ================================================================
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onOwnerHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        LivingEntity attacker = null;
        if (event.getSource().getEntity() instanceof LivingEntity living) {
            attacker = living;
        }
        if (attacker == null) return;

        notifySummonsToDefend(victim, attacker);
    }

    // ================================================================
    // 助攻：主人攻击别人（最高优先级）
    // ================================================================
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onOwnerAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        if (!(event.getSource().getEntity() instanceof Player attacker)) return;

        notifySummonsToAssist(attacker, target);
    }

    // ================================================================
    // 护主逻辑
    // ================================================================
    private static void notifySummonsToDefend(LivingEntity owner, LivingEntity attacker) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) return;

        List<SummonEntity> summons = serverLevel.getEntitiesOfClass(
                SummonEntity.class,
                owner.getBoundingBox().inflate(50),
                summon -> summon.isTame() && SummonAIHelper.isOwner(summon, owner)
        );

        for (SummonEntity summon : summons) {
            if (!SummonAIHelper.canTargetEntity(summon, attacker, owner)) continue;
            if (summon.getTarget() == attacker) continue;

            // ★ 如果已经在打另一个攻击主人的敌人，不切换（防止横跳）
            if (SummonAIHelper.isTargetingOwnerAttacker(summon, owner)) continue;

            // ★ 如果有手动目标（指令设置的），护主不覆盖
            if (SummonAIHelper.hasManualTarget(summon)) continue;

            summon.setTarget(attacker);
            summon.removeTag("jjk_is_auto_targeting");
        }
    }

    // ================================================================
    // 助攻逻辑（玩家主动攻击 = 最高优先级）
    // ================================================================
    private static void notifySummonsToAssist(LivingEntity owner, LivingEntity target) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) return;

        List<SummonEntity> summons = serverLevel.getEntitiesOfClass(
                SummonEntity.class,
                owner.getBoundingBox().inflate(50),
                summon -> summon.isTame() && SummonAIHelper.isOwner(summon, owner)
        );

        for (SummonEntity summon : summons) {
            if (!SummonAIHelper.canTargetEntity(summon, target, owner)) continue;
            if (summon.getTarget() == target) continue;

            // ★★★ 玩家主动攻击 = 无条件切换（最高优先级）★★★
            summon.setTarget(target);
            summon.removeTag("jjk_is_auto_targeting");
        }
    }
}
