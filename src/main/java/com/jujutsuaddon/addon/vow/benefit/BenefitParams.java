package com.jujutsuaddon.addon.vow.benefit;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

/**
 * 收益参数容器
 * Benefit Parameters
 *
 * 存储收益的可配置参数，支持多种数据类型。
 * 使用键值对方式存储，方便扩展和序列化。
 *
 * 例如：输出加成收益可以配置加成百分比
 *      技能限制收益可以配置允许的技能列表
 */
public class BenefitParams {

    /** 参数键值对存储 */
    private final Map<String, Object> values = new HashMap<>();

    public BenefitParams() {}

    // ==================== 设置方法（支持链式调用） ====================

    public BenefitParams setInt(String key, int value) {
        values.put(key, value);
        return this;
    }

    public BenefitParams setFloat(String key, float value) {
        values.put(key, value);
        return this;
    }

    public BenefitParams setBoolean(String key, boolean value) {
        values.put(key, value);
        return this;
    }

    public BenefitParams setString(String key, String value) {
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
    public static BenefitParams fromNBT(CompoundTag tag) {
        BenefitParams params = new BenefitParams();
        for (String key : tag.getAllKeys()) {
            // 根据NBT标签类型判断数据类型
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
     * 创建参数的深拷贝
     */
    public BenefitParams copy() {
        BenefitParams copy = new BenefitParams();
        copy.values.putAll(this.values);
        return copy;
    }
}
