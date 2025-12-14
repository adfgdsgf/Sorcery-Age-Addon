package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.util.calc.AbilityDamageCalculator;
import com.jujutsuaddon.addon.util.context.AbilityContext;
import com.jujutsuaddon.addon.util.context.DamageContext;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import com.jujutsuaddon.addon.util.helper.CombatUtil;
import com.jujutsuaddon.addon.util.helper.MobCompatUtils;
import com.jujutsuaddon.addon.util.helper.SoulDamageUtil;
import com.jujutsuaddon.addon.util.helper.WeaponEffectProxy;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.item.cursed_tool.SplitSoulKatanaItem;

import java.util.List;

/**
 * 伤害事件处理器
 * 专门负责处理战斗相关的逻辑：暴击、伤害计算等。
 *
 * 注意：无敌帧穿透逻辑已移至 MixinLivingEntity，以确保在原版检查之前执行。
 */
@Mod.EventBusSubscriber(modid = com.jujutsuaddon.addon.JujutsuAddon.MODID)
public class DamageEventHandler {

    /**
     * 监听暴击事件 (CriticalHitEvent)
     * 目的：捕获原版或其他模组产生的暴击倍率，存入缓存供后续计算使用。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCriticalHit(CriticalHitEvent event) {
        if (event.getEntity() != null) {
            if (event.isVanillaCritical() || event.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW) {
                ModEventHandler.vanillaCritCache.put(event.getEntity().getUUID(), event.getDamageModifier());
            } else {
                ModEventHandler.vanillaCritCache.put(event.getEntity().getUUID(), 1.0f);
            }
        }
    }

    /**
     * 监听最终伤害事件 (LivingDamageEvent)
     * 目的：仅用于调试，记录玩家造成的实际伤害数值。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player && DebugManager.isDebugging(player)) {
            DamageDebugUtil.accumulateDamage(player, event.getAmount());
        }
    }

    // ====================================================================
    // 注意：原 onLivingAttack 中的无敌帧处理已移至 MixinLivingEntity
    // 这样可以确保在 hurt() 方法的最开始就处理，不会被原版逻辑覆盖
    // ====================================================================

    /**
     * 监听受伤事件 (LivingHurtEvent) - 核心逻辑！
     * 目的：接管伤害计算公式，应用各种倍率、平衡性调整和自定义逻辑。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        if (!MobCompatUtils.isAllowed(attacker)) return;

        Player playerForDebug = (attacker instanceof Player) ? (Player) attacker : null;
        DamageSource source = event.getSource();
        float originalDamage = event.getAmount();

        // 解析当前技能
        Ability currentAbility = null;
        if (source.getDirectEntity() != source.getEntity()) {
            currentAbility = CombatUtil.findAbility(source);
        }
        if (currentAbility == null) {
            currentAbility = AbilityContext.get();
        }
        if (currentAbility == null) {
            currentAbility = CombatUtil.findAbility(source);
        }

        boolean isSkillDamage = (currentAbility != null);
        if (isSkillDamage) {
            // ★ 解耦：直接传 source，让 WeaponEffectProxy 自己判断
            WeaponEffectProxy.markAbilityDamage(source, event.getEntity());
        }
        boolean isSSKSoulDamage = SoulDamageUtil.isSoulDamage(source);

        ItemStack stack = attacker.getMainHandItem();
        boolean isSSKPhysical = !isSSKSoulDamage && !isSkillDamage && !stack.isEmpty()
                && stack.getItem() instanceof SplitSoulKatanaItem;

        // 释魂刀灵魂伤害特殊处理
        if (isSSKSoulDamage) {
            Float storedDamage = ModEventHandler.SSK_PRE_ARMOR_DAMAGE.get();
            if (storedDamage != null) {
                double correctionFactor = AddonConfig.COMMON.sskDamageCorrection.get();
                float fixedDamage = (float) (storedDamage * correctionFactor);
                if (fixedDamage > originalDamage) {
                    event.setAmount(fixedDamage);
                    if (playerForDebug != null) {
                        DamageDebugUtil.logSSKCorrection(playerForDebug, storedDamage, fixedDamage, correctionFactor);
                    }
                }
            }
            return;
        }

        // 过滤非战斗伤害
        String msgId = source.getMsgId();
        boolean isVanillaAttack = msgId != null && ("player".equals(msgId) || "mob".equals(msgId));
        if (!isVanillaAttack && !isSkillDamage && !isSSKPhysical) return;

        boolean isMelee = isVanillaAttack || isSSKPhysical;

        // 判断技能是否视为近战
        if (isSkillDamage && currentAbility != null) {
            if (currentAbility.isMelee()) {
                isMelee = true;
            } else {
                String skillName = currentAbility.getClass().getSimpleName();
                List<? extends String> whitelist = AddonConfig.COMMON.meleeSkillWhitelist.get();
                for (String id : whitelist) {
                    if (skillName.toLowerCase().contains(id.toLowerCase())) {
                        isMelee = true;
                        break;
                    }
                }
            }
        }

        // 计算最终伤害
        float finalDamage = AbilityDamageCalculator.calculateDamage(
                currentAbility,
                attacker,
                event.getEntity(),
                originalDamage,
                isMelee
        );

        // PVP 平衡
        if (AddonConfig.COMMON.enablePvpBalance.get() && event.getEntity() instanceof Player) {
            finalDamage *= AddonConfig.COMMON.pvpDamageMultiplier.get();
        }

        // 应用伤害
        if (Math.abs(originalDamage - finalDamage) > 0.001f) {
            event.setAmount(finalDamage);
        }

        // 调试日志
        if (playerForDebug != null && DebugManager.isDebugging(playerForDebug) && isMelee) {
            CombatUtil.checkAndLogMainModBonus(playerForDebug, finalDamage);
        }
    }

    /**
     * 捕获伤害用于灵魂伤害补偿
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureFinalHurtDamage(LivingHurtEvent event) {
        if (event.getAmount() <= 0) return;
        Entity attacker = event.getSource().getEntity();
        Player player = null;
        if (attacker instanceof Player p) player = p;
        else if (attacker instanceof TamableAnimal t && t.getOwner() instanceof Player p) player = p;

        if (player != null) {
            boolean shouldRecord = SoulDamageUtil.shouldApplyTrueDamage(event.getSource(), player);
            if (shouldRecord) {
                DamageContext.set(event.getAmount());
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty() && stack.getItem() instanceof SplitSoulKatanaItem) {
                    ModEventHandler.SSK_PRE_ARMOR_DAMAGE.set(event.getAmount());
                }
            } else {
                DamageContext.clear();
            }
        }
    }

    /**
     * 灵魂伤害补偿逻辑
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onSoulDamageCompensation(LivingDamageEvent event) {
        Float expectedDamage = DamageContext.get();
        if (expectedDamage == null) return;

        float currentDamage = event.getAmount();
        if (currentDamage < expectedDamage) {
            float lostDamage = expectedDamage - currentDamage;
            double bypassRatio = AddonConfig.COMMON.enableSoulTrueDamage.get();
            bypassRatio = Math.max(0.0, Math.min(1.0, bypassRatio));

            if (bypassRatio > 0) {
                float damageRestored = (float) (lostDamage * bypassRatio);
                float newDamage = Math.min(expectedDamage, currentDamage + damageRestored);
                event.setAmount(newDamage);
            }
        }
        DamageContext.clear();
    }
}
