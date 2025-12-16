package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil.CritContribution;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用属性工具类
 * 自动兼容所有暴击 mod，无需硬编码
 */
public class AttributeCommonHelper {

    // =================================================================
    // 1. 暴击属性缓存（只缓存候选属性，实际使用时再检查）
    // =================================================================

    private static List<Attribute> CANDIDATE_CRIT_CHANCE_ATTRS = null;
    private static List<Attribute> CANDIDATE_CRIT_DAMAGE_ATTRS = null;

    /**
     * 扫描所有可能的暴击属性（只做一次）
     */
    public static void initCritAttributeCache() {
        if (CANDIDATE_CRIT_CHANCE_ATTRS != null) return;

        CANDIDATE_CRIT_CHANCE_ATTRS = new ArrayList<>();
        CANDIDATE_CRIT_DAMAGE_ATTRS = new ArrayList<>();

        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : ForgeRegistries.ATTRIBUTES.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Attribute attr = entry.getValue();
            String path = id.getPath().toLowerCase();

            if (path.contains("crit")) {
                // 排除防御/抵抗类
                if (path.contains("resis") || path.contains("def") || path.contains("avoid")) {
                    continue;
                }

                if (path.contains("chance") || path.contains("rate")) {
                    CANDIDATE_CRIT_CHANCE_ATTRS.add(attr);
                } else if (path.contains("dmg") || path.contains("damage") || path.contains("bonus")) {
                    CANDIDATE_CRIT_DAMAGE_ATTRS.add(attr);
                }
            }
        }
    }

    // =================================================================
    // 2. 通用属性获取
    // =================================================================

    public static double safeGetAttribute(LivingEntity entity, Attribute attribute, double defaultValue) {
        if (entity == null || attribute == null) return defaultValue;
        AttributeInstance instance = entity.getAttribute(attribute);
        return (instance != null) ? instance.getValue() : defaultValue;
    }

    public static double safeGetAttribute(LivingEntity entity, String attributeId, double defaultValue) {
        if (entity == null || attributeId == null) return defaultValue;
        try {
            ResourceLocation res = new ResourceLocation(attributeId);
            if (ForgeRegistries.ATTRIBUTES.containsKey(res)) {
                Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(res);
                return safeGetAttribute(entity, attr, defaultValue);
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    // =================================================================
    // 3. 暴击概率计算
    // =================================================================

    /**
     * 获取暴击概率
     * 自动检测实体实际拥有的暴击率属性
     */
    public static double getCritChance(LivingEntity entity) {
        return getCritChanceInternal(entity, false);
    }
    /**
     * 获取暴击概率（静默模式，用于预测/显示，不输出日志）
     */
    public static double getCritChanceSilent(LivingEntity entity)  {
        return getCritChanceInternal(entity, true);
    }
    private static double getCritChanceInternal(LivingEntity entity, boolean silent) {
        initCritAttributeCache();
        double baseChance = AddonConfig.COMMON.baseCritChance.get();
        double chance = baseChance;
        List<CritContribution> contributions = new ArrayList<>();
        for (Attribute attr : CANDIDATE_CRIT_CHANCE_ATTRS) {
            AttributeInstance instance = entity.getAttribute(attr);
            if (instance == null) continue;
            double attrValue = instance.getValue();
            double defaultVal = attr.getDefaultValue();
            boolean isMultiplicative = defaultVal >= 0.5;
            double contribution = isMultiplicative ? (attrValue - defaultVal) : attrValue;
            if (Math.abs(contribution) < 0.0001) continue;
            ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
            contributions.add(new CritContribution(
                    id != null ? id.toString() : "unknown",
                    contribution,
                    isMultiplicative
            ));
            chance += contribution;
        }
        double finalChance = Math.max(0.0, Math.min(chance, 1.0));
        // ★ 只有非静默模式才输出日志 ★
        if (!silent && entity instanceof Player player) {
            DamageDebugUtil.logCritChanceDetails(player, baseChance, contributions, finalChance);
        }
        return finalChance;
    }
    // =================================================================
// 4. 暴击伤害计算 - 同样添加静默参数
// =================================================================
    public static double getCritDamage(LivingEntity entity) {
        return getCritDamageInternal(entity, false);
    }
    public static double getCritDamageSilent(LivingEntity entity) {
        return getCritDamageInternal(entity, true);
    }
    private static double getCritDamageInternal(LivingEntity entity, boolean silent) {
        initCritAttributeCache();
        double baseDamage = AddonConfig.COMMON.baseCritDamage.get();
        double dmg = baseDamage;
        List<CritContribution> contributions = new ArrayList<>();
        for (Attribute attr : CANDIDATE_CRIT_DAMAGE_ATTRS) {
            AttributeInstance instance = entity.getAttribute(attr);
            if (instance == null) continue;
            double attrValue = instance.getValue();
            double defaultVal = attr.getDefaultValue();
            boolean isMultiplicative = defaultVal >= 1.0;
            double contribution = isMultiplicative ? (attrValue - defaultVal) : attrValue;
            if (contribution <= 0.0001) continue;
            ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
            contributions.add(new CritContribution(
                    id != null ? id.toString() : "unknown",
                    contribution,
                    isMultiplicative
            ));
            dmg += contribution;
        }
        // ★ 只有非静默模式才输出日志 ★
        if (!silent && entity instanceof Player player) {
            DamageDebugUtil.logCritDamageDetails(player, baseDamage, contributions, dmg);
        }
        return dmg;
    }
}
