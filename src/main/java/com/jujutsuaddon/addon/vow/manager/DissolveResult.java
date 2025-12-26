package com.jujutsuaddon.addon.vow.manager;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 解除誓约的结果封装类
 * Dissolve Vow Result
 *
 * 封装誓约解除操作的结果，用于向玩家反馈操作状态
 */
public class DissolveResult {

    /** 操作是否成功 */
    private final boolean success;

    /** 失败时的错误信息键 */
    @Nullable
    private final String errorKey;

    private DissolveResult(boolean success, @Nullable String errorKey) {
        this.success = success;
        this.errorKey = errorKey;
    }

    /**
     * 创建成功结果
     */
    public static DissolveResult success() {
        return new DissolveResult(true, null);
    }

    /**
     * 创建失败结果
     * @param key 错误信息的国际化键
     */
    public static DissolveResult error(String key) {
        return new DissolveResult(false, key);
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取格式化后的错误信息组件
     */
    public Component getErrorMessage() {
        return errorKey != null ? Component.translatable(errorKey) : Component.empty();
    }
}
