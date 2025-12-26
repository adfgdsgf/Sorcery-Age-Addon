package com.jujutsuaddon.addon.summon.ai;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.util.helper.SummonAIHelper;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

import java.util.HashMap;
import java.util.Map;

public class SummonAIManager {

    private static final SummonAIManager INSTANCE = new SummonAIManager();
    private final Map<Integer, SummonAIController> controllers = new HashMap<>();

    private SummonAIManager() {}

    public static SummonAIManager getInstance() {
        return INSTANCE;
    }

    public SummonAIController getOrCreateController(SummonEntity summon) {
        return controllers.computeIfAbsent(summon.getId(), id -> new SummonAIController(summon));
    }

    public void tickSummonAI(SummonEntity summon) {
        if (summon.level().isClientSide) return;
        if (!summon.isTame()) return;

        SummonAIController ctrl = getOrCreateController(summon);

        // 1. 初始化 AI Goals
        if (!ctrl.isGoalsInitialized()) {
            SummonAIHelper.cleanUpBadGoals(summon.targetSelector);
            SummonAIHelper.registerSmartGoals(summon, summon.targetSelector);
            ctrl.setGoalsInitialized(true);
        }

        // 2. 检测目标状态
        LivingEntity currentTarget = summon.getTarget();

        if (currentTarget != null) {
            if (!currentTarget.isAlive() || currentTarget.isRemoved()) {
                summon.setTarget(null);
                summon.removeTag("jjk_is_auto_targeting");
                tryAutoTarget(summon);
                return;
            }
        } else {
            if (summon.tickCount % 10 == 0) {
                tryAutoTarget(summon);
            }
        }

        // 3. 跳跃辅助
        handleJumpAssist(summon, ctrl);

        // 4. 距离回防
        handleDistanceRegroup(summon);

        // 5. 扩展索敌范围
        if (summon.tickCount % 60 == 0) {
            SummonAIHelper.extendFollowRange(summon, 64.0);
        }
    }

    public void tryAutoTarget(SummonEntity summon) {
        LivingEntity currentTarget = summon.getTarget();
        if (currentTarget != null && currentTarget.isAlive() && !currentTarget.isRemoved()) {
            return;
        }

        LivingEntity bestTarget = SummonAIHelper.findBestTarget(summon, 24.0);

        if (bestTarget != null) {
            summon.setTarget(bestTarget);
            summon.addTag("jjk_is_auto_targeting");
        }
    }

    private void handleJumpAssist(SummonEntity summon, SummonAIController ctrl) {
        ctrl.decrementJumpCooldown();
        if (summon.horizontalCollision && summon.onGround()
                && summon.getTarget() != null && ctrl.getJumpCooldown() == 0) {
            summon.getJumpControl().jump();
            ctrl.setJumpCooldown(10);
        }
    }

    private void handleDistanceRegroup(SummonEntity summon) {
        LivingEntity owner = summon.getOwner();
        LivingEntity target = summon.getTarget();
        if (owner == null || target == null) return;

        double distToOwnerSqr = summon.distanceToSqr(owner);
        double baseDist = AddonConfig.COMMON.summonRegroupDistance.get();

        if (!summon.getTags().contains("jjk_is_auto_targeting")) {
            baseDist *= AddonConfig.COMMON.manualTargetDistanceMultiplier.get();
        }

        if (distToOwnerSqr > baseDist * baseDist) {
            summon.setTarget(null);
            summon.removeTag("jjk_is_auto_targeting");
        }
    }
}
