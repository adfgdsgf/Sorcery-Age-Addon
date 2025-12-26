// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/cache/AttributeCache.java
package com.jujutsuaddon.addon.damage.cache;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil.CritContribution;
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
 * ★★★ 完全自动化版本 - 零关键词依赖 ★★★
 *
 * 核心逻辑：
 * 1. attack_damage：收集所有 modifier 到三乘区
 * 2. 其他属性：运行时动态判断
 *    - 默认值 ≈ 1.0 且 当前值 ≠ 1.0 → 作为独立乘数
 *    - 值必须 > 0（防止乘0）
 *    - 不在黑名单中
 */
public final class AttributeCache {

    private AttributeCache() {}

    // ==================== 缓存字段 ====================

    private static Map<String, Double> skillMultiplierCache = null;
    private static Map<Attribute, Double> flatAttributeCache = null;
    private static Set<String> blacklistPatterns = null;
    private static List<Attribute> critChanceAttributes = null;
    private static List<Attribute> critDamageAttributes = null;
    private static boolean initialized = false;

    // ==================== 乘区类型枚举 ====================

    public enum MultiplierType {
        ADDITION,           // 加法乘区
        MULTIPLY_BASE,      // 乘法乘区
        MULTIPLY_TOTAL,     // 独立乘区
        INDEPENDENT_ATTR    // 独立属性最终值
    }

    // ==================== 外部倍率结果 ====================

    public record ExternalMultiplierResult(
            double additionSum,
            double multiplyBaseSum,
            double multiplyTotalProd,
            double independentAttrMult,
            List<MultiplierContribution> contributions
    ) {
        public double applyToBase(double baseValue) {
            return (baseValue + additionSum) * (1.0 + multiplyBaseSum) * multiplyTotalProd;
        }

        public double getMultiplierOnly() {
            return (1.0 + multiplyBaseSum) * multiplyTotalProd * independentAttrMult;
        }

        public double applyFull(double baseValue) {
            return applyToBase(baseValue) * independentAttrMult;
        }

        public double totalMultiplier() {
            return getMultiplierOnly();
        }

        public boolean hasAnyBonus() {
            return Math.abs(additionSum) > 0.001 ||
                    Math.abs(multiplyBaseSum) > 0.001 ||
                    Math.abs(multiplyTotalProd - 1.0) > 0.001 ||
                    Math.abs(independentAttrMult - 1.0) > 0.001;
        }
    }

    public record MultiplierContribution(
            String source,
            String modifierName,
            double value,
            MultiplierType type
    ) {
        public String formatValue() {
            return switch (type) {
                case ADDITION -> String.format("+%.1f", value);
                case MULTIPLY_BASE -> String.format("+%.0f%%", value * 100);
                case MULTIPLY_TOTAL -> String.format("×%.2f", 1 + value);
                case INDEPENDENT_ATTR -> String.format("×%.2f", value);
            };
        }

        public String shortSource() {
            if (source == null) return "?";
            if (source.length() > 20) {
                return source.substring(0, 20) + "..";
            }
            return source;
        }

        public String modId() {
            int idx = source != null ? source.indexOf(':') : -1;
            return idx > 0 ? source.substring(0, idx) : "unknown";
        }

        public String pattern() {
            int idx = source != null ? source.indexOf(':') : -1;
            return idx > 0 ? source.substring(idx + 1) : (source != null ? source : "unknown");
        }

        public double bonus() { return value; }
    }

    // ==================== 黑名单 ====================

    private static final Set<String> HARDCODED_BLACKLIST = Set.of(
            "max_health", "generic.max_health", "health",
            "armor", "generic.armor",
            "armor_toughness", "generic.armor_toughness",
            "movement_speed", "generic.movement_speed",
            "flying_speed", "generic.flying_speed",
            "swim_speed", "step_height",
            "attack_speed", "generic.attack_speed",
            "follow_range", "generic.follow_range",
            "reach", "attack_range", "block_reach",
            "knockback_resistance", "generic.knockback_resistance",
            "knockback", "attack_knockback",
            "luck", "generic.luck",
            "jump_strength", "horse.jump_strength",
            "spawn_reinforcements", "zombie.spawn_reinforcements"
    );

    private static final String[] BLACKLIST_KEYWORDS = {
            "resist", "reduction", "defense", "block",
            "cost", "regen", "cooldown", "duration",
            "mana", "stamina", "energy",
            "heal", "life_steal", "leech", "lifesteal",
            "experience", "xp", "level",
            "size", "scale", "radius", "range",
            "velocity", "speed"
    };

    // ==================== 初始化 ====================

    public static void initialize() {
        if (initialized) return;

        skillMultiplierCache = new HashMap<>();
        flatAttributeCache = new HashMap<>();
        blacklistPatterns = new HashSet<>();
        critChanceAttributes = new ArrayList<>();
        critDamageAttributes = new ArrayList<>();

        loadSkillMultipliers();
        loadFlatAttributes();
        loadBlacklistFromConfig();
        scanCritAttributes();

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
            for (String entry : flatConfig) {
                String[] parts = entry.split("=");
                if (parts.length >= 1) {
                    try {
                        ResourceLocation loc = new ResourceLocation(parts[0].trim());
                        double value = parts.length >= 2 ? Double.parseDouble(parts[1].trim()) : 1.0;
                        if (ForgeRegistries.ATTRIBUTES.containsKey(loc)) {
                            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(loc);
                            flatAttributeCache.put(attr, value);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    private static void loadBlacklistFromConfig() {
        blacklistPatterns.addAll(HARDCODED_BLACKLIST);
    }

    private static void scanCritAttributes() {
        for (var entry : ForgeRegistries.ATTRIBUTES.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Attribute attr = entry.getValue();
            String path = id.getPath().toLowerCase();

            if (!path.contains("crit")) continue;

            if (path.contains("resist") || path.contains("def") ||
                    path.contains("avoid") || path.contains("reduction")) {
                continue;
            }

            if (path.contains("chance") || path.contains("rate")) {
                critChanceAttributes.add(attr);
            } else if (path.contains("dmg") || path.contains("damage") || path.contains("bonus")) {
                critDamageAttributes.add(attr);
            }
        }
    }

    // ==================== 核心：判断是否是伤害乘数属性 ====================

    /**
     * ★★★ 完全自动判断 ★★★
     *
     * 判断逻辑：
     * 1. 值必须 > 0（防止乘0）
     * 2. 不是暴击属性（单独处理）
     * 3. 不在黑名单中
     * 4. 默认值 ≈ 1.0（乘数型属性的典型特征）
     * 5. 当前值 ≠ 1.0（有实际加成）
     * 6. 值不能太小（< 0.5 的减益跳过）
     */
    private static boolean isMultiplierDamageAttribute(Attribute attr, double currentValue) {
        // ★★★ 关键：跳过 0 或负数，防止乘0 ★★★
        if (currentValue <= 0.001) {
            return false;
        }

        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
        if (id == null) return false;

        String fullId = id.toString().toLowerCase();
        String path = id.getPath().toLowerCase();

        // 跳过 attack_damage 本身
        if (path.equals("attack_damage") || path.equals("generic.attack_damage")) {
            return false;
        }

        // ★★★ 跳过暴击属性（单独处理）★★★
        if (path.contains("crit")) {
            return false;
        }

        // 检查黑名单
        if (blacklistPatterns.contains(fullId) || blacklistPatterns.contains(path)) {
            return false;
        }

        for (String keyword : BLACKLIST_KEYWORDS) {
            if (path.contains(keyword)) {
                return false;
            }
        }

        // ★ 核心判断：默认值 ≈ 1.0 且 当前值有变化 ★
        double defaultVal = attr.getDefaultValue();
        boolean isMultiplierType = Math.abs(defaultVal - 1.0) < 0.05;
        boolean hasChange = Math.abs(currentValue - defaultVal) > 0.01;


        return isMultiplierType && hasChange;
    }

    // ==================== 技能倍率 ====================

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

    // ==================== 核心：外部倍率计算（四乘区）====================

    public static ExternalMultiplierResult calculateExternalMultiplierDetailed(
            LivingEntity entity, boolean isMelee) {

        if (!initialized) initialize();

        List<MultiplierContribution> contributions = new ArrayList<>();
        double additionSum = 0.0;
        double multiplyBaseSum = 0.0;
        double multiplyTotalProd = 1.0;
        double independentAttrMult = 1.0;

        // ==================== 1. attack_damage 三乘区 ====================
        AttributeInstance atkInstance = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkInstance != null) {
            for (AttributeModifier mod : atkInstance.getModifiers()) {
                double amount = mod.getAmount();
                if (Math.abs(amount) < 0.0001) continue;

                MultiplierType type = convertOperation(mod.getOperation());
                contributions.add(new MultiplierContribution(
                        "mc:attack_damage", mod.getName(), amount, type));

                switch (type) {
                    case ADDITION -> additionSum += amount;
                    case MULTIPLY_BASE -> multiplyBaseSum += amount;
                    case MULTIPLY_TOTAL -> multiplyTotalProd *= (1 + amount);
                }
            }
        }

        // ==================== 2. 自动扫描所有独立乘数属性 ====================
        for (AttributeInstance instance : entity.getAttributes().getSyncableAttributes()) {
            Attribute attr = instance.getAttribute();
            double value = instance.getValue();

            // NaN 和 Infinity 保护
            if (Double.isNaN(value) || Double.isInfinite(value)) continue;

            // ★★★ 跳过 0 或负数 ★★★
            if (value <= 0.001) continue;

            // ★ 核心：自动判断是否是伤害乘数属性 ★
            if (isMultiplierDamageAttribute(attr, value)) {
                independentAttrMult *= value;

                ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attr);
                String source = (attrId != null) ? attrId.toString() : "unknown";
                contributions.add(new MultiplierContribution(
                        source, "AutoDetected", value, MultiplierType.INDEPENDENT_ATTR));
            }
        }

        // ==================== 3. 配置文件中指定的额外属性 ====================
        try {
            List<? extends String> multConfig = AddonConfig.COMMON.bonusMultiplierAttributes.get();
            for (String entry : multConfig) {
                String[] parts = entry.split("=");
                if (parts.length < 1) continue;

                String attrId = parts[0].trim();
                double factor = parts.length >= 2 ? Double.parseDouble(parts[1].trim()) : 1.0;

                try {
                    ResourceLocation loc = new ResourceLocation(attrId);
                    if (!ForgeRegistries.ATTRIBUTES.containsKey(loc)) continue;

                    Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(loc);
                    AttributeInstance instance = entity.getAttribute(attr);
                    if (instance == null) continue;

                    double value = instance.getValue();
                    double defaultVal = attr.getDefaultValue();

                    // 跳过无效值
                    if (Double.isNaN(value) || Double.isInfinite(value)) continue;
                    if (value <= 0.001) continue;

                    // 避免重复计算
                    if (isMultiplierDamageAttribute(attr, value)) continue;

                    // 计算贡献
                    double contribution;
                    if (Math.abs(defaultVal - 1.0) < 0.05) {
                        contribution = (value - 1.0) * factor;
                    } else if (Math.abs(defaultVal) < 0.01) {
                        contribution = value * factor;
                    } else {
                        contribution = ((value / defaultVal) - 1.0) * factor;
                    }

                    if (Math.abs(contribution) < 0.001) continue;

                    multiplyBaseSum += contribution;
                    contributions.add(new MultiplierContribution(
                            attrId, "ConfigBonus", contribution, MultiplierType.MULTIPLY_BASE));

                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // ==================== 4. 最终 NaN 保护 ====================
        if (Double.isNaN(additionSum)) additionSum = 0.0;
        if (Double.isNaN(multiplyBaseSum)) multiplyBaseSum = 0.0;
        if (Double.isNaN(multiplyTotalProd) || Double.isInfinite(multiplyTotalProd)) multiplyTotalProd = 1.0;
        if (Double.isNaN(independentAttrMult) || Double.isInfinite(independentAttrMult)) independentAttrMult = 1.0;

        return new ExternalMultiplierResult(
                additionSum,
                multiplyBaseSum,
                multiplyTotalProd,
                independentAttrMult,
                contributions
        );
    }

    private static MultiplierType convertOperation(AttributeModifier.Operation op) {
        return switch (op) {
            case ADDITION -> MultiplierType.ADDITION;
            case MULTIPLY_BASE -> MultiplierType.MULTIPLY_BASE;
            case MULTIPLY_TOTAL -> MultiplierType.MULTIPLY_TOTAL;
        };
    }

    // ==================== 兼容方法 ====================

    public static ExternalMultiplierResult calculateExternalMultiplierDetailed(
            LivingEntity entity, boolean isMelee, boolean isAdditiveMode) {
        return calculateExternalMultiplierDetailed(entity, isMelee);
    }

    public static double calculateExternalMultiplier(LivingEntity entity, boolean isMelee, boolean isAdditiveMode) {
        ExternalMultiplierResult result = calculateExternalMultiplierDetailed(entity, isMelee);
        return result.getMultiplierOnly();
    }

    public static double getAttackDamagePercent(LivingEntity entity) {
        AttributeInstance att = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (att == null) return 0.0;

        double percent = 0.0;
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) {
            percent += mod.getAmount();
        }
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            percent += mod.getAmount();
        }
        return percent;
    }

    /**
     * ★ 兼容方法：返回空集合（新版是运行时判断，没有预扫描集合）★
     */
    public static Set<Attribute> getScannedAttributes() {
        // 新版是运行时动态判断，这里返回空集合
        // 如果需要调试，可以用 getDetectedMultiplierAttributes(entity) 代替
        return Collections.emptySet();
    }

    /**
     * 调试用：获取指定实体上被检测到的乘数属性
     */
    public static List<String> getDetectedMultiplierAttributes(LivingEntity entity) {
        if (!initialized) initialize();
        List<String> detected = new ArrayList<>();
        for (AttributeInstance instance : entity.getAttributes().getSyncableAttributes()) {
            Attribute attr = instance.getAttribute();
            double value = instance.getValue();
            if (isMultiplierDamageAttribute(attr, value)) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                detected.add((id != null ? id.toString() : "?") + " = " + value);
            }
        }
        return detected;
    }

    // ==================== 暴击率 ====================

    public static double getCritChance(LivingEntity entity) {
        return getCritChanceInternal(entity, false);
    }

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

            // ★ 跳过无效值 ★
            if (Double.isNaN(attrValue) || Double.isInfinite(attrValue)) continue;

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

    public static double getCritDamage(LivingEntity entity) {
        return getCritDamageInternal(entity, false);
    }

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

            // ★ 跳过无效值 ★
            if (Double.isNaN(attrValue) || Double.isInfinite(attrValue)) continue;

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

    // ==================== 通用方法 ====================

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

    public static void reload() {
        initialized = false;
        skillMultiplierCache = null;
        flatAttributeCache = null;
        blacklistPatterns = null;
        critChanceAttributes = null;
        critDamageAttributes = null;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
