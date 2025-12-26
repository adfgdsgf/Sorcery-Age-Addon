// 文件路径: src/main/java/com/jujutsuaddon/addon/damage/calculator/AbilityDamageCalculator.java
package com.jujutsuaddon.addon.damage.calculator;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.damage.cache.AttributeCache;
import com.jujutsuaddon.addon.damage.core.DamageContext;
import com.jujutsuaddon.addon.damage.core.DamageCore;
import com.jujutsuaddon.addon.damage.core.DamageResult;
import com.jujutsuaddon.addon.event.ModEventHandler;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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

import java.util.*;

/**
 * 实际伤害计算器
 *
 * ★★★ 三乘区版本 ★★★
 *
 * 流程：
 * 1. DamageContext.forRealDamage() - 收集参数（含三乘区）
 * 2. DamageCore.calculate() - 核心计算
 * 3. applyEnchantmentBonus() - 附魔加成
 * 4. applyRpgAttributes() - RPG属性（斩杀等）
 * 5. applyActualCrit() - 实际暴击判定
 * 6. 全局倍率
 */
public final class AbilityDamageCalculator {

    private AbilityDamageCalculator() {}

    private static final Random random = new Random();

    // ==================== RPG 属性缓存 ====================

    private static Attribute EXECUTE_ATTR = null;
    private static Attribute CURRENT_HP_ATTR = null;
    private static boolean rpgAttributesInitialized = false;

    // ==================== 暴击缓存 ====================

    private static final Map<UUID, CritResult> tickCritCache = new HashMap<>();

    private record CritResult(
            boolean isCrit,
            float critMult,
            float chance,
            float critDamage,
            long tickTimestamp
    ) {}

    public static void clearCache(UUID entityUuid) {
        tickCritCache.remove(entityUuid);
    }

    // ==================== 主入口 ====================

    public static float calculateDamage(
            Ability ability,
            LivingEntity attacker,
            LivingEntity target,
            float originalBaseDamage,
            boolean isMelee) {

        // 0. 检查能力
        ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElse(null);
        if (cap == null) return originalBaseDamage;

        // 1. 构建上下文 + 核心计算
        DamageContext ctx = DamageContext.forRealDamage(
                attacker, target, ability, originalBaseDamage, isMelee);
        DamageResult coreResult = DamageCore.calculate(ctx);

        double finalDamage = coreResult.rawDamage();

        // 2. 附魔加成（在核心计算之后）
        double enchantBonus = calculateEnchantmentBonus(ctx, attacker, target);
        finalDamage += enchantBonus;

        // 3. RPG 属性加成
        RpgBonusResult rpgResult = calculateRpgBonus(attacker, target);
        if (rpgResult.bonus > 0) {
            finalDamage *= (1.0 + rpgResult.bonus);
        }

        // 4. 实际暴击判定
        CritInfo critInfo = determineActualCrit(ctx, attacker);
        if (critInfo.isCrit) {
            finalDamage *= critInfo.critMult;
        }

        // 5. 全局倍率
        finalDamage *= ctx.globalMultiplier();

        // 6. 调试日志
        if (attacker instanceof Player player && DebugManager.isDebugging(player)) {
            logDebugInfo(player, ctx, coreResult, finalDamage, critInfo, rpgResult.debugInfo);
        }

        return (float) finalDamage;

    }

    // ==================== 附魔加成 ====================

    private static double calculateEnchantmentBonus(
            DamageContext ctx,
            LivingEntity attacker,
            LivingEntity target) {

        if (!AddonConfig.COMMON.enableEnchantmentScaling.get()) {
            return 0.0;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (AddonConfig.COMMON.restrictToWeapons.get() && stack.isEmpty()) {
            return 0.0;
        }

        double enchantBonus = 0.0;
        MobType mobType = (target != null) ? target.getMobType() : MobType.UNDEFINED;
        enchantBonus += EnchantmentHelper.getDamageBonus(stack, mobType);

        // 弓箭力量附魔（仅术式）
        if (!ctx.isActuallyMelee()) {
            if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
                int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
                if (powerLevel > 0) {
                    enchantBonus += 0.5 * (powerLevel + 1);
                }
            }
        }

        if (enchantBonus > 0) {
            double enchantMult = AddonConfig.COMMON.enchantmentMultiplier.get();
            return enchantBonus * enchantMult * ctx.externalMultiplier();
        }

        return 0.0;
    }

    // ==================== RPG 属性 ====================

    private record RpgBonusResult(double bonus, String debugInfo) {}

    private static RpgBonusResult calculateRpgBonus(LivingEntity attacker, LivingEntity target) {
        if (target == null) return new RpgBonusResult(0.0, "");

        initRpgAttributes();

        float targetHp = target.getHealth();
        float targetMaxHp = target.getMaxHealth();
        if (targetMaxHp <= 0) return new RpgBonusResult(0.0, "");

        double rpgBonus = 0.0;
        StringBuilder debugInfo = new StringBuilder();

        // 斩杀伤害
        if (EXECUTE_ATTR != null) {
            AttributeInstance inst = attacker.getAttribute(EXECUTE_ATTR);
            if (inst != null && inst.getValue() > 0) {
                double missingHpPercent = 1.0 - (targetHp / targetMaxHp);
                double executeBonus = inst.getValue() * missingHpPercent;
                rpgBonus += executeBonus;
                debugInfo.append(String.format(" §c[Execute:%.1f%%]§r", executeBonus * 100));
            }
        }

        // 当前生命伤害
        if (CURRENT_HP_ATTR != null) {
            AttributeInstance inst = attacker.getAttribute(CURRENT_HP_ATTR);
            if (inst != null && inst.getValue() > 0) {
                double currentHpPercent = targetHp / targetMaxHp;
                double currentHpBonus = inst.getValue() * currentHpPercent;
                rpgBonus += currentHpBonus;
                debugInfo.append(String.format(" §c[CurHP:%.1f%%]§r", currentHpBonus * 100));
            }
        }

        return new RpgBonusResult(rpgBonus, debugInfo.toString());
    }

    private static void initRpgAttributes() {
        if (rpgAttributesInitialized) return;

        try {
            ResourceLocation executeApo = new ResourceLocation("apotheosis", "execute_damage");
            ResourceLocation executeLib = new ResourceLocation("attributeslib", "execute_damage");
            ResourceLocation currentApo = new ResourceLocation("apotheosis", "current_hp_damage");
            ResourceLocation currentLib = new ResourceLocation("attributeslib", "current_hp_damage");

            if (ForgeRegistries.ATTRIBUTES.containsKey(executeApo)) {
                EXECUTE_ATTR = ForgeRegistries.ATTRIBUTES.getValue(executeApo);
            } else if (ForgeRegistries.ATTRIBUTES.containsKey(executeLib)) {
                EXECUTE_ATTR = ForgeRegistries.ATTRIBUTES.getValue(executeLib);
            }

            if (ForgeRegistries.ATTRIBUTES.containsKey(currentApo)) {
                CURRENT_HP_ATTR = ForgeRegistries.ATTRIBUTES.getValue(currentApo);
            } else if (ForgeRegistries.ATTRIBUTES.containsKey(currentLib)) {
                CURRENT_HP_ATTR = ForgeRegistries.ATTRIBUTES.getValue(currentLib);
            }
        } catch (Exception ignored) {}

        rpgAttributesInitialized = true;
    }

    // ==================== 暴击系统 ====================

    private record CritInfo(
            boolean isCrit,
            float critMult,
            float displayChance,
            float displayDamage,
            boolean linkedCrit
    ) {}

    private static CritInfo determineActualCrit(DamageContext ctx, LivingEntity attacker) {
        if (!AddonConfig.COMMON.enableCritSystem.get()) {
            return new CritInfo(false, 1.0f, 0f, 1.0f, false);
        }

        boolean isCrit = false;
        float critMult = 1.0f;
        float displayChance;
        float displayDamage;
        boolean linkedCrit = false;

        if (ctx.isActuallyMelee()) {
            // 纯近战：优先检查原版暴击
            Float vanillaModifier = ModEventHandler.vanillaCritCache.get(attacker.getUUID());
            if (vanillaModifier != null && vanillaModifier > 1.0f) {
                isCrit = true;
                critMult = vanillaModifier;
                CritResult displayResult = getOrCreateCritResult(attacker);
                displayChance = displayResult.chance;
                displayDamage = displayResult.critDamage;
            } else {
                CritResult result = getOrCreateCritResult(attacker);
                isCrit = result.isCrit;
                critMult = result.critMult;
                displayChance = result.chance;
                displayDamage = result.critDamage;
            }
        } else {
            // 术式：使用自定义暴击
            CritResult result = getOrCreateCritResult(attacker);
            isCrit = result.isCrit;
            critMult = result.critMult;
            displayChance = result.chance;
            displayDamage = result.critDamage;
        }

        // 检查是否使用了缓存（联动暴击）
        CritResult cached = tickCritCache.get(attacker.getUUID());
        if (cached != null && cached.tickTimestamp == attacker.level().getGameTime()) {
            linkedCrit = true;
        }

        return new CritInfo(isCrit, critMult, displayChance, displayDamage, linkedCrit);
    }

    private static CritResult getOrCreateCritResult(LivingEntity entity) {
        UUID uuid = entity.getUUID();
        long currentTick = entity.level().getGameTime();

        CritResult cached = tickCritCache.get(uuid);
        if (cached != null && cached.tickTimestamp == currentTick) {
            return cached;
        }

        float chance = (float) AttributeCache.getCritChance(entity);
        float critDamage = (float) AttributeCache.getCritDamage(entity);

        boolean isCrit = random.nextFloat() < chance;
        float mult = isCrit ? critDamage : 1.0f;

        CritResult result = new CritResult(isCrit, mult, chance, critDamage, currentTick);
        tickCritCache.put(uuid, result);
        return result;
    }

    // ==================== 调试日志 ====================

    private static void logDebugInfo(
            Player player,
            DamageContext ctx,
            DamageResult result,
            double finalDamage,
            CritInfo critInfo,
            String rpgDebugInfo) {
        String skillName = (ctx.ability() != null) ? ctx.ability().getClass().getSimpleName() : null;
        StringBuilder dynamicMultInfo = new StringBuilder();
        dynamicMultInfo.append(ctx.dynamicMultInfo());
        dynamicMultInfo.append(rpgDebugInfo);
        if (critInfo.linkedCrit) {
            dynamicMultInfo.append(" ").append(Component.translatable("debug.jujutsu_addon.linked_crit").getString());
        }
        // ★ 调用更新后的日志方法，传递四乘区数据 ★
        DamageDebugUtil.logCalculation(
                player,
                ctx.roleKey(),
                ctx.originalBaseDamage(),
                (float) ctx.preservation(),
                ctx.totalPanel(),
                (float) ctx.roleMultiplier(),
                (float) ctx.speedModifier(),
                ctx.balancerMultiplier(),
                (float) finalDamage,
                critInfo.isCrit,
                critInfo.displayChance,
                critInfo.displayDamage,
                ctx.isActuallyMelee(),
                dynamicMultInfo.toString(),
                skillName,
                ctx.cursedEnergyOutput(),
                // 四乘区数据
                ctx.externalAddition(),
                ctx.externalMultBase(),
                ctx.externalMultTotal(),
                ctx.independentAttrMult(),
                ctx.externalContributions()
        );
        // 类名日志
        if (ctx.ability() != null && skillName != null) {
            if (DamageDebugUtil.shouldLogCalculationForSkill(player, "classinfo_" + skillName)) {
                String className = ctx.ability().getClass().getName();
                player.sendSystemMessage(Component.translatable("debug.jujutsu_addon.helper.class_name", className));
            }
        }
    }
}
