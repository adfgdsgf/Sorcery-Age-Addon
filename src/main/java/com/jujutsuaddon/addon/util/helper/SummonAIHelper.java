package com.jujutsuaddon.addon.util.helper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import java.util.HashSet;
import java.util.Set;

public class SummonAIHelper {

    /**
     * 清理掉原模组那些“不听话”的 AI 目标
     */
    public static void cleanUpBadGoals(GoalSelector targetSelector) {
        Set<WrappedGoal> goalsToRemove = new HashSet<>();
        for (WrappedGoal goal : targetSelector.getAvailableGoals()) {
            Class<?> goalClass = goal.getGoal().getClass();
            String className = goalClass.getName();

            // 移除原版的护主、反击逻辑，因为我们要用自己的智能版
            if (goal.getGoal() instanceof OwnerHurtByTargetGoal ||
                    goal.getGoal() instanceof OwnerHurtTargetGoal ||
                    goal.getGoal() instanceof HurtByTargetGoal) {
                goalsToRemove.add(goal);
            }
            // 移除 JJK 原模组的 TargetGoal (通常写得很死板)
            if (TargetGoal.class.isAssignableFrom(goalClass) && className.contains("radon.jujutsu_kaisen")) {
                goalsToRemove.add(goal);
            }
        }
        for (WrappedGoal goal : goalsToRemove) {
            targetSelector.removeGoal(goal.getGoal());
        }
    }

    /**
     * 注册我们自己的智能 AI 目标
     */
    public static void registerSmartGoals(TamableAnimal summon, GoalSelector targetSelector) {
        // 1. 助攻 (优先级 1)
        targetSelector.addGoal(1, new OwnerHurtTargetGoal(summon) {
            @Override
            public boolean canUse() {
                // 如果当前有目标，且不是自动索敌找来的（即手动），则不切换
                if (summon.getTarget() != null && summon.getTarget().isAlive()) {
                    if (!summon.getTags().contains("jjk_is_auto_targeting")) return false;
                }
                return super.canUse();
            }
            @Override
            public void start() {
                super.start();
                summon.removeTag("jjk_is_auto_targeting");
            }
        });

        // 2. 护主 (优先级 2)
        targetSelector.addGoal(2, new OwnerHurtByTargetGoal(summon) {
            @Override
            public boolean canUse() {
                if (summon.getTarget() != null && summon.getTarget().isAlive()) {
                    if (!summon.getTags().contains("jjk_is_auto_targeting")) return false;
                }
                return super.canUse();
            }
            @Override
            public void start() {
                super.start();
                summon.removeTag("jjk_is_auto_targeting");
            }
        });

        // 3. 反击 (优先级 3)
        targetSelector.addGoal(3, new HurtByTargetGoal(summon) {
            @Override
            public boolean canUse() {
                if (summon.getTarget() != null && summon.getTarget().isAlive()) {
                    if (!summon.getTags().contains("jjk_is_auto_targeting")) return false;
                }
                return super.canUse();
            }
        });

        // 4. 自动索敌 (优先级 4)
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(
                summon,
                LivingEntity.class,
                10,
                false,
                false,
                (entity) -> {
                    if (entity.getTags().contains("jjk_ritual_enemy")) return true;
                    LivingEntity theOwner = summon.getOwner();
                    if (theOwner != null) {
                        if (entity == theOwner) return false;
                        if (entity == summon) return false;
                        if (entity instanceof TamableAnimal pet && pet.getOwner() == theOwner) {
                            return pet.getTarget() == theOwner;
                        }
                    }

                    // 识别未调伏的十影式神为敌人
                    if (entity instanceof TenShadowsSummon tenShadows && !tenShadows.isTame()) {
                        return true;
                    }

                    return entity instanceof Enemy;
                }
        ) {
            @Override
            public boolean canUse() {
                // 只要有目标，就不自动索敌
                if (summon.getTarget() != null) return false;
                return super.canUse();
            }
            @Override
            public void start() {
                super.start();
                summon.addTag("jjk_is_auto_targeting");
            }
            @Override
            public void stop() {
                super.stop();
                summon.removeTag("jjk_is_auto_targeting");
            }
        });
    }
}
