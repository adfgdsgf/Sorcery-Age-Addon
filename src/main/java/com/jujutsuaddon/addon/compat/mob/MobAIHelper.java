package com.jujutsuaddon.addon.compat.mob;

import com.jujutsuaddon.addon.compat.mob.goal.CustomSorcererGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.entity.ai.goal.WaterWalkingFloatGoal;

public class MobAIHelper {

    public static void injectAI(PathfinderMob mob) {
        if (mob.getCapability(SorcererDataHandler.INSTANCE).isPresent()) {
            boolean hasGoal = mob.goalSelector.getAvailableGoals().stream()
                    .anyMatch(wrapped -> wrapped.getGoal() instanceof CustomSorcererGoal);

            if (!hasGoal) {
                // 1. 自定义咒术师 AI
                mob.goalSelector.addGoal(1, new CustomSorcererGoal(mob));

                // 2. 水上行走
                mob.goalSelector.addGoal(2, new WaterWalkingFloatGoal(mob));

                // ★ 3. 基于阵营的目标选择器 ★
                mob.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                        mob,
                        LivingEntity.class,
                        10,
                        true,
                        false,
                        (target) -> MobFactionHelper.isEnemy(mob, target)
                ));
            }
        }
    }
}
