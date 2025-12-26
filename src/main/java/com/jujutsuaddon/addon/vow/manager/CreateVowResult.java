package com.jujutsuaddon.addon.vow.manager;

import com.jujutsuaddon.addon.vow.CustomBindingVow;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 创建誓约的结果封装类
 * Create Vow Result
 *
 * 封装誓约创建操作的结果，包含成功/失败状态、
 * 创建的誓约实例（成功时）或错误信息（失败时）
 */
public class CreateVowResult {

    /** 操作是否成功 */
    private final boolean success;

    /** 创建成功时的誓约实例 */
    @Nullable
    private final CustomBindingVow vow;

    /** 失败时的错误信息键（用于国际化） */
    @Nullable
    private final String errorKey;

    /** 错误信息的格式化参数 */
    private final Object[] errorArgs;

    private CreateVowResult(boolean success, @Nullable CustomBindingVow vow,
                            @Nullable String errorKey, Object... args) {
        this.success = success;
        this.vow = vow;
        this.errorKey = errorKey;
        this.errorArgs = args;
    }

    /**
     * 创建成功结果
     * @param vow 成功创建的誓约
     */
    public static CreateVowResult success(CustomBindingVow vow) {
        return new CreateVowResult(true, vow, null);
    }

    /**
     * 创建失败结果
     * @param key 错误信息的国际化键
     * @param args 格式化参数
     */
    public static CreateVowResult error(String key, Object... args) {
        return new CreateVowResult(false, null, key, args);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public CustomBindingVow getVow() {
        return vow;
    }

    /**
     * 获取格式化后的错误信息组件
     */
    public Component getErrorMessage() {
        return errorKey != null ? Component.translatable(errorKey, errorArgs) : Component.empty();
    }
}
