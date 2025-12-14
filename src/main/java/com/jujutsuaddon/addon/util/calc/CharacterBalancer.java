package com.jujutsuaddon.addon.util.calc;

import com.jujutsuaddon.addon.AddonConfig;
import net.minecraft.world.entity.LivingEntity;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

public class CharacterBalancer {

    // ==========================================
    // 体术 (Melee)
    // ==========================================

    public static double getMeleeMultiplier(LivingEntity attacker) {
        if (attacker == null) return 1.0;
        ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return 1.0;

        // 1. 幻兽琥珀 (优先级最高)
        if (cap.hasToggled(JJKAbilities.MYTHICAL_BEAST_AMBER.get())) {
            return AddonConfig.COMMON.mbaMeleeMultiplier.get();
        }

        CursedTechnique technique = cap.getTechnique();

        // 2. 无术式 (包括常态鹿紫云一)
        if (technique == null) {
            return AddonConfig.COMMON.noTechniqueMeleeMultiplier.get();
        }

        // 3. 东堂葵
        if (technique == CursedTechnique.BOOGIE_WOOGIE) {
            return AddonConfig.COMMON.todoMeleeMultiplier.get();
        }

        // 4. 普通咒术师
        return AddonConfig.COMMON.sorcererMeleeMultiplier.get();
    }

    public static double getMeleePreservation(LivingEntity attacker) {
        if (attacker == null) return 1.0;
        ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return 1.0;

        if (cap.hasToggled(JJKAbilities.MYTHICAL_BEAST_AMBER.get())) {
            return AddonConfig.COMMON.mbaMeleePreservation.get();
        }

        CursedTechnique technique = cap.getTechnique();

        if (technique == null) {
            return AddonConfig.COMMON.noTechniqueMeleePreservation.get();
        }

        if (technique == CursedTechnique.BOOGIE_WOOGIE) {
            return AddonConfig.COMMON.todoMeleePreservation.get();
        }

        return AddonConfig.COMMON.sorcererMeleePreservation.get();
    }

    // ==========================================
    // 术式 (Technique)
    // ==========================================

    public static double getTechniqueMultiplier(LivingEntity attacker) {
        if (attacker == null) return 1.0;
        ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return 1.0;

        if (cap.hasToggled(JJKAbilities.MYTHICAL_BEAST_AMBER.get())) {
            return AddonConfig.COMMON.mbaTechniqueMultiplier.get();
        }

        CursedTechnique technique = cap.getTechnique();

        // 无术式者理论上没有术式伤害，但如果有（比如通过指令获得），给个默认值
        if (technique == null) {
            return AddonConfig.COMMON.sorcererTechniqueMultiplier.get();
        }

        if (technique == CursedTechnique.BOOGIE_WOOGIE) {
            return AddonConfig.COMMON.todoTechniqueMultiplier.get();
        }

        return AddonConfig.COMMON.sorcererTechniqueMultiplier.get();
    }

    public static double getTechniquePreservation(LivingEntity attacker) {
        if (attacker == null) return 1.0;
        ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return 1.0;

        if (cap.hasToggled(JJKAbilities.MYTHICAL_BEAST_AMBER.get())) {
            return AddonConfig.COMMON.mbaTechniquePreservation.get();
        }

        CursedTechnique technique = cap.getTechnique();

        if (technique == null) {
            return AddonConfig.COMMON.sorcererTechniquePreservation.get();
        }

        if (technique == CursedTechnique.BOOGIE_WOOGIE) {
            return AddonConfig.COMMON.todoTechniquePreservation.get();
        }

        return AddonConfig.COMMON.sorcererTechniquePreservation.get();
    }

    // 辅助方法：判断是否是特殊角色（用于 Debug 显示名称）
    public static String getSpecialRoleName(LivingEntity attacker) {
        if (attacker == null) return "unknown";
        ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return "unknown";

        if (cap.hasToggled(JJKAbilities.MYTHICAL_BEAST_AMBER.get())) return "mba";
        CursedTechnique technique = cap.getTechnique();
        if (technique == null) return "no_tech";
        if (technique == CursedTechnique.BOOGIE_WOOGIE) return "todo";

        return "sorcerer";
    }
}
