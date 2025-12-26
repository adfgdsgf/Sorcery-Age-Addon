package com.jujutsuaddon.addon.vow.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 条件参数容器
 * Condition Parameters
 *
 * 存储条件的可配置参数，支持多种数据类型。
 *
 * 例如：时间限制条件可以配置开始/结束时间
 *      技能禁用条件可以配置禁用的技能列表
 */
public class ConditionParams {

    /** 参数键值对存储 */
    private final Map<String, Object> values = new HashMap<>();

    public ConditionParams() {}

    // ==================== 设置方法（支持链式调用） ====================

    public ConditionParams setInt(String key, int value) {
        values.put(key, value);
        return this;
    }

    public ConditionParams setFloat(String key, float value) {
        values.put(key, value);
        return this;
    }

    public ConditionParams setBoolean(String key, boolean value) {
        values.put(key, value);
        return this;
    }

    public ConditionParams setString(String key, String value) {
        values.put(key, value);
        return this;
    }

    public ConditionParams setStringList(String key, List<String> value) {
        values.put(key, new ArrayList<>(value));
        return this;
    }

    // ==================== 获取方法（带默认值） ====================

    public int getInt(String key, int defaultValue) {
        Object value = values.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    public float getFloat(String key, float defaultValue) {
        Object value = values.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object value = values.get(key);
        if (value instanceof List) {
            return new ArrayList<>((List<String>) value);
        }
        return new ArrayList<>();
    }

    // ==================== 序列化 ====================

    /**
     * 序列化为NBT标签
     */
    @SuppressWarnings("unchecked")
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Integer) {
                tag.putInt(key, (Integer) value);
            } else if (value instanceof Float) {
                tag.putFloat(key, (Float) value);
            } else if (value instanceof Boolean) {
                tag.putBoolean(key, (Boolean) value);
            } else if (value instanceof String) {
                tag.putString(key, (String) value);
            } else if (value instanceof List) {
                ListTag listTag = new ListTag();
                for (String s : (List<String>) value) {
                    listTag.add(StringTag.valueOf(s));
                }
                tag.put(key, listTag);
            }
        }
        return tag;
    }

    /**
     * 从NBT标签反序列化
     */
    public static ConditionParams fromNBT(CompoundTag tag) {
        ConditionParams params = new ConditionParams();
        for (String key : tag.getAllKeys()) {
            byte type = tag.getTagType(key);
            switch (type) {
                case Tag.TAG_INT -> params.setInt(key, tag.getInt(key));
                case Tag.TAG_FLOAT -> params.setFloat(key, tag.getFloat(key));
                case Tag.TAG_BYTE -> params.setBoolean(key, tag.getBoolean(key));
                case Tag.TAG_STRING -> params.setString(key, tag.getString(key));
                case Tag.TAG_LIST -> {
                    ListTag listTag = tag.getList(key, Tag.TAG_STRING);
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < listTag.size(); i++) {
                        list.add(listTag.getString(i));
                    }
                    params.setStringList(key, list);
                }
            }
        }
        return params;
    }

    /**
     * 创建参数的深拷贝
     */
    @SuppressWarnings("unchecked")
    public ConditionParams copy() {
        ConditionParams copy = new ConditionParams();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List) {
                copy.values.put(entry.getKey(), new ArrayList<>((List<String>) value));
            } else {
                copy.values.put(entry.getKey(), value);
            }
        }
        return copy;
    }
}
