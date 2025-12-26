package com.jujutsuaddon.addon.vow.calculation;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 誓约验证结果
 * Vow Validation Result
 *
 * 用于验证誓约配置是否合法，包括：
 * - 条件权重是否足够覆盖收益消耗
 * - 是否存在互斥的条件/收益组合
 * - 参数是否在有效范围内
 */
public class ValidationResult {

    /** 验证是否通过 */
    private final boolean valid;

    /** 验证失败时的错误信息键 */
    @Nullable
    private final String errorKey;

    private ValidationResult(boolean valid, @Nullable String errorKey) {
        this.valid = valid;
        this.errorKey = errorKey;
    }

    /**
     * 创建验证通过的结果
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    /**
     * 创建验证失败的结果
     * @param key 错误信息的国际化键
     */
    public static ValidationResult error(String key) {
        return new ValidationResult(false, key);
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * 获取格式化后的错误信息组件
     */
    public Component getErrorMessage() {
        return errorKey != null ? Component.translatable(errorKey) : Component.empty();
    }

    public boolean isSuccess() {
        return valid;
    }
    @Nullable
    public String getErrorKey() {
        return errorKey;
    }
}
