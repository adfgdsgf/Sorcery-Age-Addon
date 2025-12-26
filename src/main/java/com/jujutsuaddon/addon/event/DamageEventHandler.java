package com.jujutsuaddon.addon.event;

import com.jujutsuaddon.addon.config.AddonConfig;
import com.jujutsuaddon.addon.damage.calculator.AbilityDamageCalculator;
import com.jujutsuaddon.addon.context.SoulDamageContext;
import com.jujutsuaddon.addon.util.debug.DamageDebugUtil;
import com.jujutsuaddon.addon.util.debug.DebugManager;
import com.jujutsuaddon.addon.util.helper.CombatUtil;
import com.jujutsuaddon.addon.util.helper.MobCompatUtils;
import com.jujutsuaddon.addon.util.helper.SoulDamageUtil;
import com.jujutsuaddon.addon.util.helper.WeaponEffectProxy;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.registries.ForgeRegistries;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.item.cursed_tool.SplitSoulKatanaItem;

import java.util.List;

/**
 * 伤害事件处理器
 * 专门负责处理战斗相关的逻辑：暴击、伤害计算等。
 *
 * ★★★ 极简白名单模式：只处理 JJK 相关的伤害 ★★★
 */
@Mod.EventBusSubscriber(modid = com.jujutsuaddon.addon.JujutsuAddon.MODID)
public class DamageEventHandler {

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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player && DebugManager.isDebugging(player)) {
            DamageDebugUtil.accumulateDamage(player, event.getAmount());
        }
    }

    // ==================== 核心检测结果 ====================

    /**
     * 伤害检测结果
     */
    private record DamageCheckResult(
            boolean shouldProcess,
            boolean isMelee,
            Ability ability
    ) {
        static DamageCheckResult skip() {
            return new DamageCheckResult(false, false, null);
        }

        static DamageCheckResult process(boolean isMelee, Ability ability) {
            return new DamageCheckResult(true, isMelee, ability);
        }
    }

    // ==================== 硬编码白名单检测 ====================

    /**
     * 需要特例放行的技能类名列表
     * 这些技能使用原版伤害源，容易被误判为普通攻击，需要通过调用栈强制识别
     */
    private static final List<String> WHITELISTED_SKILL_CLASSES = List.of(
            "radon.jujutsu_kaisen.ability.misc.Blitz",   // 迅雷
            "radon.jujutsu_kaisen.ability.misc.Barrage", // 缭乱
            "radon.jujutsu_kaisen.ability.misc.Punch",   // 拳击
            "radon.jujutsu_kaisen.ability.misc.Slam"     // 砸地
    );

    /**
     * ★★★ 核心技术：调用栈检查 ★★★
     * 检查当前代码执行路径中，是否包含上述四个技能类。
     * 这比检查手持物品或爆炸类型要精准得多，绝无误判。
     */
    private static boolean isCausedByWhitelistedSkill() {
        return StackWalker.getInstance().walk(stream -> stream.anyMatch(frame -> {
            String className = frame.getClassName();
            // 检查类名是否包含白名单中的关键词
            // 使用 contains 是为了兼容 Lambda 表达式 (例如 Barrage$$Lambda...)
            for (String skillClass : WHITELISTED_SKILL_CLASSES) {
                if (className.contains(skillClass)) {
                    return true;
                }
            }
            return false;
        }));
    }

    /**
     * ★★★ 核心：判断是否应该处理这个伤害 ★★★
     *
     * 极简白名单：只处理 JJK mod 的伤害
     * - JJK 技能伤害
     * - JJK 投射物
     * - JJK 武器近战
     * - ★ 特例：硬编码的四个体术技能
     *
     * 其他一律不处理（包括 TACZ、原版弓箭、普通剑等）
     */
    private static DamageCheckResult shouldProcessDamage(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity target) {

        // ===== 1. JJK 技能伤害（最高优先级）=====
        if (source instanceof JJKDamageSources.JujutsuDamageSource jjkSource) {
            Ability ability = jjkSource.getAbility();
            boolean isMelee = (ability != null && ability.isMelee());
            return DamageCheckResult.process(isMelee, ability);
        }

        Entity direct = source.getDirectEntity();

        // ===== 2. JJK 投射物 =====
        if (direct != null && direct != attacker) {
            String className = direct.getClass().getName();

            // 只处理 JJK 投射物，其他一律跳过
            if (className.contains("jujutsu_kaisen") || className.contains("radon.jujutsu")) {
                Ability ability = CombatUtil.findAbilityFromProjectile(direct);
                return DamageCheckResult.process(false, ability);
            }

            // 非 JJK 投射物 → 跳过（包括 TACZ 子弹、原版箭等）
            return DamageCheckResult.skip();
        }

        // ===== 3. JJK 武器近战 =====
        // 只要拿着 JJK 武器，无论是平A还是技能，都算
        String msgId = source.getMsgId();
        if ("player".equals(msgId) || "mob".equals(msgId)) {
            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.isEmpty()) {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(weapon.getItem());
                if (itemId != null && "jujutsu_kaisen".equals(itemId.getNamespace())) {
                    return DamageCheckResult.process(true, null);
                }
            }
        }

        // ===== 4. ★★★ 硬编码特例检测 ★★★ =====
        // 针对 Blitz, Barrage, Punch, Slam 这四个使用原版伤害源的技能
        // 只有当攻击者是玩家或术师时才进行昂贵的栈检查
        if (attacker instanceof Player || attacker.getCapability(radon.jujutsu_kaisen.capability.data.sorcerer.SorcererDataHandler.INSTANCE).isPresent()) {
            if (isCausedByWhitelistedSkill()) {
                // 检测到了！这是那四个技能之一造成的伤害
                // Slam 是爆炸，视为非近战；其他视为近战
                // 修复：1.20+ 版本没有 isExplosion()，改用 msgId 判断
                boolean isExplosion = msgId.contains("explosion");
                return DamageCheckResult.process(!isExplosion, null);
            }
        }

        // ===== 其他一律不处理 =====
        return DamageCheckResult.skip();
    }

    // ==================== 主事件处理 ====================

    /**
     * 监听受伤事件 (LivingHurtEvent) - 核心逻辑！
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {

        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        if (!MobCompatUtils.isAllowed(attacker)) return;

        Player playerForDebug = (attacker instanceof Player) ? (Player) attacker : null;
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float originalDamage = event.getAmount();

        // ★★★ 核心：判断是否应该处理这个伤害 ★★★
        DamageCheckResult checkResult = shouldProcessDamage(source, attacker, target);

        if (!checkResult.shouldProcess) {
            return;  // 不处理，保持原伤害
        }

        Ability currentAbility = checkResult.ability;
        boolean isSkillDamage = (currentAbility != null);
        boolean isMelee = checkResult.isMelee;

        // SSK 检测（释魂刀是 JJK 武器，在 shouldProcessDamage 中已通过）
        boolean isSSKSoulDamage = SoulDamageUtil.isSoulDamage(source);
        ItemStack stack = attacker.getMainHandItem();
        boolean isSSKPhysical = !isSSKSoulDamage && !isSkillDamage && !stack.isEmpty()
                && stack.getItem() instanceof SplitSoulKatanaItem;

        if (isSSKPhysical) {
            isMelee = true;
        }

        // 标记技能伤害
        if (isSkillDamage) {
            WeaponEffectProxy.markAbilityDamage(source, target);
        }

        // 释魂刀灵魂伤害特殊处理
        if (isSSKSoulDamage) {
            // 修复：直接使用当前的 originalDamage 进行计算
            // 之前尝试读取 SSK_PRE_ARMOR_DAMAGE 是错误的，因为该值在 captureFinalHurtDamage (LOWEST) 才设置
            double correctionFactor = AddonConfig.COMMON.sskDamageCorrection.get();
            float fixedDamage = (float) (originalDamage * correctionFactor);

            if (fixedDamage > originalDamage) {
                event.setAmount(fixedDamage);
                if (playerForDebug != null) {
                    DamageDebugUtil.logSSKCorrection(playerForDebug, originalDamage, fixedDamage, correctionFactor);
                }
            }
            return;
        }

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
                target,
                originalDamage,
                isMelee
        );

        // PVP 平衡
        if (AddonConfig.COMMON.enablePvpBalance.get() && target instanceof Player) {
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

    // ==================== 其他事件处理 ====================

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
                SoulDamageContext.set(event.getAmount());
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty() && stack.getItem() instanceof SplitSoulKatanaItem) {
                    ModEventHandler.SSK_PRE_ARMOR_DAMAGE.set(event.getAmount());
                }
            } else {
                SoulDamageContext.clear();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onSoulDamageCompensation(LivingDamageEvent event) {
        Float expectedDamage = SoulDamageContext.get();
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
        SoulDamageContext.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurtFinalDebug(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;

        Player player = null;
        if (attacker instanceof Player p) {
            player = p;
        } else if (attacker instanceof TamableAnimal t && t.getOwner() instanceof Player p) {
            player = p;
        }

        if (player == null || !DebugManager.isDebugging(player)) return;

        float finalAmount = event.getAmount();

        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "debug.jujutsu_addon.event_final",
                event.getEntity().getName().getString(),
                String.format("%.1f", finalAmount)));
    }
}
