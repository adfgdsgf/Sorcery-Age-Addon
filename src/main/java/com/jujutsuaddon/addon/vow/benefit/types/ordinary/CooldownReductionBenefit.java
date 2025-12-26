package com.jujutsuaddon.addon.vow.benefit.types.ordinary;

import com.jujutsuaddon.addon.api.vow.IBenefit;
import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.benefit.BenefitCategory;
import com.jujutsuaddon.addon.vow.benefit.BenefitParams;
import com.jujutsuaddon.addon.vow.calculation.VowCalculator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;

/**
 * 冷却缩减收益
 * Cooldown Reduction Benefit
 *
 * 减少技能冷却时间的百分比。
 * 上限较低（50%），因为冷却缩减非常强力。
 */
public class CooldownReductionBenefit implements IBenefit {

    /** 收益唯一ID */
    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "cooldown_reduction");

    /** 参数键：投入的权重点数 */
    public static final String PARAM_INVESTED_POINTS = "investedPoints";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("benefit.jujutsu_addon.cooldown_reduction");
    }

    @Override
    public Component getDescription(BenefitParams params) {
        float bonus = getCurrentBonus(null, params);
        int percentage = Math.round(bonus * 100);
        return Component.translatable("benefit.jujutsu_addon.cooldown_reduction.desc", percentage);
    }

    @Override
    public float getRequiredWeight(BenefitParams params) {
        return params.getFloat(PARAM_INVESTED_POINTS, 1.0f);
    }

    @Override
    public boolean isScalable() { return true; }

    @Override
    public float getMaxBonus() { return VowCalculator.MAX_COOLDOWN_REDUCTION; }

    @Override
    public BenefitCategory getCategory() { return BenefitCategory.COOLDOWN; }

    @Override
    public void apply(LivingEntity owner, ISorcererData data, BenefitParams params) {}

    @Override
    public void remove(LivingEntity owner, ISorcererData data, BenefitParams params) {}

    @Override
    public float getCurrentBonus(@Nullable LivingEntity owner, BenefitParams params) {
        float invested = params.getFloat(PARAM_INVESTED_POINTS, 1.0f);
        return VowCalculator.calculateCooldownReduction(invested);
    }

    @Override
    public boolean isConfigurable() { return true; }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return new ParamDefinition()
                .addFloat(PARAM_INVESTED_POINTS,
                        Component.translatable("benefit.jujutsu_addon.cooldown_reduction.param.points"),
                        0.5f, 50f, 5.0f);
    }

    @Override
    public BenefitParams createDefaultParams() {
        return new BenefitParams().setFloat(PARAM_INVESTED_POINTS, 5.0f);
    }

    @Override
    public CompoundTag serializeParams(BenefitParams params) { return params.serializeNBT(); }

    @Override
    public BenefitParams deserializeParams(CompoundTag nbt) { return BenefitParams.fromNBT(nbt); }
}
