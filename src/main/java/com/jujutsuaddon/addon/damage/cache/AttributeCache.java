// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/cache/AttributeCache.java
package com.jujutsuaddon.addon.damage.cache;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil.CritContribution;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * 统一属性缓存管理器
 *
 * 负责：
 * 1. 技能倍率配置缓存
 * 2. 额外属性面板缓存
 * 3. 倍率属性缓存（其他mod的伤害加成）
 * 4. 暴击属性缓存（暴击率/暴击伤害）
 */
public final class AttributeCache {

    private AttributeCache() {}

    private static final UUID JJK_ATTACK_DAMAGE_UUID =
            UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");

    // ==================== 缓存字段 ====================

    private static Map<String, Double> skillMultiplierCache = null;
    private static Map<Attribute, Double> flatAttributeCache = null;
    private static Map<Attribute, Double> multiplierAttributeCache = null;
    private static List<Attribute> critChanceAttributes = null;
    private static List<Attribute> critDamageAttributes = null;
    private static boolean initialized = false;

    // ==================== 扫描关键词 ====================

    private static final String[] AUTO_SCAN_KEYWORDS = {
            "magic_damage", "spell_power", "spell_damage", "mana_damage",
            "intelligence", "magic_attack",
            "projectile", "arrow", "ranged",
            "fire", "explosion", "cold", "lightning", "holy", "elemental",
            "all_damage", "all_attack", "bonus_damage", "extra_damage",
            "damage_bonus", "attack_bonus", "damage_dealt", "total_damage"
    };

    private static final String[] AUTO_SCAN_BLACKLIST = {
            "resistance", "reduction", "cost", "regen", "speed", "velocity"
    };

    private static final String[] CRIT_BLACKLIST = {
            "resis", "def", "avoid", "reduction"
    };

    // ==================== 初始化 ====================

    public static void initialize() {
        if (initialized) return;

        skillMultiplierCache = new HashMap<>();
        flatAttributeCache = new HashMap<>();
        multiplierAttributeCache = new HashMap<>();
        critChanceAttributes = new ArrayList<>();
        critDamageAttributes = new ArrayList<>();

        loadSkillMultipliers();
        loadFlatAttributes();
        loadMultiplierAttributes();
        loadCritAttributes();

        initialized = true;
    }

    private static void loadSkillMultipliers() {
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
    }

    private static void loadFlatAttributes() {
        try {
            List<? extends String> flatConfig = AddonConfig.COMMON.extraAttributeScaling.get();
            parseAttributeConfig(flatConfig, flatAttributeCache);
        } catch (Exception ignored) {}
    }

    private static void loadMultiplierAttributes() {
        // 自动扫描
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

        // 配置覆盖
        try {
            List<? extends String> multConfig = AddonConfig.COMMON.bonusMultiplierAttributes.get();
            parseAttributeConfig(multConfig, multiplierAttributeCache);
        } catch (Exception ignored) {}
    }

    /**
     * 扫描所有暴击相关属性
     */
    private static void loadCritAttributes() {
        for (Map.Entry<ResourceKey<Attribute>, Attribute> entry : ForgeRegistries.ATTRIBUTES.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Attribute attr = entry.getValue();
            String path = id.getPath().toLowerCase();

            // 必须包含 crit
            if (!path.contains("crit")) continue;

            // 排除防御/抵抗类
            boolean isBlacklisted = false;
            for (String block : CRIT_BLACKLIST) {
                if (path.contains(block)) {
                    isBlacklisted = true;
                    break;
                }
            }
            if (isBlacklisted) continue;

            // 分类
            if (path.contains("chance") || path.contains("rate")) {
                critChanceAttributes.add(attr);
            } else if (path.contains("dmg") || path.contains("damage") || path.contains("bonus")) {
                critDamageAttributes.add(attr);
            }
        }
    }

    private static void parseAttributeConfig(List<? extends String> configList, Map<Attribute, Double> targetMap) {
        for (String entry : configList) {
            String[] parts = entry.split("=");
            if (parts.length >= 1) {
                try {
                    ResourceLocation loc = new ResourceLocation(parts[0].trim());
                    double value = parts.length >= 2 ? Double.parseDouble(parts[1].trim()) : 1.0;

                    if (ForgeRegistries.ATTRIBUTES.containsKey(loc)) {
                        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(loc);
                        targetMap.put(attr, value);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ==================== 技能倍率 ====================

    /**
     * 获取技能配置倍率
     */
    public static double getSkillMultiplier(Object skillObject) {
        if (skillObject == null) return 1.0;
        if (!initialized) initialize();

        String simpleName = skillObject.getClass().getSimpleName();
        if (skillMultiplierCache.containsKey(simpleName)) {
            return skillMultiplierCache.get(simpleName);
        }

        String objectName = skillObject.getClass().getName();
        for (Map.Entry<String, Double> entry : skillMultiplierCache.entrySet()) {
            if (objectName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 1.0;
    }

    // ==================== 额外属性面板 ====================

    /**
     * 获取额外属性面板（固定加成）
     */
    public static double getExtraAttributePanel(Player player) {
        if (player == null) return 0.0;
        if (!initialized) initialize();

        double extraDamage = 0.0;
        for (Map.Entry<Attribute, Double> entry : flatAttributeCache.entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());
            if (instance != null && instance.getValue() > 0) {
                extraDamage += (instance.getValue() * entry.getValue());
            }
        }
        return extraDamage;
    }

    // ==================== 外部倍率计算 ====================

    /**
     * 计算外部倍率（其他mod的百分比加成）
     */
    public static double calculateExternalMultiplier(LivingEntity entity, boolean isMelee, boolean isAdditiveMode) {
        if (!initialized) initialize();

        double externalMultiplier = 1.0;

        // 攻击力百分比加成
        double atkPercentBonus = getAttackDamagePercent(entity);
        if (atkPercentBonus > 0 && isAdditiveMode) {
            externalMultiplier += atkPercentBonus;
        }

        // 术式才收集其他mod的属性加成
        if (!isMelee) {
            Map<String, Double> modMaxBonusMap = new HashMap<>();

            for (Map.Entry<Attribute, Double> entry : multiplierAttributeCache.entrySet()) {
                Attribute attr = entry.getKey();
                Double factor = entry.getValue();

                AttributeInstance instance = entity.getAttribute(attr);
                double currentVal = (instance != null) ? instance.getValue() : attr.getDefaultValue();
                double defaultVal = attr.getDefaultValue();
                double bonus = 0.0;

                if (Math.abs(defaultVal - 1.0) < 0.001) {
                    if (currentVal > 1.0) bonus = (currentVal - 1.0) * factor;
                } else if (Math.abs(defaultVal) < 0.001) {
                    if (currentVal > 0) bonus = currentVal * factor;
                } else {
                    if (currentVal > defaultVal) bonus = (currentVal - defaultVal) * factor;
                }

                if (bonus > 0) {
                    ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                    String modId = (id != null) ? id.getNamespace() : "unknown";
                    modMaxBonusMap.merge(modId, bonus, Math::max);
                }
            }

            for (double modBonus : modMaxBonusMap.values()) {
                if (isAdditiveMode) {
                    externalMultiplier += modBonus;
                } else {
                    externalMultiplier *= (1.0 + modBonus);
                }
            }
        }

        return Math.max(1.0, externalMultiplier);
    }

    /**
     * 获取攻击力百分比加成
     */
    public static double getAttackDamagePercent(LivingEntity entity) {
        AttributeInstance att = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (att == null) return 0.0;

        double percent = 0.0;
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) {
            if (!mod.getId().equals(JJK_ATTACK_DAMAGE_UUID)) percent += mod.getAmount();
        }
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            if (!mod.getId().equals(JJK_ATTACK_DAMAGE_UUID)) percent += mod.getAmount();
        }
        return percent;
    }

    /**
     * 获取倍率属性缓存（供 AbilityDamageCalculator 使用）
     */
    public static Map<Attribute, Double> getMultiplierAttributeCache() {
        if (!initialized) initialize();
        return multiplierAttributeCache;
    }

    // ==================== 暴击率 ====================

    /**
     * 获取暴击率
     */
    public static double getCritChance(LivingEntity entity) {
        return getCritChanceInternal(entity, false);
    }

    /**
     * 获取暴击率（静默模式，不输出日志）
     */
    public static double getCritChanceSilent(LivingEntity entity) {
        return getCritChanceInternal(entity, true);
    }

    private static double getCritChanceInternal(LivingEntity entity, boolean silent) {
        if (!initialized) initialize();

        double baseChance = AddonConfig.COMMON.baseCritChance.get();
        double chance = baseChance;
        List<CritContribution> contributions = silent ? null : new ArrayList<>();

        for (Attribute attr : critChanceAttributes) {
            AttributeInstance instance = entity.getAttribute(attr);
            if (instance == null) continue;

            double attrValue = instance.getValue();
            double defaultVal = attr.getDefaultValue();

            // 判断是否是乘法型属性（默认值 >= 0.5 说明是百分比形式）
            boolean isMultiplicative = defaultVal >= 0.5;
            double contribution = isMultiplicative ? (attrValue - defaultVal) : attrValue;

            if (Math.abs(contribution) < 0.0001) continue;

            chance += contribution;

            if (contributions != null) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                contributions.add(new CritContribution(
                        id != null ? id.toString() : "unknown",
                        contribution,
                        isMultiplicative
                ));
            }
        }

        double finalChance = Math.max(0.0, Math.min(chance, 1.0));

        if (!silent && entity instanceof Player player) {
            DamageDebugUtil.logCritChanceDetails(player, baseChance, contributions, finalChance);
        }

        return finalChance;
    }

    // ==================== 暴击伤害 ====================

    /**
     * 获取暴击伤害倍率
     */
    public static double getCritDamage(LivingEntity entity) {
        return getCritDamageInternal(entity, false);
    }

    /**
     * 获取暴击伤害倍率（静默模式，不输出日志）
     */
    public static double getCritDamageSilent(LivingEntity entity) {
        return getCritDamageInternal(entity, true);
    }

    private static double getCritDamageInternal(LivingEntity entity, boolean silent) {
        if (!initialized) initialize();

        double baseDamage = AddonConfig.COMMON.baseCritDamage.get();
        double dmg = baseDamage;
        List<CritContribution> contributions = silent ? null : new ArrayList<>();

        for (Attribute attr : critDamageAttributes) {
            AttributeInstance instance = entity.getAttribute(attr);
            if (instance == null) continue;

            double attrValue = instance.getValue();
            double defaultVal = attr.getDefaultValue();

            // 判断是否是乘法型属性（默认值 >= 1.0 说明是倍率形式）
            boolean isMultiplicative = defaultVal >= 1.0;
            double contribution = isMultiplicative ? (attrValue - defaultVal) : attrValue;

            if (contribution <= 0.0001) continue;

            dmg += contribution;

            if (contributions != null) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                contributions.add(new CritContribution(
                        id != null ? id.toString() : "unknown",
                        contribution,
                        isMultiplicative
                ));
            }
        }

        if (!silent && entity instanceof Player player) {
            DamageDebugUtil.logCritDamageDetails(player, baseDamage, contributions, dmg);
        }

        return dmg;
    }

    // ==================== 通用属性获取 ====================

    /**
     * 安全获取属性值
     */
    public static double safeGetAttribute(LivingEntity entity, Attribute attribute, double defaultValue) {
        if (entity == null || attribute == null) return defaultValue;
        AttributeInstance instance = entity.getAttribute(attribute);
        return (instance != null) ? instance.getValue() : defaultValue;
    }

    /**
     * 安全获取属性值（通过属性ID）
     */
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

    // ==================== 缓存管理 ====================

    /**
     * 重新加载缓存（配置变更时调用）
     */
    public static void reload() {
        initialized = false;
        skillMultiplierCache = null;
        flatAttributeCache = null;
        multiplierAttributeCache = null;
        critChanceAttributes = null;
        critDamageAttributes = null;
    }
}
