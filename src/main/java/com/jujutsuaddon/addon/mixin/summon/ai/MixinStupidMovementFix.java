package com.jujutsuaddon.addon.mixin.summon.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import radon.jujutsu_kaisen.entity.curse.KuchisakeOnnaEntity;
import radon.jujutsu_kaisen.entity.idle_transfiguration.base.TransfiguredSoulEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.AgitoEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.DivineDogEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.GreatSerpentEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.MahoragaEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.MaxElephantEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.NueEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.NueTotalityEntity;
import radon.jujutsu_kaisen.entity.ten_shadows.ToadEntity;

@Mixin({
        AgitoEntity.class,
        DivineDogEntity.class,
        GreatSerpentEntity.class,
        MahoragaEntity.class,
        MaxElephantEntity.class,
        //NueEntity.class,
        //NueTotalityEntity.class,
        ToadEntity.class,
        KuchisakeOnnaEntity.class,
        TransfiguredSoulEntity.class
})
public abstract class MixinStupidMovementFix extends TamableAnimal {

    protected MixinStupidMovementFix(net.minecraft.world.entity.EntityType<? extends TamableAnimal> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    @Redirect(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/control/MoveControl;setWantedPosition(DDDD)V"
            ),
            remap = true
    )
    private void stopStupidMovement(MoveControl instance, double x, double y, double z, double speed) {
        TamableAnimal entity = (TamableAnimal) (Object) this;

        // ============================================================
        // 策略 A：改造人
        // ============================================================
        if (entity instanceof TransfiguredSoulEntity) {
            if (entity.getTarget() != null) {
                double baseSpeed = 1.4D;
                double chaosFactor = (entity.getRandom().nextDouble() * 0.6) - 0.2;
                if (entity.getRandom().nextInt(20) == 0) {
                    chaosFactor += 0.8;
                }
                entity.getNavigation().setSpeedModifier(baseSpeed + chaosFactor);
            }
            return;
        }

        // ============================================================
        // 策略 B：魔虚罗 - 智能放行
        // ============================================================
        if (entity instanceof MahoragaEntity) {
            LivingEntity target = entity.getTarget();

            // 没有目标时，屏蔽原版命令（防止跑向世界尽头）
            if (target == null) {
                return;
            }

            double distSqr = entity.distanceToSqr(target);

            // 死区（4.5~12格）：MeleeAttackGoal 可能停下来，但斩击还没触发
            // 用高速命令强制冲刺
            if (distSqr > 20.0 && distSqr < 144.0) {
                instance.setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.2);
                return;
            }

            // ★★★ 关键修复：远距离或近距离时，放行原版命令！★★★
            instance.setWantedPosition(x, y, z, speed);
            return;
        }

        // ============================================================
        // 策略 C：其他实体 - 屏蔽垃圾代码
        // ============================================================
        // 留空
    }
}
