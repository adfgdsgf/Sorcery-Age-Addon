package com.jujutsuaddon.addon.mixin.core;

import com.jujutsuaddon.addon.AddonConfig;
import com.jujutsuaddon.addon.context.AbilityContext;
import com.jujutsuaddon.addon.context.SoulDamageContext;
import com.jujutsuaddon.addon.util.helper.EnchantmentTriggerHandler;
import com.jujutsuaddon.addon.util.helper.ProjectileHitTracker;
import com.jujutsuaddon.addon.util.helper.SoulDamageUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import radon.jujutsu_kaisen.ability.base.Ability;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    @Shadow
    protected float lastHurt;
    public MixinLivingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    // ==================== 单次命中投射物处理 ====================
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void jujutsuAddon$blockDuplicateProjectileHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (((Entity)(Object)this).level().isClientSide) return;

        Entity directEntity = source.getDirectEntity();

        // 检查直接伤害来源是否是单次命中投射物
        if (directEntity != null && ProjectileHitTracker.isSingleHitProjectile(directEntity)) {
            if (ProjectileHitTracker.shouldBlockHit(directEntity, (Entity)(Object)this)) {
                cir.setReturnValue(false); // 阻止伤害
                return;
            }
        }
    }
    // ==================== 辅助方法 ====================
    @Unique
    private static boolean jujutsuAddon$matchesWhitelist(String className, List<? extends String> whitelist) {
        if (className == null || className.isEmpty()) return false;
        for (String pattern : whitelist) {
            if (className.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    @Unique
    private static boolean jujutsuAddon$matchesDamageSource(DamageSource source, List<? extends String> whitelist) {
        Ability ability = AbilityContext.get();
        if (ability != null && jujutsuAddon$matchesWhitelist(ability.getClass().getName(), whitelist)) {
            return true;
        }
        if (source.getDirectEntity() != null) {
            if (jujutsuAddon$matchesWhitelist(source.getDirectEntity().getClass().getName(), whitelist)) {
                return true;
            }
        }
        if (source.getEntity() != null) {
            if (jujutsuAddon$matchesWhitelist(source.getEntity().getClass().getName(), whitelist)) {
                return true;
            }
        }
        return false;
    }
    @Unique
    private static boolean jujutsuAddon$isJJKDamage(DamageSource source) {
        if (AbilityContext.get() != null) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        if (direct != null) {
            String className = direct.getClass().getName();
            if (className.contains("jujutsu_kaisen") || className.contains("radon.jujutsu")) {
                return true;
            }
        }
        String msgId = source.getMsgId();
        if (msgId != null && msgId.contains("jujutsu")) {
            return true;
        }
        if ("player".equals(msgId) || "mob".equals(msgId)) {
            return false;
        }
        return AddonConfig.COMMON.triggerEnchantForUnknownDamage.get();
    }
    // ==================== 灵魂伤害上下文 ====================
    @Inject(method = "hurt", at = @At("HEAD"))
    private void jujutsuAddon$prepareSoulContext(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (SoulDamageUtil.isSoulDamage(source) && AddonConfig.COMMON.enableSoulTrueDamage.get() > 0.001) {
            SoulDamageContext.set(amount);
        }
    }
    @Inject(method = "hurt", at = @At("RETURN"))
    private void jujutsuAddon$clearSoulContext(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        SoulDamageContext.clear();
    }
    // ==================== 无敌帧处理 - HEAD阶段 ====================

    /**
     * HEAD阶段：只处理【强制穿透】
     * 完全清除无敌帧，让每次攻击都能造成完整伤害
     */
    @Inject(method = "hurt", at = @At("HEAD"))
    private void jujutsuAddon$handleBypassIframe(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide) return;
        if (target.invulnerableTime > 0) {
            // ===== 强制穿透白名单 =====
            // 完全无视无敌帧，每次攻击都造成完整伤害
            List<? extends String> bypassWhitelist = AddonConfig.COMMON.iframeBypassWhitelist.get();
            if (jujutsuAddon$matchesDamageSource(source, bypassWhitelist)) {
                target.invulnerableTime = 0;
                this.lastHurt = 0;
                // 不return！让原版方法继续处理，它会造成伤害并设置新的无敌帧
                // 下次攻击时我们会再次清除
            }
        }
    }
    // ==================== 无敌帧处理 - RETURN阶段 ====================

    /**
     * RETURN阶段：处理【节奏控制】
     * 在原版设置完无敌帧后，将其缩短
     * 这样既能正常触发无敌帧保护，又能缩短保护时间
     */
    @Inject(method = "hurt", at = @At("RETURN"))
    private void jujutsuAddon$handleResetIframe(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide) return;
        // 只有当伤害成功造成时才处理
        if (!cir.getReturnValue()) return;
        int configValue = AddonConfig.COMMON.iframeSetTo.get();
        // ===== 全局重置模式 =====
        if (AddonConfig.COMMON.ignoreAllIframes.get()) {
            if (target.invulnerableTime > configValue) {
                target.invulnerableTime = configValue;
            }
            return;
        }
        // ===== 节奏控制白名单 =====
        // 只有原版设置的无敌帧 > 我们想要的值时才缩短
        List<? extends String> resetWhitelist = AddonConfig.COMMON.ignoreIframeSkills.get();
        if (jujutsuAddon$matchesDamageSource(source, resetWhitelist)) {
            if (target.invulnerableTime > configValue) {
                target.invulnerableTime = configValue;
            }
        }
    }
    // ==================== 附魔触发 ====================

    @Inject(method = "hurt", at = @At("HEAD"))
    private void jujutsuAddon$triggerEnchantments(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide) return;
        Entity attacker = source.getEntity();
        // 玩家直接攻击（JJK技能）
        if (attacker instanceof Player player) {
            if (jujutsuAddon$isJJKDamage(source)) {
                EnchantmentTriggerHandler.triggerEnchantments(player, target);
            }
        }
        // JJK 召唤物攻击
        else if (attacker instanceof TamableAnimal summon && summon.getOwner() instanceof Player owner) {
            String className = summon.getClass().getName();
            if (className.startsWith("radon.jujutsu_kaisen")) {
                EnchantmentTriggerHandler.triggerEnchantmentsForSummon(owner, summon, target);
            }
        }
    }
}