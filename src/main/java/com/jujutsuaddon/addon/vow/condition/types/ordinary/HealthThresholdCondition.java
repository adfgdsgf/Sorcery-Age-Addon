package com.jujutsuaddon.addon.vow.condition.types.ordinary;

import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.validation.CheckContext;
import com.jujutsuaddon.addon.vow.validation.CheckTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 血量阈值条件
 * Health Threshold Condition
 *
 * 限制玩家只能在血量高于/低于某个百分比时使用咒术。
 * 典型用例：「背水一战」- 只有血量低于30%时才能使用
 *
 * 权重计算：阈值越极端，权重越高
 */
public class HealthThresholdCondition implements ICondition {

    /** 条件唯一ID */
    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "health_threshold");

    /** 参数键：血量阈值（1-99 整数百分比） */
    public static final String PARAM_THRESHOLD_PERCENT = "thresholdPercent";

    /** 参数键：是否要求低于阈值（true=低于才能用，false=高于才能用） */
    public static final String PARAM_BELOW = "below";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("condition.jujutsu_addon.health_threshold");
    }

    @Override
    public Component getDescription(ConditionParams params) {
        // 直接读取整数，不需要四舍五入
        int percent = params.getInt(PARAM_THRESHOLD_PERCENT, 50);
        boolean below = params.getBoolean(PARAM_BELOW, true);

        // 选择对应的描述键
        String key = below
                ? "condition.jujutsu_addon.health_threshold.desc.below"   // 血量低于X%时才能使用
                : "condition.jujutsu_addon.health_threshold.desc.above";  // 血量高于X%时才能使用

        return Component.translatable(key, percent);
    }

    @Override
    public float calculateWeight(ConditionParams params) {
        int percent = params.getInt(PARAM_THRESHOLD_PERCENT, 50);
        boolean below = params.getBoolean(PARAM_BELOW, true);

        // 转换为 0.0 - 0.99
        float ratio = percent / 100.0f;

        float difficulty;
        float multiplier;

        // 难度与倍率计算
        if (below) {
            // 低于模式：阈值越低越难 (低于10%比低于90%难)
            // 比如 10% -> 难度 0.9
            difficulty = 1.0f - ratio;
            // 低于阈值风险更高，给予 1.5 倍点数
            multiplier = 1.67f;
        } else {
            // 高于模式：阈值越高越难 (高于90%比高于10%难)
            // 比如 90% -> 难度 0.9
            difficulty = ratio;
            // 高于阈值相对安全，给予 1.0 倍点数
            multiplier = 1.25f;
        }

        // 基础分 100 * 难度系数 * 倍率
        return difficulty * 50.0f * multiplier;
    }

    @Override
    public boolean isViolated(LivingEntity owner, ConditionParams params, CheckContext context) {
        // 读取参数 (整数转小数)
        int percent = params.getInt(PARAM_THRESHOLD_PERCENT, 50);
        float threshold = percent / 100.0f;

        boolean below = params.getBoolean(PARAM_BELOW, true);

        // 计算当前血量百分比
        float currentRatio = owner.getHealth() / owner.getMaxHealth();

        // below=true: 要求血量低于阈值，血量高于阈值就违反
        // below=false: 要求血量高于阈值，血量低于阈值就违反
        return below ? (currentRatio >= threshold) : (currentRatio <= threshold);
    }

    @Override
    public CheckTrigger[] getTriggers() {
        return new CheckTrigger[] { CheckTrigger.ABILITY_ATTEMPT };
    }

    @Override
    public boolean requiresTickCheck() { return false; }

    @Override
    public boolean isConfigurable() { return true; }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        // 使用 addInt 避免浮点数显示问题
        return new ParamDefinition()
                .addInt(PARAM_THRESHOLD_PERCENT,
                        Component.translatable("condition.jujutsu_addon.health_threshold.param.threshold"),
                        1, 99, 50)
                .addBoolean(PARAM_BELOW,
                        Component.translatable("condition.jujutsu_addon.health_threshold.param.below"),
                        true);
    }

    @Override
    public ConditionParams createDefaultParams() {
        return new ConditionParams()
                .setInt(PARAM_THRESHOLD_PERCENT, 50)
                .setBoolean(PARAM_BELOW, true);
    }

    @Override
    public CompoundTag serializeParams(ConditionParams params) { return params.serializeNBT(); }

    @Override
    public ConditionParams deserializeParams(CompoundTag nbt) { return ConditionParams.fromNBT(nbt); }
}
