package com.jujutsuaddon.addon.mixin.summon.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

@Mixin(SummonEntity.class)
public abstract class MixinSummonEntityFix extends TamableAnimal {

    protected MixinSummonEntityFix(net.minecraft.world.entity.EntityType<? extends TamableAnimal> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    /**
     * 拦截 SummonEntity.actuallyHurt 中的 setTarget 调用。
     * 只有在“当前没有重要目标”时，才允许因为挨打而切换目标。
     */
    @Redirect(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lradon/jujutsu_kaisen/entity/base/SummonEntity;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"
            )
    )
    private void preventTargetSwitchOnHurt(SummonEntity instance, LivingEntity attacker) {
        // 【关键修正】如果式神未调伏（是敌人），不要干涉它的仇恨逻辑！让它保持原版行为。
        if (!instance.isTame()) {
            instance.setTarget(attacker);
            return;
        }

        // --- 下面是已调伏式神的逻辑 ---

        // 1. 如果当前没有目标，或者目标已经死了 -> 允许反击
        if (instance.getTarget() == null || !instance.getTarget().isAlive()) {
            instance.setTarget(attacker);
            return;
        }

        // 2. 如果当前有目标，但这个目标是“自动索敌”找来的（不重要） -> 允许反击，转火打攻击者
        if (instance.getTags().contains("jjk_is_auto_targeting")) {
            instance.setTarget(attacker);
            instance.removeTag("jjk_is_auto_targeting"); // 既然转火了，就移除自动标记
            return;
        }

        // 3. 如果当前有目标，且没有自动标记（说明是主人手动指定的） -> **拒绝反击**！
        // (什么都不做，保持当前目标)
    }
}
