package com.jujutsuaddon.addon.capability.vow;

import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.VowState;
import com.jujutsuaddon.addon.vow.condition.ConditionParams;
import com.jujutsuaddon.addon.vow.manager.DeactivateReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 誓约数据接口
 * 定义玩家誓约数据的所有操作
 */
public interface IVowData {

    // ★★★ 新增：初始化方法 ★★★
    // 必须在这里定义，VowData 里的 @Override 才能生效
    void init(LivingEntity owner);

    // ==================== 誓约管理 ====================

    void addVow(CustomBindingVow vow);

    void removeVow(UUID vowId);

    @Nullable CustomBindingVow getVow(UUID vowId);

    List<CustomBindingVow> getAllVows();

    List<CustomBindingVow> getActiveVows();

    List<CustomBindingVow> getVowsByState(VowState state);

    // ==================== ★ 新增：查询激活条件的参数 (修复报错) ★ ====================

    /**
     * 获取当前激活的某个条件的参数
     * 如果有多个誓约包含该条件，返回第一个找到的参数
     * @param conditionId 条件ID
     * @return 参数，如果没有激活该条件则返回 null
     */
    @Nullable
    ConditionParams getActiveConditionParams(ResourceLocation conditionId);

    // ==================== ★ 新增：消耗一次性收益 ★ ====================

    /**
     * 尝试消耗提供指定收益的永久誓约。
     * 1. 如果配置开启了“永久收益模式”，则无事发生（无限使用）。
     * 2. 如果配置关闭（默认），则将该誓约状态改为 EXHAUSTED（已耗尽），并释放条件占用。
     * @param benefitId 收益ID
     */
    void consumeOneTimeBenefit(ResourceLocation benefitId);

    // ==================== 激活/停用 ====================

    boolean activateVow(UUID vowId, LivingEntity owner);

    void deactivateVow(UUID vowId, LivingEntity owner, DeactivateReason reason);

    void deactivateAllVows(LivingEntity owner, DeactivateReason reason);

    // ==================== 收益查询 ====================

    float getTotalOutputBonus();

    float getTotalEnergyBonus();

    default float getTotalCooldownReduction() {
        return 0f;
    }

    // ==================== ★ 条件独占管理 (Anti-Stacking) ★ ====================

    /**
     * 检查誓约是否可以激活
     * 只有当誓约中的条件被【其他誓约】占用时才返回 false
     *
     * @param vow 要检查的誓约
     * @return 如果可以激活返回true
     */
    boolean canActivateVow(CustomBindingVow vow);

    /**
     * 获取已被占用的条件签名映射
     * Key = Condition Occupancy Key (例如 "jujutsu:time_range" 或 "jujutsu:ability_ban:fire_arrow")
     * Value = 占用该条件的誓约 ID
     */
    Map<String, UUID> getOccupiedConditionOwners();

    /**
     * 清除所有占用记录（管理员/调试用）
     */
    void clearOccupiedConditions();

    // ==================== 全局惩罚管理 ====================

    /**
     * 获取全局惩罚结束的时间戳（系统时间毫秒）
     * 在此时间之前，无法激活任何誓约
     */
    long getPenaltyEndTime();

    /**
     * 设置全局惩罚结束时间
     */
    void setPenaltyEndTime(long timestamp);

    /**
     * 检查当前是否处于惩罚期
     */
    default boolean isUnderPenalty() {
        return System.currentTimeMillis() < getPenaltyEndTime();
    }

    // ==================== 脏标记 ====================

    void markDirty();

    boolean isDirty();

    void clearDirty();

    // ==================== 序列化 ====================

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag nbt);

}
