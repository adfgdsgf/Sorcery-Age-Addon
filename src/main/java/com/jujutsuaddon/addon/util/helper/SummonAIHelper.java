package com.jujutsuaddon.addon.util.helper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import radon.jujutsu_kaisen.entity.base.SummonEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import java.util.*;

public class SummonAIHelper {

    private static final UUID FOLLOW_RANGE_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000003");

    // ================================================================
    // AI Goal 管理
    // ================================================================

    public static void cleanUpBadGoals(GoalSelector targetSelector) {
        Set<WrappedGoal> goalsToRemove = new HashSet<>();
        for (WrappedGoal goal : targetSelector.getAvailableGoals()) {
            Class<?> goalClass = goal.getGoal().getClass();
            String className = goalClass.getName();

            if (goal.getGoal() instanceof OwnerHurtByTargetGoal ||
                    goal.getGoal() instanceof OwnerHurtTargetGoal ||
                    goal.getGoal() instanceof HurtByTargetGoal ||
                    goal.getGoal() instanceof NearestAttackableTargetGoal) {
                goalsToRemove.add(goal);
            }
            if (TargetGoal.class.isAssignableFrom(goalClass) && className.contains("radon.jujutsu_kaisen")) {
                goalsToRemove.add(goal);
            }
        }
        for (WrappedGoal goal : goalsToRemove) {
            targetSelector.removeGoal(goal.getGoal());
        }
    }

    public static void registerSmartGoals(TamableAnimal summon, GoalSelector targetSelector) {
        // 1. 助攻
        targetSelector.addGoal(1, new OwnerHurtTargetGoal(summon) {
            @Override
            public boolean canUse() {
                if (summon.getTarget() != null && summon.getTarget().isAlive()) {
                    if (!summon.getTags().contains("jjk_is_auto_targeting")) return false;
                }
                LivingEntity owner = summon.getOwner();
                if (owner == null) return false;
                LivingEntity target = owner.getLastHurtMob();
                if (target == null || target == summon) return false;  // ★ 排除自己
                return super.canUse();
            }
            @Override
            public void start() {
                super.start();
                summon.removeTag("jjk_is_auto_targeting");
            }
        });
        // 2. 护主
        targetSelector.addGoal(2, new OwnerHurtByTargetGoal(summon) {
            @Override
            public boolean canUse() {
                if (summon.getTarget() != null && summon.getTarget().isAlive()) {
                    if (!summon.getTags().contains("jjk_is_auto_targeting")) return false;
                }
                LivingEntity owner = summon.getOwner();
                if (owner == null) return false;
                LivingEntity attacker = owner.getLastHurtByMob();
                if (attacker == null || attacker == summon) return false;  // ★ 排除自己
                return super.canUse();
            }
            @Override
            public void start() {
                super.start();
                summon.removeTag("jjk_is_auto_targeting");
            }
        });
        // 3. 反击 ★★★ 这里是关键修复 ★★★
        targetSelector.addGoal(3, new HurtByTargetGoal(summon) {
            @Override
            public boolean canUse() {
                if (summon.getTarget() != null && summon.getTarget().isAlive()) {
                    if (!summon.getTags().contains("jjk_is_auto_targeting")) return false;
                }

                LivingEntity attacker = summon.getLastHurtByMob();

                // ★ 不能反击自己（防止魔虚罗范围伤害自伤）
                if (attacker == null || attacker == summon) return false;

                // ★ 不能反击主人
                if (summon instanceof SummonEntity se) {
                    LivingEntity owner = se.getOwner();
                    if (owner != null && attacker == owner) return false;

                    // ★ 不能反击同主人的其他式神
                    if (attacker instanceof SummonEntity otherSummon && otherSummon.isTame()) {
                        LivingEntity otherOwner = otherSummon.getOwner();
                        if (otherOwner != null && owner != null &&
                                otherOwner.getUUID().equals(owner.getUUID())) {
                            return false;
                        }
                    }
                }

                return super.canUse();
            }
            @Override
            public void start() {
                super.start();
                summon.removeTag("jjk_is_auto_targeting");
            }
        });
    }

    // ================================================================
    // 目标寻找
    // ================================================================

    public static LivingEntity findBestTarget(SummonEntity summon, double searchRange) {
        LivingEntity owner = summon.getOwner();
        if (owner == null) return null;

        AABB searchBox = summon.getBoundingBox().inflate(searchRange);

        List<LivingEntity> potentialTargets = summon.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> isValidAutoTarget(entity, summon, owner)
        );

        if (potentialTargets.isEmpty()) return null;

        return potentialTargets.stream()
                .min(Comparator
                        .comparingInt((LivingEntity e) -> getTargetPriority(e, owner))
                        .thenComparingDouble(summon::distanceToSqr))
                .orElse(null);
    }

    // ================================================================
    // 目标验证
    // ================================================================

    public static boolean isValidAutoTarget(LivingEntity entity, SummonEntity summon, LivingEntity owner) {
        if (entity == summon) return false;
        if (entity == owner) return false;
        if (!entity.isAlive()) return false;
        if (entity.isRemoved()) return false;

        if (entity instanceof SummonEntity otherSummon && otherSummon.isTame()) {
            LivingEntity otherOwner = otherSummon.getOwner();
            if (otherOwner != null && otherOwner.getUUID().equals(owner.getUUID())) {
                return false;
            }
        }

        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity petOwner = tamable.getOwner();
            if (petOwner != null && petOwner.getUUID().equals(owner.getUUID())) {
                return false;
            }
        }

        if (entity.getTags().contains("jjk_ritual_enemy")) return true;
        if (entity instanceof TenShadowsSummon ts && !ts.isTame()) return true;
        if (entity instanceof Enemy) return true;
        if (entity instanceof Mob mob && mob.getTarget() == owner) return true;

        return false;
    }

    public static boolean canTargetEntity(SummonEntity summon, LivingEntity target, LivingEntity owner) {
        if (target == null || !target.isAlive()) return false;
        if (target == summon || target == owner) return false;

        if (target instanceof SummonEntity otherSummon && otherSummon.isTame()) {
            LivingEntity otherOwner = otherSummon.getOwner();
            if (otherOwner != null && otherOwner.getUUID().equals(owner.getUUID())) {
                return false;
            }
        }

        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity petOwner = tamable.getOwner();
            if (petOwner != null && petOwner.getUUID().equals(owner.getUUID())) {
                return false;
            }
        }

        return true;
    }

    public static int getTargetPriority(LivingEntity entity, LivingEntity owner) {
        if (entity instanceof Mob mob && mob.getTarget() == owner) return 0;
        if (entity instanceof TenShadowsSummon ts && !ts.isTame()) return 1;
        if (entity.getTags().contains("jjk_ritual_enemy")) return 2;
        if (entity instanceof Enemy) return 3;
        return 10;
    }

    // ================================================================
    // 属性扩展
    // ================================================================

    public static void extendFollowRange(SummonEntity summon, double extraRange) {
        AttributeInstance followRange = summon.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null && followRange.getModifier(FOLLOW_RANGE_UUID) == null) {
            followRange.addTransientModifier(new AttributeModifier(
                    FOLLOW_RANGE_UUID, "JJK Addon Smart AI", extraRange, AttributeModifier.Operation.ADDITION));
        }
    }

    // ================================================================
    // 工具方法
    // ================================================================

    public static boolean isOwner(SummonEntity summon, LivingEntity entity) {
        LivingEntity owner = summon.getOwner();
        return owner != null && owner.getUUID().equals(entity.getUUID());
    }

    public static boolean hasManualTarget(SummonEntity summon) {
        return summon.getTarget() != null
                && summon.getTarget().isAlive()
                && !summon.getTags().contains("jjk_is_auto_targeting");
    }

    /**
     * 判断式神当前是否在攻击一个正在攻击主人的敌人
     * 用于护主时防止横跳
     */
    public static boolean isTargetingOwnerAttacker(SummonEntity summon, LivingEntity owner) {
        LivingEntity target = summon.getTarget();
        if (target == null || !target.isAlive()) return false;

        if (target instanceof Mob mob) {
            return mob.getTarget() == owner;
        }
        return false;
    }
}
