package com.jujutsuaddon.addon.client.util;

import com.jujutsuaddon.addon.client.config.AddonClientConfig;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 模糊搜索助手
 * 自动检测拼音/罗马字等搜索增强mod
 */
public class FuzzySearchHelper {

    private static volatile boolean initialized = false;
    private static Method activeMethod = null;
    private static String activeModId = null;

    // ==================== 已知的搜索mod映射 ====================
    // 格式: modId -> [className, methodName, paramType1, paramType2]
    private static final Map<String, String[]> KNOWN_MODS = new LinkedHashMap<>();

    static {
        // 中文 - JECharacters
        KNOWN_MODS.put("jecharacters", new String[]{
                "me.towdium.jecharacters.utils.Match",
                "contains",
                "String", "CharSequence"
        });

        // 多语言 - Searchables (如果存在)
        KNOWN_MODS.put("searchables", new String[]{
                "com.supermartijn642.searchables.Searchables",
                "matches",
                "String", "String"
        });

        // 可以继续添加更多已知mod...
    }

    // ==================== 初始化 ====================

    public static void reloadFromConfig() {
        initialized = false;
        activeMethod = null;
        activeModId = null;
        ensureInitialized();
    }

    private static synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;

        // 获取配置的mod列表
        List<? extends String> modList;
        try {
            modList = AddonClientConfig.CLIENT.searchMods.get();
        } catch (Exception e) {
            modList = Arrays.asList("jecharacters");
        }

        // 按配置顺序尝试加载
        for (String modId : modList) {
            if (tryLoadMod(modId.trim().toLowerCase())) {
                break; // 找到一个就停止
            }
        }
    }

    private static boolean tryLoadMod(String modId) {
        if (!ModList.get().isLoaded(modId)) {
            return false;
        }

        // 查找已知映射
        String[] info = KNOWN_MODS.get(modId);
        if (info != null) {
            return tryLoadMethod(modId, info[0], info[1], info[2], info[3]);
        }

        // 未知mod：尝试常见模式
        return tryCommonPatterns(modId);
    }

    private static boolean tryLoadMethod(String modId, String className, String methodName,
                                         String param1, String param2) {
        try {
            Class<?> clazz = Class.forName(className);
            Class<?> p1 = param1.equals("CharSequence") ? CharSequence.class : String.class;
            Class<?> p2 = param2.equals("CharSequence") ? CharSequence.class : String.class;

            Method method = clazz.getMethod(methodName, p1, p2);
            activeMethod = method;
            activeModId = modId;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean tryCommonPatterns(String modId) {
        // 尝试常见的类名和方法名模式
        String[] classPatterns = {
                "com." + modId + ".Match",
                "com." + modId + ".utils.Match",
                "com." + modId + ".search.Matcher",
                modId + ".Match",
                modId + ".utils.Match"
        };

        String[] methodPatterns = {"contains", "matches", "match", "search"};

        for (String className : classPatterns) {
            for (String methodName : methodPatterns) {
                if (tryLoadMethod(modId, className, methodName, "String", "String") ||
                        tryLoadMethod(modId, className, methodName, "String", "CharSequence")) {
                    return true;
                }
            }
        }

        return false;
    }

    // ==================== 公共API ====================

    /**
     * 模糊匹配
     */
    public static boolean contains(String text, String search) {
        ensureInitialized();

        if (text == null || search == null || search.isEmpty()) {
            return true;
        }

        String lowerText = text.toLowerCase();
        String lowerSearch = search.toLowerCase();

        // 尝试使用增强搜索
        if (activeMethod != null) {
            try {
                Object result = activeMethod.invoke(null, lowerText, lowerSearch);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception ignored) {
                // 静默失败，回退到普通搜索
            }
        }

        // 回退：普通包含匹配
        return lowerText.contains(lowerSearch);
    }

    /**
     * 是否有增强搜索
     */
    public static boolean isEnhancedSearchAvailable() {
        ensureInitialized();
        return activeMethod != null;
    }

    /**
     * 获取当前使用的mod ID
     */
    public static String getActiveModId() {
        ensureInitialized();
        return activeModId != null ? activeModId : "none";
    }
}
