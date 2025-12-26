package com.jujutsuaddon.addon.client.skillbar.render;

import com.jujutsuaddon.addon.client.util.AbilityDamagePredictor;
import com.jujutsuaddon.addon.util.helper.TechniqueHelper;
import com.jujutsuaddon.addon.util.helper.tenshadows.TenShadowsHelper;
import com.jujutsuaddon.addon.vow.manager.VowManager; // 引入誓约管理器
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

    // ★★★ 新增：誓约封印状态 ★★★
    public boolean isBanned = false;

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
    public boolean isFusion = false;           // 是否是融合式神
    public boolean conditionsNotMet = false;   // 条件未满足

    // ==================== 伤害预测 ====================
    /** 伤害类型 */
    public AbilityDamagePredictor.DamageType damageType = AbilityDamagePredictor.DamageType.UNKNOWN;
    /** 原版基础伤害（JJK mod 的伤害）*/
    public float vanillaDamage = -1;
    /** 附属加成后的伤害 */
    public float addonDamage = -1;
    /** 暴击伤害 */
    public float critDamage = -1;
    /** 是否能预测伤害 */
    public boolean canPredictDamage = false;
    /** 是否有附属修改（增加或减少）*/
    public boolean hasAddonModification = false;
    /** 伤害变化方向 */
    public AbilityDamagePredictor.PredictionResult.DamageChange damageChange =
            AbilityDamagePredictor.PredictionResult.DamageChange.NONE;

    // ==================== 静态工厂方法 ====================

    public static AbilityStatus build(LocalPlayer player, Ability ability) {
        return build(player, ability, null, null);
    }

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

        // ★★★ 检查誓约封印 ★★★
        // 调用 VowManager 检查玩家是否通过誓约封印了此技能
        try {
            if (VowManager.isAbilityBanned(player, ability)) {
                status.isBanned = true;
                status.canUse = false; // 封印后不可用
            }
        } catch (Exception ignored) {
            // 防止 VowManager 初始化前调用导致崩溃
        }

        // 十影召唤物特殊状态
        if (ability instanceof Summon<?> summon && summon.isTenShadows()) {
            status.isTenShadowsSummon = true;
            status.isFusion = summon.isTotality();
            ITenShadowsData tenData = player.getCapability(TenShadowsDataHandler.INSTANCE).orElse(null);
            if (tenData != null) {
                var registry = player.level().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
                if (status.isFusion) {
                    // ========== 融合式神 ==========
                    status.isTamed = true;  // 融合式神默认已调伏

                    // 1. 检查融合式神本身是否死亡
                    if (summon.canDie()) {
                        for (EntityType<?> entityType : summon.getTypes()) {
                            if (tenData.isDead(registry, entityType)) {
                                status.isDead = true;
                                break;
                            }
                        }
                    }

                    // 2. 如果自己没死，让原版判断融合条件是否满足
                    if (!status.isDead && !ability.isValid(player)) {
                        status.conditionsNotMet = true;
                        status.canUse = false;
                    }
                } else {
                    // ========== 普通式神 ==========
                    // 1. 检查自己是否死亡
                    if (summon.canDie()) {
                        for (EntityType<?> entityType : summon.getTypes()) {
                            if (tenData.isDead(registry, entityType)) {
                                status.isDead = true;
                                break;
                            }
                        }
                    }
                    // 2. 判断调伏状态
                    if (status.isDead) {
                        status.isTamed = true;
                    } else {
                        status.isTamed = summon.isTamed(player);
                    }
                }
            }
        }

        // 召唤冲突检查
        if (TenShadowsHelper.isEnabled() && TenShadowsHelper.isTenShadowsAbility(ability)) {
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
            status.techniqueNotActive = TechniqueHelper.hasAbilityTechniqueConflict(player, ability);
            if (status.techniqueNotActive) status.canUse = false;
        }

        // ==================== 伤害预测 ====================
        try {
            AbilityDamagePredictor.PredictionResult prediction = AbilityDamagePredictor.predict(ability);
            status.damageType = prediction.type;
            status.vanillaDamage = prediction.vanillaDamage;
            status.addonDamage = prediction.addonDamage;
            status.critDamage = prediction.critDamage;
            status.canPredictDamage = prediction.canPredict;
            status.hasAddonModification = prediction.hasAddonModification();
            status.damageChange = prediction.getDamageChange();
        } catch (Exception ignored) {
            // 预测失败，保持默认值
        }

        return status;
    }

    // ==================== 便捷方法 ====================

    public boolean isOn() {
        return isActive || hasSummon;
    }

    public boolean isDisabled() {
        return !canUse && !isOn();
    }

    /**
     * 获取显示用的伤害值
     */
    public float getDisplayDamage() {
        if (!canPredictDamage) return -1;
        return addonDamage;
    }

    public boolean isDamageIncreased() {
        return damageChange == AbilityDamagePredictor.PredictionResult.DamageChange.INCREASED;
    }

    public boolean isDamageDecreased() {
        return damageChange == AbilityDamagePredictor.PredictionResult.DamageChange.DECREASED;
    }

    public String formatDamage(float damage) {
        if (damage < 0) return "?";
        if (damage == 0) return "0";
        if (damage >= 10000) {
            return String.format("%.1fW", damage / 10000);
        }
        if (damage >= 1000) {
            return String.format("%.1fK", damage / 1000);
        }
        if (damage >= 100) {
            return String.format("%.0f", damage);
        }
        return String.format("%.1f", damage);
    }

    public String formatDamageWithChange() {
        if (!canPredictDamage) return "?";

        String damageStr = formatDamage(addonDamage);

        if (critDamage > addonDamage * 1.1f) {
            String critStr = formatDamage(critDamage);
            damageStr = damageStr + " §7(§c" + critStr + "§7)";
        }

        return switch (damageChange) {
            case INCREASED -> "§a↑§r " + damageStr;
            case DECREASED -> "§c↓§r " + damageStr;
            case NONE -> damageStr;
        };
    }
}
