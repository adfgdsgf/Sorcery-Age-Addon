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
import radon.jujutsu_kaisen.chant.ChantHandler;
// ★★★ 导入主模组的配置类 ★★★
import radon.jujutsu_kaisen.config.ConfigHolder;

/**
 * 咏唱必须条件
 * Chant Required Condition
 *
 * 1. 检查咏唱次数 (Count)。
 * 2. 动态读取主模组配置的最大咏唱数，作为滑条上限。
 */
public class ChantRequiredCondition implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "chant_required");
    public static final String PARAM_MIN_COUNT = "minCount";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("condition.jujutsu_addon.chant_required");
    }

    @Override
    public Component getDescription(ConditionParams params) {
        int count = params.getInt(PARAM_MIN_COUNT, 1);
        return Component.translatable("condition.jujutsu_addon.chant_required.desc", count);
    }

    @Override
    public float calculateWeight(ConditionParams params) {
        int count = params.getInt(PARAM_MIN_COUNT, 1);
        return count * 5.0f;
    }

    @Override
    public boolean isViolated(LivingEntity owner, ConditionParams params, CheckContext context) {
        if (context.getTrigger() != CheckTrigger.ABILITY_ATTEMPT) return false;
        if (context.getAbility() == null) return false;

        // 获取当前咏唱次数
        int currentChantCount = (int) ChantHandler.getChant(owner, context.getAbility());
        // 获取要求次数
        int requiredCount = params.getInt(PARAM_MIN_COUNT, 1);

        return currentChantCount < requiredCount;
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
        // ★★★ 动态获取主模组配置的最大值 ★★★
        int maxLimit = 5; // 默认值，防止获取失败
        try {
            // 从 ConfigHolder.SERVER 中获取 maximumChantCount
            maxLimit = ConfigHolder.SERVER.maximumChantCount.get();
        } catch (Exception e) {
            // 如果配置还没加载，保持默认值 5
        }

        return new ParamDefinition()
                .addInt(PARAM_MIN_COUNT,
                        Component.translatable("condition.jujutsu_addon.chant_required.param.count"),
                        1, maxLimit, 1); // 上限现在是动态的了
    }

    @Override
    public ConditionParams createDefaultParams() {
        return new ConditionParams().setInt(PARAM_MIN_COUNT, 1);
    }

    @Override
    public CompoundTag serializeParams(ConditionParams params) { return params.serializeNBT(); }

    @Override
    public ConditionParams deserializeParams(CompoundTag nbt) { return ConditionParams.fromNBT(nbt); }
}
