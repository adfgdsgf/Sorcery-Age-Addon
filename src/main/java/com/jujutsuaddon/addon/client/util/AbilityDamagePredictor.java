package com.jujutsuaddon.addon.client.util;

import com.jujutsuaddon.addon.client.cache.DamagePredictionCache;
import com.jujutsuaddon.addon.damage.data.AbilityDamageData;
import com.jujutsuaddon.addon.damage.data.DamageContext;
import com.jujutsuaddon.addon.damage.formula.DamageFormula;
import com.jujutsuaddon.addon.damage.result.DamagePrediction;
import com.jujutsuaddon.addon.network.s2c.SyncDamagePredictionsS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
import radon.jujutsu_kaisen.ability.base.Summon;

/**
 * 客户端伤害预测 - 优先使用服务端数据
 */
public class AbilityDamagePredictor {

    public enum DamageType {
        DIRECT_DAMAGE, POWER_BASED, SUMMON, DOMAIN, UTILITY, UNKNOWN;

        public static DamageType from(DamageContext.AbilityType type) {
            return switch (type) {
                case DIRECT_DAMAGE -> DIRECT_DAMAGE;
                case POWER_BASED -> POWER_BASED;
                case SUMMON -> SUMMON;
                case DOMAIN -> DOMAIN;
                case UTILITY -> UTILITY;
            };
        }

        public static DamageType fromNetworkId(int id) {
            return switch (id) {
                case 0 -> DIRECT_DAMAGE;
                case 1 -> POWER_BASED;
                case 2 -> SUMMON;
                case 3 -> DOMAIN;
                case 4 -> UTILITY;
                default -> UNKNOWN;
            };
        }
    }

    // ==================== 主要 API ====================

    public static PredictionResult predict(Ability ability) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || ability == null) {
            return PredictionResult.unknown();
        }

        // ★★★ 优先使用服务端缓存 ★★★
        SyncDamagePredictionsS2CPacket.PredictionData serverData = DamagePredictionCache.get(ability);
        if (serverData != null) {
            DamageType type = DamageType.fromNetworkId(serverData.damageType);
            return new PredictionResult(type, serverData.vanillaDamage,
                    serverData.addonDamage, serverData.critDamage, serverData.isMelee);
        }

        // ★★★ 召唤物：没有服务端数据就显示问号，不做本地计算 ★★★
        if (ability instanceof Summon<?>) {
            return new PredictionResult(DamageType.SUMMON, -1, -1, -1, false);
        }

        // 非召唤物技能可以本地计算
        return calculateLocal(ability, player);
    }

    public static DamageType getDamageType(Ability ability) {
        if (ability == null) return DamageType.UNKNOWN;
        DamageContext.AbilityType type = detectType(ability);
        return DamageType.from(type);
    }

    // ==================== 本地计算（仅非召唤物）====================

    private static PredictionResult calculateLocal(Ability ability, LocalPlayer player) {
        DamageContext.AbilityType type = detectType(ability);

        // 使用 DamageFormula 计算
        DamageContext ctx = DamageContext.create(player, ability);
        DamagePrediction p = DamageFormula.calculate(ctx);

        return new PredictionResult(
                DamageType.from(p.type()),
                p.vanillaDamage(),
                p.addonDamage(),
                p.critDamage(),
                p.isMelee()
        );
    }

    private static DamageContext.AbilityType detectType(Ability ability) {
        if (ability instanceof Summon<?>) return DamageContext.AbilityType.SUMMON;
        if (ability instanceof DomainExpansion) return DamageContext.AbilityType.DOMAIN;

        AbilityDamageData.CachedData data = AbilityDamageData.get(ability);

        if (data.baseDamage() != null && data.baseDamage() > 0)
            return DamageContext.AbilityType.DIRECT_DAMAGE;

        if (data.projectileClass() != null)
            return DamageContext.AbilityType.POWER_BASED;
        if (ability instanceof Ability.IAttack) return DamageContext.AbilityType.POWER_BASED;
        if (ability instanceof Ability.IDomainAttack) return DamageContext.AbilityType.POWER_BASED;

        Ability.Classification c = ability.getClassification();
        if (c == Ability.Classification.SLASHING || c == Ability.Classification.FIRE ||
                c == Ability.Classification.WATER || c == Ability.Classification.PLANTS ||
                c == Ability.Classification.BLUE || c == Ability.Classification.LIGHTNING ||
                c == Ability.Classification.CURSED_SPEECH) {
            return DamageContext.AbilityType.POWER_BASED;
        }
        return DamageContext.AbilityType.UTILITY;
    }

    // ==================== 缓存清理 ====================

    public static void clearCache() {
        AbilityDamageData.clearCache();
    }

    // ==================== PredictionResult ====================

    public static class PredictionResult {
        public final DamageType type;
        public final float vanillaDamage;
        public final float addonDamage;
        public final float critDamage;
        public final boolean isMelee;
        public final boolean canPredict;

        public PredictionResult(DamageType type, float vanillaDamage, float addonDamage,
                                float critDamage, boolean isMelee) {
            this.type = type;
            this.vanillaDamage = vanillaDamage;
            this.addonDamage = addonDamage;
            this.critDamage = critDamage;
            this.isMelee = isMelee;
            this.canPredict = vanillaDamage >= 0;
        }

        public static PredictionResult unknown() {
            return new PredictionResult(DamageType.UNKNOWN, -1, -1, -1, false);
        }

        public static PredictionResult utility() {
            return new PredictionResult(DamageType.UTILITY, 0, 0, 0, false);
        }

        public boolean hasAddonModification() {
            if (!canPredict || vanillaDamage <= 0) return false;
            float ratio = addonDamage / vanillaDamage;
            return ratio > 1.02f || ratio < 0.98f;
        }

        public float getDisplayDamage() {
            return canPredict ? addonDamage : vanillaDamage;
        }

        public boolean isDamageIncreased() {
            if (!canPredict || vanillaDamage <= 0) return false;
            return addonDamage / vanillaDamage > 1.02f;
        }

        public boolean isDamageDecreased() {
            if (!canPredict || vanillaDamage <= 0) return false;
            return addonDamage / vanillaDamage < 0.98f;
        }

        public DamageChange getDamageChange() {
            if (!canPredict || addonDamage <= 0 || vanillaDamage <= 0) {
                return DamageChange.NONE;
            }
            float ratio = addonDamage / vanillaDamage;
            if (ratio > 1.02f) return DamageChange.INCREASED;
            if (ratio < 0.98f) return DamageChange.DECREASED;
            return DamageChange.NONE;
        }

        public enum DamageChange { INCREASED, DECREASED, NONE }

        public String formatDamage(float damage) {
            if (damage < 0) return "?";
            if (damage == 0) return "0";

            String baseStr;
            if (damage >= 10000) baseStr = String.format("%.1fW", damage / 10000);
            else if (damage >= 1000) baseStr = String.format("%.1fK", damage / 1000);
            else if (damage >= 100) baseStr = String.format("%.0f", damage);
            else baseStr = String.format("%.1f", damage);

            if (this.canPredict && this.critDamage > damage * 1.1f) {
                String critStr;
                if (critDamage >= 10000) critStr = String.format("%.1fW", critDamage / 10000);
                else if (critDamage >= 1000) critStr = String.format("%.1fK", critDamage / 1000);
                else critStr = String.format("%.0f", critDamage);
                return baseStr + " §7(§c" + critStr + "§7)";
            }
            return baseStr;
        }

        public String formatWithChange() {
            if (!canPredict) return "?";
            String damageStr = formatDamage(addonDamage);
            return switch (getDamageChange()) {
                case INCREASED -> "§a↑" + damageStr;
                case DECREASED -> "§c↓" + damageStr;
                case NONE -> damageStr;
            };
        }
    }
}
