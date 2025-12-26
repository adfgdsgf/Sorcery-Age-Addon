package com.jujutsuaddon.addon.api.vow;

import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.benefit.BenefitCategory;
import com.jujutsuaddon.addon.vow.benefit.BenefitParams;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;

/**
 * 束缚誓约收益接口
 * Binding Vow Benefit Interface
 */
public interface IBenefit {

    ResourceLocation getId();

    Component getDisplayName();

    Component getDescription(BenefitParams params);

    float getRequiredWeight(BenefitParams params);

    boolean isScalable();

    float getMaxBonus();

    BenefitCategory getCategory();

    void apply(LivingEntity owner, ISorcererData data, BenefitParams params);

    void remove(LivingEntity owner, ISorcererData data, BenefitParams params);

    /** @param owner 可为null（用于纯计算） */
    float getCurrentBonus(@Nullable LivingEntity owner, BenefitParams params);

    boolean isConfigurable();

    /** 使用 ParamDefinition，匹配实现类 */
    @Nullable
    ParamDefinition getConfigurableParams();

    BenefitParams createDefaultParams();

    CompoundTag serializeParams(BenefitParams params);

    BenefitParams deserializeParams(CompoundTag nbt);

    /**
     * 获取此收益允许的誓约类型
     * 用于区分 普通誓约(数值加成) 和 永久誓约(机制改变)
     *
     * @return DISSOLVABLE = 普通模式显示
     *         PERMANENT   = 永久模式显示
     */
    default VowType getAllowedVowType() {
        return VowType.DISSOLVABLE;
    }

    /**
     * ★★★ 新增：可用性检查 ★★★
     * 检查该收益对指定实体是否可用。
     * 用于 GUI 列表过滤 和 服务端合法性校验。
     *
     * @param entity 玩家实体
     * @return true=可用(显示在列表), false=不可用(隐藏且无法创建)
     */
    default boolean isAvailable(LivingEntity entity) {
        return true; // 默认所有人都可用
    }
}
