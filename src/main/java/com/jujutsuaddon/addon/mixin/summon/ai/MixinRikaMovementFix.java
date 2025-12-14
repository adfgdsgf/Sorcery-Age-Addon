package com.jujutsuaddon.addon.mixin.summon.ai;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import radon.jujutsu_kaisen.entity.curse.RikaEntity;

@Mixin(RikaEntity.class)
public abstract class MixinRikaMovementFix extends TamableAnimal {

    protected MixinRikaMovementFix(net.minecraft.world.entity.EntityType<? extends TamableAnimal> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    /**
     * 专门针对里香的修复。
     * 她的强制移动逻辑写在 tick() 方法里，而不是 customServerAiStep()。
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/control/MoveControl;setWantedPosition(DDDD)V"
            ),
            remap = true
    )
    private void stopRikaStupidMovement(MoveControl instance, double x, double y, double z, double speed) {
        // 留空，屏蔽里香的强制移动
    }
}
