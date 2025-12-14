package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.calc.*;
import com.jujutsuaddon.addon.util.context.TamedCostContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 召唤物属性缩放助手 - 精简版 v3
 *
 * 修复：强制使用 TAMED 状态计算，确保与基准扫描时一致
 */
public class SummonScalingHelper {

    private static final UUID JJK_SORCERER_DAMAGE_UUID = UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");

    // 缓存 Swarm 列表
    private static List<String> cachedSwarmList = null;

    // =========================================================
    // 主模组加成处理
    // =========================================================

    public static double scanAndCleanMainModBonus(TamableAnimal summon, AttributeInstance attr,
                                                  UUID addonUuid, String nbtKey) {
        if (summon.getPersistentData().contains(nbtKey)) {
            return summon.getPersistentData().getDouble(nbtKey);
        }

        double existingBonus = 0.0;
        if (attr != null) {
            for (AttributeModifier mod : attr.getModifiers()) {
                if (!mod.getId().equals(addonUuid)) {
                    existingBonus += mod.getAmount();
                }
            }
            if (existingBonus > 0) {
                List<AttributeModifier> toRemove = new ArrayList<>();
                for (AttributeModifier mod : attr.getModifiers()) {
                    if (!mod.getId().equals(addonUuid) && mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                        toRemove.add(mod);
                    }
                }
                for (AttributeModifier mod : toRemove) {
                    attr.removeModifier(mod);
                }
            }
        }
        summon.getPersistentData().putDouble(nbtKey, existingBonus);
        return existingBonus;
    }

    // =========================================================
    // 稀释因子计算
    // =========================================================

    public static double calculateDilutionFactor(TamableAnimal currentSummon, LivingEntity owner) {
        double range = 128.0;
        var searchBox = owner.getBoundingBox().inflate(range);

        List<? extends TamableAnimal> siblings = owner.level().getEntitiesOfClass(
                currentSummon.getClass(),
                searchBox,
                entity -> {
                    if (entity == null || !entity.isAlive()) return false;
                    if (!entity.isTame()) return false;
                    if (entity.isOwnedBy(owner)) return true;
                    return entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(owner.getUUID());
                }
        );

        int count = Math.max(1, siblings.size());
        return 1.0 / (double) count;
    }

    // =========================================================
    // 阶级倍率计算（核心方法）- 修复版
    // =========================================================

    /**
     * 计算召唤物的阶级倍率
     *
     * ★ 修复：强制使用 TAMED 状态计算，确保与扫描时一致 ★
     */
    public static double calculateTierMultiplier(TamableAnimal summon, LivingEntity owner) {
        if (!AddonConfig.COMMON.enableTierScaling.get()) {
            return 1.0;
        }

        // 1. 获取召唤物对应的技能
        Ability ability = null;
        if (summon instanceof TenShadowsSummon shadowSummon) {
            ability = shadowSummon.getAbility();
        }

        // 如果是非十影召唤物，尝试通过注册表查找
        if (ability == null) {
            ability = findAbilityForSummon(summon);
        }

        if (ability == null) {
            return 1.0;
        }

        // ★ 2. 强制使用 TAMED 状态计算（已召唤的式神都是调伏状态）★
        TamedCostContext.setForceTamed(true);
        float multiplier;
        try {
            // 直接调用 AbilityBalancer，它内部已经处理了 balancerScalingExponent
            multiplier = AbilityBalancer.getSummonMultiplier(ability, owner);
        } finally {
            TamedCostContext.setForceTamed(false);
        }

        // 3. 应用最低保底
        double minMult = AddonConfig.COMMON.minimumTierMultiplier.get();
        return Math.max(multiplier, minMult);
    }

    /**
     * 通过注册表查找召唤物对应的技能（兼容非十影召唤物）
     */
    @Nullable
    private static Ability findAbilityForSummon(TamableAnimal summon) {
        try {
            ResourceLocation registryName = new ResourceLocation("jujutsu_kaisen", "ability");
            IForgeRegistry<Ability> registry = RegistryManager.ACTIVE.getRegistry(registryName);
            if (registry != null) {
                for (Ability ability : registry) {
                    if (ability instanceof Summon<?> summonAbility) {
                        Class<?> definedClass = summonAbility.getClazz();
                        if (definedClass != null && definedClass.isInstance(summon)) {
                            return ability;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 获取调伏状态下的消耗（用于调试）
     */
    public static float getTamedCostForBenchmark(TamableAnimal summon, LivingEntity owner) {
        if (!(summon instanceof TenShadowsSummon shadowSummon)) {
            return -1.0f;
        }

        Ability ability = shadowSummon.getAbility();
        if (ability == null) {
            return -1.0f;
        }

        TamedCostContext.setForceTamed(true);
        try {
            AbilityCategory category = CategoryResolver.resolve(ability, owner);
            return CategoryBenchmark.calculateCostForCategory(ability, category, owner);
        } finally {
            TamedCostContext.setForceTamed(false);
        }
    }

    // =========================================================
    // 群体倍率计算
    // =========================================================

    public static double calculateSwarmMultiplier(TamableAnimal summon) {
        String className = summon.getClass().getName();

        if (cachedSwarmList == null) {
            cachedSwarmList = new ArrayList<>(AddonConfig.COMMON.swarmEntityList.get());
        }

        for (String swarmName : cachedSwarmList) {
            if (className.contains(swarmName)) {
                return AddonConfig.COMMON.swarmScalingModifier.get();
            }
        }
        return 1.0;
    }

    // =========================================================
    // 其他工具方法
    // =========================================================

    public static float calculateSmartHealthRatio(float currentHp, float currentMax, float lastMaxHealth) {
        if (currentHp > currentMax) return 1.0f;
        if (Math.abs(currentHp - currentMax) < 0.5f) return 1.0f;
        if (Math.abs(currentHp - lastMaxHealth) < 0.5f) return 1.0f;

        float denominator = Math.min(currentMax, lastMaxHealth);
        if (denominator <= 0) denominator = 1.0f;

        float ratio = currentHp / denominator;
        return (ratio > 0.99f) ? 1.0f : ratio;
    }

    public static void handleAttributeInheritance(TamableAnimal summon, LivingEntity owner,
                                                  double dilutionFactor, double tierMultiplier) {
        String className = summon.getClass().getName();
        if (!className.startsWith("radon.jujutsu_kaisen")) return;

        double autoRatio = AddonConfig.COMMON.autoAttributeInheritanceRatio.get();
        if (autoRatio <= 0.001) return;

        List<? extends String> blacklist = AddonConfig.COMMON.autoAttributeBlacklist.get();

        summon.getAttributes().getSyncableAttributes().forEach(summonAttr -> {
            Attribute attribute = summonAttr.getAttribute();
            ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (attrId == null) return;

            String attrName = attrId.toString();
            String path = attrId.getPath().toLowerCase();

            if (attribute == Attributes.MOVEMENT_SPEED ||
                    attribute == Attributes.FLYING_SPEED ||
                    attribute == Attributes.JUMP_STRENGTH ||
                    attribute == Attributes.MAX_HEALTH ||
                    attribute == Attributes.ATTACK_DAMAGE) return;

            if (path.contains("crit_chance") || path.contains("crit_damage") || path.contains("crit_rate")) return;
            if (attrName.contains("gravity") || attrName.contains("step_height")) return;
            if (blacklist.contains(attrName)) return;

            AttributeInstance ownerAttr = owner.getAttribute(attribute);
            if (ownerAttr == null) return;

            double ownerValue = ownerAttr.getValue();
            double bonus = ownerValue * autoRatio * dilutionFactor * tierMultiplier;

            UUID modifierUUID = UUID.nameUUIDFromBytes(("jjk_auto_inherit_" + attrName).getBytes());
            AttributeModifier existing = summonAttr.getModifier(modifierUUID);

            if (existing != null) {
                if (Math.abs(existing.getAmount() - bonus) < 0.01) return;
                summonAttr.removeModifier(modifierUUID);
            }

            if (Math.abs(bonus) > 0.01) {
                summonAttr.addTransientModifier(new AttributeModifier(
                        modifierUUID, "JJK Auto Inherit " + attrName, bonus, AttributeModifier.Operation.ADDITION
                ));
            }
        });
    }

    public static double calculateOffensiveMultiplier(LivingEntity entity) {
        boolean isAdditiveMode = AddonConfig.COMMON.useAdditiveExternalAttributes.get();
        double externalMultiplier = 1.0;

        Map<Attribute, Double> multiplierMap = DamageUtil.getMultiplierAttributeCache();
        Map<String, Double> modMaxBonusMap = new HashMap<>();

        for (Map.Entry<Attribute, Double> entry : multiplierMap.entrySet()) {
            Attribute attr = entry.getKey();
            Double factor = entry.getValue();

            AttributeInstance instance = entity.getAttribute(attr);
            double currentVal = (instance != null) ? instance.getValue() : attr.getDefaultValue();
            double defaultVal = attr.getDefaultValue();
            double bonus = 0.0;

            if (Math.abs(defaultVal - 1.0) < 0.001) {
                if (currentVal > 1.0) bonus = (currentVal - 1.0) * factor;
            } else if (Math.abs(defaultVal) < 0.001) {
                if (currentVal > 0) bonus = currentVal * factor;
            } else {
                if (currentVal > defaultVal) bonus = (currentVal - defaultVal) * factor;
            }

            if (bonus > 0) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                String modId = (id != null) ? id.getNamespace() : "unknown";
                modMaxBonusMap.merge(modId, bonus, Math::max);
            }
        }

        for (double modBonus : modMaxBonusMap.values()) {
            if (isAdditiveMode) {
                externalMultiplier += modBonus;
            } else {
                externalMultiplier *= (1.0 + modBonus);
            }
        }

        double atkPercentBonus = getAttackDamagePercent(entity);
        if (atkPercentBonus > 0) {
            if (isAdditiveMode) {
                externalMultiplier += atkPercentBonus;
            } else {
                externalMultiplier *= (1.0 + atkPercentBonus);
            }
        }

        if (AddonConfig.COMMON.enableCritSystem.get()) {
            double chance = AttributeCommonHelper.getCritChance(entity);
            double damageMult = AttributeCommonHelper.getCritDamage(entity);

            if (chance > 0 && damageMult > 1.0) {
                double critExpectation = chance * (damageMult - 1.0);
                if (isAdditiveMode) {
                    externalMultiplier += critExpectation;
                } else {
                    externalMultiplier *= (1.0 + critExpectation);
                }
            }
        }

        return Math.max(1.0, externalMultiplier);
    }

    private static double getAttackDamagePercent(LivingEntity entity) {
        AttributeInstance att = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (att == null) return 0.0;

        double percent = 0.0;
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) {
            if (!mod.getId().equals(JJK_SORCERER_DAMAGE_UUID)) percent += mod.getAmount();
        }
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            if (!mod.getId().equals(JJK_SORCERER_DAMAGE_UUID)) percent += mod.getAmount();
        }
        return percent;
    }

    public static void clearCache() {
        cachedSwarmList = null;
    }
}
