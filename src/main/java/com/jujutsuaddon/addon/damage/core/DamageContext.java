// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/core/DamageContext.java
package com.jujutsuaddon.addon.damage.core;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.balance.ability.AbilityBalancer;
import com.jujutsuaddon.addon.balance.character.CharacterBalancer;
import com.jujutsuaddon.addon.damage.cache.AttributeCache;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一伤害上下文
 *
 * ★★★ 四乘区版本（三乘区 + 独立属性）★★★
 */
public record DamageContext(
        LivingEntity attacker,
        @Nullable LivingEntity target,
        @Nullable Ability ability,
        float originalBaseDamage,

        boolean isHeavenlyRestriction,
        boolean isActuallyMelee,
        String roleKey,

        // 面板数据
        double vanillaFlat,
        double vanillaFinal,
        double weaponRatio,
        double totalPanel,
        double attackSpeed,
        double effectiveSpeed,

        // 职业/角色倍率
        double roleMultiplier,
        double preservation,
        double speedModifier,

        // ★ 四乘区数据 ★
        double externalAddition,      // 加法乘区
        double externalMultBase,      // 乘法乘区
        double externalMultTotal,     // 独立乘区
        double independentAttrMult,   // ★ 独立属性乘数（projectile_damage 等）

        // 兼容旧版
        double externalMultiplier,
        double baseMultiplier,
        double panelMultiplier,

        // 技能倍率
        float balancerMultiplier,
        double skillConfigMultiplier,

        // 暴击
        double critChance,
        double critDamage,

        // 全局
        double globalMultiplier,
        boolean enablePanelScaling,

        // 咒力输出
        float cursedEnergyOutput,

        // 详细贡献列表
        List<AttributeCache.MultiplierContribution> externalContributions,

        String dynamicMultInfo
) {

    // ==================== 工厂方法 ====================

    public static DamageContext forRealDamage(
            LivingEntity attacker,
            @Nullable LivingEntity target,
            @Nullable Ability ability,
            float originalBaseDamage,
            boolean isMelee) {

        return new Builder(attacker, ability, originalBaseDamage, isMelee)
                .withTarget(target)
                .build();
    }

    public static DamageContext forPrediction(
            LivingEntity attacker,
            Ability ability,
            float baseDamage,
            float skillMultiplier,
            float power) {

        float originalBaseDamage = baseDamage * skillMultiplier * power;
        boolean isMelee = ability.isMelee();

        return new Builder(attacker, ability, originalBaseDamage, isMelee)
                .withSilentMode()
                .build();
    }

    public static DamageContext forPrediction(LivingEntity attacker, Ability ability) {
        float power = ability.getPower(attacker);
        return forPrediction(attacker, ability, 1.0f, 1.0f, power);
    }

    // ==================== Builder ====================

    public static class Builder {
        private final LivingEntity attacker;
        private final Ability ability;
        private final float originalBaseDamage;
        private final boolean isMelee;

        private LivingEntity target = null;
        private boolean silentMode = false;

        // 计算结果
        private boolean isHeavenlyRestriction;
        private boolean isActuallyMelee;
        private String roleKey;
        private double vanillaFlat, vanillaFinal, weaponRatio, totalPanel;
        private double attackSpeed, effectiveSpeed;
        private double roleMultiplier, preservation, speedModifier;

        // ★ 四乘区 ★
        private double externalAddition;
        private double externalMultBase;
        private double externalMultTotal;
        private double independentAttrMult = 1.0;  // ★ 新增
        private double externalMultiplier, baseMultiplier, panelMultiplier;

        private float balancerMultiplier;
        private double skillConfigMultiplier;
        private double critChance, critDamage;
        private double globalMultiplier;
        private boolean enablePanelScaling;
        private float cursedEnergyOutput;
        private List<AttributeCache.MultiplierContribution> externalContributions = new ArrayList<>();

        public Builder(LivingEntity attacker, Ability ability,
                       float originalBaseDamage, boolean isMelee) {
            this.attacker = attacker;
            this.ability = ability;
            this.originalBaseDamage = originalBaseDamage;
            this.isMelee = isMelee;
        }

        public Builder withTarget(LivingEntity target) {
            this.target = target;
            return this;
        }

        public Builder withSilentMode() {
            this.silentMode = true;
            return this;
        }

        public DamageContext build() {
            collectAllParameters();

            return new DamageContext(
                    attacker, target, ability, originalBaseDamage,
                    isHeavenlyRestriction, isActuallyMelee, roleKey,
                    vanillaFlat, vanillaFinal, weaponRatio, totalPanel,
                    attackSpeed, effectiveSpeed,
                    roleMultiplier, preservation, speedModifier,
                    externalAddition, externalMultBase, externalMultTotal, independentAttrMult,
                    externalMultiplier, baseMultiplier, panelMultiplier,
                    balancerMultiplier, skillConfigMultiplier,
                    critChance, critDamage,
                    globalMultiplier, enablePanelScaling,
                    cursedEnergyOutput,
                    externalContributions,
                    ""
            );
        }

        private void collectAllParameters() {
            ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE)
                    .resolve().orElse(null);

            isHeavenlyRestriction = (cap != null && cap.hasTrait(Trait.HEAVENLY_RESTRICTION));
            isActuallyMelee = isMelee && !(ability instanceof Ability.IAttack);

            enablePanelScaling = AddonConfig.COMMON.enableAttackDamageScaling.get();
            globalMultiplier = AddonConfig.COMMON.globalDamageMultiplier.get();

            collectAttackDamageData();
            collectAttackSpeedData();
            collectRoleMultipliers();
            collectExternalMultiplier();
            collectSkillMultipliers();
            collectCritData();
            collectCursedEnergyOutput(cap);

            roleKey = buildRoleKey();
        }

        private void collectAttackDamageData() {
            AttributeInstance atkAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atkAttr != null) {
                vanillaFlat = atkAttr.getBaseValue();
                if (vanillaFlat < 1.0) vanillaFlat = 1.0;
            } else {
                vanillaFlat = 1.0;
            }
            vanillaFinal = vanillaFlat;
            weaponRatio = 1.0;

            double modExtraFlat = 0.0;
            if (attacker instanceof Player player) {
                modExtraFlat = AttributeCache.getExtraAttributePanel(player);
            }

            totalPanel = vanillaFlat + modExtraFlat;
        }

        private void collectAttackSpeedData() {
            AttributeInstance speedAttr = attacker.getAttribute(Attributes.ATTACK_SPEED);
            attackSpeed = (speedAttr != null) ? speedAttr.getValue() : 4.0;
            effectiveSpeed = Math.max(4.0, attackSpeed);

            double speedScaling = isHeavenlyRestriction ?
                    AddonConfig.COMMON.hrAttackSpeedScaling.get() :
                    AddonConfig.COMMON.sorcererAttackSpeedScaling.get();

            speedModifier = 1.0 + (effectiveSpeed - 1.0) * speedScaling;
        }

        private void collectRoleMultipliers() {
            if (isHeavenlyRestriction) {
                roleMultiplier = AddonConfig.COMMON.hrMeleeMultiplier.get();
                preservation = AddonConfig.COMMON.hrMeleePreservation.get();
            } else {
                roleMultiplier = isActuallyMelee ?
                        CharacterBalancer.getMeleeMultiplier(attacker) :
                        CharacterBalancer.getTechniqueMultiplier(attacker);

                preservation = isActuallyMelee ?
                        CharacterBalancer.getMeleePreservation(attacker) :
                        CharacterBalancer.getTechniquePreservation(attacker);
            }
        }

        /**
         * ★ 核心：收集四乘区数据 ★
         */
        private void collectExternalMultiplier() {
            AttributeCache.ExternalMultiplierResult result =
                    AttributeCache.calculateExternalMultiplierDetailed(attacker, isActuallyMelee);

            // 四乘区原始数据
            externalAddition = result.additionSum();
            externalMultBase = result.multiplyBaseSum();
            externalMultTotal = result.multiplyTotalProd();
            independentAttrMult = result.independentAttrMult();

            // ★ NaN 保护 ★
            if (Double.isNaN(externalAddition)) externalAddition = 0.0;
            if (Double.isNaN(externalMultBase)) externalMultBase = 0.0;
            if (Double.isNaN(externalMultTotal)) externalMultTotal = 1.0;
            if (Double.isNaN(independentAttrMult)) independentAttrMult = 1.0;

            // 贡献详情
            externalContributions = result.contributions();

            // 兼容旧版：计算综合倍率（包含独立属性）
            externalMultiplier = (1.0 + externalMultBase) * externalMultTotal * independentAttrMult;
            baseMultiplier = externalMultiplier;
            panelMultiplier = externalMultiplier;
        }

        private void collectSkillMultipliers() {
            balancerMultiplier = 1.0f;
            skillConfigMultiplier = 1.0;

            if (ability != null && !isActuallyMelee) {
                if (silentMode) {
                    balancerMultiplier = AbilityBalancer.getDamageMultiplierSilent(ability, attacker);
                } else {
                    balancerMultiplier = AbilityBalancer.getDamageMultiplier(ability, attacker);
                }
                skillConfigMultiplier = AttributeCache.getSkillMultiplier(ability);
            }
        }

        private void collectCritData() {
            if (silentMode) {
                critChance = AttributeCache.getCritChanceSilent(attacker);
                critDamage = AttributeCache.getCritDamageSilent(attacker);
            } else {
                critChance = AttributeCache.getCritChance(attacker);
                critDamage = AttributeCache.getCritDamage(attacker);
            }
        }

        private void collectCursedEnergyOutput(@Nullable ISorcererData cap) {
            if (cap != null) {
                this.cursedEnergyOutput = cap.getOutput();
            } else {
                this.cursedEnergyOutput = 1.0f;
            }
        }

        private String buildRoleKey() {
            if (isHeavenlyRestriction) return "hr";
            String roleName = CharacterBalancer.getSpecialRoleName(attacker);
            return roleName + (isActuallyMelee ? "_melee" : "_tech");
        }
    }
}
