package com.jujutsuaddon.addon.util.context;

public class WeaponProxyContext {
    private static final ThreadLocal<Boolean> FORCE_FULL_CHARGE = ThreadLocal.withInitial(() -> false);

    public static void set(boolean value) {
        FORCE_FULL_CHARGE.set(value);
    }

    public static boolean isActive() {
        return FORCE_FULL_CHARGE.get();
    }

    public static void clear() {
        FORCE_FULL_CHARGE.remove();
    }
}
