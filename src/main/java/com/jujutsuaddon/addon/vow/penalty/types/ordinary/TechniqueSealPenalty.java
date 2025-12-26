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
 * 术式封印惩罚
 * Technique Seal Penalty
 *
 * 违约后一段时间内无法使用咒术。
 * 永久誓约的封印时间更长。
 */
public class TechniqueSealPenalty implements IPenalty {

    /** 惩罚唯一ID */
    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "technique_seal");

    /** 参数键：封印持续时间（秒） */
    public static final String PARAM_DURATION_SECONDS = "durationSeconds";

    /** 参数键：是否封印所有术式 */
    public static final String PARAM_SEAL_ALL = "sealAll";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("penalty.jujutsu_addon.technique_seal");
    }

    @Override
    public Component getDescription(PenaltyParams params) {
        int seconds = params.getInt(PARAM_DURATION_SECONDS, 30);
        boolean sealAll = params.getBoolean(PARAM_SEAL_ALL, true);
        String key = sealAll
                ? "penalty.jujutsu_addon.technique_seal.desc.all"
                : "penalty.jujutsu_addon.technique_seal.desc.partial";
        return Component.translatable(key, seconds);
    }

    @Override
    public PenaltyType getType() { return PenaltyType.TEMPORARY; }

    @Override
    public PenaltySeverity getSeverity() { return PenaltySeverity.SEVERE; }

    @Override
    public void execute(LivingEntity owner, ISorcererData data, PenaltyParams params, ViolationContext context) {
        int durationSeconds = params.getInt(PARAM_DURATION_SECONDS, 30);

        // 永久誓约封印时间更长
        if (context.isPermanentVow()) {
            durationSeconds *= 3;
        }

        // TODO: 通过VowCapability设置封印状态
        // 在技能使用检查时读取封印状态并阻止使用
    }

    @Override
    public int getDuration(PenaltyParams params) {
        return params.getInt(PARAM_DURATION_SECONDS, 30) * 20; // 转换为tick
    }

    @Override
    public void remove(LivingEntity owner, ISorcererData data, PenaltyParams params) {
        // 清除封印状态
    }

    @Override
    public boolean isConfigurable() { return true; }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return new ParamDefinition()
                .addInt(PARAM_DURATION_SECONDS,
                        Component.translatable("penalty.jujutsu_addon.technique_seal.param.duration"),
                        5, 300, 30)
                .addBoolean(PARAM_SEAL_ALL,
                        Component.translatable("penalty.jujutsu_addon.technique_seal.param.seal_all"),
                        true);
    }

    @Override
    public PenaltyParams createDefaultParams() {
        return new PenaltyParams()
                .setInt(PARAM_DURATION_SECONDS, 30)
                .setBoolean(PARAM_SEAL_ALL, true);
    }

    @Override
    public CompoundTag serializeParams(PenaltyParams params) { return params.serializeNBT(); }

    @Override
    public PenaltyParams deserializeParams(CompoundTag nbt) { return PenaltyParams.fromNBT(nbt); }
}
