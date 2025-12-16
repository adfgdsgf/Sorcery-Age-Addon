package com.jujutsuaddon.addon.damage;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.damage.data.AbilityDamageData;
import com.jujutsuaddon.addon.damage.data.DamageContext;
import com.jujutsuaddon.addon.damage.formula.DamageFormula;
import com.jujutsuaddon.addon.damage.result.DamagePrediction;
import com.jujutsuaddon.addon.network.s2c.SyncDamagePredictionsS2CPacket;
import com.jujutsuaddon.addon.network.s2c.SyncDamagePredictionsS2CPacket.PredictionData;
import com.jujutsuaddon.addon.util.calc.AbilityBalancer;
import com.jujutsuaddon.addon.util.context.TamedCostContext;
import com.jujutsuaddon.addon.util.helper.SummonScalingHelper;
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
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.capability.data.sorcerer.CursedTechnique;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Layer 3: 服务端入口
 */
public final class ServerDamagePredictor {

    private ServerDamagePredictor() {}

    private static final Map<EntityType<?>, Double> ENTITY_ATTACK_CACHE = new ConcurrentHashMap<>();
    private static final UUID JJK_ATTACK_DAMAGE_UUID = UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");

    // Swarm 列表缓存
    private static List<String> cachedSwarmList = null;

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
                DamagePrediction prediction = calculate(player, ability);
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
    public static DamagePrediction calculate(ServerPlayer player, Ability ability) {
        // 召唤物特殊处理
        if (ability instanceof Summon<?> summon) {
            return calculateSummonDamage(player, summon);
        }

        DamageContext ctx = DamageContext.create(player, ability);
        return DamageFormula.calculate(ctx);
    }

    // ==================== 主模组公式 ====================

    /**
     * ★★★ 计算主模组的攻击力加成 ★★★
     *
     * 公式来源：
     * - SorcererUtil.getPower(): power = 1.6 + experience / 1340
     * - TenShadowsSummon.getExperience(): 召唤物经验 = 主人经验 × 0.9
     *
     * @param player 玩家
     * @return 主模组攻击力加成值
     */
    private static double calculateMainModAtkBonus(ServerPlayer player) {
        ISorcererData cap = player.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) {
            return 1.6; // 最低值 (经验为0时)
        }

        float ownerExp = cap.getExperience();
        // 召唤物经验 = 主人经验 × 0.9 (来自 TenShadowsSummon.getExperience())
        float summonExp = ownerExp * 0.9f;
        // power = 1.6 + exp / 1340 (来自 SorcererUtil.getPower())
        return 1.6 + summonExp / 1340.0;
    }

    // ==================== 召唤物伤害计算 ====================

    /**
     * 计算召唤物伤害 - 与 SummonScalingHelper 保持一致
     */
    private static DamagePrediction calculateSummonDamage(ServerPlayer player, Summon<?> summon) {
        List<EntityType<?>> types = summon.getTypes();
        if (types == null || types.isEmpty()) {
            return DamagePrediction.summon();
        }
        EntityType<?> entityType = types.get(0);
        double baseAttack = getEntityBaseAttackDamage(player.serverLevel(), entityType);
        if (baseAttack < 0) {
            return DamagePrediction.summon();
        }
        // ★★★ 主模组攻击力加成 ★★★
        double mainModBonus = calculateMainModAtkBonus(player);
        // ★★★ 原版伤害 = 基础攻击力 + 主模组加成（不含Addon）★★★
        float vanillaDamage = (float) (baseAttack + mainModBonus);
        // ==================== Addon 加成计算 ====================
        // ★★★ 关键修复：必须设置 TamedCostContext ★★★
        float tierMultiplier = 1.0f;
        try {
            TamedCostContext.setForceTamed(true);
            try {
                tierMultiplier = AbilityBalancer.getSummonMultiplierSilent(summon, player);
            } finally {
                TamedCostContext.setForceTamed(false);
            }
        } catch (Exception ignored) {}
        // 2. 获取 swarmMultiplier
        float swarmMult = calculateSwarmMultiplier(summon);
        // 3. 稀释因子 - 预测时假设只有1个式神（不稀释）
        float dilutionFactor = 1.0f;
        // 4. 最终倍率
        float finalMultiplier = tierMultiplier * swarmMult * dilutionFactor;
        // 5. 获取主人基础攻击（排除JJK加成）
        double rawOwnerDmg = getOwnerBaseAttack(player);
        // 6. 外部倍率
        double externalMultiplier = SummonScalingHelper.calculateOffensiveMultiplier(player);
        // 7. DPS补偿因子
        double dpsConfigFactor = AddonConfig.COMMON.summonDpsCompensationFactor.get();
        double ownerAtkSpeed = getOwnerAttackSpeed(player);
        double dpsMultiplier = (dpsConfigFactor > 0.001) ? Math.max(4.0, ownerAtkSpeed) * dpsConfigFactor : 1.0;
        // 8. 有效主人伤害
        double effectiveOwnerDamage = rawOwnerDmg * externalMultiplier * dpsMultiplier;
        // 9. 攻击力比例配置
        double atkRatio = AddonConfig.COMMON.summonAtkRatio.get();
        // 10. 计算Addon缩放加成
        double addonScalingBonus = effectiveOwnerDamage * atkRatio * finalMultiplier;
        // ★★★ 最终攻击力 = 基础攻击力 + 主模组加成 + Addon缩放加成 ★★★
        float addonDamage = (float) (baseAttack + mainModBonus + addonScalingBonus);
        return new DamagePrediction(
                DamageContext.AbilityType.SUMMON,
                vanillaDamage,
                addonDamage,
                addonDamage,
                false
        );
    }

    /**
     * 获取主人基础攻击（排除JJK加成）- 与 SummonScalingHelper.getOwnerBaseAttack 一致
     */
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

    /**
     * 获取主人攻速
     */
    private static double getOwnerAttackSpeed(ServerPlayer player) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        double speed = (speedAttr != null) ? speedAttr.getValue() : 4.0;
        return (Double.isNaN(speed) || speed <= 0) ? 4.0 : speed;
    }

    /**
     * 计算Swarm倍率 - 与 SummonScalingHelper.calculateSwarmMultiplier 一致
     */
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

    /**
     * 获取实体基础攻击力
     */
    private static double getEntityBaseAttackDamage(ServerLevel level, EntityType<?> entityType) {
        Double cached = ENTITY_ATTACK_CACHE.get(entityType);
        if (cached != null) {
            return cached;
        }

        double damage = -1.0;

        // 方法1: DefaultAttributes
        try {
            @SuppressWarnings("unchecked")
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) entityType;
            var supplier = DefaultAttributes.getSupplier(livingType);
            if (supplier.hasAttribute(Attributes.ATTACK_DAMAGE)) {
                damage = supplier.getBaseValue(Attributes.ATTACK_DAMAGE);
            }
        } catch (Exception ignored) {}

        // 方法2: 创建临时实体
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

    private static PredictionData toNetworkData(DamagePrediction p) {
        int typeOrdinal = switch (p.type()) {
            case DIRECT_DAMAGE -> 0;
            case POWER_BASED -> 1;
            case SUMMON -> 2;
            case DOMAIN -> 3;
            case UTILITY -> 4;
        };

        return new PredictionData(
                p.vanillaDamage(),
                p.addonDamage(),
                p.critDamage(),
                p.isMelee(),
                typeOrdinal
        );
    }

    public static void clearCache() {
        AbilityDamageData.clearCache();
        ENTITY_ATTACK_CACHE.clear();
        cachedSwarmList = null;
    }
}
