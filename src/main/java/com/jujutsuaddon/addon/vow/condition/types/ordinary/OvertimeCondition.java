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
 * 加班束缚条件
 * Overtime Binding Condition
 *
 * 灵感来源：原作中的「加班」束缚誓约
 *
 * 机制说明：
 * - 这是一个"收益调节型"条件，不会导致违约
 * - 在"正常时间"（如白天），收益效果反转为负面
 * - 在"加班时间"（如夜晚），收益效果正常生效
 * - 例如：选择+20%咒力输出收益，白天会变成-20%，晚上才是+20%
 *
 * 特点：
 * - 简单易懂的束缚机制
 * - 权重上限较低（因为不会导致违约/惩罚）
 * - 适合入门玩家
 *
 * 参数说明：
 * - overtimeStart: 加班开始时间（0-23）
 * - overtimeEnd: 加班结束时间（0-23）
 */
public class OvertimeCondition implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation("jujutsu_addon", "overtime");

    /** 参数键：加班开始时间 */
    public static final String PARAM_OVERTIME_START = "overtimeStart";

    /** 参数键：加班结束时间 */
    public static final String PARAM_OVERTIME_END = "overtimeEnd";

    /** 此条件的最大权重上限（因为机制简单） */
    public static final float MAX_WEIGHT = 3.0f;

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("condition.jujutsu_addon.overtime");
    }

    @Override
    public Component getDescription(ConditionParams params) {
        int start = params.getInt(PARAM_OVERTIME_START, 18);
        int end = params.getInt(PARAM_OVERTIME_END, 6);

        return Component.translatable("condition.jujutsu_addon.overtime.desc",
                formatHour(start), formatHour(end));
    }

    /**
     * 获取详细机制说明（用于GUI显示）
     */
    public Component getMechanicDescription(ConditionParams params) {
        int start = params.getInt(PARAM_OVERTIME_START, 18);
        int end = params.getInt(PARAM_OVERTIME_END, 6);
        int normalHours = calculateNormalHours(start, end);

        return Component.translatable("condition.jujutsu_addon.overtime.mechanic",
                formatHour(start), formatHour(end), normalHours);
    }

    private String formatHour(int hour) {
        return String.format("%02d:00", hour);
    }

    /**
     * 计算"正常时间"（非加班时间）的小时数
     */
    private int calculateNormalHours(int overtimeStart, int overtimeEnd) {
        int overtimeHours;
        if (overtimeStart <= overtimeEnd) {
            overtimeHours = overtimeEnd - overtimeStart;
        } else {
            overtimeHours = (24 - overtimeStart) + overtimeEnd;
        }
        return 24 - overtimeHours;
    }

    /**
     * 计算权重
     * 正常时间越长（惩罚时间越长），权重越高
     * 但有上限，因为不会导致违约
     */
    @Override
    public float calculateWeight(ConditionParams params) {
        int start = params.getInt(PARAM_OVERTIME_START, 18);
        int end = params.getInt(PARAM_OVERTIME_END, 6);

        int normalHours = calculateNormalHours(start, end);

        // 基础权重：每小时约0.15权重，12小时约1.8权重
        float baseWeight = normalHours * 0.15f;

        // 应用上限
        return Math.min(baseWeight, MAX_WEIGHT);
    }

    /**
     * 检查当前是否在加班时间
     */
    public boolean isOvertimeNow(LivingEntity owner, ConditionParams params) {
        int start = params.getInt(PARAM_OVERTIME_START, 18);
        int end = params.getInt(PARAM_OVERTIME_END, 6);

        // 获取MC时间并转换为小时
        long dayTime = owner.level().getDayTime() % 24000;
        int currentHour = (int) ((dayTime / 1000 + 6) % 24);

        // 判断是否在加班时间内
        if (start <= end) {
            return currentHour >= start && currentHour < end;
        } else {
            return currentHour >= start || currentHour < end;
        }
    }

    /**
     * 获取当前的效果倍率
     * @return 1.0 = 正常效果, -1.0 = 反转效果
     */
    public float getCurrentMultiplier(LivingEntity owner, ConditionParams params) {
        return isOvertimeNow(owner, params) ? 1.0f : -1.0f;
    }

    /**
     * 此条件不会导致违约
     * 它通过调节收益效果来实现限制
     */
    @Override
    public boolean isViolated(LivingEntity owner, ConditionParams params, CheckContext context) {
        // 加班束缚不会导致违约，只是调节收益
        return false;
    }

    @Override
    public boolean requiresTickCheck() {
        return true;  // 需要实时检查以调整效果
    }

    @Override
    public CheckTrigger[] getTriggers() {
        return new CheckTrigger[] { CheckTrigger.SECOND };
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public @Nullable ParamDefinition getConfigurableParams() {
        return new ParamDefinition()
                .addInt(PARAM_OVERTIME_START,
                        Component.translatable("condition.jujutsu_addon.overtime.param.start"),
                        0, 23, 18)
                .addInt(PARAM_OVERTIME_END,
                        Component.translatable("condition.jujutsu_addon.overtime.param.end"),
                        0, 23, 6);
    }

    @Override
    public ConditionParams createDefaultParams() {
        return new ConditionParams()
                .setInt(PARAM_OVERTIME_START, 18)  // 下午6点
                .setInt(PARAM_OVERTIME_END, 6);    // 早上6点
    }

    @Override
    public CompoundTag serializeParams(ConditionParams params) {
        return params.serializeNBT();
    }

    @Override
    public ConditionParams deserializeParams(CompoundTag nbt) {
        return ConditionParams.fromNBT(nbt);
    }

    /**
     * 获取条件类型标签（用于UI分类）
     */
    public ConditionType getConditionType() {
        return ConditionType.MODIFIER;
    }

    /**
     * 条件类型枚举
     */
    public enum ConditionType {
        /** 违约型：违反会触发惩罚 */
        VIOLATION,
        /** 调节型：动态调整收益效果 */
        MODIFIER
    }
}
