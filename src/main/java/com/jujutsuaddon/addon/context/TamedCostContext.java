package com.jujutsuaddon.addon.context; // 注意包名

public class TamedCostContext {
    private static final ThreadLocal<Boolean> FORCE_TAMED = ThreadLocal.withInitial(() -> false);

    public static void setForceTamed(boolean force) {
        FORCE_TAMED.set(force);
    }

    public static boolean shouldForceTamed() {
        return FORCE_TAMED.get();
    }
}
