package com.jujutsuaddon.addon.util.helper;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.calc.*;
import com.jujutsuaddon.addon.util.context.TamedCostContext;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.entity.ten_shadows.base.TenShadowsSummon;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 召唤物属性缩放助手 - 重构版
 * 包含所有属性计算、应用和继承逻辑
 */
public class SummonScalingHelper {

    // ==================== 常量定义 ====================
    private static final UUID HP_MODIFIER_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000001");
    private static final UUID ATK_MODIFIER_UUID = UUID.fromString("d3e1a2f4-0000-1000-8000-000000000002");
    private static final UUID JJK_SORCERER_DAMAGE_UUID = UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");

    // NBT Keys (替代 Mixin 字段)
    private static final String NBT_LAST_HP_BONUS = "jjk_addon_last_hp_bonus";
    private static final String NBT_LAST_ATK_BONUS = "jjk_addon_last_atk_bonus";
    private static final String NBT_LAST_MAX_HP = "jjk_addon_last_max_hp";
    private static final String NBT_MAIN_MOD_HP = "jjk_addon_main_mod_hp_bonus";
    private static final String NBT_MAIN_MOD_ATK = "jjk_addon_main_mod_atk_bonus";

    // 缓存 Swarm 列表
    private static List<String> cachedSwarmList = null;

    // ==================== 核心逻辑入口 ====================

    /**
     * 执行属性缩放逻辑 (应在 Entity 加入世界和 Tick 时调用)
     */
    public static void runScalingLogic(TamableAnimal summon) {
        if (!AddonConfig.COMMON.enableSummonScaling.get()) return;

        LivingEntity owner = summon.getOwner();
        if (owner == null || !MobCompatUtils.isAllowed(owner)) return;

        // 仅处理 JJK 实体
        if (!summon.getClass().getName().startsWith("radon.jujutsu_kaisen")) return;

        // 1. 标签处理
        if (summon.isTame()) {
            if (!summon.getTags().contains("jjk_tamed_ally")) {
                summon.addTag("jjk_tamed_ally");
                summon.removeTag("jjk_ritual_enemy");
            }
        }

        // 2. 计算各种倍率
        double dilutionFactor = calculateDilutionFactor(summon, owner);
        double tierMult = calculateTierMultiplier(summon, owner);
        double swarmMult = calculateSwarmMultiplier(summon);
        double finalMultiplier = tierMult * swarmMult * dilutionFactor;

        // 3. 获取并清理主模组自带的加成
        AttributeInstance hpAttr = summon.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance atkAttr = summon.getAttribute(Attributes.ATTACK_DAMAGE);

        double mainModHpBonus = scanAndCleanMainModBonus(summon, hpAttr, HP_MODIFIER_UUID, NBT_MAIN_MOD_HP);
        double mainModAtkBonus = scanAndCleanMainModBonus(summon, atkAttr, ATK_MODIFIER_UUID, NBT_MAIN_MOD_ATK);

        // 4. 决定是否应用缩放
        boolean isTamedAlly = summon.isTame();
        boolean allowUntamedScaling = AddonConfig.COMMON.enableUntamedStatScaling.get();
        boolean shouldScale = isTamedAlly || allowUntamedScaling;

        // 5. 计算血量加成
        double rawOwnerHp = owner.getMaxHealth();
        double safeOwnerHp = (rawOwnerHp < 0 || Double.isNaN(rawOwnerHp) || Double.isInfinite(rawOwnerHp)) ? 20.0 : rawOwnerHp;
        double hpRatio = AddonConfig.COMMON.summonHpRatio.get();

        double scalingHpBonus = shouldScale ? (safeOwnerHp * hpRatio * finalMultiplier) : 0.0;
        double totalHpBonus = Math.max(0, scalingHpBonus + mainModHpBonus);

        // 6. 计算攻击力加成
        double rawOwnerDmg = getOwnerBaseAttack(owner);
        double externalMultiplier = calculateOffensiveMultiplier(owner);
        double dpsConfigFactor = AddonConfig.COMMON.summonDpsCompensationFactor.get();

        double ownerAtkSpeed = getOwnerAttackSpeed(owner);
        double dpsMultiplier = (dpsConfigFactor > 0.001) ? Math.max(4.0, ownerAtkSpeed) * dpsConfigFactor : 1.0;

        double effectiveOwnerDamage = rawOwnerDmg * externalMultiplier * dpsMultiplier;
        double atkRatio = AddonConfig.COMMON.summonAtkRatio.get();

        double scalingAtkBonus = shouldScale ? (effectiveOwnerDamage * atkRatio * finalMultiplier) : 0.0;
        double totalAtkBonus = Math.max(0, scalingAtkBonus + mainModAtkBonus);

        // 7. 调试日志
        if (owner instanceof Player player && DebugManager.isDebugging(player)) {
            // 简单的去重逻辑，避免刷屏
            double lastLoggedHp = summon.getPersistentData().getDouble("debug_last_logged_hp");
            if (Math.abs(totalHpBonus - lastLoggedHp) > 0.1) {
                DamageDebugUtil.logFullSummonStatus(player, summon, scalingHpBonus, mainModHpBonus, totalHpBonus,
                        summon.getHealth(), summon.getMaxHealth(), rawOwnerDmg, externalMultiplier,
                        ownerAtkSpeed, dpsConfigFactor, dpsMultiplier, effectiveOwnerDamage,
                        atkRatio, finalMultiplier, scalingAtkBonus, -1.0f);
                summon.getPersistentData().putDouble("debug_last_logged_hp", totalHpBonus);
            }
        }

        // 8. 应用属性修改
        applyAttributes(summon, hpAttr, atkAttr, totalHpBonus, totalAtkBonus);

        // 9. 处理属性继承 (护甲、幸运等)
        handleAttributeInheritance(summon, owner, dilutionFactor, finalMultiplier);
    }

    // ==================== 属性应用逻辑 ====================

    private static void applyAttributes(TamableAnimal summon, AttributeInstance hpAttr, AttributeInstance atkAttr,
                                        double totalHpBonus, double totalAtkBonus) {
        // 读取上一次计算的值
        double lastCalculatedHpBonus = summon.getPersistentData().getDouble(NBT_LAST_HP_BONUS);
        double lastCalculatedAtkBonus = summon.getPersistentData().getDouble(NBT_LAST_ATK_BONUS);

        // 应用 HP
        if (hpAttr != null) {
            boolean isSignificantChange = Math.abs(lastCalculatedHpBonus - totalHpBonus) > (lastCalculatedHpBonus * 0.01 + 0.1);
            // 如果变化显著，或者修饰符丢失，则重新应用
            if (isSignificantChange || hpAttr.getModifier(HP_MODIFIER_UUID) == null) {
                summon.getPersistentData().putDouble(NBT_LAST_HP_BONUS, totalHpBonus);
                applyHpModifier(summon, hpAttr, totalHpBonus);
            }
        }

        // 应用 ATK
        if (atkAttr != null) {
            boolean isSignificantChange = Math.abs(lastCalculatedAtkBonus - totalAtkBonus) > (lastCalculatedAtkBonus * 0.01 + 0.1);
            if (isSignificantChange || atkAttr.getModifier(ATK_MODIFIER_UUID) == null) {
                summon.getPersistentData().putDouble(NBT_LAST_ATK_BONUS, totalAtkBonus);

                atkAttr.removeModifier(ATK_MODIFIER_UUID);
                if (totalAtkBonus > 0.1) {
                    atkAttr.addTransientModifier(new AttributeModifier(
                            ATK_MODIFIER_UUID, "JJK Addon Summon Atk", totalAtkBonus,
                            AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    private static void applyHpModifier(TamableAnimal summon, AttributeInstance hpAttr, double newBonus) {
        float currentMax = summon.getMaxHealth();
        float currentHp = summon.getHealth();

        // 读取上一次的最大血量
        float lastMaxHealth = summon.getPersistentData().getFloat(NBT_LAST_MAX_HP);
        if (lastMaxHealth <= 0) lastMaxHealth = currentMax; // 初始化

        AttributeModifier existing = hpAttr.getModifier(HP_MODIFIER_UUID);

        // 智能回血比例计算
        float ratio = calculateSmartHealthRatio(currentHp, currentMax, lastMaxHealth);

        boolean attributesChanged = false;
        if (existing == null) {
            if (newBonus > 0.1) attributesChanged = true;
        } else {
            if (Math.abs(existing.getAmount() - newBonus) > (existing.getAmount() * 0.01 + 0.1)) {
                attributesChanged = true;
            }
        }

        if (attributesChanged) {
            hpAttr.removeModifier(HP_MODIFIER_UUID);
            if (newBonus > 0.1) {
                hpAttr.addTransientModifier(new AttributeModifier(
                        HP_MODIFIER_UUID, "JJK Addon Summon HP", newBonus,
                        AttributeModifier.Operation.ADDITION));
            }

            // 重新获取更新后的最大血量
            float newMax = summon.getMaxHealth();
            float targetHealth = newMax * ratio;

            // 只有当血量变化较大时才设置，减少网络包
            if (Math.abs(targetHealth - summon.getHealth()) > 0.5) {
                summon.setHealth(targetHealth);
            }

            // 更新记录
            summon.getPersistentData().putFloat(NBT_LAST_MAX_HP, newMax);
        }
    }

    // ==================== 辅助计算方法 ====================

    private static double getOwnerBaseAttack(LivingEntity owner) {
        if (owner instanceof Player player) {
            AttributeInstance att = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (att == null) return 1.0;
            double base = att.getBaseValue();
            double flatBonus = 0.0;
            for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.ADDITION)) {
                if (mod.getId().equals(JJK_SORCERER_DAMAGE_UUID)) continue;
                flatBonus += mod.getAmount();
            }
            return Math.max(1.0, base + flatBonus);
        } else {
            AttributeInstance mobAtkAttr = owner.getAttribute(Attributes.ATTACK_DAMAGE);
            return (mobAtkAttr != null) ? mobAtkAttr.getValue() : 1.0;
        }
    }

    private static double getOwnerAttackSpeed(LivingEntity owner) {
        AttributeInstance speedAttr = owner.getAttribute(Attributes.ATTACK_SPEED);
        double speed = (speedAttr != null) ? speedAttr.getValue() : 4.0;
        return (Double.isNaN(speed) || speed <= 0) ? 4.0 : speed;
    }

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

    public static double calculateTierMultiplier(TamableAnimal summon, LivingEntity owner) {
        if (!AddonConfig.COMMON.enableTierScaling.get()) {
            return 1.0;
        }

        Ability ability = null;
        if (summon instanceof TenShadowsSummon shadowSummon) {
            ability = shadowSummon.getAbility();
        }

        if (ability == null) {
            ability = findAbilityForSummon(summon);
        }

        if (ability == null) {
            return 1.0;
        }

        TamedCostContext.setForceTamed(true);
        float multiplier;
        try {
            multiplier = AbilityBalancer.getSummonMultiplier(ability, owner);
        } finally {
            TamedCostContext.setForceTamed(false);
        }

        double minMult = AddonConfig.COMMON.minimumTierMultiplier.get();
        return Math.max(multiplier, minMult);
    }

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
        return DamageUtil.calculateExternalMultiplier(entity, false, true, true);
    }

    public static void clearCache() {
        cachedSwarmList = null;
    }
}
