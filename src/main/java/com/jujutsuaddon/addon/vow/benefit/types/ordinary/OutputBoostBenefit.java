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
 * 输出提升收益
 * Output Boost Benefit
 *
 * 增加玩家的咒力输出百分比。
 * 这是最常用的收益类型，直接增强所有咒术的威力。
 *
 * 计算公式使用对数曲线，前期提升快后期提升慢，
 * 防止投入过多权重获得不合理的收益。
 */
public class OutputBoostBenefit implements IBenefit {

    /** 收益唯一ID */
    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "output_boost");

    /** 参数键：投入的权重点数 */
    public static final String PARAM_INVESTED_POINTS = "investedPoints";

    /**
     * 获取收益ID
     */
    @Override
    public ResourceLocation getId() {
        return ID;
    }

    /**
     * 获取显示名称
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("benefit.jujutsu_addon.output_boost");
    }

    /**
     * 根据参数生成描述文本
     */
    @Override
    public Component getDescription(BenefitParams params) {
        // 计算当前收益值
        float bonus = getCurrentBonus(null, params);
        // 转换为百分比显示
        int percentage = Math.round(bonus * 100);
        return Component.translatable("benefit.jujutsu_addon.output_boost.desc", percentage);
    }

    /**
     * 获取此收益需要消耗的权重
     * 直接返回投入的点数（玩家在UI中设置）
     */
    @Override
    public float getRequiredWeight(BenefitParams params) {
        return params.getFloat(PARAM_INVESTED_POINTS, 1.0f);
    }

    /**
     * 此收益是否可缩放
     * true表示可以投入更多点数获得更高收益
     */
    @Override
    public boolean isScalable() {
        return true;
    }

    /**
     * 获取最大收益值（上限）
     */
    @Override
    public float getMaxBonus() {
        return VowCalculator.MAX_OUTPUT_BONUS;
    }

    /**
     * 获取收益类别
     */
    @Override
    public BenefitCategory getCategory() {
        return BenefitCategory.OUTPUT;
    }

    /**
     * 应用收益效果
     * 实际的输出修改通过Mixin或事件实现，这里可以添加视觉效果
     */
    @Override
    public void apply(LivingEntity owner, ISorcererData data, BenefitParams params) {
        // 输出加成的实际应用在VowManager.getActiveOutputBonus()中
        // 通过Mixin拦截伤害计算时调用该方法获取加成值
    }

    /**
     * 移除收益效果
     */
    @Override
    public void remove(LivingEntity owner, ISorcererData data, BenefitParams params) {
        // 输出加成会在getActiveOutputBonus()中自动不再计算已停用的誓约
    }

    /**
     * 获取当前收益的实际数值
     * @param owner 持有者（可为null，用于纯计算）
     * @param params 收益参数
     * @return 收益值（0.0-1.0+）
     */
    @Override
    public float getCurrentBonus(@Nullable LivingEntity owner, BenefitParams params) {
        // 获取投入的点数
        float invested = params.getFloat(PARAM_INVESTED_POINTS, 1.0f);
        // 使用VowCalculator计算实际收益（对数曲线）
        return VowCalculator.calculateOutputBonus(invested);
    }

    /**
     * 此收益是否可配置
     */
    @Override
    public boolean isConfigurable() {
        return true;
    }

    /**
     * 获取可配置参数定义（用于GUI生成）
     */
    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return new ParamDefinition()
                // 投入点数：0.5-50，默认5
                .addFloat(PARAM_INVESTED_POINTS,
                        Component.translatable("benefit.jujutsu_addon.output_boost.param.points"),
                        0.5f,   // 最小投入
                        50f,    // 最大投入
                        5.0f);  // 默认值
    }

    /**
     * 创建默认参数
     */
    @Override
    public BenefitParams createDefaultParams() {
        return new BenefitParams()
                .setFloat(PARAM_INVESTED_POINTS, 5.0f);
    }

    /**
     * 序列化参数
     */
    @Override
    public CompoundTag serializeParams(BenefitParams params) {
        return params.serializeNBT();
    }

    /**
     * 反序列化参数
     */
    @Override
    public BenefitParams deserializeParams(CompoundTag nbt) {
        return BenefitParams.fromNBT(nbt);
    }
}
