package com.jujutsuaddon.addon.client.skillbar;

import net.minecraft.resources.ResourceLocation;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;

import javax.annotation.Nullable;

public class SkillBarData {

    public static final int SLOT_COUNT = 6;
    public static final int MAX_PRESETS = 5;

    // 术式切换的特殊前缀
    public static final String TECHNIQUE_PREFIX = "jujutsu_addon:technique_switch/";
    public static final String CURSE_MANAGEMENT = "jujutsu_addon:curse_management";  // ★ 新增

    // 多套预设 - 改为存储字符串，以支持两种类型
    private final String[][] presets = new String[MAX_PRESETS][SLOT_COUNT];

    // 当前使用的预设索引
    private int currentPreset = 0;

    // ===== 预设管理 =====

    public int getCurrentPresetIndex() {
        return currentPreset;
    }

    public void setCurrentPreset(int index) {
        if (index >= 0 && index < MAX_PRESETS) {
            currentPreset = index;
        }
    }

    public void nextPreset() {
        currentPreset = (currentPreset + 1) % MAX_PRESETS;
    }

    public void prevPreset() {
        currentPreset = (currentPreset - 1 + MAX_PRESETS) % MAX_PRESETS;
    }

    // ===== 普通技能槽位操作 =====

    public void setSlot(int slot, @Nullable Ability ability) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        if (ability != null) {
            ResourceLocation key = JJKAbilities.getKey(ability);
            presets[currentPreset][slot] = key != null ? key.toString() : null;
        } else {
            presets[currentPreset][slot] = null;
        }
    }

    @Nullable
    public Ability getSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return null;
        String value = presets[currentPreset][slot];
        // 如果是术式前缀，返回null（不是普通技能）
        if (value == null || value.startsWith(TECHNIQUE_PREFIX)) return null;

        try {
            ResourceLocation key = new ResourceLocation(value);
            return JJKAbilities.getValue(key);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public ResourceLocation getSlotKey(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return null;
        String value = presets[currentPreset][slot];
        if (value == null || value.startsWith(TECHNIQUE_PREFIX)) return null;
        try {
            return new ResourceLocation(value);
        } catch (Exception e) {
            return null;
        }
    }

    // ===== 术式槽位操作 =====

    public void setSlotTechnique(int slot, @Nullable CursedTechnique technique) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        presets[currentPreset][slot] = technique != null ?
                TECHNIQUE_PREFIX + technique.name() : null;
    }

    public boolean isSlotTechnique(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return false;
        String value = presets[currentPreset][slot];
        return value != null && value.startsWith(TECHNIQUE_PREFIX);
    }

    @Nullable
    public CursedTechnique getSlotTechnique(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return null;
        String value = presets[currentPreset][slot];
        if (value == null || !value.startsWith(TECHNIQUE_PREFIX)) return null;

        String techniqueName = value.substring(TECHNIQUE_PREFIX.length());
        try {
            return CursedTechnique.valueOf(techniqueName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ===== 咒灵管理槽位操作 =====
    public void setSlotCurseManagement(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        presets[currentPreset][slot] = CURSE_MANAGEMENT;
    }
    public boolean isSlotCurseManagement(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return false;
        return CURSE_MANAGEMENT.equals(presets[currentPreset][slot]);
    }

    // ===== 通用槽位操作 =====

    public void clearSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        presets[currentPreset][slot] = null;
    }

    public boolean isEmpty(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return true;
        return presets[currentPreset][slot] == null;
    }

    public int findAbility(Ability ability) {
        if (ability == null) return -1;
        ResourceLocation key = JJKAbilities.getKey(ability);
        if (key == null) return -1;
        String keyStr = key.toString();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (keyStr.equals(presets[currentPreset][i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean isCurrentPresetEmpty() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (presets[currentPreset][i] != null) {
                return false;
            }
        }
        return true;
    }

    public int getActivePresetCount() {
        int count = 0;
        for (int p = 0; p < MAX_PRESETS; p++) {
            for (int s = 0; s < SLOT_COUNT; s++) {
                if (presets[p][s] != null) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    // ===== 序列化 =====

    public String[] serialize() {
        String[] result = new String[MAX_PRESETS * SLOT_COUNT + 1];
        result[0] = String.valueOf(currentPreset);

        for (int p = 0; p < MAX_PRESETS; p++) {
            for (int s = 0; s < SLOT_COUNT; s++) {
                int index = p * SLOT_COUNT + s + 1;
                result[index] = presets[p][s] != null ? presets[p][s] : "";
            }
        }
        return result;
    }

    public void deserialize(String[] data) {
        if (data == null || data.length == 0) return;

        try {
            currentPreset = Integer.parseInt(data[0]);
            if (currentPreset < 0 || currentPreset >= MAX_PRESETS) {
                currentPreset = 0;
            }
        } catch (NumberFormatException e) {
            currentPreset = 0;
        }

        for (int p = 0; p < MAX_PRESETS; p++) {
            for (int s = 0; s < SLOT_COUNT; s++) {
                int index = p * SLOT_COUNT + s + 1;
                if (index < data.length && data[index] != null && !data[index].isEmpty()) {
                    presets[p][s] = data[index];
                } else {
                    presets[p][s] = null;
                }
            }
        }
    }

    public void clear() {
        currentPreset = 0;
        for (int p = 0; p < MAX_PRESETS; p++) {
            for (int s = 0; s < SLOT_COUNT; s++) {
                presets[p][s] = null;
            }
        }
    }
}
