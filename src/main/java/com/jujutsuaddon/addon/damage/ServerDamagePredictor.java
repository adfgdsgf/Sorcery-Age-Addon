// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/ServerDamagePredictor.java
package com.jujutsuaddon.addon.damage;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.balance.ability.AbilityBalancer;
import com.jujutsuaddon.addon.context.TamedCostContext;
import com.jujutsuaddon.addon.damage.analysis.AbilityDamageData;
import com.jujutsuaddon.addon.damage.core.DamageContext;
import com.jujutsuaddon.addon.damage.core.DamageCore;
import com.jujutsuaddon.addon.damage.core.DamageResult;
import com.jujutsuaddon.addon.network.s2c.SyncDamagePredictionsS2CPacket;
import com.jujutsuaddon.addon.network.s2c.SyncDamagePredictionsS2CPacket.PredictionData;
import com.jujutsuaddon.addon.summon.SummonScalingHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端伤害预测器
 *
 * ★★★ 使用 DamageContext + DamageCore 统一公式 ★★★
 */
public final class ServerDamagePredictor {

    private ServerDamagePredictor() {}

    private static final Map<EntityType<?>, Double> ENTITY_ATTACK_CACHE = new ConcurrentHashMap<>();
    private static final UUID JJK_ATTACK_DAMAGE_UUID = UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");
    private static List<String> cachedSwarmList = null;

    // ==================== 伤害类型枚举 ====================

    public enum DamageType {
        DIRECT_DAMAGE(0),
        POWER_BASED(1),
        SUMMON(2),
        DOMAIN(3),
        UTILITY(4);

        private final int networkId;

        DamageType(int networkId) {
            this.networkId = networkId;
        }

        public int getNetworkId() {
            return networkId;
        }
    }

    // ==================== 预测结果 ====================

    public record PredictionResult(
            DamageType type,
            float vanillaDamage,
            float addonDamage,
            float critDamage,
            boolean isMelee
    ) {
        public static PredictionResult utility() {
            return new PredictionResult(DamageType.UTILITY, 0, 0, 0, false);
        }

        public static PredictionResult summon(float vanilla, float addon) {
            return new PredictionResult(DamageType.SUMMON, vanilla, addon, addon, false);
        }
    }

    // ==================== 主入口 ====================

    /**
     * 计算玩家所有技能的伤害预测
     */
    public static SyncDamagePredictionsS2CPacket calculateAll(ServerPlayer player) {
        Map<String, PredictionData> predictions = new HashMap<>();

        ISorcererData cap = player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) {
            return new SyncDamagePredictionsS2CPacket(predictions);
        }

        Set<Ability> abilities = collectAbilities(player, cap);

        for (Ability ability : abilities) {
            try {
                PredictionResult prediction = calculate(player, ability);
                predictions.put(
                        ability.getClass().getName(),
                        toNetworkData(prediction)
                );
            } catch (Exception ignored) {}
        }

        return new SyncDamagePredictionsS2CPacket(predictions);
    }

    /**
     * 计算单个技能的伤害预测
     */
    public static PredictionResult calculate(ServerPlayer player, Ability ability) {
        // 1. 特殊类型处理
        if (ability instanceof Summon<?> summon) {
            return calculateSummonDamage(player, summon);
        }

        if (ability instanceof DomainExpansion) {
            return PredictionResult.utility();
        }

        // 2. 检测伤害类型
        DamageType type = detectDamageType(ability);
        if (type == DamageType.UTILITY) {
            return PredictionResult.utility();
        }

        // 3. 获取技能基础数据
        AbilityDamageData.CachedData data = AbilityDamageData.get(ability);
        float baseDamage = (data.baseDamage() != null) ? data.baseDamage() : 1.0f;
        float skillMultiplier = data.multiplier();
        float power = ability.getPower(player);

        // 4. 原版伤害（不含 Addon 修改）
        float vanillaDamage = baseDamage * skillMultiplier * power;

        // 5. 使用 DamageContext + DamageCore 计算
        DamageContext ctx = DamageContext.forPrediction(player, ability, baseDamage, skillMultiplier, power);
        DamageResult result = DamageCore.calculate(ctx);

        // 6. 应用全局倍率
        double globalMult = AddonConfig.COMMON.globalDamageMultiplier.get();
        float addonDamage = (float) result.withGlobalMultiplier(globalMult);
        float critDamage = (float) result.critWithGlobal(globalMult);

        boolean isMelee = ability.isMelee() || (ability instanceof Ability.IAttack);

        return new PredictionResult(type, vanillaDamage, addonDamage, critDamage, isMelee);
    }

    // ==================== 伤害类型检测 ====================

    private static DamageType detectDamageType(Ability ability) {
        if (ability instanceof Summon<?>) return DamageType.SUMMON;
        if (ability instanceof DomainExpansion) return DamageType.DOMAIN;

        AbilityDamageData.CachedData data = AbilityDamageData.get(ability);

        if (data.baseDamage() != null && data.baseDamage() > 0) {
            return DamageType.DIRECT_DAMAGE;
        }

        if (data.projectileClass() != null) return DamageType.POWER_BASED;
        if (ability instanceof Ability.IAttack) return DamageType.POWER_BASED;
        if (ability instanceof Ability.IDomainAttack) return DamageType.POWER_BASED;

        if (isDamageClassification(ability.getClassification())) {
            return DamageType.POWER_BASED;
        }

        return DamageType.UTILITY;
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

    // ==================== 召唤物计算 ====================

    private static PredictionResult calculateSummonDamage(ServerPlayer player, Summon<?> summon) {
        List<EntityType<?>> types = summon.getTypes();
        if (types == null || types.isEmpty()) {
            return PredictionResult.summon(-1, -1);
        }

        EntityType<?> entityType = types.get(0);
        double baseAttack = getEntityBaseAttackDamage(player.serverLevel(), entityType);
        if (baseAttack < 0) {
            return PredictionResult.summon(-1, -1);
        }

        // 主模组攻击力加成
        double mainModBonus = calculateMainModAtkBonus(player);

        // 原版伤害 = 基础攻击力 + 主模组加成
        float vanillaDamage = (float) (baseAttack + mainModBonus);

        // Addon 加成计算
        float tierMultiplier = 1.0f;
        try {
            TamedCostContext.setForceTamed(true);
            tierMultiplier = AbilityBalancer.getSummonMultiplierSilent(summon, player);
        } finally {
            TamedCostContext.setForceTamed(false);
        }

        float swarmMult = calculateSwarmMultiplier(summon);
        float dilutionFactor = 1.0f; // 预测时假设只有1个
        float finalMultiplier = tierMultiplier * swarmMult * dilutionFactor;

        double rawOwnerDmg = getOwnerBaseAttack(player);
        double externalMultiplier = SummonScalingHelper.calculateOffensiveMultiplier(player);

        double dpsConfigFactor = AddonConfig.COMMON.summonDpsCompensationFactor.get();
        double ownerAtkSpeed = getOwnerAttackSpeed(player);
        double dpsMultiplier = (dpsConfigFactor > 0.001) ? Math.max(4.0, ownerAtkSpeed) * dpsConfigFactor : 1.0;

        double effectiveOwnerDamage = rawOwnerDmg * externalMultiplier * dpsMultiplier;
        double atkRatio = AddonConfig.COMMON.summonAtkRatio.get();
        double addonScalingBonus = effectiveOwnerDamage * atkRatio * finalMultiplier;

        float addonDamage = (float) (baseAttack + mainModBonus + addonScalingBonus);

        return PredictionResult.summon(vanillaDamage, addonDamage);
    }

    private static double calculateMainModAtkBonus(ServerPlayer player) {
        ISorcererData cap = player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return 1.6;

        float ownerExp = cap.getExperience();
        float summonExp = ownerExp * 0.9f;
        return 1.6 + summonExp / 1340.0;
    }

    private static double getOwnerBaseAttack(ServerPlayer player) {
        AttributeInstance att = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (att == null) return 1.0;

        double base = att.getBaseValue();
        double flatBonus = 0.0;

        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.ADDITION)) {
            if (mod.getId().equals(JJK_ATTACK_DAMAGE_UUID)) continue;
            flatBonus += mod.getAmount();
        }

        return Math.max(1.0, base + flatBonus);
    }

    private static double getOwnerAttackSpeed(ServerPlayer player) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        double speed = (speedAttr != null) ? speedAttr.getValue() : 4.0;
        return (Double.isNaN(speed) || speed <= 0) ? 4.0 : speed;
    }

    private static float calculateSwarmMultiplier(Summon<?> summon) {
        Class<?> entityClass = summon.getClazz();
        if (entityClass == null) return 1.0f;

        String className = entityClass.getName();

        if (cachedSwarmList == null) {
            cachedSwarmList = new ArrayList<>(AddonConfig.COMMON.swarmEntityList.get());
        }

        for (String swarmName : cachedSwarmList) {
            if (className.contains(swarmName)) {
                return AddonConfig.COMMON.swarmScalingModifier.get().floatValue();
            }
        }
        return 1.0f;
    }

    private static double getEntityBaseAttackDamage(ServerLevel level, EntityType<?> entityType) {
        Double cached = ENTITY_ATTACK_CACHE.get(entityType);
        if (cached != null) return cached;

        double damage = -1.0;

        try {
            @SuppressWarnings("unchecked")
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) entityType;
            var supplier = DefaultAttributes.getSupplier(livingType);
            if (supplier.hasAttribute(Attributes.ATTACK_DAMAGE)) {
                damage = supplier.getBaseValue(Attributes.ATTACK_DAMAGE);
            }
        } catch (Exception ignored) {}

        if (damage < 0 && level != null) {
            try {
                Entity temp = entityType.create(level);
                if (temp instanceof LivingEntity living) {
                    damage = living.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    temp.discard();
                }
            } catch (Exception ignored) {}
        }

        if (damage >= 0) {
            ENTITY_ATTACK_CACHE.put(entityType, damage);
        }

        return damage;
    }

    // ==================== 辅助方法 ====================

    private static Set<Ability> collectAbilities(ServerPlayer player, ISorcererData cap) {
        Set<Ability> abilities = new LinkedHashSet<>();

        for (Ability ability : JJKAbilities.getAbilities(player)) {
            if (ability != null) abilities.add(ability);
        }

        addTechniqueAbilities(abilities, cap.getTechnique());
        addTechniqueAbilities(abilities, cap.getCurrentCopied());
        addTechniqueAbilities(abilities, cap.getCurrentStolen());

        return abilities;
    }

    private static void addTechniqueAbilities(Set<Ability> set, CursedTechnique technique) {
        if (technique != null) {
            Ability[] abilities = technique.getAbilities();
            if (abilities != null) {
                for (Ability a : abilities) {
                    if (a != null) set.add(a);
                }
            }
        }
    }

    private static PredictionData toNetworkData(PredictionResult p) {
        return new PredictionData(
                p.vanillaDamage(),
                p.addonDamage(),
                p.critDamage(),
                p.isMelee(),
                p.type().getNetworkId()
        );
    }

    public static void clearCache() {
        AbilityDamageData.clearCache();
        ENTITY_ATTACK_CACHE.clear();
        cachedSwarmList = null;
    }
}
