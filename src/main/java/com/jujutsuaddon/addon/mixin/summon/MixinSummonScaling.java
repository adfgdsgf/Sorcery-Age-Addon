package com.jujutsuaddon.addon.mixin.summon;

import com.jujutsuaddon.addon.summon.SummonScalingHelper;
import com.jujutsuaddon.addon.util.helper.WeaponEffectProxy; // 确保导入这个
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import radon.jujutsu_kaisen.entity.base.SummonEntity;

@Mixin(SummonEntity.class)
public abstract class MixinSummonScaling extends TamableAnimal {

    protected MixinSummonScaling(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "onAddedToWorld", at = @At("TAIL"), remap = false)
    private void onJJKAddedToWorld(CallbackInfo ci) {
        if (this.level().isClientSide) return;

        if (this.getOwner() instanceof Player owner) {
            WeaponEffectProxy.registerSummonOwner(this, owner);
        }

        // 调用 Helper
        SummonScalingHelper.runScalingLogic(this);

        this.setHealth(this.getMaxHealth());

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(this,
                    new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(this.getId(),
                            this.getAttributes().getSyncableAttributes()));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onJJKTick(CallbackInfo ci) {
        if (this.level().isClientSide) return;

        // 频率控制逻辑保留在 Mixin 中比较合适，或者也可以移到 Helper
        if (this.tickCount < 60) {
            if (this.tickCount % 10 == 0) {
                SummonScalingHelper.runScalingLogic(this);
                if (this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
            }
        } else {
            if (this.tickCount % 40 == 0) {
                SummonScalingHelper.runScalingLogic(this);
            }
        }
    }
}
