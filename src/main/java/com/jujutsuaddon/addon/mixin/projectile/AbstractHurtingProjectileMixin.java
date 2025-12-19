package com.jujutsuaddon.addon.mixin.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile.ControlledProjectileTick;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin extends Projectile {

    protected AbstractHurtingProjectileMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void jujutsuAddon$onTick(CallbackInfo ci) {
        if (this instanceof IFrozenProjectile fp && fp.jujutsuAddon$isControlled()) {
            // ★★★ 调用 super.tick() ★★★
            super.tick();

            // ★★★ 两端都执行 ★★★
            ControlledProjectileTick.tickHurtingProjectile((AbstractHurtingProjectile)(Object)this);

            ci.cancel();
        }
    }
}
