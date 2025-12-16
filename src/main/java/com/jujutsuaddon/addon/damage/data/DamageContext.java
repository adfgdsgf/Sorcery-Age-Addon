package com.jujutsuaddon.addon.damage.data;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
import radon.jujutsu_kaisen.ability.base.Summon;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 伤害计算所需的上下文数据
 *
 * 设计目的：
 * 1. 解耦数据收集和计算逻辑
 * 2. 支持单元测试（可以手动构造）
 * 3. 避免在计算过程中重复获取属性
 */
public record DamageContext(
        // 技能数据
        @Nullable Float baseDamage,       // 技能基础伤害
        float skillMultiplier,            // 技能内置倍率
        float power,                      // getPower() 结果
        AbilityType abilityType,          // 技能类型
        boolean isMelee,                  // isMelee()
        boolean isAttackAbility,          // IAttack
        Ability.Classification classification,  // 分类

        // 玩家属性
        double attackDamage,              // 面板攻击力（已扣除JJK加成）
        double attackSpeed,               // 攻速

        // 角色/职业数据
        double techniqueMultiplier,       // 术式倍率
        double meleeMultiplier,           // 近战倍率
        double techniquePreservation,     // 术式保留率
        double meleePreservation,         // 近战保留率

        // 外部倍率
        double externalMultiplier,        // 外部属性倍率
        float balancerMultiplier,         // 动态平衡器倍率
        double skillConfigMultiplier,     // 技能配置倍率
        double critMultiplier,            // 暴击倍率

        // 全局配置
        double globalMultiplier,          // 全局伤害倍率
        double attackSpeedScaling,        // 攻速缩放
        boolean enableAttackDamageScaling // 是否启用面板缩放
) {

    private static final UUID JJK_ATTACK_DAMAGE_UUID =
            UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");

    public enum AbilityType {
        DIRECT_DAMAGE,  // 有明确 DAMAGE 字段
        POWER_BASED,    // 基于 power
        SUMMON,         // 召唤物
        DOMAIN,         // 领域展开
        UTILITY         // 功能性，无伤害
    }

    // ==================== 工厂方法 ====================

    /**
     * 从实体和技能创建上下文
     */
    public static DamageContext create(LivingEntity entity, Ability ability) {
        // 获取技能数据
        AbilityDamageData.CachedData data = AbilityDamageData.get(ability);
        AbilityType type = detectType(ability, data);

        // 获取玩家属性
        double attackDamage = getCleanAttackDamage(entity);
        double attackSpeed = getAttackSpeed(entity);

        // 判断缩放类型
        boolean useTechniqueScaling = shouldUseTechniqueScaling(ability);

        // 获取角色倍率（这些方法你已有）
        double techniqueMultiplier = com.jujutsuaddon.addon.util.calc.CharacterBalancer.getTechniqueMultiplier(entity);
        double meleeMultiplier = com.jujutsuaddon.addon.util.calc.CharacterBalancer.getMeleeMultiplier(entity);
        double techniquePreservation = com.jujutsuaddon.addon.util.calc.CharacterBalancer.getTechniquePreservation(entity);
        double meleePreservation = com.jujutsuaddon.addon.util.calc.CharacterBalancer.getMeleePreservation(entity);

        // 外部倍率
        double externalMultiplier = com.jujutsuaddon.addon.util.calc.DamageUtil.calculateExternalMultiplier(
                entity, !useTechniqueScaling, false, true);

        // 平衡器
        float balancerMultiplier = useTechniqueScaling ?
                com.jujutsuaddon.addon.util.calc.AbilityBalancer.getDamageMultiplierSilent(ability, entity) : 1.0f;

        // 技能配置倍率
        double skillConfigMultiplier = com.jujutsuaddon.addon.util.calc.DamageUtil.getSkillMultiplier(ability);

        // 暴击倍率
        double critMultiplier = com.jujutsuaddon.addon.util.calc.DamageUtil.getCritDamageMultiplier(entity);

        // 全局配置
        double globalMultiplier = com.jujutsuaddon.addon.AddonConfig.COMMON.globalDamageMultiplier.get();
        double attackSpeedScaling = com.jujutsuaddon.addon.AddonConfig.COMMON.sorcererAttackSpeedScaling.get();
        boolean enableAttackDamageScaling = com.jujutsuaddon.addon.AddonConfig.COMMON.enableAttackDamageScaling.get();

        return new DamageContext(
                data.baseDamage(),
                data.multiplier(),
                ability.getPower(entity),
                type,
                ability.isMelee(),
                ability instanceof Ability.IAttack,
                ability.getClassification(),
                attackDamage,
                attackSpeed,
                techniqueMultiplier,
                meleeMultiplier,
                techniquePreservation,
                meleePreservation,
                externalMultiplier,
                balancerMultiplier,
                skillConfigMultiplier,
                critMultiplier,
                globalMultiplier,
                attackSpeedScaling,
                enableAttackDamageScaling
        );
    }

    // ==================== 辅助判断 ====================

    /**
     * 判断是否使用术式缩放
     */
    public boolean useTechniqueScaling() {
        // IAttack 虽然近战触发，但本质是术式伤害
        if (isAttackAbility) return true;
        // 纯近战用近战缩放
        if (isMelee) return false;
        // 其他都是术式
        return true;
    }

    /**
     * 获取有效的基础伤害
     */
    public float getEffectiveBaseDamage() {
        if (baseDamage != null && baseDamage > 0) {
            // 只返回 baseDamage * multiplier，power 单独处理
            return baseDamage * skillMultiplier;
        }
        // 没有 DAMAGE 字段，返回 1（power 会在后续乘）
        return 1.0f;
    }

    /**
     * 获取原版预期伤害（用于颜色比较）
     * = baseDamage * multiplier * power（不含 Addon 修正）
     */
    public float getVanillaDamage() {
        if (baseDamage != null && baseDamage > 0) {
            return baseDamage * skillMultiplier * power;
        }
        return power;
    }

    // ==================== 私有方法 ====================

    private static AbilityType detectType(Ability ability, AbilityDamageData.CachedData data) {
        if (ability instanceof Summon<?>) return AbilityType.SUMMON;
        if (ability instanceof DomainExpansion) return AbilityType.DOMAIN;

        // 有明确的 DAMAGE 字段
        if (data.baseDamage() != null && data.baseDamage() > 0) return AbilityType.DIRECT_DAMAGE;

        // ★★★ 新增：如果找到了投射物类，说明是伤害技能 ★★★
        if (data.projectileClass() != null) return AbilityType.POWER_BASED;

        if (ability instanceof Ability.IAttack) return AbilityType.POWER_BASED;
        if (ability instanceof Ability.IDomainAttack) return AbilityType.POWER_BASED;
        if (isDamageClassification(ability.getClassification())) return AbilityType.POWER_BASED;
        return AbilityType.UTILITY;
    }

    private static boolean isDamageClassification(Ability.Classification c) {
        return c == Ability.Classification.SLASHING ||
                c == Ability.Classification.FIRE ||
                c == Ability.Classification.WATER ||
                c == Ability.Classification.PLANTS ||
                c == Ability.Classification.BLUE ||
                c == Ability.Classification.LIGHTNING ||
                c == Ability.Classification.CURSED_SPEECH;
    }

    private static boolean shouldUseTechniqueScaling(Ability ability) {
        if (ability instanceof Ability.IAttack) return true;
        if (ability.isMelee()) return false;
        return true;
    }

    private static double getCleanAttackDamage(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) return 1.0;

        double value = attr.getValue();
        AttributeModifier jjkMod = attr.getModifier(JJK_ATTACK_DAMAGE_UUID);
        if (jjkMod != null) {
            value -= jjkMod.getAmount();
        }
        return Math.max(1.0, value);
    }

    private static double getAttackSpeed(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attributes.ATTACK_SPEED);
        return attr != null ? attr.getValue() : 4.0;
    }
}
