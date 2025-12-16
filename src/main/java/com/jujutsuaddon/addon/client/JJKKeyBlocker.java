package com.jujutsuaddon.addon.client;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import com.jujutsuaddon.addon.client.util.FeatureToggleManager;
import net.minecraft.client.KeyMapping;
import radon.jujutsu_kaisen.client.JJKKeys;

import java.util.Set;

/**
 * JJK 按键屏蔽器
 *
 * 功能：判断一个按键是否应该被屏蔽
 * 被 KeyMappingMixin 调用，用于决定是否拦截 JJK 的按键
 *
 * @author JujutsuAddon
 */
public class JJKKeyBlocker {

    /**
     * 需要屏蔽的 JJK 按键集合
     * 使用 Set 是因为查找效率高（O(1)）
     * 使用懒加载是为了避免类加载顺序问题（JJKKeys 可能还没初始化）
     */
    private static Set<KeyMapping> BLOCKED_KEYS;

    /**
     * 判断一个按键是否应该被屏蔽
     *
     * @param key 要检查的按键映射
     * @return true = 应该屏蔽（让 isDown/consumeClick 返回 false）
     *         false = 不屏蔽（让原方法正常执行）
     */
    public static boolean shouldBlock(KeyMapping key) {
        // ★★★ 功能开关检查 ★★★
        if (!FeatureToggleManager.isKeybindSystemEnabled()) {
            return false;
        }

        // ===== 前置条件检查 =====
        // 1. 配置未加载时不屏蔽（游戏刚启动时可能发生）
        if (AddonClientConfig.CLIENT == null) {
            return false;
        }

        // 2. 技能栏功能未启用时不屏蔽
        //    用户可能想用原版 JJK 的按键系统
        if (!AddonClientConfig.CLIENT.enableSkillBar.get()) {
            return false;
        }

        // 3. 屏蔽选项未启用时不屏蔽
        //    用户可以在配置中关闭屏蔽功能
        if (!AddonClientConfig.CLIENT.blockJJKKeys.get()) {
            return false;
        }

        // 4. 技能键被临时禁用时不屏蔽
        //    用户按了 \ 键临时关闭技能栏，此时应该恢复 JJK 原版按键
        if (!ClientEvents.areSkillKeysEnabled()) {
            return false;
        }

        // ===== 检查是否是 JJK 的按键 =====
        return isJJKKey(key);
    }

    /**
     * 检查一个按键是否是 JJK 的按键
     *
     * @param key 要检查的按键
     * @return true = 是 JJK 按键，需要屏蔽
     */
    private static boolean isJJKKey(KeyMapping key) {
        // 懒加载：第一次调用时才初始化
        // 这样可以确保 JJKKeys 类已经被加载
        if (BLOCKED_KEYS == null) {
            BLOCKED_KEYS = Set.of(
                    // ========== 技能相关 ==========
                    JJKKeys.ACTIVATE_ABILITY,              // R - 触发当前选中的技能
                    JJKKeys.ACTIVATE_J2TSU,                // T - 触发术式
                    // JJKKeys.ACTIVATE_RCT_OR_HEAL,       // X - 反转术式/治疗（可选保留）
                    //JJKKeys.ACTIVATE_CURSED_ENERGY_SHIELD, // Z - 咒力护盾

                    // ========== 菜单相关 ==========
                    JJKKeys.SHOW_ABILITY_MENU,             // C - 技能轮盘菜单
                    JJKKeys.SHOW_DOMAIN_MENU,              // V - 领域轮盘菜单
                    JJKKeys.ACTIVATE_MELEE_MENU,           // LAlt - 近战菜单
                    JJKKeys.ACTIVATE_J2TSU_MENU            // B - 术式菜单

                    // ========== 移动相关（默认不屏蔽）==========
                    // JJKKeys.DASH,                       // G - 冲刺
                    // JJKKeys.QUICKDASH                   // H - 快速冲刺

                    // ========== 其他功能（默认不屏蔽）==========
                    // JJKKeys.OPEN_INVENTORY_CURSE,       // N - 咒灵背包
                    // JJKKeys.OPEN_JUJUTSU_MENU,          // P - 咒术菜单
                    // JJKKeys.INCREASE_OUTPUT,            // UP - 增加输出
                    // JJKKeys.DECREASE_OUTPUT             // DOWN - 减少输出
            );
        }

        return BLOCKED_KEYS.contains(key);
    }
}
