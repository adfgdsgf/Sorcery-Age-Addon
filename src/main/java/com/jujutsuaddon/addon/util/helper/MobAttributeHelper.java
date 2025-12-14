package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.event.SorcererProtectionHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import radon.jujutsu_kaisen.util.EntityUtil;

import java.util.UUID;

public class MobAttributeHelper {

    public static final UUID ADDON_HEALTH_UUID = UUID.fromString("a8b9c0d1-e2f3-4a5b-6c7d-8e9f0a1b2c3d");

    /**
     * 转化模式：把血量转护甲，并抵消原模组的加血
     * @param totalExtraHealth 期望的总额外血量 (用于计算护甲)
     * @param correctionModifier 修正值 (负数，用于抵消原模组的加血)
     */
    public static void applyConvertedStats(LivingEntity mob, double totalExtraHealth, double correctionModifier, double armorRatio, double toughnessRatio) {
        if (!mob.isAlive()) return;

        // 1. 算护甲
        double armorToAdd = totalExtraHealth * armorRatio;
        double toughnessToAdd = totalExtraHealth * toughnessRatio;

        // 2. 加护甲
        updateModifier(mob, Attributes.ARMOR, SorcererProtectionHandler.CONVERTED_ARMOR_UUID, "Mob Converted Armor", armorToAdd);
        updateModifier(mob, Attributes.ARMOR_TOUGHNESS, SorcererProtectionHandler.CONVERTED_TOUGHNESS_UUID, "Mob Converted Toughness", toughnessToAdd);

        // 3. 修正血量 (把原模组加的血扣掉)
        // 这里的 correctionModifier 是 -jjkTheoreticalBonus，绝对不会导致 1 血
        if (Math.abs(correctionModifier) > 0.01) {
            EntityUtil.applyModifier(mob, Attributes.MAX_HEALTH, ADDON_HEALTH_UUID, "Health Correction (Conversion)", correctionModifier, AttributeModifier.Operation.ADDITION);
        } else {
            EntityUtil.removeModifier(mob, Attributes.MAX_HEALTH, ADDON_HEALTH_UUID);
        }

        // 4. 修正当前血量
        if (mob.getHealth() > mob.getMaxHealth()) {
            mob.setHealth(mob.getMaxHealth());
        }
    }

    /**
     * 正常血量模式 (补差价)
     * @param bonusToAdd 需要补的血量 (玩家理论加成 - JJK理论加成)
     */
    public static void applyNormalHealthStats(LivingEntity mob, double bonusToAdd) {
        if (!mob.isAlive()) return;

        // 1. 清理转化护甲
        AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(SorcererProtectionHandler.CONVERTED_ARMOR_UUID);
        AttributeInstance toughness = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) toughness.removeModifier(SorcererProtectionHandler.CONVERTED_TOUGHNESS_UUID);

        // 2. 应用补差价修饰符
        float oldMax = mob.getMaxHealth();

        if (Math.abs(bonusToAdd) > 0.1) {
            EntityUtil.applyModifier(mob, Attributes.MAX_HEALTH, ADDON_HEALTH_UUID, "Sorcerer Growth Bonus", bonusToAdd, AttributeModifier.Operation.ADDITION);
        } else {
            EntityUtil.removeModifier(mob, Attributes.MAX_HEALTH, ADDON_HEALTH_UUID);
        }

        // 3. 动态回血
        float newMax = mob.getMaxHealth();
        if (newMax > oldMax) {
            mob.setHealth(mob.getHealth() + (newMax - oldMax));
        }
    }

    private static void updateModifier(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID uuid, String name, double value) {
        AttributeInstance instance = entity.getAttribute(attr);
        if (instance != null) {
            AttributeModifier existing = instance.getModifier(uuid);
            if (existing == null || Math.abs(existing.getAmount() - value) > 0.1) {
                instance.removeModifier(uuid);
                if (value > 0.01) {
                    instance.addTransientModifier(new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }
}
