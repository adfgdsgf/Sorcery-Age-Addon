package com.jujutsuaddon.addon.compat.mob.goal;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import radon.jujutsu_kaisen.ability.AbilityHandler;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.AbsorbedCurse;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.item.CursedSpiritOrbItem;
import radon.jujutsu_kaisen.item.JJKItems;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.ArrayList;
import java.util.List;

public class CustomSorcererGoal extends Goal {
    private static final int CHANGE_COPIED_TECHNIQUE_INTERVAL = 10 * 20;

    private final PathfinderMob mob;
    private long lastCanUseCheck;

    // [性能优化] 缓存技能列表，避免每 Tick 重新获取
    private List<Ability> cachedAbilities = new ArrayList<>();
    private int cacheTimer = 0;

    public CustomSorcererGoal(PathfinderMob mob) {
        this.mob = mob;
    }

    @Override
    public void tick() {
        // [性能优化] 每 20 Tick (1秒) 更新一次技能列表缓存
        // 这样可以减少 95% 的 JJKAbilities.getAbilities 调用开销
        if (this.cacheTimer-- <= 0) {
            this.cachedAbilities = JJKAbilities.getAbilities(this.mob);
            this.cacheTimer = 20;
        }

        // 安全检查：如果 Capability 获取失败直接返回，防止空指针崩溃
        if (!this.mob.getCapability(SorcererDataHandler.INSTANCE).isPresent()) return;
        ISorcererData cap = this.mob.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

        // --- 乙骨忧太 (里香) 逻辑 ---
        // [微优化] 先判断是否有里香，再执行取余运算
        if (cap.hasToggled(JJKAbilities.RIKA.get())) {
            if (cap.getCurrentCopied() == null || this.mob.tickCount % CHANGE_COPIED_TECHNIQUE_INTERVAL == 0) {
                List<CursedTechnique> copied = new ArrayList<>(cap.getCopied());
                if (!copied.isEmpty()) {
                    cap.setCurrentCopied(copied.get(HelperMethods.RANDOM.nextInt(copied.size())));
                }
            }
        }

        // --- 夏油杰 (咒灵操术) 逻辑 ---
        if (cap.hasTechnique(CursedTechnique.CURSE_MANIPULATION)) {
            LivingEntity target = this.mob.getTarget();

            // [微优化] 只有当有目标且随机判定通过时，才去获取 curses 列表
            if (target != null && this.mob.tickCount % 5 == 0 && HelperMethods.RANDOM.nextInt(4) == 0) {
                List<AbsorbedCurse> curses = cap.getCurses();

                if (!curses.isEmpty()) {
                    if (target.getCapability(SorcererDataHandler.INSTANCE).isPresent()) {
                        ISorcererData targetCap = target.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();
                        AbsorbedCurse closest = null;
                        float minDiff = Float.MAX_VALUE;

                        // [微优化] 优化循环查找逻辑
                        float targetExp = (float) targetCap.getExperience();
                        for (AbsorbedCurse curse : curses) {
                            float diff = Math.abs(JJKAbilities.getCurseExperience(curse) - targetExp);
                            if (diff < minDiff) {
                                minDiff = diff;
                                closest = curse;
                            }
                        }

                        if (closest != null) {
                            JJKAbilities.summonCurse(this.mob, closest, true);
                        }
                    } else {
                        JJKAbilities.summonCurse(this.mob, HelperMethods.RANDOM.nextInt(curses.size()), true);
                    }
                }
            }

            // [微优化] 只有主手拿着咒灵玉时才执行吃玉逻辑
            ItemStack stack = this.mob.getMainHandItem();
            if (!stack.isEmpty() && stack.is(JJKItems.CURSED_SPIRIT_ORB.get())) {
                this.mob.playSound(this.mob.getEatingSound(stack), 1.0F, 1.0F + (HelperMethods.RANDOM.nextFloat() - HelperMethods.RANDOM.nextFloat()) * 0.4F);
                cap.addCurse(CursedSpiritOrbItem.getAbsorbed(stack));
                this.mob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }

        // --- 技能释放核心逻辑 ---
        // 使用缓存的列表进行遍历
        for (Ability ability : this.cachedAbilities) {
            // 如果是我们手动管理的技能，直接跳过，不让 AI 干涉
            if (isManagedManually(ability)) {
                continue;
            }

            // [微优化] 只有当技能冷却好了，才去判断 shouldTrigger
            // shouldTrigger 内部通常包含距离计算和视线检查，比较耗时
            if (!cap.isCooldownDone(ability)) {
                continue;
            }

            boolean success = ability.shouldTrigger(this.mob, this.mob.getTarget());
            Ability.ActivationType activationType = ability.getActivationType(this.mob);

            if (activationType == Ability.ActivationType.TOGGLED) {
                if (success) {
                    if (!JJKAbilities.hasToggled(this.mob, ability)){
                        AbilityHandler.trigger(this.mob, ability);
                    }
                } else if (JJKAbilities.hasToggled(this.mob, ability)) {
                    AbilityHandler.untrigger(this.mob, ability);
                    return; // 动作发生变化，结束本次 tick
                }
            } else if (activationType == Ability.ActivationType.CHANNELED) {
                if (success) {
                    if (!JJKAbilities.isChanneling(this.mob, ability)) {
                        AbilityHandler.trigger(this.mob, ability);
                        if (isBlockingAbility(ability)) return;
                    }
                } else if (JJKAbilities.isChanneling(this.mob, ability)) {
                    AbilityHandler.untrigger(this.mob, ability);
                    if (isBlockingAbility(ability)) return;
                }
            } else if (success) {
                // 瞬发技能
                AbilityHandler.trigger(this.mob, ability);
                if (isMovementAbility(ability)) {
                    return; // 位移技能释放后，暂停其他 AI 决策一小会儿
                }
            }
        }
    }

    /**
     * 判断技能是否由 MobTechniqueHelper 手动管理
     */
    private boolean isManagedManually(Ability ability) {
        if (ability == JJKAbilities.INFINITY.get()) return true;
        if (ability == JJKAbilities.CURSED_ENERGY_FLOW.get()) return true;
        if (ability == JJKAbilities.CLEAVE.get()) return true;
        return false;
    }

    /**
     * [新增] 辅助判断：是否为阻塞型技能 (治疗/反转)
     */
    private boolean isBlockingAbility(Ability ability) {
        return ability == JJKAbilities.HEAL.get() ||
                ability == JJKAbilities.RCT1.get() ||
                ability == JJKAbilities.RCT2.get() ||
                ability == JJKAbilities.RCT3.get();
    }

    /**
     * [新增] 辅助判断：是否为位移/体术技能
     */
    private boolean isMovementAbility(Ability ability) {
        return ability == JJKAbilities.QUICKDASH.get() ||
                ability == JJKAbilities.DASH.get() ||
                ability == JJKAbilities.SLAM.get() ||
                ability == JJKAbilities.PUNCH.get();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        long i = this.mob.level().getGameTime();
        if (i - this.lastCanUseCheck > 20L) {
            this.lastCanUseCheck = i;
            return true;
        } else {
            return false;
        }
    }
}
