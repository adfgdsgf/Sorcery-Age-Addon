package com.jujutsuaddon.addon.client.gui.util;

/**
 * 誓约GUI颜色常量
 * 统一管理所有界面颜色，杜绝硬编码
 */
public final class VowGuiColors {

    private VowGuiColors() {}

    // ==================== 状态颜色 (文字/图标) ====================
    public static final int STATE_ACTIVE = 0xFF55FF55;     // 亮绿
    public static final int STATE_INACTIVE = 0xFFFFFF55;   // 亮黄
    public static final int STATE_VIOLATED = 0xFFFF5555;   // 亮红
    public static final int STATE_DISSOLVED = 0xFF888888;  // 灰色
    public static final int STATE_EXPIRED = 0xFF666666;    // 深灰
    public static final int STATE_EXHAUSTED = 0xFF555555;  // ★ 新增：已耗尽状态 (深灰/黑色系)

    // ==================== 类型颜色 ====================
    public static final int TYPE_PERMANENT = 0xFFFF6666;   // 红色系
    public static final int TYPE_DISSOLVABLE = 0xFF66AAFF; // 蓝色系

    // ==================== 权重条/统计颜色 ====================
    public static final int WEIGHT_CONDITION = 0xFF55AAFF;   // 条件权重（蓝色）
    public static final int WEIGHT_BENEFIT = 0xFFFFAA55;     // 收益消耗（橙色）
    public static final int WEIGHT_REMAINING = 0xFF55FF55;   // 剩余权重（绿色）
    public static final int WEIGHT_OVERFLOW = 0xFFFF5555;    // 超出（红色）
    public static final int WEIGHT_BAR_BG = 0xFF222222;

    // ==================== 面板/背景颜色 ====================
    public static final int PANEL_BG = 0xE0101020;
    public static final int PANEL_BORDER = 0xFF444466;
    public static final int PANEL_HEADER_BG = 0xFF202040;

    // ==================== 列表条目颜色 ====================
    public static final int ENTRY_BG_NORMAL = 0x80202030;
    public static final int ENTRY_BG_HOVERED = 0xA0303050;
    public static final int ENTRY_BG_SELECTED = 0xA0404080;
    public static final int ENTRY_BORDER_NORMAL = 0x60FFFFFF;
    public static final int ENTRY_BORDER_HOVERED = 0xFFAAAAFF;
    public static final int ENTRY_BORDER_SELECTED = 0xFF8888FF;

    // ==================== 槽位/区域颜色 ====================
    public static final int SLOT_CONDITION_BG = 0x80003366;
    public static final int SLOT_CONDITION_BORDER = 0xFF4488CC;
    public static final int SLOT_BENEFIT_BG = 0x80663300;
    public static final int SLOT_BENEFIT_BORDER = 0xFFCC8844;
    public static final int SLOT_PENALTY_BG = 0x80660022;
    public static final int SLOT_PENALTY_BORDER = 0xFFCC4466;
    public static final int SLOT_EMPTY_BG = 0x40333333;
    public static final int SLOT_EMPTY_BORDER = 0x60666666;

    // ==================== 按钮颜色 (BG, HOVER, BORDER) ====================
    // 主要操作（蓝）
    public static final int BTN_PRIMARY_BG = 0xFF2255AA;
    public static final int BTN_PRIMARY_HOVER = 0xFF3366CC;
    public static final int BTN_PRIMARY_BORDER = 0xFF4477DD;

    // 危险操作（红 - 用于删除/停用）
    public static final int BTN_DANGER_BG = 0xFFAA2222;    // 原: 0xFFAA3333
    public static final int BTN_DANGER_HOVER = 0xFFCC3333; // 原: 0xFFCC4444
    public static final int BTN_DANGER_BORDER = 0xFFFF6666;

    // 成功/激活操作（绿 - 用于激活）
    public static final int BTN_SUCCESS_BG = 0xFF22AA22;   // 原: 0xFF338833
    public static final int BTN_SUCCESS_HOVER = 0xFF33CC33;// 原: 0xFF44AA44
    public static final int BTN_SUCCESS_BORDER = 0xFF66FF66;

    // 禁用状态（灰）
    public static final int BTN_DISABLED_BG = 0xFF444444;
    public static final int BTN_DISABLED_BORDER = 0xFF555555;

    // ==================== 文字颜色 ====================
    public static final int TEXT_TITLE = 0xFFFFFFDD;
    public static final int TEXT_NORMAL = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;
    public static final int TEXT_DISABLED = 0xFF666666;
    public static final int TEXT_WARNING = 0xFFFFAA00;
    public static final int TEXT_ERROR = 0xFFFF5555;
    public static final int TEXT_DIM = 0xFF888888;
    public static final int TEXT_HINT = 0xFF666688;

    // ==================== 工具方法 ====================

    public static int getStateColor(String stateName) {
        return switch (stateName.toUpperCase()) {
            case "ACTIVE" -> STATE_ACTIVE;
            case "INACTIVE" -> STATE_INACTIVE;
            case "VIOLATED" -> STATE_VIOLATED;
            case "DISSOLVED" -> STATE_DISSOLVED;
            case "EXPIRED" -> STATE_EXPIRED;
            case "EXHAUSTED" -> STATE_EXHAUSTED; // ★ 新增分支
            default -> TEXT_NORMAL;
        };
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
