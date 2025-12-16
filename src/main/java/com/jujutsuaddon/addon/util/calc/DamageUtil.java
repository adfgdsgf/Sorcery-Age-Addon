package com.jujutsuaddon.addon.util.calc;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.helper.AttributeCommonHelper;
import com.jujutsuaddon.addon.util.helper.SummonScalingHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public static void reload() {
        skillMultiplierCache = null;
        flatAttributeCache = null;
        multiplierAttributeCache = null;
        CategoryResolver.reload();
        CategoryBenchmark.reload();
        SummonScalingHelper.clearCache();
        loadCaches();
    }

    public static void loadCaches() {
        if (skillMultiplierCache != null) return;

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

        flatAttributeCache = new HashMap<>();
        try {
            List<? extends String> flatConfig = AddonConfig.COMMON.extraAttributeScaling.get();
            parseAttributeConfig(flatConfig, flatAttributeCache);
        } catch (Exception ignored) {}

        multiplierAttributeCache = new HashMap<>();
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

    /**
     * ★★★ 新增：获取暴击伤害倍率 (用于预测器) ★★★
     */
    public static double getCritDamageMultiplier(LivingEntity entity) {
        try {
            // 尝试从 Helper 获取真实的暴击倍率 (通常是 1.5 或更高)
            return AttributeCommonHelper.getCritDamageSilent(entity);
        } catch (Throwable t) {
            return 1.5; // 默认保底
        }
    }

    public static double calculateExternalMultiplier(LivingEntity entity, boolean isMelee,
                                                     boolean includeCritExpectation, boolean silent) {
        if (multiplierAttributeCache == null) loadCaches();
        boolean isAdditiveMode = AddonConfig.COMMON.useAdditiveExternalAttributes.get();
        double externalMultiplier = 1.0;

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

        double atkPercentBonus = getAttackDamagePercent(entity);
        if (atkPercentBonus > 0) {
            if (isAdditiveMode) {
                externalMultiplier += atkPercentBonus;
            } else {
                externalMultiplier *= (1.0 + atkPercentBonus);
            }
        }

        if (includeCritExpectation && AddonConfig.COMMON.enableCritSystem.get()) {
            double critChance = silent ?
                    AttributeCommonHelper.getCritChanceSilent(entity) :
                    AttributeCommonHelper.getCritChance(entity);
            double critDamage = silent ?
                    AttributeCommonHelper.getCritDamageSilent(entity) :
                    AttributeCommonHelper.getCritDamage(entity);
            if (critChance > 0 && critDamage > 1.0) {
                double critExpectation = critChance * (critDamage - 1.0);
                if (isAdditiveMode) {
                    externalMultiplier += critExpectation;
                } else {
                    externalMultiplier *= (1.0 + critExpectation);
                }
            }
        }
        return Math.max(1.0, externalMultiplier);
    }

    private static final UUID JJK_ATTACK_DAMAGE_UUID = UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");
    private static double getAttackDamagePercent(LivingEntity entity) {
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

    public static double calculateExternalMultiplier(LivingEntity entity, boolean isMelee) {
        return calculateExternalMultiplier(entity, isMelee, false, false);
    }

    public static Map<Attribute, Double> getMultiplierAttributeCache() {
        if (multiplierAttributeCache == null) loadCaches();
        return multiplierAttributeCache;
    }
}
