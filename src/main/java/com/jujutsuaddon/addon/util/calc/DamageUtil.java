package com.jujutsuaddon.addon.util.calc;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.helper.SummonScalingHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageUtil {

    private static Map<String, Double> skillMultiplierCache = null;
    private static Map<Attribute, Double> flatAttributeCache = null;
    private static Map<Attribute, Double> multiplierAttributeCache = null;

    private static final String[] AUTO_SCAN_KEYWORDS = {
            "magic_damage", "spell_power", "spell_damage", "mana_damage", "intelligence", "magic_attack",
            "projectile", "arrow", "ranged",
            "fire", "explosion", "cold", "lightning", "holy", "elemental", "all_damage"
    };

    private static final String[] AUTO_SCAN_BLACKLIST = {
            "resistance", "reduction", "cost", "regen", "speed", "velocity"
    };

    /**
     * 刷新所有缓存（配置重载时调用）
     */
    public static void reload() {
        // 清理自身缓存
        skillMultiplierCache = null;
        flatAttributeCache = null;
        multiplierAttributeCache = null;

        // 刷新新分类系统
        CategoryResolver.reload();
        CategoryBenchmark.reload();

        // 刷新召唤物缓存
        SummonScalingHelper.clearCache();

        // 重新加载
        loadCaches();
    }

    public static void loadCaches() {
        if (skillMultiplierCache != null) return;

        // 1. 技能倍率
        skillMultiplierCache = new HashMap<>();
        try {
            List<? extends String> skillConfig = AddonConfig.COMMON.skillMultipliers.get();
            for (String entry : skillConfig) {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    try {
                        skillMultiplierCache.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        // 2. 白值转化
        flatAttributeCache = new HashMap<>();
        try {
            List<? extends String> flatConfig = AddonConfig.COMMON.extraAttributeScaling.get();
            parseAttributeConfig(flatConfig, flatAttributeCache);
        } catch (Exception ignored) {}

        // 3. 倍率转化
        multiplierAttributeCache = new HashMap<>();

        // A. 自动扫描
        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : ForgeRegistries.ATTRIBUTES.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Attribute attr = entry.getValue();
            String path = id.getPath().toLowerCase();

            boolean isBlacklisted = false;
            for (String block : AUTO_SCAN_BLACKLIST) {
                if (path.contains(block)) {
                    isBlacklisted = true;
                    break;
                }
            }
            if (isBlacklisted) continue;

            boolean isMatch = false;
            for (String keyword : AUTO_SCAN_KEYWORDS) {
                if (path.contains(keyword)) {
                    isMatch = true;
                    break;
                }
            }

            if (isMatch) {
                multiplierAttributeCache.put(attr, 1.0);
            }
        }

        // B. 配置文件覆盖
        try {
            List<? extends String> multConfig = AddonConfig.COMMON.bonusMultiplierAttributes.get();
            parseAttributeConfig(multConfig, multiplierAttributeCache);
        } catch (Exception ignored) {}
    }

    private static void parseAttributeConfig(List<? extends String> configList, Map<Attribute, Double> targetMap) {
        for (String entry : configList) {
            String[] parts = entry.split("=");
            if (parts.length >= 1) {
                try {
                    ResourceLocation loc = new ResourceLocation(parts[0].trim());
                    double value = 1.0;
                    if (parts.length >= 2) {
                        value = Double.parseDouble(parts[1].trim());
                    }

                    if (ForgeRegistries.ATTRIBUTES.containsKey(loc)) {
                        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(loc);
                        targetMap.put(attr, value);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public static double getSkillMultiplier(Object skillObject) {
        if (skillObject == null) return 1.0;
        if (skillMultiplierCache == null) loadCaches();
        String simpleName = skillObject.getClass().getSimpleName();
        if (skillMultiplierCache.containsKey(simpleName)) return skillMultiplierCache.get(simpleName);
        String objectName = skillObject.getClass().getName();
        for (Map.Entry<String, Double> entry : skillMultiplierCache.entrySet()) {
            if (objectName.contains(entry.getKey())) return entry.getValue();
        }
        return 1.0;
    }

    public static double getExtraAttributePanel(Player player) {
        if (player == null) return 0.0;
        if (flatAttributeCache == null) loadCaches();
        double extraDamage = 0.0;
        for (Map.Entry<Attribute, Double> entry : flatAttributeCache.entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());
            if (instance != null && instance.getValue() > 0) {
                extraDamage += (instance.getValue() * entry.getValue());
            }
        }
        return extraDamage;
    }

    public static Map<Attribute, Double> getMultiplierAttributeCache() {
        if (multiplierAttributeCache == null) loadCaches();
        return multiplierAttributeCache;
    }
}
