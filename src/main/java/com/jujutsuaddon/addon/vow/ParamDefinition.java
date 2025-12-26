package com.jujutsuaddon.addon.vow;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可配置参数的定义
 * 用于UI显示和验证
 */
public class ParamDefinition {

    private final List<Entry> entries = new ArrayList<>();

    public ParamDefinition addInt(String key, Component displayName, int min, int max, int defaultValue) {
        entries.add(new Entry(key, ParamType.INT, displayName, min, max, defaultValue));
        return this;
    }

    public ParamDefinition addFloat(String key, Component displayName, float min, float max, float defaultValue) {
        entries.add(new Entry(key, ParamType.FLOAT, displayName, min, max, defaultValue));
        return this;
    }

    public ParamDefinition addBoolean(String key, Component displayName, boolean defaultValue) {
        entries.add(new Entry(key, ParamType.BOOLEAN, displayName, 0, 1, defaultValue ? 1 : 0));
        return this;
    }

    public ParamDefinition addString(String key, Component displayName, String defaultValue) {
        Entry entry = new Entry(key, ParamType.STRING, displayName, 0, 0, 0);
        entry.stringDefault = defaultValue;
        entries.add(entry);
        return this;
    }

    /**
     * 添加字符串选择列表 (下拉框/循环按钮)
     * @param key 参数键名
     * @param displayName 显示名称
     * @param options 选项列表
     * @param defaultValue 默认选项
     */
    public ParamDefinition addStringSelection(String key, Component displayName, List<String> options, String defaultValue) {
        Entry entry = new Entry(key, ParamType.SELECTION, displayName, 0, 0, 0);
        // 确保列表不为空且不可变，防止外部修改
        entry.validValues = options != null ? new ArrayList<>(options) : new ArrayList<>();
        entry.stringDefault = defaultValue;
        entries.add(entry);
        return this;
    }

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    public enum ParamType {
        INT,
        FLOAT,
        BOOLEAN,
        STRING,
        RESOURCE_LOCATION,
        SELECTION
    }

    public static class Entry {
        private final String key;
        private final ParamType type;
        private final Component displayName;
        private final Number min;
        private final Number max;
        private final Number defaultValue;

        private String stringDefault;
        private List<String> validValues = new ArrayList<>(); // 初始化防止空指针

        Entry(String key, ParamType type, Component displayName, Number min, Number max, Number defaultValue) {
            this.key = key;
            this.type = type;
            this.displayName = displayName;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
        }

        public String getKey() { return key; }
        public ParamType getType() { return type; }
        public Component getDisplayName() { return displayName; }
        public Number getMin() { return min; }
        public Number getMax() { return max; }
        public Number getDefaultValue() { return defaultValue; }
        public String getStringDefault() { return stringDefault; }

        /** 获取有效选项列表 */
        public List<String> getValidValues() {
            return validValues != null ? validValues : Collections.emptyList();
        }
    }
}
