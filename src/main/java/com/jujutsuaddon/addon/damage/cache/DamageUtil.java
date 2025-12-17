// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/cache/DamageUtil.java
package com.jujutsuaddon.addon.damage.cache;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.balance.ability.CategoryBenchmark;
import com.jujutsuaddon.addon.balance.ability.CategoryResolver;
import com.jujutsuaddon.addon.summon.SummonScalingHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * 伤害工具类 - 委托给 AttributeCache
 *
 * 保留此类是为了向后兼容，实际逻辑已移至 AttributeCache
 */
public class DamageUtil {

    public static void reload() {
        AttributeCache.reload();
        CategoryResolver.reload();
        CategoryBenchmark.reload();
        SummonScalingHelper.clearCache();
    }

    public static void loadCaches() {
        AttributeCache.initialize();
    }

    public static double getSkillMultiplier(Object skillObject) {
        return AttributeCache.getSkillMultiplier(skillObject);
    }

    public static double getExtraAttributePanel(Player player) {
        return AttributeCache.getExtraAttributePanel(player);
    }

    public static double getCritDamageMultiplier(LivingEntity entity) {
        try {
            return AttributeCache.getCritDamageSilent(entity);  // ★ 改这里
        } catch (Throwable t) {
            return 1.5;
        }
    }

    public static double calculateExternalMultiplier(LivingEntity entity, boolean isMelee,
                                                     boolean includeCritExpectation, boolean silent) {
        boolean isAdditiveMode = AddonConfig.COMMON.useAdditiveExternalAttributes.get();
        double externalMultiplier = AttributeCache.calculateExternalMultiplier(entity, isMelee, isAdditiveMode);

        // 暴击期望
        if (includeCritExpectation && AddonConfig.COMMON.enableCritSystem.get()) {
            // ★ 改这里：使用 AttributeCache
            double critChance = silent ?
                    AttributeCache.getCritChanceSilent(entity) :
                    AttributeCache.getCritChance(entity);
            double critDamage = silent ?
                    AttributeCache.getCritDamageSilent(entity) :
                    AttributeCache.getCritDamage(entity);

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

    public static double calculateExternalMultiplier(LivingEntity entity, boolean isMelee) {
        return calculateExternalMultiplier(entity, isMelee, false, false);
    }

    public static Map<Attribute, Double> getMultiplierAttributeCache() {
        return AttributeCache.getMultiplierAttributeCache();
    }
}
