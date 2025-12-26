package com.jujutsuaddon.addon.mixin.ability;

import com.jujutsuaddon.addon.capability.vow.VowDataProvider;
import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.benefit.BenefitEntry;
import com.jujutsuaddon.addon.vow.benefit.types.permanent.WorldSlashBypassBenefit;
import com.jujutsuaddon.addon.vow.util.VowConditionHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.base.Ability.Status;
import radon.jujutsu_kaisen.ability.shrine.WorldSlash;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

@Mixin(value = WorldSlash.class, remap = false)
public class MixinWorldSlash {

    // 1. 绕过咏唱检查 (因)
    @Inject(method = "isTriggerable", at = @At("HEAD"), cancellable = true)
    public void injectBypass(LivingEntity owner, CallbackInfoReturnable<Status> cir) {
        // 安全检查：必须是御厨子术式
        if (!VowConditionHelper.hasInnateTechnique(owner, CursedTechnique.SHRINE)) {
            return;
        }

        // ★★★ 核心修复：回归标准调用 ★★★
        // 之前我们用了 VowManager.getEffectiveActiveVows，现在不需要了。
        // 因为 CustomBindingVow.getState() 已经处理了逻辑，
        // vowData.getActiveVows() 会自动包含那些“视同激活”的誓约。
        owner.getCapability(VowDataProvider.VOW_DATA).ifPresent(vowData -> {
            for (CustomBindingVow vow : vowData.getActiveVows()) {
                for (BenefitEntry entry : vow.getBenefits()) {
                    if (entry.getBenefit().getId().equals(WorldSlashBypassBenefit.ID)) {
                        // 找到了！直接返回 SUCCESS，跳过原版代码里的 CHANT 检查
                        cir.setReturnValue(Status.SUCCESS);
                        return;
                    }
                }
            }
        });
    }

    // 2. 技能释放成功后消耗誓约 (果)
    @Inject(method = "run", at = @At("HEAD"))
    public void onRun(LivingEntity owner, CallbackInfo ci) {
        if (owner.level().isClientSide) return;

        // 调用 Benefit 里的静态方法，消耗誓约
        // Benefit 内部调用了 VowManager.consumeBenefit，
        // 而 VowManager.consumeBenefit 已经包含了无限模式的判断，所以这里是安全的。
        WorldSlashBypassBenefit.onWorldSlashCast(owner);
    }
}
