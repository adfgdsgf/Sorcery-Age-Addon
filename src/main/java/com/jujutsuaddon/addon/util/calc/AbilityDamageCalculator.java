package com.jujutsuaddon.addon.util.calc;

import com.google.common.collect.Multimap;
import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.event.ModEventHandler;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import com.jujutsuaddon.addon.util.helper.AttributeCommonHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.capability.data.sorcerer.ISorcererData;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.Trait;

import java.util.*;

public class AbilityDamageCalculator {

    private static final Random random = new Random();
    private static final UUID JJK_ATTACK_DAMAGE_UUID = UUID.fromString("4979087e-da76-4f8a-93ef-6e5847bfa2ee");

    // [优化] 缓存属性对象，避免每次攻击都去查注册表
    private static Attribute EXECUTE_ATTR = null;
    private static Attribute CURRENT_HP_ATTR = null;
    private static boolean attributesInitialized = false;

    // =================================================================
    // 暴击结果缓存类 - 保存完整的暴击信息
    // =================================================================
    private static class CritResult {
        final boolean isCrit;
        final float critMult;
        final float chance;      // 暴击率
        final float critDamage;  // 暴击伤害倍率
        final long tickTimestamp;

        CritResult(boolean isCrit, float critMult, float chance, float critDamage, long tickTimestamp) {
            this.isCrit = isCrit;
            this.critMult = critMult;
            this.chance = chance;
            this.critDamage = critDamage;
            this.tickTimestamp = tickTimestamp;
        }
    }

    private static final Map<UUID, CritResult> tickCritCache = new HashMap<>();

    public static void clearCache(UUID entityUuid) {
        tickCritCache.remove(entityUuid);
    }

    // [优化] 初始化方法：只在第一次调用时执行
    private static void initAttributes() {
        if (attributesInitialized) return;
        try {
            // 尝试获取 Apotheosis 或 AttributesLib 的属性
            if (ForgeRegistries.ATTRIBUTES.containsKey(new ResourceLocation("apotheosis", "execute_damage"))) {
                EXECUTE_ATTR = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("apotheosis", "execute_damage"));
            } else if (ForgeRegistries.ATTRIBUTES.containsKey(new ResourceLocation("attributeslib", "execute_damage"))) {
                EXECUTE_ATTR = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "execute_damage"));
            }

            if (ForgeRegistries.ATTRIBUTES.containsKey(new ResourceLocation("apotheosis", "current_hp_damage"))) {
                CURRENT_HP_ATTR = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("apotheosis", "current_hp_damage"));
            } else if (ForgeRegistries.ATTRIBUTES.containsKey(new ResourceLocation("attributeslib", "current_hp_damage"))) {
                CURRENT_HP_ATTR = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "current_hp_damage"));
            }
        } catch (Exception ignored) {}
        attributesInitialized = true;
    }

    public static float calculateDamage(Ability ability, LivingEntity attacker, LivingEntity target, float originalBaseDamage, boolean isMelee) {

        ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return originalBaseDamage;

        initAttributes(); // 确保属性已初始化

        ItemStack stack = attacker.getMainHandItem();
        boolean isAdditiveMode = AddonConfig.COMMON.useAdditiveExternalAttributes.get();

        // 1. 基础数据
        double vanillaFinal;
        double vanillaFlat = 0.0;

        // [优化] 直接使用 Attributes.ATTACK_DAMAGE，不走 safeGetAttribute
        AttributeInstance atkAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            vanillaFlat = atkAttr.getValue();
            AttributeModifier jjkMod = atkAttr.getModifier(JJK_ATTACK_DAMAGE_UUID);
            if (jjkMod != null) {
                vanillaFlat -= jjkMod.getAmount();
                if (vanillaFlat < 1.0) vanillaFlat = 1.0;
            }
        } else {
            vanillaFlat = 1.0;
        }

        double attackDamagePercentBonus = getAttackDamagePercent(attacker);

        if (isAdditiveMode) {
            vanillaFinal = vanillaFlat;
        } else {
            vanillaFinal = vanillaFlat * (1.0 + attackDamagePercentBonus);
        }

        double weaponRatio = (vanillaFlat > 0.0001) ? (vanillaFinal / vanillaFlat) : 1.0;

        // 2. 外部倍率收集
        double externalMultiplier = 1.0;
        StringBuilder dynamicMultInfo = new StringBuilder();
        String symbol = isAdditiveMode ? "§a+§r" : "§cx§r";

        if (attackDamagePercentBonus > 0) {
            if (isAdditiveMode) {
                externalMultiplier += attackDamagePercentBonus;
                dynamicMultInfo.append(" ").append(Component.translatable("debug.jujutsu_addon.atk_percent_add", attackDamagePercentBonus * 100).getString());
            } else {
                dynamicMultInfo.append(" ").append(Component.translatable("debug.jujutsu_addon.atk_percent_mul", attackDamagePercentBonus * 100).getString());
            }
        }

        // ★★★ 修复：IAttack 技能虽然近战触发，但伤害本质是术式 ★★★
        boolean isActuallyMelee = isMelee && !(ability instanceof Ability.IAttack);

        if (!isActuallyMelee) {
            // [优化] 使用 DamageUtil 已经加载好的缓存
            Map<Attribute, Double> multiplierMap = DamageUtil.getMultiplierAttributeCache();
            Map<String, Double> modMaxBonusMap = new HashMap<>();

            for (Map.Entry<Attribute, Double> entry : multiplierMap.entrySet()) {
                Attribute attr = entry.getKey();
                Double factor = entry.getValue();

                // [优化] 直接获取属性值，避免字符串查找
                AttributeInstance instance = attacker.getAttribute(attr);
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

            for (Map.Entry<String, Double> entry : modMaxBonusMap.entrySet()) {
                double modBonus = entry.getValue();
                String modId = entry.getKey();
                if (isAdditiveMode) {
                    externalMultiplier += modBonus;
                } else {
                    externalMultiplier *= (1.0 + modBonus);
                }
                String val = String.format("%.2f", modBonus * 100);
                dynamicMultInfo.append(" ").append(symbol).append("§6[").append(modId).append(":").append(val).append("%]§r");
            }
        }

        if (target != null) {
            double rpgBonus = 0.0;
            float targetHp = target.getHealth();
            float targetMaxHp = target.getMaxHealth();
            if (targetMaxHp > 0) {
                // [优化] 使用缓存的 Attribute 对象
                if (EXECUTE_ATTR != null) {
                    AttributeInstance inst = attacker.getAttribute(EXECUTE_ATTR);
                    if (inst != null && inst.getValue() > 0) {
                        double missingHpPercent = 1.0 - (targetHp / targetMaxHp);
                        rpgBonus += (inst.getValue() * missingHpPercent);
                    }
                }
                if (CURRENT_HP_ATTR != null) {
                    AttributeInstance inst = attacker.getAttribute(CURRENT_HP_ATTR);
                    if (inst != null && inst.getValue() > 0) {
                        double currentHpPercent = targetHp / targetMaxHp;
                        rpgBonus += (inst.getValue() * currentHpPercent);
                    }
                }
            }

            if (rpgBonus > 0) {
                if (isAdditiveMode) {
                    externalMultiplier += rpgBonus;
                } else {
                    externalMultiplier *= (1.0 + rpgBonus);
                }
                dynamicMultInfo.append(" ").append(symbol).append("§c[RPG:").append(String.format("%.2f", rpgBonus * 100)).append("%]§r");
            }
        }

        double panelMultiplier = externalMultiplier;
        double baseMultiplier = weaponRatio;

        if (isAdditiveMode) {
            baseMultiplier += (externalMultiplier - 1.0);
        } else {
            baseMultiplier *= externalMultiplier;
        }

        double modExtraFlat = 0.0;
        if (attacker instanceof Player player) {
            modExtraFlat = DamageUtil.getExtraAttributePanel(player);
        }
        double totalPanel = vanillaFinal + modExtraFlat;

        // [优化] 直接获取攻速
        double attackSpeed = 4.0;
        AttributeInstance speedAttr = attacker.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr != null) attackSpeed = speedAttr.getValue();

        if (!stack.isEmpty()) {
            try {
                Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
                if (modifiers.containsKey(Attributes.ATTACK_SPEED)) {
                    for (AttributeModifier mod : modifiers.get(Attributes.ATTACK_SPEED)) {
                        if (mod.getOperation() == AttributeModifier.Operation.ADDITION && mod.getAmount() < 0) {
                            attackSpeed -= mod.getAmount();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        double effectiveSpeed = Math.max(4.0, attackSpeed);

        double finalDamage;
        double debugPreservationRatio = 0.0;
        double debugClassMult = 0;
        double debugSpeedMult = 0;
        String debugSourceKey = "unknown";

        if (cap.hasTrait(Trait.HEAVENLY_RESTRICTION)) {
            debugSourceKey = "hr";
            double hrSpeedScaling = AddonConfig.COMMON.hrAttackSpeedScaling.get();
            double speedModifier = 1.0 + (effectiveSpeed - 1.0) * hrSpeedScaling;
            double classMultiplier = AddonConfig.COMMON.hrMeleeMultiplier.get();

            if (!stack.isEmpty()) {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (itemId != null) {
                    String idStr = itemId.toString();
                    List<? extends String> bonuses = AddonConfig.COMMON.heavenlyRestrictionBonus.get();
                    for (String entry : bonuses) {
                        String[] parts = entry.split("=");
                        if (parts.length == 2 && idStr.equals(parts[0].trim())) {
                            try {
                                classMultiplier = Double.parseDouble(parts[1].trim());
                                break;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            debugClassMult = classMultiplier;
            debugSpeedMult = speedModifier;

            double rawDamage = totalPanel * classMultiplier * speedModifier * panelMultiplier;
            finalDamage = rawDamage;

            if (ability != null) {
                double preservationRatio = AddonConfig.COMMON.hrMeleePreservation.get();
                double preservedOriginal = originalBaseDamage * preservationRatio;
                double preservedFinal = preservedOriginal * baseMultiplier;

                if (preservedFinal > finalDamage) {
                    finalDamage = preservedFinal;
                    debugPreservationRatio = preservationRatio;
                    debugClassMult = 0;
                    debugSpeedMult = 0;
                }
            }

        } else {
            // ========================================
            // ★★★ 咒术师分支 - 核心修复区域 ★★★
            // ========================================
            double sorcererSpeedScaling = AddonConfig.COMMON.sorcererAttackSpeedScaling.get();
            double speedModifier = 1.0 + (effectiveSpeed - 1.0) * sorcererSpeedScaling;

            // ★★★ 关键修复：使用 isActuallyMelee 而不是 isMelee ★★★
            // IAttack 技能（如 BlueFists）虽然近战触发，但伤害本质是术式
            double roleMultiplier = isActuallyMelee ?
                    CharacterBalancer.getMeleeMultiplier(attacker) :
                    CharacterBalancer.getTechniqueMultiplier(attacker);

            double preservationRatio = isActuallyMelee ?
                    CharacterBalancer.getMeleePreservation(attacker) :
                    CharacterBalancer.getTechniquePreservation(attacker);

            debugPreservationRatio = preservationRatio;

            String roleName = CharacterBalancer.getSpecialRoleName(attacker);
            // ★ 更新 debugSourceKey 使用 isActuallyMelee
            debugSourceKey = roleName + (isActuallyMelee ? "_melee" : "_tech");

            double baseDamagePart = originalBaseDamage * preservationRatio * baseMultiplier;

            debugClassMult = roleMultiplier;
            debugSpeedMult = speedModifier;

            double rawTotalDamage;
            if (AddonConfig.COMMON.enableAttackDamageScaling.get()) {
                double panelPart = totalPanel * roleMultiplier * speedModifier * panelMultiplier;
                rawTotalDamage = baseDamagePart + panelPart;
            } else {
                rawTotalDamage = baseDamagePart * roleMultiplier;
                debugClassMult = roleMultiplier;
                totalPanel = 0;
            }
            finalDamage = rawTotalDamage;
        }

        float balancerMult = 1.0f;
        if (ability != null) {
            double skillMult = DamageUtil.getSkillMultiplier(ability);
            // ★ 同样使用 isActuallyMelee
            if (!isActuallyMelee) {
                balancerMult = AbilityBalancer.getDamageMultiplier(ability, attacker);
            }
            double totalSkillMult = skillMult * balancerMult;
            finalDamage *= totalSkillMult;
            panelMultiplier *= totalSkillMult;
        }

        if (AddonConfig.COMMON.enableEnchantmentScaling.get()) {
            if (!AddonConfig.COMMON.restrictToWeapons.get() || !stack.isEmpty()) {
                double enchantBonus = 0.0;
                MobType type = (target != null) ? target.getMobType() : MobType.UNDEFINED;
                enchantBonus += EnchantmentHelper.getDamageBonus(stack, type);

                // ★ 同样使用 isActuallyMelee
                if (!isActuallyMelee && (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)) {
                    int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
                    if (powerLevel > 0) enchantBonus += 0.5 * (powerLevel + 1);
                }

                finalDamage += (enchantBonus * AddonConfig.COMMON.enchantmentMultiplier.get() * baseMultiplier);
            }
        }

        // =================================================================
        // 暴击系统
        // =================================================================
        boolean isCrit = false;
        float critMult = 1.0f;
        float displayChance = 0f;
        float displayDmg = 1.0f;

        if (AddonConfig.COMMON.enableCritSystem.get()) {
            // ★ 同样使用 isActuallyMelee
            if (isActuallyMelee) {
                // 纯近战：优先检查原版暴击
                Float vanillaModifier = ModEventHandler.vanillaCritCache.get(attacker.getUUID());
                if (vanillaModifier != null && vanillaModifier > 1.0f) {
                    isCrit = true;
                    critMult = vanillaModifier;
                    CritResult displayResult = getOrCreateCritResult(attacker);
                    displayChance = displayResult.chance;
                    displayDmg = displayResult.critDamage;
                } else {
                    CritResult result = getOrCreateCritResult(attacker);
                    isCrit = result.isCrit;
                    critMult = result.critMult;
                    displayChance = result.chance;
                    displayDmg = result.critDamage;
                    if (isCrit) finalDamage *= critMult;
                }
            } else {
                // 术式（包括 IAttack）：使用自定义暴击系统
                CritResult result = getOrCreateCritResult(attacker);
                isCrit = result.isCrit;
                critMult = result.critMult;
                displayChance = result.chance;
                displayDmg = result.critDamage;
                if (isCrit) finalDamage *= critMult;
            }

            if (tickCritCache.containsKey(attacker.getUUID())) {
                CritResult cached = tickCritCache.get(attacker.getUUID());
                if (cached.tickTimestamp == attacker.level().getGameTime()) {
                    dynamicMultInfo.append(" ").append(Component.translatable("debug.jujutsu_addon.linked_crit").getString());
                }
            }
        }

        finalDamage *= AddonConfig.COMMON.globalDamageMultiplier.get();

        if (attacker instanceof Player player && DebugManager.isDebugging(player)) {
            String skillName = (ability != null) ? ability.getClass().getSimpleName() : null;

            DamageDebugUtil.logCalculation(
                    player,
                    debugSourceKey,
                    originalBaseDamage,
                    (float) debugPreservationRatio,
                    totalPanel,
                    (float) debugClassMult,
                    (float) debugSpeedMult,
                    (float) panelMultiplier,
                    balancerMult,
                    (float) finalDamage,
                    isCrit,
                    displayChance,
                    displayDmg,
                    isActuallyMelee,  // ★ 改用 isActuallyMelee
                    dynamicMultInfo.toString(),
                    isAdditiveMode,
                    weaponRatio,
                    (float) baseMultiplier,
                    skillName
            );

            if (ability != null && skillName != null) {
                if (DamageDebugUtil.shouldLogCalculationForSkill(player, "classinfo_" + skillName)) {
                    String className = ability.getClass().getName();
                    player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.helper.class_name", className));
                }
            }
        }
        return (float) finalDamage;
    }


    // =================================================================
    // 核心修复：统一的暴击结果获取方法
    // 同一tick内只计算一次，避免重复调用和重复日志
    // =================================================================
    private static CritResult getOrCreateCritResult(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        long currentTick = entity.level().getGameTime();

        // 检查缓存：同一tick返回缓存结果
        if (tickCritCache.containsKey(uuid)) {
            CritResult cached = tickCritCache.get(uuid);
            if (cached.tickTimestamp == currentTick) {
                return cached;
            }
        }

        // 新的tick，重新计算（这里会输出日志，但只输出一次）
        float chance = (float) AttributeCommonHelper.getCritChance(entity);
        float critDamage = (float) AttributeCommonHelper.getCritDamage(entity);

        boolean isCrit = random.nextFloat() < chance;
        float mult = isCrit ? critDamage : 1.0f;

        CritResult result = new CritResult(isCrit, mult, chance, critDamage, currentTick);
        tickCritCache.put(uuid, result);
        return result;
    }

    private static double getAttackDamagePercent(LivingEntity entity) {
        AttributeInstance att = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (att == null) return 0.0;

        double percent = 0.0;
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) {
            if (!mod.getId().equals(JJK_ATTACK_DAMAGE_UUID)) percent += mod.getAmount();
        }
        for (AttributeModifier mod : att.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            if (!mod.getId().equals(JJK_ATTACK_DAMAGE_UUID)) percent += mod.getAmount();
        }
        return percent;
    }
}
