package com.jujutsuaddon.addon.client.skillbar;

/**
 * 技能栏相关常量
 * 统一管理所有尺寸，便于全局调整
 */
public final class SkillBarConstants {

    private SkillBarConstants() {}

    // ===== HUD 技能栏尺寸 =====
    public static final int HUD_SLOT_SIZE = 22;
    public static final int HUD_ICON_SIZE = 18;
    public static final int HUD_ICON_OFFSET = 2;
    public static final int HUD_SLOT_PADDING = 2;
    public static final int HUD_HEADER_HEIGHT = 12;

    // ===== 配置界面槽位尺寸 =====
    public static final int CONFIG_SLOT_SIZE = 32;
    public static final int CONFIG_SLOT_PADDING = 4;
    public static final int CONFIG_ICON_OFFSET = 4;  // ← 这个之前漏了！

    // ===== 列表条目尺寸 =====
    public static final int ENTRY_HEIGHT = 18;
    public static final int ENTRY_ICON_SIZE = 16;
    public static final int ENTRY_INDENT_UNIT = 10;

    // ===== 拖拽相关 =====
    public static final int DRAG_ICON_SIZE = 24;
    public static final int DRAG_THRESHOLD = 5;
}
