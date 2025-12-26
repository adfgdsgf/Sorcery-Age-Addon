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
 * 咒力量提升收益
 * Energy Boost Benefit
 *
 * 增加玩家的最大咒力量百分比。
 * 允许玩家储存更多咒力，施放更多技能。
 */
public class EnergyBoostBenefit implements IBenefit {

    /** 收益唯一ID */
    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "energy_boost");

    /** 参数键：投入的权重点数 */
    public static final String PARAM_INVESTED_POINTS = "investedPoints";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("benefit.jujutsu_addon.energy_boost");
    }

    @Override
    public Component getDescription(BenefitParams params) {
        float bonus = getCurrentBonus(null, params);
        int percentage = Math.round(bonus * 100);
        return Component.translatable("benefit.jujutsu_addon.energy_boost.desc", percentage);
    }

    @Override
    public float getRequiredWeight(BenefitParams params) {
        return params.getFloat(PARAM_INVESTED_POINTS, 1.0f);
    }

    @Override
    public boolean isScalable() { return true; }

    @Override
    public float getMaxBonus() { return VowCalculator.MAX_ENERGY_BONUS; }

    @Override
    public BenefitCategory getCategory() { return BenefitCategory.ENERGY; }

    @Override
    public void apply(LivingEntity owner, ISorcererData data, BenefitParams params) {}

    @Override
    public void remove(LivingEntity owner, ISorcererData data, BenefitParams params) {}

    @Override
    public float getCurrentBonus(@Nullable LivingEntity owner, BenefitParams params) {
        float invested = params.getFloat(PARAM_INVESTED_POINTS, 1.0f);
        return VowCalculator.calculateEnergyBonus(invested);
    }

    @Override
    public boolean isConfigurable() { return true; }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return new ParamDefinition()
                .addFloat(PARAM_INVESTED_POINTS,
                        Component.translatable("benefit.jujutsu_addon.energy_boost.param.points"),
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
