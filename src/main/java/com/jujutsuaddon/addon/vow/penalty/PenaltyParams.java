package com.jujutsuaddon.addon.vow.penalty;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

/**
 * 惩罚参数容器
 * Penalty Parameters
 */
public class PenaltyParams {

    /** 参数键值对存储 */
    private final Map<String, Object> values = new HashMap<>();

    public PenaltyParams() {}

    // ==================== 设置方法（支持链式调用） ====================

    public PenaltyParams setInt(String key, int value) {
        values.put(key, value);
        return this;
    }

    public PenaltyParams setFloat(String key, float value) {
        values.put(key, value);
        return this;
    }

    public PenaltyParams setBoolean(String key, boolean value) {
        values.put(key, value);
        return this;
    }

    public PenaltyParams setString(String key, String value) {
        values.put(key, value);
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

    // ==================== 序列化 ====================

    /**
     * 序列化为NBT标签
     */
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
            }
        }
        return tag;
    }

    /**
     * 从NBT标签反序列化
     */
    public static PenaltyParams fromNBT(CompoundTag tag) {
        PenaltyParams params = new PenaltyParams();
        for (String key : tag.getAllKeys()) {
            if (tag.contains(key, 3)) {        // TAG_Int
                params.setInt(key, tag.getInt(key));
            } else if (tag.contains(key, 5)) { // TAG_Float
                params.setFloat(key, tag.getFloat(key));
            } else if (tag.contains(key, 1)) { // TAG_Byte (boolean)
                params.setBoolean(key, tag.getBoolean(key));
            } else if (tag.contains(key, 8)) { // TAG_String
                params.setString(key, tag.getString(key));
            }
        }
        return params;
    }

    /**
     * 从NBT标签反序列化（别名方法，保持API一致性）
     */
    public static PenaltyParams deserializeNBT(CompoundTag tag) {
        return fromNBT(tag);
    }

    /**
     * 创建参数的深拷贝
     */
    public PenaltyParams copy() {
        PenaltyParams copy = new PenaltyParams();
        copy.values.putAll(this.values);
        return copy;
    }

    @Override
    public String toString() {
        return "PenaltyParams{" + values + "}";
    }
}
