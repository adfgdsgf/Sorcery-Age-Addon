package com.jujutsuaddon.addon.client;

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
    public static KeyMapping EDIT_SKILL_POSITION;  // 可选，因为Config界面里有按钮
    public static KeyMapping NEXT_PRESET;
    public static KeyMapping PREV_PRESET;
    public static KeyMapping TOGGLE_SKILL_KEYS;

    // ===== 冷却HUD按键 =====
    public static KeyMapping OPEN_HUD_EDIT;

    public static void init() {
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
    }

    public static List<KeyMapping> getAllKeys() {
        List<KeyMapping> all = new ArrayList<>(SKILL_SLOT_KEYS);
        all.add(OPEN_SKILL_CONFIG);
        all.add(NEXT_PRESET);
        all.add(PREV_PRESET);
        all.add(TOGGLE_SKILL_KEYS);
        all.add(OPEN_HUD_EDIT);
        return all;
    }
}
