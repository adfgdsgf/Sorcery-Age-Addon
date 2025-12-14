package com.jujutsuaddon.addon.mixin.summon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TamableAnimal.class)
public abstract class MixinSummonFriendlyFire extends Animal {

    protected MixinSummonFriendlyFire(net.minecraft.world.entity.EntityType<? extends Animal> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    // 1. 允许攻击 (WantsToAttack)
    // 解决“能不能出刀”的问题
    @Inject(method = "wantsToAttack", at = @At("HEAD"), cancellable = true)
    public void allowAttackingTraitorSummons(LivingEntity target, LivingEntity owner, CallbackInfoReturnable<Boolean> cir) {
        if (!this.getClass().getName().contains("radon.jujutsu_kaisen")) return;

        // 优先级最高：如果是仪式敌人，必须允许攻击
        if (target.getTags().contains("jjk_ritual_enemy")) {
            cir.setReturnValue(true);
            return;
        }

        if (!(target instanceof TamableAnimal targetSummon)) return;
        if (targetSummon.getOwner() != owner) return;

        boolean isTraitor = (targetSummon.getTarget() == owner);
        boolean isManualCommand = owner.isShiftKeyDown();

        if (isTraitor || isManualCommand) {
            cir.setReturnValue(true);
        }
    }

    // 2. 打破盟友关系 (IsAlliedTo)
    // 【核心修复】解决“呆子不动”的问题
    // 告诉系统：如果对方是仪式怪，它就不是我的盟友，不用客气，打！
    @Inject(method = "isAlliedTo", at = @At("HEAD"), cancellable = true)
    public void breakAllianceWithRitualEnemy(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (!this.getClass().getName().contains("radon.jujutsu_kaisen")) return;

        // 如果对方身上有“仪式敌人”的标签，直接视为陌生人/敌人
        if (other.getTags().contains("jjk_ritual_enemy")) {
            cir.setReturnValue(false);
        }
    }
}
