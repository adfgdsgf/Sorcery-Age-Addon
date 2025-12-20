// 文件路径: src/main/java/com/jujutsuaddon/addon/ability/limitless/Infinity/pressure/damage/PressureDamageCalculator.java
package com.jujutsuaddon.addon.ability.limitless.Infinity.pressure.damage;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.balance.character.CharacterBalancer;
import com.jujutsuaddon.addon.damage.cache.AttributeCache;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;

/**
 * 压力伤害计算器
 *
 * 接入统一伤害系统，复用：
 * - 角色倍率 (CharacterBalancer)
 * - 攻速修正
 * - 全局伤害倍率
 * - 暴击系统（可选）
 *
 * 公式：
 * finalDamage = pressureValue × roleMultiplier × speedModifier × globalMultiplier
 */
public final class PressureDamageCalculator {

    private PressureDamageCalculator() {}

    // ==================== 主计算方法 ====================

    /**
     * 计算压力伤害
     *
     * @param owner          施术者
     * @param target         目标
     * @param pressureValue  当前压力值（已经过 PressureCurve 处理）
     * @param pinnedTicks    被压制的tick数
     * @param blockCount     碰撞方块数
     * @param hardnessBonus  硬度加成
     * @return 最终伤害值
     */
    public static float calculate(
            LivingEntity owner,
            LivingEntity target,
            double pressureValue,
            int pinnedTicks,
            int blockCount,
            double hardnessBonus) {

        if (pressureValue <= 0) return 0f;

        // 1. 获取角色信息
        ISorcererData ownerCap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        boolean isHR = ownerCap != null && ownerCap.hasTrait(Trait.HEAVENLY_RESTRICTION);

        // 2. 获取角色倍率（压力伤害视为"术式"类型）
        double roleMultiplier;
        if (isHR) {
            roleMultiplier = AddonConfig.COMMON.hrMeleeMultiplier.get();
        } else {
            roleMultiplier = CharacterBalancer.getTechniqueMultiplier(owner);
        }

        // 3. 获取攻速修正
        double speedModifier = calculateSpeedModifier(owner, isHR);

        // 4. 基础伤害 = 压力值
        double baseDamage = pressureValue;

        // 5. 应用角色和攻速
        double damage = baseDamage * roleMultiplier * speedModifier;

        // 6. 压制时间加成（被压越久伤害越高）
        double pinnedBonus = calculatePinnedBonus(pinnedTicks);
        damage *= pinnedBonus;

        // 7. 方块数量加成
        double blockBonus = calculateBlockBonus(blockCount);
        damage *= blockBonus;

        // 8. 硬度加成
        damage *= hardnessBonus;

        // 9. 全局伤害倍率
        double globalMult = AddonConfig.COMMON.globalDamageMultiplier.get();
        damage *= globalMult;

        // 10. 伤害上限（使用配置）
        double maxDamage = PressureDamageConfig.getMaxDamagePerHit();
        damage = Math.min(damage, maxDamage);

        return (float) damage;
    }

    // ==================== 辅助计算 ====================

    private static double calculateSpeedModifier(LivingEntity owner, boolean isHR) {
        AttributeInstance speedAttr = owner.getAttribute(Attributes.ATTACK_SPEED);
        double attackSpeed = (speedAttr != null) ? speedAttr.getValue() : 4.0;
        double effectiveSpeed = Math.max(4.0, attackSpeed);

        double speedScaling = isHR ?
                AddonConfig.COMMON.hrAttackSpeedScaling.get() :
                AddonConfig.COMMON.sorcererAttackSpeedScaling.get();

        return 1.0 + (effectiveSpeed - 1.0) * speedScaling;
    }

    private static double calculatePinnedBonus(int pinnedTicks) {
        if (pinnedTicks <= 10) return 1.0;
        // 被压10tick后开始累积，最高+60%
        return 1.0 + Math.min((pinnedTicks - 10) * 0.015, 0.6);
    }

    private static double calculateBlockBonus(int blockCount) {
        // 多个方块压制时加成，最高+30%
        return 1.0 + Math.min(blockCount * 0.06, 0.3);
    }

    // ==================== 简化版本（向后兼容） ====================

    /**
     * 简化计算（不含压制时间等额外加成）
     */
    public static float calculateSimple(LivingEntity owner, double pressureValue) {
        return calculate(owner, null, pressureValue, 0, 1, 1.0);
    }
}
