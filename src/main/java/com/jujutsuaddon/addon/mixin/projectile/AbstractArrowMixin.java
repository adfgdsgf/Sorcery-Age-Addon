package com.jujutsuaddon.addon.mixin.projectile;

import com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.projectile.ControlledProjectileTick;
import com.jujutsuaddon.addon.api.IFrozenProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Entity {

    @Shadow
    protected boolean inGround;

    public AbstractArrowMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void jujutsuAddon$onTick(CallbackInfo ci) {
        if (!(this instanceof IFrozenProjectile fp)) return;
        if (!fp.jujutsuAddon$isControlled()) return;

        // 箭矢已在地上，释放控制
        if (this.inGround) {
            fp.jujutsuAddon$setControlled(false);
            return;
        }

        // 调用 Entity.tick()，不是 Projectile.tick()
        // 这样不会触发 ProjectileMixin 的注入
        super.tick();

        // ★★★ 只在服务端执行控制逻辑 ★★★
        if (!this.level().isClientSide) {
            ControlledProjectileTick.tick((AbstractArrow) (Object) this);
        }

        this.checkInsideBlocks();
        ci.cancel();
    }
}
