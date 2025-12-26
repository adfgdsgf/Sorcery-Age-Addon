package com.jujutsuaddon.addon.vow.condition.types.ordinary;

import com.jujutsuaddon.addon.api.vow.ICondition;
import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.calculation.VowCalculator;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.penalty.PenaltyParams;
import com.jujutsuaddon.addon.vow.penalty.types.ordinary.CursedEnergyBacklashPenalty;
import com.jujutsuaddon.addon.vow.validation.CheckContext;
import com.jujutsuaddon.addon.vow.validation.CheckTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 时间段限制条件
 * Time Range Restriction Condition
 *
 * 只在玩家尝试使用咒术时检查！
 * 如果在禁止时间段内使用咒术 → 违约
 * 只是处于禁止时间段但不使用咒术 → 不违约
 */
public class TimeRangeCondition implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "time_range");

    public static final String PARAM_START_HOUR = "startHour";
    public static final String PARAM_END_HOUR = "endHour";
    public static final String PARAM_INVERT = "invert";

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("condition.jujutsu_addon.time_range");
    }

    @Override
    public Component getDescription(ConditionParams params) {
        int startHour = params.getInt(PARAM_START_HOUR, 18);
        int endHour = params.getInt(PARAM_END_HOUR, 6);
        boolean invert = params.getBoolean(PARAM_INVERT, true);

        String key = invert
                ? "condition.jujutsu_addon.time_range.desc.outside"
                : "condition.jujutsu_addon.time_range.desc.inside";

        return Component.translatable(key, formatHour(startHour), formatHour(endHour));
    }

    public Component getMechanicDescription(ConditionParams params) {
        return Component.translatable("condition.jujutsu_addon.time_range.mechanic");
    }

    private String formatHour(int hour) {
        return String.format("%02d:00", hour);
    }

    @Override
    public float calculateWeight(ConditionParams params) {
        int startHour = params.getInt(PARAM_START_HOUR, 18);
        int endHour = params.getInt(PARAM_END_HOUR, 6);

        int restrictedHours;
        if (startHour <= endHour) {
            restrictedHours = endHour - startHour;
        } else {
            restrictedHours = (24 - startHour) + endHour;
        }

        boolean invert = params.getBoolean(PARAM_INVERT, true);
        if (!invert) {
            restrictedHours = 24 - restrictedHours;
        }

        return VowCalculator.calculateTimeWeight(restrictedHours);
    }

    @Override
    public boolean isViolated(LivingEntity owner, ConditionParams params, CheckContext context) {
        // ★ 关键：只在使用术式时才检查！
        if (context.getTrigger() != CheckTrigger.ABILITY_ATTEMPT) {
            return false; // 不是使用术式，不判定违约
        }

        // 必须有技能信息才能判定（防止误判）
        if (context.getAbility() == null) {
            return false;
        }

        int startHour = params.getInt(PARAM_START_HOUR, 18);
        int endHour = params.getInt(PARAM_END_HOUR, 6);
        boolean invert = params.getBoolean(PARAM_INVERT, true);

        long dayTime = owner.level().getDayTime() % 24000;
        int currentHour = (int) ((dayTime / 1000 + 6) % 24);

        boolean inRange;
        if (startHour <= endHour) {
            inRange = currentHour >= startHour && currentHour < endHour;
        } else {
            inRange = currentHour >= startHour || currentHour < endHour;
        }

        // invert=true: 禁止在时间段内使用 → 在时间段内使用就违约
        // invert=false: 禁止在时间段外使用 → 在时间段外使用就违约
        boolean isInForbiddenTime = invert ? inRange : !inRange;

        return isInForbiddenTime; // 在禁止时间使用术式 = 违约
    }

    @Override
    public boolean requiresTickCheck() {
        return false; // 不需要每tick检查！
    }

    @Override
    public CheckTrigger[] getTriggers() {
        // ★ 只在使用术式时触发检查！
        return new CheckTrigger[] {
                CheckTrigger.ABILITY_ATTEMPT
        };
    }

    // ==================== 默认惩罚 ====================

    @Override
    @Nullable
    public ResourceLocation getDefaultPenaltyId() {
        // 时间段限制的默认惩罚：咒力反噬
        return CursedEnergyBacklashPenalty.ID;
    }

    @Override
    public PenaltyParams getDefaultPenaltyParams() {
        // 默认15%最大生命值伤害
        return new PenaltyParams()
                .setFloat(CursedEnergyBacklashPenalty.PARAM_DAMAGE_PERCENT, 0.15f)
                .setBoolean(CursedEnergyBacklashPenalty.PARAM_IGNORE_ARMOR, false);
    }

    // ==================== 配置 ====================

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return new ParamDefinition()
                .addInt(PARAM_START_HOUR,
                        Component.translatable("condition.jujutsu_addon.time_range.param.start"),
                        0, 23, 18)
                .addInt(PARAM_END_HOUR,
                        Component.translatable("condition.jujutsu_addon.time_range.param.end"),
                        0, 23, 6)
                .addBoolean(PARAM_INVERT,
                        Component.translatable("condition.jujutsu_addon.time_range.param.invert"),
                        true);
    }

    @Override
    public ConditionParams createDefaultParams() {
        return new ConditionParams()
                .setInt(PARAM_START_HOUR, 18)
                .setInt(PARAM_END_HOUR, 6)
                .setBoolean(PARAM_INVERT, true);
    }

    @Override
    public CompoundTag serializeParams(ConditionParams params) {
        return params.serializeNBT();
    }

    @Override
    public ConditionParams deserializeParams(CompoundTag nbt) {
        return ConditionParams.fromNBT(nbt);
    }
}
