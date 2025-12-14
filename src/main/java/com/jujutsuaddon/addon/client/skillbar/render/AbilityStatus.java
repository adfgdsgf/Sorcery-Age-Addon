package com.jujutsuaddon.addon.client.skillbar.render;

import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import com.jujutsuaddon.addon.util.helper.TenShadowsHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.ten_shadows.ITenShadowsData;
import radon.jujutsu_kaisen.capability.data.ten_shadows.TenShadowsDataHandler;

import javax.annotation.Nullable;

public class AbilityStatus {
    // 基础状态
    public boolean isToggleable = false;
    public boolean isSummon = false;
    public boolean isChanneled = false;
    public boolean isActive = false;
    public boolean hasSummon = false;
    public boolean canUse = true;
    public int cooldown = 0;
    public Ability.Status statusCode = Ability.Status.SUCCESS;

    // 冲突状态
    public boolean summonConflict = false;
    public boolean techniqueNotActive = false;

    // 技能信息
    public float cost = 0;
    public int maxCooldown = 0;

    // 十影调伏状态
    public boolean isTenShadowsSummon = false;
    public boolean isTamed = false;
    public boolean isDead = false;

    // ==================== 静态工厂方法 ====================

    /**
     * ★ 统一的状态构建方法 - 供所有地方使用 ★
     */
    public static AbilityStatus build(LocalPlayer player, Ability ability) {
        return build(player, ability, null, null);
    }

    /**
     * ★ 带术式信息的状态构建 ★
     */
    public static AbilityStatus build(LocalPlayer player, Ability ability,
                                      @Nullable CursedTechnique fromTechnique,
                                      @Nullable TechniqueHelper.TechniqueSource sourceType) {
        AbilityStatus status = new AbilityStatus();
        if (player == null || ability == null) return status;

        ISorcererData data = player.getCapability(SorcererDataHandler.INSTANCE).orElse(null);
        if (data == null) return status;

        // 基础类型
        Ability.ActivationType type = ability.getActivationType(player);
        status.isToggleable = type == Ability.ActivationType.TOGGLED;
        status.isSummon = ability instanceof Summon<?>;
        status.isChanneled = type == Ability.ActivationType.CHANNELED;
        status.isActive = data.hasToggled(ability);

        // 召唤物状态
        if (status.isSummon) {
            status.hasSummon = data.hasSummonOfClass(((Summon<?>) ability).getClazz());
        }

        // 基础可用性
        Ability.Status s = ability.getStatus(player);
        status.canUse = (s == Ability.Status.SUCCESS);
        status.statusCode = s;
        status.cooldown = data.getRemainingCooldown(ability);

        // 技能信息
        try {
            status.cost = ability.getCost(player);
            status.maxCooldown = ability.getRealCooldown(player);
        } catch (Exception e) {
            status.cost = 0;
            status.maxCooldown = 0;
        }

        // 十影召唤物特殊状态
        if (ability instanceof Summon<?> summon && summon.isTenShadows()) {
            status.isTenShadowsSummon = true;
            ITenShadowsData tenData = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
            if (tenData != null) {
                var registry = player.level().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
                boolean allTamed = true;
                boolean anyDead = false;
                for (EntityType<?> entityType : summon.getTypes()) {
                    if (!tenData.hasTamed(registry, entityType)) allTamed = false;
                    if (summon.canDie() && tenData.isDead(registry, entityType)) anyDead = true;
                }
                status.isTamed = allTamed;
                status.isDead = anyDead;
            }
        }

        // 召唤冲突检查
        if (TenShadowsHelper.isEnabled() && TenShadowsHelper.isAbilityModeSkill(ability)) {
            status.summonConflict = TenShadowsHelper.hasSummonConflict(player, ability);
            if (status.summonConflict) status.canUse = false;
        }

        // 术式激活检查
        if (fromTechnique != null && sourceType != null) {
            boolean isTechniqueActive = TechniqueHelper.isTechniqueActive(player, fromTechnique);
            if (!isTechniqueActive && !status.isActive && !status.hasSummon) {
                status.techniqueNotActive = true;
                status.canUse = false;
            }
        } else {
            // ★ 即使没传入 fromTechnique，也要检查！★
            status.techniqueNotActive = TechniqueHelper.hasAbilityTechniqueConflict(player, ability);
            if (status.techniqueNotActive) status.canUse = false;
        }

        return status;
    }

    // ==================== 便捷方法 ====================

    /**
     * 是否处于"激活"状态（开关开启或召唤物存在）
     */
    public boolean isOn() {
        return isActive || hasSummon;
    }

    /**
     * 是否完全不可用
     */
    public boolean isDisabled() {
        return !canUse && !isOn();
    }
}
