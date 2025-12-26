package com.jujutsuaddon.addon.api.vow;

import com.jujutsuaddon.addon.vow.ParamDefinition;
import com.jujutsuaddon.addon.vow.VowType;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.penalty.PenaltyParams;
import com.jujutsuaddon.addon.vow.validation.CheckContext;
import com.jujutsuaddon.addon.vow.validation.CheckTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 束缚誓约条件接口
 * Binding Vow Condition Interface
 */
public interface ICondition {

    ResourceLocation getId();

    Component getDisplayName();

    Component getDescription(ConditionParams params);

    float calculateWeight(ConditionParams params);

    boolean isViolated(LivingEntity owner, ConditionParams params, CheckContext context);

    /** 返回数组，匹配 TimeRangeCondition */
    CheckTrigger[] getTriggers();

    boolean requiresTickCheck();

    default void onActivate(LivingEntity owner, ConditionParams params) {}

    default void onDeactivate(LivingEntity owner, ConditionParams params) {}

    boolean isConfigurable();

    /** 使用 ParamDefinition，匹配实现类 */
    @Nullable
    ParamDefinition getConfigurableParams();

    ConditionParams createDefaultParams();

    CompoundTag serializeParams(ConditionParams params);

    ConditionParams deserializeParams(CompoundTag nbt);

    // ==================== 默认惩罚（混合方案核心） ====================

    /**
     * 获取此条件的默认惩罚ID
     * 违约时会自动执行此惩罚
     * @return 默认惩罚ID，返回null表示无默认惩罚
     */
    @Nullable
    default ResourceLocation getDefaultPenaltyId() {
        return null;
    }

    /**
     * 获取默认惩罚的参数
     */
    default PenaltyParams getDefaultPenaltyParams() {
        return new PenaltyParams();
    }

    // ==================== ★ 新增：独占签名机制 (Anti-Stacking) ★ ====================

    /**
     * 获取条件的独占签名 (Occupancy Key)
     */
    default String getOccupancyKey(ConditionParams params) {
        return getId().toString();
    }

    // ==================== ★ 新增：列表过滤 (List Filtering) ★ ====================

    /**
     * 获取此条件允许的誓约类型
     * 用于在创建界面过滤列表。
     *
     * @return DISSOLVABLE = 仅普通模式显示
     *         PERMANENT   = 仅永久模式显示
     */
    default VowType getAllowedVowType() {
        // 默认为普通模式 (DISSOLVABLE)
        return VowType.DISSOLVABLE;
    }

    /**
     * ★★★ 新增：可用性检查 ★★★
     * 检查该条件对指定实体是否可用。
     * 用于 GUI 列表过滤 和 服务端合法性校验。
     *
     * @param entity 玩家实体
     * @return true=可用(显示在列表), false=不可用(隐藏且无法创建)
     */
    default boolean isAvailable(LivingEntity entity) {
        return true; // 默认所有人都可用
    }
}
