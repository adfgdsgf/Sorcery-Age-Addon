package com.jujutsuaddon.addon.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AddonKeyBindings {

    public static final String CATEGORY = "key.categories.jujutsu_addon";

    // ===== 技能栏按键 =====
    public static final List<KeyMapping> SKILL_SLOT_KEYS = new ArrayList<>();

    public static KeyMapping OPEN_SKILL_CONFIG;
    public static KeyMapping EDIT_SKILL_POSITION;
    public static KeyMapping NEXT_PRESET;
    public static KeyMapping PREV_PRESET;
    public static KeyMapping TOGGLE_SKILL_KEYS;

    // ===== 冷却HUD按键 =====
    public static KeyMapping OPEN_HUD_EDIT;

    // ===== 誓约系统按键 =====
    public static KeyMapping OPEN_VOW_SCREEN;

    // ===== 自瞄按键 =====
    public static KeyMapping TOGGLE_AIM_ASSIST;

    // ★★★ 新增：无下限滚轮修饰键 ★★★
    public static KeyMapping INFINITY_SCROLL_MODIFIER;

    // ★★★ 新增：投射物反弹修饰键 ★★★
    public static KeyMapping REFLECT_TO_OWNER_MODIFIER;   // 反弹到原发射者
    public static KeyMapping REFLECT_TO_CURSOR_MODIFIER;  // 反弹到准星方向

    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // 技能槽位 Z X C V B N
        int[] defaultSlotKeys = {
                GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C,
                GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N
        };

        for (int i = 0; i < 6; i++) {
            SKILL_SLOT_KEYS.add(new KeyMapping(
                    "key.jujutsu_addon.skill_slot_" + (i + 1),
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    defaultSlotKeys[i],
                    CATEGORY
            ));
        }

        OPEN_SKILL_CONFIG = new KeyMapping(
                "key.jujutsu_addon.open_skill_bar_config",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        );

        NEXT_PRESET = new KeyMapping(
                "key.jujutsu_addon.next_preset",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                CATEGORY
        );

        PREV_PRESET = new KeyMapping(
                "key.jujutsu_addon.prev_preset",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                CATEGORY
        );

        TOGGLE_SKILL_KEYS = new KeyMapping(
                "key.jujutsu_addon.toggle_skill_keys",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                CATEGORY
        );

        OPEN_HUD_EDIT = new KeyMapping(
                "key.jujutsu_addon.hud_edit",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON,
                CATEGORY
        );

        TOGGLE_AIM_ASSIST = new KeyMapping(
                "key.jujutsu_addon.toggle_aim_assist",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                CATEGORY
        );

        // ★★★ 无下限滚轮修饰键 - 默认 Left Control ★★★
        INFINITY_SCROLL_MODIFIER = new KeyMapping(
                "key.jujutsu_addon.infinity_scroll_modifier",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,  // 默认左Ctrl
                CATEGORY
        );
        // ★★★ 投射物反弹 - 反弹到原发射者 - 默认 Left Shift ★★★
        REFLECT_TO_OWNER_MODIFIER = new KeyMapping(
                "key.jujutsu_addon.reflect_to_owner",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_SHIFT,
                CATEGORY
        );
        // ★★★ 投射物反弹 - 反弹到准星方向 - 默认 Left Control ★★★
        REFLECT_TO_CURSOR_MODIFIER = new KeyMapping(
                "key.jujutsu_addon.reflect_to_cursor",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                CATEGORY
        );
        // ===== 誓约界面 - 默认 Y 键 =====
        OPEN_VOW_SCREEN = new KeyMapping(
                "key.jujutsu_addon.open_vow_screen",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                CATEGORY
        );
    }

    public static List<KeyMapping> getAllKeys() {
        List<KeyMapping> all = new ArrayList<>(SKILL_SLOT_KEYS);
        all.add(OPEN_SKILL_CONFIG);
        all.add(NEXT_PRESET);
        all.add(PREV_PRESET);
        all.add(TOGGLE_SKILL_KEYS);
        all.add(OPEN_HUD_EDIT);
        all.add(TOGGLE_AIM_ASSIST);
        all.add(INFINITY_SCROLL_MODIFIER);  // ★ 添加
        all.add(REFLECT_TO_OWNER_MODIFIER);   // ★ 新增
        all.add(REFLECT_TO_CURSOR_MODIFIER);
        all.add(OPEN_VOW_SCREEN);
        return all;
    }
}
