package com.jujutsuaddon.addon.vow.penalty.types.ordinary;

import com.jujutsuaddon.addon.api.vow.IPenalty;
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
 * 术式损坏惩罚（永久性）
 * Technique Damage Penalty (Permanent)
 *
 * 违反永久誓约可能导致术式永久损坏。
 * 这是最严重的惩罚类型，无法恢复。
 */
public class TechniqueDamagePenalty implements IPenalty {

    /** 惩罚唯一ID */
    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "technique_damage");

    /** 参数键：术式效率降低百分比 */
    public static final String PARAM_DAMAGE_PERCENT = "damagePercent";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("penalty.jujutsu_addon.technique_damage");
    }

    @Override
    public Component getDescription(PenaltyParams params) {
        int percent = Math.round(params.getFloat(PARAM_DAMAGE_PERCENT, 0.1f) * 100);
        return Component.translatable("penalty.jujutsu_addon.technique_damage.desc", percent);
    }

    @Override
    public PenaltyType getType() { return PenaltyType.PERMANENT; }

    @Override
    public PenaltySeverity getSeverity() { return PenaltySeverity.FATAL; }

    @Override
    public void execute(LivingEntity owner, ISorcererData data, PenaltyParams params, ViolationContext context) {
        float damagePercent = params.getFloat(PARAM_DAMAGE_PERCENT, 0.1f);
        // TODO: 实现永久性术式效率降低
        // 可以存储在VowData中，在计算输出时读取并应用
    }

    @Override
    public boolean isRemovable() { return false; }

    @Override
    public boolean isConfigurable() { return true; }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return new ParamDefinition()
                .addFloat(PARAM_DAMAGE_PERCENT,
                        Component.translatable("penalty.jujutsu_addon.technique_damage.param.percent"),
                        0.05f, 0.5f, 0.1f);
    }

    @Override
    public PenaltyParams createDefaultParams() {
        return new PenaltyParams().setFloat(PARAM_DAMAGE_PERCENT, 0.1f);
    }

    @Override
    public CompoundTag serializeParams(PenaltyParams params) { return params.serializeNBT(); }

    @Override
    public PenaltyParams deserializeParams(CompoundTag nbt) { return PenaltyParams.fromNBT(nbt); }
}
