package com.jujutsuaddon.addon.vow;

import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可配置参数定义
 * Configurable Parameters Definition
 *
 * 定义一个条件/收益/惩罚可以配置的参数列表。
 * 用于GUI动态生成配置界面。
 *
 * 例如：时间限制条件可以配置：
 *      - 开始时间（整数）
 *      - 结束时间（整数）
 *      - 是否包含边界（布尔）
 */
public class ConfigurableParams {

    /** 参数条目列表 */
    private final List<ParamEntry> entries = new ArrayList<>();

    /**
     * 添加整数参数
     * @param key 参数键
     * @param name 显示名称
     * @param min 最小值
     * @param max 最大值
     * @param defaultValue 默认值
     */
    public ConfigurableParams addInt(String key, Component name, int min, int max, int defaultValue) {
        entries.add(new ParamEntry(key, ParamType.INT, name, min, max, defaultValue, null));
        return this;
    }

    /**
     * 添加浮点数参数
     * @param key 参数键
     * @param name 显示名称
     * @param min 最小值
     * @param max 最大值
     * @param defaultValue 默认值
     */
    public ConfigurableParams addFloat(String key, Component name, float min, float max, float defaultValue) {
        entries.add(new ParamEntry(key, ParamType.FLOAT, name, min, max, defaultValue, null));
        return this;
    }

    /**
     * 添加布尔参数
     * @param key 参数键
     * @param name 显示名称
     * @param defaultValue 默认值
     */
    public ConfigurableParams addBoolean(String key, Component name, boolean defaultValue) {
        entries.add(new ParamEntry(key, ParamType.BOOLEAN, name, 0, 1, defaultValue ? 1 : 0, null));
        return this;
    }

    /**
     * 添加字符串参数
     * @param key 参数键
     * @param name 显示名称
     * @param defaultValue 默认值
     */
    public ConfigurableParams addString(String key, Component name, String defaultValue) {
        entries.add(new ParamEntry(key, ParamType.STRING, name, 0, 0, 0, defaultValue));
        return this;
    }

    /**
     * 添加技能选择参数
     * @param key 参数键
     * @param name 显示名称
     */
    public ConfigurableParams addAbility(String key, Component name) {
        entries.add(new ParamEntry(key, ParamType.ABILITY, name, 0, 0, 0, null));
        return this;
    }

    /**
     * 添加技能列表选择参数
     * @param key 参数键
     * @param name 显示名称
     */
    public ConfigurableParams addAbilityList(String key, Component name) {
        entries.add(new ParamEntry(key, ParamType.ABILITY_LIST, name, 0, 0, 0, null));
        return this;
    }

    /**
     * 获取所有参数条目
     * @return 不可修改的参数条目列表
     */
    public List<ParamEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    // ==================== 参数条目内部类 ====================

    /**
     * 单个参数的定义
     */
    public static class ParamEntry {
        /** 参数键（用于存储和获取值） */
        public final String key;

        /** 参数类型 */
        public final ParamType type;

        /** 显示名称（支持国际化） */
        public final Component displayName;

        /** 数值最小值 */
        public final double min;

        /** 数值最大值 */
        public final double max;

        /** 数值类型的默认值 */
        public final double numericDefault;

        /** 字符串类型的默认值 */
        public final String stringDefault;

        public ParamEntry(String key, ParamType type, Component displayName,
                          double min, double max, double numericDefault, String stringDefault) {
            this.key = key;
            this.type = type;
            this.displayName = displayName;
            this.min = min;
            this.max = max;
            this.numericDefault = numericDefault;
            this.stringDefault = stringDefault;
        }

        /**
         * 获取整数默认值
         */
        public int getDefaultInt() {
            return (int) numericDefault;
        }

        /**
         * 获取浮点数默认值
         */
        public float getDefaultFloat() {
            return (float) numericDefault;
        }

        /**
         * 获取布尔默认值
         */
        public boolean getDefaultBoolean() {
            return numericDefault > 0;
        }

        /**
         * 获取字符串默认值
         */
        public String getDefaultString() {
            return stringDefault != null ? stringDefault : "";
        }
    }

    /**
     * 参数类型枚举
     */
    public enum ParamType {
        /** 整数 */
        INT,

        /** 浮点数 */
        FLOAT,

        /** 布尔值 */
        BOOLEAN,

        /** 字符串 */
        STRING,

        /** 单个技能选择 */
        ABILITY,

        /** 技能列表选择 */
        ABILITY_LIST
    }
}
