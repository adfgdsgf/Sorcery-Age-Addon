package com.jujutsuaddon.addon.api.vow;

import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.penalty.PenaltyParams;
import com.jujutsuaddon.addon.vow.penalty.PenaltySeverity;
import com.jujutsuaddon.addon.vow.penalty.PenaltyType;
import com.jujutsuaddon.addon.vow.penalty.ViolationContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;

/**
 * 束缚誓约惩罚接口
 * Binding Vow Penalty Interface
 */
public interface IPenalty {

    ResourceLocation getId();

    Component getDisplayName();

    Component getDescription(PenaltyParams params);

    PenaltyType getType();

    PenaltySeverity getSeverity();

    void execute(LivingEntity owner, ISorcererData data, PenaltyParams params, ViolationContext context);

    /** 获取持续时间（tick），仅临时惩罚使用 */
    default int getDuration(PenaltyParams params) { return 0; }

    /** 移除惩罚效果 */
    default void remove(LivingEntity owner, ISorcererData data, PenaltyParams params) {}

    /** 是否可被移除（永久惩罚返回false） */
    default boolean isRemovable() { return getType() != PenaltyType.PERMANENT; }

    boolean isConfigurable();

    @Nullable
    ParamDefinition getConfigurableParams();

    PenaltyParams createDefaultParams();

    CompoundTag serializeParams(PenaltyParams params);

    PenaltyParams deserializeParams(CompoundTag nbt);

    // ==================== 权重加成（混合方案核心） ====================

    /**
     * 获取选择此惩罚提供的额外权重加成
     * 惩罚越严重，加成越高
     * 玩家选择额外惩罚 → 获得更多权重 → 可以兑换更强收益
     */
    default float getBonusWeight(PenaltyParams params) {
        return switch (getSeverity()) {
            case MINOR -> 0.3f;
            case MODERATE -> 0.6f;
            case SEVERE -> 1.0f;
            case FATAL -> 1.5f;
        };
    }

    /**
     * 是否可作为额外惩罚选择
     * 某些惩罚可能只能作为默认惩罚，不允许玩家主动选择
     */
    default boolean isSelectable() {
        return true;
    }
}
