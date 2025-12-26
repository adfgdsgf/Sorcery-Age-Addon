// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/cache/DamageUtil.java
package com.jujutsuaddon.addon.damage.cache;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.balance.ability.CategoryBenchmark;
import com.jujutsuaddon.addon.balance.ability.CategoryResolver;
import com.jujutsuaddon.addon.summon.SummonScalingHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

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
            return AttributeCache.getCritDamageSilent(entity);
        } catch (Throwable t) {
            return 1.5;
        }
    }

    /**
     * 计算外部倍率（三乘区版本）
     *
     * @param entity              实体
     * @param isMelee             是否近战
     * @param includeCritExpectation 是否包含暴击期望
     * @param silent              是否静默模式（不输出日志）
     * @return 综合倍率
     */
    public static double calculateExternalMultiplier(LivingEntity entity, boolean isMelee,
                                                     boolean includeCritExpectation, boolean silent) {
        // 使用三乘区系统
        AttributeCache.ExternalMultiplierResult result =
                AttributeCache.calculateExternalMultiplierDetailed(entity, isMelee);

        double externalMultiplier = result.getMultiplierOnly();

        // 暴击期望
        if (includeCritExpectation && AddonConfig.COMMON.enableCritSystem.get()) {
            double critChance = silent ?
                    AttributeCache.getCritChanceSilent(entity) :
                    AttributeCache.getCritChance(entity);
            double critDamage = silent ?
                    AttributeCache.getCritDamageSilent(entity) :
                    AttributeCache.getCritDamage(entity);

            if (critChance > 0 && critDamage > 1.0) {
                double critExpectation = critChance * (critDamage - 1.0);
                externalMultiplier *= (1.0 + critExpectation);
            }
        }

        return Math.max(1.0, externalMultiplier);
    }

    public static double calculateExternalMultiplier(LivingEntity entity, boolean isMelee) {
        return calculateExternalMultiplier(entity, isMelee, false, false);
    }

    /**
     * 获取扫描到的属性集合
     */
    public static Set<Attribute> getScannedAttributes() {
        return AttributeCache.getScannedAttributes();
    }

    /**
     * @deprecated 使用 getScannedAttributes() 代替
     */
    @Deprecated
    public static Set<Attribute> getMultiplierAttributeCache() {
        return AttributeCache.getScannedAttributes();
    }
}
