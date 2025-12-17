// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/core/DamageContext.java
package com.jujutsuaddon.addon.damage.core;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.balance.ability.AbilityBalancer;
import com.jujutsuaddon.addon.balance.character.CharacterBalancer;
import com.jujutsuaddon.addon.damage.cache.AttributeCache;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 统一伤害上下文
 */
public record DamageContext(
        LivingEntity attacker,
        @Nullable LivingEntity target,
        @Nullable Ability ability,
        float originalBaseDamage,

        boolean isHeavenlyRestriction,
        boolean isActuallyMelee,
        String roleKey,

        double vanillaFlat,
        double vanillaFinal,
        double weaponRatio,
        double totalPanel,
        double attackSpeed,
        double effectiveSpeed,

        double roleMultiplier,
        double preservation,
        double speedModifier,
        double externalMultiplier,
        double baseMultiplier,
        double panelMultiplier,

        float balancerMultiplier,
        double skillConfigMultiplier,

        double critChance,
        double critDamage,

        double globalMultiplier,
        boolean enablePanelScaling,
        boolean isAdditiveMode,

        String dynamicMultInfo
) {

    private static final UUID JJK_ATTACK_DAMAGE_UUID =
            UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");

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
        private double externalMultiplier, baseMultiplier, panelMultiplier;
        private float balancerMultiplier;
        private double skillConfigMultiplier;
        private double critChance, critDamage;
        private double globalMultiplier;
        private boolean enablePanelScaling, isAdditiveMode;

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
                    externalMultiplier, baseMultiplier, panelMultiplier,
                    balancerMultiplier, skillConfigMultiplier,
                    critChance, critDamage,
                    globalMultiplier, enablePanelScaling, isAdditiveMode,
                    ""
            );
        }

        private void collectAllParameters() {
            ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE)
                    .resolve().orElse(null);

            isHeavenlyRestriction = (cap != null && cap.hasTrait(Trait.HEAVENLY_RESTRICTION));
            isActuallyMelee = isMelee && !(ability instanceof Ability.IAttack);

            isAdditiveMode = AddonConfig.COMMON.useAdditiveExternalAttributes.get();
            enablePanelScaling = AddonConfig.COMMON.enableAttackDamageScaling.get();
            globalMultiplier = AddonConfig.COMMON.globalDamageMultiplier.get();

            collectAttackDamageData();
            collectAttackSpeedData();
            collectRoleMultipliers();
            collectExternalMultiplier();
            calculateMultipliers();
            collectSkillMultipliers();
            collectCritData();

            roleKey = buildRoleKey();
        }

        private void collectAttackDamageData() {
            AttributeInstance atkAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);

            if (atkAttr != null) {
                vanillaFlat = atkAttr.getValue();
                AttributeModifier jjkMod = atkAttr.getModifier(JJK_ATTACK_DAMAGE_UUID);
                if (jjkMod != null) {
                    vanillaFlat -= jjkMod.getAmount();
                    if (vanillaFlat < 1.0) vanillaFlat = 1.0;
                }
            } else {
                vanillaFlat = 1.0;
            }

            double percentBonus = AttributeCache.getAttackDamagePercent(attacker);

            if (isAdditiveMode) {
                vanillaFinal = vanillaFlat;
            } else {
                vanillaFinal = vanillaFlat * (1.0 + percentBonus);
            }

            weaponRatio = (vanillaFlat > 0.001) ? (vanillaFinal / vanillaFlat) : 1.0;

            double modExtraFlat = 0.0;
            if (attacker instanceof Player player) {
                modExtraFlat = AttributeCache.getExtraAttributePanel(player);
            }
            totalPanel = vanillaFinal + modExtraFlat;
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

        private void collectExternalMultiplier() {
            externalMultiplier = AttributeCache.calculateExternalMultiplier(
                    attacker, isActuallyMelee, isAdditiveMode);
        }

        private void calculateMultipliers() {
            if (isAdditiveMode) {
                baseMultiplier = weaponRatio + (externalMultiplier - 1.0);
            } else {
                baseMultiplier = weaponRatio * externalMultiplier;
            }
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

        // ★ 修改这里：使用 AttributeCache 替代 AttributeCommonHelper ★
        private void collectCritData() {
            if (silentMode) {
                critChance = AttributeCache.getCritChanceSilent(attacker);
                critDamage = AttributeCache.getCritDamageSilent(attacker);
            } else {
                critChance = AttributeCache.getCritChance(attacker);
                critDamage = AttributeCache.getCritDamage(attacker);
            }
        }

        private String buildRoleKey() {
            if (isHeavenlyRestriction) return "hr";
            String roleName = CharacterBalancer.getSpecialRoleName(attacker);
            return roleName + (isActuallyMelee ? "_melee" : "_tech");
        }
    }
}
