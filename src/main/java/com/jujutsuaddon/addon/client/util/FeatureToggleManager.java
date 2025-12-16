package com.jujutsuaddon.addon.client.util;

import com.jujutsuaddon.addon.JujutsuAddon;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 功能开关管理器
 * 当原mod集成了相应功能后，自动禁用附属的对应功能
 */
public class FeatureToggleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JujutsuAddon.MODID);

    // ==================== 版本阈值配置 ====================
    // 当 JJK mod 版本 >= 这些值时，对应功能自动禁用
    // 初始设为一个很高的版本号，等原作者集成后再改成实际版本

    /** 技能栏系统集成版本 */
    private static final String JJK_SKILL_BAR_VERSION = "99.0.0";

    /** 冷却HUD集成版本 */
    private static final String JJK_COOLDOWN_HUD_VERSION = "99.0.0";

    /** 影子库存UI集成版本 */
    private static final String JJK_SHADOW_STORAGE_VERSION = "99.0.0";

    /** 咒灵管理UI集成版本 */
    private static final String JJK_CURSE_MANAGEMENT_VERSION = "99.0.0";

    // ==================== 功能枚举 ====================

    public enum Feature {
        /** 技能快捷栏 (SkillBarOverlay, SkillBarConfigScreen, etc.) */
        SKILL_BAR,

        /** 快捷键系统 (AddonKeyBindings, JJKKeyBlocker) */
        KEYBIND_SYSTEM,

        /** 冷却HUD (AbilityCooldownOverlay, HUDEditScreen) */
        COOLDOWN_HUD,

        /** 影子库存UI (ShadowStorageScreen, ShadowStorageOverlay) */
        SHADOW_STORAGE,

        /** 咒灵管理UI (CurseManagementScreen) */
        CURSE_MANAGEMENT
    }

    // ==================== 缓存 ====================

    private static String cachedJJKVersion = null;
    private static Boolean[] featureCache = null;

    // ==================== 主要API ====================

    /**
     * 检查功能是否应该启用
     *
     * @param feature 要检查的功能
     * @return true = 附属功能应该启用, false = 原mod已集成，附属功能应该禁用
     */
    public static boolean isEnabled(Feature feature) {
        if (featureCache == null) {
            featureCache = new Boolean[Feature.values().length];
        }

        int index = feature.ordinal();
        if (featureCache[index] == null) {
            featureCache[index] = checkFeature(feature);
        }
        return featureCache[index];
    }

    // ==================== 便捷方法 ====================

    /** 技能栏系统是否启用 */
    public static boolean isSkillBarEnabled() {
        return isEnabled(Feature.SKILL_BAR);
    }

    /** 快捷键系统是否启用 */
    public static boolean isKeybindSystemEnabled() {
        return isEnabled(Feature.KEYBIND_SYSTEM);
    }

    /** 冷却HUD是否启用 */
    public static boolean isCooldownHudEnabled() {
        return isEnabled(Feature.COOLDOWN_HUD);
    }

    /** 影子库存UI是否启用 */
    public static boolean isShadowStorageEnabled() {
        return isEnabled(Feature.SHADOW_STORAGE);
    }

    /** 咒灵管理UI是否启用 */
    public static boolean isCurseManagementEnabled() {
        return isEnabled(Feature.CURSE_MANAGEMENT);
    }

    // ==================== 内部逻辑 ====================

    private static boolean checkFeature(Feature feature) {
        String jjkVersion = getJJKVersion();
        if (jjkVersion == null) {
            // 无法获取版本，保守起见启用附属功能
            return true;
        }

        String requiredVersion = switch (feature) {
            case SKILL_BAR, KEYBIND_SYSTEM -> JJK_SKILL_BAR_VERSION;
            case COOLDOWN_HUD -> JJK_COOLDOWN_HUD_VERSION;
            case SHADOW_STORAGE -> JJK_SHADOW_STORAGE_VERSION;
            case CURSE_MANAGEMENT -> JJK_CURSE_MANAGEMENT_VERSION;
        };

        // 如果 JJK 版本 >= 集成版本，则禁用附属功能
        boolean jjkHasFeature = compareVersions(jjkVersion, requiredVersion) >= 0;

        if (jjkHasFeature) {
            LOGGER.info("[JujutsuAddon] Feature {} disabled - integrated in JJK {}",
                    feature.name(), jjkVersion);
        }

        return !jjkHasFeature;
    }

    private static String getJJKVersion() {
        if (cachedJJKVersion == null) {
            cachedJJKVersion = ModList.get()
                    .getModContainerById("jujutsu_kaisen")
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse(null);
        }
        return cachedJJKVersion;
    }

    /**
     * 比较版本号
     * @return 负数 = v1 < v2, 0 = 相等, 正数 = v1 > v2
     */
    private static int compareVersions(String v1, String v2) {
        try {
            String[] parts1 = v1.split("[.\\-+]");
            String[] parts2 = v2.split("[.\\-+]");

            int length = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < length; i++) {
                int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
                int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

                if (num1 != num2) {
                    return num1 - num2;
                }
            }
            return 0;
        } catch (Exception e) {
            // 解析失败，假设版本不够新
            return -1;
        }
    }

    private static int parseVersionPart(String part) {
        try {
            // 移除非数字字符（如 "1.0.0-beta" 中的 "beta"）
            String numericPart = part.replaceAll("[^0-9]", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 刷新缓存（配置热重载时调用）
     */
    public static void refreshCache() {
        featureCache = null;
        cachedJJKVersion = null;
    }

    /**
     * 检查原mod是否有特定类（更精确的检测方式）
     */
    public static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 获取当前 JJK mod 版本（用于调试/显示）
     */
    public static String getJJKVersionString() {
        String version = getJJKVersion();
        return version != null ? version : "unknown";
    }
}
