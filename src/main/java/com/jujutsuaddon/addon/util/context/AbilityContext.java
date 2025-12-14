package com.jujutsuaddon.addon.util.context;

import radon.jujutsu_kaisen.ability.base.Ability;

public class AbilityContext {
    private static final ThreadLocal<Ability> CURRENT_ABILITY = new ThreadLocal<>();
    private static final ThreadLocal<Long> LAST_SET_TIME = new ThreadLocal<>();

    public static void set(Ability ability) {
        CURRENT_ABILITY.set(ability);
        LAST_SET_TIME.set(System.currentTimeMillis());
    }

    public static Ability get() {
        Long lastTime = LAST_SET_TIME.get();
        if (lastTime == null) return null;
        // 50ms 过期检查，防止污染
        if (System.currentTimeMillis() - lastTime > 50) {
            clear();
            return null;
        }
        return CURRENT_ABILITY.get();
    }

    public static void clear() {
        CURRENT_ABILITY.remove();
        LAST_SET_TIME.remove();
    }
}
