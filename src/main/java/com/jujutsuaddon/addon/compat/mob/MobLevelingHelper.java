package com.jujutsuaddon.addon.compat.mob;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.util.helper.MobAttributeHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.JujutsuType;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererGrade;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;
import radon.jujutsu_kaisen.config.ConfigHolder;
import radon.jujutsu_kaisen.entity.curse.base.PackCursedSpirit;
import radon.jujutsu_kaisen.util.EntityUtil;

import java.util.UUID;

public class MobLevelingHelper {
    private static final UUID CE_FLOW_ARMOR_UUID = UUID.fromString("c7b8f3f0-9c4f-4e76-b69f-dc2f3d94e7b8");
    private static final UUID CE_FLOW_ARMOR_TOUGHNESS_UUID = UUID.fromString("f3a3c0e2-bc9a-45a2-9219-0f1f6de65c17");
    private static final String NBT_LAST_GRADE_KEY = "jujutsu_addon_last_grade";

    /**
     * [通用安全方法] 获取属性值
     * @param defaultValue 如果属性不存在，返回此默认值 (防止 0 导致乘法问题)
     */
    private static double safeGetAttributeValue(LivingEntity entity, Attribute attribute, double defaultValue) {
        if (entity == null || attribute == null) return defaultValue;
        AttributeInstance instance = entity.getAttribute(attribute);
        return (instance != null) ? instance.getValue() : defaultValue;
    }

    /**
     * [通用安全方法] 获取属性基础值
     * @param defaultValue 如果属性不存在，返回此默认值
     */
    private static double safeGetAttributeBaseValue(LivingEntity entity, Attribute attribute, double defaultValue) {
        if (entity == null || attribute == null) return defaultValue;
        AttributeInstance instance = entity.getAttribute(attribute);
        return (instance != null) ? instance.getBaseValue() : defaultValue;
    }

    public static void handleLevelingAndStats(LivingEntity mob, ISorcererData cap) {
        if (mob.level().isClientSide || !mob.isAlive()) return;

        updateGrade(mob, cap);
        updateStats(mob, cap);
    }

    private static void updateGrade(LivingEntity mob, ISorcererData cap) {
        double currentExp = cap.getExperience();
        SorcererGrade calculatedGrade = SorcererGrade.GRADE_4;

        for (SorcererGrade grade : SorcererGrade.values()) {
            if (currentExp >= grade.getRequiredExperience()) {
                if (grade.getRequiredExperience() >= calculatedGrade.getRequiredExperience()) {
                    calculatedGrade = grade;
                }
            }
        }

        String lastGradeName = mob.getPersistentData().getString(NBT_LAST_GRADE_KEY);
        if (!lastGradeName.equals(calculatedGrade.name())) {
            cap.setGrade(calculatedGrade);
            mob.getPersistentData().putString(NBT_LAST_GRADE_KEY, calculatedGrade.name());
            cap.setEnergy(cap.getMaxEnergy());
        }
    }

    private static void updateStats(LivingEntity mob, ISorcererData cap) {
        float specialGradeExp = (float) SorcererGrade.SPECIAL_GRADE.getRequiredExperience();
        float progress = (float) (cap.getExperience() / specialGradeExp);

        float minArmor = ConfigHolder.SERVER.playerCEArmorMin.get().floatValue();
        float maxArmor = ConfigHolder.SERVER.playerCEArmorMax.get().floatValue();
        float armorScale = ConfigHolder.SERVER.playerCEArmor.get().floatValue();

        float armorBonus = minArmor + (armorScale * progress);
        float toughnessBonus = minArmor / 2 + ((armorScale * 0.575F) * progress);

        // 获取基础值时，如果没有该属性，返回 0.0 是合理的（没有护甲就是0护甲）
        double baseArmor = safeGetAttributeBaseValue(mob, Attributes.ARMOR, 0.0);
        double baseToughness = safeGetAttributeBaseValue(mob, Attributes.ARMOR_TOUGHNESS, 0.0);

        float maxToughness = maxArmor * 0.6F;

        if (baseArmor + armorBonus > maxArmor) armorBonus = Math.max(0, maxArmor - (float)baseArmor);
        if (baseToughness + toughnessBonus > maxToughness) toughnessBonus = Math.max(0, maxToughness - (float)baseToughness);

        if (mob.getAttribute(Attributes.ARMOR) != null) {
            EntityUtil.applyModifier(mob, Attributes.ARMOR, CE_FLOW_ARMOR_UUID, "CE Flow Armor Bonus", armorBonus, AttributeModifier.Operation.ADDITION);
        }
        if (mob.getAttribute(Attributes.ARMOR_TOUGHNESS) != null) {
            EntityUtil.applyModifier(mob, Attributes.ARMOR_TOUGHNESS, CE_FLOW_ARMOR_TOUGHNESS_UUID, "CE Flow Armor Toughness Bonus", toughnessBonus, AttributeModifier.Operation.ADDITION);
        }

        float power = cap.getRealPower();
        float npcMult = ConfigHolder.SERVER.npcHPMult.get().floatValue();
        float npcMin = ConfigHolder.SERVER.npcHPMin.get().floatValue();
        double jjkTheoreticalBonus = (Math.ceil(((power - 1.0F) * npcMult) / 20.0F) * 20.0F) + npcMin;

        float playerMult = ConfigHolder.SERVER.playerHPMult.get().floatValue();
        float playerMin = ConfigHolder.SERVER.playerHPMin.get().floatValue();
        double playerTheoreticalBonus = (Math.ceil(((power - 1.0F) * playerMult) / 20.0F) * 20.0F) + playerMin;

        boolean enableConversion = AddonConfig.COMMON.enableMobHealthToArmor.get();

        if (enableConversion) {
            double correctionModifier = -jjkTheoreticalBonus;
            boolean isHR = cap.hasTrait(Trait.HEAVENLY_RESTRICTION);
            double armorRatio = isHR ? AddonConfig.COMMON.mobHRHealthToArmorRatio.get() : AddonConfig.COMMON.mobHealthToArmorRatio.get();
            double toughnessRatio = isHR ? AddonConfig.COMMON.mobHRHealthToToughnessRatio.get() : AddonConfig.COMMON.mobHealthToToughnessRatio.get();
            MobAttributeHelper.applyConvertedStats(mob, playerTheoreticalBonus, correctionModifier, armorRatio, toughnessRatio);
        } else {
            double correctionModifier = playerTheoreticalBonus - jjkTheoreticalBonus;
            MobAttributeHelper.applyNormalHealthStats(mob, correctionModifier);
        }
    }

    public static float calculateStrength(LivingEntity entity) {
        float strength = entity.getMaxHealth() * 0.1F;
        float armor = (float) entity.getArmorValue();

        // 韧性如果没有，返回 0.0 是安全的，因为公式是 2.0 + toughness / 4.0，0不会导致除零
        float toughness = (float) safeGetAttributeValue(entity, Attributes.ARMOR_TOUGHNESS, 0.0);

        float f = 2.0F + toughness / 4.0F;
        float f1 = Mth.clamp(armor - strength / f, armor * 0.2F, 20.0F);
        strength /= 1.0F - f1 / 25.0F;

        MobEffectInstance instance = entity.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (instance != null) {
            int resistance = instance.getAmplifier();
            int i = (resistance + 1) * 5;
            int j = 25 - i;
            if (j == 0) return strength;
            else {
                float x = 25.0F / (float) j;
                strength = strength * x;
            }
        }

        // 攻击力如果没有，返回 1.0 (按照您的担忧，给一个保底值，虽然这里是加法，但给1也无伤大雅)
        // 主人，这里我给您填了 1.0，如果您觉得还是 0 好，随时可以改哦~
        strength += (float) safeGetAttributeValue(entity, Attributes.ATTACK_DAMAGE, 1.0);

        // 移速如果没有，返回 0.1 (给一点点基础分)
        strength += (float) safeGetAttributeValue(entity, Attributes.MOVEMENT_SPEED, 0.1);

        if (entity instanceof PackCursedSpirit pack) {
            strength += pack.getMinCount() + ((float) (pack.getMaxCount() - pack.getMinCount()) / 2);
        }

        if (entity.getCapability(SorcererDataHandler.INSTANCE).isPresent()) {
            ISorcererData cap = entity.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
            if (cap != null) {
                strength += cap.getExperience() * 0.1F;
                if (cap.getType() == JujutsuType.CURSE || cap.isUnlocked(JJKAbilities.RCT1.get())) {
                    strength *= 1.25F;
                }
            }
        }
        return strength;
    }

    public static void handleKillExperience(LivingEntity attacker, LivingEntity target, ISorcererData cap) {
        float targetStrength = calculateStrength(target) * 1.25F;
        float ownerStrength = calculateStrength(attacker) * 0.5F;
        float damageRatio = 1.0F;

        float expMultiplier = ConfigHolder.SERVER.experienceMultiplier.get().floatValue();
        float minExp = ConfigHolder.SERVER.minEXP.get().floatValue();
        float maxExp = ConfigHolder.SERVER.maxEXP.get().floatValue();

        float experience = Math.min(targetStrength * expMultiplier,
                (targetStrength - ownerStrength) * 5.0F * damageRatio * expMultiplier);

        if (experience < 0.1F && minExp == 0) return;

        experience = Math.max(minExp, experience);

        if (maxExp != 0) {
            if (cap.hasTrait(Trait.PRODIGY)) experience *= 2;
            experience = Math.min(maxExp, experience);
        }

        cap.addExperience(experience);

        float pointMultiplier = ConfigHolder.SERVER.pointMultiplier.get().floatValue();
        int minPoints = ConfigHolder.SERVER.minPoints.get();
        int maxPoints = ConfigHolder.SERVER.maxPoints.get();

        float rawExperience = Math.min(targetStrength, (targetStrength - ownerStrength) * 5.0F * damageRatio);
        int points = (int) Math.floor(rawExperience * 0.2F * pointMultiplier);

        points = Math.max(minPoints, points);

        if (maxPoints != 0) {
            if (cap.hasTrait(Trait.PRODIGY)) points *= 2;
            points = Math.min(maxPoints, points);
        }

        if (points > 0) cap.addPoints(points);

        cap.tick(attacker);
    }
}
