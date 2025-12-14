package com.jujutsuaddon.addon.util.context;

public class DamageContext {
    // 使用 ThreadLocal 防止多线程或多实体同时受伤时数据串台
    private static final ThreadLocal<Float> EXPECTED_SOUL_DAMAGE = new ThreadLocal<>();

    // 【修改】参数改为 Float 包装类，允许传入 null
    public static void set(Float damage) {
        if (damage == null) {
            EXPECTED_SOUL_DAMAGE.remove();
        } else {
            EXPECTED_SOUL_DAMAGE.set(damage);
        }
    }

    public static Float get() {
        return EXPECTED_SOUL_DAMAGE.get();
    }

    public static void clear() {
        EXPECTED_SOUL_DAMAGE.remove();
    }
}
