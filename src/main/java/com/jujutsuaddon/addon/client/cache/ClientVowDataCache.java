package com.jujutsuaddon.addon.client.cache;

import com.jujutsuaddon.addon.vow.CustomBindingVow;
import com.jujutsuaddon.addon.vow.condition.ConditionEntry;

import java.util.*;

/**
 * 客户端誓约数据缓存
 * 存储从服务端同步过来的条件占用信息和惩罚状态
 */
public class ClientVowDataCache {

    /** 客户端缓存的誓约列表 */
    private static final List<CustomBindingVow> clientVows = new ArrayList<>();

    /**
     * ★ 占用条件签名 -> 拥有该条件的誓约ID
     * 对应服务端的 occupiedConditionOwners
     */
    private static final Map<String, UUID> occupiedConditionOwners = new HashMap<>();

    /** 全局惩罚结束时间戳 */
    private static long penaltyEndTime = 0L;

    // ==================== 更新方法（同步包调用） ====================

    /**
     * 统一更新所有缓存数据
     */
    public static void update(List<CustomBindingVow> vows, Map<String, UUID> occupiedConditions, long penaltyTime) {
        // 更新誓约列表
        clientVows.clear();
        if (vows != null) {
            clientVows.addAll(vows);
        }

        // ★ 更新占用条件
        occupiedConditionOwners.clear();
        if (occupiedConditions != null) {
            occupiedConditionOwners.putAll(occupiedConditions);
        }

        // 更新惩罚时间
        penaltyEndTime = penaltyTime;
    }

    /**
     * 清空缓存（登出时调用）
     */
    public static void clear() {
        clientVows.clear();
        occupiedConditionOwners.clear();
        penaltyEndTime = 0L;
    }

    // ==================== ★★★ 新增：获取所有誓约 (UI调用) ★★★ ====================
    public static List<CustomBindingVow> getAllVows() {
        return new ArrayList<>(clientVows);
    }

    /**
     * ★★★ 新增：本地更新誓约状态 (解决返回列表不刷新问题) ★★★
     * 在 VowDetailScreen 操作后调用此方法，手动修改缓存中的状态
     */
    public static void updateVowState(UUID vowId, com.jujutsuaddon.addon.vow.VowState newState) {
        for (CustomBindingVow vow : clientVows) {
            if (vow.getVowId().equals(vowId)) {
                vow.setState(newState);
                break;
            }
        }
    }
    /**
     * ★★★ 新增：计算客户端输出加成 (Mixin调用) ★★★
     * 遍历缓存中的激活誓约，累加输出提升收益
     */
    public static float calculateTotalOutputBonus() {
        float total = 0f;
        for (CustomBindingVow vow : clientVows) {
            if (vow.getState() == com.jujutsuaddon.addon.vow.VowState.ACTIVE) {
                for (com.jujutsuaddon.addon.vow.benefit.BenefitEntry entry : vow.getBenefits()) {
                    // 通过ID判断是否为输出提升，避免类加载问题
                    if (entry.getBenefit().getId().toString().equals("jujutsu_addon:output_boost")) {
                        // 计算收益 (owner传null即可，因为是纯数值计算)
                        total += entry.getBenefit().getCurrentBonus(null, entry.getParams());
                    }
                }
            }
        }
        return total;
    }
    /**
     * ★★★ 新增：计算客户端咒力加成 (MixinSorcererData 调用) ★★★
     * 用于让客户端的咒力条正确显示增加后的上限
     */
    public static float calculateTotalEnergyBonus() {
        float total = 0f;
        for (CustomBindingVow vow : clientVows) {
            if (vow.getState() == com.jujutsuaddon.addon.vow.VowState.ACTIVE) {
                for (com.jujutsuaddon.addon.vow.benefit.BenefitEntry entry : vow.getBenefits()) {
                    // 对应 EnergyBoostBenefit 的 ID
                    if (entry.getBenefit().getId().toString().equals("jujutsu_addon:energy_boost")) {
                        total += entry.getBenefit().getCurrentBonus(null, entry.getParams());
                    }
                }
            }
        }
        return total;
    }
    /**
     * ★★★ 可选：计算客户端冷却缩减 ★★★
     * 虽然 AddCooldown 是在服务端处理的，但为了以后可能的 Tooltip 显示，建议加上
     */
    public static float calculateTotalCooldownReduction() {
        float total = 0f;
        for (CustomBindingVow vow : clientVows) {
            if (vow.getState() == com.jujutsuaddon.addon.vow.VowState.ACTIVE) {
                for (com.jujutsuaddon.addon.vow.benefit.BenefitEntry entry : vow.getBenefits()) {
                    // 对应 CooldownReductionBenefit 的 ID
                    if (entry.getBenefit().getId().toString().equals("jujutsu_addon:cooldown_reduction")) {
                        total += entry.getBenefit().getCurrentBonus(null, entry.getParams());
                    }
                }
            }
        }
        // 上限 80% 缩减，防止负数
        return Math.min(total, 0.8f);
    }

    // ==================== 惩罚查询 ====================

    public static boolean isUnderPenalty() {
        return System.currentTimeMillis() < penaltyEndTime;
    }

    public static long getPenaltySecondsLeft() {
        long diff = penaltyEndTime - System.currentTimeMillis();
        return diff > 0 ? diff / 1000 : 0;
    }

    // ==================== ★ 占用查询方法 (UI逻辑核心) ====================

    /**
     * 检查誓约是否包含被【其他誓约】占用的条件
     * 如果条件被自己占用，返回false（可以激活）
     *
     * 用于UI判断是否禁用激活按钮
     *
     * @param vow 要检查的誓约
     * @return 如果包含被其他誓约占用的条件返回true (不可激活)
     */
    public static boolean containsOccupiedCondition(CustomBindingVow vow) {
        if (vow == null) return false;
        UUID vowId = vow.getVowId();

        for (ConditionEntry entry : vow.getConditions()) {
            // 在客户端计算 Key，逻辑与服务端一致
            String key = entry.getCondition().getOccupancyKey(entry.getParams());

            if (key != null) {
                UUID owner = occupiedConditionOwners.get(key);
                // ★ 关键：只有被其他誓约占用才返回true
                if (owner != null && !owner.equals(vowId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取誓约中被【其他誓约】占用的条件数量
     * (用于UI显示提示，比如 "1个条件冲突")
     */
    public static int countOccupiedConditions(CustomBindingVow vow) {
        if (vow == null) return 0;
        UUID vowId = vow.getVowId();

        int count = 0;
        for (ConditionEntry entry : vow.getConditions()) {
            String key = entry.getCondition().getOccupancyKey(entry.getParams());
            if (key != null) {
                UUID owner = occupiedConditionOwners.get(key);
                if (owner != null && !owner.equals(vowId)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 检查特定Key是否被占用（不考虑拥有者）
     */
    public static boolean isKeyOccupied(String key) {
        return occupiedConditionOwners.containsKey(key);
    }

    /**
     * 获取占用特定Key的誓约ID
     */
    public static UUID getKeyOwner(String key) {
        return occupiedConditionOwners.get(key);
    }
}
